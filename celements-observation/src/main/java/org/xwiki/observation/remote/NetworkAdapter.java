package org.xwiki.observation.remote;

import java.util.function.Consumer;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Handle all the actual communication with the network.
 * <p>
 * It's the entry point of the chosen implementation for the actual event distribution.
 */
@ComponentRole
public interface NetworkAdapter {

  /**
   * Send serializable event to the network depending of the implementation.
   *
   * @param remoteEvent
   *          the serializable event to send
   */
  void send(RemoteEventData remoteEvent);

  /**
   * Start the adapter
   */
  void start(Consumer<RemoteEventData> onRemoteEvent);

  /**
   * Stop the adapter
   */
  void stop();
}
