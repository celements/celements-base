package com.celements.query;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.store.DefaultHibernateStore;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

@Component
public class QueryExecutionService implements IQueryExecutionServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryExecutionService.class);

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  @Override
  public List<List<String>> executeReadSql(String sql) throws XWikiException {
    return executeReadSql(String.class, sql);
  }

  @Override
  public <T> List<List<T>> executeReadSql(Class<T> type, String sql) throws XWikiException {
    Session session = null;
    try {
      session = getNewHibSession();
      List<?> result = session.createSQLQuery(sql).list();
      return harmoniseResult(type, result);
    } catch (HibernateException | ClassCastException exc) {
      throw new XWikiException(0, 0, "error while executing or parsing sql", exc);
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  private <T> List<List<T>> harmoniseResult(Class<T> type, List<?> result)
      throws ClassCastException {
    List<List<T>> ret = new ArrayList<>();
    for (Object elem : result) {
      List<T> resultRow = new ArrayList<>();
      if ((elem != null) && elem.getClass().isArray()) { // multiple columns selected
        for (Object col : ((Object[]) elem)) {
          resultRow.add(cast(type, col));
        }
      } else { // one column selected
        resultRow.add(cast(type, elem));
      }
      ret.add(resultRow);
    }
    return ret;
  }

  private <T> T cast(Class<T> type, Object elem) throws ClassCastException {
    if ((elem != null) && (type == String.class)) {
      elem = elem.toString();
    }
    return type.cast(elem);
  }

  @Override
  public int executeWriteSQL(String sql) throws XWikiException {
    return executeWriteSQLs(Arrays.asList(sql)).get(0);
  }

  @Override
  public List<Integer> executeWriteSQLs(List<String> sqls) throws XWikiException {
    Session session = null;
    try {
      session = getNewHibSession();
      return executeWriteSqlInTransaction(session, sqls);
    } catch (HibernateException hibExc) {
      throw new XWikiException(0, 0, "error while executing sql", hibExc);
    } finally {
      if (session != null) {
        session.close();
      }
    }
  }

  private List<Integer> executeWriteSqlInTransaction(Session session, List<String> sqls)
      throws HibernateException {
    List<Integer> results = new ArrayList<>();
    Transaction transaction = session.beginTransaction();
    boolean success = false;
    try {
      for (String sql : sqls) {
        results.add(session.createSQLQuery(sql).executeUpdate());
        LOGGER.info("executed sql '{}' for db '{}' returned '{}'", sql, context.getWikiRef(),
            results.get(results.size() - 1));
      }
      success = true;
    } finally {
      if (success) {
        transaction.commit();
      } else {
        transaction.rollback();
      }
    }
    return results;
  }

  private Session getNewHibSession() throws XWikiException, HibernateException {
    Session session = getHibStore().getSessionFactory().openSession();
    getHibStore().setDatabase(session, context.getWikiRef());
    return session;
  }

  @Override
  public int executeWriteHQL(String hql, Map<String, Object> binds) throws XWikiException {
    return executeWriteHQL(hql, binds, null);
  }

  @Override
  public int executeWriteHQL(final String hql, final Map<String, Object> binds,
      WikiReference wikiRef) throws XWikiException {
    wikiRef = Optional.ofNullable(wikiRef).orElse(context.getWikiRef());
    HibernateCallback<Integer> callback = new ExecuteWriteCallback(hql, binds);
    return getHibStore().executeWrite(wikiRef, true, callback);
  }

  @Override
  public DocumentReference executeAndGetDocRef(Query query) throws QueryException {
    DocumentReference ret = null;
    List<DocumentReference> list = executeAndGetDocRefs(query);
    if (list.size() > 0) {
      ret = list.get(0);
    }
    return ret;
  }

  @Override
  public List<DocumentReference> executeAndGetDocRefs(Query query) throws QueryException {
    List<DocumentReference> ret = new ArrayList<>();
    WikiReference wikiRef = context.getWikiRef();
    if (!Strings.isNullOrEmpty(query.getWiki())) {
      wikiRef = modelUtils.resolveRef(query.getWiki(), WikiReference.class);
    }
    for (Object fullName : query.execute()) {
      if ((fullName instanceof String) && !Strings.isNullOrEmpty((String) fullName)) {
        ret.add(modelUtils.resolveRef((String) fullName, DocumentReference.class, wikiRef));
      } else {
        LOGGER.debug("executeAndGetDocRefs: received invalid fullName '{}'", fullName);
      }
    }
    LOGGER.info("executeAndGetDocRefs: {} results for query '{}' and wiki '{}'", ret.size(),
        query.getStatement(), wikiRef);
    return ret;
  }

  private XWikiHibernateStore getHibStore() {
    return (XWikiHibernateStore) Utils.getComponent(XWikiStoreInterface.class,
        DefaultHibernateStore.NAME);
  }

  @Override
  public boolean existsIndex(WikiReference wikiRef, String table, String name)
      throws XWikiException {
    String sql = getIndexExistSql(modelUtils.getDatabaseName(wikiRef), checkNotNull(emptyToNull(
        table)), checkNotNull(emptyToNull(name)));
    return executeReadSql(String.class, sql).size() > 0;
  }

  private String getIndexExistSql(String database, String table, String name) {
    return "select INDEX_NAME from INFORMATION_SCHEMA.STATISTICS where TABLE_SCHEMA = '" + database
        + "' " + "and TABLE_NAME = '" + table + "' " + "and INDEX_NAME = '" + name + "'";
  }

}
