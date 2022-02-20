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

import java.net.URL;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;

/**
 * Looks for configuration data in {@code /WEB-INF/xwiki.properties}.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component("xwikiproperties")
public class XWikiPropertiesConfigurationSource extends CommonsConfigurationSource
    implements Initializable {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(XWikiPropertiesConfigurationSource.class);

  private static final String XWIKI_PROPERTIES_FILE = "/WEB-INF/xwiki.properties";

  /**
   * Injected by the Component Manager.
   */
  @Requirement
  private Container container;

  /**
   * {@inheritDoc}
   *
   * @see Initializable#initialize()
   */
  @Override
  public void initialize() throws InitializationException {
    // Register the Commons Properties Configuration, looking for a xwiki.properties file
    // in the XWiki path somewhere.
    URL xwikiPropertiesUrl;
    try {
      xwikiPropertiesUrl = this.container.getApplicationContext()
          .getResource(XWIKI_PROPERTIES_FILE);
      setConfiguration(new PropertiesConfiguration(xwikiPropertiesUrl));
    } catch (Exception e) {
      // Note: if we cannot read the configuration file we log a warning but continue since XWiki
      // will use
      // default values for all configurable elements.
      LOGGER.warn("Failed to load configuration file [{}]. Using default configuration.",
          XWIKI_PROPERTIES_FILE, e);
      // Use a default Commons Configuration implementation since we couldn't use a Properties
      // configuration.
      setConfiguration(new BaseConfiguration());
    }
  }
}