package com.celements.init;

import static com.xpn.xwiki.XWikiConstant.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.wiki.event.WikiCreatedEvent;
import com.xpn.xwiki.XWikiException;

@Component
public class CentralWikiCreator implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CentralWikiCreator.class);

  private final XWikiProvider xwikiProvider;
  private final ApplicationEventPublisher eventPublisher;

  @Inject
  public CentralWikiCreator(
      XWikiProvider xwikiProvider,
      ApplicationEventPublisher eventPublisher) {
    this.xwikiProvider = xwikiProvider;
    this.eventPublisher = eventPublisher;
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
      xwiki.updateDatabase(CENTRAL_WIKI.getName(), true, true, null);
      // listeners create wiki config, classes, mandatory documents, ...
      eventPublisher.publishEvent(new WikiCreatedEvent(CENTRAL_WIKI));
      LOGGER.info("created central wiki [{}]", CENTRAL_WIKI);
    } catch (XWikiException xwe) {
      LOGGER.error("Failed to create central wiki [{}]", CENTRAL_WIKI, xwe);
    }
  }

}
