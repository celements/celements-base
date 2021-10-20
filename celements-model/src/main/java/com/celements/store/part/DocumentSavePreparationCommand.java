package com.celements.store.part;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.UUID;

import javax.annotation.concurrent.Immutable;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.XWikiDocumentCreator;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

import one.util.streamex.StreamEx;

@Immutable
class DocumentSavePreparationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final XWikiDocument doc;
  private final CelHibernateStore store;
  private final XWikiContext context;

  private Session session;

  DocumentSavePreparationCommand(XWikiDocument doc, CelHibernateStore store, XWikiContext context) {
    this.doc = checkNotNull(doc);
    this.store = checkNotNull(store);
    this.context = checkNotNull(context);
  }

  public String getDatabase() {
    return context.getDatabase();
  }

  public Session getSession() {
    checkState(session != null, "execute command first");
    return session;
  }

  public DocumentSavePreparationCommand execute(boolean bTransaction)
      throws HibernateException, XWikiException {
    doc.setStore(store);
    ensureDatabaseConsistency();
    prepareSession(bTransaction);
    try {
      prepareDocId();
      if (doc.getTranslation() == 0) {
        prepareMainDoc();
      } else {
        doc.setElement(XWikiDocument.HAS_OBJECTS, false);
        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, false);
      }
    } catch (IdComputationException exc) {
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
          "saveXWikiDoc - [" + getDatabase() + "] failed to compute id for [" + doc + "]", exc);
    }
    return this;
  }

  private void prepareSession(boolean bTransaction) throws XWikiException {
    checkState(session == null, "command already executed");
    if (bTransaction) {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      store.beginTransaction(sfactory, context);
    }
    session = store.getSession(context);
    session.setFlushMode(FlushMode.COMMIT);
  }

  private void ensureDatabaseConsistency() {
    if (!doc.getDocumentReference().getWikiReference().getName().equals(getDatabase())) {
      LOGGER.warn("saveXWikiDoc - [{}] not matching database, adjusting from doc [{}]",
          getDatabase(), doc.getDocumentReference(), new Throwable());
      boolean isMetaDataDirty = doc.isMetaDataDirty();
      doc.setDocumentReference(RefBuilder.from(doc.getDocumentReference()).wiki(getDatabase())
          .build(DocumentReference.class));
      doc.setMetaDataDirty(isMetaDataDirty);
    }
  }

  private void prepareDocId() throws HibernateException, XWikiException {
    // documents with valid id have been loaded from the store, thus no id computation needed
    if (!doc.hasValidId()) {
      long nextFreeDocId = findNextFreeDocId();
      LOGGER.debug("saveXWikiDoc - [{}] computed doc id [{}] for [{}]",
          getDatabase(), nextFreeDocId, doc.getDocumentReference());
      doc.setId(nextFreeDocId, store.getIdComputer().getIdVersion());
      if (!doc.isNew()) {
        doc.setNew(true);
        LOGGER.warn("saveXWikiDoc - [{}] document without valid id wasn't set to new [{}]",
            getDatabase(), doc.getDocumentReference(), new Throwable());
      }
    }
  }

  private long findNextFreeDocId() throws HibernateException, XWikiException {
    String docKey = store.getDocKey(doc.getDocumentReference(), doc.getLanguage());
    SortedMap<Long, String> existingDocKeys = store.loadExistingDocKeys(getSession(),
        doc.getDocumentReference(), doc.getLanguage());
    checkForCollisions(docKey, existingDocKeys);
    return StreamEx.of(store.getIdComputer()
        .getDocumentIdIterator(doc.getDocumentReference(), doc.getLanguage()))
        .filter(not(existingDocKeys::containsKey))
        .findFirst()
        .orElseThrow(() -> new XWikiException(MODULE_XWIKI_STORE,
            ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "saveXWikiDoc - [" + getDatabase()
                + "] failed, collision count exhausted [" + docKey + "]"));
  }

  private void checkForCollisions(String docKey, SortedMap<Long, String> existingDocKeys)
      throws XWikiException {
    Iterator<Entry<Long, String>> iter = existingDocKeys.entrySet().iterator();
    for (int collisionCount = 0; iter.hasNext(); collisionCount++) {
      Entry<Long, String> entry = iter.next();
      Long docId = entry.getKey();
      String existingDocKey = entry.getValue();
      if (docKey.equals(existingDocKey)) {
        // should not happen ;) findNextFreeDocId should never be called for existing documents
        // but let's check it here anyway and fail if it happens
        throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
            "saveXWikiDoc - [" + getDatabase() + "] failed, unable to compute next free id "
                + "for an existing document [" + docKey + "]");
      } else {
        LOGGER.warn("saveXWikiDoc - [{}] collision detected: id [{}], doc [{}], existing doc [{}], "
            + "count [{}]", getDatabase(), docId, docKey, existingDocKey, collisionCount);
      }
    }
  }

  private void prepareMainDoc() throws IdComputationException, XWikiException {
    updateBaseClassXml(doc, context);
    doc.setElement(XWikiDocument.HAS_OBJECTS, XWikiObjectFetcher.on(doc).exists());
    doc.setElement(XWikiDocument.HAS_ATTACHMENTS, !doc.getAttachmentList().isEmpty());
    new XObjectPreparer(doc, context).execute();
  }

  private void updateBaseClassXml(XWikiDocument doc, XWikiContext context) {
    BaseClass bclass = doc.getXClass();
    if (bclass != null) {
      bclass.setDocumentReference(doc.getDocumentReference());
      if (!bclass.getFieldList().isEmpty()) {
        doc.setXClassXML(bclass.toXMLString());
      } else {
        doc.setXClassXML("");
      }
      // Store this XWikiClass in the context in case of recursive usage of classes
      context.addBaseClass(bclass);
    }
  }

  private class XObjectPreparer {

    private final XWikiDocument doc;
    private final XWikiDocument origDoc;

    XObjectPreparer(XWikiDocument doc, XWikiContext context) throws XWikiException {
      this.doc = doc;
      this.origDoc = loadOriginalDocument(context);
    }

    private XWikiDocument loadOriginalDocument(XWikiContext context) throws XWikiException {
      XWikiDocument dummyDoc = getDocCreator().createWithoutDefaults(doc.getDocumentReference());
      // XXX do not check doc.isNew() here, it is not reliably set. see [CELDEV-701]
      if (doc.hasElement(XWikiDocument.HAS_OBJECTS) && getPrimaryStore(context).exists(dummyDoc,
          context)) {
        return getPrimaryStore(context).loadXWikiDoc(dummyDoc, context);
      }
      return dummyDoc;
    }

    void execute() throws IdComputationException {
      for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
        obj.setDocumentReference(doc.getDocumentReference());
        if (Strings.isNullOrEmpty(obj.getGuid())) {
          obj.setGuid(UUID.randomUUID().toString());
        }
        if (!obj.hasValidId()) {
          Optional<BaseObject> existingObj = XWikiObjectFetcher.on(origDoc)
              .filter(new ClassReference(obj.getXClassReference()))
              .filter(obj.getNumber())
              .stream().findFirst();
          if (existingObj.isPresent() && existingObj.get().hasValidId()) {
            obj.setId(existingObj.get().getId(), existingObj.get().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] already existed, keeping id", obj);
          } else {
            long nextId = store.getIdComputer().computeNextObjectId(doc);
            obj.setId(nextId, store.getIdComputer().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] is new, computed new id", obj);
            existingObj.ifPresent(this::logExistingObject);
          }
        }
      }
    }

    private void logExistingObject(BaseObject existingObj) {
      // observed in com.xpn.xwiki.web.ObjectAddAction, see [CELDEV-693]
      LOGGER.warn("saveXWikiDoc - overwriting existing object [{}] because of invalid id, "
          + "possibly due to cache poisoning before save through 'XWiki#getDocument': {}",
          existingObj, existingObj.toXMLString(), new Throwable());
    }
  }

  private XWikiDocumentCreator getDocCreator() {
    return Utils.getComponent(XWikiDocumentCreator.class);
  }

  /**
   * @return the cache store if one is configured, else it's self referencing
   */
  private XWikiStoreInterface getPrimaryStore(XWikiContext context) {
    return context.getWiki().getStore();
  }

}
