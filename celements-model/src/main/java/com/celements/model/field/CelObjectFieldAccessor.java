package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiObjectClass.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.fields.ClassField;
import com.xpn.xwiki.doc.CelObject;
import com.xpn.xwiki.doc.CelProperty;

/**
 * Read-only {@link FieldAccessor} for accessing {@link CelObject} properties.
 */
@Component
public class CelObjectFieldAccessor extends AbstractObjectFieldAccessor<CelObject> {

  @Override
  public String getName() {
    return "CelObjectFieldAccessor";
  }

  static final Map<String, Function<CelObject, ?>> GETTERS = Map
      .<String, Function<CelObject, ?>>of(
          FIELD_DOC_REF.getName(), CelObject::getDocumentReference,
          FIELD_CLASS_REF.getName(), obj -> new ClassReference(obj.getClassReference()),
          FIELD_NUMBER.getName(), CelObject::getNumber);

  @Override
  protected Map<String, Function<CelObject, ?>> getters() {
    return GETTERS;
  }

  @Override
  protected Optional<Object> getRawValue(CelObject obj, String fieldName) {
    return obj.getProperty(fieldName).map(CelProperty::getValue);
  }

  @Override
  public <V> boolean set(CelObject obj, ClassField<V> field, V newValue) {
    throw new UnsupportedOperationException("CelObject is immutable");
  }

}
