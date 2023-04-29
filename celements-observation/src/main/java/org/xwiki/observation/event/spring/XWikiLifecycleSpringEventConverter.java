package org.xwiki.observation.event.spring;

import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.event.Event;

import com.celements.servlet.CelementsLifecycleEvent;

@Component
public class XWikiLifecycleSpringEventConverter
    implements ApplicationListener<CelementsLifecycleEvent> {

  private final ObservationManager observationManager;

  @Inject
  public XWikiLifecycleSpringEventConverter(ObservationManager observationManager) {
    this.observationManager = observationManager;
  }

  @Override
  public void onApplicationEvent(CelementsLifecycleEvent event) {
    Event xwikiEvent = (event.getType() == CelementsLifecycleEvent.State.STARTED)
        ? new ApplicationStartedEvent()
        : new ApplicationStoppedEvent();
    observationManager.notify(xwikiEvent, this);
  }

}
