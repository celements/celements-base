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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.core.io.ResourceLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.ConverterManager;

/**
 * Looks for configuration data in {@code /WEB-INF/xwiki.properties}.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component(XWikiPropertiesConfigurationSource.NAME)
public class XWikiPropertiesConfigurationSource extends CommonsConfigurationSource {

  public static final String NAME = "xwikiproperties";

  @Inject
  public XWikiPropertiesConfigurationSource(
      @NotNull ResourceLoader loader,
      @Nullable ConverterManager converterManager) {
    super(loadConfiguration(loader, "xwiki.properties"), converterManager);
  }
}
