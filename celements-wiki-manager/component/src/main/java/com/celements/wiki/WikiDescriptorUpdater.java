package com.celements.wiki;

import static com.xpn.xwiki.XWikiConstant.*;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.util.ModelUtils;
import com.celements.wiki.event.WikiCreatingEvent;
import com.celements.wiki.event.WikiDeletedEvent;
import com.celements.wiki.event.WikiEvent;
import com.celements.wiki.exception.WikiDescriptorException;
import com.xpn.xwiki.XWikiConfigSource;

/**
 * Creates/deletes wiki descriptors in main wiki on creation/deletion events.
 */
@Component
public class WikiDescriptorUpdater implements ApplicationListener<WikiEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDescriptorUpdater.class);

  private final ModelUtils modelUtils;
  private final WikiService wikiService;
  private final WikiDescriptorService descriptorService;
  private final XWikiConfigSource xwikiCfg;

  @Inject
  public WikiDescriptorUpdater(
      ModelUtils modelUtils,
      WikiService wikiService,
      WikiDescriptorService descriptorService,
      XWikiConfigSource xwikiCfg) {
    this.modelUtils = modelUtils;
    this.wikiService = wikiService;
    this.descriptorService = descriptorService;
    this.xwikiCfg = xwikiCfg;
  }

  @Override
  public int getOrder() {
    return -800; // after XWikiServerClass creation in XWiki#initializeMandatoryClasses
  }

  @Override
  public void onApplicationEvent(WikiEvent wikiEvent) {
    if (wikiEvent instanceof WikiCreatingEvent) {
      createDescriptors(wikiEvent.getWiki());
    } else if (wikiEvent instanceof WikiDeletedEvent) {
      deleteDescriptors(wikiEvent.getWiki());
    } else {
      LOGGER.debug("unsupported event [{}]", wikiEvent);
    }
  }

  private void createDescriptors(WikiReference wikiRef) {
    var host = determineHost();
    try {
      var hostPrefix = wikiRef.getName();
      if (modelUtils.isMainWiki(wikiRef)) {
        descriptorService.createDescriptor(wikiRef, host);
        hostPrefix = xwikiCfg.getMainWikiName(); // prefix 'main' and not 'xwiki'
      }
      descriptorService.createDescriptor(wikiRef, hostPrefix + "." + host);
    } catch (WikiDescriptorException exc) {
      LOGGER.error("failed to create wiki descriptors for [{}]", wikiRef, exc);
    }
  }

  private void deleteDescriptors(WikiReference wikiRef) {
    try {
      descriptorService.deleteDescriptors(wikiRef);
    } catch (WikiDescriptorException exc) {
      LOGGER.error("failed to delete wiki descriptors for [{}]", wikiRef, exc);
    }
  }

  private String determineHost() {
    return Optional
        // 1. check environment variable
        .ofNullable(System.getenv("SERVER_HOST"))
        // 2. check main wiki config (XWikiServer object)
        .or(() -> wikiService.streamUrisForWiki(MAIN_WIKI).findFirst()
            .map(URI::getHost)
            .map(host -> host.replace(xwikiCfg.getMainWikiName() + ".", "")))
        // 3. fallback to localhost
        .orElse("localhost");
  }

}
