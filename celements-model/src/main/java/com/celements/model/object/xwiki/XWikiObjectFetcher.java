package com.celements.model.object.xwiki;

import static com.google.common.base.MoreObjects.*;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.model.object.AbstractObjectFetcher;
import com.celements.model.object.ObjectBridge;
import com.celements.model.object.ObjectHandler;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@NotThreadSafe
public class XWikiObjectFetcher extends
    AbstractObjectFetcher<XWikiObjectFetcher, XWikiDocument, BaseObject> {

  public static XWikiObjectFetcher on(@NotNull XWikiDocument doc) {
    return new XWikiObjectFetcher(doc, XWikiObjectBridge.NAME);
  }

  public static XWikiObjectFetcher from(
      @NotNull ObjectHandler<XWikiDocument, BaseObject> objHandler) {
    return XWikiObjectFetcher.on(objHandler.getDocument())
        .withTranslation(objHandler.getTranslationDoc().orElse(null))
        .with(objHandler.getQuery());
  }

  public static XWikiObjectFetcher empty() {
    XWikiDocument dummyDoc = new XWikiDocument(new DocumentReference("$", "$", "$"));
    return new XWikiObjectFetcher(dummyDoc, XWikiEmptyObjectBridge.NAME);
  }

  private final String bridgeHint;

  private XWikiObjectFetcher(XWikiDocument doc, String bridgeHint) {
    super(doc);
    this.bridgeHint = bridgeHint;
  }

  @Override
  public XWikiObjectFetcher clone() {
    return from(getThis());
  }

  @Override
  protected XWikiObjectFetcher disableCloning() {
    return super.disableCloning();
  }

  @Override
  protected XWikiObjectBridge getBridge() {
    return (XWikiObjectBridge) Utils.getComponent(ObjectBridge.class, firstNonNull(bridgeHint,
        XWikiObjectBridge.NAME));
  }

  @Override
  protected XWikiObjectFetcher getThis() {
    return this;
  }

}
