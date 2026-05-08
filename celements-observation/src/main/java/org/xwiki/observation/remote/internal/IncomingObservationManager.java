package org.xwiki.observation.remote.internal;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.stereotype.Service;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

@Service
public class IncomingObservationManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(IncomingObservationManager.class);

  private final RemoteObservationManagerConfiguration configuration;
  private final EventConverterManager eventConverterManager;
  private final ObservationManager observationManager;
  private final RemoteObservationManagerContext remoteEventManagerContext;
  private final Execution execution;
  private final ExecutionContextManager executionContextManager;
  private final BeanFactory beanFactory;

  @Inject
  public IncomingObservationManager(
      RemoteObservationManagerConfiguration configuration,
      EventConverterManager eventConverterManager,
      ObservationManager observationManager,
      RemoteObservationManagerContext remoteEventManagerContext,
      Execution execution,
      ExecutionContextManager executionContextManager,
      BeanFactory beanFactory) {
    this.configuration = configuration;
    this.eventConverterManager = eventConverterManager;
    this.observationManager = observationManager;
    this.remoteEventManagerContext = remoteEventManagerContext;
    this.execution = execution;
    this.executionContextManager = executionContextManager;
    this.beanFactory = beanFactory;
  }

  @PostConstruct
  public void initialize() {
    NetworkAdapter adapter = configuration.getImplementation()
        .map(name -> beanFactory.getBean(name, NetworkAdapter.class))
        .orElse(null);
    if (adapter == null) {
      LOGGER.info("Remote observation manager is disabled");
      return;
    }
    adapter.start(this::notify);
  }

  public void notify(RemoteEventData remoteEvent) {
    LocalEventData localEvent = null;
    try {
      initEContext();
      localEvent = eventConverterManager.createLocalEventData(remoteEvent);
      if (localEvent != null) {
        remoteEventManagerContext.pushRemoteState();
        observationManager.notify(localEvent.getEvent(), localEvent.getSource(),
            localEvent.getData());
      }
    } catch (ExecutionContextException e) {
      LOGGER.error("Failed to initialize execution context", e);
    } finally {
      if (localEvent != null) {
        remoteEventManagerContext.popRemoteState();
      }
      execution.removeContext();
    }
  }

  private void initEContext() throws ExecutionContextException {
    ExecutionContext executionContext = new ExecutionContext();
    execution.setContext(executionContext);
    executionContextManager.initialize(executionContext);
  }
}
