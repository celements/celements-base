package org.xwiki.observation.remote.internal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    return getConfigSource()
        .map(cfg -> cfg.getProperty("observation.remote.enabled", Boolean.class))
        .filter(Objects::nonNull)
        .orElse(false);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<String> getChannels() {
    return getConfigSource()
        .map(cfg -> cfg.getProperty("observation.remote.channels", List.class))
        .filter(Objects::nonNull)
        .orElse(Collections.emptyList());
  }

  @Override
  public String getNetworkAdapter() {
    return getConfigSource()
        .map(cfg -> cfg.getProperty("observation.remote.networkadapter", "jgroups"))
        .orElse("jgroups");
  }

  private Optional<ConfigurationSource> getConfigSource() {
    try {
      return Optional.of(componentManager.lookup(ConfigurationSource.class, "xwikiproperties"));
    } catch (ComponentLookupException e) {
      return Optional.empty();
    }
  }

}
