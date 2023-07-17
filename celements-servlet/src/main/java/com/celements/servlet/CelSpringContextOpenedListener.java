package com.celements.servlet;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.celements.init.CelementsStartedEvent;

@Component
@Profile("!test")
public class CelSpringContextOpenedListener implements ApplicationListener<ContextRefreshedEvent> {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelSpringContextOpenedListener.class);

  @Override
  public void onApplicationEvent(ContextRefreshedEvent event) {
    LOGGER.debug("opening {}", event);
    ApplicationContext context = event.getApplicationContext();
    if (context instanceof WebApplicationContext) {
      ServletContext servletContext = ((WebApplicationContext) context).getServletContext();
      context.getBean(ServletContainerInitializer.class)
          .initializeApplicationContext(servletContext);
    }
    context.publishEvent(new CelementsStartedEvent(this));
    LOGGER.info("opened {}", event);
  }

}
