package com.celements.model.classes;

import static com.celements.configuration.ConfigSourceUtils.*;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.WikiReference;

import com.celements.configuration.CelementsPropertiesConfigurationSource;
import com.celements.model.context.ModelContext;
import com.google.common.collect.ImmutableList;

public abstract class AbstractClassPackage implements ClassPackage {

  @Deprecated
  protected Logger LOGGER = LoggerFactory.getLogger(ClassPackage.class);
  protected Logger logger = LoggerFactory.getLogger(this.getClass());

  @Requirement("xwikiproperties")
  protected ConfigurationSource xwikiPropertiesSource;

  @Requirement(CelementsPropertiesConfigurationSource.NAME)
  protected ConfigurationSource celementsPropertiesSource;

  @Requirement("wiki")
  protected ConfigurationSource wikiPreferencesSource;

  @Requirement
  protected ConfigurationSource configSrc;

  @Requirement
  protected ModelContext context;

  @Override
  public boolean isActivated() {
    boolean activated = getActiveWikis().contains(context.getWikiRef())
        || streamProperty(CFG_SRC_KEY, getConfigSources()).anyMatch(getName()::equals);
    logger.debug("{}: {}", getName(), activated ? "activated" : "deactivated");
    return activated;
  }

  /**
   * may be overridden to hardcode active wikis, useful for e.g. customizings
   */
  protected List<WikiReference> getActiveWikis() {
    return Collections.emptyList();
  }

  /**
   * may be overridden to change order or add custom config sources, e.g. project specific ones
   */
  protected List<ConfigurationSource> getConfigSources() {
    // may be read from the default source when CompositeConfigurationSource#sources is exposed
    return ImmutableList.of(
        xwikiPropertiesSource, // first check xwiki.properties
        celementsPropertiesSource, // then check celements.properties
        wikiPreferencesSource // last check XWikiPreferences of current wiki
    );
  }

}
