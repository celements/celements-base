package com.celements.init;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class CelementsInitialisedEventPublisher
    implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelementsInitialisedEventPublisher.class);

  private final WikiUpdater wikiUpdater;
  private final ApplicationEventPublisher eventPublisher;

  @Inject
  public CelementsInitialisedEventPublisher(
      WikiUpdater wikiUpdater,
      ApplicationEventPublisher eventPublisher) {
    this.wikiUpdater = wikiUpdater;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    LOGGER.info("awaiting all wiki updates...");
    wikiUpdater.shutdown().join();
    LOGGER.info("Celements initialised");
    eventPublisher.publishEvent(new CelementsInitialisedEvent(event));
  }

}
