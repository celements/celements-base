package com.celements.filebase;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.doc.XWikiDocument;

public final class FileBaseTag {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileBaseTag.class);

  public static final ClassReference FILEBASE_TAG_CLASS_REF = new ClassReference("Classes",
      "FilebaseTag");
  public static final ClassField<String> ATTACHMENT_FIELD = new StringField.Builder(
      FILEBASE_TAG_CLASS_REF, "attachment").build();

  private final DocumentReference tagRef;
  private final Map<String, String> prettyNames;
  private final IModelAccessFacade modelAccess;
  private final ModelUtils modelUtils;
  private final ModelContext modelContext;

  public FileBaseTag(DocumentReference tagRef, Map<String, String> prettyNames,
      IModelAccessFacade modelAccess, ModelUtils modelUtils, ModelContext modelContext) {
    this.tagRef = tagRef;
    this.prettyNames = prettyNames != null ? Collections.unmodifiableMap(prettyNames)
        : Collections.emptyMap();
    this.modelAccess = modelAccess;
    this.modelUtils = modelUtils;
    this.modelContext = modelContext;
  }

  public DocumentReference getTagRef() {
    return tagRef;
  }

  public Map<String, String> getPrettyNames() {
    return prettyNames;
  }

  public String getPrettyName() {
    return Stream.of(modelContext.getLanguage().orElse(null), modelContext.getDefaultLanguage())
        .filter(lang -> !StringUtils.isBlank(lang))
        .map(prettyNames::get)
        .filter(name -> !StringUtils.isBlank(name))
        .findFirst()
        .orElse(tagRef.getName());
  }

  public List<AttachmentReference> getTagFileList() {
    XWikiDocument tagDoc = modelAccess.getOrCreateDocument(tagRef);
    return XWikiObjectFetcher.on(tagDoc)
        .filter(FILEBASE_TAG_CLASS_REF)
        .stream()
        .map(obj -> obj.getStringValue("attachment"))
        .filter(s -> !StringUtils.isBlank(s) && s.contains("/"))
        .map(this::toAttachmentRef)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private AttachmentReference toAttachmentRef(String attachmentStr) {
    try {
      String[] parts = attachmentStr.split("/", 2);
      if (parts.length == 2) {
        DocumentReference docRef = modelUtils.resolveRef(parts[0], DocumentReference.class,
            tagRef.getWikiReference());
        return new AttachmentReference(parts[1], docRef);
      }
    } catch (Exception exp) {
      LOGGER.debug("invalid attachment reference [{}]", attachmentStr, exp);
    }
    return null;
  }

}
