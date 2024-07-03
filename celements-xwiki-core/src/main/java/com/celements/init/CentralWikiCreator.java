package com.celements.init;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.XWikiConstant.*;

import java.sql.SQLException;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.observation.ObservationManager;

import com.celements.execution.XWikiExecutionProp;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

@Component
public class CentralWikiCreator implements ApplicationListener<CelementsInitialisedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CentralWikiCreator.class);

  private final Execution execution;

  @Inject
  public CentralWikiCreator(Execution execution) {
    this.execution = execution;
  }

  @Override
  public int getOrder() {
    return -1000; // high precedence
  }

  @Override
  public void onApplicationEvent(CelementsInitialisedEvent event) {
    var eCtx = execution.getContext();
    var xwiki = eCtx.get(XWikiExecutionProp.XWIKI).orElseThrow();
    var context = eCtx.get(XWIKI_CONTEXT).orElseThrow();
    var wikiName = CENTRAL_WIKI.getName();
    try {
      xwiki.getStore().createWiki(wikiName, context);
      xwiki.updateDatabase(wikiName, true, true, context);
      LOGGER.info("created central wiki [{}]", wikiName);
      Utils.getComponent(ObservationManager.class)
          .notify(new WikiCreatedEvent(wikiName), wikiName, context);
    } catch (XWikiException xwe) {
      var cause = xwe.getCause();
      if ((cause instanceof SQLException) && cause.getMessage().contains("database exists")) {
        LOGGER.debug("skipped central wiki creation, already exists");
      } else {
        LOGGER.error("Failed to create central wiki [{}]", wikiName, xwe);
      }
    }
  }

}
