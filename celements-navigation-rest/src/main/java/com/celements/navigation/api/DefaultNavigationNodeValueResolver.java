package com.celements.navigation.api;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.navigation.cmd.MultilingualMenuNameCommand;
import com.celements.url.UrlService;

@Component
final class DefaultNavigationNodeValueResolver implements NavigationNodeValueResolver {

  private final ModelUtils modelUtils;
  private final ModelContext modelContext;
  private final UrlService urlService;
  private final MultilingualMenuNameCommand menuNameCommand;

  @Inject
  DefaultNavigationNodeValueResolver(ModelUtils modelUtils, ModelContext modelContext,
      UrlService urlService) {
    this(modelUtils, modelContext, urlService, new MultilingualMenuNameCommand());
  }

  DefaultNavigationNodeValueResolver(ModelUtils modelUtils, ModelContext modelContext,
      UrlService urlService, MultilingualMenuNameCommand menuNameCommand) {
    this.modelUtils = modelUtils;
    this.modelContext = modelContext;
    this.urlService = urlService;
    this.menuNameCommand = menuNameCommand;
  }

  @Override
  public String serialize(DocumentReference docRef) {
    return modelUtils.serializeRefLocal(docRef);
  }

  @Override
  public String resolveTitle(DocumentReference docRef, String language) {
    return menuNameCommand.getMultilingualMenuName(modelUtils.serializeRefLocal(docRef), language,
        modelContext.getXWikiContext());
  }

  @Override
  public String resolveUrl(DocumentReference docRef, String language) {
    return urlService.getURL(docRef, "view", "language=" + language);
  }

}
