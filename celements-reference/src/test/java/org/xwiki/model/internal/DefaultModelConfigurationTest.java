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

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link DefaultModelConfiguration}.
 *
 * @version $Id: cc01c66f29059826583ab02bfcac83039597b5aa $
 * @since 2.2M1
 */
public class DefaultModelConfigurationTest extends AbstractBaseComponentTest {

  private ModelConfiguration configuration;

  private ConfigurationSource source;

  @Before
  public void prepareTest() throws Exception {
    source = registerComponentMock(ConfigurationSource.class, "xwikiproperties");
    configuration = getBeanFactory().getBean(DefaultModelConfiguration.class);
  }

  @Test
  public void test_getDefaultReferenceName_whenDefinedInConfiguration() {
    expect(source.getProperty(eq("model.reference.default.wiki"), anyString()))
        .andReturn("defaultWiki");
    expect(source.getProperty(eq("model.reference.default.document"), anyString()))
        .andReturn("defaultDocument");
    expect(source.getProperty(eq("model.reference.default.space"), anyString()))
        .andReturn("defaultSpace");
    expect(source.getProperty(eq("model.reference.default.attachment"), anyString()))
        .andReturn("defaultFilename");
    expect(source.getProperty(eq("model.reference.default.object"), anyString()))
        .andReturn("defaultObject");
    expect(source.getProperty(eq("model.reference.default.object_property"), anyString()))
        .andReturn("defaultProperty");

    replayDefault();

    assertEquals("defaultWiki",
        this.configuration.getDefaultReferenceValue(EntityType.WIKI));
    assertEquals("defaultDocument",
        this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT));
    assertEquals("defaultSpace",
        this.configuration.getDefaultReferenceValue(EntityType.SPACE));
    assertEquals("defaultFilename",
        this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT));
    assertEquals("defaultObject",
        this.configuration.getDefaultReferenceValue(EntityType.OBJECT));
    assertEquals("defaultProperty",
        this.configuration.getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    verifyDefault();
  }

  @Test
  public void test_getDefaultReferenceName_whenNotDefinedInConfiguration() {
    expect(source.getProperty("model.reference.default.wiki", "xwiki")).andReturn("xwiki");
    expect(source.getProperty("model.reference.default.document", "WebHome"))
        .andReturn("WebHome");
    expect(source.getProperty("model.reference.default.space", "Main")).andReturn("Main");
    expect(source.getProperty("model.reference.default.attachment", "filename"))
        .andReturn("filename");
    expect(source.getProperty("model.reference.default.object", "object"))
        .andReturn("Main.WebHome");
    expect(source.getProperty("model.reference.default.object_property", "property"))
        .andReturn("property");

    replayDefault();

    assertEquals("xwiki", this.configuration.getDefaultReferenceValue(EntityType.WIKI));
    assertEquals("WebHome",
        this.configuration.getDefaultReferenceValue(EntityType.DOCUMENT));
    assertEquals("Main", this.configuration.getDefaultReferenceValue(EntityType.SPACE));
    assertEquals("filename",
        this.configuration.getDefaultReferenceValue(EntityType.ATTACHMENT));
    assertEquals("Main.WebHome", configuration.getDefaultReferenceValue(EntityType.OBJECT));
    assertEquals("property",
        configuration.getDefaultReferenceValue(EntityType.OBJECT_PROPERTY));
    verifyDefault();
  }
}
