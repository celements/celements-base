package com.celements.init.request;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.execution.XWikiExecutionProp.*;
import static com.celements.logging.LogUtils.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.XWikiProvider;
import com.celements.init.update.WikiUpdater;
import com.celements.struts.StrutsActionUtils;
import com.celements.url.UrlService;
import com.celements.wiki.WikiService;
import com.celements.wiki.exception.WikiMissingException;
import com.google.common.base.Stopwatch;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

@Component
public class CelementsRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelementsRequestFilter.class);

  private final Execution execution;
  private final ExecutionContextManager execContextManager;
  private final ServletContainerInitializer containerInitializer;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiProvider xwikiProvider;
  private final StrutsActionUtils actionUtils;

  @Inject
  public CelementsRequestFilter(
      Execution execution,
      ExecutionContextManager execContextManager,
      ServletContainerInitializer containerInitializer,
      @Lazy WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiProvider xwikiProvider,
      UrlService urlService,
      StrutsActionUtils actionUtils) {
    this.execution = execution;
    this.execContextManager = execContextManager;
    this.containerInitializer = containerInitializer;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiProvider = xwikiProvider;
    this.actionUtils = actionUtils;
  }

  public ExecutionContext preExecute(HttpServletRequest request,
      HttpServletResponse response) throws WikiMissingException, ExecutionException,
      ExecutionContextException, ServletContainerException {
    LOGGER.trace("preExecute for servlet-request");
    return preExecute(actionUtils.getActionForRequest(request), request, response);
  }

  public ExecutionContext preExecute(String action, HttpServletRequest request,
      HttpServletResponse response) throws WikiMissingException, ExecutionException,
      ExecutionContextException, ServletContainerException {
    if (execution.getContext() != null) {
      return execution.getContext();
    }
    LOGGER.debug("preExecute - action [{}], request [{}]",
        action, defer(() -> request.getRequestURL().toString()));
    ExecutionContext eContext = createExecContextForRequest(action, request, response);
    containerInitializer.initializeRequest(request);
    containerInitializer.initializeResponse(response);
    containerInitializer.initializeSession(request);
    execContextManager.initialize(eContext);
    WikiReference wikiRef = eContext.get(WIKI).orElseThrow(IllegalStateException::new);
    XWikiContext xContext = eContext.get(XWIKI_CONTEXT).orElseThrow(IllegalStateException::new);
    XWiki xwiki = awaitWikiAvailability(wikiRef, Duration.ofHours(1));
    xwiki.prepareResources(xContext);
    LOGGER.debug("request initialized for action={}", action);
    return eContext;
  }

  private ExecutionContext createExecContextForRequest(String action,
      HttpServletRequest request, HttpServletResponse response)
      throws WikiMissingException {
    ExecutionContext context = new ExecutionContext();
    execution.setContext(context);
    XWikiRequest xRequest = new XWikiServletRequest(request);
    context.set(WIKI, wikiService.determineWiki(xRequest.getUri()));
    context.set(XWIKI_REQUEST, xRequest);
    context.set(XWIKI_REQUEST_ACTION, action);
    context.set(XWIKI_REQUEST_URI, xRequest.getUri());
    XWikiResponse xResponse = new XWikiServletResponse(response);
    context.set(XWIKI_RESPONSE, xResponse);
    return context;
  }

  private XWiki awaitWikiAvailability(WikiReference wikiRef, Duration awaitDuration)
      throws ExecutionException {
    Stopwatch timer = Stopwatch.createStarted();
    wikiUpdater.getFuture(wikiRef).ifPresent(rethrow(future -> {
      LOGGER.trace("awaitWikiUpdate - [{}]", wikiRef);
      awaitWikiUpdate(future, awaitDuration);
      LOGGER.debug("awaitWikiUpdate - done [{}], took {}", wikiRef.getName(), timer.elapsed());
    }));
    return xwikiProvider.await(awaitDuration.minus(timer.elapsed()));
  }

  private void awaitWikiUpdate(CompletableFuture<Void> future, Duration awaitDuration)
      throws ExecutionException {
    try {
      future.get(awaitDuration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    } catch (TimeoutException exc) {
      throw new ExecutionException("timed out awaiting wiki update", exc);
    } catch (InterruptedException iexc) {
      LOGGER.warn("getXWiki - interrupted", iexc);
      Thread.currentThread().interrupt();
    }
  }

  public void postExecute() {
    if (execution.getContext() == null) {
      return;
    }
    LOGGER.debug("postExecute");
    containerInitializer.cleanup();
    execution.removeContext();
  }

}
