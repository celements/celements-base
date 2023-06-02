package com.celements.init;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
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
import com.xpn.xwiki.XWikiException;

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
    return getFromEContext()
        .map(Optional::of) // replace with #or in Java9+
        .orElseGet(this::getFromSContext);
  }

  @NotNull
  public XWiki await(Duration awaitDuration) throws XWikiException {
    return getFromEContext()
        .map(Optional::of) // replace with #or in Java9+
        .orElseGet(rethrow(() -> getFromSContext(awaitDuration)))
        .orElseThrow(IllegalStateException::new);
  }

  private Optional<XWiki> getFromEContext() {
    return Optional.ofNullable((XWiki) getEContext().getProperty(XWiki.CONTEXT_KEY));
  }

  private Optional<XWiki> getFromSContext() {
    return Optional.ofNullable(getXWikiServletFuture())
        .filter(future -> future.isDone() && !future.isCompletedExceptionally())
        .map(CompletableFuture::join);
  }

  private Optional<XWiki> getFromSContext(Duration awaitDuration) throws XWikiException {
    try {
      LOGGER.trace("awaitXWikiBootstrap");
      CompletableFuture<XWiki> future = getXWikiServletFuture();
      checkState(future != null, "should not happen, are we before ApplicationStartedEvent?");
      return Optional.ofNullable(((awaitDuration == null) || awaitDuration.isNegative())
          ? getXWikiServletFuture().join()
          : getXWikiServletFuture().get(awaitDuration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS));
    } catch (ExecutionException | TimeoutException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
          "Could not initialize main XWiki context", exc);
    } catch (InterruptedException iexc) {
      LOGGER.warn("getXWiki - interrupted", iexc);
      Thread.currentThread().interrupt();
      throw new IllegalStateException();
    }
  }

  @SuppressWarnings("unchecked")
  private CompletableFuture<XWiki> getXWikiServletFuture() {
    return (CompletableFuture<XWiki>) servletContext.getAttribute(XWiki.CONTEXT_KEY);
  }

  private ExecutionContext getEContext() {
    return execution.getContext();
  }

}
