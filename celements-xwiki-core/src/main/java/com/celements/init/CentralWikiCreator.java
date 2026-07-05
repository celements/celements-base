package com.celements.init;

import static com.xpn.xwiki.XWikiConstant.*;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.init.wiki.WikiCreator;
import com.celements.init.wiki.WikiCreator.WikiCreationException;

@Component
public class CentralWikiCreator implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(CentralWikiCreator.class);

  private final WikiCreator wikiCreator;

  @Inject
  public CentralWikiCreator(WikiCreator wikiCreator) {
    this.wikiCreator = wikiCreator;
  }

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    try {
      wikiCreator.ensureWiki(CENTRAL_WIKI);
    } catch (WikiCreationException xwe) {
      LOGGER.error("Failed to create central wiki [{}]", CENTRAL_WIKI, xwe);
    }
  }

}
