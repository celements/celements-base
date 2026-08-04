package com.celements.navigation.api;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.web.service.IWebUtilsService;

@Component
class NavigationRequestResolver {

  private final ModelUtils modelUtils;
  private final ModelContext modelContext;
  private final IWebUtilsService webUtilsService;

  @Inject
  NavigationRequestResolver(ModelUtils modelUtils, ModelContext modelContext,
      IWebUtilsService webUtilsService) {
    this.modelUtils = modelUtils;
    this.modelContext = modelContext;
    this.webUtilsService = webUtilsService;
  }

  NavigationRequest resolve(String nodeSpace, String currentNode, String language, String partName,
      int showInactiveToLevel) {
    if (showInactiveToLevel < 0 || showInactiveToLevel > 100) {
      throw invalidParameter();
    }
    var spaceRef = resolveCanonicalLocal(nodeSpace, SpaceReference.class);
    var serializedCurrentNode = Optional.ofNullable(currentNode);
    var currentNodeRef = serializedCurrentNode
        .map(value -> resolveCanonicalLocal(value, DocumentReference.class));
    var resolvedLanguage = resolveLanguage(language, spaceRef);
    var normalizedPartName = normalizeOptional(partName);
    return new NavigationRequest(spaceRef, nodeSpace, currentNodeRef, serializedCurrentNode,
        resolvedLanguage, normalizedPartName, showInactiveToLevel);
  }

  private <T extends EntityReference> T resolveCanonicalLocal(String value, Class<T> type) {
    if (value == null || value.isBlank() || !value.equals(value.trim())) {
      throw invalidReference();
    }
    var wikiRef = modelContext.getWikiRef();
    final T reference;
    try {
      reference = modelUtils.resolveRef(value, type, wikiRef);
    } catch (IllegalArgumentException exception) {
      throw invalidReference();
    }
    if (reference == null || !value.equals(modelUtils.serializeRefLocal(reference))) {
      throw invalidReference();
    }
    return reference;
  }

  private String resolveLanguage(String requestedLanguage, SpaceReference nodeSpace) {
    var explicitLanguage = normalizeOptional(requestedLanguage);
    if (explicitLanguage.isEmpty()) {
      return modelContext.getLanguage().orElseGet(modelContext::getDefaultLanguage);
    }
    final String normalizedLanguage;
    try {
      normalizedLanguage = modelUtils.normalizeLang(explicitLanguage.orElseThrow());
    } catch (IllegalArgumentException exception) {
      throw unsupportedLanguage();
    }
    if (normalizedLanguage.isBlank()
        || !webUtilsService.getAllowedLanguages(nodeSpace).contains(normalizedLanguage)) {
      throw unsupportedLanguage();
    }
    return normalizedLanguage;
  }

  private Optional<String> normalizeOptional(String value) {
    return Optional.ofNullable(value).map(String::trim).filter(normalized -> !normalized.isEmpty());
  }

  private NavigationApiException invalidReference() {
    return new NavigationApiException(HttpStatus.BAD_REQUEST, "invalid_reference",
        "The reference is invalid.");
  }

  private NavigationApiException invalidParameter() {
    return new NavigationApiException(HttpStatus.BAD_REQUEST, "invalid_parameter",
        "The parameter is invalid.");
  }

  private NavigationApiException unsupportedLanguage() {
    return new NavigationApiException(HttpStatus.BAD_REQUEST, "unsupported_language",
        "The language is not supported.");
  }

}
