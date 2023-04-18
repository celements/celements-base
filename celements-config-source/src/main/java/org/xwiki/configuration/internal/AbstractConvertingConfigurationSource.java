package org.xwiki.configuration.internal;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.properties.ConverterManager;

import com.celements.common.MoreObjectsCel;
import com.google.common.base.Strings;

public abstract class AbstractConvertingConfigurationSource implements ConfigurationSource {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Requirement
  private ConverterManager converterManager;

  @Override
  public <T> Stream<T> stream(String key, Class<T> type) {
    return MoreObjectsCel.stream(getPropertyInternal(key, null))
        .map(value -> convertValue(type, value));
  }

  @Override
  public <T> Optional<T> get(String key, Class<T> type) {
    return Optional.ofNullable(getPropertyInternal(key, type));
  }

  protected <T> T getPropertyInternal(String key, Class<T> type) {
    try {
      Object value = getValue(key, type);
      return convertValue(type, value);
    } catch (ClassCastException | org.xwiki.properties.converter.ConversionException
        | org.apache.commons.configuration.ConversionException exc) {
      throw new org.xwiki.configuration.ConversionException(
          "Key [" + key + "] is not of type [" + type + "]", exc);
    }
  }

  protected abstract Object getValue(String key, Class<?> type);

  @SuppressWarnings("unchecked")
  protected <T> T convertValue(Class<T> type, Object value) {
    if ((type != null) && (value != null)) {
      value = converterManager.convert(type, value);
    }
    // we need nulls instead of defaults for Optional/Stream
    if (value instanceof String) {
      value = Strings.emptyToNull(((String) value).trim());
    } else if ((value instanceof Collection<?>) && ((Collection<?>) value).isEmpty()) {
      value = null;
    }
    if (type != null) {
      return type.cast(value);
    } else {
      return (T) value;
    }
  }

}
