package com.celements.init;

import static com.google.common.base.Preconditions.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;
import com.xpn.xwiki.util.AbstractXWikiRunnable;

@Component
public class WikiUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiUpdater.class);
  private static final int THREAD_COUNT = 10;

  private final XWikiConfigSource xwikiCfg;
  private final Execution execution;
  private final ExecutorService executor;
  private final ConcurrentHashMap<WikiReference, CompletableFuture<Void>> wikiUpdates;

  @Inject
  public WikiUpdater(XWikiConfigSource xwikiCfg, Execution execution) {
    this.xwikiCfg = xwikiCfg;
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
    return wikiUpdates.compute(wikiRef,
        (wiki, future) -> (future == null) || future.isDone()
            ? CompletableFuture.runAsync(new WikiUpdateRunnable(wikiRef, getContext()), executor)
            : future);
  }

  // TODO start migration per wiki within updateAsync
  public void runMigrationsSync() throws XWikiException {
    LOGGER.debug("runMigrations - waiting for all wiki updates to finish...");
    getAllFutures().forEach(CompletableFuture::join);
    LOGGER.debug("runMigrations - wiki updates finished, starting migrations");
    getMigrationManager(getContext()).startMigrations(getContext());
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

  @PreDestroy
  public void shutdown() {
    executor.shutdown();
  }

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
  }

  private class WikiUpdateRunnable extends AbstractXWikiRunnable {

    private final WikiReference wikiRef;

    WikiUpdateRunnable(WikiReference wikiRef, XWikiContext context) {
      super(XWikiContext.EXECUTIONCONTEXT_KEY, context.clone());
      this.wikiRef = wikiRef;
    }

    @Override
    protected void runInternal() {
      try {
        LOGGER.debug("updateWiki - starting [{}]", wikiRef.getName());
        Stopwatch t = Stopwatch.createStarted();
        getContext().getWiki().updateDatabase(wikiRef.getName(), false, getContext());
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
