package com.celements.store;

import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableBiMap.*;
import static com.xpn.xwiki.XWikiException.*;
import static java.util.stream.Collectors.*;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.logging.LogLevel;
import com.celements.logging.LogUtils;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.DocumentIdComputer;
import com.celements.store.id.IdVersion;
import com.celements.store.id.UniqueHashIdComputer;
import com.celements.store.part.CelHibernateStoreCollectionPart;
import com.celements.store.part.CelHibernateStoreDocumentPart;
import com.celements.store.part.CelHibernateStorePropertyPart;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateStore;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

@Singleton
@Component(CelHibernateStore.NAME)
public class CelHibernateStore extends XWikiHibernateStore {

  public static final String NAME = "celHibernate";

  @Requirement(UniqueHashIdComputer.NAME)
  private CelementsIdComputer idComputer;

  @Requirement
  private List<DocumentIdComputer> docIdComputers;

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
      log(LogLevel.INFO, "exists - start", doc);
      // FIXME [CELDEV-924] Store add lang support for exists check and cache
      boolean ret = super.exists(doc, context);
      log(LogLevel.INFO, "exists - end", doc);
      return ret;
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("exists - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC);
    } catch (Exception exc) {
      logError("exists - error", doc, exc);
      throw exc;
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, final XWikiContext context,
      final boolean bTransaction) throws XWikiException {
    try {
      log(LogLevel.INFO, "saveXWikiDoc - start", doc);
      documentStorePart.saveXWikiDoc(doc, context, bTransaction);
      log(LogLevel.INFO, "saveXWikiDoc - end", doc);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC);
    } catch (Exception exc) {
      logError("saveXWikiDoc - error", doc, exc);
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
      log(LogLevel.INFO, "loadXWikiDoc - start", doc);
      XWikiDocument ret = documentStorePart.loadXWikiDoc(doc, context);
      log(LogLevel.INFO, "loadXWikiDoc - end", doc);
      return ret;
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_READING_DOC);
    } catch (Exception exc) {
      logError("loadXWikiDoc - error", doc, exc);
      throw exc;
    }
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      log(LogLevel.INFO, "deleteXWikiDoc - start", doc);
      documentStorePart.deleteXWikiDoc(doc, context);
      log(LogLevel.INFO, "deleteXWikiDoc - end", doc);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC);
    } catch (Exception exc) {
      logError("deleteXWikiDoc - error", doc, exc);
      throw exc;
    }
  }

  public String getDocKey(XWikiDocument doc) {
    return getDocKey(doc.getDocumentReference(), doc.getLanguage());
  }

  public String getDocKey(DocumentReference docRef, String lang) {
    return getDocKey(Stream.of(serialize(docRef, LOCAL), lang));
  }

  private String getDocKey(Stream<?> keyParts) {
    return keyParts.filter(Objects::nonNull).map(Object::toString)
        .collect(joining(".")).trim();
  }

  /**
   * returns a map with all existing docIds for all {@link IdVersion} for the given docRef and lang
   * sorted by collision count. the map value represents the {@link #getDocKey} for the docId
   */
  public ImmutableBiMap<Long, String> loadExistingDocKeys(Session session,
      DocumentReference docRef, String lang) throws HibernateException {
    Set<Long> allPossibleDocIds = StreamEx.of(docIdComputers)
        .map(computer -> computer.getDocumentIdIterator(docRef, lang))
        .flatMap(StreamEx::of)
        .toSet();
    return loadExistingDocKeys(session, allPossibleDocIds)
        .collect(toImmutableBiMap(Entry::getKey, Entry::getValue));
  }

  /**
   * docIds for the same doc are sorted by collision count firstly (and object count secondly)
   * irrespective of their signum due to 2-complement representation
   */
  @SuppressWarnings("unchecked")
  private EntryStream<Long, String> loadExistingDocKeys(Session session, Collection<Long> docIds) {
    Iterator<Object[]> iter = session.createQuery(
        "select id, fullName, language from XWikiDocument where id in (:ids) order by id")
        .setParameterList("ids", docIds)
        .iterate();
    return StreamEx.of(iter)
        .filter(Objects::nonNull)
        .mapToEntry(row -> getDocKey(Stream.of(row).skip(1)))
        .filterValues(not(String::isEmpty))
        .mapKeys(row -> Stream.of(row).findFirst().map(String::valueOf).map(Longs::tryParse)
            .orElse(null))
        .filterKeys(Objects::nonNull)
        .distinctKeys();
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      log(LogLevel.DEBUG, "saveXObject - start", object);
      collectionStorePart.saveXWikiCollection(object, context, bTransaction);
      log(LogLevel.DEBUG, "saveXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXObject - error", object, exc);
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
      log(LogLevel.DEBUG, "loadXObject - start", object);
      collectionStorePart.loadXWikiCollection(object, doc, context, bTransaction, alreadyLoaded);
      log(LogLevel.DEBUG, "loadXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXObject - error", object, exc);
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
      log(LogLevel.DEBUG, "deleteXObject - start", object);
      collectionStorePart.deleteXWikiCollection(object, context, bTransaction, evict);
      log(LogLevel.DEBUG, "deleteXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT);
    } catch (Exception exc) {
      logError("deleteXObject - error", object, exc);
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
      log(LogLevel.TRACE, "loadXProperty - start", property);
      propertyStorePart.loadXWikiProperty(property, context, bTransaction);
      log(LogLevel.TRACE, "loadXProperty - end", property);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXProperty - error", property, exc);
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
      log(LogLevel.TRACE, "saveXProperty - start", property);
      propertyStorePart.saveXWikiProperty(property, context, runInOwnTransaction);
      log(LogLevel.TRACE, "saveXProperty - end", property);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXProperty - error", property, exc);
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

  public void log(LogLevel level, String msg, Object obj) {
    LogUtils.log(logger, level, () -> buildLogMessage(msg, obj));
  }

  public void logError(String msg, Object obj, Throwable cause) {
    LogUtils.log(logger, LogLevel.ERROR, () -> buildLogMessage(msg, obj), cause);
  }

  public XWikiException newXWikiException(String msg, Object obj, Throwable cause, int code) {
    return new XWikiException(MODULE_XWIKI_STORE, code, buildLogMessage(msg, obj), cause);
  }

  public String buildLogMessage(String msg, Object obj) {
    if (obj instanceof XWikiDocument) {
      XWikiDocument doc = (XWikiDocument) obj;
      return MessageFormat.format("{0}: {1} {2}", msg, Long.toString(doc.getId()), getDocKey(doc));
    } else if (obj instanceof PropertyInterface) {
      PropertyInterface property = (PropertyInterface) obj;
      return MessageFormat.format("{0}: {1} {2}", msg, Long.toString(property.getId()), property);
    } else {
      return MessageFormat.format("{0}: {1}", msg, obj);
    }
  }

  public ModelUtils getModelUtils() {
    return modelUtils;
  }

  public String serialize(DocumentReference docRef, ReferenceSerializationMode mode) {
    return getModelUtils().serializeRef(docRef, mode);
  }

  public String serialize(XWikiDocument doc, ReferenceSerializationMode mode) {
    return serialize(doc.getDocumentReference(), mode);
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
    try {
      bTransaction = beginTransaction(context);
      Session session = getSession(context);
      String database = context.getDatabase();
      try {
        context.setDatabase(wikiName);
        setDatabase(session, context);
        SQLQuery query = session.createSQLQuery(
            "select TABLE_NAME from information_schema.tables where table_schema = :dbname");
        query.setParameter("dbname", getSchemaFromWikiName(context));
        query.list(); // if query is successfully executed -> assuming database exists.
        available = false;
      } catch (XWikiException | HibernateException e) {
        // Failed to switch to database. Assume it means database does not exists.
        available = true;
      } finally {
        context.setDatabase(database);
        setDatabase(session, context);
      }
    } catch (XWikiException e) {
      Object[] args = { wikiName };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE,
          "Exception while listing databases to search for {0}", e, args);
    } finally {
      if (bTransaction) {
        endTransaction(context, false);
      }
    }
    logger.trace("isWikiNameAvailable - [{}] [{}]", wikiName, available);
    return available;
  }

}
