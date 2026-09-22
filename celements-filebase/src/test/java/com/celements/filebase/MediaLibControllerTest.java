package com.celements.filebase;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserService;
import com.celements.filebase.dto.DeleteItem;
import com.celements.filebase.dto.DeleteRequest;
import com.celements.filebase.dto.ListResponse;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.celements.url.UrlService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

public class MediaLibControllerTest extends AbstractComponentTest {

  private MediaLibController mediaLibCtrl;
  private FileItemHelper fileItemHelper;
  private IFileBaseServiceRole fileBaseServiceMock;
  private ModelContext modelContextMock;
  private UrlService urlServiceMock;
  private UserService userServiceMock;
  private User userMock;
  private MockMvc mockMvc;

  @Before
  public void prepare() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("test", "n/a", "ROLE_USER"));
    fileBaseServiceMock = registerComponentMock(IFileBaseServiceRole.class);
    modelContextMock = registerComponentMock(ModelContext.class);
    urlServiceMock = registerComponentMock(UrlService.class);
    userServiceMock = registerComponentMock(UserService.class);
    userMock = createDefaultMock(User.class);
    fileItemHelper = getBeanFactory().getBean(FileItemHelper.class);
    mediaLibCtrl = getBeanFactory().getBean(MediaLibController.class);
    mockMvc = MockMvcBuilders.standaloneSetup(
        AopTestUtils.<MediaLibController>getTargetObject(mediaLibCtrl)).build();
  }

  @Test
  public void testNormalizeFileName_file() {
    String fileName = fileItemHelper.normalizeFileName("local://IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

  @Test
  public void testNormalizeFileName_subPath() {
    String fileName = fileItemHelper.normalizeFileName("local://test/IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

  @Test
  public void test_search_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasListingRight(eq("local://"), same(userMock))).andReturn(true);
    expect(fileBaseServiceMock.getFilesNameMatch(anyObject())).andReturn(List.of());
    replayDefault();
    ListResponse response = mediaLibCtrl.search("test", "local://");
    verifyDefault();
    assertNotNull(response);
    assertEquals("local://", response.dirname());
  }

  @Test
  public void test_search_bindsVueFinderFilter_andReturnsMatchingFile() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasListingRight(eq("local://"), same(userMock))).andReturn(true);
    XWikiDocument docMock = createDefaultMock(XWikiDocument.class);
    XWikiAttachment matchingAtt = createDefaultMock(XWikiAttachment.class);
    XWikiAttachment otherAtt = createDefaultMock(XWikiAttachment.class);
    expect(matchingAtt.getFilename()).andReturn("mELVin.png").anyTimes();
    expect(otherAtt.getFilename()).andReturn("other.png").anyTimes();
    expect(matchingAtt.getDoc()).andReturn(docMock).anyTimes();
    expect(docMock.getDocumentReference())
        .andReturn(new DocumentReference("FileRepo", "public", "xwiki")).anyTimes();
    expect(matchingAtt.getFilesize()).andReturn(100).anyTimes();
    expect(matchingAtt.getDate()).andReturn(new Date()).anyTimes();
    expect(urlServiceMock.getURL(anyObject(), eq("download"))).andReturn("http://download");
    expect(urlServiceMock.getURL(anyObject(), eq("download"), anyString()))
        .andReturn("http://preview");
    expect(fileBaseServiceMock.getFilesNameMatch(anyObject())).andAnswer(() -> {
      IAttachmentMatcher matcher = (IAttachmentMatcher) getCurrentArguments()[0];
      return List.of(matchingAtt, otherAtt).stream().filter(matcher::accept).toList();
    });
    replayDefault();
    var result = mockMvc.perform(get("/api/files/search").servletPath("/api")
        .param("path", "local://").param("filter", "Melvin"))
        .andExpect(status().isOk())
        .andReturn();
    verifyDefault();
    JsonNode response = new ObjectMapper().readTree(result.getResponse().getContentAsByteArray());
    assertEquals("local://", response.get("dirname").asText());
    assertEquals(1, response.get("files").size());
    assertEquals("mELVin.png", response.at("/files/0/basename").asText());
  }

  @Test
  public void test_search_denied() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasListingRight(eq("local://"), same(userMock))).andReturn(false);
    replayDefault();
    try {
      mediaLibCtrl.search("test", "local://");
      fail("Expected FORBIDDEN");
    } catch (ResponseStatusException rse) {
      assertEquals(HttpStatus.FORBIDDEN, rse.getStatus());
    }
    verifyDefault();
  }

  @Test
  public void test_delete_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasDeleteRight(eq("local://"), same(userMock))).andReturn(true);
    expect(fileBaseServiceMock.hasListingRight(eq("local://"), same(userMock))).andReturn(true);
    expect(fileBaseServiceMock.getFilesNameMatch(anyObject())).andReturn(List.of());
    expect(fileBaseServiceMock.deleteFileList(eq(List.of("a.png")))).andReturn(1);
    replayDefault();

    DeleteItem item = new DeleteItem("local://a.png", "file");
    DeleteRequest request = new DeleteRequest("local://", List.of(item));

    ListResponse response = mediaLibCtrl.delete(request);
    verifyDefault();
    assertNotNull(response);
  }

  @Test
  public void test_delete_denied() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasDeleteRight(eq("local://"), same(userMock))).andReturn(false);
    replayDefault();

    DeleteRequest request = new DeleteRequest("local://", null);

    try {
      mediaLibCtrl.delete(request);
      fail("Expected FORBIDDEN");
    } catch (ResponseStatusException rse) {
      assertEquals(HttpStatus.FORBIDDEN, rse.getStatus());
    }
    verifyDefault();
  }

  private void expectCheckAuth() throws Exception {
    XWikiUser xuser = new XWikiUser("xwiki:User.test");
    expect(getXContext().getWiki().checkAuth(same(getXContext()))).andReturn(xuser).anyTimes();
    expect(userServiceMock.getUser(eq("xwiki:User.test"))).andReturn(userMock).anyTimes();
  }

}
