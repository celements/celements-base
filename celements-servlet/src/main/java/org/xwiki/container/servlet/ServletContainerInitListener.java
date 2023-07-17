package org.xwiki.container.servlet;

import javax.servlet.ServletContext;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.init.CelementsStartedEvent;

@Component
public class ServletContainerInitListener
    implements ApplicationListener<CelementsStartedEvent>, Ordered {

  public static final int ORDER = -2000; // very high precedence

  private final ServletContext servletContext;
  private final ServletContainerInitializer containerInitializer;

  public ServletContainerInitListener(ServletContext servletContext,
      ServletContainerInitializer containerInitializer) {
    this.servletContext = servletContext;
    this.containerInitializer = containerInitializer;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    containerInitializer.initializeApplicationContext(servletContext);
  }

}
