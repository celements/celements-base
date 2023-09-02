package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.migration.BaseCollectionIdColumnMigration.*;
import static com.celements.model.migration.InformationSchema.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.easymock.IExpectationSetters;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class BaseCollectionIdColumnMigrationTest extends AbstractComponentTest {

  private BaseCollectionIdColumnMigration migration;
  private IQueryExecutionServiceRole queryExecMock;
  private Configuration hibCfgMock;
  private Builder<List<String>> idColumnBuilder;
  private Builder<List<String>> foreignKeyBuilder;

  @Before
  public void prepareTest() throws Exception {
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    hibCfgMock = createDefaultMock(Configuration.class);
    registerComponentMocks(HibernateSessionFactory.class, ModelUtils.class);
    expect(getMock(HibernateSessionFactory.class).getConfiguration()).andReturn(
        hibCfgMock).anyTimes();
    migration = (BaseCollectionIdColumnMigration) Utils.getComponent(ICelementsMigrator.class,
        NAME);
    idColumnBuilder = ImmutableList.builder();
    foreignKeyBuilder = ImmutableList.builder();
  }

  @Test
  public void test_migrate_xwikitables() throws Exception {
    for (String table : XWIKI_TABLES) {
      expectModifyIdColumn(table);
    }
    expectMappings(ImmutableList.of());
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_xwikitables_withFK() throws Exception {
    expectModifyFkProperties();
    expectMappings(ImmutableList.of());
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  private void expectModifyFkProperties() throws XWikiException {
    String table = "xwikiproperties";
    String fkTable1 = "xwikiintegers";
    String fkName1 = "FK2780715A3433FD87";
    String fkTable2 = "xwikilists";
    String fkName2 = "FKDF25AE4F283EE295";
    addForeignKey(fkName1, fkTable1, "XWI_ID", table, "XWP_ID");
    addForeignKey(fkName1, fkTable1, "XWI_NAME", table, "XWP_NAME");
    addForeignKey(fkName2, fkTable2, "XWL_ID", table, "XWP_ID");
    addForeignKey(fkName2, fkTable2, "XWL_NAME", table, "XWP_NAME");
    expectModifyIdColumn(table, 1);
    expectModifyIdColumn(fkTable1);
    expectModifyFkLists();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " drop foreign key "
        + fkName1)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable2 + " drop foreign key "
        + fkName2)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " add constraint " + fkName1
        + " foreign key (XWI_ID,XWI_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable2 + " add constraint " + fkName2
        + " foreign key (XWL_ID,XWL_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
  }

  private void expectModifyFkLists() throws XWikiException {
    String table = "xwikilists";
    String fkTable = "xwikilistitems";
    String fkName = "FKC0862BA3FB72A11";
    addForeignKey(fkName, fkTable, "XWL_ID", table, "XWL_ID");
    addForeignKey(fkName, fkTable, "XWL_NAME", table, "XWL_NAME");
    expectModifyIdColumn(table, 1);
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " drop foreign key "
        + fkName)).andReturn(1).once();
    expectModifyIdColumn("xwikilistitems");
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " add constraint " + fkName
        + " foreign key (XWL_ID,XWL_NAME) references " + table + " (XWL_ID,XWL_NAME)")).andReturn(
            1).once();
  }

  @Test
  public void test_migrate_xwikitables_fail() throws Exception {
    String table = "xwikiproperties";
    String fkTable1 = "xwikiintegers";
    String fkName1 = "FK2780715A3433FD87";
    addForeignKey(fkName1, fkTable1, "XWI_ID", table, "XWP_ID");
    addForeignKey(fkName1, fkTable1, "XWI_NAME", table, "XWP_NAME");
    expectModifyIdColumn(table, new XWikiException());
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " drop foreign key "
        + fkName1)).andReturn(1).once();
    // FK has to be readded, even after exception occured
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " add constraint " + fkName1
        + " foreign key (XWI_ID,XWI_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
    expectInformationSchemaLoad();

    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class) {

      @Override
      protected void execute() throws Exception {
        migration.migrate(null, getContext());
      }

    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_migrate_mappedtables() throws Exception {
    String table = "table_mapped";
    Builder<PersistentClass> builder = ImmutableList.builder();
    builder.add(createMapping("entity", table, LongType.class));
    builder.add(createMapping("entity_Integer", table + "_int", IntegerType.class));
    builder.add(createMapping("entity_String", table + "_str", StringType.class));
    builder.add(createMapping("entity_None", table + "_none", null));
    expectModifyIdColumn(table);
    expectMappings(builder.build());
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  private PersistentClass createMapping(String entityName, String tableName,
      Class<? extends Type> hibType) {
    RootClass mapping = new RootClass();
    mapping.setEntityName(entityName);
    Table table = new Table(tableName);
    mapping.setTable(table);
    if (hibType != null) {
      SimpleValue identifier = new SimpleValue(table);
      identifier.setTypeName(hibType.getName());
      mapping.setIdentifier(identifier);
    }
    return mapping;
  }

  private void expectMappings(Iterable<?> iterable) {
    expect(hibCfgMock.getClassMappings()).andReturn(iterable.iterator()).once();
  }

  private void expectModifyIdColumn(String table) throws XWikiException {
    expectModifyIdColumn(table, 1);
  }

  private void expectModifyIdColumn(String table, Object ret) throws XWikiException {
    addColumn(table);
    IExpectationSetters<Integer> exp = expect(queryExecMock.executeWriteSQL(getModifyIdColumnSql(
        table, createIdColumnName(table))));
    if (ret instanceof Integer) {
      exp.andReturn((Integer) ret).once();
    } else if (ret instanceof Throwable) {
      exp.andThrow((Throwable) ret).once();
    }
  }

  private void addColumn(String table) {
    idColumnBuilder.add(ImmutableList.of(table, createIdColumnName(table), "int"));
  }

  private void addForeignKey(String name, String table, String col, String referencedTable,
      String referencedCol) {
    foreignKeyBuilder.add(Arrays.asList(name, table, col, referencedTable, referencedCol));
  }

  private void expectInformationSchemaLoad() throws XWikiException {
    String database = "db";
    expect(getMock(ModelUtils.class).getDatabaseName(Utils.getComponent(
        ModelContext.class).getWikiRef())).andReturn(database).anyTimes();
    expect(queryExecMock.executeReadSql(getLoadColumnsSql(database))).andReturn(
        idColumnBuilder.build()).once();
    expect(queryExecMock.executeReadSql(getLoadForeignKeysSql(database))).andReturn(
        foreignKeyBuilder.build()).once();
  }

  @Test
  public void test_getModifyIdColumnSql() throws Exception {
    replayDefault();
    assertEquals("alter table tbl modify column TBL_ID bigint not null", getModifyIdColumnSql("tbl",
        createIdColumnName("tbl")));
    verifyDefault();
  }

  private String createIdColumnName(String table) {
    return table.toUpperCase() + "_ID";
  }

  @Test
  public void test_getLoadForeignKeysSql() throws Exception {
    assertEquals("select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, "
        + "REFERENCED_COLUMN_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE "
        + "where TABLE_SCHEMA = 'db' and REFERENCED_TABLE_NAME is not null "
        + "order by REFERENCED_TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION",
        getLoadForeignKeysSql("db"));
  }

  @Test
  public void test_getLoadColumnsSql() throws Exception {
    assertEquals("select k.TABLE_NAME, k.COLUMN_NAME, c.DATA_TYPE "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE as k, INFORMATION_SCHEMA.COLUMNS as c "
        + "where k.TABLE_SCHEMA = 'db' and k.CONSTRAINT_NAME = 'PRIMARY' and k.COLUMN_NAME "
        + "like '%_ID' and k.TABLE_SCHEMA = c.TABLE_SCHEMA and k.TABLE_NAME = c.TABLE_NAME and "
        + "k.COLUMN_NAME = c.COLUMN_NAME group by k.TABLE_NAME", getLoadColumnsSql("db"));
  }

}
