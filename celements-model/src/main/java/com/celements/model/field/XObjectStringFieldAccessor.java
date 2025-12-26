package com.celements.model.field;

import static com.google.common.base.Strings.*;

import java.util.Collection;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.context.ModelContext;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * {@link StringFieldAccessor} for accessing {@link BaseObject} properties. Use
 * {@link XObjectFieldAccessor} instead if possible.
 */
@Component(XObjectStringFieldAccessor.NAME)
public class XObjectStringFieldAccessor implements StringFieldAccessor<BaseObject> {

  private static final Logger LOGGER = LoggerFactory.getLogger(XObjectStringFieldAccessor.class);

  public static final String NAME = "xobject";

  @Requirement
  private ModelContext context;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Optional<Object> get(BaseObject obj, String fieldName) {
    try {
      var value = Optional.ofNullable(obj)
          .flatMap(o -> getAndNormalizeValue(o, fieldName));
      LOGGER.info("get - obj [{}], field [{}], value [{}]", obj, fieldName, value);
      return value;
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw createException("failed to get value", obj, fieldName, exc);
    }
  }

  private Optional<Object> getAndNormalizeValue(BaseObject obj, String fieldName) {
    return Optional.ofNullable((BaseProperty) obj.safeget(fieldName))
        .map(BaseProperty::getValue)
        .map(value -> (value instanceof String)
            ? emptyToNull(value.toString().trim()) // avoid comparing empty string to null
            : value);
  }

  @Override
  public boolean set(BaseObject obj, String fieldName, Object newValue) {
    Object currentValue = get(obj, fieldName).map(this::normalizeValue).orElse(null);
    newValue = normalizeValue(newValue);
    if (Objects.equal(newValue, currentValue)) {
      return false;
    }
    try {
      obj.set(fieldName, newValue, context.getXWikiContext());
      LOGGER.info("set - obj [{}], field [{}], newValue [{}], oldValue [{}]",
          obj, fieldName, newValue, currentValue);
      return true;
    } catch (ClassCastException | IllegalArgumentException exc) {
      throw createException("failed to set value '" + newValue + "'", obj, fieldName, exc);
    }
  }

  private Object normalizeValue(Object value) {
    if (value instanceof Collection) {
      return Joiner.on('|').join((Collection<?>) value);
    }
    return value;
  }

  private FieldAccessException createException(String message, BaseObject obj, String fieldName,
      Throwable cause) {
    return new FieldAccessException(message + " on field '" + new ClassReference(
        obj.getXClassReference()).serialize() + "." + fieldName + "'", cause);
  }

}
