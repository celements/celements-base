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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightNotFoundException;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 *
 * @version $Id$
 */
public class XWikiRightServiceImplTest extends AbstractComponentTest {

  private XWikiRightServiceImpl rightService;

  private XWikiDocument user;

  private XWikiDocument group;

  private XWikiDocument group2;

  private final Map<String, XWikiDocument> documents = new HashMap<>();

  private boolean wikiOwnerEnabled;

  @Before
  public void prepareTest() throws Exception {
    rightService = new XWikiRightServiceImpl();
    XWikiGroupService groupService = createDefaultMock(XWikiGroupService.class);
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);

    user = new XWikiDocument(new DocumentReference("wiki", "XWiki", "user"));
    this.user.setNew(false);
    getContext().setDatabase(this.user.getWikiName());
    BaseObject userObject = new BaseObject();
    userObject.setClassName("XWiki.XWikiUser");
    this.user.addXObject(userObject);
    documents.put(user.getPrefixedFullName(), user);

    group = new XWikiDocument(new DocumentReference("wiki", "XWiki", "group"));
    this.group.setNew(false);
    getContext().setDatabase(this.group.getWikiName());
    BaseObject groupObject = new BaseObject();
    groupObject.setClassName("XWiki.XWikiGroups");
    groupObject.setStringValue("member", this.user.getFullName());
    this.group.addXObject(groupObject);
    documents.put(group.getPrefixedFullName(), group);

    group2 = new XWikiDocument(new DocumentReference("wiki2", "XWiki", "group2"));
    this.group2.setNew(false);
    getContext().setDatabase(this.group2.getWikiName());
    BaseObject group2Object = new BaseObject();
    group2Object.setClassName("XWiki.XWikiGroups");
    group2Object.setStringValue("member", this.user.getPrefixedFullName());
    this.group2.addXObject(groupObject);
    documents.put(group2.getPrefixedFullName(), group2);

    expect(getWikiMock().isVirtualMode()).andReturn(true).anyTimes();
    expect(getWikiMock().getGroupService(same(getContext()))).andReturn(groupService).anyTimes();
    expect(getWikiMock().isReadOnly()).andReturn(false).anyTimes();
    expect(getWikiMock().getWikiOwner(anyString(), same(getContext()))).andAnswer(() ->
        wikiOwnerEnabled ? user.getPrefixedFullName() : null).anyTimes();
    expect(getWikiMock().getMaxRecursiveSpaceChecks(same(getContext()))).andReturn(0).anyTimes();
    expect(getWikiMock().getStore()).andReturn(store).anyTimes();
    expect(store.loadCelDocument(anyObject(DocumentReference.class)))
        .andAnswer(() -> {
          DocumentReference reference = getCurrentArgument(0);
          String localName = reference.getLastSpaceReference().getName() + "."
              + reference.getName();
          XWikiDocument byName = documents.get(localName);
          if (byName == null) {
            byName = documents.get(reference.getWikiReference().getName() + ":" + localName);
          }
          return java.util.stream.Stream.concat(java.util.stream.Stream.ofNullable(byName),
              documents.values().stream()
              .filter(document -> document.getDocumentReference().equals(reference))
              .limit(1))
              .findFirst()
              .map(CelDocument.Default::from);
        }).anyTimes();
    expect(getWikiMock().getXWikiPreference(anyString(), anyString(), same(getContext())))
        .andReturn("false").anyTimes();
    expect(getWikiMock().getXWikiPreferenceAsInt(anyString(), anyInt(), same(getContext())))
        .andReturn(0).anyTimes();
    expect(getWikiMock().getSpacePreference(anyString(), anyString(), same(getContext())))
        .andReturn("false").anyTimes();
    expect(getWikiMock().getSpacePreferenceAsInt(anyString(), anyInt(), same(getContext())))
        .andReturn(0).anyTimes();

    expect(groupService.getAllGroupsReferencesForMember(anyObject(DocumentReference.class),
        anyInt(), anyInt(), same(getContext()))).andAnswer(() -> {
          DocumentReference member = getCurrentArgument(0);
          if (member.equals(user.getDocumentReference())) {
            if (group.getWikiName().equals(getContext().getDatabase())) {
              return Collections.singleton(group.getDocumentReference());
            } else if (group2.getWikiName().equals(getContext().getDatabase())) {
              return Collections.singleton(group2.getDocumentReference());
            }
          }
          return Collections.emptyList();
        }).anyTimes();
    replayDefault();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  @Test
  public void test_hasProgrammingRights_doesNotCreateCelDocument() {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page")) {

      @Override
      public Map<DocumentReference, List<BaseObject>> getXObjects() {
        throw new AssertionError("Mutable document must not be converted to a CelDocument");
      }
    };
    doc.setContentAuthor(null);

