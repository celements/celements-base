package com.xpn.xwiki.doc;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Deeply immutable Celements representation of an {@link XWikiDocument}.
 */
public abstract sealed class CelDocument {

  public static CelDocument from(XWikiDocument doc) {
    return requireNonNull(doc).isTrans() ? Translation.from(doc) : Default.from(doc);
  }

  private final Identity identity;
  private final MetaData metaData;

  private CelDocument(XWikiDocument doc) {
    requireNonNull(doc);
    identity = Identity.from(doc);
    metaData = MetaData.from(doc);
  }

  public final MetaData getMetaData() {
    return metaData;
  }

  public final Identity getIdentity() {
    return identity;
  }

  public final DocumentReference getDocRef() {
    return getIdentity().docRef();
  }

  public final DocumentReference getDocumentReference() {
    return getDocRef();
  }

  public final WikiReference getWikiRef() {
    return getDocRef().getWikiReference();
  }

  public final long getId() {
    return getIdentity().id();
  }

  public final IdVersion getIdVersion() {
    return getIdentity().idVersion();
  }

  public final String getLanguage() {
    return getDocumentReference().getLocale().map(Locale::toString).orElse("");
  }

  public final String getDefaultLanguage() {
    return metaData.defaultLanguage();
  }

  public final int getTranslation() {
    return metaData.translation();
  }

  public final boolean isTrans() {
    return getTranslation() != 0;
  }

  public final String getContent() {
    return metaData.content();
  }

  public final String getTitle() {
    return metaData.title();
  }

  public final String getCreator() {
    return metaData.creator();
  }

  public final String getAuthor() {
    return metaData.author();
  }

  public final String getContentAuthor() {
    return metaData.contentAuthor();
  }

  public final String getCustomClass() {
    return metaData.customClass();
  }

  public final DocumentReference getParentReference() {
    return metaData.parentRef();
  }

  public final String getXClassXML() {
    return metaData.xClassXML();
  }

  public final Instant getDate() {
    return metaData.updateDate();
  }

  public final Instant getContentUpdateDate() {
    return metaData.contentUpdateDate();
  }

  public final Instant getCreationDate() {
    return metaData.creationDate();
  }

  public final String getVersion() {
    return getIdentity().version();
  }

  public final boolean isHidden() {
    return metaData.hidden();
  }

  public final String getComment() {
    return metaData.comment();
  }

  public final boolean isMinorEdit() {
    return metaData.minorEdit();
  }

  public final int getElements() {
    return metaData.elements();
  }

  public final String getDefaultTemplate() {
    return metaData.defaultTemplate();
  }

  public final String getValidationScript() {
    return metaData.validationScript();
  }

  @Override
  public final String toString() {
    return getDocRef().toString();
  }

  public record Identity(
      long id,
      IdVersion idVersion,
      DocumentReference docRef,
      String version) {

    public Identity {
      requireNonNull(docRef);
    }

    public static Identity from(XWikiDocument doc) {
      requireNonNull(doc);
      return new Identity(
          doc.hasValidId() ? doc.getId() : 0,
          doc.hasValidId() ? doc.getIdVersion() : null,
          doc.getDocRefWithLocale(),
          doc.getVersion());
    }
  }

  public record MetaData(
      String title,
      String defaultLanguage,
      int translation,
      Instant updateDate,
      Instant contentUpdateDate,
      Instant creationDate,
      String author,
      String contentAuthor,
      String creator,
      String content,
      String customClass,
      DocumentReference parentRef,
      String xClassXML,
      int elements,
      String defaultTemplate,
      String validationScript,
      String comment,
      boolean minorEdit,
      boolean hidden) {

    public static MetaData from(XWikiDocument doc) {
      requireNonNull(doc);
      return new MetaData(
          doc.getTitle(),
          doc.getDefaultLanguage(),
          doc.getTranslation(),
          Optional.ofNullable(doc.getDate()).map(Date::toInstant).orElse(null),
          Optional.ofNullable(doc.getContentUpdateDate()).map(Date::toInstant).orElse(null),
          Optional.ofNullable(doc.getCreationDate()).map(Date::toInstant).orElse(null),
          doc.getAuthor(),
          doc.getContentAuthor(),
          doc.getCreator(),
          doc.getContent(),
          doc.getCustomClass(),
          doc.getParentReference(),
          doc.getXClassXML(),
          doc.getElements(),
          doc.getDefaultTemplate(),
          doc.getValidationScript(),
          doc.getComment(),
          doc.isMinorEdit(),
          doc.isHidden());
    }
  }

  public static final class Default extends CelDocument {

    public static Default from(XWikiDocument doc) {
      return new Default(doc);
    }

    private final BaseClass xClass;
    private final List<CelObject> xObjects;
    private final List<CelAttachment> attachments;

    private Default(XWikiDocument doc) {
      super(doc);
      checkArgument(!doc.isTrans(), "default document must have translation = 0");
      xClass = doc.getXClass().getFieldList().isEmpty() ? null
          : (BaseClass) doc.getXClass().clone();
      xObjects = toCelObjects(doc);
      attachments = doc.getAttachmentList().stream().map(CelAttachment::from).toList();
    }

    private static List<CelObject> toCelObjects(XWikiDocument doc) {
      return doc.getXObjects().entrySet().stream()
          .flatMap(entry -> entry.getValue().stream()
              .filter(Objects::nonNull)
              .map(obj -> CelObject.from(obj, Optional
                  .ofNullable(obj.getXClassReference())
                  .orElse(entry.getKey()))))
          .toList();
    }

    public Optional<BaseClass> getXClass() {
      return Optional.ofNullable(xClass).map(x -> (BaseClass) x.clone());
    }

    public List<CelObject> getXObjects() {
      return xObjects;
    }

    public Stream<CelObject> streamXObjects(EntityReference classRef) {
      var localClassRef = new LocalDocumentReference(classRef);
      return xObjects.stream().filter(x -> x.getClassReference().equals(localClassRef));
    }

    public List<CelAttachment> getAttachmentList() {
      return attachments;
    }
  }

  public static final class Translation extends CelDocument {

    public static Translation from(XWikiDocument doc) {
      return new Translation(doc);
    }

    private Translation(XWikiDocument doc) {
      super(doc);
      checkArgument(doc.isTrans(), "doc must have translation != 0");
      checkArgument(!doc.getLanguage().isEmpty(), "translation doc without language");
    }
  }

}
