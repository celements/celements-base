package org.xwiki.observation.remote.internal;

import static com.google.common.base.Strings.*;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.stereotype.Component;
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

  private final ConfigurationSource configSource;

  @Inject
  public DefaultRemoteObservationManagerConfiguration(
      @Named("xwikiproperties") ConfigurationSource configSource) {
    this.configSource = configSource;
  }

  @Override
  public boolean isEnabled() {
    return getImplementation().isPresent();
  }

  @Override
  public Optional<String> getImplementation() {
    return Optional.ofNullable(configSource.getProperty(CFG_KEY, String.class))
        .map(str -> nullToEmpty(str).trim())
        .filter(s -> !s.isEmpty());
  }

}
