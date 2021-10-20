package com.celements.store;

import static com.xpn.xwiki.XWikiException.*;

import java.text.MessageFormat;
import java.time.Duration;

import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.logging.LogLevel;
import com.celements.logging.LogUtils;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.UniqueHashIdComputer;
import com.celements.store.part.CelHibernateStoreCollectionPart;
import com.celements.store.part.CelHibernateStoreDocumentPart;
import com.celements.store.part.CelHibernateStorePropertyPart;
import com.google.common.base.Stopwatch;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateStore;

@Singleton
@Component
public class CelHibernateStore extends XWikiHibernateStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  @Requirement(UniqueHashIdComputer.NAME)
  private CelementsIdComputer idComputer;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext modelContext;

  private final CelHibernateStoreDocumentPart documentStorePart;
  private final CelHibernateStoreCollectionPart collectionStorePart;
  private final CelHibernateStorePropertyPart propertyStorePart;

  public CelHibernateStore() {
    super();
    documentStorePart = new CelHibernateStoreDocumentPart(this);
    collectionStorePart = new CelHibernateStoreCollectionPart(this);
    propertyStorePart = new CelHibernateStorePropertyPart(this);
  }

  @Override
  public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.INFO, "exists", doc);
      // FIXME [CELDEV-924] Store add lang support for exists check and cache
      boolean ret = super.exists(doc, context);
      logEnd(LogLevel.INFO, "exists", doc, timer);
      return ret;
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("exists - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC);
    } catch (Exception exc) {
      logError("exists", doc, exc);
      throw exc;
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, final XWikiContext context,
      final boolean bTransaction) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.INFO, "saveXWikiDoc", doc);
      documentStorePart.saveXWikiDoc(doc, context, bTransaction);
      logEnd(LogLevel.INFO, "saveXWikiDoc", doc, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC);
    } catch (Exception exc) {
      logError("saveXWikiDoc", doc, exc);
      throw exc;
    }
  }

  public CelementsIdComputer getIdComputer() {
    return idComputer;
  }

  // TODO CELDEV-531 - improve load performance
  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, final XWikiContext context)
      throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.INFO, "loadXWikiDoc", doc);
      XWikiDocument ret = documentStorePart.loadXWikiDoc(doc, context);
      logEnd(LogLevel.INFO, "loadXWikiDoc", doc, timer);
      return ret;
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_READING_DOC);
    } catch (Exception exc) {
      logError("loadXWikiDoc", doc, exc);
      throw exc;
    }
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.INFO, "deleteXWikiDoc", doc);
      documentStorePart.deleteXWikiDoc(doc, context);
      logEnd(LogLevel.INFO, "deleteXWikiDoc", doc, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC);
    } catch (Exception exc) {
      logError("deleteXWikiDoc", doc, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.DEBUG, "saveXObject", object);
      collectionStorePart.saveXWikiCollection(object, context, bTransaction);
      logEnd(LogLevel.DEBUG, "saveXObject", object, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXObject", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiCollection(BaseCollection object, XWikiDocument doc, XWikiContext context,
      boolean bTransaction, boolean alreadyLoaded) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.DEBUG, "loadXObject", object);
      collectionStorePart.loadXWikiCollection(object, doc, context, bTransaction, alreadyLoaded);
      logEnd(LogLevel.DEBUG, "loadXObject", object, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXObject", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction, boolean evict) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.DEBUG, "deleteXObject", object);
      collectionStorePart.deleteXWikiCollection(object, context, bTransaction, evict);
      logEnd(LogLevel.DEBUG, "deleteXObject", object, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT);
    } catch (Exception exc) {
      logError("deleteXObject", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.TRACE, "loadXProperty", property);
      propertyStorePart.loadXWikiProperty(property, context, bTransaction);
      logEnd(LogLevel.TRACE, "loadXProperty", property, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXProperty", property, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiProperty(final PropertyInterface property, final XWikiContext context,
      final boolean runInOwnTransaction) throws XWikiException {
    try {
      Stopwatch timer = logStart(LogLevel.TRACE, "saveXProperty", property);
      propertyStorePart.saveXWikiProperty(property, context, runInOwnTransaction);
      logEnd(LogLevel.TRACE, "saveXProperty", property, timer);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXProperty", property, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public BaseClass loadXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiClassProperty(PropertyClass property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  private Stopwatch logStart(LogLevel level, String msg, Object obj) {
    LogUtils.log(LOGGER, level, () -> buildLogMessage(msg + " - start", obj, null));
    return LogUtils.isLevelEnabled(LOGGER, level) ? Stopwatch.createStarted() : null;
  }

  private void logEnd(LogLevel level, String msg, Object obj, Stopwatch timer) {
    LogUtils.log(LOGGER, level, () -> buildLogMessage(msg + " - end", obj,
        (timer != null) ? timer.elapsed() : null));
  }

  private void logError(String msg, Object obj, Throwable cause) {
    LogUtils.log(LOGGER, LogLevel.ERROR, () -> buildLogMessage(msg + " - error", obj), cause);
  }

  private XWikiException newXWikiException(String msg, Object obj, Throwable cause, int code) {
    return new XWikiException(MODULE_XWIKI_STORE, code, buildLogMessage(msg, obj), cause);
  }

  private String buildLogMessage(String msg, Object obj) {
    return buildLogMessage(msg, obj, null);
  }

  private String buildLogMessage(String msg, Object obj, Duration time) {
    String timeMsg = (time != null) ? MessageFormat.format("took {0}", time) : "";
    if (obj instanceof XWikiDocument) {
      XWikiDocument doc = (XWikiDocument) obj;
      return MessageFormat.format("{0}: {1} {2} {3}", msg, Long.toString(doc.getId()),
          modelUtils.serializeRef(doc.getDocumentReference()), timeMsg);
    } else if (obj instanceof PropertyInterface) {
      PropertyInterface property = (PropertyInterface) obj;
      return MessageFormat.format("{0}: {1} {2} {3}", msg, Long.toString(property.getId()),
          property, timeMsg);
    } else {
      return MessageFormat.format("{0}: {1} {2}", msg, obj, timeMsg);
    }
  }

  public ModelUtils getModelUtils() {
    return modelUtils;
  }

  public ModelContext getModelContext() {
    return modelContext;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#isWikiNameAvailable(java.lang.String,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException {
    boolean available;

    boolean bTransaction = true;
    String database = context.getDatabase();

    try {
      bTransaction = beginTransaction(context);
      Session session = getSession(context);

      context.setDatabase(wikiName);
      try {
        setDatabase(session, context);
        SQLQuery query = session.createSQLQuery(
            "select TABLE_NAME from information_schema.tables where table_schema = :dbname");
        query.setParameter("dbname", getSchemaFromWikiName(context));
        query.list(); // if query is successfully executed -> assuming database exists.
        available = false;
      } catch (XWikiException | HibernateException e) {
        // Failed to switch to database. Assume it means database does not exists.
        available = true;
      }
    } catch (XWikiException e) {
      Object[] args = { wikiName };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE,
          "Exception while listing databases to search for {0}", e, args);
    } finally {
      context.setDatabase(database);
      if (bTransaction) {
        endTransaction(context, false);
      }
    }

    return available;
  }

}
