package com.celements.model.access;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ClassFieldValue;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@ComponentRole
public interface IModelAccessFacade {

  String DEFAULT_LANG = "";

  @NotNull
  XWikiDocument getDocument(@NotNull DocumentReference docRef)
      throws DocumentNotExistsException;

  @NotNull
  XWikiDocument getDocument(@NotNull DocumentReference docRef, @Nullable String lang)
      throws DocumentNotExistsException;

  @NotNull
  Optional<XWikiDocument> getDocumentOpt(@NotNull DocumentReference docRef);

  @NotNull
  Optional<XWikiDocument> getDocumentOpt(@NotNull DocumentReference docRef, @Nullable String lang);

  @NotNull
  XWikiDocument createDocument(@NotNull DocumentReference docRef)
      throws DocumentAlreadyExistsException;

  @NotNull
  XWikiDocument createDocument(@NotNull DocumentReference docRef, @Nullable String lang)
      throws DocumentAlreadyExistsException;

  @NotNull
  XWikiDocument getOrCreateDocument(@NotNull DocumentReference docRef);

  @NotNull
  XWikiDocument getOrCreateDocument(@NotNull DocumentReference docRef, @Nullable String lang);

  /**
   * @return true if the default document denoted by {@code docRef} exists.
   */
  boolean exists(@NotNull DocumentReference docRef);

  /**
   * @return true if the document denoted by {@code docRef} exists for the given {@code lang}.
   *         This may be a translation or the default document with the inquired {@code lang}.
   */
  boolean existsLang(@NotNull DocumentReference docRef, @Nullable String lang);

  void saveDocument(@NotNull XWikiDocument doc) throws DocumentSaveException;

  void saveDocument(@NotNull XWikiDocument doc, @Nullable String comment)
      throws DocumentSaveException;

  void saveDocument(@NotNull XWikiDocument doc, @Nullable String comment,
      boolean isMinorEdit) throws DocumentSaveException;

  void deleteDocument(@NotNull DocumentReference docRef, boolean totrash)
      throws DocumentDeleteException;

  void deleteDocument(@NotNull XWikiDocument doc, boolean totrash)
      throws DocumentDeleteException;

  void deleteDocumentWithoutTranslations(@NotNull XWikiDocument doc, boolean totrash)
      throws DocumentDeleteException;

  @NotNull
  List<String> getExistingLangs(@NotNull DocumentReference docRef);

  @NotNull
  Map<String, XWikiDocument> getTranslations(@NotNull DocumentReference docRef);

  boolean isTranslation(@NotNull XWikiDocument doc);

