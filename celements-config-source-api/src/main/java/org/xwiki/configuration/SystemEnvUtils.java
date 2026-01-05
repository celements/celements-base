package org.xwiki.configuration;

import java.util.Locale;
import java.util.Optional;

public final class SystemEnvUtils {

  private SystemEnvUtils() {}

  public static Optional<String> getEnv(String key) {
    if ((key == null) || key.isBlank()) {
      return Optional.empty();
    }
    var envKey = key.trim().replace('.', '_').replace('-', '_');
    return Optional.ofNullable(System.getenv(envKey.toUpperCase(Locale.ROOT)))
        .or(() -> Optional.ofNullable(System.getenv(envKey)));
  }

}
