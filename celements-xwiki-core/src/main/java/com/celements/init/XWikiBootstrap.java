package com.celements.init;

import static com.google.common.base.Preconditions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.inject.Singleton;
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
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiExecutionContextInitializer;
import com.xpn.xwiki.web.Utils;

@Immutable
@Singleton
@Component
public class XWikiBootstrap implements ApplicationListener<CelementsLifecycleEvent>, Ordered {

  protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiBootstrap.class);

  private static final AtomicBoolean INIT_FLAG = new AtomicBoolean(false);

  private final ServletContext servletContext;
  private final Execution execution;
  private final ExecutionContextManager executionManager;
  private final ComponentManager componentManager;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiConfigSource xwikiCfg;

  @Inject
  public XWikiBootstrap(
      ServletContext servletContext,
      Execution execution,
      ExecutionContextManager executionManager,
      ComponentManager componentManager,
      WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiConfigSource xwikiCfg) {
    this.servletContext = servletContext;
    this.execution = execution;
    this.executionManager = executionManager;
    this.componentManager = componentManager;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiCfg = xwikiCfg;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    if (event.getType() == CelementsLifecycleEvent.State.STARTED) {
      checkState(!INIT_FLAG.getAndSet(true), "already initialised");
      checkState(servletContext.getAttribute(XWiki.CONTEXT_KEY) == null);
      CompletableFuture<XWiki> xwikiFuture = new CompletableFuture<>();
      servletContext.setAttribute(XWiki.CONTEXT_KEY, xwikiFuture);
      try {
        XWiki xwiki = bootstrapXWiki();
        // make XWiki available to all requests via servlet context, see {@link XWikiProvider}
        xwikiFuture.complete(xwiki);
        LOGGER.info("XWiki published");
      } catch (Exception exc) {
        xwikiFuture.completeExceptionally(exc);
        throw new XWikiBootstrapException(exc);
      }
    }
  }

  private XWiki bootstrapXWiki() throws XWikiException, ExecutionContextException {
    Utils.setComponentManager(componentManager);
    ExecutionContext executionCtx = initExecutionContext();
    XWiki xwiki = new XWiki(true);
    executionCtx.setProperty(XWiki.CONTEXT_KEY, xwiki);
    xwiki.loadPlugins();
    triggerWikiUpdates();
    return xwiki;
  }

  public ExecutionContext initExecutionContext() throws ExecutionContextException {
    ExecutionContext executionCtx = new ExecutionContext();
    execution.setContext(executionCtx);
    // disable awaiting XWiki instance in this bootstrap execution
    executionCtx.setProperty(XWikiExecutionContextInitializer.CTX_NO_AWAIT_KEY, true);
    executionManager.initialize(executionCtx);
    return executionCtx;
  }

  private void triggerWikiUpdates() {
    try {
      if ("1".equals(xwikiCfg.getProperty("xwiki.store.updatedatabase", "1"))
          && xwikiCfg.isVirtualMode()) {
        wikiService.streamAllWikis()
            .forEach(wikiUpdater::updateAsync);
      }
      if ("1".equals(xwikiCfg.getProperty("xwiki.store.migration", "0"))) {
        wikiUpdater.runAllMigrationsAsync();
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
