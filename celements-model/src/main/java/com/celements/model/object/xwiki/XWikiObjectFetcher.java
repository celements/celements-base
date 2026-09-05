package com.celements.model.object.xwiki;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import com.celements.model.object.AbstractObjectFetcher;
import com.celements.model.object.ObjectHandler;
import com.celements.spring.context.SpringContextProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

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
    XWikiDocument dummyDoc = new XWikiDocument(EMPTY_DOC_REF);
    return new XWikiObjectFetcher(dummyDoc, XWikiEmptyObjectBridge.NAME);
  }

  private final String bridgeBeanName;

  private XWikiObjectFetcher(XWikiDocument doc, String bridgeBeanName) {
    super(doc);
    this.bridgeBeanName = bridgeBeanName;
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
    return SpringContextProvider.getBeanFactory()
        .getBean(bridgeBeanName, XWikiObjectBridge.class);
  }

  @Override
  protected XWikiObjectFetcher getThis() {
    return this;
  }

}
