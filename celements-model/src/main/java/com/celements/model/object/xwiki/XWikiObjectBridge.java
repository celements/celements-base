package com.celements.model.object.xwiki;

import static com.celements.logging.LogUtils.*;
import static com.celements.model.access.IModelAccessFacade.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.text.MessageFormat;
import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.exception.ClassDocumentLoadException;
import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.XDocumentFieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.model.object.ObjectBridge;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import one.util.streamex.StreamEx;

@Immutable
@Component(XWikiObjectBridge.NAME)
public class XWikiObjectBridge implements ObjectBridge<XWikiDocument, BaseObject> {

  public static final String NAME = "XWikiObjectBridge";

  private final FieldAccessor<XWikiDocument> xDocAccessor;
  private final FieldAccessor<BaseObject> xObjAccessor;
  private final ModelContext context;

  @Inject
  public XWikiObjectBridge(
      XDocumentFieldAccessor xDocAccessor,
      XObjectFieldAccessor xObjAccessor,
      ModelContext context) {
    this.xDocAccessor = xDocAccessor;
    this.xObjAccessor = xObjAccessor;
    this.context = context;
  }

  @Override
  public Class<XWikiDocument> getDocumentType() {
    return XWikiDocument.class;
  }

  @Override
  public Class<BaseObject> getObjectType() {
    return BaseObject.class;
  }

  @Override
  @Deprecated
  public void checkDoc(XWikiDocument doc) {
    // nothing to check on a document in general
  }

  private void checkIsMainDoc(XWikiDocument doc) {
    checkArgument(doc.getTranslation() == 0, defer(() -> MessageFormat.format(
        "object operations not allowed on translation [{0}] of doc [{1}]",
        doc.getLanguage(), doc.getDocumentReference())));
  }

  @Override
  public DocumentReference getDocRef(XWikiDocument doc) {
    return doc.getDocumentReference();
  }

  @Override
  public String getLanguage(XWikiDocument doc) {
    return normalizeLang(doc.getLanguage());
  }

  @Override
  public String getDefaultLanguage(XWikiDocument doc) {
    return normalizeLang(doc.getDefaultLanguage());
  }

  private String normalizeLang(String lang) {
    return "default".equals(lang) ? DEFAULT_LANG : Strings.nullToEmpty(lang);
  }

  @Override
  public StreamEx<LocalDocumentReference> getDocClasses(XWikiDocument doc) {
    checkIsMainDoc(doc);
    return StreamEx.of(doc.getXObjectClassRefs()).map(LocalDocumentReference::new).distinct();
  }

  @Override
  public StreamEx<BaseObject> getObjects(XWikiDocument doc, LocalDocumentReference classRef) {
    checkIsMainDoc(doc);
    WikiReference docWiki = doc.getDocumentReference().getWikiReference();
    List<BaseObject> objects = firstNonNull(doc.getXObjects(classRef.getDocRef(docWiki)),
        ImmutableList.<BaseObject>of());
    return StreamEx.of(objects).nonNull();
  }

  @Override
  public int getObjectNumber(BaseObject obj) {
    return obj.getNumber();
  }

  @Override
  public LocalDocumentReference getObjectClass(BaseObject obj) {
    return new LocalDocumentReference(obj.getXClassReference());
  }

  @Override
  public BaseObject cloneObject(BaseObject obj) {
    return (BaseObject) obj.clone();
  }

  @Override
  public BaseObject createObject(XWikiDocument doc, LocalDocumentReference classRef) {
    checkIsMainDoc(doc);
    WikiReference docWiki = doc.getDocumentReference().getWikiReference();
    try {
      return doc.newXObject(classRef.getDocRef(docWiki), context.getXWikiContext());
    } catch (XWikiException xwe) {
      throw new ClassDocumentLoadException(classRef.getDocRef(docWiki), xwe);
    }
  }

  @Override
  public boolean deleteObject(XWikiDocument doc, BaseObject obj) {
    checkIsMainDoc(doc);
    return doc.removeXObject(obj);
  }

  @Override
  public FieldAccessor<XWikiDocument> getDocumentFieldAccessor() {
    return xDocAccessor;
  }

  @Override
  public FieldAccessor<BaseObject> getObjectFieldAccessor() {
    return xObjAccessor;
  }

}
