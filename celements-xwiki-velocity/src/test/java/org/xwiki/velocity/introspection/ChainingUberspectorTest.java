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
package org.xwiki.velocity.introspection;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.util.introspection.SecureUberspector;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.internal.DefaultVelocityEngine;

import com.celements.velocity.test.AbstractComponentTest;

/**
 * Unit tests for {@link ChainingUberspector}.
 */
public class ChainingUberspectorTest extends AbstractComponentTest {

  private DefaultVelocityEngine engine;

  @Before
  public void prepareTest() throws Exception {
    VelocityConfiguration configuration = registerComponentMock(VelocityConfiguration.class);
    expect(configuration.getProperties()).andReturn(new Properties());
    engine = getBeanFactory().getBean(DefaultVelocityEngine.class);
    replayDefault();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  /*
   * Tests that the uberspectors in the chain are called, and without a real uberspector no methods
   * are found.
   */
  @Test
  public void test_emptyChain() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        TestingUberspector.class.getCanonicalName());
    TestingUberspector.methodCalls = 0;
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
    assertEquals("$bar", writer.toString());
    assertEquals(1, TestingUberspector.methodCalls);
  }

  /*
   * Tests that using several uberspectors in the chain works, and methods are correctly found by
   * the last uberspector
   * in the chain.
   */
  @Test
  public void test_basicChaining() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        UberspectImpl.class.getCanonicalName() + "," + TestingUberspector.class.getCanonicalName());
    TestingUberspector.methodCalls = 0;
    TestingUberspector.getterCalls = 0;
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
    assertEquals("hello", writer.toString());
    assertEquals(1, TestingUberspector.methodCalls);
    assertEquals(0, TestingUberspector.getterCalls);
  }

  /*
   * Tests that invalid uberspectors classnames are ignored.
   */
  @Test
  public void test_invalidUberspectorsAreIgnored() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        UberspectImpl.class.getCanonicalName() + ","
            + AbstractChainableUberspector.class.getCanonicalName()
            + "," + InvalidUberspector.class.getCanonicalName() + ","
            + TestingUberspector.class.getCanonicalName() + "," + Date.class.getCanonicalName());
    TestingUberspector.methodCalls = 0;
    InvalidUberspector.methodCalls = 0;
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
    assertEquals("hello", writer.toString());
    assertEquals(1, TestingUberspector.methodCalls);
    assertEquals(0, InvalidUberspector.methodCalls);
  }

  /*
   * Tests that a non-chainable entry in the chain does not forward calls.
   */
  @Test
  public void test_chainBreakingOnNonChainableEntry() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        TestingUberspector.class.getCanonicalName() + "," + UberspectImpl.class.getCanonicalName());
    TestingUberspector.methodCalls = 0;
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader("#set($foo = 'hello')#set($bar = $foo.toString())$bar"));
    assertEquals("hello", writer.toString());
    assertEquals(0, TestingUberspector.methodCalls);
  }

  /*
   * Checks that the default (non-secure) uberspector works and allows calling restricted methods.
   */
  @Test
  public void test_defaultUberspectorWorks() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        UberspectImpl.class.getCanonicalName());
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader(
            "#set($foo = 'hello')" + "#set($bar = $foo.getClass().getConstructors())$bar"));
    assertTrue(writer.toString().startsWith("[Ljava.lang.reflect.Constructor"));
  }

  /*
   * Checks that the secure uberspector works and does not allow calling restricted methods.
   */
  @Test
  public void test_secureUberspectorWorks() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        SecureUberspector.class.getCanonicalName());
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader(
            "#set($foo = 'hello')" + "#set($bar = $foo.getClass().getConstructors())$foo$bar"));
    assertEquals("hello$bar", writer.toString());
  }

  /*
   * Checks that when the chain property is not configured, by default the secure ubespector is
   * used.
   */
  @Test
  public void test_secureUberspectorEnabledByDefault() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES, "");
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    engine.evaluate(new org.apache.velocity.VelocityContext(), writer, "mytemplate",
        new StringReader(
            "#set($foo = 'hello')" + "#set($bar = $foo.getClass().getConstructors())$foo$bar"));
    assertEquals("hello$bar", writer.toString());
  }

  /*
   * Checks that the deprecated check uberspector works.
   */
  @SuppressWarnings("deprecation")
  @Test
  public void test_deprecatedUberspector() throws Exception {
    Properties prop = new Properties();
    prop.setProperty(RuntimeConstants.UBERSPECT_CLASSNAME,
        ChainingUberspector.class.getCanonicalName());
    prop.setProperty(ChainingUberspector.UBERSPECT_CHAIN_CLASSNAMES,
        UberspectImpl.class.getCanonicalName() + "," + TestingUberspector.class.getCanonicalName()
            + ","
            + DeprecatedCheckUberspector.class.getCanonicalName());
    TestingUberspector.methodCalls = 0;
    TestingUberspector.getterCalls = 0;
    engine.initialize(prop);
    StringWriter writer = new StringWriter();
    VelocityContext context = new org.apache.velocity.VelocityContext();
    Date d = new Date();
    context.put("date", d);

    engine.evaluate(context, writer, "mytemplate",
        new StringReader("#set($foo = $date.getYear())$foo $date.month"));

    assertEquals(d.getYear() + " " + d.getMonth(), writer.toString());
    assertEquals(1, TestingUberspector.methodCalls);
    assertEquals(1, TestingUberspector.getterCalls);
  }
}
