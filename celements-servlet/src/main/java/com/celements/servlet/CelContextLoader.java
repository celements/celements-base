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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.container.servlet.XWikiServlet;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

public class CelContextLoader extends ContextLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelContextLoader.class);

  public void initAppContext(ServletContext servletContext) {
    WebApplicationContext context = initWebApplicationContext(servletContext);
    servletContext.setAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING, context);
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
    context.getBean(ObservationManager.class)
        .notify(new ApplicationStartedEvent(), this);
  }

  public void closeAppContext(ServletContext servletContext) {
    ConfigurableApplicationContext context = getSpringContext(servletContext);
    servletContext.removeAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING);
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

  private void closeXWikiAppContext(ConfigurableApplicationContext ctx) {
    try {
      ctx.getBean(ObservationManager.class)
          .notify(new ApplicationStoppedEvent(), this);
    } catch (Exception exc) {
      LOGGER.error("contextDestroyed - failed ApplicationStoppedEvent", exc);
    } finally {
      Container xwikiContainer = ctx.getBean(Container.class);
      ctx.getBean(ApplicationContextListenerManager.class)
          .destroyApplicationContext(xwikiContainer.getApplicationContext());
    }
  }

  private ConfigurableApplicationContext getSpringContext(ServletContext servletContext) {
    return (ConfigurableApplicationContext) servletContext
        .getAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING);
  }
}
