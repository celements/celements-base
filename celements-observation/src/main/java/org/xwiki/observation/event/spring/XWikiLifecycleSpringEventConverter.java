package org.xwiki.observation.event.spring;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

import com.celements.init.CelementsLifecycleEvent;
import com.celements.init.CelementsStartedEvent;
import com.celements.init.CelementsStoppedEvent;

@Component
@Lazy // otherwise ObservationManager will be eagerly initialised causing issues
public class XWikiLifecycleSpringEventConverter
    implements ApplicationListener<CelementsLifecycleEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(XWikiLifecycleSpringEventConverter.class);

  private final ObservationManager observationManager;

  @Inject
  public XWikiLifecycleSpringEventConverter(ObservationManager observationManager) {
    this.observationManager = observationManager;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    Event xwikiEvent = getXWikiEvent(event);
    if (xwikiEvent != null) {
      LOGGER.info("firing [{}]", xwikiEvent);
      observationManager.notify(xwikiEvent, this);
    } else {
      LOGGER.info("unable to convert lifecycle event [{}] to an xwiki event", event);
    }
  }

  private Event getXWikiEvent(CelementsLifecycleEvent event) {
    switch (event.getState()) {
      case CelementsStartedEvent.STATE:
        return new ApplicationStartedEvent();
      case CelementsStoppedEvent.STATE:
        return new ApplicationStoppedEvent();
      default:
        return null;
    }
  }

  @Override
  public int getOrder() {
    return 0; // default
  }

}
