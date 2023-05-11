package com.celements.model.migration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.LongType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(BaseCollectionIdColumnMigration.NAME)
public class BaseCollectionIdColumnMigration extends AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      BaseCollectionIdColumnMigration.class);

  public static final String NAME = "BaseCollectionIdColumnMigration";

  static final Set<String> XWIKI_TABLES = ImmutableSet.of("xwikiobjects", "xwikiproperties",
      "xwikiintegers", "xwikilongs", "xwikifloats", "xwikidoubles", "xwikistrings", "xwikidates",
      "xwikilargestrings", "xwikilists", "xwikilistitems", "xwikiclasses", "xwikiclassesprop",
      "xwikinumberclasses", "xwikibooleanclasses", "xwikistringclasses", "xwikidateclasses",
      "xwikislistclasses", "xwikidblistclasses");

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  private InformationSchema informationSchema;

  private Set<String> migratedTables = new HashSet<>();

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "migrates id columns for BaseCollections from int to bigint";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * 14.04.2018 -> 3025 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(3025);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext xwikiContext)
      throws XWikiException {
    LOGGER.info("migrating id columns for database [{}]", getDatabaseWithPrefix());
    try {
      migrateXWikiTables();
      migrateMappedTables();
    } catch (Exception exc) {
      LOGGER.error("Failed to migrate id columns for database [{}]", getDatabaseWithPrefix(), exc);
      throw exc;
    } finally {
      clearMigrationData();
    }
  }

  private void migrateXWikiTables() throws XWikiException {
    for (String table : XWIKI_TABLES) {
      LOGGER.debug("[{}] try id column migration for xwiki table", table);
      migrateTable(table);
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateMappedTables() throws XWikiException {
    for (Iterator<PersistentClass> iter = getHibConfig().getClassMappings(); iter.hasNext();) {
      PersistentClass mapping = iter.next();
      String table = mapping.getTable().getName();
      if (!XWIKI_TABLES.contains(table)) {
        LOGGER.debug("[{}] try id column migration for mapped table", table);
        if (isMappingWithLongPrimaryKey(mapping)) {
          migrateTable(table);
        } else {
          LOGGER.debug("[{}] skip table, id isn't mapped as long", table);
        }
      }
    }
  }

  private boolean isMappingWithLongPrimaryKey(PersistentClass mapping) {
    try {
      return (((SimpleValue) mapping.getIdentifier()).getType().getClass() == LongType.class);
    } catch (ClassCastException | NullPointerException exc) {
      return false;
    }
  }

  private void migrateTable(String table) throws XWikiException {
    if (validateTableForMigration(table)) {
      Collection<ForeignKey> droppedForeignKeys = new HashSet<>();
      try {
        dropReferencingForeignKeys(table, droppedForeignKeys);
        for (ForeignKey fk : droppedForeignKeys) {
          migrateTable(fk.getTable());
        }
        String column = getInformationSchema().get(table).getPkColumnName();
        int count = queryExecutor.executeWriteSQL(getModifyIdColumnSql(table, column));
        migratedTables.add(table);
        LOGGER.info("[{}] updated id column for {} rows", table, count);
      } finally {
        addForeignKeys(table, droppedForeignKeys);
      }
    }
  }

  private boolean validateTableForMigration(String table) throws XWikiException {
    boolean validated = false;
    try {
      if (migratedTables.contains(table)) {
        LOGGER.trace("[{}] skip table, already migrated", table);
      } else {
        String type = getInformationSchema().get(table).getPkDataType();
        if ("int".equals(type)) {
          validated = true;
        } else if ("bigint".equals(type)) {
          LOGGER.trace("[{}] skip table, already [bigint]", table);
        } else {
          LOGGER.info("[{}] skip table, id column type is [{}]", table, type);
        }
      }
    } catch (IllegalArgumentException iae) {
      LOGGER.warn("[{}] skip table, no TableSchemaData", table, iae);
    }
    return validated;
  }

  static String getModifyIdColumnSql(String table, String column) {
    return "alter table " + table + " modify column " + column + " bigint not null";
  }

  private void addForeignKeys(String table, Collection<ForeignKey> foreignKeys) {
    if (foreignKeys.size() > 0) {
      LOGGER.debug("[{}] add {} FKs", table, foreignKeys.size());
    }
    for (ForeignKey fk : foreignKeys) {
      try {
        LOGGER.trace("[{}] adding {}", table, fk);
        queryExecutor.executeWriteSQL(fk.getAddSql());
      } catch (XWikiException xwe) {
        LOGGER.error("[{}] failed to add {}", table, fk, xwe);
      }
    }
  }

  private void dropReferencingForeignKeys(String table, Collection<ForeignKey> droppedForeignKeys)
      throws XWikiException {
    for (ForeignKey fk : getInformationSchema().get(table).getForeignKeys()) {
      LOGGER.debug("[{}] dropping {}", table, fk);
      queryExecutor.executeWriteSQL(fk.getDropSql());
      droppedForeignKeys.add(fk);
    }
    if (droppedForeignKeys.size() > 0) {
      LOGGER.info("[{}] dropped {} FKs", table, droppedForeignKeys.size());
    }
  }

  private void clearMigrationData() {
    informationSchema = null;
    migratedTables = new HashSet<>();
  }

  private InformationSchema getInformationSchema() throws XWikiException {
    if (informationSchema == null) {
      informationSchema = new InformationSchema(getDatabaseWithPrefix());
    }
    return informationSchema;
  }

  String getDatabaseWithPrefix() {
    return modelUtils.getDatabaseName(context.getWikiRef());
  }

  private Configuration getHibConfig() {
    return sessionFactory.getConfiguration();
  }

}
