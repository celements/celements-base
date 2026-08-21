package com.celements.navigation.api;

import java.util.Optional;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

record NavigationRequest(SpaceReference nodeSpace, String serializedNodeSpace,
    Optional<DocumentReference> currentNode, Optional<String> serializedCurrentNode,
    String language, Optional<String> partName, int showInactiveToLevel) {

}
