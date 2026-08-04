package com.celements.navigation.api;

import static com.celements.rights.access.EAccessLevel.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.navigation.TreeNode;
import com.celements.navigation.filter.InternalRightsFilter;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.parents.IDocumentParentsListerRole;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.xpn.xwiki.user.api.XWikiUser;

public class NavigationRightsFilteringTest extends AbstractComponentTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);
  private final List<InternalRightsFilter> observedFilters = new ArrayList<>();
  private ITreeNodeService treeService;
  private IDocumentParentsListerRole parentsLister;
  private DefaultNavigationNodeValueResolver values;
  private IRightsAccessFacadeRole rightsAccess;
  private NavigationTreeBuilder builder;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(ITreeNodeService.class, IDocumentParentsListerRole.class,
        DefaultNavigationNodeValueResolver.class, IRightsAccessFacadeRole.class);
    treeService = getMock(ITreeNodeService.class);
    parentsLister = getMock(IDocumentParentsListerRole.class);
    values = getMock(DefaultNavigationNodeValueResolver.class);
    rightsAccess = getMock(IRightsAccessFacadeRole.class);
    var beanFactory = (DefaultListableBeanFactory) getBeanFactory();
    beanFactory.destroySingleton(NavigationTreeBuilder.class.getName());
    beanFactory.registerResolvableDependency(ITreeNodeService.class, treeService);
    beanFactory.registerResolvableDependency(IDocumentParentsListerRole.class, parentsLister);
    beanFactory.registerResolvableDependency(DefaultNavigationNodeValueResolver.class, values);
    builder = getBeanFactory().getBean(NavigationTreeBuilder.class.getName(),
        NavigationTreeBuilder.class);
  }

  @Test
  public void test_guestRightsAreAppliedAtRootAndNestedLevelsWithoutLeafDisclosure()
      throws Exception {
    var response = buildForCaller("XWiki.XWikiGuest", false);
    var root = response.segments().get(0).nodes().get(0);
    assertTrue(root.isLeaf());
    assertTrue(root.children().isEmpty());
    assertEquals(2, observedFilters.size());
    assertEquals("main", observedFilters.get(0).getMenuPart());
    assertTrue(observedFilters.get(1).getMenuPart().isEmpty());
  }

  @Test
  public void test_authenticatedRightsAreAppliedAtRootAndEveryReturnedNestedLevel()
      throws Exception {
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
    getXContext().setUser(userName);
    expect(
        rightsAccess.hasAccessLevel(eq(rootNode.getDocumentReference()), eq(VIEW),
            isA(XWikiUser.class)))
        .andReturn(true);
    expect(rightsAccess.hasAccessLevel(eq(childNode.getDocumentReference()), eq(VIEW),
        isA(XWikiUser.class))).andReturn(childVisible);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andAnswer(rightsFiltered(List.of(rootNode)));
    expectValues(rootNode);
    expect(treeService.getSubNodesForParent(eq(rootNode.getDocumentReference()),
        isA(InternalRightsFilter.class)))
        .andAnswer(rightsFiltered(List.of(childNode)));
    if (childVisible) {
      expectValues(childNode);
      expect(treeService.getSubNodesForParent(eq(childNode.getDocumentReference()),
          isA(InternalRightsFilter.class))).andAnswer(rightsFiltered(List.of()));
    }
    replayDefault();
    var response = builder.build(new NavigationRequest(spaceRef, "Content", Optional.empty(),
        Optional.empty(), "de", Optional.of("main"), 100));
    verifyDefault();
    return response;
  }

  private IAnswer<List<TreeNode>> rightsFiltered(List<TreeNode> nodes) {
    return () -> {
      var filter = (InternalRightsFilter) getCurrentArguments()[1];
      observedFilters.add(filter);
      return nodes.stream().filter(node -> filter.includeTreeNode(node, getXContext())).toList();
    };
  }

  private void expectValues(TreeNode node) {
    String localRef = "Content." + node.getDocumentReference().getName();
    expect(values.serialize(node.getDocumentReference())).andReturn(localRef);
    expect(values.resolveTitle(node.getDocumentReference(), "de"))
        .andReturn(node.getDocumentReference().getName());
    expect(values.resolveUrl(node.getDocumentReference(), "de"))
        .andReturn("/Content/" + node.getDocumentReference().getName() + "?language=de");
  }

}
