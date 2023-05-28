package com.celements.init;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.celements.servlet.CelementsLifecycleEvent;
import com.xpn.xwiki.ServerUrlUtilsRole;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiServletContext;

@Component
public class XWikiBootstrap implements ApplicationListener<CelementsLifecycleEvent>, Ordered {

  protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiBootstrap.class);

  private final ServletContext servletContext;
  private final ServerUrlUtilsRole serverUrlUtils;
  private final Execution execution;
  private final ExecutionContextManager executionManager;
  private final ComponentManager componentManager;
  private final XWikiStubContextProvider stubContextProvider;

  @Inject
  public XWikiBootstrap(
      ServletContext servletContext,
      ServerUrlUtilsRole serverUrlUtils,
      Execution execution,
      ExecutionContextManager executionManager,
      ComponentManager componentManager,
      XWikiStubContextProvider stubContextProvider) {
    this.servletContext = servletContext;
    this.serverUrlUtils = serverUrlUtils;
    this.execution = execution;
    this.executionManager = executionManager;
    this.componentManager = componentManager;
    this.stubContextProvider = stubContextProvider;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    if (event.getType() == CelementsLifecycleEvent.State.STARTED) {
      try {
        bootstrapXWiki();
      } catch (Exception exc) {
        throw new XWikiBootstrapException(exc);
      }
    }
  }

  private void bootstrapXWiki() throws XWikiException, IOException, ExecutionContextException {
    XWikiConfig xwikiCfg = loadXWikiConfig();
    XWikiContext xwikiContext = createInitialXWikiContext(xwikiCfg);
    initExecutionContext(xwikiContext);
    Utils.setComponentManager(componentManager);
    XWiki xwiki = createXWiki(xwikiCfg, xwikiContext);
    // TODO requires XWiki ? Cfg should suffice
    xwikiContext.setURLFactory(xwiki.getURLFactoryService().createURLFactory(xwikiContext));
    stubContextProvider.initialize(xwikiContext);
    publishXWikiInstance(xwiki);
  }

  private XWikiContext createInitialXWikiContext(XWikiConfig xwikiCfg)
      throws MalformedURLException {
    XWikiContext ctx = new XWikiContext();
    ctx.setMode(XWikiContext.MODE_SERVLET);
    ctx.setEngineContext(new XWikiServletContext(servletContext));
    ctx.setMainXWiki(XWikiConstant.MAIN_WIKI.getName());
    ctx.setDatabase(XWikiConstant.MAIN_WIKI.getName());
    ctx.setURL(serverUrlUtils.getServerURL(xwikiCfg::getProperty));
    return ctx;
  }

  private XWiki createXWiki(XWikiConfig cfg, XWikiContext context) throws XWikiException {
    // TODO dive into this and move it here
    XWiki xwiki = new XWiki(cfg, context, context.getEngineContext(), true);
    xwiki.setDatabase(context.getDatabase());
    context.setWiki(xwiki);
    return xwiki;
  }

  public void initExecutionContext(XWikiContext xwikiContext) throws ExecutionContextException {
    ExecutionContext executionCtx = new ExecutionContext();
    execution.setContext(executionCtx);
    executionManager.initialize(executionCtx);
    executionCtx.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, xwikiContext);
  }

  private XWikiConfig loadXWikiConfig() throws IOException, XWikiException {
    try (InputStream is = servletContext.getResourceAsStream("/WEB-INF/xwiki.cfg")) {
      return new XWikiConfig(is);
    }
  }

  /**
   * make XWiki available to all requests, see {@link XWiki#getXWiki(XWikiContext)}.
   */
  private void publishXWikiInstance(XWiki xwiki) {
    servletContext.setAttribute(XWikiEngineContext.XWIKI_KEY, xwiki);
    LOGGER.info("XWiki started");
  }

  public class XWikiBootstrapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    XWikiBootstrapException(Throwable cause) {
      super(cause);
    }

  }

}