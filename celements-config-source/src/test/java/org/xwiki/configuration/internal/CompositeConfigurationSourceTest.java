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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConversionException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link CompositeConfigurationSource}.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class CompositeConfigurationSourceTest extends AbstractBaseComponentTest {

  private CompositeConfigurationSource composite;

  private Configuration config1;

  private Configuration config2;

  @Before
  public void setUp() throws Exception {
    composite = new CompositeConfigurationSource();
    ConverterManager converterManager = getComponentManager().lookup(ConverterManager.class);

    CommonsConfigurationSource source1 = new CommonsConfigurationSource();
    ReflectionUtils.setFieldValue(source1, "converterManager", converterManager);
    config1 = new BaseConfiguration();
    source1.setConfiguration(config1);
    composite.addConfigurationSource(source1);

    CommonsConfigurationSource source2 = new CommonsConfigurationSource();
    ReflectionUtils.setFieldValue(source2, "converterManager", converterManager);
    config2 = new BaseConfiguration();
    source2.setConfiguration(config2);
    composite.addConfigurationSource(source2);
  }

  @Test
  public void test_containsKey_presentAndMissing() {
    config1.setProperty("key1", "value1");
    config1.setProperty("key3", "value3");
    config2.setProperty("key2", "value2");
    config2.setProperty("key3", "value3");

    assertTrue(composite.containsKey("key1"));
    assertTrue(composite.containsKey("key2"));
    assertTrue(composite.containsKey("key3"));
    assertFalse(composite.containsKey("unknown"));
  }

  @Test
  public void test_getProperty_untyped() {
    config1.setProperty("key1", "value1");
    config1.setProperty("key3", "value3");
    config2.setProperty("key2", "value2");
    config2.setProperty("key3", "value3");

    assertEquals("value1", composite.getProperty("key1"));
    assertEquals("value2", composite.getProperty("key2"));
    assertEquals("value3", composite.getProperty("key3"));
    assertNull(composite.getProperty("unknown"));
  }

  @Test
  public void test_getProperty_withClass() {
    config1.setProperty("key1", "value1");
    config1.setProperty("key3", "value3");
    config2.setProperty("key2", "value2");
    config2.setProperty("key3", "value3");

    assertEquals("value1", composite.getProperty("key1", String.class));
    assertEquals("value2", composite.getProperty("key2", String.class));
    assertEquals("value3", composite.getProperty("key3", String.class));
    assertNull(composite.getProperty("unknown", String.class));
  }

  @Test
  public void test_getProperty_withDefaultValue() {
    config1.setProperty("key1", "value1");
    config1.setProperty("key3", "value3");
    config2.setProperty("key2", "value2");
    config2.setProperty("key3", "value3");

    assertEquals("value1", composite.getProperty("key1", "default"));
    assertEquals("value2", composite.getProperty("key2", "default"));
    assertEquals("value3", composite.getProperty("key3", "default"));
    assertEquals("default", composite.getProperty("unknown", "default"));
  }

  @Test
  public void test_getKeys_multipleSources() {
    config1.setProperty("key1", "value1");
    config2.setProperty("key2", "value2");

    List<String> expected = Arrays.asList("key1", "key2");
    assertEquals(expected, composite.getKeys());
  }

  @Test
  public void test_isEmpty_emptyAndPopulated() {
    assertTrue(composite.isEmpty());

    config2.setProperty("key", "value");
    assertFalse(composite.isEmpty());
  }

  @Test
  public void test_getProperty_missingCollections() {
    assertTrue(composite.getProperty("unknown", Properties.class).isEmpty());
    assertTrue(composite.getProperty("unknown", List.class).isEmpty());
  }

  @Test
  public void test_getProperty_convertsConfiguredValues() {
    config1.setProperty("key1", "true");
    config1.setProperty("key2", "item1,item2");
    config1.setProperty("key3", "prop1=value1,prop2=value2");

    // Default value is not used since the property exists and is converted to boolean automatically
    assertTrue(composite.getProperty("key1", false));

    // Default value is not used since the property exists and is converted to List automatically
    assertEquals(Arrays.asList("item1", "item2"),
        composite.getProperty("key2", new ArrayList<String>()));

    // Default value is not used since the property exists and is converted to Properties
    // automatically
    Properties props = composite.getProperty("key3", new Properties());
    assertEquals("value1", props.getProperty("prop1"));
  }

  @Test
  public void test_getProperty_blankBooleanFallsThrough() {
    config1.setProperty("boolean", "");
    config2.setProperty("boolean", "false");

    assertEquals(Boolean.FALSE, composite.getProperty("boolean", Boolean.class));
    assertFalse(composite.getProperty("boolean", true));

    config1.setProperty("boolean", " \t ");
    config2.setProperty("boolean", "true");

    assertEquals(Boolean.TRUE, composite.getProperty("boolean", Boolean.class));
    assertTrue(composite.getProperty("boolean", false));
  }

  @Test
  public void test_getProperty_explicitFalseStopsLookup() {
    config1.setProperty("boolean", "false");
    config2.setProperty("boolean", "invalid");

    assertEquals(Boolean.FALSE, composite.getProperty("boolean", Boolean.class));
    assertFalse(composite.getProperty("boolean", true));
  }

  @Test
  public void test_getProperty_allBlankBooleansUseCallerDefault() {
    config1.setProperty("boolean", "");
    config2.setProperty("boolean", " ");

    assertNull(composite.getProperty("boolean", Boolean.class));
    assertTrue(composite.getProperty("boolean", true));
    assertFalse(composite.getProperty("boolean", false));
    assertTrue(composite.getProperty("unknown", true));
    assertFalse(composite.getProperty("unknown", false));
  }

  @Test
  public void test_getProperty_nestedCompositeDoesNotApplyDefaultEarly() throws Exception {
    config1.setProperty("boolean", "");
    config2.setProperty("boolean", " ");
    CompositeConfigurationSource outer = new CompositeConfigurationSource();
    outer.addConfigurationSource(composite);
    Configuration config3 = new BaseConfiguration();
    config3.setProperty("boolean", "false");
    CommonsConfigurationSource fallback = new CommonsConfigurationSource();
    fallback.setConfiguration(config3);
    ReflectionUtils.setFieldValue(fallback, "converterManager",
        getComponentManager().lookup(ConverterManager.class));
    outer.addConfigurationSource(fallback);

    assertEquals(Boolean.FALSE, outer.getProperty("boolean", Boolean.class));
    assertFalse(outer.getProperty("boolean", true));

    config3.clearProperty("boolean");

    assertNull(outer.getProperty("boolean", Boolean.class));
    assertTrue(outer.getProperty("boolean", true));
    assertFalse(outer.getProperty("boolean", false));
  }

  @Test
  public void test_getProperty_emptyWikiPreferenceStillInheritsProperties() throws Exception {
    DocumentAccessBridge bridge = registerComponentMock(DocumentAccessBridge.class);
    DocumentReference preferences = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
    expect(bridge.getCurrentWiki()).andReturn("wiki").anyTimes();
    expect(bridge.getProperty(preferences, preferences, "boolean")).andReturn("").anyTimes();
    CompositeConfigurationSource withWiki = new CompositeConfigurationSource();
    withWiki.addConfigurationSource(
        getComponentManager().lookup(ConfigurationSource.class, "wiki"));
    withWiki.addConfigurationSource(composite);
    config1.setProperty("boolean", "false");

    replayDefault();
    assertEquals(Boolean.FALSE, withWiki.getProperty("boolean", Boolean.class));
    assertFalse(withWiki.getProperty("boolean", true));
    verifyDefault();
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_invalidBooleanDoesNotFallThrough() {
    config1.setProperty("boolean", "invalid");
    config2.setProperty("boolean", "false");

    composite.getProperty("boolean", false);
  }

  @Test
  public void test_getProperty_blankStringStillOverridesLowerSource() {
    config1.setProperty("string", "");
    config2.setProperty("string", "value");

    assertEquals("", composite.getProperty("string"));
    assertEquals("", composite.getProperty("string", String.class));
    assertEquals("", composite.getProperty("string", "default"));
  }

  @Test
  public void test_getProperty_nullTypedValuesFallThrough() {
    ConfigurationSource nullable = createMock(ConfigurationSource.class);
    expect(nullable.containsKey("string")).andReturn(true).times(2);
    expect(nullable.getProperty("string", String.class)).andReturn(null).times(2);
    expect(nullable.containsKey("integer")).andReturn(true).times(2);
    expect(nullable.getProperty("integer", Integer.class)).andReturn(null).times(2);
    CompositeConfigurationSource layered = new CompositeConfigurationSource();
    layered.addConfigurationSource(nullable);
    layered.addConfigurationSource(composite);
    config1.setProperty("string", "value");
    config1.setProperty("integer", "7");

    replayDefault(nullable);
    assertEquals("value", layered.getProperty("string", String.class));
    assertEquals(Integer.valueOf(7), layered.getProperty("integer", Integer.class));
    assertEquals("value", layered.getProperty("string", "default"));
    assertEquals(Integer.valueOf(7), layered.getProperty("integer", 0));
    verifyDefault(nullable);
  }

  @Test
  public void test_getProperty_missingCollectionValuesUseCallerDefaults() {
    ArrayList<String> defaultList = new ArrayList<>(Arrays.asList("default"));
    Properties defaultProperties = new Properties();
    defaultProperties.setProperty("key", "default");
    CompositeConfigurationSource outer = new CompositeConfigurationSource();
    outer.addConfigurationSource(composite);

    assertSame(defaultList, outer.getProperty("unknown", defaultList));
    assertSame(defaultProperties, outer.getProperty("unknown", defaultProperties));
    assertTrue(outer.getProperty("unknown", List.class).isEmpty());
    assertTrue(outer.getProperty("unknown", Properties.class).isEmpty());
  }

  @Test
  public void test_getProperty_nestedNullPropertiesDoNotHideCallerDefault() {
    ConfigurationSource nullable = createMock(ConfigurationSource.class);
    expect(nullable.containsKey("properties")).andReturn(true).times(4);
    expect(nullable.getProperty("properties", Properties.class)).andReturn(null).times(2);
    CompositeConfigurationSource inner = new CompositeConfigurationSource();
    inner.addConfigurationSource(nullable);
    CompositeConfigurationSource outer = new CompositeConfigurationSource();
    outer.addConfigurationSource(inner);
    outer.addConfigurationSource(composite);
    Properties defaultProperties = new Properties();
    defaultProperties.setProperty("key", "default");

    replayDefault(nullable);
    assertSame(defaultProperties, outer.getProperty("properties", defaultProperties));
    config1.setProperty("properties", "key=configured");
    assertEquals("configured", outer.getProperty("properties", defaultProperties).getProperty("key"));
    verifyDefault(nullable);
  }

  @Test
  public void test_getProperty_nullDefaultRetainsExistingHandling() {
    assertNull(composite.getProperty("unknown", (String) null));
    config1.setProperty("string", "value");

    assertThrows(NullPointerException.class,
        () -> composite.getProperty("string", (String) null));
  }

  @Test
  public void test_getProperty_zeroStillOverridesLowerSource() {
    config1.setProperty("integer", "0");
    config2.setProperty("integer", "7");

    assertEquals(Integer.valueOf(0), composite.getProperty("integer", Integer.class));
    assertEquals(Integer.valueOf(0), composite.getProperty("integer", 7));
  }

  @Test
  public void test_getProperty_emptyCollectionsStillOverrideLowerSource() {
    ConfigurationSource empty = createMock(ConfigurationSource.class);
    expect(empty.containsKey("list")).andReturn(true);
    expect(empty.getProperty("list", List.class)).andReturn(Collections.emptyList());
    expect(empty.containsKey("properties")).andReturn(true).times(2);
    expect(empty.getProperty("properties", Properties.class)).andReturn(new Properties()).times(2);
    CompositeConfigurationSource layered = new CompositeConfigurationSource();
    layered.addConfigurationSource(empty);
    layered.addConfigurationSource(composite);
    config1.setProperty("list", "value");
    config1.setProperty("properties", "key=value");

    replayDefault(empty);
    assertTrue(layered.getProperty("list", List.class).isEmpty());
    assertTrue(layered.getProperty("properties", Properties.class).isEmpty());
    Properties defaultProperties = new Properties();
    defaultProperties.setProperty("key", "default");
    assertTrue(layered.getProperty("properties", defaultProperties).isEmpty());
    verifyDefault(empty);
  }
}
