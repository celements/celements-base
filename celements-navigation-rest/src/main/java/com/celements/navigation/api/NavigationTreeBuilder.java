package com.celements.navigation.api;

import static java.util.Comparator.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.navigation.TreeNode;
import com.celements.navigation.filter.InternalRightsFilter;
import com.celements.navigation.service.ITreeNodeService;
import com.celements.parents.IDocumentParentsListerRole;

@Component
class NavigationTreeBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationTreeBuilder.class);
  private static final Comparator<String> PART_NAME_COMPARATOR = comparing(
      (String value) -> value.toLowerCase(Locale.ROOT))
          .thenComparing(naturalOrder());

  private final ITreeNodeService treeNodeService;
  private final IDocumentParentsListerRole parentsLister;
  private final DefaultNavigationNodeValueResolver valueResolver;

  @Inject
  NavigationTreeBuilder(ITreeNodeService treeNodeService, IDocumentParentsListerRole parentsLister,
      DefaultNavigationNodeValueResolver valueResolver) {
    this.treeNodeService = treeNodeService;
    this.parentsLister = parentsLister;
    this.valueResolver = valueResolver;
  }

  NavigationTreeResponse build(NavigationRequest request) {
    var rootFilter = new InternalRightsFilter();
    rootFilter.setMenuPart(request.partName().orElse(""));
    var descendantFilter = new InternalRightsFilter();
    Set<DocumentReference> activePath = resolveActivePath(request);
    List<InternalNode> roots = readNodes(
        treeNodeService.getSubNodesForParent(request.nodeSpace(), rootFilter), descendantFilter,
        request.language(), new HashSet<>());
    if (request.currentNode().isPresent() && roots.stream()
        .noneMatch(root -> root.contains(request.currentNode().orElseThrow()))) {
      throw nodeNotFound();
    }
    return new NavigationTreeResponse(request.serializedNodeSpace(),
        request.serializedCurrentNode().orElse(null), request.language(),
        request.partName().orElse(null), request.showInactiveToLevel(),
        buildSegments(roots, activePath, request));
  }

  private Set<DocumentReference> resolveActivePath(NavigationRequest request) {
    return request.currentNode()
        .map(currentNode -> {
          var path = new HashSet<>(
              Objects.requireNonNull(parentsLister.getDocumentParentsList(currentNode, true)));
          path.add(currentNode);
          return Set.copyOf(path);
        })
        .orElseGet(Set::of);
  }

  private List<NavigationSegmentDto> buildSegments(List<InternalNode> roots,
      Set<DocumentReference> activePath, NavigationRequest request) {
    if (request.partName().isPresent()) {
      String partName = request.partName().orElseThrow();
      return List.of(new NavigationSegmentDto(partName,
          roots.stream()
              .filter(root -> partName.equals(root.partName()))
              .map(root -> toDto(root, 1, activePath, request))
              .toList()));
    }
    Map<String, List<InternalNode>> groupedRoots = new LinkedHashMap<>();
    roots.forEach(root -> groupedRoots.computeIfAbsent(root.partName(),
        ignored -> new ArrayList<>())
        .add(root));
    return groupedRoots.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey(PART_NAME_COMPARATOR))
        .map(entry -> new NavigationSegmentDto(emptyToNull(entry.getKey()),
            entry.getValue()
                .stream()
                .map(root -> toDto(root, 1, activePath, request))
                .toList()))
        .toList();
  }

  private NavigationNodeDto toDto(InternalNode node, int level, Set<DocumentReference> activePath,
      NavigationRequest request) {
    boolean isActive = request.currentNode().filter(node.docRef()::equals).isPresent();
    boolean onActivePath = activePath.contains(node.docRef());
    boolean expandForInactiveLevel = level < request.showInactiveToLevel();
    boolean isOpen = onActivePath || (expandForInactiveLevel && !node.children().isEmpty());
    List<NavigationNodeDto> children = isOpen
        ? node.children()
            .stream()
            .map(child -> toDto(child, level + 1, activePath, request))
            .toList()
        : List.of();
    return new NavigationNodeDto(node.serializedDocRef(), node.url(), node.title(),
        node.children().isEmpty(), isActive, isOpen, children);
  }

  private List<InternalNode> readNodes(List<TreeNode> sourceNodes, InternalRightsFilter filter,
      String language, Set<DocumentReference> ancestors) {
    return Objects.requireNonNull(sourceNodes)
        .stream()
        .map(node -> readNode(node, filter, language, ancestors))
        .flatMap(Optional::stream)
        .toList();
  }

  private Optional<InternalNode> readNode(TreeNode sourceNode, InternalRightsFilter filter,
      String language, Set<DocumentReference> ancestors) {
    DocumentReference docRef = sourceNode.getDocumentReference();
    if (!ancestors.add(docRef)) {
      throw new IllegalStateException("Navigation tree contains a cycle at " + docRef);
    }
    try {
      String serializedDocRef = valueResolver.serialize(docRef);
      if (isBlank(serializedDocRef)) {
        return invalidNode(docRef, "docRef");
      }
      String title = valueResolver.resolveTitle(docRef, language);
      if (isBlank(title)) {
        return invalidNode(docRef, "title");
      }
      String url = valueResolver.resolveUrl(docRef, language);
      if (isBlank(url)) {
        return invalidNode(docRef, "url");
      }
      List<TreeNode> sourceChildren = treeNodeService.getSubNodesForParent(docRef, filter);
      if (sourceChildren == null) {
        return invalidNode(docRef, "children");
      }
      List<InternalNode> children = readNodes(sourceChildren, filter, language, ancestors);
      return Optional.of(new InternalNode(docRef, serializedDocRef, sourceNode.getPartName(), url,
          title, children));
    } finally {
      ancestors.remove(docRef);
    }
  }

  private Optional<InternalNode> invalidNode(DocumentReference docRef, String field) {
    LOGGER.warn("Omitting navigation node [{}] because mandatory field [{}] is invalid.", docRef,
        field);
    return Optional.empty();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private String emptyToNull(String value) {
    return value.isEmpty() ? null : value;
  }

  private NavigationApiException nodeNotFound() {
    return new NavigationApiException(HttpStatus.NOT_FOUND, "navigation_node_not_found",
        "The navigation node was not found.");
  }

  private record InternalNode(DocumentReference docRef, String serializedDocRef, String partName,
      String url, String title, List<InternalNode> children) {

    private InternalNode {
      partName = Objects.requireNonNullElse(partName, "");
      children = List.copyOf(children);
    }

    boolean contains(DocumentReference reference) {
      return docRef.equals(reference) || children.stream()
          .anyMatch(child -> child.contains(reference));
    }

  }

}
