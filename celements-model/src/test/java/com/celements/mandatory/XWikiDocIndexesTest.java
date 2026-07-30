package com.celements.mandatory;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.web.Utils;

public class XWikiDocIndexesTest extends AbstractComponentTest {

  private XWikiDocIndexes mandatory;
  private IQueryExecutionServiceRole queryExecServiceMock;
  private WikiReference wikiRef;

  @Before
  public void prepareTest() throws Exception {
    queryExecServiceMock = registerComponentMock(IQueryExecutionServiceRole.class);
    mandatory = (XWikiDocIndexes) Utils.getComponent(IMandatoryDocumentRole.class,
        XWikiDocIndexes.NAME);
    wikiRef = new WikiReference("wiki");
    getContext().setDatabase(wikiRef.getName());
  }

  @Test
  public void test_dependsOnMandatoryDocuments() throws Exception {
    assertEquals(0, mandatory.dependsOnMandatoryDocuments().size());
  }

  @Test
  public void test_checkDocuments() throws Exception {
    String sql = mandatory.getAddSql();
    expect(queryExecServiceMock.existsIndex(wikiRef, mandatory.getTableName(),
        mandatory.getIndexName())).andReturn(false).once();
    expect(queryExecServiceMock.executeWriteSQL(sql)).andReturn(0).once();

    replayDefault();
    mandatory.checkDocuments();
    verifyDefault();
  }

  @Test
  public void test_checkDocuments_indexExists() throws Exception {
    expect(queryExecServiceMock.existsIndex(wikiRef, mandatory.getTableName(),
        mandatory.getIndexName())).andReturn(true).once();

    replayDefault();
    mandatory.checkDocuments();
    verifyDefault();
  }

  @Test
  public void test_getTableName() throws Exception {
    assertEquals("xwikidoc", mandatory.getTableName());
  }

  @Test
  public void test_getIndexName() throws Exception {
    assertEquals("fulnameIDX", mandatory.getIndexName());
  }

  @Test
  public void test_getAddSql() throws Exception {
    assertEquals("ALTER TABLE xwikidoc"
        + " ADD UNIQUE INDEX `fulnameIDX` (`XWD_FULLNAME`,`XWD_LANGUAGE`),"
        + " ADD INDEX `webNameUNIQUEIDX` (`XWD_WEB`(150),`XWD_NAME`(150),`XWD_LANGUAGE`),"
        + " ADD INDEX `webIDX` (`XWD_WEB`),"
        + " ADD INDEX `menuSelectINDX` (`XWD_FULLNAME`(100),`XWD_TRANSLATION`,`XWD_PARENT`(100),"
        + "`XWD_WEB`(100)),"
        + " ADD INDEX `languageIDX` (`XWD_LANGUAGE`),"
        + " ADD INDEX `elementsIDX` (`XWD_ELEMENTS`),"
        + " ADD INDEX `classXMLindex` (`XWD_CLASS_XML`(30))",
        mandatory.getAddSql());
  }

}
