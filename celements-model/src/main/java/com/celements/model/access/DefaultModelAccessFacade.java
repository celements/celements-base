package com.celements.model.access;

import static com.celements.common.MoreObjectsCel.*;
import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingRunnable;
import com.celements.common.lambda.LambdaExceptionUtil.ThrowingSupplier;
import com.celements.filebase.IAttachmentServiceRole;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.access.exception.ModelAccessRuntimeException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.context.Contextualiser;
import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.StringFieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.model.field.XObjectStringFieldAccessor;
import com.celements.model.object.ObjectFetcher;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ClassFieldValue;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.celements.store.ModelAccessStore;
import com.celements.store.StoreFactory;
import com.celements.wiki.WikiService;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

import one.util.streamex.StreamEx;

@Component
public class DefaultModelAccessFacade implements IModelAccessFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelAccessFacade.class);

  private final Supplier<XWikiStoreInterface> mainStore = Suppliers
      .memoize(StoreFactory::getMainStore);
  protected final XWikiDocumentCreator docCreator;
  protected final IRightsAccessFacadeRole rightsAccess;
  protected final ModelUtils modelUtils;
  protected final ModelContext context;
  protected final WikiService wikiService;
  protected final FieldAccessor<BaseObject> xObjFieldAccessor;
  protected final StringFieldAccessor<BaseObject> xObjStrFieldAccessor;

  @Inject
  public DefaultModelAccessFacade(
      XWikiDocumentCreator docCreator,
      IRightsAccessFacadeRole rightsAccess,
      ModelUtils modelUtils,
      ModelContext context,
      @Lazy WikiService wikiService,
      @Named(XObjectFieldAccessor.NAME) FieldAccessor<BaseObject> xObjFieldAccessor,
      @Named(XObjectStringFieldAccessor.NAME) StringFieldAccessor<BaseObject> xObjStrFieldAccessor) {
    this.docCreator = docCreator;
    this.rightsAccess = rightsAccess;
    this.modelUtils = modelUtils;
    this.context = context;
    this.wikiService = wikiService;
    this.xObjFieldAccessor = xObjFieldAccessor;
    this.xObjStrFieldAccessor = xObjStrFieldAccessor;
  }

  @Override
  public XWikiDocument getDocument(DocumentReference docRef) throws DocumentNotExistsException {
    return getDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public XWikiDocument getDocument(DocumentReference docRef, String lang)
      throws DocumentNotExistsException {
    checkNotNull(docRef);
    var mainDoc = getCelDocument(docRef, DEFAULT_LANG)
        .orElseThrow(() -> new DocumentNotExistsException(docRef, DEFAULT_LANG));
    String language = modelUtils.normalizeLang(lang);
    if (language.equals(DEFAULT_LANG) || language.equals(mainDoc.getDefaultLanguage())) {
      return XWikiDocument.from(mainDoc);
    }
    return getCelDocument(docRef, language)
        .map(XWikiDocument::from)
        .orElseThrow(() -> new DocumentNotExistsException(docRef, language));
  }

  @Override
  public Document getApiDocument(XWikiDocument doc) throws NoAccessRightsException {
    if (rightsAccess.hasAccessLevel(doc.getDocumentReference(), EAccessLevel.VIEW)) {
      return doc.newDocument(context.getXWikiContext());
    }
    throw new NoAccessRightsException(doc.getDocumentReference(), context.getUser(),
        EAccessLevel.VIEW);
  }

  @Override
  public Optional<CelDocument> getCelDocument(DocumentReference docRef) {
    return getCelDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public Optional<CelDocument> getCelDocument(DocumentReference docRef, String lang) {
    checkNotNull(docRef);
    var language = modelUtils.normalizeLang(lang);
    if (!wikiService.hasWiki(docRef.getWikiReference())) {
      return Optional.empty();
    }
    try {
      return contextualise(docRef, () -> getStore().loadCelDocument(docRef, language));
    } catch (XWikiException exc) {
      throw new DocumentLoadException(docRef, exc);
    }
  }

  @Override
  public Optional<XWikiDocument> getDocumentOpt(DocumentReference docRef) {
    return getDocumentOpt(docRef, DEFAULT_LANG);
  }

  @Override
  public Optional<XWikiDocument> getDocumentOpt(DocumentReference docRef, String lang) {
    checkNotNull(docRef);
    String language = modelUtils.normalizeLang(lang);
    // Missing documents are expected here; avoid the throwing getDocument path on this hot API.
    return getCelDocument(docRef, DEFAULT_LANG)
        .flatMap(doc -> (language.equals(DEFAULT_LANG) || language.equals(doc.getDefaultLanguage()))
            ? Optional.of(doc)
            : getCelDocument(docRef, language))
        .map(XWikiDocument::from);
  }

  @Override
  public XWikiDocument createDocument(DocumentReference docRef)
      throws DocumentAlreadyExistsException {
    return createDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public XWikiDocument createDocument(DocumentReference docRef, String lang)
      throws DocumentAlreadyExistsException {
    checkNotNull(docRef);
    lang = modelUtils.normalizeLang(lang);
    if (!existsLang(docRef, lang)) {
      return docCreator.create(docRef, lang);
    } else {
      throw new DocumentAlreadyExistsException(docRef, lang);
    }
  }

  @Override
  public XWikiDocument getOrCreateDocument(DocumentReference docRef) {
    return getOrCreateDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public XWikiDocument getOrCreateDocument(DocumentReference docRef, String lang) {
    try {
      return getDocument(docRef, lang);
    } catch (DocumentNotExistsException exc) {
      lang = modelUtils.normalizeLang(lang);
      return docCreator.create(docRef, lang);
    }
  }

  @Override
  public boolean exists(DocumentReference docRef) {
    if ((docRef == null) || !wikiService.hasWiki(docRef.getWikiReference())) {
      return false;
    }
    try {
      XWikiDocument doc = docCreator.createWithoutDefaults(docRef);
      return contextualise(docRef, () -> getStore().exists(doc, getXContext()));
    } catch (XWikiException exc) {
      throw new DocumentLoadException(docRef, exc);
    }
  }

  @Override
  public boolean existsLang(DocumentReference docRef, String lang) {
    if ((docRef == null) || !exists(docRef)) {
      return false;
    }
    String language = modelUtils.normalizeLang(lang);
    return DEFAULT_LANG.equals(language) || getCelDocument(docRef, DEFAULT_LANG)
        .filter(doc -> language.equals(doc.getDefaultLanguage()))
        .or(() -> getCelDocument(docRef, language))
        .isPresent();
  }

  @Override
  public void saveDocument(XWikiDocument doc) throws DocumentSaveException {
    saveDocument(doc, "", false);
  }

  @Override
  public void saveDocument(XWikiDocument doc, String comment) throws DocumentSaveException {
    saveDocument(doc, comment, false);
  }

  @Override
  public void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit)
      throws DocumentSaveException {
    checkNotNull(doc);
    prepareDocForSave(doc, comment, isMinorEdit);
    sanitizeLangBeforeSave(doc);
    LOGGER.info("saveDocument: doc '{}, {}', comment '{}', isMinorEdit '{}'",
        serialize(doc.getDocumentReference()), doc.getLanguage(), comment, isMinorEdit);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("saveDocument: context db '{}' and StackTrace:",
          serialize(context.getWikiRef()), new Throwable());
    }
    boolean isNewDoc = doc.isNew();
    XWikiDocument origDocBeforeSave = Optional.ofNullable(doc.getOriginalDocument())
        .orElseGet(() -> docCreator.create(doc.getDocumentReference(), doc.getLanguage()));
    notifyEvent(doc, isNewDoc ? DocumentCreatingEvent.class : DocumentUpdatingEvent.class);
    saveToStore(doc);
    try {
      XWikiDocument notifyDoc = doc.clone(); // avoid mutating doc in notify after save
      notifyDoc.setOriginalDocument(origDocBeforeSave);
      notifyEvent(notifyDoc, isNewDoc ? DocumentCreatedEvent.class : DocumentUpdatedEvent.class);
    } catch (Exception exc) {
      LOGGER.error("Failed to notify save event for doc [{}] with lang [{}]",
          serialize(doc.getDocumentReference()), doc.getLanguage(), exc);
    }
  }

  private void sanitizeLangBeforeSave(XWikiDocument doc) throws DocumentSaveException {
    if (doc.getDefaultLanguage().isEmpty()) {
      LOGGER.warn("sanitizeLangBeforeSave: default lang missing on doc [{}]",
          serialize(doc.getDocumentReference()));
      doc.setDefaultLanguage(context.getDefaultLanguage());
    }
    if (doc.getLanguage().equals(doc.getDefaultLanguage())) {
      LOGGER.warn("sanitizeLangBeforeSave: set lang equals default lang on doc [{}]",
          serialize(doc.getDocumentReference()));
      doc.setTranslation(0);
      doc.setLanguage(DEFAULT_LANG);
    } else if ((doc.getTranslation() == 0) && !doc.getLanguage().equals(DEFAULT_LANG)) {
      LOGGER.warn("sanitizeLangBeforeSave: lang [{}] set on main doc [{}]",
          doc.getLanguage(), serialize(doc.getDocumentReference()));
      doc.setLanguage(DEFAULT_LANG);
    } else if ((doc.getTranslation() != 0) && doc.getLanguage().equals(DEFAULT_LANG)) {
      throw new DocumentSaveException(doc.getDocumentReference(), doc.getLanguage(),
          "translation doc without set language");
    }
    if ((doc.getTranslation() != 0) && !exists(doc.getDocumentReference())) {
      throw new DocumentSaveException(doc.getDocumentReference(), doc.getLanguage(),
          "cannot save translation for inexistent doc");
    }
  }

  private void prepareDocForSave(XWikiDocument doc, String comment, boolean isMinorEdit) {
    doc.setAuthor(context.getUserName());
    if (doc.isNew()) {
      doc.setCreator(context.getUserName());
    }
    doc.setComment(Strings.nullToEmpty(comment));
    doc.setMinorEdit(isMinorEdit);
  }

  private void saveToStore(XWikiDocument doc) throws DocumentSaveException {
    try {
      contextualise(doc.getDocumentReference(), () -> getStore().saveXWikiDoc(doc, getXContext()));
    } catch (XWikiException exc) {
      throw new DocumentSaveException(doc.getDocumentReference(), exc);
    }
  }

  @Override
  public void deleteDocument(DocumentReference docRef, boolean totrash)
      throws DocumentDeleteException {
    try {
      XWikiDocument mainDoc = getDocument(docRef);
      getTranslations(docRef).values().forEach(rethrowConsumer(
          transDoc -> deleteDocumentInternal(transDoc, totrash)));
      deleteDocumentInternal(mainDoc, totrash);
    } catch (DocumentNotExistsException exc) {
      LOGGER.debug("doc trying to delete does not exist '{}'", serialize(docRef), exc);
    } catch (ModelAccessRuntimeException exc) {
      throw new DocumentDeleteException(docRef, exc);
    }
  }

  @Override
  public void deleteTranslation(DocumentReference docRef, String lang, boolean totrash)
      throws DocumentDeleteException {
    try {
      XWikiDocument doc = getDocument(docRef, lang);
      if (doc.isTrans()) {
        deleteDocumentInternal(doc, totrash);
      } else {
        throw new DocumentDeleteException(docRef);
      }
    } catch (DocumentNotExistsException exc) {
      LOGGER.debug("doc trying to delete does not exist '{}'", serialize(docRef), exc);
    } catch (ModelAccessRuntimeException exc) {
      throw new DocumentDeleteException(docRef, exc);
    }
  }

  @Deprecated
  @Override
  public void deleteDocument(XWikiDocument doc, boolean totrash) throws DocumentDeleteException {
    checkNotNull(doc);
    deleteDocument(doc.getDocumentReference(), totrash);
  }

  @Deprecated
  @Override
  public void deleteDocumentWithoutTranslations(XWikiDocument doc, boolean totrash)
      throws DocumentDeleteException {
    deleteDocumentInternal(doc, totrash);
  }

  void deleteDocumentInternal(XWikiDocument doc, boolean totrash) throws DocumentDeleteException {
    checkNotNull(doc);
    LOGGER.debug("deleteDocument: doc '{}, {}', totrash '{}'",
        serialize(doc.getDocumentReference()), doc.getLanguage(), totrash);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("deleteDocument: context db '{}' and StackTrace:",
          serialize(context.getWikiRef()), new Throwable());
    }
    notifyEvent(doc, DocumentDeletingEvent.class);
    deleteFromStore(doc, totrash);
    try {
      // to follow DocumentUpdatedEvent policy source doc must be a new empty document with the
      // old deleted version available using doc.getOriginalDocument()
      XWikiDocument notifyDoc = docCreator.create(doc.getDocumentReference(), doc.getLanguage());
      notifyDoc.setOriginalDocument(doc);
      notifyEvent(notifyDoc, DocumentDeletedEvent.class);
    } catch (Exception exc) {
      LOGGER.error("Failed to notify delete event for doc [{}] with lang [{}]",
          serialize(doc.getDocumentReference()), doc.getLanguage(), exc);
    }
  }

  private void deleteFromStore(XWikiDocument document, boolean toTrash)
      throws DocumentDeleteException {
    try {
      contextualise(document.getDocumentReference(), () -> {
        StoreFactory.getRecycleBinStore()
            .filter(store -> toTrash)
            .ifPresent(rethrowConsumer(store -> store.saveToRecycleBin(document,
                context.getUserName(), new Date(), getXContext(), true)));
        getStore().deleteXWikiDoc(document, getXContext());
      });
    } catch (XWikiException exc) {
      throw new DocumentDeleteException(document.getDocumentReference(), exc);
    }
  }

  @Deprecated
  @Override
  public List<String> getExistingLangs(DocumentReference docRef) {
    return getTranslationLangs(docRef);
  }

  @Override
  public List<String> getTranslationLangs(DocumentReference docRef) {
    if (docRef == null) {
      return new ArrayList<>();
    }
    try {
      XWikiDocument doc = docCreator.createWithoutDefaults(docRef);
      return contextualise(docRef, () -> getStore().getTranslationList(doc, getXContext()));
    } catch (XWikiException exc) {
      throw new DocumentLoadException(docRef, exc);
    }
  }

  @Override
  public Map<String, XWikiDocument> getTranslations(DocumentReference docRef) {
    Map<String, XWikiDocument> transMap = new HashMap<>();
    for (String lang : getTranslationLangs(docRef)) {
      lang = modelUtils.normalizeLang(lang);
      try {
        transMap.put(lang, getDocument(docRef, lang));
      } catch (DocumentNotExistsException exc) {
        LOGGER.error("failed to load existing translation '{}' for doc '{}'",
            lang, serialize(docRef), exc);
      }
    }
    return transMap;
  }

  @Deprecated
  @Override
  public boolean isTranslation(XWikiDocument doc) {
    return checkNotNull(doc).isTrans();
  }

  @Override
  public Stream<XWikiDocument> streamParents(XWikiDocument doc) {
    return streamParents(doc, XWikiDocument::getParentReference, this::getDocumentOpt);
  }

  @Override
  public Stream<CelDocument> streamParents(CelDocument doc) {
    return streamParents(doc, CelDocument::getParentReference, this::getCelDocument);
  }

  private <D> Stream<D> streamParents(D doc,
      Function<D, DocumentReference> parentRef,
      Function<DocumentReference, Optional<D>> loader) {
    return StreamEx.of(new Iterator<D>() {

      private D current = doc;
      private Set<DocumentReference> seen = new HashSet<>();

      @Override
      public boolean hasNext() {
        return Optional.ofNullable(current)
            .map(parentRef)
            .filter(DefaultModelAccessFacade.this::exists)
            .isPresent();
      }

      @Override
      public D next() {
        DocumentReference ref = Optional.ofNullable(current).map(parentRef).orElseThrow();
        if (seen.add(ref)) {
          return current = loader.apply(ref).orElseThrow();
        } else {
          throw new IllegalStateException("cyclic parent referencing: " + seen);
        }
      }
    });
  }

  @Override
  @Deprecated
  public BaseObject getXObject(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException {
    return Iterables.getFirst(getXObjects(getDocument(docRef, DEFAULT_LANG), classRef),
        null);
  }

  @Override
  @Deprecated
  public BaseObject getXObject(DocumentReference docRef, DocumentReference classRef, String key,
      Object value) throws DocumentNotExistsException {
    return Iterables.getFirst(getXObjects(getDocument(docRef, DEFAULT_LANG), classRef, key,
        value), null);
  }

  @Override
  @Deprecated
  public BaseObject getXObject(XWikiDocument doc, DocumentReference classRef) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).fetch().first().orNull();
  }

  @Override
  @Deprecated
  public BaseObject getXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return Iterables.getFirst(getXObjects(doc, classRef, key, value), null);
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<BaseObject> getXObject(DocumentReference docRef,
      DocumentReference classRef, int objectNumber) throws DocumentNotExistsException {
    return getXObject(getDocument(docRef, DEFAULT_LANG), classRef, objectNumber);
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<BaseObject> getXObject(XWikiDocument doc,
      DocumentReference classRef, int objectNumber) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).filter(
        objectNumber).fetch().first();
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException {
    return getXObjects(getDocument(docRef, DEFAULT_LANG), classRef);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Object value) throws DocumentNotExistsException {
    return getXObjects(getDocument(docRef, DEFAULT_LANG), classRef, key, value);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Collection<?> values) throws DocumentNotExistsException {
    return getXObjects(getDocument(docRef, DEFAULT_LANG), classRef, key, values);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).fetch().list();
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return getXObjects(doc, classRef, key, Arrays.asList(value));
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values) {
    ObjectFetcher<XWikiDocument, BaseObject> objFetcher = XWikiObjectEditor.on(doc).filter(
        new ClassReference(classRef)).fetch();
    List<BaseObject> ret = new ArrayList<>();
    for (BaseObject obj : objFetcher.list()) {
      if (checkPropertyKeyValues(obj, key, values)) {
        ret.add(obj);
      }
    }
    return ImmutableList.copyOf(ret);
  }

  @Override
  @Deprecated
  public Map<DocumentReference, List<BaseObject>> getXObjects(XWikiDocument doc) {
    Map<ClassIdentity, List<BaseObject>> map = XWikiObjectEditor.on(doc).fetch().map();
    WikiReference wikiRef = doc.getDocumentReference().getWikiReference();
    Map<DocumentReference, List<BaseObject>> ret = new HashMap<>();
    for (ClassIdentity classId : map.keySet()) {
      ret.put(classId.getDocRef(wikiRef), map.get(classId));
    }
    return ImmutableMap.copyOf(ret);
  }

  @Deprecated
  private boolean checkPropertyKeyValues(BaseObject obj, String key, Collection<?> checkValues) {
    boolean valid = (key == null);
    if (!valid && (checkValues != null)) {
      Object val = getProperty(obj, key);
      for (Object checkVal : checkValues) {
        valid |= Objects.equal(val, checkVal);
      }
    }
    return valid;
  }

  @Override
  public com.xpn.xwiki.api.Object getApiObject(BaseObject obj) throws NoAccessRightsException {
    com.xpn.xwiki.api.Object ret = null;
    if (obj != null) {
      try {
        if (rightsAccess.hasAccessLevel(obj.getDocumentReference(), EAccessLevel.VIEW)) {
          return getApiObjectWithoutRightCheck(obj);
        } else {
          throw new NoAccessRightsException(obj.getDocumentReference(), context.getUser(),
              EAccessLevel.VIEW);
        }
      } catch (IllegalStateException exp) {
        LOGGER.warn("getApiObject failed for '{}'", obj, exp);
      }
    }
    return ret;
  }

  @Override
  public com.xpn.xwiki.api.Object getApiObjectWithoutRightCheck(@Nullable BaseObject obj) {
    if (obj != null) {
      return obj.newObjectApi(obj, context.getXWikiContext());
    }
    return null;
  }

  @Override
  public List<com.xpn.xwiki.api.Object> getApiObjects(List<BaseObject> objs) {
    List<com.xpn.xwiki.api.Object> ret = new ArrayList<>();
    for (BaseObject obj : objs) {
      try {
        if (obj != null) {
          com.xpn.xwiki.api.Object apiObject = getApiObject(obj);
          if (apiObject != null) {
            ret.add(apiObject);
          }
        }
      } catch (NoAccessRightsException exp) {
        LOGGER.debug("getApiObjects ommits object '{}'", obj, exp);
      }
    }
    return ret;
  }

  @Override
  public List<com.xpn.xwiki.api.Object> getApiObjectsWithoutRightChecks(List<BaseObject> objs) {
    List<com.xpn.xwiki.api.Object> ret = new ArrayList<>();
    for (BaseObject obj : objs) {
      if (obj != null) {
        com.xpn.xwiki.api.Object apiObject = getApiObjectWithoutRightCheck(obj);
        if (apiObject != null) {
          ret.add(apiObject);
        }
      }
    }
    return ret;
  }

  @Override
  @Deprecated
  public BaseObject newXObject(XWikiDocument doc, DocumentReference docClassRef) {
    ClassReference classRef = new ClassReference(docClassRef);
    return XWikiObjectEditor.on(doc).filter(classRef).create().get(classRef);
  }

  @Override
  @Deprecated
  public BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef) {
    return getOrCreateXObject(doc, classRef, null, null);
  }

  @Override
  @Deprecated
  public BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    BaseObject obj = getXObject(doc, classRef, key, value);
    if (obj == null) {
      obj = newXObject(doc, classRef);
      if (key != null) {
        setProperty(obj, key, value);
      }
    }
    return obj;
  }

  @Override
  @Deprecated
  public boolean removeXObject(XWikiDocument doc, BaseObject objToRemove) {
    return XWikiObjectEditor.on(doc).filter(objToRemove).deleteFirst().isPresent();
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, List<BaseObject> objsToRemove) {
    checkNotNull(doc);
    boolean changed = false;
    for (BaseObject obj : new ArrayList<>(objsToRemove)) {
      if (obj != null) {
        changed |= XWikiObjectEditor.on(doc).filter(obj).deleteFirst().isPresent();
      }
    }
    return changed;
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef) {
    return !XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).delete().isEmpty();
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return removeXObjects(doc, classRef, key, Arrays.asList(value));
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values) {
    return removeXObjects(doc, getXObjects(doc, classRef, key, values));
  }

  @Override
  @Deprecated
  public Object getProperty(DocumentReference docRef, DocumentReference classRef, String name)
      throws DocumentNotExistsException {
    return getProperty(getDocument(docRef, DEFAULT_LANG), classRef, name);
  }

  @Override
  @Deprecated
  public Object getProperty(XWikiDocument doc, DocumentReference classRef, String name) {
    return getProperty(getXObject(doc, classRef), name);
  }

  @Override
  @Deprecated
  public Object getProperty(BaseObject obj, String name) {
    return xObjStrFieldAccessor.get(obj, name).orElse(null);
  }

  @Override
  @Deprecated
  public <T> com.google.common.base.Optional<T> getFieldValue(BaseObject obj, ClassField<T> field) {
    return xObjFieldAccessor.getValue(obj, field);
  }

  @Override
  @Deprecated
  public <T> com.google.common.base.Optional<T> getFieldValue(XWikiDocument doc,
      ClassField<T> field) {
    return com.google.common.base.Optional.fromJavaUtil(XWikiObjectFetcher.on(doc)
        .fetchField(field).stream().findFirst());
  }

  @Override
  @Deprecated
  public <T> com.google.common.base.Optional<T> getFieldValue(DocumentReference docRef,
      ClassField<T> field) throws DocumentNotExistsException {
    return getFieldValue(getDocument(docRef, DEFAULT_LANG), field);
  }

  @Override
  @Deprecated
  public <T> com.google.common.base.Optional<T> getFieldValue(XWikiDocument doc,
      ClassField<T> field, T ignoreValue) {
    checkNotNull(ignoreValue);
    com.google.common.base.Optional<T> property = getFieldValue(doc, field);
    if (property.isPresent() && Objects.equal(property.get(), ignoreValue)) {
      property = com.google.common.base.Optional.absent();
    }
    return property;
  }

  @Override
  @Deprecated
  public <T> com.google.common.base.Optional<T> getFieldValue(DocumentReference docRef,
      ClassField<T> field, T ignoreValue) throws DocumentNotExistsException {
    checkNotNull(ignoreValue);
    com.google.common.base.Optional<T> property = getFieldValue(docRef, field);
    if (property.isPresent() && Objects.equal(property.get(), ignoreValue)) {
      property = com.google.common.base.Optional.absent();
    }
    return property;
  }

  @Override
  @Deprecated
  public <T> T getProperty(DocumentReference docRef, ClassField<T> field)
      throws DocumentNotExistsException {
    return getFieldValue(docRef, field).toJavaUtil()
        .orElseGet(() -> defaultValueNonNullable(field.getType()));
  }

  @Override
  @Deprecated
  public <T> T getProperty(XWikiDocument doc, ClassField<T> field) {
    return getFieldValue(doc, field).toJavaUtil()
        .orElseGet(() -> defaultValueNonNullable(field.getType()));
  }

  @Override
  @Deprecated
  public List<ClassFieldValue<?>> getProperties(XWikiDocument doc, ClassDefinition classDef) {
    List<ClassFieldValue<?>> ret = new ArrayList<>();
    for (ClassField<?> field : classDef.getFields()) {
      ret.add(new ClassFieldValue<>(castField(field), getProperty(doc, field)));
    }
    return ret;
  }

  // unchecked suppression is ok because every wildcard extends Object
  @SuppressWarnings("unchecked")
  private ClassField<Object> castField(ClassField<?> field) {
    return (ClassField<Object>) field;
  }

  @Override
  @Deprecated
  public boolean setProperty(BaseObject obj, String name, Object value) {
    return xObjStrFieldAccessor.set(obj, name, value);
  }

  @Override
  @Deprecated
  public <T> XWikiDocument setProperty(DocumentReference docRef, ClassField<T> field, T value)
      throws DocumentNotExistsException {
    XWikiDocument doc = getDocument(docRef);
    setProperty(doc, field, value);
    return doc;
  }

  @Override
  @Deprecated
  public <T> boolean setProperty(XWikiDocument doc, ClassField<T> field, T value) {
    DocumentReference classDocRef = field.getClassReference().getDocRef();
    return setProperty(getOrCreateXObject(doc, classDocRef), field, value);
  }

  @Override
  @Deprecated
  public <T> boolean setProperty(XWikiDocument doc, ClassFieldValue<T> fieldValue) {
    return setProperty(doc, fieldValue.getField(), fieldValue.getValue());
  }

  @Override
  @Deprecated
  public <T> boolean setProperty(BaseObject obj, ClassField<T> field, T value) {
    return xObjFieldAccessor.set(obj, field, value);
  }

  @Override
  @Deprecated
  public XWikiAttachment getAttachmentNameEqual(XWikiDocument doc, String filename)
      throws AttachmentNotExistsException {
    return Utils.getComponent(IAttachmentServiceRole.class).getAttachmentNameEqual(doc, filename);
  }

  private void notifyEvent(XWikiDocument doc, Class<? extends AbstractDocumentEvent> eventType) {
    try {
      AbstractDocumentEvent event = eventType
          .getConstructor(DocumentReference.class)
          .newInstance(doc.getDocumentReference());
      LOGGER.trace("notify event [{}] for doc [{}] with lang [{}]", eventType.getSimpleName(),
          serialize(doc.getDocumentReference()), doc.getLanguage());
      contextualise(doc.getDocumentReference(), () -> getObservationManager()
          .notify(event, doc, context.getXWikiContext()));
    } catch (ReflectiveOperationException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  /**
   * beware of cyclic dependencies
   */
  private ObservationManager getObservationManager() {
    return Utils.getComponent(ObservationManager.class);
  }

  private XWikiStoreInterface getStore() {
    return tryCast(mainStore.get(), ModelAccessStore.class)
        .map(ModelAccessStore::getBackingStore)
        .orElse(mainStore.get());
  }

  private XWikiContext getXContext() {
    return context.getXWikiContext();
  }

  private <E extends Exception> void contextualise(DocumentReference docRef,
      ThrowingRunnable<E> runnable) throws E {
    contextualise(docRef, () -> {
      runnable.run();
      return null;
    });
  }

  private <T, E extends Exception> T contextualise(DocumentReference docRef,
      ThrowingSupplier<T, E> supplier) throws E {
    var wiki = docRef.getWikiReference();
    return Objects.equal(wiki, context.getWikiRef())
        ? supplier.get()
        : new Contextualiser()
            .withWiki(wiki)
            .execute(rethrow(supplier));
  }

  private Supplier<String> serialize(EntityReference ref) {
    return defer(() -> modelUtils.serializeRef(ref, ReferenceSerializationMode.GLOBAL));
  }

}
