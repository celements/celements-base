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
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.observation.remote.converter.EventConverterManager;

/**
 * JGoups based {@link RemoteObservationManager}. It's also the default implementation for now.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Service
public class DefaultRemoteObservationManager implements RemoteObservationManager {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DefaultRemoteObservationManager.class);

  /**
   * Access {@link RemoteObservationManager} configuration.
   */
  private final RemoteObservationManagerConfiguration configuration;

  /**
   * Used to convert local event from and to remote event.
   */
  private final EventConverterManager eventConverterManager;

  /**
   * Used to inject event coming from network.
   */
  private final ObservationManager observationManager;

  /**
   * Used to set some extra informations about the current event injected to the local
   * {@link ObservationManager}.
   */
  private final RemoteObservationManagerContext remoteEventManagerContext;

  /**
   * Used to initialize ExecutionContext for the remote->local thread.
   */
  private final Execution execution;

  /**
   * Used to initialize ExecutionContext for the remote->local thread.
   */
  private final ExecutionContextManager executionContextManager;

  /**
   * Used to lookup the network adapter.
   */
  private final BeanFactory beanFactory;

  /**
   * The network adapter to use to actually send and receive network messages.
   */
  private NetworkAdapter networkAdapter;

  @Inject
  public DefaultRemoteObservationManager(
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
    var adapter = configuration.getImplementation()
        .map(name -> beanFactory.getBean(name, NetworkAdapter.class))
        .orElse(null);
    if (adapter == null) {
      LOGGER.info("Remote observation manager is disabled");
      return;
    }
    adapter.start(this::notify);
    networkAdapter = adapter;
  }

  @Override
  public void notify(LocalEventData localEvent) {
    if (networkAdapter == null) {
      throw new IllegalStateException("Remote observation manager is disabled");
    }
    if (this.remoteEventManagerContext.isRemoteState()) {
      return; // the event is a remote event
    }
    RemoteEventData remoteEvent = this.eventConverterManager.createRemoteEventData(localEvent);
    // if remote event data is not filled it mean the message should not be sent to the network
    if (remoteEvent != null) {
      networkAdapter.send(remoteEvent);
    }
    if (localEvent.getEvent() instanceof ApplicationStoppedEvent) {
      networkAdapter.stop();
    }
  }

  @Override
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
