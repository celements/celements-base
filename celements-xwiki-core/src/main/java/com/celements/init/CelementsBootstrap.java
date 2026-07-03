package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.user.api.XWikiRightService.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.wiki.WikiCreationCoordinator;
import com.celements.wiki.WikiMissingException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.XWikiExecutionContextInitializer;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiUser;
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
  private final XWikiConfigSource xwikiCfg;
  private final WikiCreationCoordinator lifecyclePublisher;
  private final ListableBeanFactory beanFactory;

  @Inject
  public CelementsBootstrap(
      ServletContext servletContext,
      Execution execution,
      ExecutionContextManager executionManager,
      ComponentManager componentManager,
      @Named("hibernate") XWikiStoreInterface hibernateStore,
      XWikiConfigSource xwikiCfg,
      WikiCreationCoordinator lifecyclePublisher,
      ListableBeanFactory beanFactory) {
    this.servletContext = servletContext;
    this.execution = execution;
    this.executionManager = executionManager;
    this.componentManager = componentManager;
    this.hibernateStore = (XWikiHibernateStore) hibernateStore;
    this.xwikiCfg = xwikiCfg;
    this.lifecyclePublisher = lifecyclePublisher;
    this.beanFactory = beanFactory;
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
    } catch (Exception exc) {
      xwikiFuture.completeExceptionally(exc);
      LOGGER.error("Celements bootstrap failed");
      throw new CelementsBootstrapException(exc);
    }
    LOGGER.info("Celements bootstrap done");
  }

  private XWiki bootstrapXWiki() throws XWikiException, ExecutionContextException {
    ExecutionContext eCtx = createExecutionContext();
    Utils.setComponentManager(componentManager);
    LOGGER.debug("initialising hibernate...");
    hibernateStore.initHibernate();
    var mainWiki = initMainWiki();
    LOGGER.debug("initialising ExecutionContext...");
    executionManager.initialize(eCtx);
    LOGGER.debug("initialising XWiki...");
    XWiki xwiki = new XWiki(true);
    eCtx.set(XWIKI, xwiki);
    LOGGER.debug("loading Plugins...");
    xwiki.loadPlugins();
    if (mainWiki != null) {
      LOGGER.debug("notifying main wiki creation...");
      lifecyclePublisher.publishCreated(mainWiki);
      LOGGER.debug("flushing store caches...");
      // prevents borked XWikiPreferences
      beanFactory.getBeansOfType(XWikiCacheStoreInterface.class).values()
          .forEach(XWikiCacheStoreInterface::flushCache);
    }
    return xwiki;
  }

  public ExecutionContext createExecutionContext() {
    ExecutionContext executionCtx = new ExecutionContext();
    execution.setContext(executionCtx);
    executionCtx.set(XWIKI_USER, new XWikiUser(SUPERADMIN_FQN, true));
    // disable awaiting XWiki instance in this bootstrap execution
    executionCtx.set(XWikiExecutionContextInitializer.NO_AWAIT, true);
    return executionCtx;
  }

  private WikiReference initMainWiki() throws XWikiException {
    var mainWiki = new WikiReference(xwikiCfg.getMainWikiName());
    // main wiki must already be created on startup, but might be empty
    try {
      if (hibernateStore.isWikiEmpty(mainWiki)) {
        LOGGER.debug("initialising main wiki...");
        hibernateStore.updateSchema(mainWiki, true);
        return mainWiki;
      }
    } catch (HibernateException | WikiMissingException | XWikiException e) {
      throw new XWikiException(0, 0, "failed to init main wiki", e);
    }
    return null;
  }

  public class CelementsBootstrapException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    CelementsBootstrapException(Throwable cause) {
      super(cause);
    }

  }

}
