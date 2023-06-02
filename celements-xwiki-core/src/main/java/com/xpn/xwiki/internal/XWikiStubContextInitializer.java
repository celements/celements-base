package com.xpn.xwiki.internal;

import java.util.Optional;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * An automatic XWikiContext stub injecter for ExecutionContext for daemons unable to create a
 * proper XWikiContext (no real request information).
 *
 * @see XWikiStubContextProvider
 * @since 2.0M3
 */
@Component("XWikiStubContextInitializer")
public class XWikiStubContextInitializer implements ExecutionContextInitializer {

  @Requirement
  private XWikiStubContextProvider stubContextProvider;

  @Inject
  private XWikiProvider wikiProvider;

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    XWikiContext xcontext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    if (xcontext == null) {
      XWikiContext stubContext = stubContextProvider.createStubContext();
      context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, stubContext);
    }
    Optional.ofNullable(wikiProvider)
        .flatMap(XWikiProvider::get)
        .ifPresent(xwiki -> context.setProperty(XWiki.EXECUTION_CONTEXT_KEY, xwiki));
  }
}
