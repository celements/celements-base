package com.celements.filebase;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.filebase.dto.DeleteItem;
import com.celements.filebase.dto.DeleteRequest;
import com.celements.filebase.dto.ListResponse;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.url.UrlService;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

public class PageAttachmentsControllerTest extends AbstractComponentTest {

  private PageAttachmentsController pageAttachmentsCtrl;
  private FileItemHelper fileItemHelper;
  private IAttachmentServiceRole attServiceMock;
  private UrlService urlServiceMock;
  private IModelAccessFacade modelAccessMock;
  private IRightsAccessFacadeRole rightsAccessMock;
  private ModelContext modelContextMock;
  private UserService userServiceMock;
  private User userMock;

  @Before
  public void prepare() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("test", "n/a", "ROLE_USER"));
    attServiceMock = registerComponentMock(IAttachmentServiceRole.class);
    urlServiceMock = registerComponentMock(UrlService.class);
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    rightsAccessMock = registerComponentMock(IRightsAccessFacadeRole.class);
    modelContextMock = registerComponentMock(ModelContext.class);
    userServiceMock = registerComponentMock(UserService.class);
    userMock = createDefaultMock(User.class);
    fileItemHelper = getBeanFactory().getBean(FileItemHelper.class);
    pageAttachmentsCtrl = getBeanFactory().getBean(PageAttachmentsController.class);
  }

  @Test
  public void testNormalizeFileName_file() {
    String fileName = fileItemHelper.normalizeFileName("attachments://MySpace/MyDoc/IMG.jpg");
    assertEquals("IMG.jpg", fileName);
  }

  @Test
  public void test_list_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    WikiReference wikiRef = new WikiReference("xwiki");
    expect(modelContextMock.getWikiRef()).andReturn(wikiRef).anyTimes();

    DocumentReference docRef = new DocumentReference("MyDoc", new SpaceReference("MySpace", wikiRef));
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW), same(userMock))).andReturn(true);

    XWikiDocument docMock = createDefaultMock(XWikiDocument.class);
    expect(modelAccessMock.getOrCreateDocument(eq(docRef))).andReturn(docMock);

    XWikiAttachment attMock = createDefaultMock(XWikiAttachment.class);
    expect(attMock.getFilename()).andReturn("file.png").anyTimes();
    expect(attMock.getDoc()).andReturn(docMock).anyTimes();
    expect(attMock.getFilesize()).andReturn(100).anyTimes();
    expect(attMock.getDate()).andReturn(new Date()).anyTimes();

    expect(attServiceMock.getAttachmentsNameMatch(same(docMock), anyObject())).andReturn(List.of(attMock));
    expect(docMock.getDocumentReference()).andReturn(docRef).anyTimes();
    expect(urlServiceMock.getURL(anyObject(), eq("download"))).andReturn("http://download");
    expect(urlServiceMock.getURL(anyObject(), eq("download"), anyString())).andReturn("http://preview");

    replayDefault();
    ListResponse response = pageAttachmentsCtrl.list("MySpace", "MyDoc", "attachments://MySpace/MyDoc");
    verifyDefault();

    assertNotNull(response);
    assertEquals("attachments://MySpace/MyDoc", response.dirname());
    assertEquals(1, response.files().size());
    assertEquals("file.png", response.files().get(0).basename());
  }

  @Test
  public void test_list_denied() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    WikiReference wikiRef = new WikiReference("xwiki");
    expect(modelContextMock.getWikiRef()).andReturn(wikiRef).anyTimes();

    DocumentReference docRef = new DocumentReference("MyDoc", new SpaceReference("MySpace", wikiRef));
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW), same(userMock))).andReturn(false);

    replayDefault();
    try {
      pageAttachmentsCtrl.list("MySpace", "MyDoc", "attachments://MySpace/MyDoc");
      fail("Expected FORBIDDEN");
    } catch (ResponseStatusException rse) {
      assertEquals(HttpStatus.FORBIDDEN, rse.getStatus());
    }
    verifyDefault();
  }

  @Test
  public void test_search_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    WikiReference wikiRef = new WikiReference("xwiki");
    expect(modelContextMock.getWikiRef()).andReturn(wikiRef).anyTimes();

    DocumentReference docRef = new DocumentReference("MyDoc", new SpaceReference("MySpace", wikiRef));
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW), same(userMock))).andReturn(true);

    XWikiDocument docMock = createDefaultMock(XWikiDocument.class);
    expect(modelAccessMock.getOrCreateDocument(eq(docRef))).andReturn(docMock);

    expect(attServiceMock.getAttachmentsNameMatch(same(docMock), anyObject())).andReturn(List.of());

    replayDefault();
    ListResponse response = pageAttachmentsCtrl.search("MySpace", "MyDoc", "test", "attachments://MySpace/MyDoc");
    verifyDefault();

    assertNotNull(response);
    assertEquals("attachments://MySpace/MyDoc", response.dirname());
  }

  @Test
  public void test_delete_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    WikiReference wikiRef = new WikiReference("xwiki");
    expect(modelContextMock.getWikiRef()).andReturn(wikiRef).anyTimes();

    DocumentReference docRef = new DocumentReference("MyDoc", new SpaceReference("MySpace", wikiRef));
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.DELETE), same(userMock))).andReturn(true);
    expect(rightsAccessMock.hasAccessLevel(eq(docRef), eq(EAccessLevel.VIEW), same(userMock))).andReturn(true);

    XWikiDocument docMock = createDefaultMock(XWikiDocument.class);
    expect(modelAccessMock.getOrCreateDocument(eq(docRef))).andReturn(docMock).times(2);

    expect(attServiceMock.existsAttachmentNameEqual(same(docMock), eq("a.png"))).andReturn(true);
    attServiceMock.deleteAttachmentList(anyObject());
    expectLastCall().andReturn(1);

    expect(attServiceMock.getAttachmentsNameMatch(same(docMock), anyObject())).andReturn(List.of());

    replayDefault();

    DeleteItem item = new DeleteItem("attachments://MySpace/MyDoc/a.png", "file");
    DeleteRequest request = new DeleteRequest("attachments://MySpace/MyDoc", List.of(item));

    ListResponse response = pageAttachmentsCtrl.delete("MySpace", "MyDoc", request);
    verifyDefault();
    assertNotNull(response);
  }

  private void expectCheckAuth() throws Exception {
    XWikiUser xuser = new XWikiUser("xwiki:User.test");
    expect(getXContext().getWiki().checkAuth(same(getXContext()))).andReturn(xuser).anyTimes();
    expect(userServiceMock.getUser(eq("xwiki:User.test"))).andReturn(userMock).anyTimes();
  }

}
