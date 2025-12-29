package org.xwiki.observation.remote;

/**
 * Provide apis to manage the event network interface.
 */
public interface RemoteObservationManager {

  /**
   * Broadcast an event to the cluster.
   * <p>
   * This method is not supposed to be used directly for a new event unless the user specifically
   * want to bypass or emulate {@link org.xwiki.observation.ObservationManager}.
   */
  void notify(LocalEventData event);

  /**
   * Inject a remote event in the local {@link org.xwiki.observation.ObservationManager}.
   * <p>
   * This method is not supposed to be used directly for a new event unless the user specifically
   * want to bypass or emulate network.
   */
  void notify(RemoteEventData event);
}
