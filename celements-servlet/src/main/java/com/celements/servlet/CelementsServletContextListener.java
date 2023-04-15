
package com.celements.servlet;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.ImmutableList.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationRuntimeException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.container.servlet.XWikiServlet;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

import com.celements.spring.context.CelSpringContext;

/**
 * Implementation of the {@link ServletContextListener}. Initializes the spring and xwiki
 * application context.
 */
// TODO extend ContextLoaderListener from spring-web
public class CelementsServletContextListener implements ServletContextListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelementsServletContextListener.class);

  private CelSpringContext springContext;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    ServletContext servletContext = event.getServletContext();
    // Initialize Spring Context
    checkState(springContext == null, "Spring Application Context already initialized");
    springContext = new CelSpringContext(loadSpringConfigs(servletContext));
    springContext.registerShutdownHook();
    // Initialize XWiki Context
    springContext.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletContext);
    springContext.getBean(ObservationManager.class)
        .notify(new ApplicationStartedEvent(), this);
    // Make the Spring Context available in the Servlet Context
    servletContext.setAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING, springContext);
  }

  protected List<Class<?>> loadSpringConfigs(ServletContext servletContext) {
    try {
      Configuration springProperties = new PropertiesConfiguration(servletContext.getResource(
          "/WEB-INF/spring.properties"));
      List<?> configNames = springProperties.getList("spring.configs");
      return configNames.stream()
          .map(Objects::toString)
          .map(rethrowFunction(Class::forName))
          .collect(toImmutableList());
    } catch (MalformedURLException | ConfigurationException | ClassNotFoundException exc) {
      throw new ConfigurationRuntimeException(exc);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (springContext != null) {
      try {
        springContext.getBean(ObservationManager.class)
            .notify(new ApplicationStoppedEvent(), this);
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed ApplicationStoppedEvent", exc);
      }
      try {
        Container xwikiContainer = springContext.getBean(Container.class);
        springContext.getBean(ApplicationContextListenerManager.class)
            .destroyApplicationContext(xwikiContainer.getApplicationContext());
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed destroyApplicationContext", exc);
      }
      springContext.close();
      springContext = null;
    }
  }
}
