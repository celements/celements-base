package com.xpn.xwiki.internal;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.XWiki;

@Component
public class XWikiExecutionContextInitializer implements ExecutionContextInitializer {

  public static final String CTX_NO_AWAIT_KEY = "XWikiExecutionContextInitializer.noAwait";

  @Inject
  private XWikiProvider wikiProvider;

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    try {
      XWiki xwiki = Boolean.TRUE.equals(context.getProperty(CTX_NO_AWAIT_KEY))
          ? wikiProvider.get().orElse(null)
          : wikiProvider.await(Duration.ofHours(1));
      context.setProperty(XWiki.EXEC_CONTEXT_KEY, xwiki);
    } catch (ExecutionException xwe) {
      throw new ExecutionContextException("failed initializing XWiki", xwe);
    }
  }

}
