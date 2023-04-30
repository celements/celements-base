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
 *
 */
package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Register to {@link org.xwiki.observation.ObservationManager} for all events and send them to
 * {@link RemoteObservationManager}.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component("observation.remote")
public class LocalEventListener implements EventListener {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  /**
   * The name of the listener.
   */
  private static final String NAME = "observation.remote";

  /**
   * Used to know if remote observation manager is enabled.
   */
  @Requirement
  private RemoteObservationManagerConfiguration configuration;

  /**
   * Used to lookup for {@link RemoteObservationManager} if it's enabled. To avoid initializing it
   * if it's not needed.
   */
  @Requirement
  private ComponentManager componentManager;

  /**
   * We don't inject {@link RemoteObservationManager} automatically to load it only when necessary
   * (when remote
   * observation manager is enabled).
   */
  private RemoteObservationManager remoteObservationManager;

  @Override
  public List<Event> getEvents() {
    List<Event> events;
    if (this.configuration.isEnabled()) {
      events = Collections.<Event>singletonList(AllEvent.ALLEVENT);
    } else {
      events = Collections.emptyList();
    }
    return events;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    if (this.remoteObservationManager == null) {
      try {
        this.remoteObservationManager = this.componentManager
            .lookup(RemoteObservationManager.class);
      } catch (ComponentLookupException e) {
        logger.error("Fail to initialize RemoteObservationManager", e);
      }
    }
    this.remoteObservationManager.notify(new LocalEventData(event, source, data));
  }

}
