package com.celements.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CelStoppedEventPublisher implements ApplicationListener<ContextClosedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelStoppedEventPublisher.class);

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    LOGGER.info("Celements stopped from {}", event);
    event.getApplicationContext().publishEvent(new CelementsStoppedEvent(this));
  }

}
