package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.google.common.base.Preconditions.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiExecutionContextInitializer;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

@Immutable
@Singleton
@Component
public class CelementsBootstrap implements ApplicationListener<CelementsStartedEvent>, Ordered {

  public static final int ORDER = -1000; // high precedence

  protected static final Logger LOGGER = LoggerFactory.getLogger(CelementsBootstrap.class);

  private static final AtomicBoolean INIT_FLAG = new AtomicBoolean(false);

  private final ServletContext servletContext;
  private final Execution execution;
  private final ExecutionContextManager executionManager;
  private final ComponentManager componentManager;
  private final XWikiHibernateStore hibernateStore;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiConfigSource xwikiCfg;
  private final ConfigurationSource cfgSrc;
  private final ApplicationEventPublisher eventPublisher;

  @Inject
  public CelementsBootstrap(
      ServletContext servletContext,
      Execution execution,
      ExecutionContextManager executionManager,
      ComponentManager componentManager,
      @Named("hibernate") XWikiStoreInterface hibernateStore,
      WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiConfigSource xwikiCfg,
      @Named("allproperties") ConfigurationSource cfgSrc,
      ApplicationEventPublisher eventPublisher) {
    this.servletContext = servletContext;
    this.execution = execution;
    this.executionManager = executionManager;
    this.componentManager = componentManager;
    this.hibernateStore = (XWikiHibernateStore) hibernateStore;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiCfg = xwikiCfg;
    this.cfgSrc = cfgSrc;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    LOGGER.info("Celements bootstrap start");
    checkState(!INIT_FLAG.getAndSet(true), "already initialised");
    checkState(servletContext.getAttribute(XWiki.SERVLET_CONTEXT_KEY) == null);
    CompletableFuture<XWiki> xwikiFuture = new CompletableFuture<>();
    servletContext.setAttribute(XWiki.SERVLET_CONTEXT_KEY, xwikiFuture);
    try {
      XWiki xwiki = bootstrapXWiki();
      // make XWiki available to all requests via servlet context, see {@link XWikiProvider}
      xwikiFuture.complete(xwiki);
      LOGGER.info("XWiki initialised");
      eventPublisher.publishEvent(new CelementsInitialisedEvent(this));
      LOGGER.info("Celements initialised");
    } catch (Exception exc) {
      xwikiFuture.completeExceptionally(exc);
      LOGGER.error("Celements bootstrap failed");
      throw new CelementsBootstrapException(exc);
    }
    LOGGER.info("Celements bootstrap done");
  }

  private XWiki bootstrapXWiki() throws XWikiException, ExecutionContextException {
    Utils.setComponentManager(componentManager);
    LOGGER.debug("initialising ExecutionContext...");
    ExecutionContext executionCtx = initExecutionContext();
    LOGGER.debug("checkHibernate...");
    hibernateStore.checkHibernate(XWikiConstant.MAIN_WIKI);
    LOGGER.debug("initialising XWiki...");
    XWiki xwiki = new XWiki(true);
    executionCtx.set(XWIKI, xwiki);
    LOGGER.debug("loading Plugins...");
    xwiki.loadPlugins();
    LOGGER.debug("triggering startup tasks...");
    triggerStartupTasks();
    return xwiki;
  }

  public ExecutionContext initExecutionContext() throws ExecutionContextException {
    ExecutionContext executionCtx = new ExecutionContext();
    execution.setContext(executionCtx);
    // disable awaiting XWiki instance in this bootstrap execution
    executionCtx.set(XWikiExecutionContextInitializer.NO_AWAIT, true);
    executionManager.initialize(executionCtx);
    return executionCtx;
  }

  private void triggerStartupTasks() {
    try {
      if (Boolean.TRUE.equals(cfgSrc.getProperty("celements.init.updatedatabases", Boolean.class))
          && xwikiCfg.isVirtualMode()) {
        LOGGER.info("triggering wiki updates");
        wikiService.streamAllWikis()
            .forEach(wikiUpdater::updateAsync);
      } else {
        LOGGER.trace("skipping wiki updates");
      }
      if (Boolean.TRUE.equals(Optional
          .ofNullable(cfgSrc.getProperty("celements.init.migration", Boolean.class))
          .orElseGet(() -> "1".equals(xwikiCfg.getProperty("xwiki.store.migration", "0"))))) {
        LOGGER.info("triggering migrations");
        wikiUpdater.runAllMigrationsAsync();
      } else {
        LOGGER.trace("skipping migrations");
      }
    } finally {
      wikiUpdater.shutdown(); // no new tasks will be accepted
    }
  }

  public class CelementsBootstrapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    CelementsBootstrapException(Throwable cause) {
      super(cause);
    }

  }

}
