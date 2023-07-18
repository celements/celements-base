package com.celements.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CelStartedEventPublisher implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelStartedEventPublisher.class);

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    LOGGER.info("started {}", event);
    event.getApplicationContext().publishEvent(new CelementsStartedEvent(this));
  }

}
