package com.celements.model.object.xwiki;

import static com.google.common.base.Preconditions.*;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import com.celements.model.object.AbstractObjectEditor;
import com.celements.model.object.ObjectHandler;
import com.celements.spring.context.SpringContextProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@NotThreadSafe
public class XWikiObjectEditor extends
    AbstractObjectEditor<XWikiObjectEditor, XWikiDocument, BaseObject> {

  public static XWikiObjectEditor on(@NotNull XWikiDocument doc) {
    return new XWikiObjectEditor(checkNotNull(doc));
  }

  public static XWikiObjectEditor from(
      @NotNull ObjectHandler<XWikiDocument, BaseObject> objHandler) {
    return XWikiObjectEditor.on(objHandler.getDocument())
        .withTranslation(objHandler.getTranslationDoc().orElse(null))
        .with(objHandler.getQuery());
  }

  private XWikiObjectEditor(XWikiDocument doc) {
    super(doc);
  }

  @Override
  public XWikiObjectEditor clone() {
    return from(getThis());
  }

  @Override
  public XWikiObjectFetcher fetch() {
    return XWikiObjectFetcher.on(getDocument()).with(getQuery()).disableCloning();
  }

  @Override
  protected XWikiObjectBridge getBridge() {
    return SpringContextProvider.getBeanFactory()
        .getBean(XWikiObjectBridge.NAME, XWikiObjectBridge.class);
  }

  @Override
  protected XWikiObjectEditor getThis() {
    return this;
  }

}
