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
import static java.util.stream.Collectors.*;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.properties.ConverterManager;

import com.celements.configuration.AbstractConvertingConfigurationSource;

import one.util.streamex.StreamEx;

/**
 * Wrap a Commons Configuration instance into a XWiki {@link ConfigurationSource}. This allows us to
 * reuse the <a href=
 * "http://commons.apache.org/configuration/"numerous types of Configuration<a/> provided by Commons
 * Configuration
 * (properties file, XML files, databases, etc).
 *
 * @version $Id$
 * @since 1.6M1
 */
public class CommonsConfigurationSource extends AbstractConvertingConfigurationSource {

  private final Configuration commonsConfig;

  public CommonsConfigurationSource(
      @NotNull Configuration commonsConfig,
      @Nullable ConverterManager converterManager) {
    super(converterManager);
    this.commonsConfig = checkNotNull(commonsConfig);
  }

  protected static final Configuration loadConfiguration(ResourceLoader loader, String fileName) {
    try {
      URL propertiesUrl = loader.getResource("/WEB-INF/" + fileName).getURL();
      return new PropertiesConfiguration(propertiesUrl);
    } catch (IOException | ConfigurationException exc) {
      throw new ConfigurationRuntimeException("for " + fileName, exc);
    }
  }

  @Override
  protected Object getValue(String key, Class<?> type) {
    Function<String, Object> getter = commonsConfig::getProperty;
    if (type != null) {
      if (String.class.isAssignableFrom(type)) {
        getter = commonsConfig::getString;
      } else if (List.class.isAssignableFrom(type)) {
        getter = commonsConfig::getList;
      } else if (Properties.class.isAssignableFrom(type)) {
        getter = commonsConfig::getProperties;
      }
    }
    return getter.apply(key);
  }

  @Override
  public List<String> getKeys() {
    Iterator<?> keys = commonsConfig.getKeys();
    return StreamEx.of(keys)
        .filter(Objects::nonNull)
        .map(Object::toString)
        .collect(toList());
  }

  @Override
  public boolean containsKey(String key) {
    return commonsConfig.containsKey(key);
  }

  @Override
  public boolean isEmpty() {
    return commonsConfig.isEmpty();
  }

}
