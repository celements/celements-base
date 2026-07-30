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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */
package com.xpn.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiRightService;

public class XWikiTest extends AbstractComponentTest {

  private XWiki xwiki;
  private XWikiAuthService authService;
  private XWikiRightService rightService;

  @Before
  public void prepareTest() throws XWikiException {
    xwiki = new XWiki(false);
    authService = createDefaultMock(XWikiAuthService.class);
    rightService = createDefaultMock(XWikiRightService.class);
    xwiki.setAuthService(authService);
    xwiki.setRightService(rightService);
    getContext().setWiki(xwiki);
  }

  @Test
  public void checkAccess_fileResources_public() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "resources", "WebHome"));
    expect(authService.checkAuth(same(getContext()))).andReturn(null);
    replayDefault();
    assertTrue(xwiki.checkAccess("file", doc, getContext()));
    verifyDefault();
  }

  @Test
  public void checkAccess_fileSkins_public() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "skins", "WebHome"));
    expect(authService.checkAuth(same(getContext()))).andReturn(null);
    replayDefault();
    assertTrue(xwiki.checkAccess("file", doc, getContext()));
    verifyDefault();
  }

  @Test
  public void checkAccess_fileNonPublic_delegates() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "Content", "WebHome"));
    expect(rightService.checkAccess("file", doc, getContext())).andReturn(false);
    replayDefault();
    assertFalse(xwiki.checkAccess("file", doc, getContext()));
    verifyDefault();
  }

  @Test
  public void checkAccess_skinResources_public() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "resources", "WebHome"));
    expect(authService.checkAuth(same(getContext()))).andReturn(null);
    replayDefault();
    assertTrue(xwiki.checkAccess("skin", doc, getContext()));
    verifyDefault();
  }

  @Test
  public void checkAccess_skinSkins_public() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "skins", "WebHome"));
    expect(authService.checkAuth(same(getContext()))).andReturn(null);
    replayDefault();
    assertTrue(xwiki.checkAccess("skin", doc, getContext()));
    verifyDefault();
  }

  @Test
  public void checkAccess_skinNonPublic_delegates() throws XWikiException {
    var doc = new XWikiDocument(new DocumentReference("xwiki", "Content", "WebHome"));
    expect(rightService.checkAccess("skin", doc, getContext())).andReturn(false);
    replayDefault();
    assertFalse(xwiki.checkAccess("skin", doc, getContext()));
    verifyDefault();
  }
}
