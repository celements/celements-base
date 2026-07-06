package com.celements.wiki;

import static com.celements.wiki.classes.XWikiServerClass.*;
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

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.classes.XWikiServerClass;
import com.celements.wiki.classes.XWikiServerClass.State;
import com.celements.wiki.classes.XWikiServerClass.Visibility;
import com.celements.wiki.event.WikiCreatingEvent;
import com.celements.wiki.event.WikiDeletedEvent;
import com.celements.wiki.event.WikiEvent;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Creates/deletes wiki descriptors in main wiki on creation/deletion events.
 */
@Component
public class WikiDescriptorUpdater implements ApplicationListener<WikiEvent>, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDescriptorUpdater.class);

  private final ModelUtils modelUtils;
  private final IModelAccessFacade modelAccess;
  private final WikiService wikiService;
  private final XWikiConfigSource xwikiCfg;
  private final WikiDescriptorService descriptorService;

  @Inject
  public WikiDescriptorUpdater(
      ModelUtils modelUtils,
      IModelAccessFacade modelAccess,
      WikiService wikiService,
      WikiDescriptorService descriptorService,
      XWikiConfigSource xwikiCfg) {
    this.modelUtils = modelUtils;
    this.modelAccess = modelAccess;
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
      createWikiDescriptor(wikiEvent.getWiki());
    } else if (wikiEvent instanceof WikiDeletedEvent) {
      deleteWikiDescriptor(wikiEvent.getWiki());
    } else {
      LOGGER.debug("unsupported event [{}]", wikiEvent);
    }
  }

  private void createWikiDescriptor(WikiReference wikiRef) {
    XWikiDocument cfgDoc = modelAccess
        .getOrCreateDocument(descriptorService.getDescriptorDocRef(wikiRef));
    var editor = XWikiObjectEditor.on(cfgDoc).filter(XWikiServerClass.CLASS_REF);
    if (editor.fetch().exists()) {
      LOGGER.debug("skip wiki descriptor creation for [{}], already exists", wikiRef.getName());
      return;
    }
    var host = determineHost();
    if (modelUtils.isMainWiki(wikiRef)) {
      createWikiDescriptor(editor, wikiRef.getName(), host);
    }
    createWikiDescriptor(editor, wikiRef.getName(), wikiRef.getName() + "." + host);
    cfgDoc.setContent("#includeForm('XWiki.XWikiServerClassSheet')");
    try {
      modelAccess.saveDocument(cfgDoc, "WikiDescriptorUpdater");
      LOGGER.info("created wiki descriptor for [{}]", wikiRef.getName());
    } catch (DocumentSaveException dse) {
      LOGGER.error("failed to create wiki descriptor for [{}]", wikiRef, dse);
    }
  }

  private BaseObject createWikiDescriptor(XWikiObjectEditor editor, String name, String host) {
    return editor
        .filter(FIELD_PRETTY_NAME, name)
        .filter(FIELD_SERVER, host)
        .filter(FIELD_SECURE, isSecure())
        .filter(FIELD_VISIBILITY, Visibility.PUBLIC)
        .filter(FIELD_STATE, State.ACTIVE)
        .filter(FIELD_LANGUAGE, "en")
        .filter(FIELD_HOMEPAGE, "Content.WebHome")
        .filter(FIELD_IS_TEMPLATE, false)
        .filter(FIELD_OICD_ACTIVE, false)
        .createFirst();
  }

  private void deleteWikiDescriptor(WikiReference wikiRef) {
    try {
      modelAccess.deleteDocument(descriptorService.getDescriptorDocRef(wikiRef), true);
    } catch (DocumentDeleteException dde) {
      LOGGER.error("failed to delete wiki descriptor for [{}]", wikiRef, dde);
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

  private boolean isSecure() {
    return xwikiCfg.getProperty("xwiki.url.protocol", "").equalsIgnoreCase("https");
  }

}
