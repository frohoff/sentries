/*
 * Sentries
 * Copyright (c) 2012-2013 Erik van Oosten All rights reserved.
 *
 * The primary distribution site is https://github.com/erikvanoosten/sentries
 *
 * This software is released under the terms of the BSD 2-Clause License.
 * There is NO WARRANTY. See the file LICENSE for the full text.
 */

package nl.grons.sentries.core

import nl.grons.sentries.support.ChainableSentry
import com.yammer.metrics.core.Clock
import com.yammer.metrics.Metrics
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * Sentry that collects metric of invocations.
 * A new instance can be obtained through the [[nl.grons.sentries.SentrySupport]] mixin.
 */
class SimpleMetricsSentry(val resourceName: String, owner: Class[_]) extends ChainableSentry {

  val sentryType = "metrics"

  private[this] val clock = Clock.defaultClock()
  private[this] val timer = Metrics.newTimer(owner, resourceName + "." + sentryType + ".all")

  /**
   * Run the given code block in the context of this sentry, and return its value.
   */
  def apply[T](r: => T) = {
    val start = clock.tick()
    try r finally timer.update(clock.tick() - start, NANOSECONDS)
  }

  def reset() {}

}
