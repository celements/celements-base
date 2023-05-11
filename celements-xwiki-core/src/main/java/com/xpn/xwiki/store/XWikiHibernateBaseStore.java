package com.xpn.xwiki.store;

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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class XWikiHibernateBaseStore implements Initializable {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Requirement
  private HibernateSessionFactory sessionFactory;

  private String hibpath = "/WEB-INF/hibernate.cfg.xml";

  /**
   * Key in XWikiContext for access to current hibernate database name.
   */
  private static String currentDatabaseKey = "hibcurrentdatabase";

  /**
   * THis allows to initialize our storage engine. The hibernate config file path is taken from
   * xwiki.cfg or directly
   * in the WEB-INF directory.
   *
   * @param xwiki
   * @param context
   * @deprecated 1.6M1. Use ComponentManager.lookup(String) instead.
   */
  @Deprecated
  public XWikiHibernateBaseStore(XWiki xwiki, XWikiContext context) {
    String path = xwiki.Param("xwiki.store.hibernate.path", "/WEB-INF/hibernate.cfg.xml");
    logger.debug("Hibernate configuration file: [" + path + "]");
    setPath(path);
  }

  /**
   * Initialize the storage engine with a specific path This is used for tests.
   *
   * @param hibpath
   * @deprecated 1.6M1. Use ComponentManager.lookup(String) instead.
   */
  @Deprecated
  public XWikiHibernateBaseStore(String hibpath) {
    setPath(hibpath);
  }

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateBaseStore() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize() throws InitializationException {
    Execution execution = Utils.getComponent(Execution.class);
    XWikiContext context = (XWikiContext) execution.getContext().getProperty("xwikicontext");
    setPath(context.getWiki().getConfig().getProperty("xwiki.store.hibernate.path", getPath()));
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
  private synchronized void initHibernate(XWikiContext context) throws HibernateException {
    getConfiguration().configure(getPath());
    XWiki wiki = context.getWiki();
    if ((wiki != null) && (wiki.Param("xwiki.db") != null) && !wiki.isVirtualMode()) {
      String schemaName = getSchemaFromWikiName(context.getDatabase(), context);
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
   */
  public Session getSession(XWikiContext context) {
    Session session = (Session) context.get("hibsession");
    // Make sure we are in this mode
    try {
      if (session != null) {
        session.setFlushMode(FlushMode.COMMIT);
      }
    } catch (org.hibernate.SessionException ex) {
      session = null;
    }
    return session;
  }

  /**
   * Allows to set the current session in the context This is set in beginTransaction
   *
   * @param session
   * @param context
   */
  public void setSession(Session session, XWikiContext context) {
    if (session == null) {
      context.remove("hibsession");
    } else {
      context.put("hibsession", session);
    }
  }

  /**
   * Allows to get the current transaction from the context This is set in beginTransaction
   *
   * @param context
   * @return
   */
  public Transaction getTransaction(XWikiContext context) {
    return (Transaction) context.get("hibtransaction");
  }

  /**
   * Allows to set the current transaction This is set in beginTransaction
   *
   * @param transaction
   * @param context
   */
  public void setTransaction(Transaction transaction, XWikiContext context) {
    if (transaction == null) {
      context.remove("hibtransaction");
    } else {
      context.put("hibtransaction", transaction);
    }
  }

  /**
   * Allows to shut down the hibernate configuration Closing all pools and connections
   *
   * @param context
   * @throws HibernateException
   */
  public void shutdownHibernate(XWikiContext context) throws HibernateException {
    Session session = getSession(context);
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
   */
  public void updateSchema(XWikiContext context) throws HibernateException {
    updateSchema(context, false);
  }

  /**
   * Allows to update the schema to match the hibernate mapping
   *
   * @param context
   * @param force
   *          defines wether or not to force the update despite the xwiki.cfg settings
   * @throws HibernateException
   */
  public synchronized void updateSchema(XWikiContext context, boolean force)
      throws HibernateException {
    // We don't update the schema if the XWiki hibernate config parameter says not to update
    if ((!force) && (context.getWiki() != null)
        && ("0".equals(context.getWiki().Param("xwiki.store.hibernate.updateschema")))) {
      logger.debug("updateSchema - deactivated for wiki [{}]", context.getDatabase());
      return;
    }
    logger.info("updateSchema - for wiki [{}]...", context.getDatabase());
    String[] schemaSQL = getSchemaUpdateScript(getConfiguration(), context);
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
        .toArray(String[]::new), context);
    logger.info("updateSchema - done {}", context.getDatabase());
  }

  /**
   * Convert wiki name in database/schema name.
   *
   * @param wikiName
   *          the wiki name to convert.
   * @param context
   *          the XWiki context.
   * @return the database/schema name.
   * @since XWiki Core 1.1.2, XWiki Core 1.2M2
   */
  protected String getSchemaFromWikiName(String wikiName, XWikiContext context) {
    if (wikiName == null) {
      return null;
    }
    XWiki wiki = context.getWiki();
    String schema;
    if (context.isMainWiki(wikiName)) {
      schema = wiki.Param("xwiki.db");
      if (schema == null) {
        schema = wikiName.replace('-', '_');
      }
    } else {
      // virtual
      schema = wikiName.replace('-', '_');
    }

    // Apply prefix
    String prefix = wiki.Param("xwiki.db.prefix", "");
    return prefix + schema;
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
   */
  protected String getSchemaFromWikiName(XWikiContext context) {
    return getSchemaFromWikiName(context.getDatabase(), context);
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
   */
  public String[] getSchemaUpdateScript(Configuration config, XWikiContext context)
      throws HibernateException {
    String[] schemaSQL;
    Session session;
    Connection connection;
    DatabaseMetadata meta;
    Statement stmt = null;
    Dialect dialect = Dialect.getDialect(getConfiguration().getProperties());
    boolean bTransaction = true;
    try {
      bTransaction = beginTransaction(context);
      session = getSession(context);
      connection = session.connection();
      setDatabase(session, context);
      meta = new DatabaseMetadata(connection, dialect);
      stmt = connection.createStatement();
      logger.trace("getSchemaUpdateScript - [{}] [{}]", context.getDatabase(), dialect);
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
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
    return schemaSQL;
  }

  /**
   * Runs the update script on the current database
   *
   * @param createSQL
   * @param context
   */
  private void updateSchema(String[] createSQL, XWikiContext context) {
    // Updating the schema for custom mappings
    Session session;
    Connection connection;
    Statement stmt = null;
    boolean bTransaction = true;
    String sql = "";
    try {
      bTransaction = beginTransaction(context);
      session = getSession(context);
      connection = session.connection();
      setDatabase(session, context);
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
          endTransaction(context, true);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * Custom Mapping This function update the schema based on the dynamic custom mapping provided by
   * the class
   *
   * @param bclass
   * @param context
   * @throws com.xpn.xwiki.XWikiException
   */
  public void updateSchema(BaseClass bclass, XWikiContext context) throws XWikiException {
    String custommapping = bclass.getCustomMapping();
    if (!bclass.hasExternalCustomMapping()) {
      return;
    }

    Configuration config = makeMapping(bclass.getName(), custommapping);
    /*
     * if (isValidCustomMapping(bclass.getName(), config, bclass)==false) { throw new
     * XWikiException(
     * XWikiException.MODULE_XWIKI_STORE,
     * XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Cannot update
     * schema for class " + bclass.getName() + " because of an invalid mapping"); }
     */

    String[] sql = getSchemaUpdateScript(config, context);
    updateSchema(sql, context);
  }

  /**
   * Initializes hibernate and calls updateSchema if necessary
   *
   * @param context
   * @throws HibernateException
   */
  public void checkHibernate(XWikiContext context) throws HibernateException {
    // Note : double locking is not a recommended pattern and is not guaranteed to work on all
    // machines. See for example http://www.ibm.com/developerworks/java/library/j-dcl.html
    if (getSessionFactory() == null) {
      synchronized (this) {
        if (getSessionFactory() == null) {

          initHibernate(context);
          /* Check Schema */
          if (getSessionFactory() != null) {
            updateSchema(context);
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
   */
  protected boolean isVirtual(XWikiContext context) {
    if ((context == null) || (context.getWiki() == null)) {
      return true;
    }

    return context.getWiki().isVirtualMode();
  }

  /**
   * Virtual Wikis Allows to switch database connection
   *
   * @param session
   * @param context
   * @throws XWikiException
   */
  public void setDatabase(Session session, XWikiContext context) throws XWikiException {
    if (isVirtual(context)) {
      try {
        logger.debug("setDatabase - {}", context.getDatabase());
        if (context.getDatabase() != null) {
          String schemaName = getSchemaFromWikiName(context);
          Connection connection = session.connection();
          if (!schemaName.equals(getCurrentSchema(connection))) {
            connection.setCatalog(schemaName);
          }
          setCurrentDatabase(context, context.getDatabase());
        }
      } catch (HibernateException | SQLException e) {
        Object[] args = { context.getDatabase() };
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
   */
  protected String escapeSchema(String schema, XWikiContext context) {
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
   */
  public boolean beginTransaction(XWikiContext context) throws XWikiException {
    return beginTransaction(null, context);
  }

  /**
   * Begins a transaction
   *
   * @param withTransaction
   * @param context
   * @return
   * @throws XWikiException
   *
   * @Deprecated since 6.0 instead use
   *             {@link #beginTransaction(XWikiContext)} or
   *             {@link #beginTransaction(SessionFactory, XWikiContext)}
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
   * @Deprecated since 6.0 instead use {@link #beginTransaction(SessionFactory, XWikiContext)}
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
   */
  public boolean beginTransaction(SessionFactory sfactory, XWikiContext context)
      throws HibernateException, XWikiException {
    Transaction transaction = getTransaction(context);
    Session session = getSession(context);
    if ((session == null) == (transaction != null)) {
      logger.warn("beginTransaction - incompatible session [{}] and transaction [{}] status",
          session,
          transaction);
      // TODO: Fix this problem, don't ignore it!
      return false;
    }
    if (session != null) {
      logger.debug("beginTransaction - taking session [{}] and transaction [{}] from context",
          session, transaction);
      return false;
    }
    if (sfactory == null) {
      session = getSessionFactory().openSession();
    } else {
      session = sfactory.openSession();
    }
    logger.debug("beginTransaction - taking session from pool {}", session);
    setSession(session, context);
    setDatabase(session, context);
    transaction = session.beginTransaction();
    setTransaction(transaction, context);
    logger.debug("Opened transaction {}", transaction);
    return true;
  }

  /**
   * Ends a transaction
   *
   * @param context
   * @param commit
   *          should we commit or not
   *
   * @Deprecated since 6.0 instead use {@link #endTransaction(XWikiContext, boolean)}
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
   * @param withTransaction
   * @throws HibernateException
   */
  public void endTransaction(XWikiContext context, boolean commit) throws HibernateException {
    Session session = null;
    try {
      session = getSession(context);
      Transaction transaction = getTransaction(context);
      setSession(null, context);
      setTransaction(null, context);
      if (transaction != null) {
        logger.debug("Releasing hibernate transaction {}", transaction);
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
    }
  }

  /**
   * Cleanup all sessions Used at the shutdown time
   *
   * @param context
   */
  public void cleanUp(XWikiContext context) {
    try {
      Session session = getSession(context);
      if (session != null) {
        logger.warn("Cleanup of session was needed: {}", session);
        endTransaction(context, false);
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
   */
  public <T> T execute(XWikiContext context, boolean bTransaction, boolean doCommit,
      HibernateCallback<T> cb)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      if ((context.getDatabase() != null)
          && !context.getDatabase().equals(getCurrentDatabase(context))) {
        setDatabase(getSession(context), context);
      }
      return cb.doInHibernate(getSession(context));
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
          endTransaction(context, doCommit);
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
   */
  public <T> T executeRead(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(context, bTransaction, false, cb);
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
   */
  public <T> T executeWrite(XWikiContext context, boolean bTransaction, HibernateCallback<T> cb)
      throws XWikiException {
    return execute(context, bTransaction, true, cb);
  }

  /**
   * @param context
   *          XWikiContext
   * @return current hibernate database name
   */
  private String getCurrentDatabase(XWikiContext context) {
    return (String) context.get(currentDatabaseKey);
  }

  /**
   * @param context
   *          XWikiContext
   * @param database
   *          current hibernate database name to set
   */
  private void setCurrentDatabase(XWikiContext context, String database) {
    context.put(currentDatabaseKey, database);
  }
}
