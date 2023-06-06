package com.celements.init;

import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiExecutionProp.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiExecutionProp;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;
import com.xpn.xwiki.util.AbstractXWikiRunnable;

@Component
public class WikiUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiUpdater.class);
  private static final int THREAD_COUNT = 11;

  private final ConfigurationSource cfgSrc;
  private final XWikiConfigSource xwikiCfg;
  private final XWikiProvider wikiProvider;
  private final Execution execution;
  private final ExecutorService executor;
  private final ConcurrentHashMap<WikiReference, CompletableFuture<Void>> wikiUpdates;

  @Inject
  public WikiUpdater(
      @Named("allproperties") ConfigurationSource cfgSrc,
      XWikiConfigSource xwikiCfg,
      XWikiProvider wikiProvider,
      Execution execution) {
    this.cfgSrc = cfgSrc;
    this.xwikiCfg = xwikiCfg;
    this.wikiProvider = wikiProvider;
    this.execution = execution;
    this.executor = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactoryBuilder()
        .setNameFormat("cel-wiki-updater-%d").build());
    this.wikiUpdates = new ConcurrentHashMap<>();
  }

  public Optional<CompletableFuture<Void>> getFuture(WikiReference wiki) {
    return Optional.ofNullable(wikiUpdates.get(wiki));
  }

  public Stream<CompletableFuture<Void>> getAllFutures() {
    return wikiUpdates.values().stream();
  }

  public CompletableFuture<Void> updateAsync(WikiReference wikiRef) {
    checkNotNull(wikiRef);
    checkState(!executor.isShutdown());
    XWiki xwiki = wikiProvider.get().orElseThrow(IllegalStateException::new);
    return wikiUpdates.compute(wikiRef,
        (wiki, future) -> (future == null) || future.isDone()
            ? CompletableFuture.runAsync(new WikiUpdateRunnable(wikiRef, xwiki), executor)
            : future);
  }

  // TODO start migration per wiki within updateAsync
  public void runAllMigrationsAsync() {
    CompletableFuture.runAsync(new AbstractXWikiRunnable() {

      @Override
      protected void runInternal() throws XWikiException {
        LOGGER.debug("runMigrations - waiting for all wiki updates to finish...");
        getAllFutures().forEach(CompletableFuture::join);
        LOGGER.debug("runMigrations - wiki updates finished, starting migrations");
        getMigrationManager(getContext()).startMigrations(getContext());
        if (Boolean.TRUE.equals(Optional
            .ofNullable(cfgSrc.getProperty("celements.init.migration.exitAfterEnd", Boolean.class))
            .orElseGet(() -> "1".equals(xwikiCfg.getProperty(
                "xwiki.store.migration.exitAfterEnd", "0"))))) {
          LOGGER.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
          System.exit(0); // so brutal
        }
      }
    }, executor);
  }

  // TODO refactor to component
  private XWikiMigrationManagerInterface getMigrationManager(XWikiContext context)
      throws XWikiException {
    String storeClass = xwikiCfg.getProperty("xwiki.store.migration.manager.class");
    try {
      return (XWikiMigrationManagerInterface) Class.forName(storeClass)
          .getConstructor(XWikiContext.class)
          .newInstance(context);
    } catch (ReflectiveOperationException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
          "Cannot load class " + storeClass, exc);
    }
  }

  private XWikiContext getContext() {
    return execution.getContext().get(XWIKI_CONTEXT).orElseThrow(IllegalStateException::new);
  }

  @PreDestroy
  public void shutdown() {
    executor.shutdown();
  }

  private class WikiUpdateRunnable extends AbstractXWikiRunnable {

    private final WikiReference wikiRef;

    WikiUpdateRunnable(WikiReference wikiRef, XWiki xwiki) {
      // make XWiki available in the runnable's execution context since it's not necessarily
      // already available in the servlet context, see XWikiProvider
      super(XWikiExecutionProp.XWIKI.getName(), xwiki);
      this.wikiRef = wikiRef;
    }

    @Override
    protected void runInternal() {
      try {
        LOGGER.debug("updateWiki - starting [{}]", wikiRef.getName());
        Stopwatch t = Stopwatch.createStarted();
        XWiki xwiki = wikiProvider.get().orElseThrow(IllegalStateException::new);
        xwiki.updateDatabase(wikiRef.getName(), false, getContext());
        wikiUpdates.remove(wikiRef);
        LOGGER.info("updateWiki - done [{}], took {}", wikiRef.getName(), t.elapsed());
      } catch (HibernateException | XWikiException exc) {
        throw new DatabaseUpdateException(exc);
      }
    }

  }

  public class DatabaseUpdateException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    DatabaseUpdateException(Throwable cause) {
      super(cause);
    }

  }

}
