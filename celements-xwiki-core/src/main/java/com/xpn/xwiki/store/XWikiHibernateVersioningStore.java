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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Realization of {@link XWikiVersioningStoreInterface} for Hibernate-based storage.
 *
 * @version $Id$
 */
@Component
public class XWikiHibernateVersioningStore extends XWikiHibernateBaseStore
    implements XWikiVersioningStoreInterface {

  /** Logger. */
  private static final Log LOG = LogFactory.getLog(XWikiHibernateVersioningStore.class);

  /** Colon symbol. */
  private static final String COLON = ":";

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateVersioningStore() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    try {
      XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
      if (archive == null) {
        return new Version[0];
      }
      Collection<XWikiRCSNodeInfo> nodes = archive.getNodes();
      Version[] versions = new Version[nodes.size()];
      Iterator<XWikiRCSNodeInfo> it = nodes.iterator();
      for (int i = 0; i < versions.length; i++) {
        XWikiRCSNodeInfo node = it.next();
        versions[versions.length - 1 - i] = node.getId().getVersion();
      }
      return versions;
    } catch (Exception e) {
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_REVISIONS,
          "Exception while reading document {0} revisions", e, args);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    XWikiDocumentArchive archiveDoc = doc.getDocumentArchive();
    if (archiveDoc != null) {
      return archiveDoc;
    }
    String key = ((doc.getDatabase() == null) ? "xwiki" : doc.getDatabase()) + COLON
        + doc.getFullName();
    if (!"".equals(doc.getLanguage())) {
      key = key + COLON + doc.getLanguage();
    }

    synchronized (key) {
      archiveDoc = context.getDocumentArchive(key);
      if (archiveDoc == null) {
        String db = context.getDatabase();
        try {
          if (doc.getDatabase() != null) {
            context.setDatabase(doc.getDatabase());
          }
          archiveDoc = new XWikiDocumentArchive(doc.getId());
          loadXWikiDocArchive(archiveDoc, true, context);
          doc.setDocumentArchive(archiveDoc);
        } finally {
          context.setDatabase(db);
        }
        // This will also make sure that the Archive has a strong reference
        // and will not be discarded as long as the context exists.
        // FIXME this leaks out potentially cached instances for manipulation
        context.addDocumentArchive(key, archiveDoc);
      }
      return archiveDoc;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction,
      XWikiContext context)
      throws XWikiException {
    try {
      List<XWikiRCSNodeInfo> nodes = loadAllRCSNodeInfo(context, archivedoc.getId(), bTransaction);
      archivedoc.setNodes(nodes);
    } catch (Exception e) {
      Object[] args = { new Long(archivedoc.getId()) };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT,
          "Exception while loading archive {0}", e,
          args);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveXWikiDocArchive(final XWikiDocumentArchive archivedoc, boolean bTransaction,
      XWikiContext context)
      throws XWikiException {
    executeWrite(context, bTransaction, session -> {
      for (XWikiRCSNodeInfo ni1 : archivedoc.getDeletedNodeInfo()) {
        session.delete(ni1);
      }
      archivedoc.getDeletedNodeInfo().clear();
      for (XWikiRCSNodeInfo ni2 : archivedoc.getUpdatedNodeInfos()) {
        session.saveOrUpdate(ni2);
      }
      archivedoc.getUpdatedNodeInfos().clear();
      for (XWikiRCSNodeContent nc : archivedoc.getUpdatedNodeContents()) {
        session.update(nc);
      }
      archivedoc.getUpdatedNodeContents().clear();
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument basedoc, String sversion, XWikiContext context)
      throws XWikiException {
    XWikiDocumentArchive archive = getXWikiDocumentArchive(basedoc, context);
    Version version = new Version(sversion);

    XWikiDocument doc = archive.loadDocument(version, context);
    if (doc == null) {
      Object[] args = { basedoc.getFullName(), version.toString() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
          "Version {1} does not exist while reading document {0}", null, args);
    }
    // Make sure the document has the same name
    // as the new document (in case there was a name change
    doc.setName(basedoc.getName());
    doc.setSpace(basedoc.getSpace());

    doc.setDatabase(basedoc.getDatabase());
    return doc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void resetRCSArchive(final XWikiDocument doc, boolean bTransaction,
      final XWikiContext context)
      throws XWikiException {
    executeWrite(context, true, session -> {
      XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);
      archive.resetArchive();
      archive.getDeletedNodeInfo().clear();
      doc.setMinorEdit(false);
      deleteArchive(doc, false, context);
      updateXWikiDocArchive(doc, false, context);
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context)
      throws XWikiException {
    try {
      XWikiDocumentArchive archiveDoc = getXWikiDocumentArchive(doc, context);
      archiveDoc.updateArchive(doc, doc.getAuthor(), doc.getDate(), doc.getComment(),
          doc.getRCSVersion(),
          context);
      doc.setRCSVersion(archiveDoc.getLatestVersion());
      saveXWikiDocArchive(archiveDoc, bTransaction, context);
    } catch (Exception e) {
      Object[] args = { doc.getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT,
          "Exception while updating archive {0}", e,
          args);
    }
  }

  /**
   * {@inheritDoc}
   */
  protected List<XWikiRCSNodeInfo> loadAllRCSNodeInfo(XWikiContext context, final long id,
      boolean bTransaction)
      throws XWikiException {
    return executeRead(context, bTransaction, new HibernateCallback<List<XWikiRCSNodeInfo>>() {

      @Override
      @SuppressWarnings("unchecked")
      public List<XWikiRCSNodeInfo> doInHibernate(Session session) throws HibernateException {
        try {
          return session.createCriteria(XWikiRCSNodeInfo.class).add(
              Restrictions.eq("id.docId", Long.valueOf(id))).add(Restrictions.isNotNull("diff"))
              .list();
        } catch (IllegalArgumentException ex) {
          // This happens when the database has wrong values...
          LOG.warn("Invalid history for document " + id);
          return Collections.emptyList();
        }
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiRCSNodeContent loadRCSNodeContent(final XWikiRCSNodeId id, boolean bTransaction,
      XWikiContext context)
      throws XWikiException {
    return executeRead(context, bTransaction, session -> {
      XWikiRCSNodeContent content = new XWikiRCSNodeContent(id);
      session.load(content, content.getId());
      return content;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteArchive(final XWikiDocument doc, boolean bTransaction, XWikiContext context)
      throws XWikiException {
    executeWrite(context, bTransaction, session -> {
      session.createQuery("delete from " + XWikiRCSNodeInfo.class.getName() + " where id.docId=?")
          .setLong(0,
              doc.getId())
          .executeUpdate();
      return null;
    });
  }
}
