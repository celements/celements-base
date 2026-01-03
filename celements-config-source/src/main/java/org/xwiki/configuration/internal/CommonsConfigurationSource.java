/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.configuration.internal;

import static com.google.common.base.Preconditions.*;

import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConversionException;
import org.xwiki.properties.ConverterManager;

import one.util.streamex.StreamEx;

/**
 * Wrap a Commons Configuration instance into a XWiki {@link ConfigurationSource}. This allows us to
 * reuse the <a href="http://commons.apache.org/configuration/">numerous types of Configuration</a>
 * provided by Commons Configuration (properties file, XML files, databases, etc).
 *
 * @version $Id$
 * @since 1.6M1
 */
public class CommonsConfigurationSource implements ConfigurationSource {

  private Configuration configuration;

  /**
   * Component used for performing type conversions.
   */
  @Requirement
  private ConverterManager converterManager;

  protected void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#getProperty(String, Object)
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key, T defaultValue) {
    checkNotNull(defaultValue);
    return getPropertyOpt(key, (Class<T>) defaultValue.getClass())
        .orElse(defaultValue);
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#getProperty(String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key) {
    var envProp = getEnvProperty(key);
    if (envProp.isPresent()) {
      return (T) envProp.get();
    }
    return (T) configuration.getProperty(key);
  }

  @Override
  public <T> Optional<T> getPropertyOpt(String key) {
    return Optional.ofNullable(getProperty(key));
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#getProperty(String, Class)
   */
  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key, Class<T> valueClass) {
    T result = null;
    try {
      if (String.class.equals(valueClass)) {
        result = (T) getEnvProperty(key)
            .orElseGet(() -> configuration.getString(key));
      } else if (List.class.isAssignableFrom(valueClass)) {
        result = (T) configuration.getList(key);
      } else if (Properties.class.isAssignableFrom(valueClass)) {
        result = (T) configuration.getProperties(key);
      } else {
        result = getPropertyOpt(key)
            .map(value -> converterManager.convert(valueClass, value))
            .orElse(null);
      }
    } catch (org.apache.commons.configuration.ConversionException | ConversionException e) {
      throw new ConversionException(
          "Key [" + key + "] is not of type [" + valueClass.getName() + "]", e);
    }
    return result;
  }

  public <T> Optional<T> getPropertyOpt(String key, Class<T> valueClass) {
    return Optional.ofNullable(getProperty(key, valueClass));
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#getKeys()
   */
  @Override
  public List<String> getKeys() {
    return StreamEx.of(configuration.getKeys()).toList();
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#containsKey(String)
   */
  @Override
  public boolean containsKey(String key) {
    return getEnvProperty(key).isPresent() || configuration.containsKey(key);
  }

  /**
   * {@inheritDoc}
   *
   * @see ConfigurationSource#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return configuration.isEmpty();
  }

  private Optional<String> getEnvProperty(String key) {
    var envKey = Optional.ofNullable(key)
        .map(k -> k.replace('.', '_').replace('-', '_'))
        .orElse("");
    return Optional.ofNullable(System.getenv(envKey.toUpperCase()))
        .or(() -> Optional.ofNullable(System.getenv(envKey)));
  }

}
