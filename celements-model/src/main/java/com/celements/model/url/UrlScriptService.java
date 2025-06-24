package com.celements.model.url;

import static com.google.common.base.MoreObjects.*;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;
import com.celements.url.UrlService;

@Component("url")
public class UrlScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UrlScriptService.class);

  private final UrlService urlService, ModelContext context;
  private final ModelContext context;

  @Inject
  public URLScriptService(UrlService urlService, ModelContext context)  {
    this.urlService = urlService;
    this.context = context;
  }

  @NotNull
  public String getURL() {
    return getURL(null);
  }

  @NotNull
  public String getURL(@Nullable EntityReference ref) {
    return getURL(ref, null);
  }

  @NotNull
  public String getURL(@Nullable EntityReference ref, @Nullable String action) {
    return getURL(ref, action, null);
  }

  @NotNull
  public String getURL(@Nullable EntityReference ref, @Nullable String action,
      @Nullable String queryString) {
    try {
      return urlService.getURL(firstNonNull(ref, getCurrentReference()), action, queryString);
    } catch (Exception iae) {
      LOGGER.debug("getURL - failed for [{}], [{}], [{}]", ref, action, queryString);
      return null;
    }
  }

  @NotNull
  public String getExternalURL() {
    return getExternalURL(null);
  }

  @NotNull
  public String getExternalURL(@Nullable EntityReference ref) {
    return getExternalURL(ref, null);
  }

  @NotNull
  public String getExternalURL(@Nullable EntityReference ref, @Nullable String action) {
    return getExternalURL(ref, action, null);
  }

  @NotNull
  public String getExternalURL(@Nullable EntityReference ref, @Nullable String action,
      @Nullable String queryString) {
    try {
      return urlService.getExternalURL(firstNonNull(ref, getCurrentReference()), action,
          queryString);
    } catch (Exception iae) {
      LOGGER.debug("getExternalURL - failed for [{}], [{}], [{}]", ref, action, queryString);
      return null;
    }
  }

  @NotNull
  public UriBuilder createURIBuilder() {
    return createURIBuilder(null);
  }

  @NotNull
  UriBuilder createURIBuilder(@Nullable EntityReference ref) {
    return createURIBuilder(ref, null);
  }

  @NotNull
  UriBuilder createURIBuilder(@Nullable EntityReference ref, @Nullable String action) {
    try {
      return urlService.createURIBuilder(firstNonNull(ref, getCurrentReference()), action);
    } catch (Exception iae) {
      LOGGER.debug("createURIBuilder - failed for [{}], [{}]", ref, action);
      return null;
    }
  }

  private EntityReference getCurrentReference() {
    return context.getDocRef()
        .map(EntityReference.class::cast)
        .orElseGet(context::getWikiRef);
  }

}
