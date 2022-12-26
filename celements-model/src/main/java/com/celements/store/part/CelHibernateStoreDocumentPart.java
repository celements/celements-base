package com.celements.store.part;

import static com.celements.logging.LogUtils.*;
import static com.celements.model.util.EntityTypeUtil.*;
import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.reference.RefBuilder;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.IdVersion;
import com.celements.web.classes.oldcore.XWikiGroupsClass;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

import one.util.streamex.EntryStream;

//TODO CELDEV-626 - CelHibernateStore refactoring
public class CelHibernateStoreDocumentPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  public static final byte START_COLLISION_COUNT_DEFAULT = 1;

  private final CelHibernateStore store;

  public CelHibernateStoreDocumentPart(CelHibernateStore store) {
    this.store = checkNotNull(store);
  }

  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException, HibernateException {
    validateDocNaming(doc.getDocumentReference());
    validateDatabase(doc.getDocumentReference(), context);
    boolean commit = false;
    try {
      DocumentSavePreparationCommand savePrepCmd = new DocumentSavePreparationCommand(
          doc, store, context);
      savePrepCmd.execute(bTransaction);

      if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
        store.saveAttachmentList(doc, context, false);
      }

      // Handle the latest text file
      if (doc.isContentDirty() || doc.isMetaDataDirty()) {
        Date ndate = new Date();
        doc.setDate(ndate);
        if (doc.isContentDirty()) {
          doc.setContentUpdateDate(ndate);
          doc.setContentAuthor(doc.getAuthor());
        }
        doc.incrementVersion();
        if (context.getWiki().hasVersioning(context)) {
          context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
        }
        doc.setContentDirty(false);
        doc.setMetaDataDirty(false);
      } else {
        if (doc.getDocumentArchive() != null) {
          // Let's make sure we save the archive if we have one
          // This is especially needed if we load a document from XML
          if (context.getWiki().hasVersioning(context)) {
            context.getWiki().getVersioningStore().saveXWikiDocArchive(doc.getDocumentArchive(),
                false, context);
          }
        } else {
          // Make sure the getArchive call has been made once
          // with a valid context
          try {
            if (context.getWiki().hasVersioning(context)) {
              doc.getDocumentArchive(context);
            }
          } catch (XWikiException xwe) {
            LOGGER.debug("saveXWikiDoc - this is a non critical error: {} {}",
                doc.getId(), defer(() -> store.serialize(doc, GLOBAL)), xwe);
          }
        }
      }

      /*
       * FIXME [CELDEV-248] ModelLockService
       * the computed id needs to be locked until the save has happened to prevent overrides.
       */
      Session session = savePrepCmd.getSession();
      if (doc.isNew()) {
        session.save(doc);
      } else {
        session.update(doc);
      }
      if (doc.getTranslation() == 0) {
        deleteAndSaveXObjects(doc, context);
      }
      if (context.getWiki().hasBacklinks(context)) {
        store.saveLinks(doc, context, true);
      }
      commit = true;
      doc.setNew(false);
      // We need to ensure that the saved document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

  private void deleteAndSaveXObjects(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    if ((doc.getXObjectsToRemove() != null) && (doc.getXObjectsToRemove().size() > 0)) {
      for (BaseObject removedObject : doc.getXObjectsToRemove()) {
        store.deleteXWikiObject(removedObject, context, false);
      }
      doc.setXObjectsToRemove(new ArrayList<BaseObject>());
    }
    if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
      for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
        store.saveXWikiCollection(obj, context, false);
      }
    }
  }

  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context)
      throws XWikiException, HibernateException {
    DocumentReference docRefToLoad = RefBuilder.from(doc.getDocumentReference())
        .build(DocumentReference.class);
    validateDatabase(docRefToLoad, context);
    boolean bTransaction = true;
    try {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && store.beginTransaction(sfactory, false, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.MANUAL);

      long docId = determineDocId(session, docRefToLoad, doc.getLanguage());
      if (docId == 0) {
        LOGGER.info("loadXWikiDoc - no existing doc for [{}:{}]",
            defer(() -> store.serialize(docRefToLoad, GLOBAL)), doc.getLanguage());
      }
      session.load(doc, docId);
      validateLoadedDoc(doc, docRefToLoad);
      sanitizeDoc(doc);

      // Loading the attachment list
      if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
        store.loadAttachmentList(doc, context, false);
      }

      // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
      BaseClass bclass = new BaseClass();
      String cxml = doc.getXClassXML();
      if (cxml != null) {
        bclass.fromXML(cxml);
        bclass.setDocumentReference(docRefToLoad);
        doc.setXClass(bclass);
      }
      // Store this XWikiClass in the context so that we can use it in case of recursive usage
      // of classes
      context.addBaseClass(bclass);

      if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
        Iterator<BaseObject> objIter = loadXObjects(doc, context);
        Map<Integer, BaseObject> groupObjs = new HashMap<>();
        while (objIter.hasNext()) {
          BaseObject loadedObject = objIter.next();
          if (!loadedObject.getDocumentReference().equals(docRefToLoad)) {
            LOGGER.warn("loadXWikiDoc - skipping obj [{}], doc [{}] not matching",
                loadedObject, defer(() -> store.serialize(doc, GLOBAL)));
            continue;
          }
          BaseObject object = copyToNewXObject(doc, loadedObject, context);
          if (isGroupsObject(object)) {
            // Groups objects are handled differently.
            groupObjs.put(object.getNumber(), object);
          } else {
            store.loadXWikiCollection(object, doc, context, false, true);
          }
          doc.setXObject(object.getNumber(), object);
        }
        if (groupObjs.size() > 0) {
          loadFieldsForGroupObjects(doc, groupObjs, context);
        }
      }

      doc.setContentDirty(false);
      // FIXME can be set to 'false' after CELDEV-784, see CELDEV-785
      doc.setMetaDataDirty(true);
      // We need to ensure that the loaded document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } catch (ObjectNotFoundException e) { // document doesn't exist
      doc.setNew(true);
    } finally {
      if (bTransaction) {
        store.endTransaction(context, false);
      }
    }
    return doc;
  }

  /**
   * @return the doc id for fullName and language if one exists. this is needed because we don't
   *         know before loading if a collision has occurred or which {@link IdVersion} was used on
   *         save. returns 0 if no id exists.
   */
  long determineDocId(Session session, DocumentReference docRef, String lang) {
    return EntryStream.of(store.loadExistingDocKeys(session, docRef, lang))
        .filterValues(store.getDocKey(docRef, lang)::equals)
        .keys().findFirst()
        .orElse(0L);
  }

  private void validateLoadedDoc(XWikiDocument doc, DocumentReference expectedDocRef)
      throws XWikiException {
    if (!doc.getDocumentReference().equals(expectedDocRef)) {
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
          "loadXWikiDoc - collision detected: loading doc ["
              + store.serialize(expectedDocRef, GLOBAL) + "] returned doc ["
              + store.serialize(doc, GLOBAL) + "]");
    }
  }

  private void sanitizeDoc(XWikiDocument doc) {
    // convert java.sql.Timestamp to java.util.Date
    doc.setDate(new Date(doc.getDate().getTime()));
    doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
    doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));
    doc.setNew(false);
    doc.setMostRecent(true);
  }

  @SuppressWarnings("unchecked")
  private Iterator<BaseObject> loadXObjects(XWikiDocument doc, XWikiContext context) {
    String hql = "from BaseObject as obj where obj.name = :name order by obj.className, obj.number";
    Query query = store.getSession(context).createQuery(hql);
    query.setText("name", store.serialize(doc, LOCAL));
    return query.iterate();
  }

  private BaseObject copyToNewXObject(XWikiDocument doc, BaseObject loadedObject,
      XWikiContext context) throws XWikiException {
    BaseObject object;
    if (loadedObject.getXClassReference().equals(doc.getDocumentReference())) {
      object = doc.getXClass().newCustomClassInstance(context);
    } else {
      object = BaseClass.newCustomClassInstance(loadedObject.getXClassReference(), context);
    }
    object.setXClassReference(loadedObject.getXClassReference());
    object.setDocumentReference(doc.getDocumentReference());
    object.setNumber(loadedObject.getNumber());
    object.setGuid(loadedObject.getGuid());
    object.setId(loadedObject.getId(), loadedObject.getIdVersion());
    return object;
  }

  private boolean isGroupsObject(BaseObject object) {
    ClassReference classRef = new ClassReference(object.getXClassReference());
    return classRef.equals(getXWikiGroupsClass().getClassReference());
  }

  // AFAICT this was added as an emergency patch because loading of objects has proven too slow and
  // the objects which cause the most overhead are the XWikiGroups objects as each group object
  // (each group member) would otherwise cost 2 database queries. This will do every group member in
  // a single query.
  private void loadFieldsForGroupObjects(XWikiDocument doc, Map<Integer, BaseObject> groupObjs,
      XWikiContext context) {
    String hql = "select obj.number, prop.value from StringProperty as prop, BaseObject as obj "
        + "where obj.name = :name and obj.className = 'XWiki.XWikiGroups' and obj.id = prop.id.id "
        + "and prop.id.name = 'member'";
    Query query = store.getSession(context).createQuery(hql);
    query.setText("name", store.serialize(doc, LOCAL));
    Iterator<?> dataIter = query.iterate();
    while (dataIter.hasNext()) {
      Object[] row = (Object[]) dataIter.next();
      groupObjs.get(row[0]).setStringValue("member", (String) row[1]);
    }
  }

  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context)
      throws XWikiException, HibernateException {
    validateDatabase(doc.getDocumentReference(), context);
    boolean bTransaction = false;
    boolean commit = false;
    try {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = store.beginTransaction(sfactory, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.COMMIT);

      if (doc.isNew()) {
        throw new XWikiException(MODULE_XWIKI_STORE,
            ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
            "Impossible to delete document if it is not loaded: " + doc.getDocumentReference());
      }

      // Let's delete any attachment this document might have
      for (XWikiAttachment attachment : doc.getAttachmentList()) {
        context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, false, context,
            false);
      }

      // deleting XWikiLinks
      if (context.getWiki().hasBacklinks(context)) {
        store.deleteLinks(doc.getId(), context, true);
      }

      if (doc.getTranslation() == 0) {
        for (BaseObject object : XWikiObjectEditor.on(doc).fetch().iter().append(firstNonNull(
            doc.getXObjectsToRemove(), Collections.<BaseObject>emptyList()))) {
          store.deleteXWikiObject(object, context, false);
        }
      }

      context.getWiki().getVersioningStore().deleteArchive(doc, false, context);
      session.delete(doc);
      commit = true;
      // We need to ensure that the deleted document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

  private ClassDefinition getXWikiGroupsClass() {
    return Utils.getComponent(ClassDefinition.class, XWikiGroupsClass.CLASS_DEF_HINT);
  }

  private void validateDocNaming(DocumentReference docRef) {
    checkArgument(isMatchingEntityType(store.serialize(docRef, LOCAL), EntityType.DOCUMENT),
        "illegal doc naming [%s]", docRef);
  }

  private void validateDatabase(DocumentReference docRef, XWikiContext context) {
    WikiReference docWiki = docRef.getWikiReference();
    WikiReference providedContextWiki = new WikiReference(context.getDatabase());
    WikiReference executionContextWiki = store.getModelContext().getWikiRef();
    checkArgument(docWiki.equals(providedContextWiki) && docWiki.equals(executionContextWiki),
        "wikis not matching for doc [%s], providedContextWiki [%s], executionContextWiki [%s]",
        docRef, providedContextWiki, executionContextWiki);
  }

}
