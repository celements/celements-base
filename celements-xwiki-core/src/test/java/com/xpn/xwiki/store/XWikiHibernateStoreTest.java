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

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.Transaction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.web.Utils;

/**
 * Unit tests for the {@link XWikiHibernateStore} class.
 *
 * @version $Id$
 */
public class XWikiHibernateStoreTest extends AbstractComponentTest {

  XWikiHibernateStore store;

  @Before
  public void setUp() {
    store = Utils.getComponent(XWikiHibernateStore.class);
    store.setPath("whatever");
  }

  @Test
  public void testGetColumnsForSelectStatement() {
    Assert.assertEquals(", doc.date",
        store.getColumnsForSelectStatement("where 1=1 order by doc.date desc"));
    Assert.assertEquals(", doc.date",
        store.getColumnsForSelectStatement("where 1=1 order by doc.date asc"));
    Assert.assertEquals(", doc.date",
        store.getColumnsForSelectStatement("where 1=1 order by doc.date"));
    Assert.assertEquals(", description",
        store.getColumnsForSelectStatement("where 1=1 order by description desc"));
    Assert.assertEquals(", ascendent",
        store.getColumnsForSelectStatement("where 1=1 order by ascendent asc"));
    Assert.assertEquals(", doc.date, doc.name",
        store.getColumnsForSelectStatement("where 1=1 order by doc.date, doc.name"));
    Assert.assertEquals(", doc.date, doc.name",
        store.getColumnsForSelectStatement("where 1=1 order by doc.date ASC, doc.name DESC"));
    Assert.assertEquals("",
        store.getColumnsForSelectStatement(", BaseObject as obj where obj.name=doc.fullName"));
  }

  @Test
  public void testCreateSQLQuery() {
    Assert.assertEquals(
        "select distinct doc.space, doc.name from XWikiDocument as doc where (doc.hidden <> true or doc.hidden is null)",
        store.createSQLQuery("select distinct doc.space, doc.name", ""));
    Assert.assertEquals("select distinct doc.space, doc.name, doc.date from XWikiDocument as doc "
        + "where (doc.hidden <> true or doc.hidden is null) and 1=1 order by doc.date desc",
        store.createSQLQuery(
            "select distinct doc.space, doc.name", "where 1=1 order by doc.date desc"));
  }

  @Test
  public void testEndTransactionWhenSQLBatchUpdateExceptionThrown() throws Exception {
    final Transaction mockTransaction = createMock(Transaction.class);

    SQLException sqlException2 = new SQLException("sqlexception2");
    sqlException2.setNextException(new SQLException("nextexception2"));

    final SQLException sqlException1 = new SQLException("sqlexception1");
    sqlException1.initCause(sqlException2);
    sqlException1.setNextException(new SQLException("nextexception1"));

    mockTransaction.commit();
    expectLastCall().andThrow(new HibernateException("exception1", sqlException1));

    store.setTransaction(mockTransaction, getContext());

    try {
      replayDefault(mockTransaction);
      store.endTransaction(getContext(), true);
      verifyDefault(mockTransaction);
      Assert.fail("Should have thrown an exception here");
    } catch (HibernateException e) {
      Assert.assertEquals("Failed to commit or rollback transaction. Root cause [\n"
          + "SQL next exception = [java.sql.SQLException: nextexception1]\n"
          + "SQL next exception = [java.sql.SQLException: nextexception2]]", e.getMessage());
    }
  }

  @Test
  public void test_getSchemaFromWikiName_virtual() {
    getXWikiCfg().setProperty("xwiki.db.prefix", "pref_");
    replayDefault();
    assertNull(store.getSchemaFromWikiName(null, getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("as5df", getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("AS5DF", getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("a$s5(DF)", getContext()));
    assertEquals("pref_as5df_suf", store.getSchemaFromWikiName("AS5DF-SUF", getContext()));
    verifyDefault();
  }

  @Test
  public void test_getSchemaFromWikiName_main() {
    getXWikiCfg().setProperty("xwiki.db", "main");
    getXWikiCfg().setProperty("xwiki.db.prefix", "pref_");
    replayDefault();
    assertNull(store.getSchemaFromWikiName(null, getContext()));
    assertEquals("pref_main",
        store.getSchemaFromWikiName(XWikiConstant.MAIN_WIKI.getName(), getContext()));
    verifyDefault();
  }

}
