package com.celements.model.context;

import java.net.URL;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

@ComponentRole
public interface ModelContext {

  String XWIKI_SPACE = "XWiki";
  String WEB_PREF_DOC_NAME = "WebPreferences";
  String XWIKI_PREF_DOC_NAME = "XWikiPreferences";
  String CFG_KEY_DEFAULT_LANG = "default_language";
  String FALLBACK_DEFAULT_LANG = "en";

  /**
   * WARNING: This call is discouraged, use other methods of this service. It will be deprecated
   * once we'll have a replacement for all of them.
   *
   * @return the old, discouraged {@link XWikiContext}
   */
  @NotNull
  XWikiContext getXWikiContext();

  /**
   * @return the current wiki set in context
   */
  @NotNull
  WikiReference getWikiRef();

  /**
   * @param wikiRef
   *          to be set in context
   * @return the wiki which was set before
   */
  @NotNull
  WikiReference setWikiRef(@NotNull WikiReference wikiRef);

  /**
   * @return true if the current context wiki is main
   */
  boolean isMainWiki();

  /**
   * @deprecated since 3.0, instead use {@link ModelUtils#getMainWikiRef()}
   */
  @Deprecated
  @NotNull
  WikiReference getMainWikiRef();

  /**
   * @deprecated since 3.6, instead use {@link #getDocument}
   */
  @Deprecated
  @Nullable
  XWikiDocument getDoc();

  /**
   * @deprecated since 5.9, instead use {@link #getDocument}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<XWikiDocument> getCurrentDoc();

  @NotNull
  Optional<XWikiDocument> getDocument();

  /**
   * @deprecated since 5.9, instead use {@link #getDocRef}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<DocumentReference> getCurrentDocRef();

  @NotNull
  Optional<DocumentReference> getDocRef();

  /**
   * @deprecated since 5.9, instead use {@link #getSpaceRef}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<SpaceReference> getCurrentSpaceRef();

  @NotNull
  Optional<SpaceReference> getSpaceRef();

  /**
   * Returns the SpaceReference for the current document, if present. Otherwise a spaceReference for
   * the default space.
   *
   * @return the SpaceReference for the current document
   */
  @NotNull
  SpaceReference getCurrentSpaceRefOrDefault();

  /**
   * @param doc
   *          to be set in context
   * @return the doc which was set before
   */
  @Nullable
  XWikiDocument setDoc(@Nullable XWikiDocument doc);

  /**
   * @deprecated since 3.6, instead use {@link #getUserOpt}
   */
  @Deprecated
  @Nullable
  XWikiUser getUser();

  /**
   * @deprecated since 5.9, instead use {@link #user}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<User> getCurrentUser();

  @NotNull
  Optional<User> user();

  /**
   * @deprecated since 3.6, instead use {@link #setCurrentUser(User)}
   */
  @Deprecated
  @Nullable
  XWikiUser setUser(@Nullable XWikiUser user);

  void setCurrentUser(@Nullable User user);

  @NotNull
  Optional<DocumentReference> getUserDocRef();

  @NotNull
  String getUserName();

  /**
   * @deprecated since 5.9, instead use {@link #request}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<XWikiRequest> getRequest();

  @NotNull
  Optional<XWikiRequest> request();

  /**
   * @deprecated since 5.9, instead use {@link #getRequestParam}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getRequestParameter(String key);

  @NotNull
  Optional<String> getRequestParam(String key);

  /**
   * @deprecated since 5.9, instead use {@link #response}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<XWikiResponse> getResponse();

  @NotNull
  Optional<XWikiResponse> response();

  /**
   * @return the current language
   */
  @NotNull
  Optional<String> getLanguage();

  /**
   * @return the default language for the current wiki
   */
  @NotNull
  String getDefaultLanguage();

  /**
   * @param ref
   *          from which the default language is extracted (document, space, or wiki)
   * @return the default language for the given reference
   */
  @NotNull
  String getDefaultLanguage(@NotNull EntityReference ref);

  /**
   * @return the current url set in context
   * @deprecated since 5.9, instead use {@link #getURL}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<URL> getUrl();

  @NotNull
  Optional<URL> getURL();

  /**
   * @param url
   *          to be set in context
   * @return the url which was set before
   * @deprecated since 5.9, instead use {@link #setURL}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<URL> setUrl(@Nullable URL url);

  @NotNull
  Optional<URL> setURL(@Nullable URL url);

  /**
   * Returns the XWikiPreferences document. Creates it (in memory) if it does not exist.
   *
   * @return the XWikiPreferences document
   */
  @NotNull
  XWikiDocument getXWikiPreferenceDoc();

  /**
   * Returns the space preferences document for the given SpaceReference. Creates it (in memory) if
   * it does not exist.
   *
   * @param spaceRef
   *          a SpaceReference to the space you want the space preference document for
   * @return the space preferences document for given space
   * @throws NullPointerException
   *           if spaceRef is null
   */
  @NotNull
  XWikiDocument getSpacePreferenceDoc(@NotNull SpaceReference spaceRef);

}
