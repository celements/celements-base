package com.xpn.xwiki.store;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Stream;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.Dialect;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.tool.hbm2ddl.DatabaseMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class XWikiHibernateBaseStore implements Initializable {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private static final String DEFAULT_CFG_PATH = "/WEB-INF/hibernate.cfg.xml";

  private static final String KEY_CURRENT_DATABASE = "xwiki.store.hibernate.currentdatabase";
  private static final String KEY_SESSION = "xwiki.store.hibernate.session";
  private static final String KEY_TRANSACTION = "xwiki.store.hibernate.transaction";

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  protected Execution execution;

  @Requirement
  protected XWikiConfigSource xwikiCfg;

  private String hibpath = DEFAULT_CFG_PATH;

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateBaseStore() {}

  @Override
  public void initialize() throws InitializationException {
    setPath(xwikiCfg.getProperty("xwiki.store.hibernate.path", DEFAULT_CFG_PATH));
  }

  /**
   * Allows to get the current hibernate config file path
   *
   * @return
   */
  public String getPath() {
    return this.hibpath;
  }

  /**
   * Allows to set the current hibernate config file path
   *
   * @param hibpath
   */
  public void setPath(String hibpath) {
    this.hibpath = hibpath;
  }

  /**
   * Allows to init the hibernate configuration
   *
   * @throws org.hibernate.HibernateException
   */
  private synchronized void initHibernate(WikiReference wikiRef) throws HibernateException {
    getConfiguration().configure(getPath());
    if ((xwikiCfg.getProperty("xwiki.db") != null) && !xwikiCfg.isVirtualMode()) {
      String schemaName = getSchemaFromWikiName(wikiRef);
      getConfiguration().setProperty(Environment.DEFAULT_CATALOG, schemaName);
      getConfiguration().setProperty(Environment.DEFAULT_SCHEMA, schemaName);
    }
    if (this.sessionFactory == null) {
      this.sessionFactory = Utils.getComponent(HibernateSessionFactory.class);
    }
    setSessionFactory(getConfiguration().buildSessionFactory());
  }

  /**
   * This get's the current session. This is set in beginTransaction
   *
   * @param context
   * @return
   *
   * @deprecated since 6.0 instead use {@link #getSession()}
   */
  @Deprecated
  public Session getSession(XWikiContext context) {
    return getSession();
  }

  /**
   * This get's the current session. This is set in beginTransaction
   */
  public Session getSession() {
    Session session = (Session) getEContext().getProperty(KEY_SESSION);
    if (session != null) {
      session.setFlushMode(FlushMode.COMMIT);
      logger.trace("getSession - [{}]", defer(session::hashCode));
    }
    return session;
  }

  /**
   * Allows to set the current session in the context This is set in beginTransaction
   *
   * @param session
   * @param context
   *
   * @deprecated since 6.0 instead use {@link #setSession(Session)}
   */
  @Deprecated
  public void setSession(Session session, XWikiContext context) {
    setSession(session);
  }

  /**
   * Allows to set the current session in the context This is set in beginTransaction
   */
  public void setSession(Session session) {
    if (session == null) {
      getEContext().removeProperty(KEY_SESSION);
      logger.trace("setSession - cleared");
    } else {
      getEContext().setProperty(KEY_SESSION, session);
      logger.trace("setSession - [{}]", defer(session::hashCode));
    }
  }

  /**
   * Allows to get the current transaction from the context This is set in beginTransaction
   *
   * @param context
   * @return
   *
   * @deprecated since 6.0 instead use {@link #getTransaction()}
   */
  @Deprecated
  public Transaction getTransaction(XWikiContext context) {
    return getTransaction();
  }

  /**
   * Allows to get the current transaction from the context This is set in beginTransaction
   */
  public Transaction getTransaction() {
    return (Transaction) getEContext().getProperty(KEY_TRANSACTION);
  }

  /**
   * Allows to set the current transaction This is set in beginTransaction
   *
   * @param transaction
   * @param context
   *
   * @deprecated since 6.0 instead use {@link #setTransaction(Transaction)}
   */
  @Deprecated
  public void setTransaction(Transaction transaction, XWikiContext context) {
    setTransaction(transaction);
  }

  /**
   * Allows to set the current transaction This is set in beginTransaction
   */
  public void setTransaction(Transaction transaction) {
    if (transaction == null) {
      getEContext().removeProperty(KEY_TRANSACTION);
    } else {
      getEContext().setProperty(KEY_TRANSACTION, transaction);
    }
  }

  /**
   * Allows to shut down the hibernate configuration Closing all pools and connections
   *
   * @throws HibernateException
   */
  public void shutdownHibernate() throws HibernateException {
    Session session = getSession();
    closeSession(session);
    if (getSessionFactory() != null) {
      ((SessionFactoryImpl) getSessionFactory()).getConnectionProvider().close();
    }
  }

  /**
   * Allows to update the schema to match the hibernate mapping
   *
   * @param context
   * @throws HibernateException
   *
   * @deprecated since 6.0 instead use {@link #updateSchema(String)}
   */
  @Deprecated
  public void updateSchema(XWikiContext context) throws HibernateException {
    updateSchema(context, false);
  }

  /**
   * Allows to update the schema to match the hibernate mapping
   *
   * @throws HibernateException
   */
  public void updateSchema(WikiReference wikiRef) throws HibernateException {
    updateSchema(wikiRef, false);
  }

  /**
   * Allows to update the schema to match the hibernate mapping
   *
   * @param context
   * @param force
   *          defines wether or not to force the update despite the xwiki.cfg settings
   * @throws HibernateException
   *
   * @deprecated since 6.0 instead use {@link #updateSchema(String, boolean)}
   */
  @Deprecated
  public synchronized void updateSchema(XWikiContext context, boolean force)
      throws HibernateException {
    updateSchema(getWikiRef(context), force);
  }

  /**
   * Allows to update the schema to match the hibernate mapping
   *
   * @param wikiName
   * @param force
   *          defines wether or not to force the update despite the xwiki.cfg settings
   * @throws HibernateException
   */
  public synchronized void updateSchema(WikiReference wikiRef, boolean force)
      throws HibernateException {
    // We don't update the schema if the XWiki hibernate config parameter says not to update
    if (!force && ("0".equals(xwikiCfg.getProperty("xwiki.store.hibernate.updateschema")))) {
      logger.debug("updateSchema - deactivated for wiki [{}]", wikiRef);
      return;
    }
    logger.info("updateSchema - for wiki [{}]...", wikiRef);
    String[] schemaSQL = getSchemaUpdateScript(getConfiguration(), wikiRef);
    String[] addSQL = {
        // Make sure we have no null valued in integer fields
        "update xwikidoc set xwd_translation=0 where xwd_translation is null",
        "update xwikidoc set xwd_language='' where xwd_language is null",
        "update xwikidoc set xwd_default_language='' where xwd_default_language is null",
        "update xwikidoc set xwd_fullname=concat(xwd_web,'.',xwd_name) where xwd_fullname is null",
        "update xwikidoc set xwd_elements=3 where xwd_elements is null",
        "delete from xwikiproperties where xwp_name like 'editbox_%' and xwp_classtype='com.xpn.xwiki.objects.LongProperty'",
        "delete from xwikilongs where xwl_name like 'editbox_%'" };
    updateSchema(Stream.concat(Arrays.stream(schemaSQL), Arrays.stream(addSQL))
        .toArray(String[]::new), wikiRef);
    logger.info("updateSchema - done {}", wikiRef);
  }

  /**
   * Convert wiki name in database/schema name.
   *
   * @param wikiName
   *          the wiki name to convert.
   * @param context
   *          the XWiki context.
   * @return the database/schema name.
   *
   * @deprecated since 6.0 instead use {@link #getSchemaFromWikiName(String)}
   */
  @Deprecated
  public String getSchemaFromWikiName(String wikiName, XWikiContext context) {
    return getSchemaFromWikiName(Strings.isNullOrEmpty(wikiName) ? null
        : new WikiReference(wikiName));
  }

  /**
   * Convert wiki name in database/schema name.
   *
   * @param wikiName
   *          the wiki name to convert.
   * @return the database/schema name.
   */
  public String getSchemaFromWikiName(WikiReference wikiRef) {
    if (wikiRef == null) {
      return null;
    }
    String schemaName = "";
    if (XWikiConstant.MAIN_WIKI.equals(wikiRef)) {
      schemaName = xwikiCfg.getProperty("xwiki.db", "").trim();
    }
    if (schemaName.isEmpty()) {
      schemaName = wikiRef.getName();
    }
    logger.trace("getSchemaFromWikiName {} - {}", wikiRef, schemaName);
    return (xwikiCfg.getProperty("xwiki.db.prefix", "") + schemaName)
        .toLowerCase()
        .replace('-', '_')
        .replaceAll("[^a-z0-9_]", "");
  }

  /**
   * Convert context's database in real database/schema name.
   * <p>
   * Need hibernate to be initialized.
   *
   * @param context
   *          the XWiki context.
   * @return the database/schema name.
   * @since XWiki Core 1.1.2, XWiki Core 1.2M2
   *
   * @deprecated since 6.0 instead use {@link #getSchemaFromWikiName(String)}
   */
  @Deprecated
  protected String getSchemaFromWikiName(XWikiContext context) {
    return getSchemaFromWikiName(getWikiRef(context));
  }

  /**
   * This function gets the schema update scripts generated by comparing the current database woth
   * the current
   * hibernate mapping config.
   *
   * @param config
   * @param context
   * @return
   * @throws HibernateException
   *
   * @deprecated since 6.0 instead use {@link #getSchemaUpdateScript(Configuration, String)}
   */
  @Deprecated
  public String[] getSchemaUpdateScript(Configuration config, XWikiContext context)
      throws HibernateException {
    return getSchemaUpdateScript(config, getWikiRef(context));
  }

  /**
   * This function gets the schema update scripts generated by comparing the current database with
   * the current hibernate mapping config.
   *
   * @throws HibernateException
   */
  public String[] getSchemaUpdateScript(Configuration config, WikiReference wikiRef)
      throws HibernateException {
    String[] schemaSQL;
    Session session;
    Connection connection;
    DatabaseMetadata meta;
    Statement stmt = null;
    Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
    boolean bTransaction = true;
    try {
      bTransaction = beginTransaction(wikiRef);
      session = getSession();
      connection = session.connection();
      setDatabase(session, wikiRef);
      meta = new DatabaseMetadata(connection, dialect);
      stmt = connection.createStatement();
      logger.trace("getSchemaUpdateScript - [{}] [{}]", wikiRef, dialect);
      schemaSQL = config.generateSchemaUpdateScript(dialect, meta);
    } catch (Exception e) {
      logger.error("Failed creating schema update script", e);
      schemaSQL = new String[0];
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
        if (bTransaction) {
          endTransaction(false);
        }
      } catch (Exception e) {}
    }
    return schemaSQL;
  }

  /**
   * Runs the update script on the current database
   */
  private void updateSchema(String[] createSQL, WikiReference wikiRef) {
    // Updating the schema for custom mappings
    Session session;
    Connection connection;
    Statement stmt = null;
    boolean bTransaction = true;
    String sql = "";
    try {
      bTransaction = beginTransaction(wikiRef);
      session = getSession();
      connection = session.connection();
      setDatabase(session, wikiRef);
      stmt = connection.createStatement();
      for (String element : createSQL) {
        logger.debug("Update Schema sql: [{}]", element);
        stmt.executeUpdate(element);
      }
      connection.commit();
    } catch (Exception e) {
      logger.error("Failed updating schema while executing query [{}]", sql, e);
    } finally {
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e) {}
      try {
        if (bTransaction) {
          endTransaction(true);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * Initializes hibernate and calls updateSchema if necessary
   *
   * @param context
   * @throws HibernateException
   *
   * @deprecated since 6.0 instead use {@link #checkHibernate(String)}
   */
  @Deprecated
  public void checkHibernate(XWikiContext context) throws HibernateException {
    checkHibernate(getWikiRef(context));
  }

  /**
   * Initializes hibernate and calls updateSchema if necessary
   *
   * @throws HibernateException
   */
  public void checkHibernate(WikiReference wikiRef) throws HibernateException {
    // Note : double locking is not a recommended pattern and is not guaranteed to work on all
    // machines. See for example http://www.ibm.com/developerworks/java/library/j-dcl.html
    if (getSessionFactory() == null) {
      synchronized (this) {
        if (getSessionFactory() == null) {

          initHibernate(wikiRef);
          /* Check Schema */
          if (getSessionFactory() != null) {
            updateSchema(wikiRef);
          }
        }
      }
    }
  }

  /**
   * Checks if this xwiki setup is virtual meaning if multiple wikis can be accessed using the same
   * database pool
   *
   * @param context
   *          the XWiki context.
   * @return true if multi-wiki, false otherwise.
   *
   * @deprecated since 6.0 instead use xwikiCfg.isVirtualMode()
   */
  @Deprecated
  protected boolean isVirtual(XWikiContext context) {
    return xwikiCfg.isVirtualMode();
  }

  /**
   * Virtual Wikis Allows to switch database connection
   *
   * @param session
   * @param context
   * @throws XWikiException
   *
   * @deprecated since 6.0 instead use {@link #setDatabase(Session, String)}
   */
  @Deprecated
  public void setDatabase(Session session, XWikiContext context) throws XWikiException {
    setDatabase(session, getWikiRef(context));
  }

  /**
   * Virtual Wikis Allows to switch database connection
   *
   * @throws XWikiException
   */
  public void setDatabase(Session session, WikiReference wikiRef) throws XWikiException {
    if (xwikiCfg.isVirtualMode()) {
      try {
        if (wikiRef != null) {
          logger.trace("setDatabase - {} ", wikiRef);
          String schemaName = getSchemaFromWikiName(wikiRef);
          Connection connection = session.connection();
          if (!schemaName.equals(getCurrentSchema(connection))) {
            logger.debug("setDatabase - setting catalog to {} ", schemaName);
            connection.setCatalog(schemaName);
          }
          setCurrentWiki(wikiRef);
        }
      } catch (HibernateException | SQLException e) {
        Object[] args = { wikiRef };
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SWITCH_DATABASE,
            "Exception while switching to database {0}", e, args);
      }
    }
  }

  private String getCurrentSchema(Connection connection) {
    String catalog = null;
    try {
      catalog = connection.getCatalog();
      catalog = (catalog == null) ? null : catalog.replace('_', '-');
    } catch (SQLException exc) {
      logger.debug("getCurrentSchema - failed for inexistent schema", exc);
    }
    return catalog;
  }

  /**
   * Escape schema name depending of the database engine.
   *
   * @param schema
   *          the schema name to escape
   * @param context
   *          the XWiki context to get database engine identifier
   * @return the escaped version
   *
   * @deprecated since 6.0 instead use {@link #escapeSchema(String)}
   */
  @Deprecated
  protected String escapeSchema(String schema, XWikiContext context) {
    return escapeSchema(schema);
  }

  /**
   * Escape schema name depending of the database engine.
   *
   * @param schema
   *          the schema name to escape
   * @return the escaped version
   */
  protected String escapeSchema(String schema) {
    Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
    String closeQuote = String.valueOf(dialect.closeQuote());
    return dialect.openQuote() + schema.replace(closeQuote, closeQuote + closeQuote)
        + closeQuote;
  }

  /**
   * Begins a transaction
   *
   * @param context
   * @return
   * @throws XWikiException
   *
   * @Deprecated since 6.0 instead use {@link #beginTransaction(String)}
   */
  @Deprecated
  public boolean beginTransaction(XWikiContext context) throws XWikiException {
    return beginTransaction(null, context);
  }

  /**
   * Begins a transaction
   *
   * @throws HibernateException
   * @throws XWikiException
   *
   */
  public boolean beginTransaction(WikiReference wikiRef) throws HibernateException, XWikiException {
    return beginTransaction(null, wikiRef);
  }

  /**
   * Begins a transaction
   *
   * @param withTransaction
   * @param context
   * @return
   * @throws XWikiException
   *
   * @Deprecated since 6.0 instead use {@link #beginTransaction(String)}
   */
  @Deprecated
  public boolean beginTransaction(boolean withTransaction, XWikiContext context)
      throws XWikiException {
    return beginTransaction(null, context);
  }

  /**
   * Begins a transaction with a specific SessionFactory
   *
   * @param sfactory
   * @param withTransaction
   * @param context
   * @return
   * @throws XWikiException
   *
   * @Deprecated since 6.0 instead use {@link #beginTransaction(SessionFactory, String)}
   */
  @Deprecated
  public boolean beginTransaction(SessionFactory sfactory, boolean withTransaction,
      XWikiContext context) throws XWikiException {
    return beginTransaction(sfactory, context);
  }

  /**
   * Begins a transaction with a specific SessionFactory
   *
   * @param sfactory
   * @param context
   * @return
   * @throws HibernateException
   * @throws XWikiException
   *
   * @Deprecated since 6.0 instead use {@link #beginTransaction(SessionFactory, String)}
   */
  @Deprecated
  public boolean beginTransaction(SessionFactory sfactory, XWikiContext context)
      throws HibernateException, XWikiException {
    return beginTransaction(sfactory, getWikiRef(context));
  }

  /**
   * Begins a transaction with a specific SessionFactory
   *
   * @throws HibernateException
   * @throws XWikiException
   *
   */
  public boolean beginTransaction(SessionFactory sfactory, WikiReference wikiRef)
      throws HibernateException, XWikiException {
    Transaction transaction = getTransaction();
    Session session = getSession();
    checkState(((session == null) != (transaction != null)), // XNOR
        "beginTransaction - incompatible session and transaction status");
    if (session != null) {
      logger.trace("beginTransaction - [{}] taking session [{}] from context",
          defer(transaction::hashCode), defer(session::hashCode));
      return false;
    }
    session = ((sfactory != null) ? sfactory : getSessionFactory()).openSession();
    setDatabase(session, wikiRef);
    transaction = session.beginTransaction();
    logger.debug("beginTransaction - [{}] opened with session [{}]",
        defer(transaction::hashCode), defer(session::hashCode));
    setTransaction(transaction);
    setSession(session);
    return true;
  }

  /**
   * Ends a transaction
   *
   * @param context
   * @param commit
   *          should we commit or not
   *
   * @Deprecated since 6.0 instead use {@link #endTransaction(boolean)}
   */
  @Deprecated
  public void endTransaction(XWikiContext context, boolean commit, boolean withTransaction) {
    endTransaction(context, commit);
  }

  /**
   * Ends a transaction
   *
   * @param context
   * @param commit
   *          should we commit or not
   * @throws HibernateException
   *
   * @Deprecated since 6.0 instead use {@link #endTransaction(boolean)}
   */
  @Deprecated
  public void endTransaction(XWikiContext context, boolean commit) throws HibernateException {
    endTransaction(commit);
  }

  /**
   * Ends a transaction
   *
   * @param commit
   *          should we commit or not
   * @throws HibernateException
   */
  public void endTransaction(boolean commit) throws HibernateException {
    Session session = null;
    try {
      session = getSession();
      Transaction transaction = getTransaction();
      if (transaction != null) {
        logger.debug("endTransaction - [{}] commit {}", defer(transaction::hashCode), commit);
        if (commit) {
          transaction.commit();
        } else {
          transaction.rollback();
        }
      }
    } catch (HibernateException e) {
      // Ensure the original cause will get printed.
      throw new HibernateException("Failed to commit or rollback transaction. Root cause ["
          + getExceptionMessage(e) + "]", e);
    } finally {
      setSession(null);
      setTransaction(null);
      closeSession(session);
    }
  }

  /**
   * Hibernate and JDBC will wrap the exception thrown by the trigger in another exception (the
   * java.sql.BatchUpdateException) and this exception is sometimes wrapped again. Also the
   * java.sql.BatchUpdateException stores the underlying trigger exception in the nextException and
   * not in the cause
   * property. The following method helps you to get to the underlying trigger message.
   */
  private String getExceptionMessage(Throwable t) {
    StringBuilder sb = new StringBuilder();
    Throwable next = null;
    for (Throwable current = t; current != null; current = next) {
      next = current.getCause();
      if (next == current) {
        next = null;
      }
      if (current instanceof SQLException) {
        SQLException sx = (SQLException) current;
        while (sx.getNextException() != null) {
          sx = sx.getNextException();
          sb.append("\nSQL next exception = [" + sx + "]");
        }
      }
    }
    return sb.toString();
  }

  /**
   * Closes the hibernate session
   *
   * @param session
   * @throws HibernateException
   */
  private void closeSession(Session session) throws HibernateException {
    if (session != null) {
      session.close();
      logger.debug("closeSession - closed {}", defer(session::hashCode));
    }
  }

  /**
   * Cleanup all sessions Used at the shutdown time
   *
   * @param context
   */
  public void cleanUp(XWikiContext context) {
    try {
      Session session = getSession();
      if (session != null) {
        logger.warn("Cleanup of session was needed: {}", session);
        endTransaction(false);
      }
    } catch (HibernateException e) {}
  }

  public SessionFactory getSessionFactory() {
    return this.sessionFactory.getSessionFactory();
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory.setSessionFactory(sessionFactory);
  }

  public Configuration getConfiguration() {
    return this.sessionFactory.getConfiguration();
  }

  protected Configuration makeMapping(String className, String custommapping1) {
    Configuration hibconfig = new Configuration();
    {
      hibconfig.addXML(makeMapping(className, "xwikicustom_" + className.replaceAll("\\.", "_"),
          custommapping1));
    }
    hibconfig.buildMappings();
    return hibconfig;
  }

  protected String makeMapping(String entityName, String tableName, String custommapping1) {
    return "<?xml version=\"1.0\"?>\n" + "<!DOCTYPE hibernate-mapping PUBLIC\n"
        + "\t\"-//Hibernate/Hibernate Mapping DTD//EN\"\n"
        + "\t\"http://hibernate.sourceforge.net/hibernate-mapping-3.0.dtd\">\n"
        + "<hibernate-mapping>"
        + "<class entity-name=\"" + entityName + "\" table=\"" + tableName + "\">\n"
        + " <id name=\"id\" type=\"integer\" unsaved-value=\"any\">\n"
        + "   <column name=\"XWO_ID\" not-null=\"true\" />\n"
        + "   <generator class=\"assigned\" />\n"
        + " </id>\n" + custommapping1 + "</class>\n" + "</hibernate-mapping>";
  }

  /**
   * Callback (closure) interface for operations in hibernate. spring like.
   */
  public interface HibernateCallback<T> {

    /**
     * method executed by {@link XWikiHibernateBaseStore} and pass open session to it.
     *
     * @param session
     *          - open hibernate session
     * @return any you need be returned by
     *         {@link XWikiHibernateBaseStore#execute(XWikiContext, boolean, boolean, HibernateCallback)}
     * @throws HibernateException
     *           if any store specific exception
     * @throws XWikiException
     *           if exception in xwiki.
     */
    T doInHibernate(Session session) throws HibernateException, XWikiException;
  }

  /**
   * Execute method for operations in hibernate. spring like.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param context
   *          - used everywhere.
   * @param bTransaction
   *          - should store use old transaction(false) or create new (true)
   * @param doCommit
   *          - should store commit changes(if any), or rollback it.
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   *
   * @Deprecated since 6.0 instead use {@link #execute(String, boolean, boolean, HibernateCallback)}
   */
  @Deprecated
  public <T> T execute(XWikiContext context, boolean bTransaction, boolean doCommit,
      HibernateCallback<T> cb) throws XWikiException {
    return execute(getWikiRef(context), bTransaction, doCommit, cb);
  }

  /**
   * Execute method for operations in hibernate.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param wikiName
   * @param bTransaction
   *          - should store use old transaction(false) or create new (true)
   * @param doCommit
   *          - should store commit changes(if any), or rollback it.
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   */
  public <T> T execute(WikiReference wikiRef, boolean bTransaction, boolean doCommit,
      HibernateCallback<T> cb) throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(wikiRef);
        bTransaction = beginTransaction(wikiRef);
      }
      if ((wikiRef != null) && !wikiRef.equals(getCurrentWiki())) {
        setDatabase(getSession(), wikiRef);
      }
      return cb.doInHibernate(getSession());
    } catch (Exception e) {
      doCommit = false;
      if (e instanceof XWikiException) {
        throw (XWikiException) e;
      }
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_UNKNOWN,
          "Exception while hibernate execute", e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(doCommit);
        }
      } catch (Exception e) {
        logger.error("Exeption while close transaction", e);
      }
    }
  }

  /**
   * Execute method for read-only operations in hibernate. spring like.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param context
   *          - used everywhere.
   * @param bTransaction
   *          - should store to use old transaction(false) or create new (true)
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   * @see #execute(XWikiContext, boolean, boolean,
   *      com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
   *
   * @Deprecated since 6.0 instead use {@link #executeRead(String, boolean, HibernateCallback)}
   */
  @Deprecated
  public <T> T executeRead(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(getWikiRef(context), bTransaction, false, cb);
  }

  /**
   * Execute method for read-only operations in hibernate.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param wikiName
   * @param bTransaction
   *          - should store to use old transaction(false) or create new (true)
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   */
  public <T> T executeRead(WikiReference wikiRef, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(wikiRef, bTransaction, false, cb);
  }

  /**
   * Execute method for read-write operations in hibernate. spring like.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param context
   *          - used everywhere.
   * @param bTransaction
   *          - should store to use old transaction(false) or create new (true)
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   * @see #execute(XWikiContext, boolean, boolean,
   *      com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
   *
   * @Deprecated since 6.0 instead use {@link #executeWrite(String, boolean, HibernateCallback)}
   */
  @Deprecated
  public <T> T executeWrite(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(getWikiRef(context), bTransaction, true, cb);
  }

  /**
   * Execute method for read-write operations in hibernate.
   *
   * @return {@link HibernateCallback#doInHibernate(Session)}
   * @param wikiName
   * @param bTransaction
   *          - should store to use old transaction(false) or create new (true)
   * @param cb
   *          - callback to execute
   * @throws XWikiException
   *           if any error
   * @see #execute(XWikiContext, boolean, boolean,
   *      com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback)
   */
  public <T> T executeWrite(WikiReference wikiRef, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(wikiRef, bTransaction, true, cb);
  }

  /**
   * @return current hibernate database name
   */
  private WikiReference getCurrentWiki() {
    return (WikiReference) getEContext().getProperty(KEY_CURRENT_DATABASE);
  }

  /**
   * @param database
   *          current hibernate database name to set
   */
  private void setCurrentWiki(WikiReference wikiRef) {
    getEContext().setProperty(KEY_CURRENT_DATABASE, wikiRef);
  }

  protected final ExecutionContext getEContext() {
    return execution.getContext();
  }

  protected WikiReference getWikiRef(XWikiContext context) {
    return Strings.isNullOrEmpty(context.getDatabase()) ? null
        : new WikiReference(context.getDatabase());
  }

}
