package org.xwiki.configuration;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SystemEnvUtils {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private SystemEnvUtils() {}

  public static Optional<String> getEnv(String key) {
    if ((key == null) || key.isBlank()) {
      return Optional.empty();
    }
    var envKey = key.trim().replace('.', '_').replace('-', '_');
    return Optional.ofNullable(System.getenv(envKey.toUpperCase(Locale.ROOT)))
        .or(() -> Optional.ofNullable(System.getenv(envKey)));
  }

  public static Optional<List<String>> getEnvList(String key) {
    return getEnv(key).flatMap(value -> tryParseJsonArray(value.trim()));
  }

  static Optional<List<String>> tryParseJsonArray(String value) {
    if (!value.startsWith("[") || !value.endsWith("]")) {
      return Optional.empty();
    }
    try {
      return Optional.of(OBJECT_MAPPER.readValue(value, STRING_LIST));
    } catch (JsonProcessingException exc) {
      return Optional.empty();
    }
  }

}
