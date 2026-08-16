package com.celements.model.object.xwiki;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.context.ModelContext;
import com.celements.model.field.XDocumentFieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import one.util.streamex.StreamEx;

@Immutable
@Component(XWikiEmptyObjectBridge.NAME)
public class XWikiEmptyObjectBridge extends XWikiObjectBridge {

  public static final String NAME = "XWikiEmptyObjectBridge";

  @Inject
  public XWikiEmptyObjectBridge(
      XDocumentFieldAccessor xDocAccessor,
      XObjectFieldAccessor xObjAccessor,
      ModelContext context) {
    super(xDocAccessor, xObjAccessor, context);
  }

  @Override
  public StreamEx<LocalDocumentReference> getDocClasses(XWikiDocument doc) {
    return StreamEx.empty();
  }

  @Override
  public StreamEx<BaseObject> getObjects(XWikiDocument doc, LocalDocumentReference classRef) {
    return StreamEx.empty();
  }

}
