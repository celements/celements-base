package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiObjectClass.*;
import static java.text.MessageFormat.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.CustomClassField;
import com.xpn.xwiki.objects.BaseObject;

/**
 * {@link FieldAccessor} for accessing {@link BaseObject} properties
 */
@Component(XObjectFieldAccessor.NAME)
public class XObjectFieldAccessor extends AbstractObjectFieldAccessor<BaseObject> {

  public static final String NAME = "XObjectFieldAccessor";

  private final StringFieldAccessor<BaseObject> strFieldAccessor;

  @Inject
  public XObjectFieldAccessor(XObjectStringFieldAccessor strFieldAccessor) {
    this.strFieldAccessor = strFieldAccessor;
  }

  @Override
  public String getName() {
    return NAME;
  }

  static final Map<String, Function<BaseObject, ?>> GETTERS = Map
      .<String, Function<BaseObject, ?>>of(
          FIELD_DOC_REF.getName(), BaseObject::getDocumentReference,
          FIELD_CLASS_REF.getName(), obj -> new ClassReference(obj.getXClassReference()),
          FIELD_NUMBER.getName(), BaseObject::getNumber);

  @Override
  protected Map<String, Function<BaseObject, ?>> getters() {
    return GETTERS;
  }

  @Override
  protected Optional<Object> getRawValue(BaseObject obj, String fieldName) {
    return strFieldAccessor.get(obj, fieldName);
  }

  @Override
  public <V> boolean set(BaseObject obj, ClassField<V> field, V newValue) {
    checkClassRef(obj, field);
    var serializeValue = serializePropertyValue(field, newValue).orElse(null);
    boolean dirty = strFieldAccessor.set(obj, field.getName(), serializeValue);
    if (dirty) {
      logger.debug("set - obj [{}], field [{}], newValue [{}]", obj, field, newValue);
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

}
