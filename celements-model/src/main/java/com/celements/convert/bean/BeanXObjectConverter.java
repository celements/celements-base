package com.celements.convert.bean;

import java.util.function.Supplier;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.celements.convert.classes.XObjectDeconverter;
import com.celements.model.field.FieldAccessor;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Converts a Bean to an XObject.
 * IMPORTANT: see {@link BeanClassDefConverter}
 */
@Component(BeanXObjectConverter.NAME)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class BeanXObjectConverter<T> extends XObjectDeconverter<T> implements
    BeanClassDefConverter<T, BaseObject> {

  public static final String NAME = "beanxobject";

  @Requirement(BeanFieldAccessor.NAME)
  private FieldAccessor<T> beanAccessor;

  @Override
  public void initialize(Supplier<BaseObject> instanceSupplier) {
  }

  @Override
  public void initialize(Class<BaseObject> token) {
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public FieldAccessor<T> getFromFieldAccessor() {
    return beanAccessor;
  }

}
