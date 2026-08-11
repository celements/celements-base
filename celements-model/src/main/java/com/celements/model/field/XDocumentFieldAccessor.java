package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiDocumentClass.*;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.EntityReference;

import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.doc.XWikiDocument;

import one.util.streamex.EntryStream;

/**
 * {@link FieldAccessor} for accessing {@link XWikiDocument} properties
 */
@Component(XDocumentFieldAccessor.NAME)
public class XDocumentFieldAccessor extends AbstractDocumentFieldAccessor<XWikiDocument> {

  public static final String NAME = "XDocumentFieldAccessor";

  @Override
  public String getName() {
    return NAME;
  }

  static final Map<String, Function<XWikiDocument, ?>> GETTERS = EntryStream
      .of(ImmutableMap.<ClassField<?>, Function<XWikiDocument, ?>>builder()
          .put(FIELD_DOC_REF, XWikiDocument::getDocumentReference)
          .put(FIELD_PARENT_REF, XWikiDocument::getParentReference)
          .put(FIELD_LANGUAGE, XWikiDocument::getLanguage)
          .put(FIELD_DEFAULT_LANGUAGE, XWikiDocument::getDefaultLanguage)
          .put(FIELD_TRANSLATION, XWikiDocument::isTrans)
          .put(FIELD_CREATOR, XWikiDocument::getCreator)
          .put(FIELD_AUTHOR, XWikiDocument::getAuthor)
          .put(FIELD_CONTENT_AUTHOR, XWikiDocument::getContentAuthor)
          .put(FIELD_CREATION_DATE, XWikiDocument::getCreationDate)
          .put(FIELD_UPDATE_DATE, XWikiDocument::getDate)
          .put(FIELD_CONTENT_UPDATE_DATE, XWikiDocument::getContentUpdateDate)
          .put(FIELD_TITLE, XWikiDocument::getTitle)
          .put(FIELD_CONTENT, XWikiDocument::getContent)
          .build())
      .mapKeys(ClassField::getName)
      .toImmutableMap();

  @Override
  protected Map<String, Function<XWikiDocument, ?>> getters() {
    return GETTERS;
  }

  @Override
  public <V> boolean set(XWikiDocument doc, ClassField<V> field, V value) {
    if (!Objects.equal(value, get(doc, field).orElse(null))) {
      BiConsumer<XWikiDocument, Object> setter = SETTERS.get(field.getName());
      if (setter != null) {
        try {
          setter.accept(doc, value);
          logger.info("set: '{}' for '{}' from '{}'", value, field, doc.getDocumentReference());
          return true;
        } catch (ClassCastException cce) {
          logger.warn("set: illegal value '{}' for '{}' from '{}'",
              value, field, doc.getDocumentReference(), cce);
        }
      } else {
        throw new FieldAccessException("undefined field: " + field);
      }
    }
    return false;
  }

  static final Map<String, BiConsumer<XWikiDocument, Object>> SETTERS = EntryStream
      .of(ImmutableMap.<ClassField<?>, BiConsumer<XWikiDocument, Object>>builder()
          .put(FIELD_PARENT_REF, asObj(XWikiDocument::setParentReference, EntityReference.class))
          .put(FIELD_LANGUAGE, asObj(XWikiDocument::setLanguage))
          .put(FIELD_DEFAULT_LANGUAGE, asObj(XWikiDocument::setDefaultLanguage))
          .put(FIELD_TRANSLATION, asBool(XWikiDocument::setTranslation))
          .put(FIELD_CREATOR, asObj(XWikiDocument::setCreator))
          .put(FIELD_AUTHOR, asObj(XWikiDocument::setAuthor))
          .put(FIELD_CONTENT_AUTHOR, asObj(XWikiDocument::setContentAuthor))
          .put(FIELD_CREATION_DATE, asObj(XWikiDocument::setCreationDate))
          .put(FIELD_UPDATE_DATE, asObj(XWikiDocument::setDate))
          .put(FIELD_CONTENT_UPDATE_DATE, asObj(XWikiDocument::setContentUpdateDate))
          .put(FIELD_TITLE, asObj(XWikiDocument::setTitle))
          .put(FIELD_CONTENT, asObj(XWikiDocument::setContent, String.class))
          .build())
      .mapKeys(ClassField::getName)
      .toImmutableMap();

  @SuppressWarnings("unchecked")
  private static <T> BiConsumer<XWikiDocument, Object> asObj(BiConsumer<XWikiDocument, T> c) {
    return (doc, value) -> c.accept(doc, (T) value);
  }

  private static <T> BiConsumer<XWikiDocument, Object> asObj(BiConsumer<XWikiDocument, T> c,
      Class<T> type) {
    return asObj(c);
  }

  private static BiConsumer<XWikiDocument, Object> asBool(ObjIntConsumer<XWikiDocument> c) {
    return (doc, value) -> c.accept(doc, Boolean.TRUE.equals(value) ? 1 : 0);
  }

}
