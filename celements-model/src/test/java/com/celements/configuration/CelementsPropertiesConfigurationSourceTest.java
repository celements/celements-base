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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ComponentList;
import com.celements.common.test.HintedComponent;
import com.xpn.xwiki.web.Utils;

@ComponentList({ @HintedComponent(clazz = ConfigurationSource.class,
    hint = "celementsproperties") })
public class CelementsPropertiesConfigurationSourceTest extends AbstractComponentTest {

  @Before
  public void prepareTest() throws Exception {
    DefaultComponentDescriptor<ConfigurationSource> descriptor = new DefaultComponentDescriptor<>();
    descriptor.setRole(ConfigurationSource.class);
    descriptor.setRoleHint(CelementsPropertiesConfigurationSource.NAME);
    descriptor.setImplementation(CelementsPropertiesConfigurationSource.class);
    getComponentManager().unregisterComponent(descriptor.getRole(), descriptor.getRoleHint());
    getComponentManager().registerComponent(descriptor);
  }

  @Test
  public void test_initialize() throws Exception {
    registerComponentMock(ResourceLoader.class);
    String name = CelementsPropertiesConfigurationSource.CELEMENTS_PROPERTIES_FILE;
    Resource resourceMock = createDefaultMock(Resource.class);
    expect(getMock(ResourceLoader.class).getResource(eq(name))).andReturn(resourceMock);
    expect(resourceMock.getURL()).andThrow(new IOException());

    replayDefault();
    assertSame(CelementsPropertiesConfigurationSource.class, Utils.getComponent(
        ConfigurationSource.class, "celementsproperties").getClass());
    verifyDefault();
  }

}
