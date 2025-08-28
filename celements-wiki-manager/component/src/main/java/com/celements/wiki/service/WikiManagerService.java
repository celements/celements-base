package com.celements.wiki.service;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiConstant.*;

import java.util.Optional;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiMissingException;
import com.celements.wiki.classes.XWikiServerClass;
import com.xpn.xwiki.doc.XWikiDocument;

@Service
public class WikiManagerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiManagerService.class);

  public static final String DOC_NAME_PREFIX = "XWikiServer";

  private final ModelUtils modelUtils;
  private final IModelAccessFacade modelAccess;

  @Inject
  public WikiManagerService(
      ModelUtils modelUtils,
      IModelAccessFacade modelAccess) {
    this.modelUtils = modelUtils;
    this.modelAccess = modelAccess;
  }

  @NotNull
  public DocumentReference getWikiConfigDocRef(@NotNull WikiReference wikiRef) {
    WikiReference normWikiRef = modelUtils.normalizeWikiRef(wikiRef);
    checkNotNull(normWikiRef);
    return RefBuilder.create()
        .with(modelUtils.getMainWikiRef())
        .space(XWIKI_SPACE)
        .doc(DOC_NAME_PREFIX
            + StringUtils.capitalize(normWikiRef.getName()))
        .build(DocumentReference.class);
  }

  public boolean isOicdEnabled(@NotNull WikiReference wikiRef) {
    return getWikiConfigOptional(wikiRef)
        .flatMap(fetcher -> fetcher.fetchField(XWikiServerClass.FIELD_OICD_ACTIVE)
            .findFirst())
        .orElse(false);
  }

  @NotNull
  private Optional<XWikiObjectFetcher> getWikiConfigOptional(@NotNull WikiReference wikiRef) {
    try {
      return Optional.of(getWikiConfig(wikiRef));
    } catch (WikiMissingException exp) {
      LOGGER.warn("no wiki found for {}", wikiRef, exp);
    }
    return Optional.empty();
  }

  @NotNull
  private XWikiObjectFetcher getWikiConfig(@NotNull WikiReference wikiRef)
      throws WikiMissingException {
    try {
      XWikiDocument cfgDoc = modelAccess.getDocument(getWikiConfigDocRef(wikiRef));
      LOGGER.debug("return object-fetcher for {}", defer(cfgDoc::getDocRef));
      return XWikiObjectFetcher.on(cfgDoc).filter(XWikiServerClass.CLASS_REF);
    } catch (DocumentNotExistsException exp) {
      throw new WikiMissingException(wikiRef, exp);
    }
  }

}
