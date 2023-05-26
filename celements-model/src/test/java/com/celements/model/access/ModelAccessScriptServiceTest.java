package com.celements.model.access;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

public class ModelAccessScriptServiceTest extends AbstractComponentTest {

  private ModelAccessScriptService modelAccess;
  private DocumentReference docRef;
  private XWikiDocument doc;
  private IRightsAccessFacadeRole rightsAccessMock;

  @Before
  public void prepareTest() {
    modelAccess = (ModelAccessScriptService) Utils.getComponent(ScriptService.class,
        ModelAccessScriptService.NAME);
    modelAccess.modelAccess = createDefaultMock(IModelAccessFacade.class);
    rightsAccessMock = createDefaultMock(IRightsAccessFacadeRole.class);
    modelAccess.rightsAccess = rightsAccessMock;
    docRef = new DocumentReference("db", "space", "doc");
    doc = new XWikiDocument(docRef);
    XWikiRightService rightServiceMock = createDefaultMock(XWikiRightService.class);
    expect(getWikiMock().getRightService()).andReturn(rightServiceMock).anyTimes();
    expect(rightServiceMock.hasProgrammingRights(same(getContext()))).andReturn(true).anyTimes();
  }

  @Test
  public void test_getDocument() throws Exception {
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW))).andReturn(
        true).once();
    expect(modelAccess.modelAccess.getDocument(docRef, "")).andReturn(doc);
    Document apiDoc = new Document(doc, getContext());
    expect(modelAccess.modelAccess.getApiDocument(same(doc))).andReturn(apiDoc).once();
    replayDefault();
    Document ret = modelAccess.getDocument(docRef);
    verifyDefault();
    assertSame(ret, apiDoc);
  }

  @Test
  public void test_getDocument_loadException() throws Exception {
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW))).andReturn(
        true).once();
    expect(modelAccess.modelAccess.getDocument(docRef, ""))
        .andThrow(new DocumentLoadException(docRef));
    replayDefault();
    Document ret = modelAccess.getDocument(docRef);
    verifyDefault();
    assertNull(ret);
  }

  @Test
  public void test_getDocument_notViewRights() throws Exception {
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW))).andReturn(
        false).once();
    replayDefault();
    Document ret = modelAccess.getDocument(docRef);
    verifyDefault();
    assertNull(ret);
  }

  @Test
  public void test_getDocument_null() throws Exception {
    expect(rightsAccessMock.hasAccessLevel(isNull(DocumentReference.class), eq(
        EAccessLevel.VIEW))).andReturn(false).once();
    replayDefault();
    Document ret = modelAccess.getDocument(null);
    verifyDefault();
    assertNull(ret);
  }

  @Test
  public void test_exists() {
    expect(modelAccess.modelAccess.exists(eq(docRef))).andReturn(true).once();
    replayDefault();
    boolean ret = modelAccess.exists(docRef);
    verifyDefault();
    assertTrue(ret);
  }

  @Test
  public void test_exists_null() {
    expect(modelAccess.modelAccess.exists(isNull(DocumentReference.class))).andReturn(false).once();
    replayDefault();
    boolean ret = modelAccess.exists(null);
    verifyDefault();
    assertFalse(ret);
  }

}
