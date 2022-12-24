package com.celements.rights.access;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.internal.IEntityReferenceRandomCompleterRole;
import com.celements.web.classes.oldcore.XWikiUsersClass.Type;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.web.Utils;

public class DefaultRightsAccessFacadeTest extends AbstractComponentTest {

  private XWiki xwiki;
  private XWikiContext context;
  private DefaultRightsAccessFacade rightsAccess;
  private XWikiGroupService groupSrvMock;
  private ModelContext modelContext;
  private ModelUtils modelUtils;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(UserService.class);
    context = getContext();
    xwiki = getWikiMock();
    rightsAccess = (DefaultRightsAccessFacade) Utils.getComponent(IRightsAccessFacadeRole.class);
    modelContext = Utils.getComponent(ModelContext.class);
    modelUtils = Utils.getComponent(ModelUtils.class);
    groupSrvMock = createMockAndAddToDefault(XWikiGroupService.class);
    expect(xwiki.getGroupService(same(context))).andReturn(groupSrvMock).anyTimes();
    expect(xwiki.isVirtualMode()).andReturn(true).anyTimes();
    expect(xwiki.getWikiOwner(eq(context.getDatabase()), same(context))).andReturn(
        "xwiki:XWiki.Admin").anyTimes();
    expect(xwiki.getMaxRecursiveSpaceChecks(same(context))).andReturn(0).anyTimes();
    expect(xwiki.isReadOnly()).andReturn(false).anyTimes();
  }

  @Test
  @Deprecated
  public void test_hasAccessLevel_document_edit_true_deprecated() throws Exception {
    XWikiRightService xwikiRightsService = new XWikiRightServiceImpl();
    expect(xwiki.getRightService()).andReturn(xwikiRightsService).anyTimes();
    XWikiUser user = new XWikiUser("XWiki.TestUser");
    String spaceName = "MySpace";
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    SpaceReference spaceRef = new SpaceReference(spaceName, wikiRef);
    DocumentReference docRef = new DocumentReference("MyTestDoc", spaceRef);
    XWikiDocument testDoc = new XWikiDocument(docRef);
    expect(xwiki.getDocument(eq(docRef), same(context))).andReturn(testDoc).anyTimes();
    String docFN = context.getDatabase() + ":MySpace.MyTestDoc";
    expect(xwiki.getDocument(eq(docFN), same(context))).andReturn(testDoc).anyTimes();
    prepareEmptyGroupMembers(user);
    prepareMasterRights();
    prepareSpaceRights(spaceRef);
    replayDefault();
    assertTrue(rightsAccess.hasAccessLevel("edit", user, docRef));
    verifyDefault();
  }

  @Test
  @Deprecated
  public void test_hasAccessLevel_document_edit_Guest_false_deprecated() throws Exception {
    XWikiRightService xwikiRightsService = new XWikiRightServiceImpl();
    expect(xwiki.getRightService()).andReturn(xwikiRightsService).anyTimes();
    XWikiUser user = new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME);
    String spaceName = "MySpace";
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    SpaceReference spaceRef = new SpaceReference(spaceName, wikiRef);
    DocumentReference docRef = new DocumentReference("MyTestDoc", spaceRef);
    XWikiDocument testDoc = new XWikiDocument(docRef);
    expect(xwiki.getDocument(eq(docRef), same(context))).andReturn(testDoc).anyTimes();
    String docFN = context.getDatabase() + ":MySpace.MyTestDoc";
    expect(xwiki.getDocument(eq(docFN), same(context))).andReturn(testDoc).anyTimes();
    prepareEmptyGroupMembers(user);
    prepareMasterRights();
    prepareSpaceRights(spaceRef);
    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel("edit", user, docRef));
    verifyDefault();
  }

  @Test
  @Deprecated
  public void test_hasAccessLevel_wiki_edit_false_deprecated() throws Exception {
    XWikiUser user = new XWikiUser("XWiki.TestUser");
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel("edit", user, wikiRef));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_wiki_edit_false() throws Exception {
    XWikiUser user = new XWikiUser("XWiki.TestUser");
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel(wikiRef, EAccessLevel.EDIT, user));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_docRef() throws Exception {
    String accountName = "xwikidb:XWiki.XWikiTest";
    getContext().setUser(accountName);
    expectUser(accountName);
    DocumentReference docRef = new DocumentReference(context.getDatabase(), "MySpace",
        "MyDocument");
    EAccessLevel level = EAccessLevel.EDIT;
    expect(expectRightsServiceMock().hasAccessLevel(eq(level.getIdentifier()), eq(
        getContext().getUser()), eq(modelUtils.serializeRef(docRef)), same(context))).andReturn(
            true).once();

    replayDefault();
    assertTrue(rightsAccess.hasAccessLevel(docRef, level));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_document_edit_true() throws Exception {
    XWikiRightService xwikiRightsService = new XWikiRightServiceImpl();
    expect(xwiki.getRightService()).andReturn(xwikiRightsService).anyTimes();
    XWikiUser user = new XWikiUser("XWiki.TestUser");
    String spaceName = "MySpace";
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    SpaceReference spaceRef = new SpaceReference(spaceName, wikiRef);
    DocumentReference docRef = new DocumentReference("MyTestDoc", spaceRef);
    XWikiDocument testDoc = new XWikiDocument(docRef);
    expect(xwiki.getDocument(eq(docRef), same(context))).andReturn(testDoc).anyTimes();
    String docFN = context.getDatabase() + ":MySpace.MyTestDoc";
    expect(xwiki.getDocument(eq(docFN), same(context))).andReturn(testDoc).anyTimes();
    prepareEmptyGroupMembers(user);
    prepareMasterRights();
    prepareSpaceRights(spaceRef);
    replayDefault();
    assertTrue(rightsAccess.hasAccessLevel(docRef, EAccessLevel.EDIT, user));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_document_edit_Guest_false() throws Exception {
    XWikiRightService xwikiRightsService = new XWikiRightServiceImpl();
    expect(xwiki.getRightService()).andReturn(xwikiRightsService).anyTimes();
    XWikiUser user = new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME);
    String spaceName = "MySpace";
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    SpaceReference spaceRef = new SpaceReference(spaceName, wikiRef);
    DocumentReference docRef = new DocumentReference("MyTestDoc", spaceRef);
    prepareEmptyGroupMembers(user);
    prepareMasterRights();
    prepareSpaceRights(spaceRef);
    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel(docRef, EAccessLevel.EDIT, user));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_spaceRef() throws Exception {
    String accountName = "xwikidb:XWiki.XWikiTest";
    getContext().setUser(accountName);
    expectUser(accountName);
    SpaceReference spaceRef = new SpaceReference("MySpace", modelContext.getWikiRef());
    DocumentReference docRef = new DocumentReference("untitled1", spaceRef);
    EAccessLevel level = EAccessLevel.EDIT;
    IEntityReferenceRandomCompleterRole randomCompleterMock = createMockAndAddToDefault(
        IEntityReferenceRandomCompleterRole.class);
    rightsAccess.randomCompleter = randomCompleterMock;
    expect(randomCompleterMock.randomCompleteSpaceRef(eq(spaceRef))).andReturn(docRef).once();
    expect(expectRightsServiceMock().hasAccessLevel(eq(level.getIdentifier()), eq(
        getContext().getUser()), eq(modelUtils.serializeRef(docRef)), same(context))).andReturn(
            true).once();
    prepareSpaceRights(spaceRef);
    replayDefault();
    assertTrue(rightsAccess.hasAccessLevel(spaceRef, level));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_attRef() throws Exception {
    String accountName = "xwikidb:XWiki.XWikiTest";
    getContext().setUser(accountName);
    expectUser(accountName);
    DocumentReference docRef = new DocumentReference(context.getDatabase(), "MySpace",
        "MyDocument");
    AttachmentReference attRef = new AttachmentReference("file", docRef);
    EAccessLevel level = EAccessLevel.EDIT;
    expect(expectRightsServiceMock().hasAccessLevel(eq(level.getIdentifier()), eq(
        getContext().getUser()), eq(modelUtils.serializeRef(docRef)), same(context))).andReturn(
            true).once();

    replayDefault();
    assertTrue(rightsAccess.hasAccessLevel(attRef, level));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_wikiRef() throws Exception {
    String accountName = "XWiki.XWikiTest";
    getContext().setUser(accountName);
    expectUser(accountName);
    EAccessLevel level = EAccessLevel.EDIT;

    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel(modelContext.getWikiRef(), level));
    verifyDefault();
  }

  @Test
  public void test_hasAccessLevel_user() throws Exception {
    DocumentReference docRef = new DocumentReference(context.getDatabase(), "MySpace",
        "MyDocument");
    EAccessLevel level = EAccessLevel.VIEW;
    XWikiUser user = new XWikiUser(modelUtils.serializeRef(docRef));
    expect(expectRightsServiceMock().hasAccessLevel(eq(level.getIdentifier()), eq(user.getUser()),
        eq(modelUtils.serializeRef(docRef)), same(context))).andReturn(false);

    replayDefault();
    assertFalse(rightsAccess.hasAccessLevel(docRef, level, user));
    verifyDefault();
  }

  @Deprecated
  private void prepareSpaceRights(SpaceReference spaceRef) throws XWikiException {
    String webPrefDocName = "WebPreferences";
    DocumentReference spacePrefDocRef = new DocumentReference(webPrefDocName, spaceRef);
    XWikiDocument spacePrefDoc = new XWikiDocument(spacePrefDocRef);
    spacePrefDoc.setNew(false);
    expect(xwiki.getDocument(eq(spaceRef.getName()), eq(webPrefDocName), same(context))).andReturn(
        spacePrefDoc).anyTimes();
  }

  private void prepareEmptyGroupMembers(XWikiUser user) throws XWikiException {
    DocumentReference userRef = modelUtils.resolveRef(user.getUser(), DocumentReference.class);
    if (XWikiRightService.GUEST_USER.equals(userRef.getName())) {
      expect(groupSrvMock.getAllGroupsReferencesForMember(eq(userRef), eq(0), eq(0), same(
          context))).andReturn(Collections.<DocumentReference>emptyList()).anyTimes();
    } else {
      DocumentReference xwikiAllGroupRef = new DocumentReference(context.getDatabase(), "XWiki",
          "XWikiAllGroup");
      expect(groupSrvMock.getAllGroupsReferencesForMember(eq(userRef), eq(0), eq(0), same(
          context))).andReturn(Arrays.asList(xwikiAllGroupRef)).anyTimes();
      expect(groupSrvMock.getAllGroupsReferencesForMember(eq(xwikiAllGroupRef), eq(0), eq(0), same(
          context))).andReturn(Collections.<DocumentReference>emptyList()).anyTimes();
    }
  }

  @Deprecated
  private void prepareMasterRights() throws XWikiException {
    DocumentReference wikiPrefDocRef = new DocumentReference(context.getDatabase(), "XWiki",
        "XWikiPreferences");
    XWikiDocument wikiPrefDoc = new XWikiDocument(wikiPrefDocRef);
    wikiPrefDoc.setNew(false);
    expect(xwiki.getDocument(eq("XWiki.XWikiPreferences"), same(context))).andReturn(
        wikiPrefDoc).anyTimes();
    expect(xwiki.getXWikiPreference(eq("authenticate_edit"), eq(""), same(context))).andReturn(
        "yes").anyTimes();
    expect(xwiki.getXWikiPreferenceAsInt(eq("authenticate_edit"), eq(0), same(context))).andReturn(
        1).anyTimes();
    expect(xwiki.getSpacePreference(eq("authenticate_edit"), eq(""), same(context))).andReturn(
        "").anyTimes();
    expect(xwiki.getSpacePreferenceAsInt(eq("authenticate_edit"), eq(0), same(context))).andReturn(
        0).anyTimes();
  }

  @Test
  public void test_isLoggedIn_false() throws Exception {
    context.setUser(null);
    expectUser("XWiki.XWikiGuest");
    replayDefault();
    assertFalse(rightsAccess.isLoggedIn());
    verifyDefault();
  }

  @Test
  public void test_isLoggedIn_true() throws Exception {
    String accountName = "XWiki.XWikiTest";
    context.setUser(accountName);
    expectUser(accountName);
    replayDefault();
    assertTrue(rightsAccess.isLoggedIn());
    verifyDefault();
  }

  @Test
  public void test_isAdmin_false() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, false, null);
    expect(groupSrvMock.getAllGroupsReferencesForMember(modelUtils.resolveRef(accountName,
        DocumentReference.class), 0, 0, context)).andReturn(
            Collections.<DocumentReference>emptyList());

    replayDefault();
    assertFalse(rightsAccess.isAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdmin_false_withContextDoc() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, false, false);
    expect(groupSrvMock.getAllGroupsReferencesForMember(modelUtils.resolveRef(accountName,
        DocumentReference.class), 0, 0, context)).andReturn(
            Collections.<DocumentReference>emptyList());
    context.setDoc(new XWikiDocument(new DocumentReference("xwikidb", "MySpace", "MyDoc")));

    replayDefault();
    assertFalse(rightsAccess.isAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdmin_adminRights_xwikiPref() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertTrue(rightsAccess.isAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdmin_adminRights_webPref() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, false, true);
    context.setDoc(new XWikiDocument(new DocumentReference("xwikidb", "MySpace", "MyDoc")));

    replayDefault();
    assertTrue(rightsAccess.isAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdmin_inAdminGroup() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, false, null);
    expect(groupSrvMock.getAllGroupsReferencesForMember(modelUtils.resolveRef(accountName,
        DocumentReference.class), 0, 0, context)).andReturn(Arrays.asList(
            rightsAccess.getAdminGroupDocRef()));

    replayDefault();
    assertTrue(rightsAccess.isAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isSuperAdmin() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expect(user.isGlobal()).andReturn(true);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertTrue(rightsAccess.isSuperAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isSuperAdmin_false_notGlobal() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expect(user.isGlobal()).andReturn(false);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertFalse(rightsAccess.isSuperAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isSuperAdmin_false_notAdmin() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expectHasAdminRights(accountName, false, null);
    expect(groupSrvMock.getAllGroupsReferencesForMember(modelUtils.resolveRef(accountName,
        DocumentReference.class), 0, 0, context)).andReturn(
            Collections.<DocumentReference>emptyList());

    replayDefault();
    assertFalse(rightsAccess.isSuperAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdvancedAdmin_advanced() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expect(user.isGlobal()).andReturn(false);
    expect(user.getType()).andReturn(Type.Advanced);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertTrue(rightsAccess.isAdvancedAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdvancedAdmin_global() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expect(user.isGlobal()).andReturn(true);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertTrue(rightsAccess.isAdvancedAdmin(user));
    verifyDefault();
  }

  @Test
  public void test_isAdvancedAdmin_false_neither() throws Exception {
    String accountName = "XWiki.XWikiTest";
    User user = expectUser(accountName);
    expect(user.isGlobal()).andReturn(false);
    expect(user.getType()).andReturn(Type.Simple);
    expectHasAdminRights(accountName, true, null);

    replayDefault();
    assertFalse(rightsAccess.isAdvancedAdmin(user));
    verifyDefault();
  }

  private void expectHasAdminRights(String accountName, boolean hasAdminXWikiPref,
      Boolean hasAdminWebPref) throws XWikiException {
    accountName = getContext().getDatabase() + ":" + accountName;
    XWikiRightService rightsServiceMock = expectRightsServiceMock();
    expect(rightsServiceMock.hasAccessLevel(eq("admin"), eq(accountName), eq(
        "XWiki.XWikiPreferences"), same(context))).andReturn(hasAdminXWikiPref);
    if (hasAdminWebPref != null) {
      expect(rightsServiceMock.hasAccessLevel(eq("admin"), eq(accountName), eq(
          "MySpace.WebPreferences"), same(context))).andReturn(hasAdminWebPref);
    }
  }

  private XWikiRightService expectRightsServiceMock() {
    XWikiRightService mock = createMockAndAddToDefault(XWikiRightService.class);
    expect(xwiki.getRightService()).andReturn(mock).anyTimes();
    return mock;
  }

  private User expectUser(String accountName) throws UserInstantiationException {
    DocumentReference userDocRef = modelUtils.resolveRef(accountName, DocumentReference.class);
    expect(getMock(UserService.class).resolveUserDocRef(accountName)).andReturn(
        userDocRef).anyTimes();
    User user = createMockAndAddToDefault(User.class);
    expect(getMock(UserService.class).getUser(userDocRef)).andReturn(user).anyTimes();
    expect(user.getDocRef()).andReturn(userDocRef).anyTimes();
    return user;
  }

}
