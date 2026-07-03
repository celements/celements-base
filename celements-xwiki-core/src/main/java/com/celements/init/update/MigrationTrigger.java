package com.celements.init.update;

import static com.celements.execution.XWikiExecutionProp.*;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;

import com.celements.init.CelementsStartedEvent;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiMigrationManagerInterface;

@Component
public class MigrationTrigger implements ApplicationListener<CelementsStartedEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrationTrigger.class);

  private final Execution execution;
  private final WikiUpdater wikiUpdater;
  private final XWikiConfigSource xwikiCfg;
  private final ConfigurationSource cfgSrc;

  @Inject
  public MigrationTrigger(
      Execution execution,
      WikiUpdater wikiUpdater,
      XWikiConfigSource xwikiCfg,
      @Named("allproperties") ConfigurationSource cfgSrc) {
    this.execution = execution;
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
    if (!isMigrationEnabled()) {
      LOGGER.debug("skipping migrations");
      return;
    }
    try {
      LOGGER.info("triggering migrations");
      getMigrationManager().startMigrations(getXContext());
    } catch (XWikiException exc) {
      throw new IllegalStateException("Failed to start migrations", exc);
    }
    if (isExitAfterMigration()) {
      LOGGER.info("Waiting for all migrations to finish before exiting...");
      wikiUpdater.getShutdownFuture().whenComplete((v, e) -> {
        LOGGER.warn("Exiting because xwiki.store.migration.exitAfterEnd is set, good bye.");
        System.exit(0); // so brutal
      });
    }
  }

  private boolean isMigrationEnabled() {
    return Boolean.TRUE.equals(Optional
        .ofNullable(cfgSrc.getProperty("celements.init.migration", Boolean.class))
        .orElseGet(() -> "1".equals(xwikiCfg.getProperty("xwiki.store.migration", "0"))));
  }

  private boolean isExitAfterMigration() {
    return Boolean.TRUE.equals(Optional
        .ofNullable(cfgSrc.getProperty("celements.init.migration.exitAfterEnd", Boolean.class))
        .orElseGet(() -> "1".equals(xwikiCfg
            .getProperty("xwiki.store.migration.exitAfterEnd", "0"))));
  }

  private XWikiMigrationManagerInterface getMigrationManager() {
    String storeClass = xwikiCfg.getProperty("xwiki.store.migration.manager.class");
    try {
      return (XWikiMigrationManagerInterface) Class.forName(storeClass)
          .getConstructor(XWikiContext.class)
          .newInstance(getXContext());
    } catch (ReflectiveOperationException exc) {
      throw new IllegalStateException("Cannot instantiate migration manager: " + storeClass, exc);
    }
  }

  private XWikiContext getXContext() {
    return execution.getContext().get(XWIKI_CONTEXT).orElseThrow(IllegalStateException::new);
  }

}
