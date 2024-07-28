package com.celements.init;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWikiConfigSource;

@Component
public class MigrationTrigger implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationTrigger.class);

  private final WikiUpdater wikiUpdater;
  private final XWikiConfigSource xwikiCfg;
  private final ConfigurationSource cfgSrc;

  @Inject
  public MigrationTrigger(
      WikiUpdater wikiUpdater,
      XWikiConfigSource xwikiCfg,
      @Named("allproperties") ConfigurationSource cfgSrc) {
    this.wikiUpdater = wikiUpdater;
    this.xwikiCfg = xwikiCfg;
    this.cfgSrc = cfgSrc;
  }

  @Override
  public int getOrder() {
    return 10000;
  }

  @Override
  public void onApplicationEvent(CelementsStartedEvent event) {
    if (isMigrationEnabled()) {
      LOGGER.info("triggering migrations");
      wikiUpdater.runAllMigrationsAsync();
    } else {
      LOGGER.trace("skipping migrations");
    }
  }

  private boolean isMigrationEnabled() {
    return Boolean.TRUE.equals(Optional
        .ofNullable(cfgSrc.getProperty("celements.init.migration", Boolean.class))
        .orElseGet(() -> "1".equals(xwikiCfg.getProperty("xwiki.store.migration", "0"))));
  }

}
