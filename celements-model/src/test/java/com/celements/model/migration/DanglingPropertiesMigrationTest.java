package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.migration.DanglingPropertiesMigration.*;
import static com.celements.model.migration.InformationSchema.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.reference.ReferenceProvider;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.celements.wiki.WikiProvider;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class DanglingPropertiesMigrationTest extends AbstractComponentTest {

  private DanglingPropertiesMigration migration;
  private IQueryExecutionServiceRole queryExecMock;
  private Configuration hibCfgMock;
  private Builder<List<String>> idColumnBuilder;

  @Before
  public void prepareTest() throws Exception {
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    hibCfgMock = createDefaultMock(Configuration.class);
    registerComponentMocks(HibernateSessionFactory.class, IModelAccessFacade.class,
        ReferenceProvider.class, WikiProvider.class);
    expect(getMock(HibernateSessionFactory.class).getConfiguration()).andReturn(
        hibCfgMock).anyTimes();
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn("").anyTimes();
    migration = (DanglingPropertiesMigration) Utils.getComponent(ICelementsMigrator.class, NAME);
    idColumnBuilder = ImmutableList.builder();
  }

  @Test
  public void test_migrate_noMappings() throws Exception {
    expect(hibCfgMock.getClassMappings()).andReturn(Collections.emptyList().iterator());

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_noClassDoc() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expect(getMock(IModelAccessFacade.class).getDocument(getModelUtils().resolveRef(className,
        DocumentReference.class))).andThrow(new DocumentNotExistsException(null));

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_noInternalMapping() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expectCustomMapping(className, "external");

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_noTableSchemaData() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expectCustomMapping(className, "internal");
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_noDanglingProperties() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expectCustomMapping(className, "internal");
    addColumn(table);
    expectInformationSchemaLoad();
    expect(queryExecMock.executeReadSql(getSelectSql(table, createIdColumnName(table),
        className))).andReturn(Collections.<List<String>>emptyList());

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expectCustomMapping(className, "internal");
    addColumn(table);
    expectInformationSchemaLoad();
    expect(queryExecMock.executeReadSql(getSelectSql(table, createIdColumnName(table),
        className))).andReturn(Arrays.asList(Arrays.asList("data")));
    expect(queryExecMock.executeWriteSQL(getDeleteSql(table, createIdColumnName(table),
        className))).andReturn(1);

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_XWE() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator());
    expectCustomMapping(className, "internal");
    addColumn(table);
    expectInformationSchemaLoad();
    XWikiException xwe = new XWikiException();
    expect(queryExecMock.executeReadSql(getSelectSql(table, createIdColumnName(table),
        className))).andThrow(xwe);

    replayDefault();
    assertThrows(XWikiException.class, () -> migration.migrate(null, getContext()));
    verifyDefault();
  }

  private PersistentClass createMapping(String entityName, String tableName) {
    RootClass mapping = new RootClass();
    mapping.setEntityName(entityName);
    Table table = new Table(tableName);
    mapping.setTable(table);
    return mapping;
  }

  private void expectCustomMapping(String className, String customMapping)
      throws DocumentNotExistsException {
    BaseClass bClass = new BaseClass();
    bClass.setCustomMapping(customMapping);
    XWikiDocument classDoc = new XWikiDocument(getModelUtils().resolveRef(className,
        DocumentReference.class));
    classDoc.setXClass(bClass);
    expect(getMock(IModelAccessFacade.class).getDocument(
        classDoc.getDocumentReference())).andReturn(classDoc);
  }

  private void addColumn(String table) {
    idColumnBuilder.add(ImmutableList.of(table, createIdColumnName(table), "long"));
  }

  private void expectInformationSchemaLoad() throws XWikiException {
    expect(queryExecMock.executeReadSql(getLoadColumnsSql(getContext().getDatabase()))).andReturn(
        idColumnBuilder.build());
  }

  @Test
  public void test_getSelectSql() throws Exception {
    replayDefault();
    assertEquals("select XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER, tbl.* from tbl "
        + "left join xwikiobjects on XWO_ID = TBL_ID where XWO_ID is null "
        + "or XWO_CLASSNAME <> 'Classes.Class' order by XWO_CLASSNAME, XWO_NAME",
        getSelectSql(
            "tbl", createIdColumnName("tbl"), "Classes.Class"));
    verifyDefault();
  }

  @Test
  public void test_getDeleteSql() throws Exception {
    replayDefault();
    assertEquals("delete tbl from tbl left join xwikiobjects on XWO_ID = TBL_ID "
        + "where XWO_ID is null or XWO_CLASSNAME <> 'Classes.Class'",
        getDeleteSql("tbl",
            createIdColumnName("tbl"), "Classes.Class"));
    verifyDefault();
  }

  private String createIdColumnName(String table) {
    return table.toUpperCase() + "_ID";
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
