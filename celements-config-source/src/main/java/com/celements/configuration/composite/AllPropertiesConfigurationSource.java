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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.XWikiPropertiesConfigurationSource;

import com.google.common.collect.ImmutableList;

/**
 * Composite Configuration Source that looks in the following sources in that order:
 * <ul>
 * <li>celements properties file (celements.properties)</li>
 * <li>xwiki properties file (xwiki.properties)</li>
 * </ul>
 * Should be used when a configuration should not be overriden by wiki/space preferences (in which
 * case {@link DefaultConfigurationSource} or {@link FromWikiConfigurationSource} should be
 * used.
 */
@Component(PropertiesConfigurationSource.NAME)
public class PropertiesConfigurationSource extends CompositeConfigurationSource {

  public static final String NAME = "properties";

  @Inject
  public PropertiesConfigurationSource(
      @Named(XWikiPropertiesConfigurationSource.NAME) ConfigurationSource xwikiPropSrc,
      @Named(CelementsPropertiesConfigurationSource.NAME) ConfigurationSource celPropSrc) {
    super(ImmutableList.of(celPropSrc, xwikiPropSrc));
  }
}