package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiObjectClass.*;
import static com.google.common.base.Preconditions.*;
import static java.text.MessageFormat.*;

import java.text.MessageFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.CustomClassField;
import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link FieldAccessor} for accessing {@link BaseObject} properties
 */
@Component(XObjectFieldAccessor.NAME)
public class XObjectFieldAccessor extends AbstractFieldAccessor<BaseObject> {

  private static final Logger LOGGER = LoggerFactory.getLogger(XObjectFieldAccessor.class);

  public static final String NAME = "xobject";

  @Requirement(XObjectStringFieldAccessor.NAME)
  protected StringFieldAccessor<BaseObject> strFieldAccessor;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public <V> Optional<V> get(BaseObject obj, ClassField<V> field) {
    Optional<V> value;
    if (field.getClassReference().equals(CLASS_REF)) {
      value = Optional.of(getXObjFieldValue(obj, field));
    } else {
      checkClassRef(obj, field);
      return strFieldAccessor.get(obj, field.getName())
          .flatMap(val -> resolvePropertyValue(field, val));
    }
    LOGGER.info("getValue: '{}' for '{}' from '{} - {} - {}'", value, field,
        obj.getDocumentReference(), obj.getXClassReference(), obj.getNumber());
    return value;
  }

  @SuppressWarnings("unchecked")
  private <V> V getXObjFieldValue(BaseObject obj, ClassField<V> field) {
    V value;
    if (field == FIELD_DOC_REF) {
      value = (V) obj.getDocumentReference();
    } else if (field == FIELD_CLASS_REF) {
      value = (V) new ClassReference(obj.getXClassReference());
    } else if (field == FIELD_NUMBER) {
      value = (V) (Integer) obj.getNumber();
    } else {
      throw new FieldAccessException("undefined field: " + field);
    }
    return value;
  }

  private <T> Optional<T> resolvePropertyValue(ClassField<T> field, Object value) {
    try {
      if (field instanceof CustomClassField) {
        return ((CustomClassField<T>) field).resolve(value);
      } else {
        return Optional.of(field.getType().cast(value));
      }
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw new FieldAccessException(format("field [{0}] ill defined, expecting type [{1}], "
          + "but got [{2}]", field, field.getType(), value.getClass()), exc);
    }
  }

  @Override
  public <V> boolean set(BaseObject obj, ClassField<V> field, V newValue) {
    checkClassRef(obj, field);
    boolean dirty = strFieldAccessor.set(obj, field.getName(),
        serializePropertyValue(field, newValue).orElse(null));
    if (dirty) {
      LOGGER.info("setValue: '{}' for '{}' from '{} - {} - {}'", newValue, field,
          obj.getDocumentReference(), obj.getXClassReference(), obj.getNumber());
    }
    return dirty;
  }

  private <T> Optional<?> serializePropertyValue(ClassField<T> field, T value) {
    try {
      if (field instanceof CustomClassField) {
        return ((CustomClassField<T>) field).serialize(value);
      } else {
        return Optional.ofNullable(value);
      }
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw new FieldAccessException(format("field [{0}] ill defined, expecting type [{1}], "
          + "but got [{2}]", field, field.getType(), value.getClass()), exc);
    }
  }

  private void checkClassRef(BaseObject obj, ClassField<?> field) {
    checkNotNull(obj);
    checkNotNull(field);
    if (!field.getClassReference().isValidObjectClass()) {
      throw new FieldAccessException(MessageFormat.format(
          "BaseObject uneligible for pseudo class field [{0}]", field));
    }
    ClassReference classRef = new ClassReference(obj.getXClassReference());
    if (!classRef.equals(field.getClassReference())) {
      throw new FieldAccessException(MessageFormat.format(
          "BaseObject uneligible for [{0}], it is of class [{1}]", field, classRef));
    }
  }

}
