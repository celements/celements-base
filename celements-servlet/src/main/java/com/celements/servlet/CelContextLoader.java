package com.celements.servlet;

import static com.celements.common.MoreObjectsCel.*;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.celements.servlet.CelementsLifecycleEvent.State;

public class CelContextLoader extends ContextLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelContextLoader.class);

  public void initAppContext(ServletContext servletContext) {
    WebApplicationContext context = initWebApplicationContext(servletContext);
    context.getBean(ServletContainerInitializer.class)
        .initializeApplicationContext(servletContext);
    context.publishEvent(new CelementsLifecycleEvent(this, State.STARTED));
  }

  @Override
  protected ConfigurableWebApplicationContext createWebApplicationContext(
      ServletContext servletContext) {
    return new CelSpringWebContext();
  }

  public void closeAppContext(ServletContext servletContext) {
    ConfigurableApplicationContext context = getSpringContext(servletContext);
    if (context != null) {
      try {
        context.publishEvent(new CelementsLifecycleEvent(this, State.STOPPED));
        context.getBean(ServletContainerInitializer.class)
            .destroyApplicationContext();
      } catch (Exception exc) {
        LOGGER.error("contextDestroyed - failed closeXWikiAppContext", exc);
        tryCast(context, CelSpringWebContext.class)
            .map(c -> c.firstClosingStackTrace.get())
            .ifPresent(stackTrace -> LOGGER.error("contextDestroyed - spring app context "
                + "was closed prematurely by:", stackTrace));
      } finally {
        context = null;
        closeWebApplicationContext(servletContext);
      }
    }
  }

  private ConfigurableApplicationContext getSpringContext(ServletContext servletContext) {
    return (ConfigurableApplicationContext) servletContext
        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
  }
}
