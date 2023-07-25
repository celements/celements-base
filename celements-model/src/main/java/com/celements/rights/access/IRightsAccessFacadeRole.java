package com.celements.rights.access;

import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.auth.user.User;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

@ComponentRole
public interface IRightsAccessFacadeRole {

  /**
   * instead use hasAccessLevel(EntityReference, EAccessLevel, User)
   */
  @Deprecated
  boolean hasAccessLevel(String right, XWikiUser user, EntityReference entityRef);

  XWikiRightService getRightsService();

  boolean hasAccessLevel(EntityReference ref, EAccessLevel level);

  boolean hasAccessLevel(EntityReference ref, EAccessLevel level, User user);

  /**
   * instead use hasAccessLevel(EntityReference, EAccessLevel, User)
   */
  @Deprecated
  boolean hasAccessLevel(EntityReference ref, EAccessLevel level, XWikiUser xUser);

  boolean isInGroup(DocumentReference groupDocRef, User user);

  boolean isLoggedIn();

  boolean isAdmin();

  boolean isAdmin(User user);

  boolean isAdvancedAdmin();

  boolean isAdvancedAdmin(User user);

  boolean isSuperAdmin();

  boolean isSuperAdmin(User user);

  boolean isLayoutEditor();

  boolean isLayoutEditor(User user);

  /**
   * Gets all DocumentReferences for all groups a user belongs to and returns them in a Stream.
   *
   * @param user
   * @return a Stream of Document References
   */
  @NotNull
  Stream<DocumentReference> getGroupRefsForUser(@NotNull User user);

}
