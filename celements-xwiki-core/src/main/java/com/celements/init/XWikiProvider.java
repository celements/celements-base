package com.celements.init;

import static com.google.common.base.Preconditions.*;

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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
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

  /**
   * @return the XWiki object (as in "the XWiki API") initialised by {@link XWikiBootstrap}.
   * @throws XWikiException
   *           if there was error in the bootstrap
   */
  @NotNull
  public XWiki get() throws XWikiException {
    if (getContext().getWiki() != null) {
      return getContext().getWiki();
    }
    XWiki xwiki = awaitXWikiBootstrap();
    checkNotNull(xwiki);
    getContext().setWiki(xwiki);
    return xwiki;
  }

  private XWiki awaitXWikiBootstrap() throws XWikiException {
    try {
      LOGGER.trace("awaitXWikiBootstrap");
      return getXWikiServletFuture().get(1, TimeUnit.HOURS);
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
    CompletableFuture<XWiki> future = (CompletableFuture<XWiki>) servletContext
        .getAttribute(XWikiBootstrap.XWIKI_SERVLET_CTX_KEY);
    checkState(future != null, "should not happen, are we before ApplicationStartedEvent?");
    return future;
  }

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
  }

}
