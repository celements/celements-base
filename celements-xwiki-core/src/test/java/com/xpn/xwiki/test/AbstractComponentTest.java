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

import static org.easymock.EasyMock.*;

import org.junit.After;
import org.junit.Before;
import org.springframework.mock.web.MockServletContext;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.common.test.AbstractBaseComponentTest;
import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Same as {@link com.xpn.xwiki.test.AbstractBridgedComponentTestCase} but for EasyMock.
 *
 */
public abstract class AbstractComponentTest extends AbstractBaseComponentTest {

  private XWikiContext context;

  @Override
  protected void beforeSpringContextRefresh() {
    getBeanFactory().registerSingleton(MockServletContext.class.getName(),
        new MockServletContext(getSpringContext()));
  }

  @Before
  public final void setUpXWiki() throws Exception {
    // Statically store the component manager in {@link Utils} to be able to access it without
    // the context.
    Utils.setComponentManager(getComponentManager());

    context = new XWikiContext();
    context.setDatabase("xwiki");
    context.setMainXWiki("xwiki");
    context.setWiki(createDefaultMock(XWiki.class));

    // We need to initialize the Component Manager so that the components can be looked up
    getContext().put(ComponentManager.class.getName(), getComponentManager());

    // Bridge with old XWiki Context, required for old code.
    ExecutionContext execCtx = new ExecutionContext();
    getComponentManager().lookup(Execution.class).setContext(execCtx);
    execCtx.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, context);
    XWikiStubContextProvider ctxProviderMock = registerComponentMock(
        XWikiStubContextProvider.class);
    expect(ctxProviderMock.createStubContext(same(execCtx))).andReturn(context).anyTimes();

    // Set a simple application context, as some components fail to start without one.
    getComponentManager().lookup(Container.class)
        .setApplicationContext(new TestApplicationContext());

    final CoreConfiguration mockCoreConfiguration = registerComponentMock(CoreConfiguration.class);
    expect(mockCoreConfiguration.getDefaultDocumentSyntax())
        .andReturn(Syntax.XWIKI_1_0).anyTimes();
  }

  @After
  public void tearDownXWiki() throws Exception {
    Utils.setComponentManager(null);
  }

  protected XWikiConfig getXWikiCfg() {
    return Utils.getComponent(XWikiConfigSource.class).getXWikiConfig();
  }

  public XWikiContext getContext() {
    return context;
  }

  public XWiki getWikiMock() {
    return getMock(XWiki.class);
  }

}
