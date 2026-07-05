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

import static com.celements.spring.context.SpringContextProvider.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.execution.XWikiExecutionProp;
import com.celements.init.update.WikiUpdater;
import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Template for {@link XWikiMigrationManagerInterface}.
 *
 * @version $Id$
 */
public abstract class AbstractXWikiMigrationManager implements XWikiMigrationManagerInterface {

  /** logger. */
  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  /**
   * Internal class used to find out the migrators that are being forced in the XWiki configuration
   * file.
   */
  protected class XWikiMigration {

    public boolean isForced;
    public XWikiMigratorInterface migrator;

    public XWikiMigration(XWikiMigratorInterface migrator, boolean isForced) {
      this.migrator = migrator;
      this.isForced = isForced;
    }
  }

  /**
   * The database version when the migration process starts (before any migrator is applied).
   * This is useful for mirgator which need to run only when the database is in a certain version.
   */
  private XWikiDBVersion startupVersion;

  /**
   * Unified constructor for all subclasses.
   *
   * @param context
   *          - used everywhere
   */
  protected AbstractXWikiMigrationManager(XWikiContext context) throws XWikiException {
    this.startupVersion = getDBVersion(context);
  }

  protected final XWikiContext getXContext() {
    return getExecution().getContext().get(XWikiExecutionProp.XWIKI_CONTEXT)
        .orElseThrow(IllegalStateException::new);
  }

  protected final Execution getExecution() {
    return getSpringContext().getBean(Execution.class);
  }

  protected final WikiService getWikiService() {
    return getSpringContext().getBean(WikiService.class);
  }

  protected final WikiUpdater getWikiUpdater() {
    return getSpringContext().getBean(WikiUpdater.class);
  }

  /**
   * read data version from xwiki.cfg.
   *
   * @param context
   *          used for read config
   * @return data version if set, or null.
   */
  protected XWikiDBVersion getDBVersionFromConfig(XWikiContext context) {
    String ver = context.getWiki().getConfig().getProperty("xwiki.store.migration.version");
    return ver == null ? null : new XWikiDBVersion(Integer.parseInt(ver));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public XWikiDBVersion getDBVersion(XWikiContext context) throws XWikiException {
    XWikiDBVersion result = getDBVersionFromConfig(context);
    return result == null ? new XWikiDBVersion(0) : result;
  }

  /**
   * @param version
   *          to set
   * @param context
   *          used everywhere
   * @throws XWikiException
   *           if any error
   */
  protected abstract void setDBVersion(XWikiDBVersion version, XWikiContext context)
      throws XWikiException;

  /**
   * {@inheritDoc}
   */
  @Override
  public void startMigrations(XWikiContext context) throws XWikiException {
    getWikiService().streamAllWikis().forEach(this::startMigrations);
  }

  /**
   * @return true if the database should be migrated. This is controlled through the
   *         "xwiki.store.migration.databases" configuration property in xwiki.cfg. A value of "all"
   *         will
   *         always return true. The main database will always return true.
   */
  private boolean shouldMigrate(String database) {
    if (XWikiConstant.MAIN_WIKI.getName().equals(database)) {
      return true;
    }
    var databases = new HashSet<>(Arrays.asList(getXContext().getWiki().getConfig()
        .getPropertyAsList("xwiki.store.migration.databases")));
    return databases.contains(database) || databases.contains("all") || databases.contains("ALL");
  }

  private CompletableFuture<Void> startMigrations(WikiReference wiki) {
    if (!shouldMigrate(wiki.getName())) {
      LOG.info("Skipping migration for wiki [{}]", wiki);
      return CompletableFuture.completedFuture(null);
    }
    return getWikiUpdater().runUpdateAsyncExc(wiki, () -> {
      try {
        LOG.info("Starting migration for wiki [{}]...", wiki);
        startMigrations(getNeededMigrations(getXContext()), getXContext());
      } catch (Exception e) {
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_MIGRATION, "Migration failed", e);
      }
    });
  }

