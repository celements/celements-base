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
package org.xwiki.model.internal;

import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;

import com.google.common.collect.ImmutableMap;

/**
 * Get configuration data from the XWiki configuration using a {@link ConfigurationSource}. If no
 * {@link ConfigurationSource} component is found in the system then default to default values:
 * <ul>
 * <li>"xwiki" for the default wiki value</li>
 * <li>"Main" for the default space value</li>
 * <li>"WebHome" for the default page value</li>
 * </ul>
 *
 * @version $Id: 45e0af74861fd70baefd648b2bc4093da712355a $
 * @since 2.2M1
 */
@Component
@Singleton
public class DefaultModelConfiguration implements ModelConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelConfiguration.class);

  /**
   * Prefix for configuration keys for the Model module.
   */
  private static final String PREFIX = "model.";

  /**
   * Default values for all the Entity types, see
   * {@link #getDefaultReferenceValue(org.xwiki.model.EntityType)}.
   */
  private static final Map<EntityType, String> DEFAULT_VALUES = ImmutableMap
      .<EntityType, String>builder()
      .put(EntityType.WIKI, "xwiki")
      .put(EntityType.SPACE, "Main")
      .put(EntityType.DOCUMENT, "WebHome")
      .put(EntityType.ATTACHMENT, "filename")
      .put(EntityType.OBJECT, "object")
      .put(EntityType.OBJECT_PROPERTY, "property")
      .put(EntityType.CLASS_PROPERTY, "property")
      .build();

  /**
   * We want to make sure this component can be loaded and used even if there's no
   * ConfigurationSource available
   * in the system. This is why we lazy load the ConfigurationSource component.
   */
  @Requirement
  private ComponentManager componentManager;

  @Override
  public String getDefaultReferenceValue(EntityType type) {
    String name;
    try {
      // TODO: For the moment we only look in the XWiki properties file since otherwise looking into
      // Wiki, Space and User preferences cause some cyclic dependencies (we'll be able to do that
      // when all
      // code has been migrated to use References instead of Strings).
      ConfigurationSource configuration = this.componentManager.lookup(ConfigurationSource.class,
          "xwikiproperties");
      name = configuration.getProperty(
          PREFIX + "reference.default." + type.toString().toLowerCase(),
          DEFAULT_VALUES.get(type));
    } catch (ComponentLookupException exc) {
      LOGGER.debug("Failed to load xwikiproperties. Using default Model values", exc);
      name = DEFAULT_VALUES.get(type);
    }
    return name;
  }
}
