package com.celements.model.migration;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingSupplier;
import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.celements.store.id.IdVersion;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(BaseObjectIdMigration.NAME)
public class BaseObjectIdMigration extends AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      BaseObjectIdMigration.class);

  public static final String NAME = "BaseObjectIdMigration";

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Requirement
  private CelementsIdComputer idComputer;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "migrates ids of BaseObjects from " + IdVersion.XWIKI_2.name() + " to "
        + idComputer.getIdVersion().name();
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * 30.1.2021 -> 4351 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(4351);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    LOGGER.info("[{}] migrating object ids", context.getDatabase());
    try {
      migrateAllObjectIds(context);
    } catch (Exception exc) {
      LOGGER.error("[{}] failed to migrate object ids", context.getDatabase(), exc);
      throw exc;
    }
  }

  private void migrateAllObjectIds(XWikiContext context) throws XWikiException {
    AtomicLong count = new AtomicLong(0);
    AtomicReference<XWikiDocument> previousDoc = new AtomicReference<>();
    try {
      queryExecutor.executeReadSql(String.class, getSelectAllObjsWithOldIdSql(), row -> {
        BaseObject object = createXObjectFromRow(row, context);
        if (object != null) {
          XWikiDocument doc = getAndLockNextDoc(object.getDocumentReference(), previousDoc.get());
          if (doc != null) {
            previousDoc.set(doc);
            boolean commited = inTransaction(() -> migrateObjectId(doc, object, context), context);
            if (commited && ((count.incrementAndGet() % 10000) == 0)) {
              LOGGER.info("[{}] migrated {}", context.getDatabase(), count.get());
            }
          }
        }
      });
    } finally {
      releaseLock(previousDoc.get()); // release the last doc
    }
    LOGGER.info("[{}] migrated {} object ids", context.getDatabase(), count);
  }

  private XWikiDocument getAndLockNextDoc(DocumentReference docRef, XWikiDocument previousDoc) {
    XWikiDocument nextDoc = null;
    if ((previousDoc != null) && docRef.equals(previousDoc.getDocumentReference())) {
      nextDoc = previousDoc;
    } else {
      try {
        releaseLock(previousDoc);
      } catch (XWikiException xwe) {
        LOGGER.info("failed to release lock for [{}]", previousDoc.getDocumentReference());
      }
      try {
        nextDoc = acquireLock(modelAccess.getDocument(docRef));
      } catch (DocumentNotExistsException dne) {
        LOGGER.warn("doc for object doesn't exist [{}]", docRef, dne);
      } catch (XWikiException xwe) {
        LOGGER.warn("failed acquiring lock for [{}]", docRef);
      }
    }
    return nextDoc;

  }

  private boolean migrateObjectId(XWikiDocument doc, BaseObject object, XWikiContext context)
      throws XWikiException {
    try {
      long newId = idComputer.computeNextObjectId(doc);
      if ((object.getId() != newId) || (idComputer.getIdVersion() != object.getIdVersion())) {
        getHibStore().loadXWikiCollection(object, doc, context, false, false);
        getHibStore().deleteXWikiObject(object, context, false);
        object.setId(newId, idComputer.getIdVersion());
        getHibStore().saveXWikiCollection(object, context, false);
        LOGGER.debug("migrated [{}]", object);
        return true;
      }
      return false;
    } catch (IdComputationException exc) {
      throw new XWikiException(0, 0, exc.getMessage(), exc);
    }
  }

  private boolean inTransaction(ThrowingSupplier<Boolean, XWikiException> action,
      XWikiContext context) {
    boolean commit = false;
    try {
      getHibStore().beginTransaction(context);
      commit = action.get();
    } catch (XWikiException xwe) {
      LOGGER.error("action failed", xwe);
    } finally {
      getHibStore().endTransaction(context, commit);
    }
    return commit;
  }

  private XWikiDocument acquireLock(XWikiDocument doc) throws XWikiException {
    XWikiLock lock = doc.getLock(context.getXWikiContext());
    if ((lock == null) || NAME.equals(lock.getUserName())) {
      doc.setLock(NAME, context.getXWikiContext());
      return doc;
    }
    throw new XWikiException(0, 0, "lock already acquired by " + lock.getUserName());
  }

  private void releaseLock(XWikiDocument doc) throws XWikiException {
    if (doc != null) {
      XWikiLock lock = doc.getLock(context.getXWikiContext());
      if ((lock != null) && NAME.equals(lock.getUserName())) {
        doc.removeLock(context.getXWikiContext());
      } else {
        throw new XWikiException(0, 0, "lock not held by migration, instead held by: "
            + ((lock != null) ? lock.getUserName() : "noone"));
      }
    }
  }

  static String getSelectAllObjsWithOldIdSql() {
    return "select XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER from xwikiobjects"
        + " where XWO_ID_VERSION = " + IdVersion.XWIKI_2.ordinal()
        + " order by XWO_NAME, XWO_CLASSNAME, XWO_NUMBER";
  }

  /**
   *
   * @param row
   *          [XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER]
   */
  private BaseObject createXObjectFromRow(List<String> row, XWikiContext context) {
    try {
      long id = Longs.tryParse(row.get(0));
      DocumentReference docRef = modelUtils.resolveRef(row.get(1), DocumentReference.class);
      DocumentReference xClassRef = modelUtils.resolveRef(row.get(2), DocumentReference.class);
      int nb = Ints.tryParse(row.get(3));
      BaseObject object = BaseClass.newCustomClassInstance(xClassRef, context);
      object.setXClassReference(xClassRef);
      object.setDocumentReference(docRef);
      object.setNumber(nb);
      object.setId(id, IdVersion.XWIKI_2);
      return object;
    } catch (IllegalArgumentException | XWikiException exc) {
      LOGGER.warn("unable to parse illegal row [{}]", row, exc);
      return null;
    }
  }

  private XWikiHibernateStore getHibStore() {
    return context.getXWikiContext().getWiki().getHibernateStore();
  }

}
