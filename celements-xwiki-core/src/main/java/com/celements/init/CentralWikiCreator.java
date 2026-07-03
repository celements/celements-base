package com.celements.init;

import static com.xpn.xwiki.XWikiConstant.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.init.wiki.WikiCreationCoordinator;
import com.xpn.xwiki.XWikiException;

@Component
public class CentralWikiCreator implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CentralWikiCreator.class);

  private final XWikiProvider xwikiProvider;
  private final WikiCreationCoordinator lifecyclePublisher;

  @Inject
  public CentralWikiCreator(
      XWikiProvider xwikiProvider,
      WikiCreationCoordinator lifecyclePublisher) {
    this.xwikiProvider = xwikiProvider;
    this.lifecyclePublisher = lifecyclePublisher;
  }

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    var xwiki = xwikiProvider.get().orElseThrow();
    try {
      if (xwiki.getStore().existsWiki(CENTRAL_WIKI)) {
        LOGGER.debug("skipped central wiki creation [{}], already exists", CENTRAL_WIKI);
        return;
      }
      xwiki.getStore().createWiki(CENTRAL_WIKI);
      var force = true; // force schema update
      var initClasses = false; // init classes are created by WikiMandatoryClassesListener
      xwiki.updateDatabase(CENTRAL_WIKI.getName(), force, initClasses, null);
      // listeners create wiki descriptor, classes, mandatory documents, ...
      lifecyclePublisher.publishCreated(CENTRAL_WIKI);
      LOGGER.info("created central wiki [{}]", CENTRAL_WIKI);
    } catch (XWikiException xwe) {
      LOGGER.error("Failed to create central wiki [{}]", CENTRAL_WIKI, xwe);
    }
  }

}
