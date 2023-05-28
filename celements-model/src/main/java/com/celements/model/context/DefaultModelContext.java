package com.celements.model.context;

import static com.google.common.base.Preconditions.*;

import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.celements.common.MoreOptional;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

@Component
public class DefaultModelContext implements ModelContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelContext.class);

  @Requirement("XWikiStubContextInitializer")
  private ExecutionContextInitializer stubXWikiContextInitializer;

  @Requirement(CelementsFromWikiConfigurationSource.NAME)
  ConfigurationSource wikiConfigSrc;

  @Requirement
  ConfigurationSource defaultConfigSrc;

  @Requirement
  EntityReferenceValueProvider refValProvider;

  @Requirement
  private Execution execution;

  @Override
  public XWikiContext getXWikiContext() {
    XWikiContext context = getXWikiContextFromExecution();
    if (context == null) {
      try {
        stubXWikiContextInitializer.initialize(execution.getContext());
        context = getXWikiContextFromExecution();
        // TODO [CELDEV-347] context may still be null at this point, e.g. in first request
        // see DefaultXWikiStubContextProvider for explanation
        // see AbstractJob#createJobContext to create context from scratch
      } catch (ExecutionContextException exc) {
        new RuntimeException("failed to initialise stub context", exc);
      }
    }
    return checkNotNull(context);
  }

  private XWikiContext getXWikiContextFromExecution() {
    return (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
  }

  @Override
  public WikiReference getWikiRef() {
    return RefBuilder.create().wiki(getXWikiContext().getDatabase()).build(WikiReference.class);
  }

  @Override
  public WikiReference setWikiRef(WikiReference wikiRef) {
    WikiReference oldWiki = getWikiRef();
    getXWikiContext().setDatabase(wikiRef.getName());
    return oldWiki;
  }

  @Override
  public boolean isMainWiki() {
    return getModelUtils().isMainWiki(getWikiRef());
  }

  @Override
  @Deprecated
  public XWikiDocument getDoc() {
    return getDocInternal();
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<XWikiDocument> getCurrentDoc() {
    return com.google.common.base.Optional.fromJavaUtil(getDocument());
  }

  @Override
  public Optional<XWikiDocument> getDocument() {
    return Optional.ofNullable(getDocInternal());
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<DocumentReference> getCurrentDocRef() {
    return com.google.common.base.Optional.fromJavaUtil(getDocRef());
  }

  @Override
  public Optional<DocumentReference> getDocRef() {
    return getDocument().map(XWikiDocument::getDocumentReference);
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<SpaceReference> getCurrentSpaceRef() {
    return com.google.common.base.Optional.fromJavaUtil(getSpaceRef());
  }

  @Override
  public Optional<SpaceReference> getSpaceRef() {
    return getDocRef().map(DocumentReference::getLastSpaceReference);
  }

  @Override
  public SpaceReference getCurrentSpaceRefOrDefault() {
    return getSpaceRef().orElseGet(() -> RefBuilder.from(getWikiRef())
        .space(refValProvider.getDefaultValue(EntityType.SPACE))
        .build(SpaceReference.class));
  }

  private XWikiDocument getDocInternal() {
    return getXWikiContext().getDoc();
  }

  @Override
  public XWikiDocument setDoc(XWikiDocument doc) {
    XWikiDocument oldDoc = getDocument().orElse(null);
    getXWikiContext().setDoc(doc);
    return oldDoc;
  }

  @Override
  @Deprecated
  public XWikiUser getUser() {
    return getXWikiContext().getXWikiUser();
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<User> getCurrentUser() {
    return com.google.common.base.Optional.fromJavaUtil(user());
  }

  @Override
  public Optional<User> user() {
    try {
      return Optional.of(getUserService().getUser(getUserName()));
    } catch (UserInstantiationException exc) {
      LOGGER.debug("failed loading user [{}]", getUserDocRef(), exc);
    }
    return Optional.empty();
  }

  @Override
  @Deprecated
  public XWikiUser setUser(XWikiUser xUser) {
    XWikiUser oldUser = getUser();
    if (xUser != null) {
      setCurrentXUser(xUser);
    } else {
      clearCurrentUser();
    }
    return oldUser;
  }

  @Override
  public void setCurrentUser(User user) {
    if (user != null) {
      setCurrentXUser(user.asXWikiUser());
    } else {
      clearCurrentUser();
    }
  }

  private void setCurrentXUser(XWikiUser xUser) {
    getXWikiContext().setUser(xUser.getUser(), xUser.isMain());
  }

  private void clearCurrentUser() {
    getXWikiContext().setUser(null);
  }

  @Override
  public Optional<DocumentReference> getUserDocRef() {
    try {
      return Optional.of(getUserService().resolveUserDocRef(getUserName()));
    } catch (IllegalArgumentException iae) {
      return Optional.empty();
    }
  }

  @Override
  public String getUserName() {
    return getXWikiContext().getUser();
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<XWikiRequest> getRequest() {
    return com.google.common.base.Optional.fromJavaUtil(request());
  }

  @Override
  public Optional<XWikiRequest> request() {
    return Optional.ofNullable(getXWikiContext().getRequest());
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<String> getRequestParameter(String key) {
    return com.google.common.base.Optional.fromJavaUtil(getRequestParam(key));
  }

  @Override
  public Optional<String> getRequestParam(String key) {
    return request().map(r -> r.get(key)).flatMap(MoreOptional::asNonBlank);
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<XWikiResponse> getResponse() {
    return com.google.common.base.Optional.fromJavaUtil(response());
  }

  @Override
  public Optional<XWikiResponse> response() {
    return Optional.ofNullable(getXWikiContext().getResponse());
  }

  @Override
  public Optional<String> getLanguage() {
    return Optional.ofNullable(getXWikiContext().getLanguage());
  }

  @Override
  public String getDefaultLanguage() {
    return getDefaultLanguage(getWikiRef());
  }

  @Override
  public String getDefaultLanguage(EntityReference ref) {
    String ret = getDefaultLangFromDoc(ref);
    if (ret.isEmpty()) {
      ret = getDefaultLangFromConfigSrc(ref);
    }
    LOGGER.trace("getDefaultLanguage: for '{}' got lang" + " '{}'", ref, ret);
    return ret;
  }

  private String getDefaultLangFromDoc(EntityReference ref) {
    String ret = "";
    Optional<DocumentReference> docRef = ref.extractRef(DocumentReference.class);
    if (docRef.isPresent()) {
      try {
        ret = getModelAccess().getDocument(docRef.get()).getDefaultLanguage();
      } catch (DocumentNotExistsException exc) {
        LOGGER.info("trying to get default language for inexistent document '{}'", docRef);
      }
    }
    return ret;
  }

  private String getDefaultLangFromConfigSrc(EntityReference ref) {
    XWikiDocument spacePrefDoc = getSpacePrefDoc(ref);
    final ConfigurationSource configSrc = (spacePrefDoc != null)
        ? defaultConfigSrc // checks space preferences
        : wikiConfigSrc; // skips space preferences
    return new Contextualiser()
        .withDoc(spacePrefDoc)
        .withWiki(ref.extractRef(WikiReference.class).orElse(null))
        .execute(() -> configSrc.getProperty(CFG_KEY_DEFAULT_LANG, FALLBACK_DEFAULT_LANG));

  }

  @Override
  @Deprecated
  public XWikiDocument getXWikiPreferenceDoc() {
    return getXWikiPreferencesDoc();
  }

  @Override
  public XWikiDocument getXWikiPreferencesDoc() {
    return getModelAccess().getOrCreateDocument(getXWikiPreferencesDocRef());
  }

  @Override
  public DocumentReference getXWikiPreferencesDocRef() {
    return new RefBuilder().wiki(getWikiRef().getName()).space(XWIKI_SPACE).doc(XWIKI_PREF_DOC_NAME)
        .build(DocumentReference.class);
  }

  @Override
  public XWikiDocument getSpacePreferenceDoc(SpaceReference spaceRef) {
    checkNotNull(spaceRef);
    return getModelAccess().getOrCreateDocument(new DocumentReference(WEB_PREF_DOC_NAME, spaceRef));
  }

  private XWikiDocument getSpacePrefDoc(EntityReference ref) {
    XWikiDocument ret = null;
    Optional<SpaceReference> spaceRef = ref.extractRef(SpaceReference.class);
    if (spaceRef.isPresent()) {
      try {
        ret = getModelAccess().getDocument(new DocumentReference(WEB_PREF_DOC_NAME,
            spaceRef.get()));
      } catch (DocumentNotExistsException exc) {
        LOGGER.debug("no web preferences for space '{}'", spaceRef);
      }
    }
    return ret;
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<URL> getUrl() {
    return com.google.common.base.Optional.fromJavaUtil(getURL());
  }

  @Override
  public Optional<URL> getURL() {
    return Optional.ofNullable(getXWikiContext().getURL());
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<URL> setUrl(URL url) {
    return com.google.common.base.Optional.fromJavaUtil(setURL(url));
  }

  @Override
  public Optional<URL> setURL(URL url) {
    URL oldUrl = getXWikiContext().getURL();
    getXWikiContext().setURL(url);
    return Optional.ofNullable(oldUrl);
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private IModelAccessFacade getModelAccess() {
    return Utils.getComponent(IModelAccessFacade.class);
  }

  private UserService getUserService() {
    return Utils.getComponent(UserService.class);
  }

}
