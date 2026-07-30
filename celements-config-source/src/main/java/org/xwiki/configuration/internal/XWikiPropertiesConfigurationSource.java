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

import javax.inject.Inject;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

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

  @Inject
  private ResourceLoader resourceLoader;

  /**
   * {@inheritDoc}
   *
   * @see Initializable#initialize()
   */
  @Override
  public void initialize() throws InitializationException {
    Configuration config;
    try {
      URL propertiesURL = resourceLoader.getResource(XWIKI_PROPERTIES_FILE).getURL();
      config = new PropertiesConfiguration(propertiesURL);
    } catch (Exception exc) {
      LOGGER.warn("Failed to load configuration file '{}'", XWIKI_PROPERTIES_FILE, exc);
      config = new BaseConfiguration();
    }
    setConfiguration(config);
  }
}
