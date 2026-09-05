package com.celements.model.field;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.fields.ClassField;

/**
 * implementations allow to access values denoted by {@link ClassField} of any generic instance
 */
@ComponentRole
public interface FieldAccessor<T> {

  @NotNull
  String getName();

  /**
   * gets the value denoted by the field on an instance
   *
   * @throws FieldAccessException
   *           if an exception occurred while trying to access the field
   */
  @NotNull
  <V> Optional<V> get(@NotNull T instance, @NotNull ClassField<V> field);

  /**
   * sets the value denoted by the field on an instance
   *
   * @return true if the field value has changed on the instance
   * @throws FieldAccessException
   *           if an exception occurred while trying to access the field
   */
  <V> boolean set(@NotNull T instance, @NotNull ClassField<V> field, @Nullable V newValue);

  @NotNull
  @Deprecated
  default <V> com.google.common.base.Optional<V> getValue(@NotNull T instance,
      @NotNull ClassField<V> field) throws FieldAccessException {
    return com.google.common.base.Optional.fromJavaUtil(get(instance, field));
  }

  @Deprecated
  default <V> boolean setValue(@NotNull T instance, @NotNull ClassField<V> field, @Nullable V value)
      throws FieldAccessException {
    return set(instance, field, value);
  }

}
