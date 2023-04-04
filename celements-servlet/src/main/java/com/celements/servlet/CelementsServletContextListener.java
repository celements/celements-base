
package com.celements.servlet;

import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;

import com.celements.spring.context.CelementsAnnotationConfigApplicationContext;
import com.celements.spring.context.SpringContext;
import com.google.common.collect.ImmutableList;

/**
 * Implementation of the {@link ServletContextListener}. Initializes the spring and xwiki
 * application context.
 */
// TODO extend ContextLoaderListener from spring-web
public class CelementsServletContextListener implements ServletContextListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(CelementsServletContextListener.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    ServletContext servletCtx = sce.getServletContext();
    // Initialize Spring Context
    List<String> packages = ImmutableList.of("com.celements.spring"); // TODO from cfg?
    GenericApplicationContext springCtx = new CelementsAnnotationConfigApplicationContext(
        packages.toArray(new String[0]));
    SpringContext.INSTANCE.setContext(springCtx);
    // Initialize XWiki Context
    springCtx.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletCtx);
    springCtx.getBean(ObservationManager.class)
        .notify(new ApplicationStartedEvent(), this);
    // This is a temporary bridge to allow non XWiki components to lookup XWiki components.
    // We're putting the XWiki Component Manager instance in the Servlet Context so that it's
    // available in the XWikiAction class which in turn puts it into the XWikiContext instance.
    // Class that need to lookup then just need to get it from the XWikiContext instance.
    // This is of course not necessary for XWiki components since they just need to implement
    // the Composable interface to get access to the Component Manager or better they simply
    // need to declare their components requirements using the @Requirement annotation of the xwiki
    // component manager together with a private class member, for automatic injection by the CM on
    // init.
    // Update 2023.04: refactor clients to use SpringContext#get instead
    servletCtx.setAttribute(ComponentManager.class.getName(),
        springCtx.getBean(ComponentManager.class));
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    try {
      SpringContext.get().getBean(ObservationManager.class)
          .notify(new ApplicationStoppedEvent(), this);
    } catch (Exception exc) {
      LOGGER.error("contextDestroyed - failed ApplicationStoppedEvent", exc);
    }
    try {
      Container xwikiContainer = SpringContext.get().getBean(Container.class);
      SpringContext.get().getBean(ApplicationContextListenerManager.class)
          .destroyApplicationContext(xwikiContainer.getApplicationContext());
    } catch (Exception exc) {
      LOGGER.error("contextDestroyed - failed destroyApplicationContext", exc);
    }
  }
}
