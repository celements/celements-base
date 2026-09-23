package com.celements.convert.bean;

import static com.google.common.base.Preconditions.*;
import static java.text.MessageFormat.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;

import com.celements.convert.ConversionException;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ObjectRestriction;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XDocBeanConversionLoader<T> implements XDocBeanLoader<T> {

  protected static final Logger LOGGER = LoggerFactory.getLogger(XDocBeanLoader.class);

  private Class<T> token;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement(XObjectBeanConverter.NAME)
  private BeanClassDefConverter<BaseObject, T> converter;

  @Override
  public void initialize(Class<T> token, ClassIdentity classId) {
    this.token = checkNotNull(token);
    converter.initialize(this.token);
    converter.initialize(classId.getClassDefinition()
        .orElseThrow(() -> new IllegalArgumentException("no class definition for " + classId)));
  }

  @Override
  public Class<T> getToken() {
    checkState(token != null, "not initialized");
    return token;
  }

  @Override
  public ClassIdentity getClassId() {
    return converter.getClassDef();
  }

  @Override
  public T load(DocumentReference docRef) throws BeanLoadException {
    return load(docRef, null);
  }

  @Override
  public T load(DocumentReference docRef, Iterable<ObjectRestriction<BaseObject>> restrictions)
      throws BeanLoadException {
    return load(modelAccess.getOrCreateDocument(docRef), restrictions);
  }

  @Override
  public T load(XWikiDocument doc) throws BeanLoadException {
    return load(doc, null);
  }

  @Override
  public T load(XWikiDocument doc, Iterable<ObjectRestriction<BaseObject>> restrictions)
      throws BeanLoadException {
    XWikiObjectFetcher fetcher = XWikiObjectFetcher.on(doc).filter(getClassId().getLocalClassRef());
    if (restrictions != null) {
      fetcher.filter(restrictions);
    }
    try {
      return converter.apply(fetcher.firstAssert());
    } catch (IllegalArgumentException | ConversionException exc) {
      String msg = format("given doc [{0}] invalid for loading [{1}]", doc, getClassId());
      throw new BeanLoadException(msg, exc);
    }
  }

}
