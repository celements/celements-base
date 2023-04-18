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

import static java.util.stream.Collectors.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;

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

  private Configuration configuration;

  @Requirement
  private Container container;

  protected void setConfiguration(String name) throws ConfigurationException {
    String fileName = "/WEB-INF/" + name + ".properties";
    try {
      URL propertiesUrl = container.getApplicationContext().getResource(fileName);
      configuration = new PropertiesConfiguration(propertiesUrl);
    } catch (MalformedURLException exc) {
      throw new ConfigurationException("for " + fileName, exc);
    }
  }

  @Override
  protected Object getValue(String key, Class<?> type) {
    Function<String, Object> getter = configuration::getProperty;
    if (type != null) {
      if (String.class.isAssignableFrom(type)) {
        getter = configuration::getString;
      } else if (List.class.isAssignableFrom(type)) {
        getter = configuration::getList;
      } else if (Properties.class.isAssignableFrom(type)) {
        getter = configuration::getProperties;
      }
    }
    return getter.apply(key);
  }

  @Override
  public List<String> getKeys() {
    Iterator<?> keys = configuration.getKeys();
    return StreamEx.of(keys)
        .map(Objects::nonNull)
        .map(Object::toString)
        .collect(toList());
  }

  @Override
  public boolean containsKey(String key) {
    return configuration.containsKey(key);
  }

  @Override
  public boolean isEmpty() {
    return configuration.isEmpty();
  }

}
