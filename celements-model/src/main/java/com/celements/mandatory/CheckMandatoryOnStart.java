package com.celements.mandatory;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.user.api.XWikiRightService.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;

import com.celements.init.CelementsStartedEvent;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class CheckMandatoryOnStart implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckMandatoryOnStart.class);

  private final XWikiConfigSource xwikiCfg;
  private final WikiService wikiService;
  private final IMandatoryDocumentCompositorRole mandatoryCompositor;
  private final Execution execution;

  @Inject
  public CheckMandatoryOnStart(
      XWikiConfigSource xwikiCfg,
      @Lazy WikiService wikiService,
      IMandatoryDocumentCompositorRole mandatoryCompositor,
      Execution execution) {
    this.xwikiCfg = xwikiCfg;
    this.wikiService = wikiService;
    this.mandatoryCompositor = mandatoryCompositor;
    this.execution = execution;
  }

  @Override
  public int getOrder() {
    return 200;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    LOGGER.trace("onApplicationEvent: {}", event);
    if (!"1".equals(xwikiCfg.getProperty("celements.mandatory.checkOnStart", "1"))) {
      return;
    }
    LOGGER.info("checking mandatory documents");
    var ectx = execution.getContext();
    XWikiUser userPrev = ectx.set(XWIKI_USER, new XWikiUser(SUPERADMIN_FQN, true));
    try {
      wikiService.streamAllWikis()
          .forEach(mandatoryCompositor::checkAllMandatoryDocuments);
    } finally {
      ectx.set(XWIKI_USER, userPrev);
    }
  }

}
