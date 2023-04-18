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

import org.apache.commons.configuration.ConfigurationException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * Looks for configuration data in {@code /WEB-INF/xwiki.properties}.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component(XWikiPropertiesConfigurationSource.NAME)
public class XWikiPropertiesConfigurationSource extends CommonsConfigurationSource
    implements Initializable {

  public static final String NAME = "xwikiproperties";

  @Override
  public void initialize() throws InitializationException {
    try {
      setConfiguration("xwiki");
    } catch (ConfigurationException exc) {
      throw new InitializationException("failed setting config", exc);
    }
  }
}
