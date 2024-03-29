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
package com.xpn.xwiki.test;

import java.util.Optional;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.AbstractComponentTestCase;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Same as {@link com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase} but for JUnit 4.x and
 * JMock 2.x.
 *
 * @version $Id$
 * @since 2.2M2
 * @deprecated instead use {@link AbstractComponentTest}
 */
@Deprecated
public abstract class AbstractBridgedComponentTestCase extends AbstractComponentTestCase {

  private XWikiContext context;

  private Mockery mockery = new Mockery() {

    {
      // Used to be able to mock class instances (and not only interfaces).
      setImposteriser(ClassImposteriser.INSTANCE);
    }
  };

  @Override
  @Before
  public void setUp() throws Exception {
    this.context = new XWikiContext();
    this.context.setDatabase("xwiki");
    this.context.setMainXWiki("xwiki");
    super.setUp();

    // Statically store the component manager in {@link Utils} to be able to access it without
    // the context.
    Utils.setComponentManager(getComponentManager());

    // We need to initialize the Component Manager so that the components can be looked up
    getContext().put(ComponentManager.class.getName(), getComponentManager());

    // Bridge with old XWiki Context, required for old code.
    Execution execution = getComponentManager().lookup(Execution.class);
    execution.getContext().setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);

    // Set a simple application context, as some components fail to start without one.
    Container c = getComponentManager().lookup(Container.class);
    c.setApplicationContext(new TestApplicationContext());

    final CoreConfiguration mockCoreConfiguration = registerMockComponent(CoreConfiguration.class);
    this.mockery.checking(new Expectations() {

      {
        allowing(mockCoreConfiguration).getDefaultDocumentSyntax();
        will(returnValue(Syntax.XWIKI_1_0));
      }
    });
  }

  @Override
  protected void registerComponents() throws Exception {
    final XWikiStubContextProvider ctxProviderMock = registerMockComponent(
        XWikiStubContextProvider.class);
    this.mockery.checking(new Expectations() {

      {
        allowing(ctxProviderMock).createStubContext(with(any(ExecutionContext.class)));
        will(returnValue(context));
      }
    });
    final XWikiProvider xwikiProviderMock = registerMockComponent(XWikiProvider.class);
    this.mockery.checking(new Expectations() {

      {
        allowing(xwikiProviderMock).get();
        will(returnValue(Optional.empty()));
      }
    });
  }

  @Override
  @After
  public void tearDown() throws Exception {
    Utils.setComponentManager(null);
    super.tearDown();
  }

  public XWikiContext getContext() {
    return this.context;
  }

  @Override
  public Mockery getMockery() {
    return this.mockery;
  }
}
