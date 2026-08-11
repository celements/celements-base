package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiObjectClass.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;
import static java.text.MessageFormat.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.CustomClassField;

public abstract class AbstractObjectFieldAccessor<O> implements FieldAccessor<O> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public final <V> Optional<V> get(O obj, ClassField<V> field) {
    checkNotNull(obj);
    checkNotNull(field);
    Optional<V> value;
    if (field.getClassReference().equals(CLASS_REF)) {
      value = Optional.of(getObjectFieldValue(obj, field));
    } else {
      checkClassRef(obj, field);
      value = getRawValue(obj, field.getName())
          .map(val -> (val instanceof String str) ? emptyToNull(str.trim()) : val)
          .flatMap(val -> resolvePropertyValue(field, val));
    }
    logger.trace("get - obj [{}], field [{}], value [{}]", obj, field, value);
    return value;
  }

  protected abstract Optional<Object> getRawValue(O obj, String fieldName);

  private <V> V getObjectFieldValue(O obj, ClassField<V> field) {
    Function<O, ?> getter = getters().get(field.getName());
    if (getter == null) {
      throw new FieldAccessException("undefined field: " + field);
    }
    return field.getType().cast(getter.apply(obj));
  }

  protected abstract Map<String, Function<O, ?>> getters();

  private <V> Optional<V> resolvePropertyValue(ClassField<V> field, Object value) {
    try {
      if (field instanceof CustomClassField) {
        return ((CustomClassField<V>) field).resolve(value);
      } else {
        return Optional.of(field.getType().cast(value));
      }
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw new FieldAccessException(format("field [{0}] ill defined, expecting type [{1}], "
          + "but got [{2}]", field, field.getType(), value.getClass()), exc);
    }
  }

  protected final void checkClassRef(O obj, ClassField<?> field) {
    checkNotNull(obj);
    checkNotNull(field);
    String objectTypeName = obj.getClass().getSimpleName();
    if (!field.getClassReference().isValidObjectClass()) {
      throw new FieldAccessException(format(
          "{0} uneligible for pseudo class field [{1}]", objectTypeName, field));
    }
    ClassReference classRef = getObjectFieldValue(obj, FIELD_CLASS_REF);
    if (!classRef.equals(field.getClassReference())) {
      throw new FieldAccessException(format(
          "{0} uneligible for [{1}], it is of class [{2}]", objectTypeName, field, classRef));
    }
  }

}
