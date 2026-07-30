package com.celements.init;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;

@Component
@Profile("!test")
public class CelStartedEventPublisher implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelStartedEventPublisher.class);

  private final Execution execution;

  @Inject
  public CelStartedEventPublisher(Execution execution) {
    this.execution = execution;
  }

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    LOGGER.info("Celements started from {}", event);
    try {
      event.getApplicationContext().publishEvent(new CelementsStartedEvent(this));
    } finally {
      execution.removeContext();
    }
  }

}
