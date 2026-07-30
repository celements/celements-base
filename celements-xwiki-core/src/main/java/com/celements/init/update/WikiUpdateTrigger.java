package com.celements.init.update;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.init.CelementsStartedEvent;
import com.celements.wiki.WikiService;

@Component
public class WikiUpdateTrigger implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiUpdateTrigger.class);

  private final WikiService wikiService;
  private final WikiUpdater wikiUpdater;
  private final ConfigurationSource cfgSrc;

  @Inject
  public WikiUpdateTrigger(
      @Lazy WikiService wikiService,
      WikiUpdater wikiUpdater,
      @Named("allproperties") ConfigurationSource cfgSrc) {
    this.wikiService = wikiService;
    this.wikiUpdater = wikiUpdater;
    this.cfgSrc = cfgSrc;
  }

  @Override
  public int getOrder() {
    return -900;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    if (isUpdateWikisEnabled()) {
      LOGGER.info("triggering wiki updates");
      wikiService.streamAllWikis()
          .forEach(wikiUpdater::updateAsync);
    } else {
      LOGGER.trace("skipping wiki updates");
    }
  }

  private boolean isUpdateWikisEnabled() {
    return Boolean.TRUE.equals(cfgSrc.getProperty("celements.init.updatedatabases", Boolean.class));
  }

}
