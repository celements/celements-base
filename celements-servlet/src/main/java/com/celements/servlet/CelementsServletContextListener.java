
package com.celements.servlet;

import static com.google.common.base.Preconditions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

import com.celements.spring.CelSpringConfig;
import com.celements.spring.context.CelSpringContext;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the {@link ServletContextListener}. Initializes the spring and xwiki
 * application context.
 */
// TODO extend ContextLoaderListener from spring-web
public class CelementsServletContextListener implements ServletContextListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelementsServletContextListener.class);

  private final AtomicReference<CelSpringContext> springAppContext = new AtomicReference<>();

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext servletCtx = sce.getServletContext();
    // Initialize Spring Context
    CelSpringContext springCtx = new CelSpringContext(getAdditionalSpringConfigs());
    springCtx.registerShutdownHook();
    checkState(springAppContext.compareAndSet(null, springCtx),
        "Spring Application Context already initialized");
    // Initialize XWiki Context
    springCtx.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletCtx);
    springCtx.getBean(ObservationManager.class)
        .notify(new ApplicationStartedEvent(), this);
    // Make the CM available in the Servlet Context -> TODO change to Spring Context
    servletCtx.setAttribute(ComponentManager.class.getName(),
        springCtx.getBean(ComponentManager.class));
  }

  /**
   * Entry point for adding additional configs like {@link CelSpringConfig}.
   */
  protected List<Class<?>> getAdditionalSpringConfigs() {
    return ImmutableList.of();
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    CelSpringContext springCtx = springAppContext.get();
    if (springCtx != null) {
      try {
        springCtx.getBean(ObservationManager.class)
            .notify(new ApplicationStoppedEvent(), this);
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed ApplicationStoppedEvent", exc);
      }
      try {
        Container xwikiContainer = springCtx.getBean(Container.class);
        springCtx.getBean(ApplicationContextListenerManager.class)
            .destroyApplicationContext(xwikiContainer.getApplicationContext());
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed destroyApplicationContext", exc);
      }
      springCtx.close();
      springAppContext.set(null);
    }
  }
}
