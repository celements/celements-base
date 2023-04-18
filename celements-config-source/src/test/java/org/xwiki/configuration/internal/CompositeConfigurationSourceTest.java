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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.properties.ConverterManager;

import com.celements.common.test.AbstractBaseComponentTest;
import com.google.common.collect.ImmutableList;

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
  public void prepare() throws Exception {
    ConverterManager converterManager = getComponentManager().lookup(ConverterManager.class);
    CommonsConfigurationSource source1 = new CommonsConfigurationSource();
    ReflectionUtils.setFieldValue(source1, "converterManager", converterManager);
    config1 = new BaseConfiguration();
    ReflectionUtils.setFieldValue(source1, "configuration", config1);
    CommonsConfigurationSource source2 = new CommonsConfigurationSource();
    ReflectionUtils.setFieldValue(source2, "converterManager", converterManager);
    config2 = new BaseConfiguration();
    ReflectionUtils.setFieldValue(source2, "configuration", config2);
    composite = new CompositeConfigurationSource(ImmutableList.of(source1, source2));
  }

  // TODO test stream for single and list values -> should get from all composites

  // TODO test get for single and list values -> should get from first composites
  // TODO test get(Property) for empty overwrites, e.g. c2 has "key=value" but c1 has "key="

  @Test
  public void testContainsKey() {
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
  public void testGetProperty() {
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
  public void testGetPropertyWithClass() {
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
  public void testGetPropertyWithDefaultValue() {
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
  public void testGetKeys() {
    config1.setProperty("key1", "value1");
    config2.setProperty("key2", "value2");

    List<String> expected = Arrays.asList("key1", "key2");
    assertEquals(expected, composite.getKeys());
  }

  @Test
  public void testIsEmpty() {
    assertTrue(composite.isEmpty());

    config2.setProperty("key", "value");
    assertFalse(composite.isEmpty());
  }

  @Test
  public void testGetPropertiesAndListsWhenEmpty() {
    assertTrue(composite.getProperty("unknown", Properties.class).isEmpty());
    assertTrue(composite.getProperty("unknown", List.class).isEmpty());
  }

  @Test
  public void testTypeConversionsWhenDefaultValuesAreNotUsed() {
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
}
