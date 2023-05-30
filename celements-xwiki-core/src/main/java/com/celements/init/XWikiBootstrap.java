package com.celements.init;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.celements.wiki.WikiService;
import com.xpn.xwiki.ServerUrlUtilsRole;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;

@Component
public class XWikiBootstrap implements ApplicationListener<CelementsLifecycleEvent>, Ordered {

  public static final String XWIKI_SERVLET_CTX_KEY = "xwiki";

  protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiBootstrap.class);

  private final AtomicBoolean initialised = new AtomicBoolean(false);

  private final ServletContext servletContext;
  private final Execution execution;
  private final ExecutionContextManager executionManager;
  private final ComponentManager componentManager;
  private final ServerUrlUtilsRole serverUrlUtils;
  private final XWikiStubContextProvider stubContextProvider;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;

  @Inject
  public XWikiBootstrap(
      ServletContext servletContext,
      Execution execution,
      ExecutionContextManager executionManager,
      ComponentManager componentManager,
      ServerUrlUtilsRole serverUrlUtils,
      XWikiStubContextProvider stubContextProvider,
      WikiService wikiService,
      WikiUpdater wikiUpdater) {
    this.servletContext = servletContext;
    this.serverUrlUtils = serverUrlUtils;
    this.execution = execution;
    this.executionManager = executionManager;
    this.componentManager = componentManager;
    this.stubContextProvider = stubContextProvider;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public synchronized void onApplicationEvent(CelementsLifecycleEvent event) {
    if (event.getType() == CelementsLifecycleEvent.State.STARTED) {
      checkState(servletContext.getAttribute(XWIKI_SERVLET_CTX_KEY) == null);
      CompletableFuture<XWiki> xwikiFuture = new CompletableFuture<>();
      servletContext.setAttribute(XWIKI_SERVLET_CTX_KEY, xwikiFuture);
      try {
        XWiki xwiki = bootstrapXWiki();
        // make XWiki available to all requests via servlet context, see {@link XWiki#getXWiki}
        xwikiFuture.complete(xwiki);
        LOGGER.warn("XWiki published"); // TODO reduce
      } catch (Exception exc) {
        xwikiFuture.completeExceptionally(exc);
        throw new XWikiBootstrapException(exc);
      }
    }
  }

  private XWiki bootstrapXWiki() throws XWikiException, IOException, ExecutionContextException {
    checkState(!initialised.getAndSet(true), "already initialised");
    XWikiConfig xwikiCfg = loadXWikiConfig();
    XWikiContext xwikiContext = createMainXWikiContext(xwikiCfg);
    Utils.setComponentManager(componentManager);
    initExecutionContext(xwikiContext);
    XWiki xwiki = createXWikiInstance(xwikiCfg, xwikiContext);
    // TODO requires XWiki ? Cfg should suffice
    xwikiContext.setURLFactory(xwiki.getURLFactoryService().createURLFactory(xwikiContext));
    stubContextProvider.initialize(xwikiContext);
    updateDatabases(xwikiContext);
    return xwiki;
  }

  private XWikiConfig loadXWikiConfig() throws IOException, XWikiException {
    try (InputStream is = servletContext.getResourceAsStream("/WEB-INF/xwiki.cfg")) {
      return new XWikiConfig(is);
    }
  }

  private XWikiContext createMainXWikiContext(XWikiConfig xwikiCfg)
      throws MalformedURLException {
    XWikiContext ctx = new XWikiContext();
    ctx.setMode(XWikiContext.MODE_SERVLET);
    ctx.setEngineContext(new XWikiServletContext(servletContext));
    ctx.setMainXWiki(XWikiConstant.MAIN_WIKI.getName());
    ctx.setDatabase(XWikiConstant.MAIN_WIKI.getName());
    ctx.setURL(serverUrlUtils.getServerURL(xwikiCfg::getProperty));
    return ctx;
  }

  private XWiki createXWikiInstance(XWikiConfig cfg, XWikiContext context) throws XWikiException {
    XWiki xwiki = new XWiki(cfg, context, context.getEngineContext());
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

  private void updateDatabases(XWikiContext context) throws XWikiException {
    try {
      if (context.getWiki().isVirtualMode()) {
        wikiService.streamAllWikis()
            .forEach(wikiUpdater::updateAsync);
      }
      if ("1".equals(context.getWiki().Param("xwiki.store.migration", "0"))) {
        wikiUpdater.runMigrationsSync();
        if ("1".equals(context.getWiki().Param("xwiki.store.migration.exitAfterEnd", "0"))) {
          LOGGER.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
          System.exit(0); // TODO throw exception instead ?
        }
      }
    } finally {
      wikiUpdater.shutdown();
    }
  }

  public class XWikiBootstrapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    XWikiBootstrapException(Throwable cause) {
      super(cause);
    }

  }

}
