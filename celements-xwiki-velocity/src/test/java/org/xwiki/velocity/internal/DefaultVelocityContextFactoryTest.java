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
package org.xwiki.velocity.internal;

import java.util.Arrays;
import java.util.Properties;

import static org.easymock.EasyMock.*;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.tools.generic.ListTool;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityContextInitializer;

import com.celements.velocity.test.AbstractComponentTest;

/**
 * Unit tests for {@link DefaultVelocityContextFactory}.
 *
 * @version $Id$
 */
public class DefaultVelocityContextFactoryTest extends AbstractComponentTest {

  private DefaultVelocityContextFactory factory;

  @Before
  public void prepareTest() throws Exception {
    VelocityConfiguration configuration = registerComponentMock(VelocityConfiguration.class);
    VelocityContextInitializer initializer = registerComponentMock(
        VelocityContextInitializer.class);
    Properties properties = new Properties();
    properties.put("listtool", ListTool.class.getName());
    expect(configuration.getTools()).andReturn(properties);
    initializer.initialize(anyObject(VelocityContext.class));
    expectLastCall().times(2);
    replayDefault();
    factory = getBeanFactory().getBean(DefaultVelocityContextFactory.class);
  }

  /**
   * Verify that we get different contexts when we call the createContext method but that they
   * contain the same
   * references to the Velocity tools. Also tests that objects we put in one context are not shared
   * with other
   * contexts. Also verifies that Velocity Context Initializers are called.
   */
  @Test
  public void test_createDifferentContext() throws Exception {
    VelocityContext context1 = this.factory.createContext();
    context1.put("param", "value");
    VelocityContext context2 = this.factory.createContext();

    Assert.assertNotSame(context1, context2);
    Assert.assertNotNull(context1.get("listtool"));
    Assert.assertSame(context2.get("listtool"), context1.get("listtool"));
    Assert.assertNull(context2.get("param"));
    verifyDefault();
  }
}
