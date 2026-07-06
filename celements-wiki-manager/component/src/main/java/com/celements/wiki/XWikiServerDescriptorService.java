package com.celements.wiki;

import static com.celements.wiki.classes.XWikiServerClass.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiConstant.*;

import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiDescriptor.State;
import com.celements.wiki.WikiDescriptor.Visibility;
import com.celements.wiki.exception.WikiDescriptorException;
import com.celements.wiki.exception.WikiMissingException;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Service
public class XWikiServerDescriptorService implements WikiDescriptorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServerDescriptorService.class);

  public static final String DOC_NAME_PREFIX = "XWikiServer";

  private final ModelUtils modelUtils;
  private final IModelAccessFacade modelAccess;
  private final QueryWikiService wikiService;
  private final XWikiConfigSource xwikiCfg;

  @Inject
  public XWikiServerDescriptorService(
      ModelUtils modelUtils,
      IModelAccessFacade modelAccess,
      QueryWikiService wikiService,
      XWikiConfigSource xwikiCfg) {
    this.modelUtils = modelUtils;
    this.modelAccess = modelAccess;
    this.wikiService = wikiService;
    this.xwikiCfg = xwikiCfg;
  }

  @Override
  public DocumentReference getDescriptorDocRef(@NotNull WikiReference wikiRef) {
    WikiReference normWikiRef = modelUtils.normalizeWikiRef(wikiRef);
    checkNotNull(normWikiRef);
    return RefBuilder.create()
        .with(modelUtils.getMainWikiRef())
        .space(XWIKI_SPACE)
        .doc(DOC_NAME_PREFIX + StringUtils.capitalize(normWikiRef.getName()))
        .build(DocumentReference.class);
  }

  private XWikiDocument getDescriptorDoc(WikiReference wikiRef) throws WikiMissingException {
    try {
      return modelAccess.getDocument(getDescriptorDocRef(wikiRef));
    } catch (DocumentNotExistsException exc) {
      throw new WikiMissingException(wikiRef, exc);
    }
  }

  @Override
  public List<WikiDescriptor> getDescriptors(@NotNull WikiReference wikiRef)
      throws WikiMissingException {
    try {
      XWikiDocument descriptorDoc = getDescriptorDoc(wikiRef);
      return XWikiObjectFetcher.on(descriptorDoc).filter(CLASS_REF).stream()
          .map(obj -> XWikiObjectFetcher.on(descriptorDoc).filter(obj))
          .map(fetcher -> toDescriptor(wikiRef, fetcher))
          .toList();
    } catch (IllegalArgumentException exc) {
      throw new WikiMissingException(wikiRef, exc);
    }
  }

  private WikiDescriptor toDescriptor(WikiReference wikiRef, XWikiObjectFetcher fetcher) {
    var server = first(fetcher, FIELD_SERVER);
    var secure = Boolean.TRUE.equals(first(fetcher, FIELD_SECURE));
    return new WikiDescriptor(
        wikiRef,
        first(fetcher, FIELD_PRETTY_NAME),
        server,
        first(fetcher, FIELD_VISIBILITY),
        first(fetcher, FIELD_STATE),
        first(fetcher, FIELD_LANGUAGE),
        secure,
        first(fetcher, FIELD_OICD_ACTIVE),
        wikiService.toUri(secure ? 1 : 0, server).orElse(null));
  }

  private <T> T first(XWikiObjectFetcher fetcher, ClassField<T> field) {
    return fetcher.fetchField(field).findFirst().orElse(null);
  }

  @Override
  public boolean isOicdEnabled(@NotNull WikiReference wikiRef) {
    try {
      return getDescriptors(wikiRef).stream()
          .anyMatch(descriptor -> Boolean.TRUE.equals(descriptor.oicd()));
    } catch (WikiMissingException exc) {
      return false;
    }
  }

  @Override
  public void createDescriptor(WikiReference wikiRef, String host) throws WikiDescriptorException {
    XWikiDocument descriptorDoc = modelAccess.getOrCreateDocument(getDescriptorDocRef(wikiRef));
    var hasDescriptor = XWikiObjectFetcher.on(descriptorDoc)
        .filter(CLASS_REF)
        .filter(FIELD_SERVER, host)
        .exists();
    if (hasDescriptor) {
      LOGGER.debug("skip wiki descriptor creation for [{}], [{}], already exists",
          wikiRef.getName(), host);
      return;
    }
    addServerObj(descriptorDoc, wikiRef, host);
    descriptorDoc.setContent("#includeForm('XWiki.XWikiServerClassSheet')");
    try {
      modelAccess.saveDocument(descriptorDoc, "createDescriptor " + host);
    } catch (DocumentSaveException e) {
      throw new WikiDescriptorException(wikiRef, e);
    }
    LOGGER.info("created wiki descriptor for [{}]", wikiRef.getName());
  }

  @Override
  public void deleteDescriptors(@NotNull WikiReference wikiRef) throws WikiDescriptorException {
    try {
      modelAccess.deleteDocument(getDescriptorDocRef(wikiRef), true);
    } catch (DocumentDeleteException e) {
      throw new WikiDescriptorException(wikiRef, e);
    }
  }

  public BaseObject addServerObj(XWikiDocument doc, WikiReference wikiRef, String host) {
    return XWikiObjectEditor.on(doc)
        .filter(CLASS_REF)
        .filter(FIELD_PRETTY_NAME, wikiRef.getName())
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

  private boolean isSecure() {
    return xwikiCfg.getProperty("xwiki.url.protocol", "").equalsIgnoreCase("https");
  }

}
