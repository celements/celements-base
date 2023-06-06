package com.celements.store.part;

import static com.celements.store.part.CelHibernateStoreDocumentPart.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.concurrent.NotThreadSafe;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.XWikiDocumentCreator;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

import one.util.streamex.StreamEx;

@NotThreadSafe
class DocumentSavePreparationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final XWikiDocument doc;
  private final CelHibernateStore store;
  private final XWikiContext context;
  private final String docKey;

  private Session session;

  DocumentSavePreparationCommand(XWikiDocument doc, CelHibernateStore store, XWikiContext context) {
    this.doc = checkNotNull(doc);
    this.store = checkNotNull(store);
    this.context = checkNotNull(context);
    this.docKey = store.getDocKey(doc.getDocumentReference(), doc.getLanguage());
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
          "saveXWikiDoc - [" + getDatabase() + "] failed to compute id for [" + docKey + "]",
          exc);
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
  }

  private void ensureDatabaseConsistency() {
    if (!doc.getDocumentReference().getWikiReference().getName().equals(getDatabase())) {
      LOGGER.warn("saveXWikiDoc - [{}] not matching database, adjusting from doc [{}]",
          getDatabase(), docKey, new Throwable());
      boolean isMetaDataDirty = doc.isMetaDataDirty();
      doc.setDocumentReference(RefBuilder.from(doc.getDocumentReference()).wiki(getDatabase())
          .build(DocumentReference.class));
      doc.setMetaDataDirty(isMetaDataDirty);
    }
  }

  private void prepareDocId() throws HibernateException, IdComputationException {
    // documents with valid id have been loaded from the store, thus no id computation needed
    if (!doc.hasValidId()) {
      BiMap<String, Long> existingDocKeys = store.loadExistingDocKeys(getSession(),
          doc.getDocumentReference(), doc.getLanguage()).inverse();
      long docId = findDocId(existingDocKeys);
      LOGGER.debug("saveXWikiDoc - [{}] set doc id [{}] for [{}]", getDatabase(), docId, docKey);
      doc.setId(docId, store.getIdComputer().getIdVersion());
      if (doc.isNew() ^ !existingDocKeys.containsKey(docKey)) {
        doc.setNew(!doc.isNew());
        LOGGER.warn("saveXWikiDoc - [{}] corrected new flag to [{}] for document [{}]",
            getDatabase(), doc.isNew(), docKey, new Throwable());
      }
    }
  }

  private long findDocId(BiMap<String, Long> existingDocKeys) throws IdComputationException {
    if (existingDocKeys.containsKey(docKey)) {
      return existingDocKeys.get(docKey);
    } else if (!existingDocKeys.isEmpty()) {
      LOGGER.warn("saveXWikiDoc - [{}] collision detected: doc [{}] with {} existing: [{}]",
          getDatabase(), docKey, existingDocKeys.size(), existingDocKeys);
    }
    return StreamEx.of(store.getIdComputer()
        .getDocumentIdIterator(doc.getDocumentReference(), doc.getLanguage(),
            getStartCollisionCount()))
        .filter(not(existingDocKeys.values()::contains))
        .findFirst()
        .orElseThrow(() -> new IdComputationException("collision count exhausted"));
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

  // we'll return 1 until [CELDEV-605] "XWikiDocument/BaseCollection id migration" is done
  // we want to eliminate potential collisions with already existing XWO_IDs having count 0
  private byte getStartCollisionCount() {
    return (byte) (int) getCfgSrc().getProperty("celements.store.startCollisionCount",
        (int) START_COLLISION_COUNT_DEFAULT);
  }

  private ConfigurationSource getCfgSrc() {
    return Utils.getComponent(ConfigurationSource.class);
  }

}
