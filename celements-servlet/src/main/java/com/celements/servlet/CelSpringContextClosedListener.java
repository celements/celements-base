package com.celements.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.celements.servlet.CelementsLifecycleEvent.State;

@Component
@Profile("!test")
public class CelSpringContextClosedListener implements ApplicationListener<ContextClosedEvent> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelSpringContextClosedListener.class);

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    LOGGER.debug("closing {}", event);
    ApplicationContext context = event.getApplicationContext();
    context.publishEvent(new CelementsLifecycleEvent(this, State.STOPPED));
    if (context instanceof WebApplicationContext) {
      context.getBean(ServletContainerInitializer.class)
          .destroyApplicationContext();
    }
    LOGGER.info("closed {}", event);
  }

}
