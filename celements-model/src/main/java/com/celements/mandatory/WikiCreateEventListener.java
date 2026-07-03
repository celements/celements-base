package com.celements.mandatory;

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

  private IMandatoryDocumentCompositorRole mandatoryDocCmp;

  @Inject
  public WikiCreateEventListener(IMandatoryDocumentCompositorRole mandatoryDocCmp) {
    this.mandatoryDocCmp = mandatoryDocCmp;
  }

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public void onApplicationEvent(WikiCreatingEvent event) {
    String database = event.getWiki().getName();
    LOGGER.debug("received WikiCreatingEvent for database '{}'", database);
    mandatoryDocCmp.checkAllMandatoryDocuments(event.getWiki());
  }

}
