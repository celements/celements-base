package com.celements.model.object.cel;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import com.celements.model.object.AbstractObjectFetcher;
import com.celements.model.object.ObjectHandler;
import com.celements.spring.context.SpringContextProvider;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.CelObject;
import com.xpn.xwiki.doc.XWikiDocument;

@NotThreadSafe
public class CelObjectFetcher extends
    AbstractObjectFetcher<CelObjectFetcher, CelDocument, CelObject> {

  private static final CelDocument EMPTY_DOC = CelDocument.Default.from(
      new XWikiDocument(EMPTY_DOC_REF));

  public static CelObjectFetcher on(@NotNull CelDocument doc) {
    return new CelObjectFetcher(doc);
  }

  public static CelObjectFetcher from(
      @NotNull ObjectHandler<CelDocument, CelObject> objHandler) {
    return CelObjectFetcher.on(objHandler.getDocument())
        .withTranslation(objHandler.getTranslationDoc().orElse(null))
        .with(objHandler.getQuery());
  }

  public static CelObjectFetcher empty() {
    return CelObjectFetcher.on(EMPTY_DOC);
  }

  private CelObjectFetcher(CelDocument doc) {
    super(doc);
  }

  @Override
  public CelObjectFetcher clone() {
    return from(getThis());
  }

  @Override
  protected CelObjectBridge getBridge() {
    return SpringContextProvider.getBeanFactory().getBean(CelObjectBridge.class);
  }

  @Override
  protected CelObjectFetcher getThis() {
    return this;
  }

}
