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
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConversionException;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link com.celements.configuration.SpacePreferencesConfigurationSource}.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class DocumentConfigurationSourceTest extends AbstractBaseComponentTest {

  private ConfigurationSource source;

  @Before
  public void prepare() throws Exception {
    registerComponentMocks(DocumentAccessBridge.class);
    source = getComponentManager().lookup(ConfigurationSource.class, "space");
    expect(getMock(DocumentAccessBridge.class).getCurrentDocumentReference())
        .andReturn(new DocumentReference("wiki", "space", "doc")).anyTimes();
  }

  @Test
  public void test_getProperty_null() throws Exception {
    expectProp("key", null);

    replayDefault();
    assertNull(source.getProperty("key"));
    assertEquals("default", source.getProperty("key", "default"));
    assertNull(source.getProperty("key", String.class));
    assertTrue(source.getProperty("key", List.class).isEmpty());
    assertFalse(source.get("key", String.class).isPresent());
    assertFalse(source.stream("key", String.class).findAny().isPresent());
    verifyDefault();
  }

  @Test
  public void test_getProperty_String() throws Exception {
    String value = "value";
    expectProp("key", value);

    replayDefault();
    assertEquals(value, source.getProperty("key"));
    assertEquals(value, source.getProperty("key", ""));
    assertEquals(value, source.getProperty("key", String.class));
    assertEquals(value, source.get("key", String.class).orElse(null));
    assertEquals(Arrays.asList(value), source.stream("key", String.class).collect(toList()));
    verifyDefault();
  }

  @Test
  public void test_getProperty_String_empty() throws Exception {
    expectProp("key", " ");

    replayDefault();
    assertNull(source.getProperty("key"));
    assertEquals("default", source.getProperty("key", "default"));
    assertNull(source.getProperty("key", String.class));
    assertFalse(source.get("key", String.class).isPresent());
    assertFalse(source.stream("key", String.class).findAny().isPresent());
    verifyDefault();
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_string_conversionError() {
    expectProp("key", "value");

    replayDefault();
    source.getProperty("key", Boolean.class);
    verifyDefault();
  }

  @Test
  public void test_getProperty_bool() {
    Boolean value = true;
    expectProp("key", value);

    replayDefault();
    assertEquals(value, source.getProperty("key"));
    assertEquals(value, source.getProperty("key", Boolean.class));
    assertEquals(value, source.get("key", Boolean.class).orElse(null));
    assertEquals(Arrays.asList(value), source.stream("key", Boolean.class).collect(toList()));
    verifyDefault();
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_bool_conversionError() {
    expectProp("key", true);

    replayDefault();
    source.getProperty("key", String.class);
    verifyDefault();
  }

  @Test
  public void test_getProperty_int() {
    Integer value = 5;
    expectProp("key", value);

    replayDefault();
    assertEquals(value, source.getProperty("key"));
    assertEquals(value, source.getProperty("key", Integer.class));
    assertEquals(value, source.get("key", Integer.class).orElse(null));
    assertEquals(Arrays.asList(value), source.stream("key", Integer.class).collect(toList()));
    verifyDefault();
  }

  @Test(expected = ConversionException.class)
  public void test_getProperty_int_conversionError() {
    expectProp("key", 5);

    replayDefault();
    source.getProperty("key", List.class);
    verifyDefault();
  }

  @Test
  public void test_getProperty_list() {
    List<String> expected = Arrays.asList("value1", "value2");
    expectProp("key", expected);

    replayDefault();
    assertEquals(expected, source.getProperty("key"));
    assertEquals(expected, source.getProperty("key", List.class));
    assertEquals(expected, source.get("key", List.class).orElse(null));
    assertEquals(expected, source.stream("key", String.class).collect(toList()));
    verifyDefault();
  }

  private void expectProp(String key, Object value) {
    DocumentReference docRef = new DocumentReference("wiki", "space", "WebPreferences");
    DocumentReference classDocRef = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
    expect(getMock(DocumentAccessBridge.class).getProperty(docRef, classDocRef, key))
        .andReturn(value).atLeastOnce();
  }
}
