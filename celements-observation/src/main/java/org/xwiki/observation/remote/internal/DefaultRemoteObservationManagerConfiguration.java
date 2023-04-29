package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

/**
 * Provide remote events specific configuration.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultRemoteObservationManagerConfiguration
    implements RemoteObservationManagerConfiguration {

  /**
   * USed to access configuration storage.
   */
  @Requirement("xwikiproperties")
  private ConfigurationSource configurationSource;

  @Override
  public boolean isEnabled() {
    Boolean enabled = this.configurationSource.getProperty("observation.remote.enabled",
        Boolean.class);
    return (enabled != null) && enabled;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getChannels() {
    List<String> channels = this.configurationSource.getProperty("observation.remote.channels",
        List.class);

    return channels == null ? Collections.<String>emptyList() : channels;
  }

  @Override
  public String getNetworkAdapter() {
    return this.configurationSource.getProperty("observation.remote.networkadapter", "jgroups");
  }
}
