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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWiki;
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

  private final ServletContext servletContext;
  private final ServletContainerInitializer initializer;
  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final XWikiProvider xwikiProvider;

  @Inject
  public XWikiRequestInitializer(
      ServletContext servletContext,
      ServletContainerInitializer initializer,
      WikiService wikiService,
      WikiUpdater wikiUpdater,
      XWikiProvider xwikiProvider) {
    this.servletContext = servletContext;
    this.initializer = initializer;
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.xwikiProvider = xwikiProvider;
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
    XWiki xwiki = xwikiProvider.get();
    xwiki.prepareResources(context);
    DocumentReference requestDocRef = xwiki.getDocumentReference(context.getRequest(), context);
    context.setDatabase(requestDocRef.getWikiReference().getName());
    return context;
  }

  private WikiReference determineWiki(XWikiContext context) throws XWikiException {
    String host = Optional.ofNullable(context.getURL()).map(URL::getHost).orElse("");
    if (host.equals("localhost") || host.equals("127.0.0.1")) {
      return XWikiConstant.MAIN_WIKI;
    }
    WikiReference wikiRef = wikiService.getWikiForHost(host).orElseGet(() -> {
      if (host.indexOf(".") > 0) {
        // no wiki found based on the full host name, try to use the first part as the wiki name
        WikiReference subDomainWikiRef = new WikiReference(host.substring(0, host.indexOf(".")));
        if (wikiService.hasWiki(subDomainWikiRef)) {
          return subDomainWikiRef;
        }
      }
      return null;
    });
    // TODO virtual mode -> always main
    LOGGER.debug("determineWiki - {}", wikiRef);
    return Optional.ofNullable(wikiRef)
        .orElseThrow(() -> new XWikiException(XWikiException.MODULE_XWIKI,
            XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, "The wiki " + host + " does not exist"));
  }

  private void awaitWikiUpdate(WikiReference wikiRef) throws XWikiException {
    wikiUpdater.getFuture(wikiRef).ifPresent(rethrow(future -> {
      try {
        LOGGER.trace("awaitWikiUpdate - on {}", wikiRef);
        future.get(1, TimeUnit.HOURS); // TODO use tomcat connectionTimeout
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
