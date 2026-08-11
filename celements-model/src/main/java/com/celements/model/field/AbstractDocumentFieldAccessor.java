package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiDocumentClass.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;
import static java.text.MessageFormat.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.fields.ClassField;

public abstract class AbstractDocumentFieldAccessor<D> implements FieldAccessor<D> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  @SuppressWarnings("unchecked")
  public final <V> Optional<V> get(D doc, ClassField<V> field) {
    checkNotNull(doc);
    checkField(field);
    Function<D, ?> getter = getters().get(field.getName());
    if (getter == null) {
      throw new FieldAccessException("undefined field: " + field);
    }
    Object value = getter.apply(doc);
    if (value instanceof String) {
      value = emptyToNull(value.toString().trim());
    }
    logger.info("get: '{}' for '{}' from '{}'", value, field, doc);
    return Optional.ofNullable((V) value);
  }

  private void checkField(ClassField<?> field) {
    checkNotNull(field);
    if (!CLASS_REF.equals(field.getClassReference())) {
      throw new FieldAccessException(format("uneligible for [{0}], it is of class [{1}]",
          CLASS_REF, field.getClassReference()));
    }
  }

  protected abstract Map<String, Function<D, ?>> getters();

}