  @NotNull
  Stream<XWikiDocument> streamParents(@NotNull XWikiDocument doc);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @return the xobject or null
   * @throws DocumentNotExistsException
   *           if the document does not exist
   */
  @Deprecated
  BaseObject getXObject(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @param key
   *          for field specific xobject filtering (null means no filtering)
   * @param value
   *          for field specific xobject filtering
   * @return the xobject or null
   * @throws DocumentNotExistsException
   *           if the document does not exist
   */
  @Deprecated
  BaseObject getXObject(DocumentReference docRef, DocumentReference classRef, String key,
      Object value) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @return the xobject or null
   */
  @Deprecated
  BaseObject getXObject(XWikiDocument doc, DocumentReference classRef);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @param key
   *          for field specific xobject filtering (null means no filtering)
   * @param value
   *          for field specific xobject filtering
   * @return the xobject or null
   */
  @Deprecated
  BaseObject getXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @param objectNumber
   *          ObjectNumber of the desired XObject
   * @return the xobject in a Optional
   */
  @Deprecated
  com.google.common.base.Optional<BaseObject> getXObject(DocumentReference docRef,
      DocumentReference classRef, int objectNumber) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobject on (may not be null)
   * @param classRef
   *          type of xobject to get (may not be null)
   * @param objectNumber
   *          ObjectNumber of the desired XObject
   * @return the xobject in a Optional
   */
  @Deprecated
  com.google.common.base.Optional<BaseObject> getXObject(XWikiDocument doc,
      DocumentReference classRef, int objectNumber);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @return an unmodifiable list of xobjects (without null values) or empty list
   * @throws DocumentNotExistsException
   *           if the document does not exist
   */
  @Deprecated
  List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param value
   *          for field specific xobjects filtering
   * @return an unmodifiable list of xobjects (without null values) or empty list
   * @throws DocumentNotExistsException
   *           if the document does not exist
   */
  @Deprecated
  List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Object value) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param docRef
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param values
   *          for field specific xobjects filtering
   * @return an unmodifiable list of xobjects (without null values) or empty list
   * @throws DocumentNotExistsException
   *           if the document does not exist
   */
  @Deprecated
  List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Collection<?> values) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @return an unmodifiable list of xobjects (without null values) or empty list
   */
  @Deprecated
  List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param value
   *          for field specific xobjects filtering
   * @return an unmodifiable list of xobjects (without null values) or empty list
   */
  @Deprecated
  List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to get (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param values
   *          for field specific xobjects filtering
   * @return an unmodifiable list of xobjects (without null values) or empty list
   */
  @Deprecated
  List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher}
   * @param doc
   *          to get xobjects on (may not be null)
   * @return
   * @return an unmodifiable map of all xobjects list
   */
  @Deprecated
  Map<DocumentReference, List<BaseObject>> getXObjects(XWikiDocument doc);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to get new xobject on (may not be null)
   * @param classRef
   *          type of xobjects to create (may not be null)
   * @param key
   *          for field specific xobject filtering (null means no filtering)
   * @param value
   *          for field specific xobject filtering
   * @return newly created xobject with set key - value
   */
  @Deprecated
  BaseObject newXObject(XWikiDocument doc, DocumentReference classRef);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to get or create new xobject on (may not be null)
   * @param classRef
   *          type of xobjects to create (may not be null)
   * @return already existing or newly created xobject
   */
  @Deprecated
  BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to get or create new xobject on (may not be null)
   * @param classRef
   *          type of xobjects to create (may not be null)
   * @return already existing or newly created xobject
   */
  @Deprecated
  BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to remove xobject on (may not be null)
   * @param objsToRemove
   *          xobject to remove
   * @return true if doc has changed
   */
  @Deprecated
  boolean removeXObject(XWikiDocument doc, BaseObject objToRemove);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to remove xobjects on (may not be null)
   * @param objsToRemove
   *          xobjects to remove
   * @return true if doc has changed
   */
  @Deprecated
  boolean removeXObjects(XWikiDocument doc, List<BaseObject> objsToRemove);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to remove xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to remove
   * @return true if doc has changed
   */
  @Deprecated
  boolean removeXObjects(XWikiDocument doc, DocumentReference classRef);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to remove xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to remove (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param value
   *          for field specific xobjects filtering
   * @return true if doc has changed
   */
  @Deprecated
  boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value);

  /**
   * @deprecated instead use {@link XWikiObjectEditor}
   * @param doc
   *          to remove xobjects on (may not be null)
   * @param classRef
   *          type of xobjects to remove (may not be null)
   * @param key
   *          for field specific xobjects filtering (null means no filtering)
   * @param values
   *          for field specific xobjects filtering
   * @return true if doc has changed
   */
  @Deprecated
  boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values);

  /**
   * @deprecated instead use {@link #getDocument(DocumentReference)} with
   *             {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @Deprecated
  Object getProperty(DocumentReference docRef, DocumentReference classRef, String name)
      throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @Deprecated
  Object getProperty(XWikiDocument doc, DocumentReference classRef, String name);

  /**
   * Reads out the property value for the given BaseObject and name
   *
   * @deprecated instead use {@link XObjectFieldAccessor#get(BaseObject, ClassField)}
   */
  @Deprecated
  Object getProperty(BaseObject obj, String name);

  /**
   * @deprecated instead use {@link #getDocument(DocumentReference)} with
   *             {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @Nullable
  @Deprecated
  <T> T getProperty(@NotNull DocumentReference docRef, @NotNull ClassField<T> field)
      throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @Nullable
  @Deprecated
  <T> T getProperty(@NotNull XWikiDocument doc, @NotNull ClassField<T> field);

  /**
   * @deprecated instead use {@link XObjectFieldAccessor#get(BaseObject, ClassField)}
   */
  @NotNull
  @Deprecated
  <T> com.google.common.base.Optional<T> getFieldValue(@NotNull BaseObject obj,
      @NotNull ClassField<T> field);

  /**
   * @deprecated instead use {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @NotNull
  @Deprecated
  <T> com.google.common.base.Optional<T> getFieldValue(@NotNull XWikiDocument doc,
      @NotNull ClassField<T> field);

  /**
   * @deprecated instead use {@link #getDocument(DocumentReference)} with
   *             {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @NotNull
  @Deprecated
  <T> com.google.common.base.Optional<T> getFieldValue(@NotNull DocumentReference docRef,
      @NotNull ClassField<T> field) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @NotNull
  @Deprecated
  <T> com.google.common.base.Optional<T> getFieldValue(@NotNull XWikiDocument doc,
      @NotNull ClassField<T> field, T ignoreValue);

  /**
   * @deprecated instead use {@link #getDocument(DocumentReference)} with
   *             {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @NotNull
  @Deprecated
  <T> com.google.common.base.Optional<T> getFieldValue(@NotNull DocumentReference docRef,
      @NotNull ClassField<T> field, T ignoreValue) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectFetcher#fetchField(ClassField)}
   */
  @NotNull
  @Deprecated
  List<ClassFieldValue<?>> getProperties(@NotNull XWikiDocument doc,
      @NotNull ClassDefinition classDef);

  /**
   * @deprecated instead use {@link XObjectFieldAccessor#set(BaseObject, ClassField, Object)}
   */
  @Deprecated
  boolean setProperty(BaseObject obj, String name, Object value);

  /**
   * @deprecated instead use {@link #getDocument(DocumentReference)} with
   *             {@link XWikiObjectEditor#editField(ClassField)}
   */
  @Deprecated
  <T> XWikiDocument setProperty(@NotNull DocumentReference docRef,
      @NotNull ClassField<T> field, @Nullable T value) throws DocumentNotExistsException;

  /**
   * @deprecated instead use {@link XWikiObjectEditor#editField(ClassField)}
   */
  @Deprecated
  <T> boolean setProperty(@NotNull XWikiDocument doc, @NotNull ClassField<T> field,
      @Nullable T value);

  /**
   * @deprecated instead use {@link XWikiObjectEditor#editField(ClassField)}
   */
  @Deprecated
  <T> boolean setProperty(XWikiDocument doc, ClassFieldValue<T> fieldValue);

  /**
   * @deprecated instead use {@link XObjectFieldAccessor#set(BaseObject, ClassField, Object)}
   */
  @Deprecated
  <T> boolean setProperty(@NotNull BaseObject obj, @NotNull ClassField<T> field,
      @Nullable T value);

  /**
   * CAUTION: document.getAttachment returns "startWith" matches. Instead use
   * getAttachmentNameEqual or methods on IAttachmentServiceRole
   *
   * @param document
   * @param filename
   * @return
   * @throws AttachmentNotExistsException
   */
  XWikiAttachment getAttachmentNameEqual(XWikiDocument document, String filename)
      throws AttachmentNotExistsException;

  /**
   * getApiDocument creates a com.xpn.xwiki.api.Document for <code>doc</code>
   *
   * @param doc
   * @return an api Document object or null
   * @throws NoAccessRightsException
   *           if current context user has no view rights
   */
  Document getApiDocument(XWikiDocument doc) throws NoAccessRightsException;

  /**
   * getApiObject creates a com.xpn.xwiki.api.Object for <code>obj</code>
   *
   * @param obj
   * @return
   * @throws NoAccessRightsException
   *           if current context user has no view rights
   */
  com.xpn.xwiki.api.Object getApiObject(BaseObject obj) throws NoAccessRightsException;

  /**
   * getApiObject creates a com.xpn.xwiki.api.Object for <code>obj</code>
   *
   * @param obj
   * @return
   **/
  com.xpn.xwiki.api.Object getApiObjectWithoutRightCheck(BaseObject obj);

  /**
   * getApiObjects creates for each valid BaseObject in <code>objs</code> a
   * com.xpn.xwiki.api.Object. A BaseObject is valid if it is not null, has a correct
   * DocumentReference set and the context user has view rights an that document. Invalid
   * BaseObjects are omitted, thus the returned list may be smaller.
   *
   * @param objs
   * @return
   */
  List<com.xpn.xwiki.api.Object> getApiObjects(List<BaseObject> objs);

  /**
   * getApiObjects creates for each valid BaseObject in <code>objs</code> a
   * com.xpn.xwiki.api.Object. A BaseObject is valid if it is not null.
   *
   * @param objs
   * @return
   */
  List<com.xpn.xwiki.api.Object> getApiObjectsWithoutRightChecks(List<BaseObject> objs);

}
