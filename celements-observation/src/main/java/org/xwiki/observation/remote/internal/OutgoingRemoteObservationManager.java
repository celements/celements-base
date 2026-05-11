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

import static com.google.common.base.Preconditions.*;
import static java.text.MessageFormat.*;

import java.io.NotSerializableException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

import com.celements.common.observation.converter.Local;

/**
 * Sends local observation events to the remote observation channel after local processing.
 * <p>
 * Local observation is reentrant: listeners may trigger additional events while another event is
 * still being processed. To preserve the local causal event order across cluster nodes, outgoing
 * remote events are queued per thread and flushed only after the outermost notification completes.
 * This prevents nested events, such as derived/indexing events, from being delivered remotely
 * before the document event that caused them.
 *
 * @see CELDEV-1314
 */
@Service
public class OutgoingRemoteObservationManager {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(OutgoingRemoteObservationManager.class);

  private final RemoteObservationManagerConfiguration configuration;
  private final EventConverterManager eventConverterManager;
  private final RemoteObservationManagerContext remoteEventManagerContext;
  private final BeanFactory beanFactory;

  private final ThreadLocal<LinkedList<LocalEventData>> eventQueue = new ThreadLocal<>();
  private final Map<Class<? extends Event>, LogCounter> logCounts = new ConcurrentHashMap<>();

  private NetworkAdapter networkAdapter;

  @Inject
  public OutgoingRemoteObservationManager(
      RemoteObservationManagerConfiguration configuration,
      EventConverterManager eventConverterManager,
      RemoteObservationManagerContext remoteEventManagerContext,
      BeanFactory beanFactory) {
    this.configuration = configuration;
    this.eventConverterManager = eventConverterManager;
    this.remoteEventManagerContext = remoteEventManagerContext;
    this.beanFactory = beanFactory;
  }

  @PostConstruct
  public void initialize() {
    networkAdapter = configuration.getImplementation()
        .map(name -> beanFactory.getBean(name, NetworkAdapter.class))
        .orElse(null);
    checkState(!configuration.isEnabled() || (networkAdapter != null),
        "remote observation is enabled but network adapter implementation missing");
  }

  /**
   * Outgoing observation is enabled if it's explicitly enabled in the configuration and the
   * current event doesn't originate from {@link IncomingRemoteObservationManager} to avoid loops
   * between
   * nodes.
   */
  public boolean isEnabled() {
    return configuration.isEnabled() && !remoteEventManagerContext.isRemoteState();
  }

  public void notifyLocalThenRemote(
      LocalEventData localEvent, Consumer<LocalEventData> notifyLocal) {
    checkState(isEnabled(), "remote observation is disabled");
    boolean outermost = ensureEventQueue();
    eventQueue.get().add(localEvent);
    try {
      notifyLocal.accept(localEvent);
    } finally {
      if (outermost) {
        flushEventQueue();
      }
    }
  }

  private boolean ensureEventQueue() {
    if (eventQueue.get() == null) {
      eventQueue.set(new LinkedList<>());
      return true;
    }
    return false;
  }

  private void flushEventQueue() {
    var events = eventQueue.get();
    eventQueue.remove();
    events.stream()
        .filter(this::shouldNotifyRemote)
        .forEach(this::notify);
  }

  private boolean shouldNotifyRemote(LocalEventData localEvent) {
    return !localEvent.getEvent().getClass().isAnnotationPresent(Local.class);
  }

  void notify(LocalEventData localEvent) {
    try {
      RemoteEventData remoteEvent = eventConverterManager.createRemoteEventData(localEvent);
      if (remoteEvent != null) {
        networkAdapter.send(remoteEvent);
      } else {
        LOGGER.debug("skip [{}], no remote event data created for this event", localEvent);
      }
      if (localEvent.getEvent() instanceof ApplicationStoppedEvent) {
        networkAdapter.stop();
      }
    } catch (Exception exc) {
      if (isNotSerializableExc(exc)) {
        logNotSerializableExc(localEvent, exc);
      } else {
        LOGGER.error("Failed to send remote observation event [{}]", localEvent, exc);
      }
    }
  }

  private boolean isNotSerializableExc(Throwable exc) {
    return (exc != null) && ((exc instanceof NotSerializableException)
        || isNotSerializableExc(exc.getCause()));
  }

  private void logNotSerializableExc(LocalEventData localEvent, Exception exc) {
    var type = localEvent.getEvent().getClass();
    var source = localEvent.getSource();
    var data = localEvent.getData();
    String msg = "not serializable remote event [{0}], source [{1}], data [{2}]";
    if (!logCounts.containsKey(type) || logCounts.get(type).isOneHourAgo()) {
      final LogCounter replacedLogCount = logCounts.put(type, new LogCounter());
      if (replacedLogCount != null) {
        msg += ", occurred {3} times within last hour (since {4})";
        msg = format(msg, type.getSimpleName(), source, data,
            replacedLogCount.count, replacedLogCount.time);
      } else {
        msg = format(msg, type.getSimpleName(), source, data);
      }
      LOGGER.warn(msg, exc);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(format(msg, type.getSimpleName(), source, data), exc);
    }
    logCounts.get(type).increment();
  }

  static class LogCounter {

    final long time = System.currentTimeMillis();
    final AtomicLong count = new AtomicLong();

    public boolean isOneHourAgo() {
      return (System.currentTimeMillis() - time) >= (1000L * 60 * 60);
    }

    public void increment() {
      count.incrementAndGet();
    }

  }
}
