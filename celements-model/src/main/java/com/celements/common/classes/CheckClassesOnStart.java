package com.celements.common.classes;

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
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class CheckClassesOnStart implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CheckClassesOnStart.class);

  private final XWikiConfigSource xwikiCfg;
  private final WikiService wikiService;
  private final IClassesCompositorComponent classesCompositor;
  private final ModelUtils modelUtils;
  private final Execution execution;

  @Inject
  public CheckClassesOnStart(
      XWikiConfigSource xwikiCfg,
      @Lazy WikiService wikiService,
      IClassesCompositorComponent classesCompositor,
      ModelUtils modelUtils,
      Execution execution) {
    this.xwikiCfg = xwikiCfg;
    this.wikiService = wikiService;
    this.classesCompositor = classesCompositor;
    this.modelUtils = modelUtils;
    this.execution = execution;
  }

  @Override
  public int getOrder() {
    return 100;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    LOGGER.trace("onApplicationEvent: {}", event);
    if (!"1".equals(xwikiCfg.getProperty("celements.classCollections.checkOnStart", "1"))) {
      return;
    }
    LOGGER.info("checking classes");
    var ectx = execution.getContext();
    XWikiUser userPrev = ectx.set(XWIKI_USER, new XWikiUser(SUPERADMIN_FQN, true));
    try {
      wikiService.streamAllWikis()
          .filter(wiki -> !modelUtils.isMainWiki(wiki))
          .forEach(classesCompositor::checkClasses);
    } finally {
      ectx.set(XWIKI_USER, userPrev);
    }
  }

}
