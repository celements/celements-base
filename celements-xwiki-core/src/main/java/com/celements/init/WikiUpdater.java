package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.google.common.base.Preconditions.*;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingRunnable;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;
import com.xpn.xwiki.util.AbstractXWikiRunnable;

import one.util.streamex.EntryStream;

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

  public void await(WikiReference wiki) {
    getFuture(wiki).ifPresent(CompletableFuture::join);
  }

  public void awaitAll() {
    getAllFutures().forEach(CompletableFuture::join);
  }

  public CompletableFuture<Void> runUpdateAsync(WikiReference wikiRef, Runnable action) {
    return runUpdateAsyncExc(wikiRef, action::run);
  }

  public CompletableFuture<Void> runUpdateAsyncExc(WikiReference wikiRef,
      ThrowingRunnable<Exception> action) {
    checkNotNull(wikiRef);
    checkState(!executor.isShutdown());
    var wikiUpdateAction = new WikiUpdateRunnable(wikiRef, action);
    return wikiUpdates.compute(wikiRef,
        (wiki, future) -> (future == null) || future.isDone()
            ? CompletableFuture.runAsync(wikiUpdateAction, executor)
            : future.thenRunAsync(wikiUpdateAction, executor));
  }

  public CompletableFuture<Void> updateAsync(WikiReference wikiRef) {
    return runUpdateAsyncExc(wikiRef, () -> {
      LOGGER.debug("updateWiki - starting [{}]", wikiRef.getName());
      Stopwatch t = Stopwatch.createStarted();
      XWiki xwiki = wikiProvider.get().orElseThrow(IllegalStateException::new);
      xwiki.updateDatabase(wikiRef.getName(), false, false, getContext());
      LOGGER.info("updateWiki - done [{}], took {}", wikiRef.getName(), t.elapsed());
    });
  }

  public void runAllMigrationsAsync() {
    var wikiUpdateAction = new WikiUpdateRunnable(XWikiConstant.MAIN_WIKI, () -> {
      LOGGER.debug("runMigrations - waiting for all wiki updates to finish...");
      awaitAll(); // TODO instead awaitAll start migration per wiki with runUpdateAsync
      LOGGER.debug("runMigrations - wiki updates finished, starting migrations");
      getMigrationManager(getContext()).startMigrations(getContext());
      if (Boolean.TRUE.equals(Optional
          .ofNullable(cfgSrc.getProperty("celements.init.migration.exitAfterEnd", Boolean.class))
          .orElseGet(() -> "1".equals(xwikiCfg.getProperty(
              "xwiki.store.migration.exitAfterEnd", "0"))))) {
        LOGGER.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
        System.exit(0); // so brutal
      }
    });
    CompletableFuture.runAsync(wikiUpdateAction, executor);
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

  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @PreDestroy
  public CompletableFuture<Void> shutdown() {
    executor.shutdown();
    return cleanup();
  }

  private CompletableFuture<Void> cleanup() {
    return CompletableFuture.allOf(EntryStream.of(wikiUpdates)
        .mapKeyValue((wiki, future) -> future
            .thenRunAsync(() -> wikiUpdates.remove(wiki))) // uses the default executor
        .toArray(CompletableFuture.class));
  }

  public void shutdownAwait() {
    awaitAll(); // wait for all tasks to finish
    var cleanup = shutdown(); // tell the executor to stop accepting new tasks
    cleanup.join(); // wait for the cleanup to finish
  }

  private class WikiUpdateRunnable extends AbstractXWikiRunnable {

    private final ThrowingRunnable<Exception> action;

    WikiUpdateRunnable(WikiReference wikiRef, ThrowingRunnable<Exception> action) {
      super(Map.of(/* WIKI.getName(), wikiRef, */
          // make XWiki available in the runnable's execution context since it's not necessarily
          // already available in the servlet context, see XWikiProvider
          XWIKI.getName(), wikiProvider.get().orElseThrow(IllegalStateException::new)));
      this.action = action;
    }

    @Override
    protected void runInternal() throws Exception {
      action.run();
    }
  }

}
