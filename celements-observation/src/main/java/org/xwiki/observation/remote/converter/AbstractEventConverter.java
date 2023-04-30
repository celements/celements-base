package org.xwiki.observation.remote.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for events converters. Provide a default priority.
 *
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractEventConverter implements LocalEventConverter, RemoteEventConverter {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public int getPriority() {
    return 1000;
  }

}
