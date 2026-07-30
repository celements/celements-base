package com.celements.init.wiki;

import static com.celements.execution.XWikiExecutionProp.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;

import com.celements.wiki.event.WikiCreatingEvent;
import com.xpn.xwiki.XWikiException;

@Component
public class WikiMandatoryClassesListener
    implements ApplicationListener<WikiCreatingEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WikiMandatoryClassesListener.class);

  private final Execution execution;

  @Inject
  public WikiMandatoryClassesListener(Execution execution) {
    this.execution = execution;
  }

  @Override
  public int getOrder() {
    return -1000;
  }

  @Override
  public void onApplicationEvent(WikiCreatingEvent event) {
    String db = event.getWiki().getName();
    var xwiki = execution.getContext().get(XWIKI).orElseThrow();
    var wikiPrev = execution.getContext().get(WIKI).orElseThrow();
    try {
      execution.getContext().set(WIKI, event.getWiki());
      LOGGER.info("initialising mandatory XWiki classes for db [{}]", db);
      xwiki.initializeMandatoryClasses(null);
    } catch (XWikiException xwe) {
      throw new IllegalStateException("failed to initialise classes for [" + db + "]", xwe);
    } finally {
      execution.getContext().set(WIKI, wikiPrev);
    }
  }

}
