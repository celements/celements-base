package com.xpn.xwiki.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Unit tests for {@link com.xpn.xwiki.api.User}.
 *
 * @version $Id$
 */
public class UserTest extends AbstractComponentTest {

  @Before
  public void prepareTest() throws Exception {
    XWikiDocument doc = new XWikiDocument("XWiki", "Admin");
    BaseClass userClass = new BaseClass();
    userClass.addTextField("email", "email address", 20);
    expect(getWikiMock().getXClass(anyObject(DocumentReference.class), same(getContext())))
        .andReturn(userClass).anyTimes();
    BaseObject userObj = userClass.newCustomClassInstance(getContext());
    userObj.setClassName("XWiki.XWikiUsers");
    doc.addObject("XWiki.XWikiUsers", userObj);
    userObj.setStringValue("email", "admin@mail.com");
    expect(getWikiMock().getDocument(anyString(), same(getContext()))).andReturn(doc).anyTimes();
    replayDefault();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  /**
   * Checks that XWIKI-2040 remains fixed.
   */
  @Test
  public void test_isUserInGroup_doesNotThrowNPE() {
    User u = new User(null, null);
    assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

    XWikiUser xu = new XWikiUser(null);
    u = new User(xu, null);
    assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));

    XWikiContext c = new XWikiContext();
    u = new User(xu, c);
    assertFalse(u.isUserInGroup("XWiki.InexistentGroupName"));
  }

  @Test
  public void test_getEmail() {
    User u = new User(null, null);
    assertNull(u.getEmail());

    XWikiUser xu = new XWikiUser("XWiki.Admin");
    u = new User(xu, getContext());
    assertEquals("admin@mail.com", u.getEmail());
  }
}
