package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.google.common.base.Preconditions.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingRunnable;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.AbstractXWikiRunnable;

import one.util.streamex.EntryStream;

@Component
public class WikiUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiUpdater.class);
  private static final int THREAD_COUNT = 11;

  private final XWikiProvider wikiProvider;
  private final Execution execution;
  private final ExecutorService executor;
  private final ConcurrentHashMap<WikiReference, CompletableFuture<Void>> wikiUpdates;
  private final AtomicBoolean shutdown;
  private final CompletableFuture<Void> shutdownFuture;

  @Inject
  public WikiUpdater(
      XWikiProvider wikiProvider,
      Execution execution) {
    this.wikiProvider = wikiProvider;
    this.execution = execution;
    this.executor = Executors.newFixedThreadPool(THREAD_COUNT, new ThreadFactoryBuilder()
        .setNameFormat("cel-wiki-updater-%d").build());
    this.wikiUpdates = new ConcurrentHashMap<>();
    this.shutdown = new AtomicBoolean(false);
    this.shutdownFuture = new CompletableFuture<>();
    this.shutdownFuture.whenComplete((v, e) -> executor.shutdown());
  }

  public Optional<CompletableFuture<Void>> getFuture(WikiReference wiki) {
    return Optional.ofNullable(wikiUpdates.get(wiki));
  }

  public Map<WikiReference, CompletableFuture<Void>> getAllFutures() {
    return Collections.unmodifiableMap(wikiUpdates);
  }

  public CompletableFuture<Void> runUpdateAsync(WikiReference wikiRef, Runnable action) {
    return runUpdateAsyncExc(wikiRef, action::run);
  }

  public CompletableFuture<Void> runUpdateAsyncExc(WikiReference wikiRef,
      ThrowingRunnable<Exception> action) {
    checkNotNull(wikiRef);
    checkState(!isShutdown());
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

  private XWikiContext getContext() {
    return execution.getContext().get(XWIKI_CONTEXT).orElseThrow(IllegalStateException::new);
  }

  public boolean isShutdown() {
    return shutdown.get();
  }

  public CompletableFuture<Void> getShutdownFuture() {
    return shutdownFuture;
  }

  public CompletableFuture<Void> shutdown() {
    if (shutdown.compareAndSet(false, true)) {
      LOGGER.info("shutting down WikiUpdater");
      onAllUpdates().whenComplete((v, exc) -> {
        if (exc != null) {
          getShutdownFuture().completeExceptionally(exc);
        } else {
          getShutdownFuture().complete(null);
        }
      });
    }
    return getShutdownFuture();
  }

  private CompletableFuture<Void> onAllUpdates() {
    var allUpdates = EntryStream.of(wikiUpdates)
        .mapKeyValue((wiki, f) -> XWikiConstant.MAIN_WIKI.equals(wiki)
            ? f // propagate exceptions for main wiki
            : f.exceptionally(exc -> { // just log exceptions for other wikis
              LOGGER.error("Wiki update failed for {}", wiki, exc);
              return null;
            }));
    return CompletableFuture.allOf(allUpdates.toArray(CompletableFuture[]::new));
  }

  private class WikiUpdateRunnable extends AbstractXWikiRunnable {

    private final ThrowingRunnable<Exception> action;

    WikiUpdateRunnable(WikiReference wikiRef, ThrowingRunnable<Exception> action) {
      super(Map.of(WIKI.getName(), wikiRef,
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
