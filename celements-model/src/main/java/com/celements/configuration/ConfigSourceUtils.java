package com.celements.configuration;

import static com.google.common.base.Strings.*;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.EntityReference;

import com.celements.common.MorePredicates;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.xpn.xwiki.web.Utils;

/**
 * @deprecated since 6.0
 */
@Deprecated
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
    FluentIterable<?> values;
    Object prop = configSrc.getProperty(key);
    if (prop instanceof Iterable) {
      values = FluentIterable.from((Iterable<?>) prop);
    } else if (prop != null) {
      values = FluentIterable.of(prop);
    } else {
      values = FluentIterable.of();
    }
    return values.filter(Predicates.notNull()).transform(Functions.toStringFunction()).filter(
        MorePredicates.stringNotBlankPredicate()).toList();
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
