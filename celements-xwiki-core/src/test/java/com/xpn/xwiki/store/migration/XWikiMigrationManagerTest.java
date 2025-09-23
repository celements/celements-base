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
package com.xpn.xwiki.store.migration;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.celements.init.XWikiProvider;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.AbstractXWikiMigrationManager.XWikiMigration;
import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Test for {@link AbstractXWikiMigrationManager}
 */
public class XWikiMigrationManagerTest extends AbstractComponentTest {

  XWikiConfig config;

  @Before
  public void prepare() throws Exception {
    registerComponentMocks(WikiService.class, XWikiProvider.class);
    var xwiki = new XWiki(false);
    xwiki.setConfig(config = new XWikiConfig());
    getContext().setWiki(xwiki);
  }

  /** mocked migration manager */
  private static class TestMigrationManager extends AbstractXWikiMigrationManager {

    public TestMigrationManager(XWikiContext context) throws Exception {
      super(context);
    }

    private XWikiMigratorInterface createMigrator(final int ver) {
      return new XWikiMigratorInterface() {

        @Override
        public String getName() {
          return "Test";
        }

        @Override
        public String getDescription() {
          return "Test";
        }

        @Override
        public XWikiDBVersion getVersion() {
          return new XWikiDBVersion(ver);
        }

        @Override
        public boolean shouldExecute(XWikiDBVersion startupVersion) {
          return true;
        }

        @Override
        public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
            throws XWikiException {}
      };
    }

    @Override
    protected List<XWikiMigratorInterface> getAllMigrations(XWikiContext context)
        throws XWikiException {
      List<XWikiMigratorInterface> lst = new ArrayList<>();
      lst.add(createMigrator(345));
      lst.add(createMigrator(123));
      lst.add(createMigrator(456));
      lst.add(createMigrator(234));
      return lst;
    }

    XWikiDBVersion curversion;

    @Override
    protected void setDBVersion(XWikiDBVersion version, XWikiContext context)
        throws XWikiException {
      this.curversion = version;
    }
  }

  @Test
  public void test_getNeededMigrations_noVersion() throws Exception {
    expect(getMock(WikiService.class).streamAllWikis())
        .andReturn(Stream.of(XWikiConstant.MAIN_WIKI))
        .anyTimes();

    replayDefault();
    TestMigrationManager mm = new TestMigrationManager(getContext());
    Collection<XWikiMigration> neededMigration = mm.getNeededMigrations(getContext());
    // mm.startMigrations(getContext());
    verifyDefault();

    assertEquals(4, neededMigration.size());
    // assertEquals(457, mm.curversion.getVersion());
  }

  @Test
  public void test_getNeededMigrations_orderAndIgnore() throws Exception {
    config.setProperty("xwiki.store.migration.version", "234");
    config.setProperty("xwiki.store.migration.ignored", "345");

    replayDefault();
    TestMigrationManager mm = new TestMigrationManager(getContext());
    Collection<XWikiMigration> neededMigration = mm.getNeededMigrations(getContext());
    verifyDefault();

    assertEquals(2, neededMigration.size());
    var iter = neededMigration.iterator();
    assertEquals(234, iter.next().migrator.getVersion().getVersion());
    assertEquals(456, iter.next().migrator.getVersion().getVersion());
  }

  public static class TestForceMigratior implements XWikiMigratorInterface {

    @Override
    public String getName() {
      return "Test";
    }

    @Override
    public String getDescription() {
      return "Test";
    }

    @Override
    public XWikiDBVersion getVersion() {
      return new XWikiDBVersion(567);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion) {
      return true;
    }

    @Override
    public void migrate(XWikiMigrationManagerInterface manager, XWikiContext context)
        throws XWikiException {}
  }

  @Test
  public void test_getNeededMigrations_Force() throws Exception {
    config.setProperty("xwiki.store.migration.version", "234");
    config.setProperty("xwiki.store.migration.force", TestForceMigratior.class.getName());

    replayDefault();
    TestMigrationManager mm = new TestMigrationManager(getContext());
    Collection<XWikiMigration> neededMigration = mm.getNeededMigrations(getContext());
    verifyDefault();

    assertEquals(1, neededMigration.size());
    var iter = neededMigration.iterator();
    assertEquals(567, iter.next().migrator.getVersion().getVersion());
  }
}
