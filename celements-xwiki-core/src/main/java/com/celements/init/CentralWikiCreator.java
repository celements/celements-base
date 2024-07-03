package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.XWikiConstant.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.ObservationManager;

import com.celements.execution.XWikiExecutionProp;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

@Component
public class CentralWikiCreator implements ApplicationListener<CelementsInitialisedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CentralWikiCreator.class);

  private final ExecutionContext eCtx;
  private final WikiService wikiService;

  @Inject
  public CentralWikiCreator(Execution execution, WikiService wikiService) {
    this.eCtx = execution.getContext();
    this.wikiService = wikiService;
  }

  @Override
  public int getOrder() {
    return -1000; // high precedence
  }

  @Override
  public void onApplicationEvent(CelementsInitialisedEvent event) {
    if (!wikiService.hasWiki(CENTRAL_WIKI)) {
      XWiki xwiki = eCtx.get(XWikiExecutionProp.XWIKI).orElseThrow();
      XWikiContext context = eCtx.get(XWIKI_CONTEXT).orElseThrow();
      String wikiName = CENTRAL_WIKI.getName();
      try {
        xwiki.getStore().createWiki(wikiName, context);
        xwiki.updateDatabase(wikiName, true, true, context);
        Utils.getComponent(ObservationManager.class)
            .notify(new WikiCreatedEvent(wikiName), wikiName, context);
      } catch (XWikiException xwe) {
        LOGGER.error("Failed to create central wiki [{}]", wikiName, xwe);
      }
      LOGGER.info("created central wiki [{}]", wikiName);
    } else {
      LOGGER.debug("skipped central wiki creation, already exists");
    }
  }

}
