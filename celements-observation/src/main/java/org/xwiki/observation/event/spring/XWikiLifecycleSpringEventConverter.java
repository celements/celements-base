package org.xwiki.observation.event.spring;

import javax.inject.Inject;

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

@Component
@Lazy // otherwise ObservationManager will be eagerly initialised causing issues
public class XWikiLifecycleSpringEventConverter
    implements ApplicationListener<CelementsLifecycleEvent>, Ordered {

  private final ObservationManager observationManager;

  @Inject
  public XWikiLifecycleSpringEventConverter(ObservationManager observationManager) {
    this.observationManager = observationManager;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    Event xwikiEvent = (event.getState().equals(CelementsStartedEvent.STATE))
        ? new ApplicationStartedEvent()
        : new ApplicationStoppedEvent();
    observationManager.notify(xwikiEvent, this);
  }

  @Override
  public int getOrder() {
    return 0; // default
  }

}
