package com.celements.convert.bean;

import static com.google.common.base.Preconditions.*;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.xwiki.component.annotation.ComponentRole;

import com.celements.common.reflect.ReflectiveInstanceSupplier;
import com.celements.component.ComponentInstanceSupplier;
import com.celements.convert.classes.AbstractObjectConverter;
import com.celements.model.field.CelObjectFieldAccessor;
import com.celements.model.field.FieldAccessor;
import com.xpn.xwiki.doc.CelObject;

/** Converts an immutable {@link CelObject} to a bean. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CelObjectBeanConverter<T> extends AbstractObjectConverter<CelObject, T> implements
    BeanClassDefConverter<CelObject, T> {

  private final FieldAccessor<CelObject> celObjectAccessor;
  private final FieldAccessor<T> beanAccessor;
  private Supplier<T> supplier;

  @Inject
  public CelObjectBeanConverter(
      CelObjectFieldAccessor celObjectAccessor,
      BeanFieldAccessor<T> beanAccessor) {
    this.celObjectAccessor = celObjectAccessor;
    this.beanAccessor = beanAccessor;
  }

  @Override
  public void initialize(Supplier<T> instanceSupplier) {
    supplier = checkNotNull(instanceSupplier);
  }

  @Override
  public void initialize(Class<T> token) {
    checkNotNull(token);
    initialize(token.isAnnotationPresent(ComponentRole.class)
        ? new ComponentInstanceSupplier<>(token)
        : new ReflectiveInstanceSupplier<>(token));
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public FieldAccessor<CelObject> getFromFieldAccessor() {
    return celObjectAccessor;
  }

  @Override
  public FieldAccessor<T> getToFieldAccessor() {
    return beanAccessor;
  }

  @Override
  protected Supplier<T> getInstanceSupplier() {
    checkState(supplier != null, "not initialized");
    return supplier;
  }

}
