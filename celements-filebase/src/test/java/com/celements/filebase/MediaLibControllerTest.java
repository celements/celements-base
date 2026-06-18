package com.celements.filebase;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import com.celements.auth.user.User;
import com.celements.filebase.dto.DeleteItem;
import com.celements.filebase.dto.DeleteRequest;
import com.celements.filebase.dto.ListResponse;
import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.user.api.XWikiUser;

public class MediaLibControllerTest extends AbstractComponentTest {

  private MediaLibController mediaLibCtrl;
  private FileItemHelper fileItemHelper;
  private IFileBaseServiceRole fileBaseServiceMock;
  private ModelContext modelContextMock;
  private UserService userServiceMock;
  private User userMock;

  @Before
  public void prepare() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("test", "n/a", "ROLE_USER"));
    fileBaseServiceMock = registerComponentMock(IFileBaseServiceRole.class);
    modelContextMock = registerComponentMock(ModelContext.class);
    userServiceMock = registerComponentMock(UserService.class);
    userMock = createDefaultMock(User.class);
    fileItemHelper = getBeanFactory().getBean(FileItemHelper.class);
    mediaLibCtrl = getBeanFactory().getBean(MediaLibController.class);
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
