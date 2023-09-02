package com.celements.init;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.execution.XWikiExecutionProp.*;
import static com.google.common.base.Preconditions.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWiki;

@Component
public class XWikiProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiProvider.class);

  private final ServletContext servletContext;
  private final Execution execution;

  @Inject
  public XWikiProvider(
      ServletContext servletContext,
      Execution execution) {
    this.servletContext = servletContext;
    this.execution = execution;
  }

  @NotNull
  public Optional<XWiki> get() {
    return getFromEContext().or(this::getFromSContext);
  }

  @NotNull
  public XWiki await(Duration awaitDuration) throws ExecutionException {
    return getFromEContext()
        .or(rethrow(() -> getFromSContext(awaitDuration)))
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<XWiki> getFromEContext() {
    return getEContext().flatMap(ctx -> ctx.get(XWIKI));
  }

  private Optional<XWiki> getFromSContext() {
    LOGGER.trace("getFromSContext - noawait");
    return Optional.ofNullable(getXWikiServletFuture())
        .filter(future -> future.isDone() && !future.isCompletedExceptionally())
        .map(CompletableFuture::join);
  }

  private Optional<XWiki> getFromSContext(Duration awaitDuration) throws ExecutionException {
    try {
      LOGGER.trace("getFromSContext - await [{}]", awaitDuration);
      CompletableFuture<XWiki> future = getXWikiServletFuture();
      checkState(future != null, "should not happen, are we before ApplicationStartedEvent?");
      long awaitSeconds = Optional.ofNullable(awaitDuration)
          .filter(duration -> !duration.isNegative())
          .map(duration -> duration.get(ChronoUnit.SECONDS))
          .orElse(0L);
      return Optional.ofNullable(getXWikiServletFuture().get(awaitSeconds, TimeUnit.SECONDS));
    } catch (TimeoutException exc) {
      throw new ExecutionException("timed out awaiting XWiki", exc);
    } catch (InterruptedException iexc) {
      LOGGER.warn("getFromSContext - interrupted", iexc);
      Thread.currentThread().interrupt();
      throw new IllegalStateException();
    }
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<XWiki> getXWikiServletFuture() {
    return (CompletableFuture<XWiki>) servletContext.getAttribute(XWiki.SERVLET_CONTEXT_KEY);
  }

  private Optional<ExecutionContext> getEContext() {
    return Optional.ofNullable(execution.getContext());
  }

}
