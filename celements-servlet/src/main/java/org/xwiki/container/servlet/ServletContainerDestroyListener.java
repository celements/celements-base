package org.xwiki.container.servlet;

import javax.inject.Inject;

import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.init.CelementsStoppedEvent;

@Component
public class ServletContainerDestroyListener
    implements ApplicationListener<CelementsStoppedEvent>, Ordered {

  public static final int ORDER = 2000; // very low precedence

  private final ServletContainerInitializer containerInitializer;

  @Inject
  public ServletContainerDestroyListener(ServletContainerInitializer containerInitializer) {
    this.containerInitializer = containerInitializer;
  }

  @Override
  public int getOrder() {
    return ORDER;
  }

  @Override
  public void onApplicationEvent(CelementsStoppedEvent event) {
    containerInitializer.destroyApplicationContext();
  }

}