    assertFalse(rightService.hasProgrammingRights(doc, getContext()));
  }

  /**
   * Test if checkRight() take care of users's groups from other wikis.
   */
  @Test
  public void test_checkRight() throws XWikiRightNotFoundException, XWikiException {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

    BaseObject globalRightObj = new BaseObject();
    globalRightObj.setStringValue("levels", "view");
    globalRightObj.setStringValue("groups", group.getPrefixedFullName());
    globalRightObj.setStringValue("users", "");
    globalRightObj.setIntValue("allow", 1);
    doc.addObject("XWiki.XWikiGlobalRights", globalRightObj);

    getContext().setDatabase("wiki2");

    assertTrue(this.user.getPrefixedFullName() + " does not have global view right on wiki2",
        this.rightService.checkRight(this.user.getPrefixedFullName(),
            CelDocument.Default.from(doc), "view", true, true, true, getContext()));
  }

  @Test
  public void test_hasAccessLevel_withUserFromAnotherWiki() throws XWikiException {
    final XWikiDocument doc = new XWikiDocument(
        new DocumentReference(this.group2.getWikiName(), "Space", "Page"));

    final XWikiDocument preferences = new XWikiDocument(
        new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
    BaseObject preferencesObject = new BaseObject();
    preferencesObject.setClassName("XWiki.XWikiGlobalRights");
    preferencesObject.setStringValue("levels", "view");
    preferencesObject.setIntValue("allow", 1);
    preferences.addXObject(preferencesObject);
    preferences.setNew(false);

    documents.put("XWiki.XWikiPreferences", preferences);
    documents.put(doc.getPrefixedFullName(), doc);

    getContext().setDatabase("wiki");

    assertFalse("User from another wiki has right on a local wiki",
        this.rightService.hasAccessLevel("view",
            this.user.getPrefixedFullName(), doc.getPrefixedFullName(), true, getContext()));

    // direct user rights

    preferencesObject.setStringValue("users", this.user.getPrefixedFullName());

    getContext().setDatabase(this.user.getWikiName());

    assertTrue(
        "User from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(),
            true,
            getContext()));

    getContext().setDatabase(doc.getWikiName());

    assertTrue(
        "User from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(),
            true,
            getContext()));

    // user group rights

    preferencesObject.removeField("users");

    // group from user's wiki

    preferencesObject.setStringValue("groups", this.group.getPrefixedFullName());

    getContext().setDatabase(this.user.getWikiName());

    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(),
            true,
            getContext()));

    getContext().setDatabase(doc.getWikiName());

    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(),
            true,
            getContext()));

    // group from document's wiki

    preferencesObject.setStringValue("groups", this.group2.getFullName());

    getContext().setDatabase(this.user.getWikiName());

    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(),
            true,
            getContext()));

    getContext().setDatabase(doc.getWikiName());

    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "User group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(),
            true,
            getContext()));

    // user is wiki owner

    preferencesObject.removeField("groups");
    wikiOwnerEnabled = true;

    getContext().setDatabase(this.user.getWikiName());

    assertTrue(
        "Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "Wiki owner group from another wiki does not have right on a local wiki when tested from user wiki",
        this.rightService.hasAccessLevel("view", this.user.getFullName(), doc.getPrefixedFullName(),
            true,
            getContext()));

    getContext().setDatabase(doc.getWikiName());

    assertTrue(
        "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(),
            doc.getPrefixedFullName(), true,
            getContext()));
    assertTrue(
        "Wiki owner group from another wiki does not have right on a local wiki when tested from local wiki",
        this.rightService.hasAccessLevel("view", this.user.getPrefixedFullName(), doc.getFullName(),
            true,
            getContext()));
  }

  @Test
  public void test_hasAccessLevel_withOnlyPageAsReference() throws XWikiException {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));

    final XWikiDocument preferences = new XWikiDocument(
        new DocumentReference(doc.getWikiName(), doc.getSpaceName(), "WebPreferences"));
    BaseObject preferencesObject = new BaseObject();
    preferencesObject.setClassName("XWiki.XWikiGlobalRights");
    preferencesObject.setStringValue("levels", "view");
    preferencesObject.setIntValue("allow", 1);
    preferences.addXObject(preferencesObject);
    preferences.setNew(false);

    documents.put(preferences.getFullName(), preferences);
    documents.put("XWiki.XWikiPreferences", new XWikiDocument(
        new DocumentReference(getContext().getDatabase(), "XWiki", "XWikiPreferences")));
    documents.put(doc.getPrefixedFullName(), doc);

    getContext().setDatabase("wiki");
    getContext().setDoc(doc);

    assertFalse("Failed to check right with only page name",
        this.rightService.hasAccessLevel("view", this.user
            .getPageName(), doc.getPageName(), true, getContext()));
  }

  @Test
  public void test_hasAccessLevel_withGuestUser() throws XWikiException {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("wiki2", "Space", "Page"));

    final XWikiDocument preferences = new XWikiDocument(
        new DocumentReference("wiki2", "XWiki", "XWikiPreference"));
    BaseObject preferencesObject = new BaseObject();
    preferencesObject.setClassName("XWiki.XWikiGlobalRights");
    preferencesObject.setStringValue("levels", "view");
    preferencesObject.setIntValue("allow", 1);
    preferences.addXObject(preferencesObject);

    documents.put("XWiki.XWikiPreferences", preferences);
    documents.put(doc.getPrefixedFullName(), doc);

    getContext().setDatabase("wiki");

    assertFalse("Guest has wiew right on the document", this.rightService.hasAccessLevel("view",
        XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), true, getContext()));

    // direct user rights

    preferencesObject.setStringValue("users", XWikiRightService.GUEST_USER_FULLNAME);

    getContext().setDatabase("wiki");

    assertTrue("Guest does not have right on the document when tested from another wiki",
        this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME,
                doc.getPrefixedFullName(), true,
                getContext()));

    getContext().setDatabase(doc.getDatabase());

    assertTrue("Guest does not have right on the document when tested from the document wiki",
        this.rightService
            .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME,
                doc.getPrefixedFullName(), true,
                getContext()));
  }
}
