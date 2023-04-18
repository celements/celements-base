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
package com.celements.configuration;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.CompositeConfigurationSource;

/**
 * @deprecated since 6.0 instead use {@link PropertiesConfigurationSource}
 *
 */
@Deprecated
// TODO extend PropertiesConfigurationSource & move tests
public class CelementsAllPropertiesConfigurationSource extends CompositeConfigurationSource
    implements Initializable {

  public static final String NAME = "allproperties";

  @Requirement("xwikiproperties")
  ConfigurationSource xwikiPropertiesSource;

  @Requirement(CelementsPropertiesConfigurationSource.NAME)
  ConfigurationSource celementsPropertiesSource;

  @Override
  public void initialize() throws InitializationException {
    // first source is looked up first when a property value is requested.
    addConfigurationSource(this.celementsPropertiesSource);
    addConfigurationSource(this.xwikiPropertiesSource);
  }

}
