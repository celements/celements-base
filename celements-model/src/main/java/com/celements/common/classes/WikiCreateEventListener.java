package com.celements.common.classes;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import com.celements.wiki.event.WikiCreatingEvent;

@Component
public class WikiCreateEventListener implements ApplicationListener<WikiCreatingEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiCreateEventListener.class);

  private IClassesCompositorComponent classesCompositor;

  @Inject
  public WikiCreateEventListener(IClassesCompositorComponent classesCompositor) {
    this.classesCompositor = classesCompositor;
  }

  @Override
  public int getOrder() {
    return -200;
  }

  @Override
  public void onApplicationEvent(WikiCreatingEvent event) {
    String database = event.getWiki().getName();
    LOGGER.debug("received WikiCreatingEvent for database '{}'", database);
    classesCompositor.checkClasses(event.getWiki());
  }

}
