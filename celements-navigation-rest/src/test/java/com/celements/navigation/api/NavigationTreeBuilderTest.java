package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.navigation.TreeNode;
import com.celements.navigation.filter.InternalRightsFilter;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.parents.IDocumentParentsListerRole;

public class NavigationTreeBuilderTest {

  private final WikiReference wikiRef = new WikiReference("xwiki");
  private final SpaceReference spaceRef = new SpaceReference("Content", wikiRef);

  @Test
  public void build_groupsAndOrdersSegmentsWithoutReorderingNodes() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var unnamed = node("Unnamed", null, 1);
    var mainSecond = node("MainSecond", "main", 2);
    var upperCase = node("Upper", "A", 1);
    var lowerCase = node("Lower", "a", 1);
    var mainFirst = node("MainFirst", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(unnamed, mainSecond, upperCase, lowerCase, mainFirst));
    for (var node : List.of(unnamed, mainSecond, upperCase, lowerCase, mainFirst)) {
      expectNode(treeService, values, node, List.of());
    }
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), 0));
    verify(treeService, parentsLister, values);
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
  public void build_expandsActiveAncestorsNodeAndDirectChildren() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var root = node("Root", "main", 1);
    var active = childNode("Active", root, 1);
    var child = childNode("Child", active, 1);
    var grandchild = childNode("Grandchild", child, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectNode(treeService, values, root, List.of(active));
    expectNode(treeService, values, active, List.of(child));
    expectNode(treeService, values, child, List.of(grandchild));
    expectNode(treeService, values, grandchild, List.of());
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference(), root.getDocumentReference()));
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0));
    verify(treeService, parentsLister, values);
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
  public void build_combinesActivePathAndInactiveThresholdExpansion() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var activeRoot = node("ActiveRoot", "main", 1);
    var active = childNode("Active", activeRoot, 1);
    var activeChild = childNode("ActiveChild", active, 1);
    var activeGrandchild = childNode("ActiveGrandchild", activeChild, 1);
    var inactiveRoot = node("InactiveRoot", "main", 2);
    var inactiveChild = childNode("InactiveChild", inactiveRoot, 1);
    var inactiveGrandchild = childNode("InactiveGrandchild", inactiveChild, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(activeRoot, inactiveRoot));
    expectNode(treeService, values, activeRoot, List.of(active));
    expectNode(treeService, values, active, List.of(activeChild));
    expectNode(treeService, values, activeChild, List.of(activeGrandchild));
    expectNode(treeService, values, activeGrandchild, List.of());
    expectNode(treeService, values, inactiveRoot, List.of(inactiveChild));
    expectNode(treeService, values, inactiveChild, List.of(inactiveGrandchild));
    expectNode(treeService, values, inactiveGrandchild, List.of());
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference(), activeRoot.getDocumentReference()));
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 2));
    verify(treeService, parentsLister, values);
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
  public void build_appliesInactiveExpansionThresholds() {
    assertEquals(1, expandedDepth(0));
    assertEquals(1, expandedDepth(1));
    assertEquals(2, expandedDepth(2));
    assertEquals(3, expandedDepth(3));
    assertEquals(4, expandedDepth(100));
  }

  @Test
  public void build_unfilteredEmptyNodeSpaceReturnsEmptySegments() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), 0));
    verify(treeService, parentsLister, values);
    assertTrue(response.segments().isEmpty());
  }

  @Test
  public void build_omittedInvalidChildDoesNotDiscloseNonLeafState() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var root = node("Root", "main", 1);
    var invalid = childNode("HiddenByInvalidTitle", root, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(values, root);
    expect(treeService.getSubNodesForParent(eq(root.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(List.of(invalid));
    expect(values.serialize(invalid.getDocumentReference())).andReturn(local(invalid));
    expect(values.resolveTitle(invalid.getDocumentReference(), "de")).andReturn(" ");
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), 100));
    verify(treeService, parentsLister, values);
    var rootDto = response.segments().get(0).nodes().get(0);
    assertTrue(rootDto.isLeaf());
    assertFalse(rootDto.isOpen());
    assertTrue(rootDto.children().isEmpty());
  }

  @Test
  public void build_omitsNullAndBlankMandatoryNodeValues() {
    for (String field : List.of("docRef", "title", "url")) {
      assertInvalidNodeOmitted(field, null);
      assertInvalidNodeOmitted(field, " ");
    }
  }

  @Test
  public void build_omittedActiveNodeReturnsNotFound() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var active = node("InvalidActive", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(active));
    expect(values.serialize(active.getDocumentReference())).andReturn(local(active));
    expect(values.resolveTitle(active.getDocumentReference(), "de")).andReturn(null);
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference()));
    replay(treeService, parentsLister, values);
    var exception = assertThrows(NavigationApiException.class,
        () -> new NavigationTreeBuilder(treeService, parentsLister, values).build(
            request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0)));
    verify(treeService, parentsLister, values);
    assertEquals("navigation_node_not_found", exception.code());
  }

  @Test
  public void build_omitsInactiveNodeWithNullChildren() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var root = node("InvalidRoot", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(values, root);
    expect(treeService.getSubNodesForParent(eq(root.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(null);
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), 100));
    verify(treeService, parentsLister, values);
    assertTrue(response.segments().isEmpty());
  }

  @Test
  public void build_activeNodeWithNullChildrenReturnsNotFound() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var active = node("InvalidActive", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(active));
    expectValues(values, active);
    expect(treeService.getSubNodesForParent(eq(active.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(null);
    expect(parentsLister.getDocumentParentsList(active.getDocumentReference(), true))
        .andReturn(List.of(active.getDocumentReference()));
    replay(treeService, parentsLister, values);
    var exception = assertThrows(NavigationApiException.class,
        () -> new NavigationTreeBuilder(treeService, parentsLister, values).build(
            request(Optional.of(active.getDocumentReference()), Optional.of(local(active)), 0)));
    verify(treeService, parentsLister, values);
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("navigation_node_not_found", exception.code());
  }

  @Test
  public void build_inaccessibleCurrentNodeReturnsSafeNotFound() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var current = node("Restricted", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    expect(parentsLister.getDocumentParentsList(current.getDocumentReference(), true))
        .andReturn(List.of(current.getDocumentReference()));
    replay(treeService, parentsLister, values);
    assertNodeNotFound(treeService, parentsLister, values,
        request(Optional.of(current.getDocumentReference()), Optional.of(local(current)), 0));
  }

  @Test
  public void build_outOfRootCurrentNodeReturnsSafeNotFound() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var root = node("Root", "main", 1);
    var otherSpace = new SpaceReference("Other", wikiRef);
    var current = new DocumentReference("Current", otherSpace);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectNode(treeService, values, root, List.of());
    expect(parentsLister.getDocumentParentsList(current, true)).andReturn(List.of(current));
    replay(treeService, parentsLister, values);
    assertNodeNotFound(treeService, parentsLister, values,
        request(Optional.of(current), Optional.of("Other.Current"), 0));
  }

  @Test
  public void build_partExcludedCurrentNodeReturnsSafeNotFound() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var current = node("OtherPart", "other", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andAnswer(() -> {
          var filter = (InternalRightsFilter) getCurrentArguments()[1];
          return "main".equals(filter.getMenuPart()) ? List.of() : List.of(current);
        });
    expect(parentsLister.getDocumentParentsList(current.getDocumentReference(), true))
        .andReturn(List.of(current.getDocumentReference()));
    replay(treeService, parentsLister, values);
    var request = new NavigationRequest(spaceRef, "Content",
        Optional.of(current.getDocumentReference()), Optional.of(local(current)), "de",
        Optional.of("main"), 0);
    assertNodeNotFound(treeService, parentsLister, values, request);
  }

  @Test
  public void build_requestedPartAlwaysReturnsExactlyOneSegment() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of());
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(new NavigationRequest(spaceRef, "Content", Optional.empty(), Optional.empty(), "de",
            Optional.of("Main"), 0));
    verify(treeService, parentsLister, values);
    assertEquals(1, response.segments().size());
    assertEquals("Main", response.segments().get(0).partName());
    assertTrue(response.segments().get(0).nodes().isEmpty());
  }

  @Test
  public void build_partFilterIsCaseSensitive() {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var lowerCasePart = node("Root", "main", 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(lowerCasePart));
    expectNode(treeService, values, lowerCasePart, List.of());
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(new NavigationRequest(spaceRef, "Content", Optional.empty(), Optional.empty(), "de",
            Optional.of("Main"), 0));
    verify(treeService, parentsLister, values);
    assertEquals("Main", response.segments().get(0).partName());
    assertTrue(response.segments().get(0).nodes().isEmpty());
  }

  private void assertInvalidNodeOmitted(String field, String invalidValue) {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var root = node("Root", "main", 1);
    var invalid = childNode("Invalid", root, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(root));
    expectValues(values, root);
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
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), 100));
    verify(treeService, parentsLister, values);
    var rootDto = response.segments().get(0).nodes().get(0);
    assertTrue(rootDto.isLeaf());
    assertTrue(rootDto.children().isEmpty());
  }

  private void assertNodeNotFound(ITreeNodeService treeService,
      IDocumentParentsListerRole parentsLister, DefaultNavigationNodeValueResolver values,
      NavigationRequest request) {
    var exception = assertThrows(NavigationApiException.class,
        () -> new NavigationTreeBuilder(treeService, parentsLister, values).build(request));
    verify(treeService, parentsLister, values);
    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("navigation_node_not_found", exception.code());
  }

  private int expandedDepth(int threshold) {
    ITreeNodeService treeService = createMock(ITreeNodeService.class);
    IDocumentParentsListerRole parentsLister = createMock(IDocumentParentsListerRole.class);
    DefaultNavigationNodeValueResolver values = createMock(DefaultNavigationNodeValueResolver.class);
    var level1 = node("Level1", "main", 1);
    var level2 = childNode("Level2", level1, 1);
    var level3 = childNode("Level3", level2, 1);
    var level4 = childNode("Level4", level3, 1);
    expect(treeService.getSubNodesForParent(eq(spaceRef), isA(InternalRightsFilter.class)))
        .andReturn(List.of(level1));
    expectNode(treeService, values, level1, List.of(level2));
    expectNode(treeService, values, level2, List.of(level3));
    expectNode(treeService, values, level3, List.of(level4));
    expectNode(treeService, values, level4, List.of());
    replay(treeService, parentsLister, values);
    var response = new NavigationTreeBuilder(treeService, parentsLister, values)
        .build(request(Optional.empty(), Optional.empty(), threshold));
    verify(treeService, parentsLister, values);
    int depth = 1;
    var current = response.segments().get(0).nodes().get(0);
    while (!current.children().isEmpty()) {
      current = current.children().get(0);
      depth++;
    }
    return depth;
  }

  private void expectNode(ITreeNodeService treeService, DefaultNavigationNodeValueResolver values,
      TreeNode node, List<TreeNode> children) {
    expectValues(values, node);
    expect(treeService.getSubNodesForParent(eq(node.getDocumentReference()),
        isA(InternalRightsFilter.class))).andReturn(children);
  }

  private void expectValues(DefaultNavigationNodeValueResolver values, TreeNode node) {
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
