package com.xpn.xwiki.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

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

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    XWikiContext xcontext = (XWikiContext) context.getProperty(XWikiContext.EXEC_CONTEXT_KEY);
    if (xcontext == null) {
      XWikiContext stubContext = stubContextProvider.createStubContext();
      context.setProperty(XWikiContext.EXEC_CONTEXT_KEY, stubContext);
    }
  }

}
