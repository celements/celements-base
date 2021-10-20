package com.celements.configuration;

import static com.google.common.base.Predicates.*;
import static com.google.common.base.Strings.*;
import static com.google.common.collect.ImmutableList.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.EntityReference;

import com.celements.model.util.ModelUtils;
import com.google.common.base.Optional;
import com.google.common.collect.Streams;
import com.xpn.xwiki.web.Utils;

public class ConfigSourceUtils {

  private ConfigSourceUtils() {}

  @NotNull
  public static Optional<String> getStringProperty(@NotNull String key) {
    return getStringProperty(getDefaultCfgSrc(), key);
  }

  @NotNull
  public static Optional<String> getStringProperty(@NotNull ConfigurationSource configSrc,
      @NotNull String key) {
    return Optional.fromNullable(emptyToNull(configSrc.getProperty(key, "").trim()));
  }

  @NotNull
  public static List<String> getStringListProperty(@NotNull String key) {
    return getStringListProperty(getDefaultCfgSrc(), key);
  }

  @NotNull
  public static List<String> getStringListProperty(@NotNull ConfigurationSource configSrc,
      @NotNull String key) {
    return streamProperty(key, configSrc).collect(toImmutableList());
  }

  private static Stream<?> getPropertyAsStream(ConfigurationSource configSrc, String key) {
    Stream<?> values;
    Object prop = configSrc.getProperty(key);
    if (prop instanceof Iterable) {
      values = Streams.stream((Iterable<?>) prop);
    } else if (prop != null) {
      values = Stream.of(prop);
    } else {
      values = Stream.empty();
    }
    return values;
  }

  @NotNull
  public static Stream<String> streamProperty(@NotNull String key,
      @NotNull Iterable<ConfigurationSource> configSources) {
    return Streams.stream(configSources)
        .filter(Objects::nonNull)
        .flatMap(cfgSrc -> getPropertyAsStream(cfgSrc, key))
        .filter(Objects::nonNull)
        .map(Object::toString)
        .map(String::trim)
        .filter(not(String::isEmpty));
  }

  @NotNull
  public static Stream<String> streamProperty(@NotNull String key,
      @NotNull ConfigurationSource... configSources) {
    return streamProperty(key, Arrays.asList(configSources));
  }

  @NotNull
  public static <T extends EntityReference> Optional<T> getReferenceProperty(@NotNull String key,
      @NotNull Class<T> token) {
    return getReferenceProperty(getDefaultCfgSrc(), key, token);
  }

  @NotNull
  public static <T extends EntityReference> Optional<T> getReferenceProperty(
      @NotNull ConfigurationSource configSrc, @NotNull String key, @NotNull Class<T> token) {
    try {
      return Optional.of(getModelUtils().resolveRef(configSrc.getProperty(key, ""), token));
    } catch (IllegalArgumentException iae) {
      return Optional.absent();
    }
  }

  private static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private static ConfigurationSource getDefaultCfgSrc() {
    return Utils.getComponent(ConfigurationSource.class);
  }

}
