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
package org.xwiki.observation.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.Event;

/**
 * Default implementation of the {@link ObservationManager}.
 * <p>
 * This component use synchronized for concurrent protection instead of having
 * {@link java.util.concurrent.ConcurrentHashMap} everywhere because it's more efficient since most
 * of methods access to
 * several maps and generally do enumerations.
 *
 * @version $Id$
 */
@Component
public class DefaultObservationManager implements ObservationManager, Initializable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultObservationManager.class);

  /**
   * Registered listeners indexed on Event classes so that it's fast to find all the listeners
   * registered for a given
   * event, so that {@link #notify} calls execute fast and in a fixed amount a time.
   *
   * @todo Should we allow event inheritance?
   */
  private Map<Class<? extends Event>, Map<String, RegisteredListener>> listenersByEvent = new ConcurrentHashMap<>();

  /**
   * Registered listeners index by listener name. It makes it fast to perform operations on already
   * registered listeners.
   */
  private Map<String, EventListener> listenersByName = new ConcurrentHashMap<>();

  /**
   * Used to find all components implementing {@link EventListener} to register them automatically.
   */
  @Requirement
  private ComponentManager componentManager;

  /**
   * Helper class to store the list of events of a given type associated with a given listener. We
   * need this for performance reasons and also in order to be able to add events after a listener
   * has been registered.
   */
  private static class RegisteredListener {

    /**
     * Events of a given type associated with a given listener.
     */
    private List<Event> events = new ArrayList<>();

    /**
     * Listener associated with the events.
     */
    private EventListener listener;

    /**
     * @param listener
     *          the listener associated with the events.
     * @param event
     *          the first event to associate with the passed listener. More events are added by
     *          calling {@link #addEvent(Event)}
     */
    RegisteredListener(EventListener listener, Event event) {
      addEvent(event);
      this.listener = listener;
    }

    /**
     * @param event
     *          the event to add
     */
    void addEvent(Event event) {
      this.events.add(event);
    }

    /**
     * @param event
     *          the event to remove
     */
    void removeEvent(Event event) {
      this.events.remove(event);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Register all components implementing the {@link EventListener} interface.
   *
   * @see Initializable#initialize()
   */
  @Override
  public void initialize() throws InitializationException {
    try {
      for (EventListener listener : this.componentManager.lookupList(EventListener.class)) {
        addListener(listener);
      }
    } catch (ComponentLookupException e) {
      throw new InitializationException("Failed to lookup Event Listeners", e);
    }
  }

  @Override
  public void addListener(EventListener eventListener) {
    // Register the listener by name. If already registered, override it.
    EventListener previousListener = this.listenersByName.put(eventListener.getName(),
        eventListener);

    // If the passed event listener name is already registered, log a warning
    if (previousListener != null) {
      LOGGER.warn(
          "The [" + eventListener.getClass().getName() + "] listener has overwritten a previously "
              + "registered listener [" + previousListener.getClass().getName()
              + "] since they both are registered under the same id [" + eventListener.getName()
              + "]."
              + " In the future consider removing a Listener first if you really want to register it again.");
    }

    // For each event defined for this listener, add it to the Event Map.
    for (Event event : eventListener.getEvents()) {
      // Check if this is a new Event type not already registered
      Map<String, RegisteredListener> eventListeners = this.listenersByEvent.get(event.getClass());
      if (eventListeners == null) {
        // No listener registered for this event yet. Create a map to store listeners for this
        // event.
        eventListeners = new ConcurrentHashMap<>();
        this.listenersByEvent.put(event.getClass(), eventListeners);
        // There is no RegisteredListener yet, create one
        eventListeners.put(eventListener.getName(), new RegisteredListener(eventListener, event));
      } else {
        // Add an event to existing RegisteredListener object
        RegisteredListener registeredListener = eventListeners.get(eventListener.getName());
        if (registeredListener == null) {
          eventListeners.put(eventListener.getName(), new RegisteredListener(eventListener, event));
        } else {
          registeredListener.addEvent(event);
        }
      }
    }
  }

  @Override
  public void removeListener(String listenerName) {
    this.listenersByName.remove(listenerName);
    for (Map.Entry<Class<? extends Event>, Map<String, RegisteredListener>> entry : this.listenersByEvent
        .entrySet()) {
      entry.getValue().remove(listenerName);
      if (entry.getValue().isEmpty()) {
        this.listenersByEvent.remove(entry.getKey());
      }
    }
  }

  @Override
  public void addEvent(String listenerName, Event event) {
    Map<String, RegisteredListener> listeners = this.listenersByEvent.get(event.getClass());
    RegisteredListener listener = listeners.get(listenerName);
    if (listener != null) {
      listener.addEvent(event);
    }
  }

  @Override
  public void removeEvent(String listenerName, Event event) {
    Map<String, RegisteredListener> listeners = this.listenersByEvent.get(event.getClass());
    RegisteredListener listener = listeners.get(listenerName);
    if (listener != null) {
      listener.removeEvent(event);
    }
  }

  @Override
  public EventListener getListener(String listenerName) {
    return this.listenersByName.get(listenerName);
  }

  @Override
  public void notify(Event event, Object source, Object data) {
    // Find all listeners for this event
    Map<String, RegisteredListener> regListeners = this.listenersByEvent.get(event.getClass());
    if (regListeners != null) {
      notify(regListeners.values(), event, source, data);
    }

    // Find listener listening all events
    Map<String, RegisteredListener> allEventRegListeners = this.listenersByEvent
        .get(AllEvent.class);
    if (allEventRegListeners != null) {
      notify(allEventRegListeners.values(), event, source, data);
    }
  }

  /**
   * Call the provided listeners matching the passed Event. The definition of <em>source</em> and
   * <em>data</em> is
   * purely up to the communicating classes.
   *
   * @param listeners
   *          the listeners to notify
   * @param event
   *          the event to pass to the registered listeners
   * @param source
   *          the source of the event (or <code>null</code>)
   * @param data
   *          the additional data related to the event (or <code>null</code>)
   */
  private void notify(Collection<RegisteredListener> listeners, Event event, Object source,
      Object data) {
    for (RegisteredListener listener : listeners) {
      // Verify that one of the events matches and send the first matching event
      for (Event listenerEvent : listener.events) {
        if (listenerEvent.matches(event)) {
          try {
            listener.listener.onEvent(event, source, data);
          } catch (Exception e) {
            // protect from bad listeners
            LOGGER.error("Fail to send event [" + event + "] to listener [" + listener.listener
                + "]", e);
          }
          // Only send the first matching event since the listener should only be called once per
          // event.
          break;
        }
      }
    }
  }

  @Override
  public void notify(Event event, Object source) {
    notify(event, source, null);
  }

}
