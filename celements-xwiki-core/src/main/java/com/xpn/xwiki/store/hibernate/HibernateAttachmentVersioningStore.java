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
package com.xpn.xwiki.store.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore;

/**
 * Realization of {@link AttachmentVersioningStore} for Hibernate-based storage.
 *
 * @version $Id$
 * @since 1.4M2
 */
@Component
public class HibernateAttachmentVersioningStore extends XWikiHibernateBaseStore
    implements AttachmentVersioningStore {

  /** logger. */
  private static final Log LOG = LogFactory.getLog(HibernateAttachmentVersioningStore.class);

  /**
   * Empty constructor needed for component manager.
   */
  public HibernateAttachmentVersioningStore() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiAttachmentArchive loadArchive(final XWikiAttachment attachment, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    try {
      final XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
      archive.setAttachment(attachment);
      executeRead(context, bTransaction, session -> {
        try {
          session.load(archive, archive.getId());
        } catch (ObjectNotFoundException e) {
          // if none found then return empty created archive
        }
        return null;
      });
      attachment.setAttachment_archive(archive);
      return archive;
    } catch (Exception e) {
      Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_LOADING_ATTACHMENT,
          "Exception while loading attachment archive {0} of document {1}", e, args);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveArchive(final XWikiAttachmentArchive archive, XWikiContext context,
      boolean bTransaction)
      throws XWikiException {
    executeWrite(context, bTransaction, session -> {
      session.saveOrUpdate(archive);
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteArchive(final XWikiAttachment attachment, final XWikiContext context,
      boolean bTransaction)
      throws XWikiException {
    try {
      executeWrite(context, bTransaction, session -> {
        XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
        archive.setAttachment(attachment);
        session.delete(archive);
        return null;
      });
    } catch (Exception e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(String.format("Error deleting attachment archive [%s] of doc [%s]",
            attachment.getFilename(),
            attachment.getDoc().getFullName()), e);
      }
    }
  }
}
