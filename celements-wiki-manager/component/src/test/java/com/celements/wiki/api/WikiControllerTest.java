package com.celements.wiki.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.wiki.WikiCacheRefresher;
import com.celements.wiki.WikiCreator;
import com.celements.wiki.WikiDescriptor;
import com.celements.wiki.WikiDescriptor.State;
import com.celements.wiki.WikiDescriptor.Visibility;
import com.celements.wiki.WikiDescriptorService;
import com.celements.wiki.WikiService;
import com.celements.wiki.exception.WikiExistsException;
import com.celements.wiki.exception.WikiMissingException;
import com.xpn.xwiki.user.api.XWikiUser;

public class WikiControllerTest extends AbstractComponentTest {

  private WikiController controller;
  private User user;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(WikiService.class);
    registerComponentMock(WikiDescriptorService.class);
    registerComponentMock(WikiCreator.class);
    registerComponentMock(WikiCacheRefresher.class);
    registerComponentMock(IRightsAccessFacadeRole.class);
    registerComponentMock(UserService.class);
    user = createDefaultMock(User.class);
    controller = getBeanTarget(getBeanFactory().getBean(WikiController.class));
  }

  @SuppressWarnings("unchecked")
  private <T> T getBeanTarget(T bean) throws Exception {
    if (bean instanceof Advised advised) {
      return (T) advised.getTargetSource().getTarget();
    }
    return bean;
  }

  @Test
  public void test_getWikis() throws Exception {
    expectSuperAdmin();
    WikiReference wikiRef = new WikiReference("mywiki");
    expect(getMock(WikiService.class).streamAllWikis()).andReturn(Stream.of(wikiRef));
    expectDescriptor(wikiRef);

    replayDefault();
    List<WikiDescriptor> ret = controller.getWikis();
    verifyDefault();
    assertEquals(1, ret.size());
    assertEquals(wikiRef, ret.get(0).wiki());
    assertEquals("My Wiki", ret.get(0).prettyName());
    assertEquals("mywiki.example", ret.get(0).server());
    assertEquals(Visibility.PUBLIC, ret.get(0).visibility());
    assertEquals(State.ACTIVE, ret.get(0).state());
    assertEquals(URI.create("https://mywiki.example"), ret.get(0).uri());
  }

  @Test
  public void test_getWiki() throws Exception {
    expectSuperAdmin();
    WikiReference wikiRef = new WikiReference("mywiki");
    expectDescriptor(wikiRef);

    replayDefault();
    List<WikiDescriptor> ret = controller.getWiki("mywiki");
    verifyDefault();
    assertEquals(1, ret.size());
    assertEquals(wikiRef, ret.get(0).wiki());
    assertEquals("My Wiki", ret.get(0).prettyName());
    assertEquals("mywiki.example", ret.get(0).server());
    assertEquals(Visibility.PUBLIC, ret.get(0).visibility());
    assertEquals(State.ACTIVE, ret.get(0).state());
    assertEquals("en", ret.get(0).language());
    assertEquals(Boolean.TRUE, ret.get(0).secure());
    assertEquals(Boolean.FALSE, ret.get(0).oicd());
    assertEquals(URI.create("https://mywiki.example"), ret.get(0).uri());
  }

  @Test
  public void test_createWiki() throws Exception {
    expectSuperAdmin();
    WikiReference wikiRef = new WikiReference("newwiki");
    getMock(WikiCreator.class).createWiki(wikiRef);
    getMock(WikiCacheRefresher.class).refresh();
    expectDescriptor(wikiRef);

    replayDefault();
    ResponseEntity<List<WikiDescriptor>> ret = controller.createWiki("newwiki");
    verifyDefault();
    assertEquals(HttpStatus.CREATED, ret.getStatusCode());
    assertEquals(wikiRef, ret.getBody().get(0).wiki());
    assertEquals("My Wiki", ret.getBody().get(0).prettyName());
    assertEquals(URI.create("https://newwiki.example"), ret.getBody().get(0).uri());
  }

  @Test
  public void test_createWiki_conflict() throws Exception {
    expectSuperAdmin();
    WikiReference wikiRef = new WikiReference("existingwiki");
    getMock(WikiCreator.class).createWiki(wikiRef);
    expectLastCall().andThrow(new WikiExistsException(wikiRef));

    replayDefault();
    assertThrows(WikiExistsException.class, () -> controller.createWiki("existingwiki"));
    verifyDefault();
  }

  @Test
  public void test_handleException_wikiExists() throws Exception {
    expect(getMock(IRightsAccessFacadeRole.class).isSuperAdmin()).andReturn(true);
    WikiReference wikiRef = new WikiReference("existingwiki");

    replayDefault();
    ResponseEntity<String> ret = controller.handleException(new WikiExistsException(wikiRef));
    verifyDefault();
    assertEquals(HttpStatus.CONFLICT, ret.getStatusCode());
  }

  @Test
  public void test_getWikis_unauthorized() throws Exception {
    expect(getXContext().getWiki().checkAuth(same(getXContext()))).andReturn(null);

    replayDefault();
    try {
      controller.getWikis();
      fail("Expected UNAUTHORIZED");
    } catch (ResponseStatusException rse) {
      assertEquals(HttpStatus.UNAUTHORIZED, rse.getStatus());
    }
    verifyDefault();
  }

  @Test
  public void test_getWikis_forbidden() throws Exception {
    expectAuth();
    expect(getMock(IRightsAccessFacadeRole.class).isSuperAdmin(user)).andReturn(false);

    replayDefault();
    try {
      controller.getWikis();
      fail("Expected FORBIDDEN");
    } catch (ResponseStatusException rse) {
      assertEquals(HttpStatus.FORBIDDEN, rse.getStatus());
    }
    verifyDefault();
  }

  private void expectSuperAdmin() throws Exception {
    expectAuth();
    expect(getMock(IRightsAccessFacadeRole.class).isSuperAdmin(user)).andReturn(true);
  }

  private void expectAuth() throws Exception {
    XWikiUser xuser = new XWikiUser("xwiki:XWiki.Admin", true);
    expect(getXContext().getWiki().checkAuth(same(getXContext()))).andReturn(xuser);
    expect(getMock(UserService.class).getUser(eq("xwiki:XWiki.Admin"))).andReturn(user);
  }

  private void expectDescriptor(WikiReference wikiRef) throws WikiMissingException {
    WikiDescriptor descriptor = new WikiDescriptor(
        wikiRef,
        "My Wiki",
        wikiRef.getName() + ".example",
        Visibility.PUBLIC,
        State.ACTIVE,
        "en",
        true,
        false,
        URI.create("https://" + wikiRef.getName() + ".example"));
    expect(getMock(WikiDescriptorService.class).getDescriptors(wikiRef))
        .andReturn(List.of(descriptor));
  }

}
