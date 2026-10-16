package com.celements.model.object.cel;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.google.common.base.Preconditions.*;

import java.text.MessageFormat;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.field.CelDocumentFieldAccessor;
import com.celements.model.field.CelObjectFieldAccessor;
import com.celements.model.field.FieldAccessor;
import com.celements.model.object.ObjectBridge;
import com.google.common.base.Strings;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.CelObject;

import one.util.streamex.StreamEx;

@Immutable
@Component
public class CelObjectBridge implements ObjectBridge<CelDocument, CelObject> {

  private final FieldAccessor<CelDocument> celDocAccessor;
  private final FieldAccessor<CelObject> celObjAccessor;

  @Inject
  public CelObjectBridge(
      CelDocumentFieldAccessor celDocAccessor,
      CelObjectFieldAccessor celObjAccessor) {
    this.celDocAccessor = celDocAccessor;
    this.celObjAccessor = celObjAccessor;
  }

  @Override
  public Class<CelDocument> getDocumentType() {
    return CelDocument.class;
  }

  @Override
  public Class<CelObject> getObjectType() {
    return CelObject.class;
  }

  @Override
  @Deprecated
  public void checkDoc(CelDocument doc) {
    // nothing to check on a document in general
  }

  @Override
  public DocumentReference getDocRef(CelDocument doc) {
    return doc.getDocumentReference().withoutLocale();
  }

  @Override
  public String getLanguage(CelDocument doc) {
    return normalizeLang(doc.getLanguage());
  }

  @Override
  public String getDefaultLanguage(CelDocument doc) {
    return normalizeLang(doc.getDefaultLanguage());
  }

  private String normalizeLang(String lang) {
    return "default".equals(lang) ? DEFAULT_LANG : Strings.nullToEmpty(lang);
  }

  @Override
  public StreamEx<LocalDocumentReference> getDocClasses(CelDocument doc) {
    return StreamEx.of(assertDefaultDoc(doc).getXObjects())
        .map(CelObject::getClassReference)
        .distinct();
  }

  @Override
  public StreamEx<CelObject> getObjects(CelDocument doc, LocalDocumentReference classRef) {
    return StreamEx.of(assertDefaultDoc(doc).getXObjects())
        .filter(obj -> classRef.equals(obj.getClassReference()));
  }

  private CelDocument.Default assertDefaultDoc(CelDocument doc) {
    checkArgument(doc instanceof CelDocument.Default,
        MessageFormat.format("object operations not allowed on translation [{0}] of doc [{1}]",
            doc.getLanguage(), doc.getDocumentReference()));
    return (CelDocument.Default) doc;
  }

  @Override
  public int getObjectNumber(CelObject obj) {
    return obj.getNumber();
  }

  @Override
  public LocalDocumentReference getObjectClass(CelObject obj) {
    return obj.getClassReference();
  }

  @Override
  public CelObject cloneObject(CelObject obj) {
    return obj;
  }

  @Override
  public CelObject createObject(CelDocument doc, LocalDocumentReference classRef) {
    throw new UnsupportedOperationException("CelDocument is immutable");
  }

  @Override
  public boolean deleteObject(CelDocument doc, CelObject obj) {
    throw new UnsupportedOperationException("CelDocument is immutable");
  }

  @Override
  public FieldAccessor<CelDocument> getDocumentFieldAccessor() {
    return celDocAccessor;
  }

  @Override
  public FieldAccessor<CelObject> getObjectFieldAccessor() {
    return celObjAccessor;
  }

}
