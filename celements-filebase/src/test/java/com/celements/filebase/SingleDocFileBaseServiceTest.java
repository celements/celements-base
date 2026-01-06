package com.celements.filebase;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.filebase.exceptions.FileBaseLoadException;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentLoadException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class SingleDocFileBaseServiceTest extends AbstractComponentTest {

  private SingleDocFileBaseService fbService;
  private ConfigurationSource configurationMock;
  private IModelAccessFacade modelAccessMock;

  @Before
  public void setUp_SingleDocFileBaseService() throws Exception {
    configurationMock = registerComponentMock(ConfigurationSource.class);
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    fbService = (SingleDocFileBaseService) Utils.getComponent(IFileBaseServiceRole.class,
        SingleDocFileBaseService.FILEBASE_SINGLE_DOC);
  }

  @Test
  public void test_getFileBaseDoc_noConfig() {
    expect(configurationMock.getProperty(eq(
        IFileBaseServiceRole.FILEBASE_CONFIG_FIELD))).andReturn(null);
    replayDefault();
    try {
      fbService.getFileBaseDoc();
      fail("Expected FileBaseLoadException");
    } catch (FileBaseLoadException fble) {
      // expected outcome
    }
    verifyDefault();
  }

  @Test
  public void test_getFileBaseDoc_configMinus() {
    expect(configurationMock.getProperty(eq(
        IFileBaseServiceRole.FILEBASE_CONFIG_FIELD))).andReturn("-");
    replayDefault();
    try {
      fbService.getFileBaseDoc();
      fail("Expected FileBaseLoadException");
    } catch (FileBaseLoadException fble) {
      // expected outcome
    }
    verifyDefault();
  }

  @Test
  public void test_getFileBaseDoc_configDocLoadFail() throws DocumentLoadException {
    String spcName = "FBSpace";
    String docName = "FBDoc";
    DocumentReference fileBaseDocRef = new DocumentReference(getXContext().getDatabase(), spcName,
        docName);
    expect(configurationMock.getProperty(eq(
        IFileBaseServiceRole.FILEBASE_CONFIG_FIELD))).andReturn(spcName + "." + docName);
    expect(modelAccessMock.getOrCreateDocument(eq(fileBaseDocRef))).andThrow(
        new DocumentLoadException(fileBaseDocRef));
    replayDefault();
    try {
      fbService.getFileBaseDoc();
      fail("Expected FileBaseLoadException");
    } catch (FileBaseLoadException fble) {
      // expected outcome
    }
    verifyDefault();
  }

  @Test
  public void test_getFileBaseDoc_configDoc() throws Exception {
    String spcName = "FBSpace";
    String docName = "FBDoc";
    DocumentReference fileBaseDocRef = new DocumentReference(getXContext().getDatabase(), spcName,
        docName);
    XWikiDocument doc = new XWikiDocument(fileBaseDocRef);
    expect(configurationMock.getProperty(eq(
        IFileBaseServiceRole.FILEBASE_CONFIG_FIELD))).andReturn(spcName + "." + docName);
    expect(modelAccessMock.getOrCreateDocument(eq(fileBaseDocRef))).andReturn(doc);
    replayDefault();
    XWikiDocument resultDoc = fbService.getFileBaseDoc();
    verifyDefault();
    assertEquals(doc.getDocumentReference(), resultDoc.getDocumentReference());
  }

}
