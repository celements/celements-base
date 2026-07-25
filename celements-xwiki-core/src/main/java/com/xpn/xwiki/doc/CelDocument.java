package com.xpn.xwiki.doc;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;

import java.time.Instant;
import java.util.Date;
import java.util.List;
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
public abstract sealed class CelDocument permits CelDocument.Default, CelDocument.Translation {

  private final DocumentReference docRef;
  private final long id;
  private final IdVersion idVersion;
  private final String language;
  private final String defaultLanguage;
  private final int translation;
  private final String content;
  private final String title;
  private final String format;
  private final String meta;
  private final String creator;
  private final String author;
  private final String contentAuthor;
  private final String customClass;
  private final DocumentReference parentRef;
  private final DocumentReference templateDocRef;
  private final Instant updateDate;
  private final Instant contentUpdateDate;
  private final Instant creationDate;
  private final String version;
  private final boolean mostRecent;
  private final boolean newDocument;
  private final boolean hidden;
  private final String comment;
  private final boolean minorEdit;
  private final boolean contentDirty;
  private final boolean metaDataDirty;
  private final int elements;
  private final String defaultTemplate;
  private final String validationScript;

  private CelDocument(XWikiDocument doc) {
    requireNonNull(doc);
    docRef = requireNonNull(doc.getDocumentReference());
    id = doc.hasValidId() ? doc.getId() : 0;
    idVersion = doc.hasValidId() ? doc.getIdVersion() : null;
    language = doc.getLanguage();
    defaultLanguage = doc.getDefaultLanguage();
    translation = doc.getTranslation();
    content = doc.getContent();
    title = doc.getTitle();
    format = doc.getFormat();
    meta = doc.getMeta();
    creator = doc.getCreator();
    author = doc.getAuthor();
    contentAuthor = doc.getContentAuthor();
    customClass = doc.getCustomClass();
    parentRef = doc.getParentReference();
    templateDocRef = doc.getTemplateDocumentReference();
    updateDate = Optional.ofNullable(doc.getDate()).map(Date::toInstant).orElse(null);
    contentUpdateDate = Optional.ofNullable(doc.getContentUpdateDate()).map(Date::toInstant)
        .orElse(null);
    creationDate = Optional.ofNullable(doc.getCreationDate()).map(Date::toInstant).orElse(null);
    version = doc.getVersion();
    mostRecent = doc.isMostRecent();
    newDocument = doc.isNew();
    hidden = doc.isHidden();
    comment = doc.getComment();
    minorEdit = doc.isMinorEdit();
    contentDirty = doc.isContentDirty();
    metaDataDirty = doc.isMetaDataDirty();
    elements = doc.getElements();
    defaultTemplate = doc.getDefaultTemplate();
    validationScript = doc.getValidationScript();
  }

  public final DocumentReference getDocRef() {
    return docRef;
  }

  public final DocumentReference getDocumentReference() {
    return getDocRef();
  }

  public final WikiReference getWikiRef() {
    return getDocRef().getWikiReference();
  }

  public final long getId() {
    return id;
  }

  public final IdVersion getIdVersion() {
    return idVersion;
  }

  public final String getLanguage() {
    return language;
  }

  public final String getDefaultLanguage() {
    return defaultLanguage;
  }

  public final int getTranslation() {
    return translation;
  }

  public final boolean isTrans() {
    return getTranslation() != 0;
  }

  public final String getContent() {
    return content;
  }

  public final String getTitle() {
    return title;
  }

  public final String getFormat() {
    return format;
  }

  public final String getMeta() {
    return meta;
  }

  public final String getCreator() {
    return creator;
  }

  public final String getAuthor() {
    return author;
  }

  public final String getContentAuthor() {
    return contentAuthor;
  }

  public final String getCustomClass() {
    return customClass;
  }

  public final DocumentReference getParentReference() {
    return parentRef;
  }

  public final DocumentReference getTemplateDocumentReference() {
    return templateDocRef;
  }

  public final Instant getDate() {
    return updateDate;
  }

  public final Instant getContentUpdateDate() {
    return contentUpdateDate;
  }

  public final Instant getCreationDate() {
    return creationDate;
  }

  public final String getVersion() {
    return version;
  }

  public final boolean isMostRecent() {
    return mostRecent;
  }

  public final boolean isNew() {
    return newDocument;
  }

  public final boolean isHidden() {
    return hidden;
  }

  public final String getComment() {
    return comment;
  }

  public final boolean isMinorEdit() {
    return minorEdit;
  }

  public final boolean isContentDirty() {
    return contentDirty;
  }

  public final boolean isMetaDataDirty() {
    return metaDataDirty;
  }

  public final int getElements() {
    return elements;
  }

  public final String getDefaultTemplate() {
    return defaultTemplate;
  }

  public final String getValidationScript() {
    return validationScript;
  }

  public static CelDocument from(XWikiDocument doc) {
    requireNonNull(doc);
    return doc.isTrans()
        ? Translation.from(doc)
        : Default.from(doc);
  }

  public static CelDocument empty(DocumentReference docRef) {
    return from(new XWikiDocument(docRef));
  }

  public static final class Default extends CelDocument {

    private final String xClassXML;
    private final BaseClass xClass;
    private final List<CelObject> xObjects;
    private final List<CelAttachment> attachments;

    private Default(XWikiDocument doc) {
      super(doc);
      checkArgument(!doc.isTrans(), "default document must have translation = 0");
      xClassXML = doc.getXClassXML();
      xClass = doc.getXClass().getFieldList().isEmpty() ? null : (BaseClass) doc.getXClass().clone();
      xObjects = toCelObjects(doc);
      attachments = toCelAttachments(doc);
    }

    public static Default from(XWikiDocument doc) {
      return new Default(doc);
    }

    public String getXClassXML() {
      return xClassXML;
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

    private static List<CelObject> toCelObjects(XWikiDocument doc) {
      return doc.getXObjects().entrySet().stream()
          .flatMap(entry -> entry.getValue().stream()
              .filter(Objects::nonNull)
              .map(object -> CelObject.from(object,
                  Optional.ofNullable(object.getXClassReference()).orElse(entry.getKey()))))
          .toList();
    }

    private static List<CelAttachment> toCelAttachments(XWikiDocument doc) {
      return doc.getAttachmentList().stream()
          .map(CelAttachment::from)
          .toList();
    }
  }

  public static final class Translation extends CelDocument {

    private Translation(XWikiDocument doc) {
      super(doc);
      checkArgument(doc.isTrans(), "doc must have translation != 0");
    }

    public static Translation from(XWikiDocument doc) {
      return new Translation(doc);
    }
  }

}
