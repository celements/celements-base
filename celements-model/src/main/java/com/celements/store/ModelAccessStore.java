package com.celements.store;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.model.access.IModelAccessFacade.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.Optional;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.google.common.primitives.Ints;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import one.util.streamex.StreamEx;

@Singleton
@Component(ModelAccessStore.NAME)
public class ModelAccessStore extends DelegateStore {

  public static final String NAME = "modelAccess";

  @Requirement
  private IModelAccessFacade modelAccess;

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
    DocumentReference docRef = doc.getDocumentReference();
    try {
      return StreamEx.of(doc.getLanguage(), DEFAULT_LANG)
          .mapPartial(lang -> getDocument(docRef, lang))
          .findFirst()
          .orElseGet(rethrow(() -> modelAccess.createDocument(docRef, doc.getLanguage())));
    } catch (DocumentAlreadyExistsException exc) {
      throw new IllegalStateException("should not happen", exc);
    }
  }

  private Optional<XWikiDocument> getDocument(DocumentReference docRef, String lang) {
    try {
      return Optional.of(disableClone()
          ? modelAccess.getDocumentReadOnly(docRef, lang)
          : modelAccess.getDocument(docRef, lang));
    } catch (DocumentNotExistsException dne1) {
      return Optional.empty();
    }
  }

  private boolean disableClone() {
    String key = "celements.store." + getName() + ".disableClone";
    String disable = cfgSrc.getProperty(key, "false").toLowerCase();
    return "true".equals(disable) || (0 != Optional.ofNullable(Ints.tryParse(disable)).orElse(0));
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      boolean totrash = Boolean.TRUE.equals(context.get("delete_totrash"));
      if (doc.isTrans()) {
        modelAccess.deleteTranslation(doc.getDocumentReference(), doc.getLanguage(), totrash);
      } else {
        modelAccess.deleteDocument(doc.getDocumentReference(), totrash);
      }
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
