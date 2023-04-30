package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
  private ComponentManager componentManager;

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
    try {
      return componentManager.lookup(ConfigurationSource.class, "xwikiproperties");
    } catch (ComponentLookupException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

}
