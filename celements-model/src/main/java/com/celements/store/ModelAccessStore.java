package com.celements.store;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.xpn.xwiki.XWikiException.*;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Singleton
@Component(ModelAccessStore.NAME)
public class ModelAccessStore extends DelegateStore {

  public static final String NAME = "modelAccess";

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Override
  protected String getName() {
    return NAME;
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      modelAccess.saveDocument(doc, doc.getComment(), doc.isMinorEdit());
    } catch (DocumentSaveException exc) {
      throw asXWikiException(exc);
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    saveXWikiDoc(doc, context);
  }

  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      return modelAccess.getDocument(doc.getDocumentReference(), doc.getLanguage());
    } catch (DocumentNotExistsException exc) {
      XWikiDocument mainDoc = modelAccess.getOrCreateDocument(doc.getDocumentReference());
      String lang = modelUtils.normalizeLang(doc.getLanguage());
      if (mainDoc.getDefaultLanguage().equals(lang)) {
        lang = DEFAULT_LANG;
      }
      mainDoc.setLanguage(lang);
      mainDoc.setTranslation(DEFAULT_LANG.equals(lang) ? 0 : 1);
      return mainDoc;
    }
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      boolean totrash = Boolean.TRUE.equals(context.get("delete_totrash"));
      modelAccess.deleteDocumentWithoutTranslations(doc, totrash);
    } catch (DocumentDeleteException exc) {
      throw asXWikiException(exc);
    }
  }

  @Override
  public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
    return modelAccess.existsLang(doc.getDocumentReference(), doc.getLanguage());
  }

  private XWikiException asXWikiException(Exception exc) {
    if (exc.getCause() instanceof XWikiException) {
      return (XWikiException) exc.getCause();
    } else if (exc instanceof RuntimeException) {
      throw (RuntimeException) exc;
    } else {
      return new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
          exc.getMessage(), exc);
    }
  }

}
