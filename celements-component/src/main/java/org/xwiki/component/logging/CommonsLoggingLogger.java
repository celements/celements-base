/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.component.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge between XWiki Logging and Commons Logging.
 * 2021-06-11 replace with slf4j implementation and mark deprecated
 *
 * @version $Id$
 * @since 2.0M1
 * @deprecated since 5.1
 */
@Deprecated
public class CommonsLoggingLogger extends AbstractLogger {

  /**
   * Wrapped Commons Logging logger object. This communicates with the underlying logging framework.
   */
  private Logger logger;

  public CommonsLoggingLogger(Class<?> clazz) {
    this.logger = LoggerFactory.getLogger(clazz);
  }

  /**
   * {@inheritDoc}
   *
   * @see Logger#debug(String, Object...)
   */
  @Override
  public void debug(String message, Object... objects) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug(formatMessage(message, objects));
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see Logger#debug(String, Throwable, Object...)
   */
  @Override
  public void debug(String message, Throwable throwable, Object... objects) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug(formatMessage(message, objects), throwable);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see Logger#debug(String, Throwable)
   */
  @Override
  public void debug(String message, Throwable throwable) {
    this.logger.debug(message, throwable);
  }

  /**
   * {@inheritDoc}
   *
   * @see Logger#debug(String)
   */
  @Override
  public void debug(String message) {
    this.logger.debug(message);
  }

  @Override
  public void error(String message, Object... objects) {
    if (this.logger.isErrorEnabled()) {
      this.logger.error(formatMessage(message, objects));
    }
  }

  @Override
  public void error(String message, Throwable throwable, Object... objects) {
    if (this.logger.isErrorEnabled()) {
      this.logger.error(formatMessage(message, objects), throwable);
    }
  }

  @Override
  public void error(String message, Throwable throwable) {
    this.logger.error(message, throwable);
  }

  @Override
  public void error(String message) {
    this.logger.error(message);
  }

  @Override
  public void info(String message, Object... objects) {
    if (this.logger.isInfoEnabled()) {
      this.logger.info(formatMessage(message, objects));
    }
  }

  @Override
  public void info(String message, Throwable throwable, Object... objects) {
    if (this.logger.isInfoEnabled()) {
      this.logger.info(formatMessage(message, objects), throwable);
    }
  }

  @Override
  public void info(String message, Throwable throwable) {
    this.logger.info(message, throwable);
  }

  @Override
  public void info(String message) {
    this.logger.info(message);
  }

  @Override
  public boolean isDebugEnabled() {
    return this.logger.isDebugEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return this.logger.isErrorEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return this.logger.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return this.logger.isWarnEnabled();
  }

  @Override
  public void warn(String message, Object... objects) {
    if (this.logger.isWarnEnabled()) {
      this.logger.warn(formatMessage(message, objects));
    }
  }

  @Override
  public void warn(String message, Throwable throwable, Object... objects) {
    if (this.logger.isWarnEnabled()) {
      this.logger.warn(formatMessage(message, objects), throwable);
    }
  }

  @Override
  public void warn(String message, Throwable throwable) {
    this.logger.warn(message, throwable);
  }

  @Override
  public void warn(String message) {
    this.logger.warn(message);
  }
}
