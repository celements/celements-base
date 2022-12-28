package com.celements.model.access;

import static com.celements.model.access.IModelAccessFacade.*;

import java.util.Date;
import java.util.Optional;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class DefaultXWikiDocumentCreator implements XWikiDocumentCreator {

  @Requirement
  private ModelContext context;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private CoreConfiguration coreCfg;

  @Requirement
  private ComponentManager componentManager;

  @Override
  public XWikiDocument createWithoutDefaults(DocumentReference docRef, String lang) {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setNew(true);
    lang = modelUtils.normalizeLang(lang);
    doc.setLanguage(lang);
    doc.setDefaultLanguage(DEFAULT_LANG);
    doc.setTranslation(DEFAULT_LANG.equals(lang) ? 0 : 1);
    Date creationDate = new Date();
    doc.setCreationDate(creationDate);
    doc.setContentUpdateDate(creationDate);
    doc.setDate(creationDate);
    doc.setCreator(context.getUserName());
    doc.setAuthor(context.getUserName());
    doc.setContentAuthor(context.getUserName());
    doc.setContent("");
    doc.setContentDirty(true);
    doc.setMetaDataDirty(true);
    doc.setOriginalDocument(new XWikiDocument(doc.getDocumentReference()));
    doc.setSyntax(Syntax.XWIKI_1_0);
    return doc;
  }

  @Override
  public XWikiDocument createWithoutDefaults(DocumentReference docRef) {
    return createWithoutDefaults(docRef, IModelAccessFacade.DEFAULT_LANG);
  }

  @Override
  public XWikiDocument create(DocumentReference docRef, String lang) {
    lang = modelUtils.normalizeLang(lang);
    String defaultLang = getDefaultLangForCreatingDoc(docRef);
    if (defaultLang.equals(lang)) {
      lang = DEFAULT_LANG;
    }
    XWikiDocument doc = createWithoutDefaults(docRef, lang);
    doc.setDefaultLanguage(defaultLang);
    doc.setSyntax(getDefaultSyntax());
    return doc;
  }

  /**
   * when creating doc, get default language from space. except get it from wiki directly when
   * creating web preferences
   */
  private String getDefaultLangForCreatingDoc(DocumentReference docRef) {
    Class<? extends EntityReference> toExtractClass;
    if (docRef.getName().equals(ModelContext.WEB_PREF_DOC_NAME)) {
      toExtractClass = WikiReference.class;
    } else {
      toExtractClass = SpaceReference.class;
    }
    return context.getDefaultLanguage(docRef.extractRef(toExtractClass)
        .orElseThrow(IllegalStateException::new));
  }

  private Syntax getDefaultSyntax() {
    return Optional.ofNullable(coreCfg.getDefaultDocumentSyntax())
        .filter(syntax -> componentManager.hasComponent(Parser.class, syntax.toIdString()))
        .orElseThrow(IllegalStateException::new);
  }

  @Override
  public XWikiDocument create(DocumentReference docRef) {
    return create(docRef, IModelAccessFacade.DEFAULT_LANG);
  }

}