  /**
   * @return collection of {@link XWikiMigratorInterface} in ascending order, which need be
   *         executed.
   * @param context
   *          used everywhere
   * @throws Exception
   *           if any error
   */
  protected Collection<XWikiMigration> getNeededMigrations(XWikiContext context) throws Exception {
    XWikiDBVersion curversion = getDBVersion(context);
    var neededMigrations = new TreeMap<XWikiDBVersion, XWikiMigration>();
    var forcedMigrations = getForcedMigrations(context);
    if (!forcedMigrations.isEmpty()) {
      neededMigrations.putAll(forcedMigrations);
    } else {
      var ignoredMigrations = new HashSet<>(Arrays.asList(context.getWiki().getConfig()
          .getPropertyAsList("xwiki.store.migration.ignored")));
      for (XWikiMigratorInterface migrator : getAllMigrations(context)) {
        if (ignoredMigrations.contains(migrator.getClass().getName())
            || ignoredMigrations.contains(migrator.getVersion().toString())) {
          continue;
        }
        if (migrator.getVersion().compareTo(curversion) >= 0) {
          XWikiMigration migration = new XWikiMigration(migrator, false);
          neededMigrations.put(migrator.getVersion(), migration);
        }
      }
    }

    Collection<XWikiMigration> neededMigrationsAsCollection = neededMigrations.values();
    if (LOG.isInfoEnabled()) {
      if (!neededMigrations.isEmpty()) {
        LOG.info("Current storage version = [{}]", curversion);
        LOG.info("List of migrations that will be executed:");
        for (XWikiMigration migration : neededMigrationsAsCollection) {
          if (migration.isForced || migration.migrator.shouldExecute(this.startupVersion)) {
            LOG.info("  {} - {} {}", migration.migrator.getName(),
                migration.migrator.getDescription(), migration.isForced ? "(forced)" : "");
          }
        }
      } else {
        LOG.info("No storage migration required since current version is [{}]", curversion);
      }
    }

    return neededMigrationsAsCollection;
  }

  protected Map<XWikiDBVersion, XWikiMigration> getForcedMigrations(XWikiContext context)
      throws Exception {
    var forcedMigrations = new TreeMap<XWikiDBVersion, XWikiMigration>();
    String[] forcedMigrationsArray = context.getWiki().getConfig()
        .getPropertyAsList("xwiki.store.migration.force");
    for (String element : forcedMigrationsArray) {
      XWikiMigratorInterface migrator = (XWikiMigratorInterface) Class
          .forName(element).newInstance();
      XWikiMigration migration = new XWikiMigration(migrator, true);
      forcedMigrations.put(migrator.getVersion(), migration);
    }
    return forcedMigrations;
  }

  /**
   * @param migrations
   *          - run this migrations in order of collection
   * @param context
   *          - used everywhere
   * @throws XWikiException
   *           if any error
   */
  protected void startMigrations(Collection<XWikiMigration> migrations,
      XWikiContext context) throws Exception {
    XWikiDBVersion curversion = getDBVersion(context);
    for (XWikiMigration migration : migrations) {
      if (migration.isForced || migration.migrator.shouldExecute(this.startupVersion)) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Running migration [{}] with version [{}]",
              migration.migrator.getName(), migration.migrator.getVersion());
        }
        migration.migrator.migrate(this, context);
      } else {
        LOG.info("Skipping unneeded migration [{}] with version [{}]",
            migration.migrator.getName(), migration.migrator.getVersion());
      }
      if (migration.migrator.getVersion().compareTo(curversion) > 0) {
        setDBVersion(migration.migrator.getVersion().increment(), context);
        LOG.info("New storage version is now [{}]", getDBVersion(context));
      }
    }
  }

  /**
   * @param context
   *          used everywhere
   * @return List of all {@link XWikiMigratorInterface} for this manager
   * @throws XWikiException
   *           if any error
   */
  protected abstract List<? extends XWikiMigratorInterface> getAllMigrations(XWikiContext context)
      throws XWikiException;

}
