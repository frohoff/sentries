/*
 * Sentries
 * Copyright (c) 2012. Erik van Oosten. All rights reserved.
 *
 * The primary distribution site is https://github.com/erikvanoosten/sentries
 *
 * This software is released under the terms of the Revised BSD License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package nl.grons.sentries.support

import com.yammer.metrics.core.{Stoppable, MetricName}
import java.util.concurrent.{ConcurrentHashMap, CopyOnWriteArrayList}
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.ref.WeakReference

/**
 * A registry of sentry instances.
 */
class SentriesRegistry() {

  private[this] val listeners = new CopyOnWriteArrayList[SentriesRegistryListener]().asScala
  private[this] val sentries = newSentriesMap()

  /**
   * Adds a {@link SentriesRegistryListener} to a collection of listeners that will be notified on
   * sentry creation.  Listeners will be notified in the order in which they are added.
   * <p/>
   * <b>N.B.:</b> The listener will be notified of all existing sentries when it first registers.
   *
   * @param listener the listener that will be notified
   */
  def addListener(listener: SentriesRegistryListener) {
    listeners += listener
    sentries.foreach {
      case (name, sentryRef) =>
        sentryRef.get match {
          case Some(sentry) => listener.onSentryAdded(name, sentry)
          case None => removeSentry(name)
        }
    }
  }

  /**
   * Removes a {@link SentriesRegistryListener} from this registry's collection of listeners.
   *
   * @param listener the listener that will be removed
   */
  def removeListener(listener: SentriesRegistryListener) {
    listeners -= listener
  }

  /**
   * Gets any existing sentry with the given name or, if none exists, adds the given sentry.
   *
   * @param sentry the sentry to add
   * @param sentryOwner the class that owns the sentry
   * @param name name of the sentry
   * @param sentryType sentryType type of sentry
   * @tparam S type of the sentry
   * @return either the existing sentry or {@code sentry}
   */
  def getOrAdd[S <: NamedSentry](sentry: S, sentryOwner: Class[_], name: String, sentryType: String): S =
    getOrAdd(createName(sentryOwner, name, sentryType), sentry)

  /**
   * Removes the sentry for the given class with the given name (and sentryType).
   *
   * @param sentryOwner the class that owns the sentry
   * @param name  the name of the sentry
   * @param sentryType the sentryType of the sentry
   */
  def removeSentry(sentryOwner: Class[_], name: String, sentryType: String) {
    removeSentry(createName(sentryOwner, name, sentryType))
  }

  /**
   * Removes the sentry with the given name.
   *
   * @param name the name of the sentry
   */
  def removeSentry(name: MetricName) {
    sentries.remove(name).map { sentry =>
      if (sentry.isInstanceOf[Stoppable]) sentry.asInstanceOf[Stoppable].stop()
      notifySentriesRemoved(name)
    }
  }

  /**
   * Override to customize how {@link MetricName}s are created.
   *
   * @param sentryOwner the class which owns the sentry
   * @param name  the name of the sentry
   * @param sentryType the sentry's sentryType
   * @return the sentry's full name
   */
  protected def createName(sentryOwner: Class[_], name: String, sentryType: String): MetricName =
    new MetricName(sentryOwner, name + "." + sentryType)

  /**
   * Returns a new {@link ConcurrentMap} implementation. Subclass this to do weird things with
   * your own {@link MetricsRegistry} implementation.
   *
   * @return a new {@link mutable.ConcurrentMap}
   */
  protected def newSentriesMap(): mutable.ConcurrentMap[MetricName, WeakReference[NamedSentry]] =
    new ConcurrentHashMap[MetricName, WeakReference[NamedSentry]](1024).asScala

  /**
   * Gets any existing sentry with the given name or, if none exists, adds the given sentry.
   *
   * @param name   the sentry's name
   * @param sentry the new sentry
   * @tparam S     the type of the sentry
   * @return either the existing sentry or {@code sentry}
   */
  private def getOrAdd[S <: NamedSentry](name: MetricName, sentry: S): S = {
    sentries.putIfAbsent(name, new WeakReference(sentry)) match {
      case Some(existingRef) if existingRef.get.isDefined =>
        if (sentry.isInstanceOf[Stoppable]) sentry.asInstanceOf[Stoppable].stop()
        existingRef.get.get.asInstanceOf[S]
      case _ =>
        notifySentriesAdded(name, sentry)
        sentry
    }
  }

  private def notifySentriesRemoved(name: MetricName) {
    listeners.foreach(_.onSentryRemoved(name))
  }

  private def notifySentriesAdded(name: MetricName, sentry: NamedSentry) {
    listeners.foreach(_.onSentryAdded(name, sentry))
  }

}
