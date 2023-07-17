package org.xwiki.container.servlet;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

@Component
public class ServletContainerDestroyListener
    implements ApplicationListener<ContextClosedEvent>, Ordered {

  public static final int ORDER = 2000; // very low precedence

  private final ServletContainerInitializer containerInitializer;

  public ServletContainerDestroyListener(ServletContainerInitializer containerInitializer) {
    this.containerInitializer = containerInitializer;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    containerInitializer.destroyApplicationContext();
  }

}
