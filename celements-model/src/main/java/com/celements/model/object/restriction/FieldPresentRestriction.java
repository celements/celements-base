package com.celements.model.object.restriction;

import static com.google.common.base.Preconditions.*;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.ObjectBridge;

@Immutable
public class FieldPresentRestriction<O, T> extends ClassRestriction<O> {

  private final ClassField<T> field;

  public FieldPresentRestriction(@NotNull ObjectBridge<?, O> bridge, @NotNull ClassField<T> field) {
    super(bridge, field.getClassReference());
    this.field = checkNotNull(field);
  }

  public ClassField<T> getField() {
    return field;
  }

  @Override
  public boolean apply(@NotNull O obj) {
    return super.apply(obj) && getBridge().getObjectFieldAccessor().get(obj, getField())
        .isPresent();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getField());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof FieldPresentRestriction) {
      FieldPresentRestriction<?, ?> other = (FieldPresentRestriction<?, ?>) obj;
      return super.equals(obj) && Objects.equals(this.getField(), other.getField());
    }
    return false;
  }

  @Override
  public String toString() {
    return "FieldPresentRestriction [field=" + getField() + "]";
  }

}
