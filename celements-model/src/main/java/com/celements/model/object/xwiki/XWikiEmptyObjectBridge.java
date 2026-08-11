package com.celements.model.object.xwiki;

import javax.annotation.concurrent.Immutable;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.context.ModelContext;
import com.celements.model.field.XDocumentFieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.google.common.collect.FluentIterable;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
  public FluentIterable<? extends ClassIdentity> getDocClasses(XWikiDocument doc) {
    return FluentIterable.of();
  }

  @Override
  public FluentIterable<BaseObject> getObjects(XWikiDocument doc, ClassIdentity classId) {
    return FluentIterable.of();
  }

}
