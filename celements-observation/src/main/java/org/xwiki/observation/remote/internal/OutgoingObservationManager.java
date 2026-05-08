package org.xwiki.observation.remote.internal;

import static com.google.common.base.Preconditions.*;
import static java.text.MessageFormat.*;

import java.io.NotSerializableException;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

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
 * Sends local observation events to the remote observation channel after local listener processing.
 * <p>
 * Local observation is reentrant: while one event is being notified, a listener may notify another
 * event. Sending each event remotely right after its own local listeners finish would let nested
 * events overtake their parent event on the remote nodes. This matters for causally related events,
 * for example when a document creation triggers another event that depends on the created document
 * already being visible remotely.
 * <p>
 * To preserve that local causal order, events are added to a per-thread queue and only the
 * outermost notification flushes the queue to the remote adapter. Nested notifications append to
 * the same queue, so the remote side receives events in the same order as they entered local
 * observation.
 */
@Service
public class OutgoingObservationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(OutgoingObservationManager.class);

  private final RemoteObservationManagerConfiguration configuration;
  private final EventConverterManager eventConverterManager;
  private final RemoteObservationManagerContext remoteEventManagerContext;
  private final BeanFactory beanFactory;
  private final ThreadLocal<LinkedList<LocalEventData>> queue = new ThreadLocal<>();
  final Map<Class<? extends Event>, LogCounter> logCountMap = new ConcurrentHashMap<>();

  @Inject
  public OutgoingObservationManager(
      RemoteObservationManagerConfiguration configuration,
      EventConverterManager eventConverterManager,
      RemoteObservationManagerContext remoteEventManagerContext,
      BeanFactory beanFactory) {
    this.configuration = configuration;
    this.eventConverterManager = eventConverterManager;
    this.remoteEventManagerContext = remoteEventManagerContext;
    this.beanFactory = beanFactory;
  }

  public boolean isEnabled() {
    return configuration.isEnabled() && !remoteEventManagerContext.isRemoteState();
  }

  public void notifyLocalThenRemote(
      LocalEventData localEvent, Consumer<LocalEventData> notifyLocal) {
    checkState(isEnabled(), "remote observation is disabled");
    boolean first = queue(localEvent);
    try {
      notifyLocal.accept(localEvent);
    } finally {
      if (first) {
        processQueue();
      }
    }
  }

  private boolean queue(LocalEventData localEvent) {
    boolean first = (queue.get() == null);
    if (first) {
      queue.set(new LinkedList<>());
    }
    queue.get().add(localEvent);
    return first;
  }

  private void processQueue() {
    var events = queue.get();
    queue.remove();
    events.stream()
        .filter(this::shouldNotifyRemote)
        .forEach(this::notify);
  }

  private boolean shouldNotifyRemote(LocalEventData localEvent) {
    return !localEvent.getEvent().getClass().isAnnotationPresent(Local.class);
  }

  void notify(LocalEventData localEvent) {
    NetworkAdapter networkAdapter = getNetworkAdapter();
    checkState(networkAdapter != null,
        "remote observation is enabled but network adapter implementation missing");
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

  private NetworkAdapter getNetworkAdapter() {
    return configuration.getImplementation()
        .map(name -> beanFactory.getBean(name, NetworkAdapter.class))
        .orElse(null);
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
    if (!logCountMap.containsKey(type) || logCountMap.get(type).isOneHourAgo()) {
      final LogCounter replacedLogCount = logCountMap.put(type, new LogCounter());
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
    logCountMap.get(type).increment();
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
