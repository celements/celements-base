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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.properties.ConverterManager;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.configuration.CommonsConfigurationSource;
import com.celements.configuration.CompositeConfigurationSource;
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
    config1 = new BaseConfiguration();
    config2 = new BaseConfiguration();
    ConverterManager converterManager = getComponentManager().lookup(ConverterManager.class);
    composite = new CompositeConfigurationSource(ImmutableList.of(
        new CommonsConfigurationSource(config1, converterManager),
        new CommonsConfigurationSource(config2, converterManager)));
  }

  // TODO test stream for single and list values -> should get from all composites

  // TODO test get for single and list values -> should get from first composites
  // TODO test get(Property) for empty overwrites, e.g. c2 has "key=value" but c1 has "key="

  @Test
  public void test_containsKey() {
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
  public void test_getProperty() {
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
  public void test_getProperty_class() {
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
  public void test_getProperty_defaultValue() {
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
  public void test_getKeys() {
    config1.setProperty("key1", "value1");
    config2.setProperty("key2", "value2");

    List<String> expected = Arrays.asList("key1", "key2");
    List<String> ret = composite.getKeys();
    assertNotNull(ret);
    assertFalse(ret.isEmpty());
    assertEquals("key1", ret.get(0));
    ret.get(0);
    assertEquals(expected, ret);
  }

  @Test
  public void test_isEmpty() {
    assertTrue(composite.isEmpty());

    config2.setProperty("key", "value");
    assertFalse(composite.isEmpty());
  }

  @Test
  public void test_getPropery_defaultValues() {
    assertTrue(composite.getProperty("unknown", Properties.class).isEmpty());
    assertTrue(composite.getProperty("unknown", List.class).isEmpty());
  }

  @Test
  public void test_getProperty_typeConversionsWhenDefaultValuesAreNotUsed() {
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
