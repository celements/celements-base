package com.celements.auth.user;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.exception.DocumentSaveException;
import com.google.common.base.Optional;

@ComponentRole
public interface UserService {

  String DEFAULT_LOGIN_FIELD = "loginname";
  String USERNAME_FIELD = "xwikiname";

  /**
   * @return the default user space reference for the current wiki
   */
  @NotNull
  SpaceReference getUserSpaceRef();

  /**
   * @param wikiRef
   * @return the default user space reference for the given wikiRef
   */
  @NotNull
  SpaceReference getUserSpaceRef(@Nullable WikiReference wikiRef);

  /**
   * @param accountName
   * @return the resolved user doc reference, enforces the user space from
   *         {@link #getUserSpaceRef()}
   */
  @NotNull
  DocumentReference resolveUserDocRef(@NotNull String accountName);

  /**
   * @param userDocRef
   * @return a {@link User} instance for the given user doc
   * @throws UserInstantiationException
   *           if the given user doc is invalid
   */
  @NotNull
  User getUser(@NotNull DocumentReference userDocRef) throws UserInstantiationException;

  /**
   * @param accountName
   * @return a {@link User} instance for the given accountName
   * @throws UserInstantiationException
   *           if the given accountName is invalid
   */
  @NotNull
  User getUser(@NotNull String accountName) throws UserInstantiationException;

  /**
   * @return list of user fields for which one can login, e.g. 'name', 'email' or 'validkey'
   */
  @NotNull
  Set<String> getPossibleLoginFields();

  /**
   * creates a new user with the provided userData. generates a random name if given accountName is
   * already taken.
   *
   * @param accountName
   * @param userData
   * @param validate
   *          sends a validation mail if true
   * @return the new {@link User} instance
   * @throws UserCreateException
   *           if the user creation process failed
   */
  @NotNull
  User createNewUser(@NotNull String accountName, @NotNull Map<String, String> userData,
      boolean validate) throws UserCreateException;

  /**
   * creates a new user with the provided userData. generates a random name if no account name is
   * provided.
   *
   * @param userData
   * @param validate
   *          sends a validation mail if true
   * @return the new {@link User} instance
   * @throws UserCreateException
   *           if the user creation process failed
   */
  @NotNull
  User createNewUser(@NotNull Map<String, String> userData, boolean validate)
      throws UserCreateException;

  /**
   * gets the existing user or creates a new one if none exists with the provided userData.
   * generates a random name if no account name is provided.
   *
   * @param userData
   * @return the new {@link User} instance
   * @throws UserCreateException
   *           if the user creation process failed
   */
  @NotNull
  User getOrCreateNewUser(@NotNull Map<String, String> userData)
      throws UserCreateException;

  /**
   * adds the given user to the given group if it doesn't already exist
   *
   * @return true if the document changed, false if the user already existed in the group
   * @throws DocumentSaveException
   *           if unable to save the group document
   */
  boolean addUserToGroup(@NotNull User user, @NotNull ClassReference groupRef)
      throws DocumentSaveException;

  /**
   * adds the given user to defined default groups and at least to the XWikiAllGroup.
   *
   * @return true if the user could be added to at least one of the default groups.
   * @throws DocumentSaveException
   *           if unable to save the group document
   */
  boolean addUserToDefaultGroups(@NotNull User user) throws DocumentSaveException;

  /**
   * looks up the user by the given login value with {@link #getPossibleLoginFields()}
   *
   * @param login
   *          the login value
   * @return the matched {@link User} instance or absent if the given login doesn't match or isn't
   *         unique
   */
  @NotNull
  Optional<User> getUserForLoginField(@NotNull String login);

  /**
   * looks up the user by the given login value with the possible login fields.
   *
   * @param login
   *          the login value
   * @param possibleLoginFields
   *          list of user fields for which one can login, e.g. 'name', 'email' or 'validkey'
   * @return the matched {@link User} instance or absent if the given login doesn't match or isn't
   *         unique
   */
  @NotNull
  Optional<User> getUserForLoginField(@NotNull String login,
      @Nullable Collection<String> possibleLoginFields);

  /**
   * sends a validation mail to the given user
   *
   * @return true if successfully sent
   */
  boolean sendValidationMail(@NotNull User user);

}
