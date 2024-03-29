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

import org.jmock.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.context.Execution;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.AbstractXWikiComponentTestCase;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;

/**
 * Extension of {@link org.xwiki.test.AbstractXWikiComponentTestCase} that sets up a bridge between
 * the new Execution Context and the old XWikiContext. This allows code that uses XWikiContext to be
 * tested using this Test Case class.
 *
 * @version $Id$
 * @since 1.6M1
 * @deprecated use JUnit 4+ and {@link AbstractComponentTest}
 */
@Deprecated
public abstract class AbstractBridgedXWikiComponentTestCase extends AbstractXWikiComponentTestCase {

  private XWikiContext context;

  @Override
  protected void setUp() throws Exception {
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

    Mock mockCoreConfiguration = registerMockComponent(CoreConfiguration.class);
    mockCoreConfiguration.stubs().method("getDefaultDocumentSyntax")
        .will(returnValue(Syntax.XWIKI_1_0));
  }

  @Override
  protected void registerComponents() throws Exception {
    Mock ctxProviderMock = registerMockComponent(XWikiStubContextProvider.class, "default");
    ctxProviderMock.stubs().method("createStubContext").withAnyArguments()
        .will(returnValue(context));
  }

  @Override
  protected void tearDown() throws Exception {
    Utils.setComponentManager(null);
    super.tearDown();
  }

  public XWikiContext getContext() {
    return this.context;
  }
}
