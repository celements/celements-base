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

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Realization of {@link XWikiRecycleBinStoreInterface} for Hibernate store.
 *
 * @version $Id$
 */
@Component
public class XWikiHibernateRecycleBinStore extends XWikiHibernateBaseStore
    implements XWikiRecycleBinStoreInterface {

  /**
   * Empty constructor needed for component manager.
   */
  public XWikiHibernateRecycleBinStore() {}

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveToRecycleBin(XWikiDocument doc, String deleter, Date date, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    final XWikiDeletedDocument trashdoc = new XWikiDeletedDocument(doc, deleter, date, context);
    executeWrite(context, bTransaction, session -> {
      session.save(trashdoc);
      return null;
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDocument restoreFromRecycleBin(final XWikiDocument doc, final long index,
      final XWikiContext context, boolean bTransaction) throws XWikiException {
    return executeRead(context, bTransaction, session -> {
      XWikiDeletedDocument trashdoc = (XWikiDeletedDocument) session
          .load(XWikiDeletedDocument.class, Long.valueOf(index));
      return trashdoc.restoreDocument(null, context);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDeletedDocument getDeletedDocument(XWikiDocument doc, final long index,
      XWikiContext context, boolean bTransaction) throws XWikiException {
    return executeRead(context, bTransaction, session -> (XWikiDeletedDocument) session
        .get(XWikiDeletedDocument.class, Long.valueOf(index)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDeletedDocument[] getAllDeletedDocuments(final XWikiDocument doc,
      XWikiContext context, boolean bTransaction) throws XWikiException {
    return executeRead(context, bTransaction, session -> {
      Criteria c = session.createCriteria(XWikiDeletedDocument.class);
      c.add(Restrictions.eq("fullName", doc.getFullName()));
      c.add(Restrictions.eq("language", doc.getLanguage()));
      c.addOrder(Order.desc("date"));
      @SuppressWarnings("unchecked")
      List<XWikiDeletedDocument> deletedVersions = c.list();
      XWikiDeletedDocument[] result = new XWikiDeletedDocument[deletedVersions.size()];
      return deletedVersions.toArray(result);
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteFromRecycleBin(XWikiDocument doc, final long index, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    executeWrite(context, bTransaction, session -> {
      session.createQuery("delete from " + XWikiDeletedDocument.class.getName() + " where id=?")
          .setLong(0, index)
          .executeUpdate();
      return null;
    });
  }
}
