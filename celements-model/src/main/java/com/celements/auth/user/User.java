package com.celements.auth.user;

import java.util.Optional;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.web.classes.oldcore.XWikiUsersClass.Type;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

@NotThreadSafe
@ComponentRole
public interface User {

  void initialize(@NotNull DocumentReference userDocRef) throws UserInstantiationException;

  @NotNull
  DocumentReference getDocRef();

  @NotNull
  XWikiDocument getDocument();

  boolean isGlobal();

  @NotNull
  XWikiUser asXWikiUser();

  /**
   * @deprecated since 5.9 instead use {@link #email()}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getEmail();

  @NotNull
  Optional<String> email();

  /**
   * @deprecated since 5.9 instead use {@link #firstName()}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getFirstName();

  @NotNull
  Optional<String> firstName();

  /**
   * @deprecated since 5.9 instead use {@link #lastName()}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getLastName();

  @NotNull
  Optional<String> lastName();

  /**
   * @deprecated since 5.9 instead use {@link #prettyName()}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getPrettyName();

  @NotNull
  Optional<String> prettyName();

  /**
   * @deprecated since 5.9 instead use {@link #getAdminLang()}
   */
  @Deprecated
  @NotNull
  com.google.common.base.Optional<String> getAdminLanguage();

  @NotNull
  Optional<String> getAdminLang();

  @NotNull
  Type getType();

  boolean isActive();

  boolean isSuspended();

}
