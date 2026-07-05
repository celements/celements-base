package com.celements.wiki.event;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.observation.ObservationManager;

@Component
public class WikiSpringXWikiEventConverter
    implements ApplicationListener<WikiEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(WikiSpringXWikiEventConverter.class);

  private final ObservationManager observationManager;

  @Inject
  public WikiSpringXWikiEventConverter(
      @Lazy ObservationManager observationManager) {
    this.observationManager = observationManager;
  }

  @Override
  public void onApplicationEvent(WikiEvent event) {
    var xwikiEvent = getXWikiEvent(event);
    if (xwikiEvent != null) {
      LOGGER.info("firing [{}]", xwikiEvent);
      observationManager.notify(xwikiEvent, this);
    } else {
      LOGGER.debug("unable to convert lifecycle event [{}] to an xwiki event", event);
    }
  }

  private org.xwiki.observation.event.Event getXWikiEvent(WikiEvent event) {
    if (event instanceof WikiCreatedEvent) {
      return new org.xwiki.bridge.event.WikiCreatedEvent(event.getWiki().getName());
    } else if (event instanceof WikiDeletedEvent) {
      return new org.xwiki.bridge.event.WikiDeletedEvent(event.getWiki().getName());
    } else {
      return null;
    }
  }

  @Override
  public int getOrder() {
    return 0; // default
  }

}
