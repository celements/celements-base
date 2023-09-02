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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.NetworkAdapter;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.RemoteEventException;
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
@Component
public class DefaultRemoteObservationManager implements RemoteObservationManager, Initializable {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(DefaultRemoteObservationManager.class);

  /**
   * Access {@link RemoteObservationManager} configuration.
   */
  @Requirement
  private RemoteObservationManagerConfiguration configuration;

  /**
   * Used to convert local event from and to remote event.
   */
  @Requirement
  private EventConverterManager eventConverterManager;

  /**
   * Used to inject event coming from network.
   */
  @Requirement
  private ObservationManager observationManager;

  /**
   * Used to set some extra informations about the current event injected to the local
   * {@link ObservationManager}.
   */
  @Requirement
  private RemoteObservationManagerContext remoteEventManagerContext;

  /**
   * Used to initialize ExecutionContext for the remote->local thread.
   */
  @Requirement
  private Execution execution;

  /**
   * Used to initialize ExecutionContext for the remote->local thread.
   */
  @Requirement
  private ExecutionContextManager executionContextManager;

  /**
   * Used to lookup the network adapter.
   */
  @Requirement
  private ComponentManager componentManager;

  /**
   * The network adapter to use to actually send and receive network messages.
   */
  private NetworkAdapter networkAdapter;

  @Override
  public void initialize() throws InitializationException {
    try {
      String networkAdapterHint = this.configuration.getNetworkAdapter();
      this.networkAdapter = this.componentManager.lookup(NetworkAdapter.class, networkAdapterHint);
    } catch (ComponentLookupException e) {
      throw new InitializationException("Failed to initialize network adapter ["
          + this.configuration.getNetworkAdapter() + "]", e);
    }

    // start configured channels
    for (String channelId : this.configuration.getChannels()) {
      try {
        startChannel(channelId);
      } catch (RemoteEventException e) {
        LOGGER.error("Failed to start channel [" + channelId + "]", e);
      }
    }
  }

  @Override
  public void notify(LocalEventData localEvent) {
    if (this.remoteEventManagerContext.isRemoteState()) {
      // the event is a remote event
      return;
    }

    // Convert local->remote
    RemoteEventData remoteEvent = this.eventConverterManager.createRemoteEventData(localEvent);

    // if remote event data is not filled it mean the message should not be sent to the network
    if (remoteEvent != null) {
      this.networkAdapter.send(remoteEvent);
    }

    if (localEvent.getEvent() instanceof ApplicationStoppedEvent) {
      try {
        this.networkAdapter.stopAllChannels();
      } catch (RemoteEventException e) {
        LOGGER.error("Failed to stop channels", e);
      }
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

  @Override
  public void startChannel(String channelId) throws RemoteEventException {
    this.networkAdapter.startChannel(channelId);
  }

  @Override
  public void stopChannel(String channelId) throws RemoteEventException {
    this.networkAdapter.stopChannel(channelId);
  }

  private void initEContext() throws ExecutionContextException {
    ExecutionContext executionContext = new ExecutionContext();
    execution.setContext(executionContext);
    executionContextManager.initialize(executionContext);
  }

}
