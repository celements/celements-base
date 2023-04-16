package com.celements.servlet;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.ImmutableList.*;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;

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

public class CelContextLoader {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelContextLoader.class);

  private static final AtomicReference<CelSpringContext> springContext = new AtomicReference<>();

  public void initAppContext(ServletContext servletContext) {
    initSpringAppContext(servletContext);
    initXWikiAppContext(servletContext);
  }

  private void initSpringAppContext(ServletContext servletContext) {
    checkState(springContext.get() == null, "Spring Application Context already initialized");
    CelSpringContext ctx = new CelSpringContext(loadSpringConfigs(servletContext));
    ctx.registerShutdownHook();
    springContext.set(ctx);
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
    CelSpringContext ctx = springContext.get();
    ctx.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletContext);
    ctx.getBean(ObservationManager.class)
        .notify(new ApplicationStartedEvent(), this);
    servletContext.setAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING, ctx);
  }

  public void closeAppContext(ServletContext servletContext) {
    servletContext.removeAttribute(XWikiServlet.SERVLET_CTX_KEY_SPRING);
    CelSpringContext ctx = springContext.getAndSet(null);
    if (ctx != null) {
      try {
        closeXWikiAppContext(ctx);
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed closeXWikiAppContext", exc);
      } finally {
        ctx.close();
      }
    }
  }

  private void closeXWikiAppContext(CelSpringContext ctx) {
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

  public static CelSpringContext getSpringContext() {
    CelSpringContext ctx = springContext.get();
    checkState(ctx != null);
    return ctx;
  }
}
