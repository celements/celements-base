package com.celements.init;

import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.WikiService;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

@Component
public class XWikiRequestInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRequestInitializer.class);

  private static final ImmutableSet<String> LOCAL_HOSTS = ImmutableSet.of(
      "localhost", "127.0.0.1", "::1");

  private final ServletContext servletContext;
  private final ServletContainerInitializer initializer;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiProvider xwikiProvider;
  private final XWikiConfigSource xwikiCfg;

  @Inject
  public XWikiRequestInitializer(
      ServletContext servletContext,
      ServletContainerInitializer initializer,
      WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiProvider xwikiProvider,
      XWikiConfigSource xwikiCfg) {
    this.servletContext = servletContext;
    this.initializer = initializer;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiProvider = xwikiProvider;
    this.xwikiCfg = xwikiCfg;
  }

  public XWikiContext init(String action, HttpServletRequest request, HttpServletResponse response)
      throws MalformedURLException, ServletContainerException, XWikiException {
    XWikiContext context = Utils.prepareContext(action,
        new XWikiServletRequest(request),
        new XWikiServletResponse(response),
        new XWikiServletContext(servletContext));
    MDC.put("url", context.getURL().toExternalForm());
    initializer.initializeRequest(request, context);
    initializer.initializeResponse(response);
    initializer.initializeSession(request);
    WikiReference wikiRef = determineWiki(context);
    awaitWikiUpdate(wikiRef);
    context.setDatabase(wikiRef.getName());
    context.setOriginalDatabase(wikiRef.getName());
    XWiki xwiki = xwikiProvider.get(); // blocking on bootstrap
    xwiki.prepareResources(context);
    LOGGER.debug("request initialized");
    return context;
  }

  private WikiReference determineWiki(XWikiContext context) throws XWikiException {
    Optional<String> host = Optional.ofNullable(context.getURL()).map(URL::getHost);
    if (!xwikiCfg.isVirtualMode() || host.filter(LOCAL_HOSTS::contains).isPresent()) {
      return XWikiConstant.MAIN_WIKI;
    }
    WikiReference wikiRef = host.flatMap(wikiService::getWikiForHost)
        .map(Optional::of) // replace with #or in Java9+
        // no wiki found based on the full host name, try to use the first part as the wiki name
        .orElseGet(() -> host.filter(h -> h.indexOf(".") > 0)
            .map(h -> new WikiReference(h.substring(0, h.indexOf("."))))
            .filter(wikiService::hasWiki))
        .orElseThrow(() -> new XWikiException(XWikiException.MODULE_XWIKI,
            XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, "The wiki " + host + " does not exist"));
    LOGGER.debug("determineWiki - {}", wikiRef);
    return wikiRef;
  }

  private void awaitWikiUpdate(WikiReference wikiRef) throws XWikiException {
    wikiUpdater.getFuture(wikiRef).ifPresent(rethrow(future -> {
      try {
        LOGGER.trace("awaitWikiUpdate - on {}", wikiRef);
        future.get(1, TimeUnit.HOURS);
      } catch (ExecutionException | TimeoutException exc) {
        throw new XWikiException(XWikiException.MODULE_XWIKI,
            XWikiException.ERROR_XWIKI_INIT_FAILED, "Could not initialize main XWiki context", exc);
      } catch (InterruptedException iexc) {
        LOGGER.warn("getXWiki - interrupted", iexc);
        Thread.currentThread().interrupt();
      }
    }));
  }

  public void cleanup() {
    initializer.cleanupSession();
    MDC.remove("url");
  }

}
