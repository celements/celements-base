package com.xpn.xwiki.internal;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.celements.execution.XWikiExecutionProp;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * An automatic XWikiContext stub injecter for ExecutionContext for daemons unable to create a
 * proper XWikiContext (no real request information).
 *
 * @see XWikiStubContextProvider
 * @since 2.0M3
 */
@Component
public class XWikiStubContextInitializer implements ExecutionContextInitializer {

  private final XWikiStubContextProvider provider;

  @Inject
  public XWikiStubContextInitializer(XWikiStubContextProvider provider) {
    this.provider = provider;
  }

  @Override
  public void initialize(ExecutionContext context, ExecutionContext source) 
      throws ExecutionContextException {
    context.computeIfAbsent(XWikiExecutionProp.XWIKI_CONTEXT,
        () -> provider.createStubContext(context));
  }

}
