package com.celements.navigation.api;

import static com.celements.rights.access.EAccessLevel.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.easymock.IAnswer;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.navigation.TreeNode;
import com.celements.navigation.filter.InternalRightsFilter;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.parents.IDocumentParentsListerRole;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;

public class NavigationRightsFilteringTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);
  private final List<InternalRightsFilter> observedFilters = new ArrayList<>();

  @Test
  public void guestRightsAreAppliedAtRootAndNestedLevelsWithoutLeafDisclosure() throws Exception {
    var response = buildForCaller("XWiki.XWikiGuest", false);
    var root = response.segments().get(0).nodes().get(0);
    assertTrue(root.isLeaf());
    assertTrue(root.children().isEmpty());
    assertEquals(2, observedFilters.size());
    assertEquals("main", observedFilters.get(0).getMenuPart());
    assertTrue(observedFilters.get(1).getMenuPart().isEmpty());
  }

  @Test
  public void authenticatedRightsAreAppliedAtRootAndEveryReturnedNestedLevel() throws Exception {
    var response = buildForCaller("XWiki.Authenticated", true);
    var root = response.segments().get(0).nodes().get(0);
    assertFalse(root.isLeaf());
    assertEquals(1, root.children().size());
    assertTrue(root.children().get(0).isLeaf());
    assertEquals(3, observedFilters.size());
    assertSame(observedFilters.get(1), observedFilters.get(2));
    assertEquals("main", observedFilters.get(0).getMenuPart());
    assertTrue(observedFilters.get(1).getMenuPart().isEmpty());
  }

  private NavigationTreeResponse buildForCaller(String userName, boolean childVisible)
      throws Exception {
    var rootNode = new TreeNode(new DocumentReference("Root", spaceRef), null, 1, "main");
    var childNode = new TreeNode(new DocumentReference("Restricted", spaceRef),
        rootNode.getDocumentReference(), 1, "main");
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    IRightsAccessFacadeRole rightsAccess = createMock(IRightsAccessFacadeRole.class);
    ComponentManager componentManager = createMock(ComponentManager.class);
    XWikiContext xwikiContext = createMock(XWikiContext.class);
    var xwikiUser = new XWikiUser(userName);
    expect(componentManager.lookup(IRightsAccessFacadeRole.class, "default"))
        .andReturn(rightsAccess).anyTimes();
    expect(xwikiContext.getXWikiUser()).andReturn(xwikiUser).anyTimes();
    expect(
        rightsAccess.hasAccessLevel(eq(rootNode.getDocumentReference()), eq(VIEW), same(xwikiUser)))
        .andReturn(true);
    expect(rightsAccess.hasAccessLevel(eq(childNode.getDocumentReference()), eq(VIEW),
        same(xwikiUser))).andReturn(childVisible);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andAnswer(rightsFiltered(List.of(rootNode), xwikiContext));
    expectValues(values, rootNode);
    expect(treeService.getSubNodesForParent(eq(rootNode.getDocumentReference()),
        isA(InternalRightsFilter.class)))
        .andAnswer(rightsFiltered(List.of(childNode), xwikiContext));
    if (childVisible) {
      expectValues(values, childNode);
      expect(treeService.getSubNodesForParent(eq(childNode.getDocumentReference()),
          isA(InternalRightsFilter.class))).andAnswer(rightsFiltered(List.of(), xwikiContext));
    }
    replay(treeService, parentsLister, values, rightsAccess, componentManager, xwikiContext);
    ComponentManager previousComponentManager = previousComponentManager();
    Utils.setComponentManager(componentManager);
    try {
      var response = new NavigationTreeBuilder(treeService, parentsLister, values)
          .build(new NavigationRequest(spaceRef, "Content", Optional.empty(), Optional.empty(),
              "de", Optional.of("main"), 100));
      verify(treeService, parentsLister, values, rightsAccess, componentManager, xwikiContext);
      return response;
    } finally {
      Utils.setComponentManager(previousComponentManager);
    }
  }

  private ComponentManager previousComponentManager() {
    try {
      return Utils.getComponentManager();
    } catch (IllegalStateException exception) {
      return null;
    }
  }

  private IAnswer<List<TreeNode>> rightsFiltered(List<TreeNode> nodes, XWikiContext xwikiContext) {
    return () -> {
      var filter = (InternalRightsFilter) getCurrentArguments()[1];
      observedFilters.add(filter);
      return nodes.stream().filter(node -> filter.includeTreeNode(node, xwikiContext)).toList();
    };
  }

  private void expectValues(DefaultNavigationNodeValueResolver values, TreeNode node) {
    String localRef = "Content." + node.getDocumentReference().getName();
    expect(values.serialize(node.getDocumentReference())).andReturn(localRef);
    expect(values.resolveTitle(node.getDocumentReference(), "de"))
        .andReturn(node.getDocumentReference().getName());
    expect(values.resolveUrl(node.getDocumentReference(), "de"))
        .andReturn("/Content/" + node.getDocumentReference().getName() + "?language=de");
  }

}
