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

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConversionException;
import org.xwiki.properties.ConverterManager;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.configuration.CommonsConfigurationSource;

/**
 * Unit tests for {@link CommonsConfigurationSource}.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class CommonsConfigurationSourceTest extends AbstractBaseComponentTest {

  private Configuration configuration;

  private CommonsConfigurationSource source;

  @Before
  public void prepare() throws Exception {
    source = new CommonsConfigurationSource(configuration = new BaseConfiguration(),
        getComponentManager().lookup(ConverterManager.class));
  }

  @Test
  public void test_getProperty_defaultValue() {
    assertEquals("default", source.getProperty("key", "default"));
    configuration.setProperty("key", "value");
    assertEquals("value", source.getProperty("key", "default"));
  }

  @Test
  public void test_getProperty_string() {
    assertNull(source.getProperty("key"));
    assertNull(source.getProperty("key", String.class));
    assertFalse(source.get("key", String.class).isPresent());
    assertFalse(source.stream("key", String.class).findAny().isPresent());
    configuration.setProperty("key", "value");
    assertEquals("value", source.getProperty("key"));
    assertEquals("value", source.getProperty("key", String.class));
    assertEquals("value", source.get("key", String.class).orElse(null));
    assertEquals(Arrays.asList("value"), source.stream("key", String.class).collect(toList()));
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_string_conversionError() {
    configuration.setProperty("key", "value");
    source.getProperty("key", Boolean.class);
  }

  @Test
  public void test_getProperty_bool() {
    Boolean value = true;
    assertNull(source.getProperty("key", Boolean.class));
    assertFalse(source.get("key", Boolean.class).isPresent());
    assertFalse(source.stream("key", Boolean.class).findAny().isPresent());
    configuration.setProperty("key", value);
    assertEquals(value, source.getProperty("key"));
    assertEquals(value, source.getProperty("key", Boolean.class));
    assertEquals(value, source.get("key", Boolean.class).orElse(null));
    assertEquals(Arrays.asList(value), source.stream("key", Boolean.class).collect(toList()));
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_bool_conversionError() {
    configuration.setProperty("key", true);
    source.getProperty("key", String.class);
  }

  @Test
  public void test_getProperty_int() {
    Integer value = 5;
    assertNull(source.getProperty("key", Integer.class));
    assertFalse(source.get("key", Integer.class).isPresent());
    assertFalse(source.stream("key", Integer.class).findAny().isPresent());
    configuration.setProperty("key", value);
    assertEquals(value, source.getProperty("key"));
    assertEquals(value, source.getProperty("key", Integer.class));
    assertEquals(value, source.get("key", Integer.class).orElse(null));
    assertEquals(Arrays.asList(value), source.stream("key", Integer.class).collect(toList()));
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_int_conversionError() {
    configuration.setProperty("key", 5);
    source.getProperty("key", List.class);
  }

  @Test
  public void test_getProperty_list() {
    assertTrue(source.getProperty("key", List.class).isEmpty());
    assertFalse(source.get("key", List.class).isPresent());
    assertFalse(source.stream("key", List.class).findAny().isPresent());
    configuration.setProperty("key", "value1");
    configuration.addProperty("key", "value2");
    List<String> expected = Arrays.asList("value1", "value2");

    assertEquals(expected, source.getProperty("key"));
    assertEquals(expected, source.getProperty("key", List.class));
    assertEquals(expected, source.get("key", List.class).orElse(null));
    assertEquals(expected, source.stream("key", String.class).collect(toList()));
  }

  @Test
  public void test_getProperty_list_defaultValue() {
    configuration.setProperty("key", "value");
    assertEquals(Arrays.asList("value"), source.getProperty("key", Arrays.asList("default")));
  }

  @Test
  public void test_getProperty_properties() {
    assertTrue(source.getProperty("key", Properties.class).isEmpty());
    configuration.setProperty("key", "key1=value1");
    configuration.addProperty("key", "key2=value2");
    List<String> expectedList = Arrays.asList("key1=value1", "key2=value2");
    Properties expectedProperties = new Properties();
    expectedProperties.put("key1", "value1");
    expectedProperties.put("key2", "value2");
    assertEquals(expectedList, source.getProperty("key"));
    assertEquals(expectedProperties, source.getProperty("key", Properties.class));
    assertEquals(expectedList, source.get("key", null).orElse(null));
  }

  @Test
  public void test_getKeys() {
    assertTrue(source.getKeys().isEmpty());
    configuration.setProperty("key1", "value1");
    configuration.setProperty("key2", "value2");
    assertEquals(Arrays.asList("key1", "key2"), source.getKeys());
  }

  @Test
  public void test_isEmpty() {
    assertTrue(source.isEmpty());
    configuration.setProperty("key", "value");
    assertFalse(source.isEmpty());
  }
}
