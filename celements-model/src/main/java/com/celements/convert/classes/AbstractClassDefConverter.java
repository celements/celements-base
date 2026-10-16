package com.celements.convert.classes;

import static com.celements.common.MoreObjectsCel.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Supplier;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.convert.ConversionException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessException;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.FieldMissingException;
import com.google.common.collect.ImmutableList;

public abstract class AbstractClassDefConverter<A, B> implements ClassDefinitionConverter<A, B> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractClassDefConverter.class);

  private ClassDefinition classDef;

  @Override
  public void initialize(ClassDefinition classDef) {
    checkState(this.classDef == null, "already initialized");
    this.classDef = checkNotNull(classDef);
  }

  @Override
  public ClassDefinition getClassDef() {
    checkState(classDef != null, "not initialized");
    return classDef;
  }

  @Override
  public B apply(A data) throws ConversionException {
    return apply(getInstanceSupplier().get(), data);
  }

  @Override
  public B apply(B instance, A data) throws ConversionException {
    if (data != null) {
      for (ClassField<?> field : aggregateClassFields(new ImmutableList.Builder<>()).build()) {
        try {
          convertField(field, getToFieldAccessor(), instance, getFromFieldAccessor(), data);
        } catch (FieldAccessException exc) {
          handle(exc);
        }
      }
    }
    return instance;
  }

  protected ImmutableList.Builder<ClassField<?>> aggregateClassFields(
      ImmutableList.Builder<ClassField<?>> builder) {
    return builder.addAll(getClassDef().getFields());
  }

  private static <V, A, B> void convertField(ClassField<V> field, FieldAccessor<A> toAccessor, A to,
      FieldAccessor<B> fromAccessor, B from) throws FieldAccessException {
    toAccessor.set(to, field, fromAccessor.get(from, field)
        .orElseGet(() -> defaultValueNonNullable(field.getType())));
  }

  protected void handle(FieldAccessException exc) throws ConversionException {
    if (exc instanceof FieldMissingException) {
      LOGGER.info("incompleteness detected for '{}'", this.getClass().getSimpleName(), exc);
    } else {
      throw new ConversionException(exc);
    }
  }

  protected abstract @NotNull Supplier<B> getInstanceSupplier();

}
