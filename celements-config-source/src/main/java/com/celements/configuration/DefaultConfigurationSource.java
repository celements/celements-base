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
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;

import com.google.common.collect.ImmutableList;

/**
 * Composite Configuration Source that looks in the following sources in that order:
 * <ul>
 * <li>space preferences wiki page</li>
 * <li>wiki preferences wiki page</li>
 * <li>properties files (xwiki/celements.properties)</li>
 * </ul>
 * Should be used when a configuration should not be overridden by the user in his/her
 * profile (in which case the {@link AllConfigurationSource} should be used.
 */
@Component
public class DefaultConfigurationSource extends CompositeConfigurationSource {

  @Requirement(FromWikiConfigurationSource.NAME)
  ConfigurationSource fromWikiSource;

  @Requirement("space")
  ConfigurationSource spacePreferencesSource;

  @Inject
  public DefaultConfigurationSource(
      @Named(FromWikiConfigurationSource.NAME) ConfigurationSource fromWikiSrc,
      @Named("space") ConfigurationSource spacePrefSrc) {
    super(ImmutableList.of(spacePrefSrc, fromWikiSrc));
  }
}
