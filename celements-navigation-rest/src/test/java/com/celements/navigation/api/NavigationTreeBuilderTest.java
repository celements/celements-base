package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.navigation.TreeNode;
import com.celements.navigation.filter.InternalRightsFilter;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.parents.IDocumentParentsListerRole;

public class NavigationTreeBuilderTest extends AbstractComponentTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);
  private ITreeNodeService treeService;
  private IDocumentParentsListerRole parentsLister;
  private DefaultNavigationNodeValueResolver values;
  private NavigationTreeBuilder builder;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(ITreeNodeService.class, IDocumentParentsListerRole.class,
        DefaultNavigationNodeValueResolver.class);
    treeService = getMock(ITreeNodeService.class);
    parentsLister = getMock(IDocumentParentsListerRole.class);
    values = getMock(DefaultNavigationNodeValueResolver.class);
    var beanFactory = (DefaultListableBeanFactory) getBeanFactory();
    beanFactory.destroySingleton(NavigationTreeBuilder.class.getName());
    beanFactory.registerResolvableDependency(ITreeNodeService.class, treeService);
    beanFactory.registerResolvableDependency(IDocumentParentsListerRole.class, parentsLister);
    beanFactory.registerResolvableDependency(DefaultNavigationNodeValueResolver.class, values);
    builder = getBeanFactory().getBean(NavigationTreeBuilder.class.getName(),
        NavigationTreeBuilder.class);
  }

  @Test
  public void test_build_groupsAndOrdersSegmentsWithoutReorderingNodes() {
    var unnamed = node("Unnamed", null, 1);
    var mainSecond = node("MainSecond", "main", 2);
    var upperCase = node("Upper", "A", 1);
    var lowerCase = node("Lower", "a", 1);
    var mainFirst = node("MainFirst", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(unnamed, mainSecond, upperCase, lowerCase, mainFirst));
    for (var node : List.of(unnamed, mainSecond, upperCase, lowerCase, mainFirst)) {
      expectNode(node, List.of());
    }
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), 0));
    verifyDefault();
    assertEquals(4, response.segments().size());
    assertNull(response.segments().get(0).partName());
    assertEquals("A", response.segments().get(1).partName());
    assertEquals("a", response.segments().get(2).partName());
    assertEquals("main", response.segments().get(3).partName());
    assertEquals(List.of("Content.MainSecond", "Content.MainFirst"),
        response.segments().get(3).nodes().stream().map(NavigationNodeDto::docRef).toList());
    assertTrue(response.segments().get(0).nodes().get(0).isLeaf());
    assertFalse(response.segments().get(0).nodes().get(0).isOpen());
  }

  @Test
  public void test_build_expandsActiveAncestorsNodeAndDirectChildren() {
    var root = node("Root", "main", 1);
    var active = childNode("Active", root, 1);
    var child = childNode("Child", active, 1);
    var grandchild = childNode("Grandchild", child, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectNode(root, List.of(active));
    expectNode(active, List.of(child));
    expectNode(child, List.of(grandchild));
    expectNode(grandchild, List.of());
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference(), root.getDocumentReference()));
    replayDefault();
    var response = builder.build(
        request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0));
    verifyDefault();
    var rootDto = response.segments().get(0).nodes().get(0);
    var activeDto = rootDto.children().get(0);
    var childDto = activeDto.children().get(0);
    assertTrue(rootDto.isOpen());
    assertFalse(rootDto.isActive());
    assertTrue(activeDto.isOpen());
    assertTrue(activeDto.isActive());
    assertEquals(1, activeDto.children().size());
    assertFalse(childDto.isOpen());
    assertTrue(childDto.children().isEmpty());
    assertFalse(childDto.isLeaf());
  }

  @Test
  public void test_build_combinesActivePathAndInactiveThresholdExpansion() {
    var activeRoot = node("ActiveRoot", "main", 1);
    var active = childNode("Active", activeRoot, 1);
    var activeChild = childNode("ActiveChild", active, 1);
    var activeGrandchild = childNode("ActiveGrandchild", activeChild, 1);
    var inactiveRoot = node("InactiveRoot", "main", 2);
    var inactiveChild = childNode("InactiveChild", inactiveRoot, 1);
    var inactiveGrandchild = childNode("InactiveGrandchild", inactiveChild, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(activeRoot, inactiveRoot));
    expectNode(activeRoot, List.of(active));
    expectNode(active, List.of(activeChild));
    expectNode(activeChild, List.of(activeGrandchild));
    expectNode(activeGrandchild, List.of());
    expectNode(inactiveRoot, List.of(inactiveChild));
    expectNode(inactiveChild, List.of(inactiveGrandchild));
    expectNode(inactiveGrandchild, List.of());
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference(), activeRoot.getDocumentReference()));
    replayDefault();
    var response = builder.build(
        request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 2));
    verifyDefault();
    var activeRootDto = response.segments().get(0).nodes().get(0);
    var activeDto = activeRootDto.children().get(0);
    var activeChildDto = activeDto.children().get(0);
    var inactiveRootDto = response.segments().get(0).nodes().get(1);
    var inactiveChildDto = inactiveRootDto.children().get(0);
    assertTrue(activeRootDto.isOpen());
    assertTrue(activeDto.isOpen());
    assertTrue(activeDto.isActive());
    assertFalse(activeChildDto.isOpen());
    assertTrue(activeChildDto.children().isEmpty());
    assertTrue(inactiveRootDto.isOpen());
    assertFalse(inactiveChildDto.isOpen());
    assertTrue(inactiveChildDto.children().isEmpty());
  }

  @Test
  public void test_build_appliesInactiveExpansionThresholds() {
    assertEquals(1, expandedDepth(0));
    assertEquals(1, expandedDepth(1));
    assertEquals(2, expandedDepth(2));
    assertEquals(3, expandedDepth(3));
    assertEquals(4, expandedDepth(100));
  }

  @Test
  public void test_build_unfilteredEmptyNodeSpaceReturnsEmptySegments() {
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), 0));
    verifyDefault();
    assertTrue(response.segments().isEmpty());
  }

  @Test
  public void test_build_omittedInvalidChildDoesNotDiscloseNonLeafState() {
    var root = node("Root", "main", 1);
    var invalid = childNode("HiddenByInvalidTitle", root, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(root);
    expect(treeService.getSubNodesForParent(eq(root.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(List.of(invalid));
    expect(values.serialize(invalid.getDocumentReference())).andReturn(local(invalid));
    expect(values.resolveTitle(invalid.getDocumentReference(), "de")).andReturn(" ");
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), 100));
    verifyDefault();
    var rootDto = response.segments().get(0).nodes().get(0);
    assertTrue(rootDto.isLeaf());
    assertFalse(rootDto.isOpen());
    assertTrue(rootDto.children().isEmpty());
  }

  @Test
  public void test_build_omitsNullAndBlankMandatoryNodeValues() {
    for (String field : List.of("docRef", "title", "url")) {
      assertInvalidNodeOmitted(field, null);
      assertInvalidNodeOmitted(field, " ");
    }
  }

  @Test
  public void test_build_omittedActiveNodeReturnsNotFound() {
    var active = node("InvalidActive", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(active));
    expect(values.serialize(active.getDocumentReference())).andReturn(local(active));
    expect(values.resolveTitle(active.getDocumentReference(), "de")).andReturn(null);
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference()));
    replayDefault();
    var exception = assertThrows(NavigationApiException.class,
        () -> builder.build(
            request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0)));
    verifyDefault();
    assertEquals("navigation_node_not_found", exception.code());
  }

  @Test
  public void test_build_omitsInactiveNodeWithNullChildren() {
    var root = node("InvalidRoot", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(root);
    expect(treeService.getSubNodesForParent(eq(root.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(null);
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), 100));
    verifyDefault();
    assertTrue(response.segments().isEmpty());
  }

  @Test
  public void test_build_activeNodeWithNullChildrenReturnsNotFound() {
    var active = node("InvalidActive", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(active));
    expectValues(active);
    expect(treeService.getSubNodesForParent(eq(active.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(null);
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference()));
    replayDefault();
    var exception = assertThrows(NavigationApiException.class,
        () -> builder.build(
            request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0)));
    verifyDefault();
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("navigation_node_not_found", exception.code());
  }

  @Test
  public void test_build_inaccessibleCurrentNodeReturnsSafeNotFound() {
    var current = node("Restricted", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    expect(parentsLister.getDocumentParentsList(current.getDocumentReference(), true))
        .andReturn(List.of(current.getDocumentReference()));
    replayDefault();
    assertNodeNotFound(request(Optional.of(current.getDocumentReference()),
        Optional.of(local(current)), 0));
  }

  @Test
  public void test_build_outOfRootCurrentNodeReturnsSafeNotFound() {
    var root = node("Root", "main", 1);
    var otherSpace = new SpaceReference("Other", wikiRef);
    var current = new DocumentReference("Current", otherSpace);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectNode(root, List.of());
    expect(parentsLister.getDocumentParentsList(current, true)).andReturn(List.of(current));
    replayDefault();
    assertNodeNotFound(request(Optional.of(current), Optional.of("Other.Current"), 0));
  }

  @Test
  public void test_build_partExcludedCurrentNodeReturnsSafeNotFound() {
    var current = node("OtherPart", "other", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andAnswer(() -> {
          var filter = (InternalRightsFilter) getCurrentArguments()[1];
          return "main".equals(filter.getMenuPart()) ? List.of() : List.of(current);
        });
    expect(parentsLister.getDocumentParentsList(current.getDocumentReference(), true))
        .andReturn(List.of(current.getDocumentReference()));
    replayDefault();
    var request = new NavigationRequest(spaceRef, "Content",
        Optional.of(current.getDocumentReference()), Optional.of(local(current)), "de",
        Optional.of("main"), 0);
    assertNodeNotFound(request);
  }

  @Test
  public void test_build_requestedPartAlwaysReturnsExactlyOneSegment() {
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    replayDefault();
    var response = builder.build(new NavigationRequest(spaceRef, "Content", Optional.empty(),
        Optional.empty(), "de", Optional.of("Main"), 0));
    verifyDefault();
    assertEquals(1, response.segments().size());
    assertEquals("Main", response.segments().get(0).partName());
    assertTrue(response.segments().get(0).nodes().isEmpty());
  }

  @Test
  public void test_build_partFilterIsCaseSensitive() {
    var lowerCasePart = node("Root", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(lowerCasePart));
    expectNode(lowerCasePart, List.of());
    replayDefault();
    var response = builder.build(new NavigationRequest(spaceRef, "Content", Optional.empty(),
        Optional.empty(), "de", Optional.of("Main"), 0));
    verifyDefault();
    assertEquals("Main", response.segments().get(0).partName());
    assertTrue(response.segments().get(0).nodes().isEmpty());
  }

  private void assertInvalidNodeOmitted(String field, String invalidValue) {
    var root = node("Root", "main", 1);
    var invalid = childNode("Invalid", root, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(root);
    expect(treeService.getSubNodesForParent(eq(root.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(List.of(invalid));
    expect(values.serialize(invalid.getDocumentReference()))
        .andReturn("docRef".equals(field) ? invalidValue : local(invalid));
    if (!"docRef".equals(field)) {
      expect(values.resolveTitle(invalid.getDocumentReference(), "de"))
          .andReturn("title".equals(field) ? invalidValue : "Invalid");
    }
    if ("url".equals(field)) {
      expect(values.resolveUrl(invalid.getDocumentReference(), "de")).andReturn(invalidValue);
    }
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), 100));
    verifyDefault();
    var rootDto = response.segments().get(0).nodes().get(0);
    assertTrue(rootDto.isLeaf());
    assertTrue(rootDto.children().isEmpty());
    resetDefault();
  }

  private void assertNodeNotFound(NavigationRequest request) {
    var exception = assertThrows(NavigationApiException.class,
        () -> builder.build(request));
    verifyDefault();
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("navigation_node_not_found", exception.code());
  }

  private int expandedDepth(int threshold) {
    var level1 = node("Level1", "main", 1);
    var level2 = childNode("Level2", level1, 1);
    var level3 = childNode("Level3", level2, 1);
    var level4 = childNode("Level4", level3, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(level1));
    expectNode(level1, List.of(level2));
    expectNode(level2, List.of(level3));
    expectNode(level3, List.of(level4));
    expectNode(level4, List.of());
    replayDefault();
    var response = builder.build(request(Optional.empty(), Optional.empty(), threshold));
    verifyDefault();
    int depth = 1;
    var current = response.segments().get(0).nodes().get(0);
    while (!current.children().isEmpty()) {
      current = current.children().get(0);
      depth++;
    }
    resetDefault();
    return depth;
  }

  private void expectNode(TreeNode node, List<TreeNode> children) {
    expectValues(node);
    expect(treeService.getSubNodesForParent(eq(node.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(children);
  }

  private void expectValues(TreeNode node) {
    expect(values.serialize(node.getDocumentReference())).andReturn(local(node));
    expect(values.resolveTitle(node.getDocumentReference(), "de"))
        .andReturn(node.getDocumentReference().getName());
    expect(values.resolveUrl(node.getDocumentReference(), "de"))
        .andReturn("/Content/" + node.getDocumentReference().getName() + "?language=de");
  }

  private NavigationRequest request(Optional<DocumentReference> currentNode,
      Optional<String> serializedCurrentNode, int threshold) {
    return new NavigationRequest(spaceRef, "Content", currentNode, serializedCurrentNode, "de",
        Optional.empty(), threshold);
  }

  private TreeNode node(String name, String partName, int position) {
    return new TreeNode(new DocumentReference(name, spaceRef), null, position, partName);
  }

  private TreeNode childNode(String name, TreeNode parent, int position) {
    return new TreeNode(new DocumentReference(name, spaceRef), parent.getDocumentReference(),
        position, parent.getPartName());
  }

  private String local(TreeNode node) {
    return "Content." + node.getDocumentReference().getName();
  }

}
