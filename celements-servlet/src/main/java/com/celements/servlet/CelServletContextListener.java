
package com.celements.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Implementation of the {@link ServletContextListener}. Initializes the spring and xwiki
 * application context.
 */
public class CelServletContextListener extends CelContextLoader
    implements ServletContextListener {

  @Override
  public void contextInitialized(ServletContextEvent event) {
    initAppContext(event.getServletContext());
  }

  @Override
  public void contextDestroyed(ServletContextEvent event) {
    closeAppContext(event.getServletContext());
  }
}
