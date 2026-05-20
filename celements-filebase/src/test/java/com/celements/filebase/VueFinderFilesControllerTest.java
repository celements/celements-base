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
import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.user.api.XWikiUser;

public class VueFinderFilesControllerTest extends AbstractComponentTest {

  private VueFinderFilesController vueFinderCtrl;
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
    vueFinderCtrl = getBeanFactory().getBean(VueFinderFilesController.class);
  }

  @Test
  public void testNormalizeFileName_file() {
    String fileName = vueFinderCtrl.normalizeFileName("local://IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

  @Test
  public void testNormalizeFileName_subPath() {
    String fileName = vueFinderCtrl.normalizeFileName("local://test/IMG-20250606-WA0000.jpg");
    assertEquals("IMG-20250606-WA0000.jpg", fileName);
  }

  @Test
  public void test_search_allowed() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasListingRight(eq("local://"), same(userMock))).andReturn(true);
    expect(fileBaseServiceMock.getFilesNameMatch(anyObject())).andReturn(List.of());
    replayDefault();
    ListResponse response = vueFinderCtrl.search("test", "local://");
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
      vueFinderCtrl.search("test", "local://");
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

    VueFinderFilesController.DeleteRequest request = new VueFinderFilesController.DeleteRequest();
    request.path = "local://";
    VueFinderFilesController.DeleteItem item = new VueFinderFilesController.DeleteItem();
    item.path = "local://a.png";
    item.type = "file";
    request.items = List.of(item);

    ListResponse response = vueFinderCtrl.delete(request);
    verifyDefault();
    assertNotNull(response);
  }

  @Test
  public void test_delete_denied() throws Exception {
    expectCheckAuth();
    expect(modelContextMock.user()).andReturn(Optional.of(userMock)).anyTimes();
    expect(fileBaseServiceMock.hasDeleteRight(eq("local://"), same(userMock))).andReturn(false);
    replayDefault();

    VueFinderFilesController.DeleteRequest request = new VueFinderFilesController.DeleteRequest();
    request.path = "local://";

    try {
      vueFinderCtrl.delete(request);
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
