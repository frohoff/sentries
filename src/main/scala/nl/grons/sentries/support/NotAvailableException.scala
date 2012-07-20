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

/**
 * Resource not available.
 */
class NotAvailableException(val resourceName: String, message: String, cause: Throwable) extends RuntimeException(message, cause)
