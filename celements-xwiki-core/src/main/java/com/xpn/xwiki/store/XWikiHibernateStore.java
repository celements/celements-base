/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.store;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Settings;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * The XWiki Hibernate database driver.
 *
 * @version $Id$
 */
@Component
public class XWikiHibernateStore extends XWikiHibernateBaseStore implements XWikiStoreInterface {

  private static final Log log = LogFactory.getLog(XWikiHibernateStore.class);

  private Map<String, String[]> validTypesMap = new HashMap<>();

  /**
   * QueryManager for this store.
   */
  @Requirement
  private QueryManager queryManager;

  /**
   * Used to convert a string into a proper Document Reference.
   */
  private DocumentReferenceResolver currentDocumentReferenceResolver = Utils
      .getComponent(DocumentReferenceResolver.class, "current");

  /**
   * Used to resolve a string into a proper Document Reference using the current document's
   * reference to fill the
   * blanks, except for the page name for which the default page name is used instead and for the
   * wiki name for which
   * the current wiki is used instead of the current document reference's wiki.
   */
  private DocumentReferenceResolver currentMixedDocumentReferenceResolver = Utils
      .getComponent(DocumentReferenceResolver.class, "currentmixed");

  /**
   * Used to convert a proper Document Reference to string (standard form).
   */
  private EntityReferenceSerializer<String> defaultEntityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class);

  /**
   * Used to convert a Document Reference to string (compact form without the wiki part).
   */
  private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class, "compactwiki");

  /**
   * Used to convert a proper Document Reference to a string but without the wiki name.
   */
  private EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils
      .getComponent(EntityReferenceSerializer.class, "local");

  /**
   * This allows to initialize our storage engine. The hibernate config file path is taken from
   * xwiki.cfg or directly
   * in the WEB-INF directory.
   *
   * @param xwiki
   * @param context
   * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
   */
  @Deprecated
  public XWikiHibernateStore(XWiki xwiki, XWikiContext context) {
    super(xwiki, context);
    initValidColumTypes();
  }

  /**
   * Initialize the storage engine with a specific path. This is used for tests.
   *
   * @param hibpath
   * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
   */
  @Deprecated
  public XWikiHibernateStore(String hibpath) {
    super(hibpath);
    initValidColumTypes();
  }

  /**
   * @see #XWikiHibernateStore(XWiki, XWikiContext)
   * @deprecated 1.6M1. Use ComponentManager.lookup(XWikiStoreInterface.class) instead.
   */
  @Deprecated
  public XWikiHibernateStore(XWikiContext context) {
    this(context.getWiki(), context);
  }

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateStore() {
    initValidColumTypes();
  }

  /**
   * This initializes the valid custom types Used for Custom Mapping
   */
  private void initValidColumTypes() {
    String[] string_types = { "string", "text", "clob" };
    String[] number_types = { "integer", "long", "float", "double", "big_decimal", "big_integer",
        "yes_no", "true_false" };
    String[] date_types = { "date", "time", "timestamp" };
    String[] boolean_types = { "boolean", "yes_no", "true_false", "integer" };
    this.validTypesMap = new HashMap<>();
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.StringClass", string_types);
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.TextAreaClass", string_types);
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.PasswordClass", string_types);
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.NumberClass", number_types);
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.DateClass", date_types);
    this.validTypesMap.put("com.xpn.xwiki.objects.classes.BooleanClass", boolean_types);
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
        available = false;
      } catch (XWikiException e) {
        // Failed to switch to database. Assume it means database does not exists.
        available = true;
      }
    } catch (Exception e) {
      Object[] args = { wikiName };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DATABASE,
          "Exception while listing databases to search for {0}", e, args);
    } finally {
      context.setDatabase(database);
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }

    return available;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#createWiki(java.lang.String,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public void createWiki(String wikiName, XWikiContext context) throws XWikiException {
    boolean bTransaction = true;
    String database = context.getDatabase();
    Statement stmt = null;
    try {
      bTransaction = beginTransaction(context);
      Session session = getSession(context);
      Connection connection = session.connection();
      stmt = connection.createStatement();

      String schema = getSchemaFromWikiName(wikiName, context);
      String escapedSchema = escapeSchema(schema, context);

      DatabaseProduct databaseProduct = getDatabaseProductName(context);
      if (DatabaseProduct.ORACLE == databaseProduct) {
        stmt.execute("create user " + escapedSchema + " identified by " + escapedSchema);
        stmt.execute("grant resource to " + escapedSchema);
      } else if (DatabaseProduct.DERBY == databaseProduct) {
        stmt.execute("CREATE SCHEMA " + escapedSchema);
      } else if (DatabaseProduct.HSQLDB == databaseProduct) {
        stmt.execute("CREATE SCHEMA " + escapedSchema + " AUTHORIZATION DBA");
      } else if (DatabaseProduct.DB2 == databaseProduct) {
        stmt.execute("CREATE SCHEMA " + escapedSchema);
      } else // TODO: find a proper java lib to convert from java encoding to mysql charset name and
      // collation
      if ((DatabaseProduct.MYSQL == databaseProduct)
          && context.getWiki().getEncoding().equals("UTF-8")) {
        stmt.execute("create database " + escapedSchema + " CHARACTER SET utf8 COLLATE utf8_bin");
      } else {
        stmt.execute("create database " + escapedSchema);
      }

      endTransaction(context, true);
    } catch (Exception e) {
      Object[] args = { wikiName };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CREATE_DATABASE,
          "Exception while create wiki database {0}",
          e, args);
    } finally {
      context.setDatabase(database);
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e) {}
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#deleteWiki(java.lang.String,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public void deleteWiki(String wikiName, XWikiContext context) throws XWikiException {
    boolean bTransaction = true;
    String database = context.getDatabase();
    Statement stmt = null;
    try {
      bTransaction = beginTransaction(context);
      Session session = getSession(context);
      Connection connection = session.connection();
      stmt = connection.createStatement();

      String schema = getSchemaFromWikiName(wikiName, context);
      String escapedSchema = escapeSchema(schema, context);

      DatabaseProduct databaseProduct = getDatabaseProductName(context);
      if (DatabaseProduct.ORACLE == databaseProduct) {
        stmt.execute("DROP USER " + escapedSchema + " CASCADE");
      } else if ((DatabaseProduct.DERBY == databaseProduct)
          || (DatabaseProduct.HSQLDB == databaseProduct)) {
        stmt.execute("DROP SCHEMA " + escapedSchema);
      } else if (DatabaseProduct.DB2 == databaseProduct) {
        stmt.execute("DROP SCHEMA " + escapedSchema + " RESTRICT");
      } else if (DatabaseProduct.MYSQL == databaseProduct) {
        stmt.execute("DROP DATABASE " + escapedSchema);
      }

      endTransaction(context, true);
    } catch (Exception e) {
      Object[] args = { wikiName };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETE_DATABASE,
          "Exception while delete wiki database {0}",
          e, args);
    } finally {
      context.setDatabase(database);
      try {
        if (stmt != null) {
          stmt.close();
        }
      } catch (Exception e) {}
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * Verifies if a wiki document exists
   *
   * @param doc
   * @param context
   * @return
   * @throws XWikiException
   */
  @Override
  public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
    boolean bTransaction = true;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {

      doc.setStore(this);
      checkHibernate(context);

      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }

      bTransaction = bTransaction && beginTransaction(false, context);
      Session session = getSession(context);
      String fullName = doc.getFullName();

      String sql = "select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName";
      if (monitor != null) {
        monitor.setTimerDesc("hibernate", sql);
      }
      Query query = session.createQuery(sql);
      query.setString("fullName", fullName);
      Iterator<String> it = query.list().iterator();
      while (it.hasNext()) {
        if (fullName.equals(it.next())) {
          return true;
        }
      }
      return false;
    } catch (Exception e) {
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CHECK_EXISTS_DOC,
          "Exception while reading document {0}", e,
          args);
    } finally {
      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }

      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    saveXWikiDoc(doc, context, true);
  }

  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    boolean bTransaction = true;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }
      checkHibernate(context);
      SessionFactory sfactory = injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && beginTransaction(sfactory, context);
      Session session = getSession(context);
      session.setFlushMode(FlushMode.COMMIT);

      if (doc.getStore() == null) {
        Object[] args = { doc.getFullName() };
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
            "Impossible to delete document {0} if it is not loaded", null, args);
      }

      // Let's delete any attachment this document might have
      List attachlist = doc.getAttachmentList();
      for (Object element : attachlist) {
        XWikiAttachment attachment = (XWikiAttachment) element;
        context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, false, context,
            false);
      }

      // deleting XWikiLinks
      if (context.getWiki().hasBacklinks(context)) {
        deleteLinks(doc.getId(), context, true);
      }

      BaseClass bclass = doc.getXClass();
      if ((bclass.getFieldList().size() > 0) && (useClassesTable(true, context))) {
        deleteXWikiClass(bclass, context, false);
      }

      // Find the list of classes for which we have an object
      // Remove properties planned for removal
      if (doc.getObjectsToRemove().size() > 0) {
        for (BaseObject bobj : doc.getObjectsToRemove()) {
          if (bobj != null) {
            deleteXWikiObject(bobj, context, false);
          }
        }
        doc.setObjectsToRemove(new ArrayList<BaseObject>());
      }
      for (List<BaseObject> objects : doc.getXObjects().values()) {
        for (BaseObject obj : objects) {
          if (obj != null) {
            deleteXWikiObject(obj, context, false);
          }
        }
      }
      context.getWiki().getVersioningStore().deleteArchive(doc, false, context);

      session.delete(doc);

      // We need to ensure that the deleted document becomes the original document
      doc.setOriginalDocument(doc.clone());

      if (bTransaction) {
        endTransaction(context, true);
      }
    } catch (Exception e) {
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC,
          "Exception while deleting document {0}", e,
          args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveXWikiObject(BaseObject object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    saveXWikiCollection(object, context, bTransaction);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadXWikiObject(BaseObject object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    loadXWikiCollection(object, null, context, bTransaction, false);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    loadXWikiCollection(object, null, context, bTransaction, false);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction,
      boolean alreadyLoaded) throws XWikiException {
    loadXWikiCollection(object, null, context, bTransaction, alreadyLoaded);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadXWikiCollection(BaseCollection object1, XWikiDocument doc, XWikiContext context,
      boolean bTransaction, boolean alreadyLoaded) throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction)
      throws XWikiException {
    deleteXWikiCollection(object, context, bTransaction, false);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction, boolean evict) throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void deleteXWikiObject(BaseObject baseObject, XWikiContext context, boolean bTransaction,
      boolean bEvict)
      throws XWikiException {
    deleteXWikiCollection(baseObject, context, bTransaction, bEvict);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void deleteXWikiObject(BaseObject baseObject, XWikiContext context, boolean b)
      throws XWikiException {
    deleteXWikiCollection(baseObject, context, b);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void deleteXWikiClass(BaseClass baseClass, XWikiContext context, boolean b)
      throws XWikiException {
    deleteXWikiCollection(baseClass, context, b);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(false, context);
      }
      Session session = getSession(context);

      try {
        session.load(property, (Serializable) property);
        // In Oracle, empty string are converted to NULL. Since an undefined property is not found
        // at all, it is
        // safe to assume that a retrieved NULL value should actually be an empty string.
        if (property instanceof BaseStringProperty) {
          BaseStringProperty stringProperty = (BaseStringProperty) property;
          if (stringProperty.getValue() == null) {
            stringProperty.setValue("");
          }
        }
      } catch (ObjectNotFoundException e) {
        // Let's accept that there is no data in property tables
        // but log it
        if (log.isErrorEnabled()) {
          log.error(
              "No data for property " + property.getName() + " of object id " + property.getId());
        }
      }

      // TODO: understand why collections are lazy loaded
      // Let's force reading lists if there is a list
      // This seems to be an issue since Hibernate 3.0
      // Without this test ViewEditTest.testUpdateAdvanceObjectProp fails
      if (property instanceof ListProperty) {
        ((ListProperty) property).getList();
      }

      if (bTransaction) {
        endTransaction(context, false, false);
      }
    } catch (Exception e) {
      BaseCollection obj = property.getObject();
      Object[] args = { (obj != null) ? obj.getName() : "unknown", property.getName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
          "Exception while loading property {1} of object {0}", e, args);

    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveXWikiProperty(final PropertyInterface property,
      final XWikiContext context, final boolean runInOwnTransaction) throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public BaseClass loadXWikiClass(BaseClass bclass, XWikiContext context) throws XWikiException {
    return loadXWikiClass(bclass, context, true);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public BaseClass loadXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveXWikiClassProperty(PropertyClass property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    throw new UnsupportedOperationException("overwritten by CelHibStore");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void loadAttachmentList(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(false, context);
      }
      Session session = getSession(context);

      Query query = session.createQuery("from XWikiAttachment as attach where attach.docId=:docid");
      query.setLong("docid", doc.getId());
      List<XWikiAttachment> list = query.list();
      for (XWikiAttachment attachment : list) {
        attachment.setDoc(doc);
      }
      doc.setAttachmentList(list);
      if (bTransaction) {
        endTransaction(context, false, false);
        bTransaction = false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCHING_ATTACHMENT,
          "Exception while searching attachments for documents {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveAttachmentList(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      getSession(context);

      List<XWikiAttachment> list = doc.getAttachmentList();
      for (XWikiAttachment attachment : list) {
        attachment.setDoc(doc);
        saveAttachment(attachment, false, context, false);
      }

      if (bTransaction) {
        // The session is closed here, too.
        endTransaction(context, true);
      }
    } catch (Exception e) {
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT_LIST,
          "Exception while saving attachments attachment list of document {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    saveAttachment(attachment, true, context, bTransaction);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void saveAttachment(XWikiAttachment attachment, boolean parentUpdate, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      Session session = getSession(context);

      Query query = session
          .createQuery("select attach.id from XWikiAttachment as attach where attach.id = :id");
      query.setLong("id", attachment.getId());
      if (query.uniqueResult() == null) {
        session.save(attachment);
      } else {
        session.update(attachment);
      }

      // If the attachment content is "dirty" (out of sync with the database)
      if (attachment.isContentDirty()) {
        // We must save the content of the attachment.
        // updateParent and bTransaction must be false because the content should be saved in the
        // same
        // transation as the attachment and if the parent doc needs to be updated, this function
        // will do it.
        context.getWiki().getAttachmentStore().saveAttachmentContent(attachment, false, context,
            false);
      }

      if (parentUpdate) {
        context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
      }

      if (bTransaction) {
        endTransaction(context, true);
      }

      // Mark the attachment content and metadata as not dirty.
      // Ideally this would only happen if the transaction is committed successfully but since an
      // unsuccessful
      // transaction will most likely be accompanied by an exception, the cache will not have a
      // chance to save
      // the copy of the document with erronious information. If this is not set here, the cache
      // will return
      // a copy of the attachment which claims to be dirty although it isn't.
      attachment.setMetaDataDirty(false);
      if (attachment.isContentDirty()) {
        attachment.getAttachment_content().setContentDirty(false);
      }

    } catch (Exception e) {
      Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_ATTACHMENT,
          "Exception while saving attachments for attachment {0} of document {1}", e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    XWikiLock lock = null;
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(false, context);
      }
      Session session = getSession(context);

      Query query = session
          .createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId");
      query.setLong("docId", docId);
      if (query.uniqueResult() != null) {
        lock = new XWikiLock();
        session.load(lock, new Long(docId));
      }

      if (bTransaction) {
        endTransaction(context, false, false);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LOCK, "Exception while loading lock",
          e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
    return lock;
  }

  @Override
  public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      Session session = getSession(context);

      Query query = session
          .createQuery("select lock.docId from XWikiLock as lock where lock.docId = :docId");
      query.setLong("docId", lock.getDocId());
      if (query.uniqueResult() == null) {
        session.save(lock);
      } else {
        session.update(lock);
      }

      if (bTransaction) {
        endTransaction(context, true);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_LOCK,
          "Exception while locking document", e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      Session session = getSession(context);

      session.delete(lock);

      if (bTransaction) {
        endTransaction(context, true);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LOCK, "Exception while deleting lock",
          e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  @Override
  public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    List<XWikiLink> links = new ArrayList<>();
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(false, context);
      }
      Session session = getSession(context);

      Query query = session.createQuery(" from XWikiLink as link where link.id.docId = :docId");
      query.setLong("docId", docId);

      links = query.list();

      if (bTransaction) {
        endTransaction(context, false, false);
        bTransaction = false;
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_LINKS, "Exception while loading links",
          e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
    return links;
  }

  /**
   * @since 2.2M2
   */
  @Override
  public List<DocumentReference> loadBacklinks(DocumentReference documentReference,
      boolean bTransaction,
      XWikiContext context) throws XWikiException {
    List<DocumentReference> backlinkReferences = new ArrayList<>();
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(false, context);
      }
      Session session = getSession(context);

      // the select clause is compulsory to reach the fullName i.e. the page pointed
      Query query = session.createQuery("select backlink.fullName from XWikiLink as backlink where "
          + "backlink.id.link = :backlink");
      query.setString("backlink", this.localEntityReferenceSerializer.serialize(documentReference));

      List<String> backlinkNames = query.list();

      // Convert strings into references
      for (String backlinkName : backlinkNames) {
        backlinkReferences.add(this.currentMixedDocumentReferenceResolver.resolve(backlinkName));
      }

      if (bTransaction) {
        endTransaction(context, false, false);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_BACKLINKS,
          "Exception while loading backlinks", e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
    return backlinkReferences;
  }

  /**
   * @deprecated since 2.2M2 use {@link #loadBacklinks(DocumentReference, boolean, XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    List<String> backlinkNames = new ArrayList<>();
    List<DocumentReference> backlinkReferences = loadBacklinks(
        this.currentMixedDocumentReferenceResolver.resolve(fullName), bTransaction, context);
    for (DocumentReference backlinkReference : backlinkReferences) {
      backlinkNames.add(this.localEntityReferenceSerializer.serialize(backlinkReference));
    }
    return backlinkNames;
  }

  @Override
  public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      Session session = getSession(context);

      // need to delete existing links before saving the page's one
      deleteLinks(doc.getId(), context, bTransaction);

      // necessary to blank links from doc
      context.remove("links");

      if (doc.getSyntaxId().equals("xwiki/1.0")) {
        saveLinks10(doc, context, session);
      } else {
        // When not in 1.0 content get WikiLinks directly from XDOM
        Set<XWikiLink> links = doc.getUniqueWikiLinkedPages(context);
        for (XWikiLink wikiLink : links) {
          session.save(wikiLink);
        }
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_LINKS, "Exception while saving links",
          e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  private void saveLinks10(XWikiDocument doc, XWikiContext context, Session session)
      throws XWikiException {
    // call to RenderEngine and converting the list of links into a list of backlinks
    // Note: We need to set the passed document as the current document as the "wiki"
    // renderer uses context.getDoc().getSpace() to find out the space name if no
    // space is specified in the link. A better implementation would be to pass
    // explicitely the current space to the render() method.
    ExecutionContext econtext = Utils.getComponent(Execution.class).getContext();

    List<String> links;
    try {
      // Create new clean context to avoid wiki manager plugin requests in same session
      XWikiContext renderContext = (XWikiContext) context.clone();

      renderContext.setDoc(doc);
      econtext.setProperty("xwikicontext", renderContext);

      setSession(null, renderContext);
      setTransaction(null, renderContext);

      XWikiRenderer renderer = renderContext.getWiki().getRenderingEngine().getRenderer("wiki");
      renderer.render(doc.getContent(), doc, doc, renderContext);

      links = (List<String>) renderContext.get("links");
    } catch (Exception e) {
      // If the rendering fails lets forget backlinks without errors
      links = Collections.emptyList();
    } finally {
      econtext.setProperty("xwikicontext", context);
    }

    if (links != null) {
      for (String reference : links) {
        // XWikiLink is the object declared in the Hibernate mapping
        XWikiLink link = new XWikiLink();
        link.setDocId(doc.getId());
        link.setFullName(doc.getFullName());
        link.setLink(reference);

        session.save(link);
      }
    }
  }

  @Override
  public void deleteLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      if (bTransaction) {
        checkHibernate(context);
        bTransaction = beginTransaction(context);
      }
      Session session = getSession(context);

      Query query = session
          .createQuery("delete from XWikiLink as link where link.id.docId = :docId");
      query.setLong("docId", docId);
      query.executeUpdate();

      if (bTransaction) {
        endTransaction(context, true);
        bTransaction = false;
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_LINKS,
          "Exception while deleting links", e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false);
        }
      } catch (Exception e) {}
    }
  }

  public void getContent(XWikiDocument doc, StringBuffer buf) {
    buf.append(doc.getContent());
  }

  @Override
  public List<String> getClassList(XWikiContext context) throws XWikiException {
    boolean bTransaction = true;
    try {
      checkHibernate(context);
      bTransaction = beginTransaction(false, context);
      Session session = getSession(context);

      Query query = session.createQuery("select doc.fullName from XWikiDocument as doc "
          + "where (doc.xWikiClassXML is not null and doc.xWikiClassXML like '<%')");
      Iterator<String> it = query.list().iterator();
      List<String> list = new ArrayList<>();
      while (it.hasNext()) {
        String name = it.next();
        list.add(name);
      }

      if (useClassesTable(false, context)) {
        query = session.createQuery("select bclass.name from BaseClass as bclass");
        it = query.list().iterator();
        while (it.hasNext()) {
          String name = it.next();
          if (!list.contains(name)) {
            list.add(name);
          }
        }
      }
      if (bTransaction) {
        endTransaction(context, false, false);
      }
      return list;
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH, "Exception while searching class list",
          e);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}
    }
  }

  private boolean useClassesTable(boolean write, XWikiContext context) {
    String param = "xwiki.store.hibernate.useclasstables";
    if (write) {
      return ("1".equals(context.getWiki().Param(param + ".write", "0")));
    } else {
      return ("1".equals(context.getWiki().Param(param + ".read", "1")));
    }
  }

  /**
   * Add values into named query.
   *
   * @param parameterId
   *          the parameter id to increment.
   * @param query
   *          the query to fill.
   * @param parameterValues
   *          the values to add to query.
   * @return the id of the next parameter to add.
   */
  private int injectParameterListToQuery(int parameterId, Query query, Collection parameterValues) {
    int index = parameterId;

    if (parameterValues != null) {
      for (Iterator valueIt = parameterValues.iterator(); valueIt.hasNext(); ++index) {
        injectParameterToQuery(index, query, valueIt.next());
      }
    }

    return index;
  }

  /**
   * Add value into named query.
   *
   * @param parameterId
   *          the parameter id to increment.
   * @param query
   *          the query to fill.
   * @param parameterValue
   *          the values to add to query.
   */
  private void injectParameterToQuery(int parameterId, Query query, Object parameterValue) {
    query.setParameter(parameterId, parameterValue);
  }

  /**
   * @since 2.2M2
   */
  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause,
      List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return searchDocumentReferences(parametrizedSqlClause, 0, 0, parameterValues, context);
  }

  /**
   * @deprecated since 2.2M2 use
   *             {@link #searchDocumentReferences(String, List, com.xpn.xwiki.XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context)
      throws XWikiException {
    return searchDocumentsNames(parametrizedSqlClause, 0, 0, parameterValues, context);
  }

  /**
   * @since 2.2M1
   */
  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb,
      int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    String sql = createSQLQuery("select distinct doc.space, doc.name", parametrizedSqlClause);
    return searchDocumentReferencesInternal(sql, nb, start, parameterValues, context);
  }

  /**
   * @deprecated since 2.2M1 use
   *             {@link #searchDocumentReferences(String, int, int, List, XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    String sql = createSQLQuery("select distinct doc.space, doc.name", parametrizedSqlClause);
    return searchDocumentsNamesInternal(sql, nb, start, parameterValues, context);
  }

  /**
   * @since 2.2M2
   */
  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
      throws XWikiException {
    return searchDocumentReferences(wheresql, 0, 0, "", context);
  }

  /**
   * @deprecated since 2.2M1 use {@link #searchDocumentReferences(String, XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, XWikiContext context)
      throws XWikiException {
    return searchDocumentsNames(wheresql, 0, 0, "", context);
  }

  /**
   * @since 2.2M2
   */
  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      XWikiContext context)
      throws XWikiException {
    return searchDocumentReferences(wheresql, nb, start, "", context);
  }

  /**
   * @deprecated since 2.2M1 use {@link #searchDocumentReferences(String, int, int, XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return searchDocumentsNames(wheresql, nb, start, "", context);
  }

  /**
   * @since 2.2M2
   */
  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      String selectColumns,
      XWikiContext context) throws XWikiException {
    String sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
    return searchDocumentReferencesInternal(sql, nb, start, Collections.EMPTY_LIST, context);
  }

  /**
   * @deprecated since 2.2M1 use
   *             {@link #searchDocumentReferences(String, int, int, String, XWikiContext)}
   */
  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
      XWikiContext context) throws XWikiException {
    String sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
    return searchDocumentsNamesInternal(sql, nb, start, Collections.EMPTY_LIST, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public <T> List<T> search(String sql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return search(sql, nb, start, (List) null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int, java.util.List,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public <T> List<T> search(String sql, int nb, int start, List<?> parameterValues,
      XWikiContext context)
      throws XWikiException {
    return search(sql, nb, start, null, parameterValues, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int,
   *      java.lang.Object[][],
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      XWikiContext context)
      throws XWikiException {
    return search(sql, nb, start, whereParams, null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#search(java.lang.String, int, int,
   *      java.lang.Object[][],
   *      java.util.List, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      List<?> parameterValues,
      XWikiContext context)
      throws XWikiException {
    boolean bTransaction = true;

    if (sql == null) {
      return null;
    }

    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }
      checkHibernate(context);
      bTransaction = beginTransaction(false, context);
      Session session = getSession(context);

      if (whereParams != null) {
        sql += generateWhereStatement(whereParams);
      }

      Query query = session.createQuery(filterSQL(sql));

      // Add values for provided HQL request containing "?" characters where to insert real
      // values.
      int parameterId = injectParameterListToQuery(0, query, parameterValues);

      if (whereParams != null) {
        for (Object[] whereParam : whereParams) {
          query.setString(parameterId++, (String) whereParam[1]);
        }
      }

      if (start != 0) {
        query.setFirstResult(start);
      }
      if (nb != 0) {
        query.setMaxResults(nb);
      }
      Iterator it = query.list().iterator();
      List list = new ArrayList();
      while (it.hasNext()) {
        list.add(it.next());
      }
      return list;
    } catch (Exception e) {
      Object[] args = { sql };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
          "Exception while searching documents with sql {0}",
          e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }
  }

  private String generateWhereStatement(Object[][] whereParams) {
    StringBuffer str = new StringBuffer();

    str.append(" where ");
    for (int i = 0; i < whereParams.length; i++) {
      if (i > 0) {
        if ((whereParams[i - 1].length >= 4) && (whereParams[i - 1][3] != "")
            && (whereParams[i - 1][3] != null)) {
          str.append(" ");
          str.append(whereParams[i - 1][3]);
          str.append(" ");
        } else {
          str.append(" and ");
        }
      }
      str.append(whereParams[i][0]);
      if ((whereParams[i].length >= 3) && (whereParams[i][2] != "")
          && (whereParams[i][2] != null)) {
        str.append(" ");
        str.append(whereParams[i][2]);
        str.append(" ");
      } else {
        str.append(" = ");
      }
      str.append(" ?");
    }
    return str.toString();
  }

  public List search(Query query, int nb, int start, XWikiContext context) throws XWikiException {
    boolean bTransaction = true;

    if (query == null) {
      return null;
    }

    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate", query.getQueryString());
      }
      checkHibernate(context);
      bTransaction = beginTransaction(false, context);
      if (start != 0) {
        query.setFirstResult(start);
      }
      if (nb != 0) {
        query.setMaxResults(nb);
      }
      Iterator it = query.list().iterator();
      List list = new ArrayList();
      while (it.hasNext()) {
        list.add(it.next());
      }
      if (bTransaction) {
        // The session is closed here, too.
        endTransaction(context, false, false);
        bTransaction = false;
      }
      return list;
    } catch (Exception e) {
      Object[] args = { query.toString() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
          "Exception while searching documents with sql {0}",
          e, args);
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see XWikiStoreInterface#countDocuments(String, XWikiContext)
   */
  @Override
  public int countDocuments(String wheresql, XWikiContext context) throws XWikiException {
    String sql = createSQLQuery("select count(distinct doc.fullName)", wheresql);
    List l = search(sql, 0, 0, context);
    return ((Number) l.get(0)).intValue();
  }

  /**
   * {@inheritDoc}
   *
   * @see XWikiStoreInterface#countDocuments(String, List, XWikiContext)
   */
  @Override
  public int countDocuments(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context)
      throws XWikiException {
    String sql = createSQLQuery("select count(distinct doc.fullName)", parametrizedSqlClause);
    List l = search(sql, 0, 0, parameterValues, context);
    return ((Number) l.get(0)).intValue();
  }

  /**
   * @deprecated since 2.2M1 used
   *             {@link #searchDocumentReferencesInternal(String, int, int, List, XWikiContext)}
   */
  @Deprecated
  private List<String> searchDocumentsNamesInternal(String sql, int nb, int start,
      List parameterValues,
      XWikiContext context) throws XWikiException {
    List<String> documentNames = new ArrayList<>();
    for (DocumentReference reference : searchDocumentReferencesInternal(sql, nb, start,
        parameterValues, context)) {
      documentNames.add(this.compactWikiEntityReferenceSerializer.serialize(reference));
    }
    return documentNames;
  }

  /**
   * @since 2.2M1
   */
  private List<DocumentReference> searchDocumentReferencesInternal(String sql, int nb, int start,
      List parameterValues, XWikiContext context) throws XWikiException {
    List<DocumentReference> documentReferences = new ArrayList<>();
    for (Object[] result : searchGenericInternal(sql, nb, start, parameterValues, context)) {
      // Construct a reference, using the current wiki as the wiki reference name. This is because
      // the wiki
      // name is not stored in the database for document references.
      DocumentReference reference = new DocumentReference((String) result[1],
          new SpaceReference((String) result[0], new WikiReference(context.getDatabase())));
      documentReferences.add(reference);
    }
    return documentReferences;
  }

  /**
   * @since 2.2M1
   */
  private List<Object[]> searchGenericInternal(String sql, int nb, int start,
      List parameterValues, XWikiContext context) throws XWikiException {
    boolean bTransaction = false;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate", sql);
      }

      checkHibernate(context);
      bTransaction = beginTransaction(false, context);
      Session session = getSession(context);
      Query query = session.createQuery(filterSQL(sql));

      injectParameterListToQuery(0, query, parameterValues);

      if (start != 0) {
        query.setFirstResult(start);
      }
      if (nb != 0) {
        query.setMaxResults(nb);
      }
      Iterator it = query.list().iterator();
      List<Object[]> list = new ArrayList<>();
      while (it.hasNext()) {
        list.add((Object[]) it.next());
      }
      return list;
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
          "Exception while searching documents with SQL [{0}]", e, new Object[] { sql });
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      boolean, boolean, int,
   *      int, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping,
      boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, customMapping, checkRight, nb, start, null,
        context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      boolean, boolean, int,
   *      int, java.util.List, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping,
      boolean checkRight, int nb, int start, List<?> parameterValues, XWikiContext context)
      throws XWikiException {
    // Search documents
    List<Object[]> documentDatas = new ArrayList<>();
    boolean bTransaction = true;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      String sql;
      if (distinctbylanguage) {
        sql = createSQLQuery("select distinct doc.space, doc.name, doc.language", wheresql);
      } else {
        sql = createSQLQuery("select distinct doc.space, doc.name", wheresql);
      }

      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate", sql);
      }

      checkHibernate(context);
      if (bTransaction) {
        // Inject everything until we know what's needed
        SessionFactory sfactory = customMapping ? injectCustomMappingsInSessionFactory(context)
            : getSessionFactory();
        bTransaction = beginTransaction(sfactory, false, context);
      }
      Session session = getSession(context);

      Query query = session.createQuery(filterSQL(sql));

      injectParameterListToQuery(0, query, parameterValues);

      if (start != 0) {
        query.setFirstResult(start);
      }
      if (nb != 0) {
        query.setMaxResults(nb);
      }
      documentDatas.addAll(query.list());
      if (bTransaction) {
        endTransaction(context, false, false);
      }
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SEARCH,
          "Exception while searching documents with SQL [{0}]", e, new Object[] { wheresql });
    } finally {
      try {
        if (bTransaction) {
          endTransaction(context, false, false);
        }
      } catch (Exception e) {}

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }

    // Resolve documents. We use two separated sessions because rights service could need to switch
    // database to
    // check rights
    List<XWikiDocument> documents = new ArrayList<>();
    for (Object[] result : documentDatas) {
      XWikiDocument doc = new XWikiDocument(
          new DocumentReference(context.getDatabase(), (String) result[0], (String) result[1]));
      if (checkRight
          && !context.getWiki().getRightService().checkAccess("view", doc, context)) {
        continue;
      }

      DocumentReference documentReference = doc.getDocumentReference();
      if (distinctbylanguage) {
        String language = (String) result[2];
        XWikiDocument document = context.getWiki().getDocument(documentReference, context);
        if ((language == null) || (language.equals(""))) {
          documents.add(document);
        } else {
          documents.add(document.getTranslatedDocument(language, context));
        }
      } else {
        documents.add(context.getWiki().getDocument(documentReference, context));
      }
    }

    return documents;
  }

  /**
   * @param queryPrefix
   *          the start of the SQL query (for example "select distinct doc.space, doc.name")
   * @param whereSQL
   *          the where clause to append
   * @return the full formed SQL query, to which the order by columns have been added as returned
   *         columns (this is
   *         required for example for HSQLDB).
   */
  protected String createSQLQuery(String queryPrefix, String whereSQL) {
    StringBuffer sql = new StringBuffer(queryPrefix);

    String normalizedWhereSQL;
    if (StringUtils.isBlank(whereSQL)) {
      normalizedWhereSQL = "";
    } else {
      normalizedWhereSQL = whereSQL.trim();
    }

    sql.append(getColumnsForSelectStatement(normalizedWhereSQL));
    sql.append(" from XWikiDocument as doc");

    if (!normalizedWhereSQL.equals("")) {
      if ((!normalizedWhereSQL.startsWith("where")) && (!normalizedWhereSQL.startsWith(","))) {
        sql.append(" where ");
      } else {
        sql.append(" ");
      }
      sql.append(normalizedWhereSQL);
    }

    String result = sql.toString();
    int idx = result.toLowerCase().indexOf("where ");
    if (idx >= 0) {
      // With 'WHERE'
      idx = idx + 6;
      result = result.substring(0, idx) + "(doc.hidden <> true or doc.hidden is null) and "
          + result.substring(idx);
    } else {
      // Without 'WHERE'
      int oidx = Math.min(result.toLowerCase().indexOf("order by "), Integer.MAX_VALUE);
      int gidx = Math.min(result.toLowerCase().indexOf("group by "), Integer.MAX_VALUE);
      idx = Math.min(oidx, gidx);
      if ((idx > 0) && (idx < Integer.MAX_VALUE)) {
        // Without 'WHERE', but with 'ORDER BY' or 'GROUP BY'
        result = result.substring(0, idx) + "where doc.hidden <> true or doc.hidden is null "
            + result.substring(idx);
      } else {
        // Without 'WHERE', 'ORDER BY' or 'GROUP BY'... This should not happen at all.
        result = result + " where (doc.hidden <> true or doc.hidden is null)";
      }
      // TODO: Take into account GROUP BY, HAVING and other keywords when there's no WHERE in the
      // query
    }

    return result;
  }

  /**
   * @param whereSQL
   *          the SQL where clause
   * @return the list of columns to return in the select clause as a string starting with ", " if
   *         there are columns or
   *         an empty string otherwise. The returned columns are extracted from the where clause.
   *         One reason for doing
   *         so is because HSQLDB only support SELECT DISTINCT SQL statements where the columns
   *         operated on are
   *         returned from the query.
   */
  protected String getColumnsForSelectStatement(String whereSQL) {
    StringBuffer columns = new StringBuffer();

    int orderByPos = whereSQL.toLowerCase().indexOf("order by");
    if (orderByPos >= 0) {
      String orderByStatement = whereSQL.substring(orderByPos + "order by".length() + 1);
      StringTokenizer tokenizer = new StringTokenizer(orderByStatement, ",");
      while (tokenizer.hasMoreTokens()) {
        String column = tokenizer.nextToken().trim();
        // Remove "desc" or "asc" from the column found
        column = StringUtils.removeEndIgnoreCase(column, " desc");
        column = StringUtils.removeEndIgnoreCase(column, " asc");
        columns.append(", ").append(column.trim());
      }
    }

    return columns.toString();
  }

  @Override
  public boolean isCustomMappingValid(BaseClass bclass, String custommapping1,
      XWikiContext context) {
    try {
      Configuration hibconfig = makeMapping(bclass.getName(), custommapping1);
      return isValidCustomMapping(bclass.getName(), hibconfig, bclass);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public SessionFactory injectCustomMappingsInSessionFactory(XWikiDocument doc,
      XWikiContext context)
      throws XWikiException {
    // If we haven't turned of dynamic custom mappings we should not inject them
    if (!context.getWiki().hasDynamicCustomMappings()) {
      return getSessionFactory();
    }

    boolean result = injectCustomMappings(doc, context);
    if (!result) {
      return getSessionFactory();
    }

    Configuration config = getConfiguration();
    SessionFactoryImpl sfactory = (SessionFactoryImpl) config.buildSessionFactory();
    Settings settings = sfactory.getSettings();
    ConnectionProvider provider = ((SessionFactoryImpl) getSessionFactory()).getSettings()
        .getConnectionProvider();
    Field field = null;
    try {
      field = settings.getClass().getDeclaredField("connectionProvider");
      field.setAccessible(true);
      field.set(settings, provider);
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED,
          "Mapping injection failed", e);
    }
    return sfactory;
  }

  @Override
  public void injectCustomMappings(XWikiContext context) throws XWikiException {
    SessionFactory sfactory = injectCustomMappingsInSessionFactory(context);
    setSessionFactory(sfactory);
  }

  @Override
  public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException {
    Configuration config = getConfiguration();
    setSessionFactory(injectInSessionFactory(config));
  }

  public SessionFactory injectCustomMappingsInSessionFactory(BaseClass bclass, XWikiContext context)
      throws XWikiException {
    boolean result = injectCustomMapping(bclass, context);
    if (!result) {
      return getSessionFactory();
    }

    Configuration config = getConfiguration();
    return injectInSessionFactory(config);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public SessionFactory injectInSessionFactory(Configuration config) throws XWikiException {
    SessionFactoryImpl sfactory = (SessionFactoryImpl) config.buildSessionFactory();
    Settings settings = sfactory.getSettings();
    ConnectionProvider provider = ((SessionFactoryImpl) getSessionFactory()).getSettings()
        .getConnectionProvider();
    Field field = null;
    try {
      field = settings.getClass().getDeclaredField("connectionProvider");
      field.setAccessible(true);
      field.set(settings, provider);
    } catch (Exception e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_MAPPING_INJECTION_FAILED,
          "Mapping injection failed", e);
    }
    return sfactory;
  }

  public SessionFactory injectCustomMappingsInSessionFactory(XWikiContext context)
      throws XWikiException {
    // If we haven't turned of dynamic custom mappings we should not inject them
    if (!context.getWiki().hasDynamicCustomMappings()) {
      return getSessionFactory();
    }

    List list;
    if (useClassesTable(true, context)) {
      list = searchDocuments(
          ", BaseClass as bclass where bclass.name=doc.fullName and bclass.customMapping is not null",
          true,
          false, false, 0, 0, context);
    }
    list = searchDocuments("", true, false, false, 0, 0, context);
    boolean result = false;

    for (Object element : list) {
      XWikiDocument doc = (XWikiDocument) element;
      if (doc.getXClass().getFieldList().size() > 0) {
        result |= injectCustomMapping(doc.getXClass(), context);
      }
    }

    if (!result) {
      return getSessionFactory();
    }

    Configuration config = getConfiguration();
    return injectInSessionFactory(config);
  }

  @Override
  public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    // If we haven't turned of dynamic custom mappings we should not inject them
    if (!context.getWiki().hasDynamicCustomMappings()) {
      return false;
    }

    boolean result = false;
    for (List<BaseObject> objectsOfType : doc.getXObjects().values()) {
      for (BaseObject object : objectsOfType) {
        if (object != null) {
          result |= injectCustomMapping(object.getXClass(context), context);
          // Each class must be mapped only once
          break;
        }
      }
    }
    return result;
  }

  @Override
  public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context)
      throws XWikiException {
    // If we haven't turned of dynamic custom mappings we should not inject them
    if (!context.getWiki().hasDynamicCustomMappings()) {
      return false;
    }

    String custommapping = doc1class.getCustomMapping();
    if (!doc1class.hasExternalCustomMapping()) {
      return false;
    }

    Configuration config = getConfiguration();

    // don't add a mapping that's already there
    if (config.getClassMapping(doc1class.getName()) != null) {
      return true;
    }

    Configuration mapconfig = makeMapping(doc1class.getName(), custommapping);
    if (!isValidCustomMapping(doc1class.getName(), mapconfig, doc1class)) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_INVALID_MAPPING, "Invalid Custom Mapping");
    }

    config.addXML(makeMapping(doc1class.getName(),
        "xwikicustom_" + doc1class.getName().replaceAll("\\.", "_"),
        custommapping));
    config.buildMappings();
    return true;
  }

  private boolean isValidCustomMapping(String className, Configuration hibconfig,
      BaseClass bclass) {
    PersistentClass mapping = hibconfig.getClassMapping(className);
    if (mapping == null) {
      return true;
    }

    Iterator it = mapping.getPropertyIterator();
    while (it.hasNext()) {
      Property hibprop = (Property) it.next();
      String propname = hibprop.getName();
      PropertyClass propclass = (PropertyClass) bclass.getField(propname);
      if (propclass == null) {
        log.warn("Mapping contains invalid field name " + propname);
        return false;
      }

      boolean result = isValidColumnType(hibprop.getValue().getType().getName(),
          propclass.getClassName());
      if (!result) {
        log.warn("Mapping contains invalid type in field " + propname);
        return false;
      }
    }

    return true;
  }

  @Override
  public List getCustomMappingPropertyList(BaseClass bclass) {
    List list = new ArrayList();
    Configuration hibconfig;
    if (bclass.hasExternalCustomMapping()) {
      hibconfig = makeMapping(bclass.getName(), bclass.getCustomMapping());
    } else {
      hibconfig = getConfiguration();
    }
    PersistentClass mapping = hibconfig.getClassMapping(bclass.getName());
    if (mapping == null) {
      return null;
    }

    Iterator it = mapping.getPropertyIterator();
    while (it.hasNext()) {
      Property hibprop = (Property) it.next();
      String propname = hibprop.getName();
      list.add(propname);
    }
    return list;
  }

  private boolean isValidColumnType(String name, String className) {
    String[] validtypes = this.validTypesMap.get(className);
    if (validtypes == null) {
      return true;
    } else {
      return ArrayUtils.contains(validtypes, name);
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public XWikiBatcherStats getBatcherStats() {
    return null; // XWikiBatcher.getSQLStats();
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Deprecated
  public void resetBatcherStats() {
    // XWikiBatcher.getSQLStats().resetStats();
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context)
      throws XWikiException {
    return searchDocuments(wheresql, null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, java.util.List,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, List<?> parameterValues,
      XWikiContext context)
      throws XWikiException {
    return searchDocuments(wheresql, 0, 0, parameterValues, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      XWikiContext context)
      throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, 0, 0, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      boolean,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping,
      XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, customMapping, 0, 0, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      XWikiContext context)
      throws XWikiException {
    return searchDocuments(wheresql, nb, start, null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, int, int,
   *      java.util.List,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, true, nb, start, parameterValues, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int,
   *      int, java.util.List,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb,
      int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, false, nb, start, parameterValues,
        context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean, int,
   *      int,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb,
      int start,
      XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, nb, start, null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      boolean, int, int,
   *      com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping,
      int nb, int start, XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start, null, context);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocuments(java.lang.String, boolean,
   *      boolean, int, int,
   *      java.util.List, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping,
      int nb, int start, List<?> parameterValues, XWikiContext context) throws XWikiException {
    return searchDocuments(wheresql, distinctbylanguage, customMapping, true, nb, start,
        parameterValues, context);
  }

  @Override
  public List<String> getTranslationList(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    String hql = "select doc.language from XWikiDocument as doc where doc.space = ? and doc.name = ? "
        + "and (doc.language <> '' or (doc.language is not null and '' is null))";
    ArrayList<String> params = new ArrayList<>();
    params.add(doc.getSpace());
    params.add(doc.getName());
    List<String> list = search(hql, 0, 0, params, context);
    return (list == null) ? new ArrayList<>() : list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public QueryManager getQueryManager() {
    return this.queryManager;
  }

  /**
   * This is in response to the fact that Hibernate interprets backslashes differently from the
   * database.
   * Our solution is to simply replace all instances of \ with \\ which makes the first backslash
   * escape the second.
   *
   * @param sql
   *          the uncleaned sql.
   * @return same as sql except it is guarenteed not to contain groups of odd numbers of
   *         backslashes.
   * @since 2.4M1
   */
  private String filterSQL(String sql) {
    return StringUtils.replace(sql, "\\", "\\\\");
  }
}
