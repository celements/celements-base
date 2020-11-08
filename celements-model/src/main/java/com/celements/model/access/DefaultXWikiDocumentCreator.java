package com.celements.model.access;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.celements.model.util.References.*;

import java.util.Date;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.References;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class DefaultXWikiDocumentCreator implements XWikiDocumentCreator {

  @Requirement
  private ModelContext context;
  @Requirement
  private ModelUtils modelUtils;

  @Override
  public XWikiDocument createWithoutDefaults(DocumentReference docRef, String lang) {
    XWikiDocument doc = new XWikiDocument(cloneRef(docRef, DocumentReference.class));
    doc.setNew(true);
    lang = modelUtils.normalizeLang(lang);
    doc.setLanguage(lang);
    doc.setTranslation(DEFAULT_LANG.equals(lang) ? 0 : 1);
    Date creationDate = new Date();
    doc.setCreationDate(creationDate);
    doc.setContentUpdateDate(creationDate);
    doc.setDate(creationDate);
    doc.setCreator(context.getUserName());
    doc.setAuthor(context.getUserName());
    doc.setContent("");
    doc.setContentDirty(true);
    doc.setMetaDataDirty(true);
    doc.setOriginalDocument(new XWikiDocument(doc.getDocumentReference()));
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
    doc.setSyntax(doc.getSyntax()); // assures that syntax is set, 'new' has to be true
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
    return context.getDefaultLanguage(References.extractRef(docRef, toExtractClass).get());
  }

  @Override
  public XWikiDocument create(DocumentReference docRef) {
    return create(docRef, IModelAccessFacade.DEFAULT_LANG);
  }

}
