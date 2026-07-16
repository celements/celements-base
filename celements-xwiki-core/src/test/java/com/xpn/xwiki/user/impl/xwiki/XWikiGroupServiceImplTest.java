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

import java.util.Arrays;
import java.util.HashSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiGroupServiceImplTest extends AbstractComponentTest {

  XWikiGroupServiceImpl groupService;

  private XWikiDocument user;

  private XWikiDocument userWithSpaces;

  private XWikiDocument group;
  private BaseObject groupObject;

  @Before
  public void prepareTest() throws Exception {
    groupService = new XWikiGroupServiceImpl();

    user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
    getContext().setDatabase(this.user.getWikiName());
    BaseObject userObject = new BaseObject();
    userObject.setClassName("XWiki.XWikiUser");
    this.user.addXObject(userObject);
    userWithSpaces = new XWikiDocument(
        new DocumentReference("wiki", "XWiki", "user with spaces"));
    getContext().setDatabase(this.userWithSpaces.getWikiName());
    BaseObject userWithSpacesObject = new BaseObject();
    userWithSpacesObject.setClassName("XWiki.XWikiUser");
    this.userWithSpaces.addXObject(userWithSpacesObject);
    group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
    getContext().setDatabase(this.group.getWikiName());
    this.groupObject = new BaseObject();
    this.groupObject.setClassName("XWiki.XWikiGroups");
    this.groupObject.setStringValue("member", this.user.getFullName());
    this.group.addXObject(this.groupObject);
    expect(getWikiMock().isVirtualMode()).andReturn(true).anyTimes();
    expect(getWikiMock().isReadOnly()).andReturn(false).anyTimes();
    expect(getWikiMock().getWikiOwner(anyString(), same(getContext()))).andReturn(null).anyTimes();
    expect(getWikiMock().getMaxRecursiveSpaceChecks(same(getContext()))).andReturn(0).anyTimes();
    expect(getWikiMock().getDocument(anyString(), eq("WebPreferences"), same(getContext())))
        .andAnswer(() -> new XWikiDocument(new DocumentReference(getContext().getDatabase(),
            (String) getCurrentArgument(0), "WebPreferences"))).anyTimes();
    expect(getWikiMock().getDocument(anyString(), same(getContext()))).andAnswer(() -> {
      String name = getCurrentArgument(0);
      if (name.equals(user.getPrefixedFullName())) {
        return user;
      } else if (name.equals(userWithSpaces.getPrefixedFullName())) {
        return userWithSpaces;
      }
      return group;
    }).anyTimes();
    replayDefault();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  @Test
  public void test_listMemberForGroup() throws XWikiException {
    assertEquals(new HashSet<>(Arrays.asList(this.user.getFullName())),
        new HashSet<>(this.groupService
            .listMemberForGroup(this.group.getFullName(), getContext())));

    this.groupObject.setStringValue("member", this.userWithSpaces.getFullName());

    assertEquals(new HashSet<>(Arrays.asList(this.userWithSpaces.getFullName())),
        new HashSet<>(this.groupService
            .listMemberForGroup(this.group.getFullName(), getContext())));
  }
}
