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

import java.time.Duration;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.execution.XWikiExecutionProp;
import com.celements.init.XWikiProvider;
import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Native component test base for the xwiki-core module, which cannot depend on shared tests.
 */
public abstract class AbstractComponentTest extends AbstractBaseComponentTest {

  private XWikiContext context;

  @Override
  protected void beforeSpringContextRefresh(ConfigurableApplicationContext context) {
    context.getBeanFactory().registerSingleton(MockServletContext.class.getName(),
        new MockServletContext(context));
  }

  @Before
  public final void setUpXWiki() throws Exception {
    // Statically store the component manager in {@link Utils} to be able to access it without
    // the context.
    Utils.setComponentManager(getComponentManager());

    ExecutionContext execCtx = new ExecutionContext();
    getComponentManager().lookup(Execution.class).setContext(execCtx);
    execCtx.set(XWikiExecutionProp.WIKI, XWikiConstant.MAIN_WIKI);

    // Bridge with old XWiki Context, required for old code.
    context = new XWikiContext();
    execCtx.set(XWikiExecutionProp.XWIKI_CONTEXT, context);
    context.setDatabase(XWikiConstant.MAIN_WIKI.getName());
    context.setOriginalDatabase(XWikiConstant.MAIN_WIKI.getName());
    context.setWiki(createDefaultMock(XWiki.class));

    // We need to initialize the Component Manager so that the components can be looked up
    getContext().put(ComponentManager.class.getName(), getComponentManager());

    XWikiStubContextProvider ctxProviderMock = registerComponentMock(
        XWikiStubContextProvider.class);
    expect(ctxProviderMock.createStubContext(same(execCtx))).andReturn(context).anyTimes();

    XWikiProvider xwikiProviderMock = registerComponentMock(XWikiProvider.class);
    expect(xwikiProviderMock.get()).andReturn(Optional.empty()).anyTimes();
    expect(xwikiProviderMock.await(anyObject(Duration.class))).andReturn(getWikiMock()).anyTimes();

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
