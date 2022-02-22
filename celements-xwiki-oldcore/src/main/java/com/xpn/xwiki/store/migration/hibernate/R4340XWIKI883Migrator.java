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
package com.xpn.xwiki.store.migration.hibernate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-883: global access preferences cannot be updated
 *
 * @version $Id$
 */
public class R4340XWIKI883Migrator extends AbstractXWikiHibernateMigrator {

  /**
   * {@inheritDoc}
   *
   * @see AbstractXWikiHibernateMigrator#getName()
   */
  @Override
  public String getName() {
    return "R4340XWIKI883";
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.store.migration.hibernate.AbstractXWikiHibernateMigrator#getDescription()
   */
  @Override
  public String getDescription() {
    return "See http://jira.xwiki.org/jira/browse/XWIKI-883";
  }

  /** {@inheritDoc} */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(4340);
  }

  /** {@inheritDoc} */
  @Override
  public void migrate(XWikiHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    manager.getStore(context).executeWrite(context, true, session -> {
      Query q = session.createQuery(
          "select s from BaseObject o, StringProperty s where o.className like 'XWiki.XWiki%Rights' and o.id=s.id and (s.name='users' or s.name='groups')");
      List lst = q.list();
      if (lst.size() == 0) {
        return null;
      }
      List lst2 = new ArrayList(lst.size());
      for (Iterator it1 = lst.iterator(); it1.hasNext();) {
        StringProperty sp = (StringProperty) it1.next();
        LargeStringProperty lsp = new LargeStringProperty();
        lsp.setId(sp.getId());
        lsp.setName(sp.getName());
        lsp.setValue(sp.getValue());
        lst2.add(lsp);
      }
      for (Iterator it2 = lst.iterator(); it2.hasNext();) {
        session.delete(it2.next());
      }
      for (Iterator it3 = lst2.iterator(); it3.hasNext();) {
        session.save(it3.next());
      }
      return null;
    });
  }
}
