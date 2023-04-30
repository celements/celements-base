package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentRole;
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

  @Requirement
  private Map<String, ConfigurationSource> configSources;

  @Override
  public boolean isEnabled() {
    Boolean enabled = getCfgSrc().getProperty("observation.remote.enabled", Boolean.class);
    return (enabled != null) && enabled;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getChannels() {
    List<String> channels = getCfgSrc().getProperty("observation.remote.channels", List.class);
    return channels == null ? Collections.<String>emptyList() : channels;
  }

  @Override
  public String getNetworkAdapter() {
    return getCfgSrc().getProperty("observation.remote.networkadapter", "jgroups");
  }

  private ConfigurationSource getCfgSrc() {
    ConfigurationSource cfgSrc = configSources.get("xwikiproperties");
    if (cfgSrc == null) {
      cfgSrc = configSources.get(ComponentRole.DEFAULT_HINT);
    }
    return cfgSrc;
  }

}
