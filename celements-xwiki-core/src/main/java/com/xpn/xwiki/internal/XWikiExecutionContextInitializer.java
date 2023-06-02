package com.xpn.xwiki.internal;

import java.time.Duration;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.celements.init.XWikiProvider;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;

@Component
public class XWikiExecutionContextInitializer implements ExecutionContextInitializer {

  @Inject
  private XWikiProvider wikiProvider;

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    try {
      XWiki xwiki = context.getProperty("noAwait", false)
          ? wikiProvider.get().orElse(null)
          : wikiProvider.await(Duration.ofHours(1));
      context.setProperty(XWiki.CONTEXT_KEY, xwiki);
    } catch (XWikiException xwe) {
      throw new ExecutionContextException("failed initializing XWiki", xwe);
    }
  }

}
