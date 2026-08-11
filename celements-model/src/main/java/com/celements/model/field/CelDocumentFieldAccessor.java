package com.celements.model.field;

import static com.celements.web.classes.oldcore.XWikiDocumentClass.*;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.celements.model.classes.fields.ClassField;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.doc.CelDocument;

import one.util.streamex.EntryStream;

/**
 * Read-only {@link FieldAccessor} for accessing {@link CelDocument} properties.
 */
@Component
public class CelDocumentFieldAccessor extends AbstractDocumentFieldAccessor<CelDocument> {

  @Override
  public String getName() {
    return "CelDocumentFieldAccessor";
  }

  static final Map<String, Function<CelDocument, ?>> GETTERS = EntryStream
      .of(ImmutableMap.<ClassField<?>, Function<CelDocument, ?>>builder()
          .put(FIELD_DOC_REF, CelDocument::getDocumentReference)
          .put(FIELD_PARENT_REF, CelDocument::getParentReference)
          .put(FIELD_LANGUAGE, CelDocument::getLanguage)
          .put(FIELD_DEFAULT_LANGUAGE, CelDocument::getDefaultLanguage)
          .put(FIELD_TRANSLATION, CelDocument::isTrans)
          .put(FIELD_CREATOR, CelDocument::getCreator)
          .put(FIELD_AUTHOR, CelDocument::getAuthor)
          .put(FIELD_CONTENT_AUTHOR, CelDocument::getContentAuthor)
          .put(FIELD_CREATION_DATE, doc -> toDate(doc.getCreationDate()))
          .put(FIELD_UPDATE_DATE, doc -> toDate(doc.getDate()))
          .put(FIELD_CONTENT_UPDATE_DATE, doc -> toDate(doc.getContentUpdateDate()))
          .put(FIELD_TITLE, CelDocument::getTitle)
          .put(FIELD_CONTENT, CelDocument::getContent)
          .build())
      .mapKeys(ClassField::getName)
      .toImmutableMap();

  @Override
  protected Map<String, Function<CelDocument, ?>> getters() {
    return GETTERS;
  }

  private static Date toDate(Instant instant) {
    return Optional.ofNullable(instant).map(Date::from).orElse(null);
  }

  @Override
  public <V> boolean set(CelDocument doc, ClassField<V> field, V newValue) {
    throw new UnsupportedOperationException("CelDocument is immutable");
  }

}
