package com.celements.init;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiException.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.WikiService;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

@Component
public class XWikiRequestInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRequestInitializer.class);

  private static final ImmutableSet<String> LOCAL_HOSTS = ImmutableSet.of(
      "localhost", "127.0.0.1", "::1");

  private final ServletContainerInitializer initializer;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiProvider xwikiProvider;
  private final XWikiConfigSource xwikiCfg;

  @Inject
  public XWikiRequestInitializer(
      ServletContainerInitializer initializer,
      WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiProvider xwikiProvider,
      XWikiConfigSource xwikiCfg) {
    this.initializer = initializer;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiProvider = xwikiProvider;
    this.xwikiCfg = xwikiCfg;
  }

  public XWikiContext init(String action, HttpServletRequest request, HttpServletResponse response)
      throws MalformedURLException, ServletContainerException, XWikiException {
    ExecutionContext execContext = initializer.initializeRequest(request);
    XWikiContext context = prepareXWikiContext(execContext, action, request, response);
    MDC.put("url", context.getURL().toExternalForm());
    initializer.initializeResponse(response);
    initializer.initializeSession(request);
    WikiReference wikiRef = determineWiki(context.getURL());
    context.setDatabase(wikiRef.getName());
    context.setOriginalDatabase(wikiRef.getName());
    XWiki xwiki = awaitWikiAvailability(wikiRef, Duration.ofHours(1));
    xwiki.prepareResources(context);
    LOGGER.info("request initialized");
    return context;
  }

  private XWikiContext prepareXWikiContext(ExecutionContext execCtx, String action,
      HttpServletRequest request, HttpServletResponse response) throws MalformedURLException {
    XWikiContext context = (XWikiContext) execCtx.getProperty(XWikiContext.EXEC_CONTEXT_KEY);
    checkNotNull(context, "should have been initialized by XWikiStubContextInitializer");
    context.setAction(action);
    context.setRequest(new XWikiServletRequest(request));
    context.setResponse(new XWikiServletResponse(response));
    context.setURL(context.getRequest().getURL());
    return context;
  }

  private WikiReference determineWiki(URL url) throws XWikiException {
    String host = Strings.nullToEmpty(url.getHost());
    checkArgument(!host.isEmpty());
    if (!xwikiCfg.isVirtualMode() || LOCAL_HOSTS.contains(host)) {
      return XWikiConstant.MAIN_WIKI;
    }
    WikiReference wikiRef = wikiService.getWikiForHost(host)
        .map(Optional::of) // replace with #or in Java9+
        // no wiki found based on the full host name, try to use the first part as the wiki name
        .orElseGet(() -> getWikiFromDomain(host))
        .orElseThrow(() -> new XWikiException(MODULE_XWIKI, ERROR_XWIKI_DOES_NOT_EXIST,
            "The wiki " + host + " does not exist"));
    LOGGER.debug("determineWiki - {}", wikiRef);
    return wikiRef;
  }

  private Optional<WikiReference> getWikiFromDomain(String host) {
    return Optional.of(host).filter(h -> h.indexOf(".") > 0)
        .map(h -> new WikiReference(h.substring(0, h.indexOf("."))))
        .filter(log(wikiService::hasWiki)
            .warn(LOGGER).msg("using wiki domain fallback"));
  }

  private XWiki awaitWikiAvailability(WikiReference wikiRef, Duration awaitDuration)
      throws XWikiException {
    Stopwatch timer = Stopwatch.createStarted();
    wikiUpdater.getFuture(wikiRef).ifPresent(rethrow(future -> {
      LOGGER.trace("awaitWikiUpdate - [{}]", wikiRef);
      awaitWikiUpdate(future, awaitDuration);
      LOGGER.debug("awaitWikiUpdate - done [{}], took {}", wikiRef.getName(), timer.elapsed());
    }));
    return xwikiProvider.await(awaitDuration.minus(timer.elapsed()));
  }

  private void awaitWikiUpdate(CompletableFuture<Void> future, Duration awaitDuration)
      throws XWikiException {
    try {
      future.get(awaitDuration.get(ChronoUnit.SECONDS), TimeUnit.SECONDS);
    } catch (ExecutionException | TimeoutException exc) {
      throw new XWikiException(MODULE_XWIKI, ERROR_XWIKI_INIT_FAILED,
          "Could not initialize main XWiki context", exc);
    } catch (InterruptedException iexc) {
      LOGGER.warn("getXWiki - interrupted", iexc);
      Thread.currentThread().interrupt();
    }
  }

  public void cleanup() {
    LOGGER.info("cleanup");
    initializer.cleanup();
    MDC.remove("url");
  }

}
