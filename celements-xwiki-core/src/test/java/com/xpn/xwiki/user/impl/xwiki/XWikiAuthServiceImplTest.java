/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.user.impl.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.security.Principal;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl}.
 *
 * @version $Id$
 */
public class XWikiAuthServiceImplTest extends AbstractComponentTest {

  private XWikiAuthServiceImpl authService;
  private DocumentReference userClassDocRef;

  @Before
  public void setUp() throws Exception {
    this.authService = new XWikiAuthServiceImpl();
    userClassDocRef = new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiUsers");
    BaseClass userClass = new BaseClass();
    userClass.setDocumentReference(userClassDocRef);
    userClass.addPasswordField("password", "Password", 10);
    expect(getMock(XWiki.class).getUserClass(getContext())).andReturn(userClass).anyTimes();
    getXWikiCfg().setProperty("xwiki.virtual", "1");
  }

  /**
   * Test that it's not possible to log in with a superadmin user when the superadmin password
   * configuration is turned
   * off.
   */
  @Test
  public void testAuthenticateWithSuperAdminWhenSuperAdminPasswordIsTurnedOff() throws Exception {
    expect(getMock(XWiki.class).Param("xwiki.superadminpassword")).andReturn(null);
    replayDefault();
    Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER,
        "whatever", getContext());
    verifyDefault();
    assertNull(principal);
  }

  /**
   * Test that it's not possible to log in with a superadmin user when the superadmin password
   * configuration is turned
   * off.
   */
  @Test
  public void testAuthenticateWithSuperAdminPrefixedWithXWikiWhenSuperAdminPasswordIsTurnedOff()
      throws Exception {
    expect(getMock(XWiki.class).Param("xwiki.superadminpassword")).andReturn(null);
    replayDefault();
    Principal principal = this.authService.authenticate(XWikiRightService.SUPERADMIN_USER_FULLNAME,
        "whatever", getContext());
    verifyDefault();
    assertNull(principal);
  }

  @Test
  public void testAuthenticateWithSuperAdminWithWhiteSpacesWhenSuperAdminPasswordIsTurnedOff()
      throws Exception {
    expect(getMock(XWiki.class).Param("xwiki.superadminpassword")).andReturn(null);
    replayDefault();
    Principal principal = authService.authenticate(" " + XWikiRightService.SUPERADMIN_USER + " ",
        "whatever", getContext());
    verifyDefault();
    assertNull(principal);
  }

  /**
   * Test that superadmin is authenticated as superadmin whatever the case.
   */
  @Test
  public void testAuthenticateWithSuperAdminWithDifferentCase() throws Exception {
    expect(getMock(XWiki.class).Param("xwiki.superadminpassword")).andReturn("pass");
    replayDefault();
    Principal principal = authService.authenticate(XWikiRightService.SUPERADMIN_USER.toUpperCase(),
        "pass", getContext());
    verifyDefault();
    assertNotNull(principal);
    assertEquals(XWikiRightService.SUPERADMIN_USER_FULLNAME, principal.getName());
  }

  /**
   * Test that SomeUser is correctly authenticated as XWiki.SomeUser when xwiki:SomeUser is entered
   * as username.
   */
  @Test
  public void testLoginWithWikiPrefix() throws Exception {
    // Setup a simple user profile document
    XWikiDocument userDoc = new XWikiDocument("XWiki", "SomeUser");
    BaseObject userObj = new BaseObject();
    userObj.setXClassReference(userClassDocRef);
    userObj.setStringValue("password", "pass");
    userDoc.addXObject(userObj);

    // Make a simple XWiki.XWikiUsers class that will contain a default password field
    BaseClass userClass = new BaseClass();
    userClass.addPasswordField("password", "Password", 20);
    userClass.setClassName("XWiki.XWikiUsers");

    // Prepare the XWiki mock
    expect(getMock(XWiki.class).exists(userDoc.getDocRef(), getContext()))
        .andReturn(true);
    expect(getMock(XWiki.class).getDocument("XWiki.SomeUser", getContext())).andReturn(userDoc);

    replayDefault();
    // Finally run the test: Using xwiki:Admin should correctly authenticate the Admin user
    Principal principal = this.authService.authenticate("xwiki:SomeUser", "pass",
        this.getContext());
    verifyDefault();
    assertNotNull(principal);
    assertEquals("xwiki:XWiki.SomeUser", principal.getName());
  }

  /**
   * Test that user is authenticated with a global account when a local one with the same name
   * exists and the username
   * contains a wiki prefix.
   */
  @Test
  public void testLogintoVirtualXwikiWithWikiPrefixUsername() throws Exception {
    // Setup simple user profile documents
    XWikiDocument userDocLocal = new XWikiDocument("local", "XWiki", "Admin");

    // Make a simple XWiki.XWikiUsers class that will contain a default password field
    BaseClass userClass = new BaseClass();
    userClass.addPasswordField("password", "Password", 20);
    userClass.setClassName("XWiki.XWikiUsers");

    BaseObject userObj = new BaseObject();
    userObj.setXClassReference(userClassDocRef);
    userObj.setStringValue("password", "admin");
    userDocLocal.addXObject(userObj);

    // Prepare the XWiki mock for local
    expect(getMock(XWiki.class).exists(anyObject(DocumentReference.class), same(getContext())))
        .andReturn(true).times(2);
    expect(getMock(XWiki.class).getDocument("XWiki.Admin", getContext()))
        .andReturn(userDocLocal).times(2);

    replayDefault();
    // Run the test: Using Xwiki.Admin should correctly authenticate the Admin user
    Principal principalLocal = this.authService.authenticate("XWiki.Admin", "admin",
        this.getContext());
    // Set the database name to local.
    this.getContext().setDatabase("local");
    // Finally run the test: Using xwiki:Xwiki.Admin should correctly authenticate the Admin user
    Principal principalVirtual = this.authService.authenticate("xwiki:XWiki.Admin", "admin",
        this.getContext());
    verifyDefault();

    assertNotNull(principalLocal);
    assertEquals("XWiki.Admin", principalLocal.getName());
    assertNotNull(principalVirtual);
    assertEquals("xwiki:XWiki.Admin", principalVirtual.getName());
  }

  @Test
  public void testStripContextPathFromURLWithSlashAfter() throws Exception {
    expect(getMock(XWiki.class).getWebAppPath(getContext())).andReturn("xwiki/");

    replayDefault();
    assertEquals("/something", authService.stripContextPathFromURL(
        new URL("http://localhost:8080/xwiki/something"), getContext()));
    verifyDefault();
  }

  @Test
  public void testStripContextPathFromURLWithSlashBefore() throws Exception {
    expect(getMock(XWiki.class).getWebAppPath(getContext())).andReturn("/xwiki");

    replayDefault();
    assertEquals("/something", authService.stripContextPathFromURL(
        new URL("http://localhost:8080/xwiki/something"), getContext()));
    verifyDefault();
  }
}
