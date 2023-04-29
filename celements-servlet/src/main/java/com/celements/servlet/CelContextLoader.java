package com.celements.servlet;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.google.common.collect.ImmutableList.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.celements.servlet.CelementsLifecycleEvent.State;

public class CelContextLoader extends ContextLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelContextLoader.class);

  public void initAppContext(ServletContext servletContext) {
    initWebApplicationContext(servletContext);
    initXWikiAppContext(servletContext);
  }

  @Override
  protected ConfigurableWebApplicationContext createWebApplicationContext(
      ServletContext servletContext) {
    return new CelSpringWebContext(loadSpringConfigs(servletContext));
  }

  protected List<Class<?>> loadSpringConfigs(ServletContext servletContext) {
    try {
      Configuration springProperties = new PropertiesConfiguration(servletContext.getResource(
          "/WEB-INF/spring.properties"));
      List<?> configNames = springProperties.getList("spring.config");
      return configNames.stream()
          .map(Objects::toString)
          .map(rethrowFunction(Class::forName))
          .collect(toImmutableList());
    } catch (MalformedURLException | ConfigurationException | ClassNotFoundException exc) {
      throw new ConfigurationRuntimeException(exc);
    }
  }

  private void initXWikiAppContext(ServletContext servletContext) {
    ConfigurableApplicationContext context = getSpringContext(servletContext);
    context.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletContext);
    context.getBean(ApplicationEventPublisher.class)
        .publishEvent(new CelementsLifecycleEvent(this, State.STARTED));
  }

  public void closeAppContext(ServletContext servletContext) {
    ConfigurableApplicationContext context = getSpringContext(servletContext);
    if (context != null) {
      try {
        closeXWikiAppContext(context);
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed closeXWikiAppContext", exc);
      } finally {
        context = null;
        closeWebApplicationContext(servletContext);
      }
    }
  }

  private void closeXWikiAppContext(ConfigurableApplicationContext context) {
    try {
      context.getBean(ApplicationEventPublisher.class)
          .publishEvent(new CelementsLifecycleEvent(this, State.STOPPED));
    } catch (Exception exc) {
      LOGGER.error("contextDestroyed - failed ApplicationStoppedEvent", exc);
    } finally {
      context.getBean(ServletContainerInitializer.class)
          .destroyApplicationContext();
    }
  }

  private ConfigurableApplicationContext getSpringContext(ServletContext servletContext) {
    return (ConfigurableApplicationContext) servletContext
        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  }
}
