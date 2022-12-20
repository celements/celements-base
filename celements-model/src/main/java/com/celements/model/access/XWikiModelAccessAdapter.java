package com.celements.model.access;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.exception.DocumentAccessException;
import com.celements.model.access.exception.ModelAccessRuntimeException;
import com.xpn.xwiki.XWikiAdapter;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class XWikiModelAccessAdapter implements XWikiAdapter {

  @Requirement
  private IModelAccessFacade modelAccess;

  @Override
  public boolean exists(DocumentReference documentReference) {
    return modelAccess.exists(documentReference);
  }

  @Override
  public XWikiDocument getDocument(XWikiDocument doc) throws XWikiException {
    try {
      return modelAccess.getOrCreateDocument(doc.getDocumentReference(), doc.getLanguage());
    } catch (ModelAccessRuntimeException exc) {
      throw asXWE(exc);
    }
  }

  @Override
  public void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit)
      throws XWikiException {
    try {
      modelAccess.saveDocument(doc, comment, isMinorEdit);
    } catch (DocumentAccessException exc) {
      throw asXWE(exc);
    }
  }

  @Override
  public void deleteDocument(XWikiDocument doc, boolean totrash) throws XWikiException {
    try {
      modelAccess.deleteDocument(doc, totrash);
    } catch (DocumentAccessException exc) {
      throw asXWE(exc);
    }
  }

  private XWikiException asXWE(Exception exc) {
    if (exc.getCause() instanceof XWikiException) {
      return (XWikiException) exc.getCause();
    } else if (exc instanceof RuntimeException) {
      throw (RuntimeException) exc;
    } else {
      return new XWikiException(0, 0, exc.getMessage(), exc);
    }
  }

}
