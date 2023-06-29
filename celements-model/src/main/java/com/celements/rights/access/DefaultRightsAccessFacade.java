package com.celements.rights.access;

import static com.xpn.xwiki.user.api.XWikiRightService.*;

import java.util.Collection;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.auth.user.User;
import com.celements.init.XWikiProvider;
import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.internal.IEntityReferenceRandomCompleterRole;
import com.celements.web.classes.oldcore.XWikiUsersClass.Type;
import com.google.common.base.Optional;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class DefaultRightsAccessFacade implements IRightsAccessFacadeRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRightsAccessFacade.class);

  private final IEntityReferenceRandomCompleterRole randomCompleter;
  private final ModelUtils modelUtils;
  private final ModelContext context;
  private final XWikiProvider xwiki;

  @Inject
  public DefaultRightsAccessFacade(IEntityReferenceRandomCompleterRole randomCompleter,
      ModelUtils modelUtils, ModelContext context, XWikiProvider xwiki) {
    super();
    this.randomCompleter = randomCompleter;
    this.modelUtils = modelUtils;
    this.context = context;
    this.xwiki = xwiki;
  }

  @Override
  public XWikiRightService getRightsService() {
    return xwiki.get().orElseThrow().getRightService();
  }

  private XWikiGroupService getGroupService() {
    try {
      return xwiki.get().orElseThrow().getGroupService(context.getXWikiContext());
    } catch (XWikiException xwe) {
      throw new IllegalStateException("failed getting GroupService", xwe);
    }
  }

  @Override
  @Deprecated
  public boolean hasAccessLevel(String right, XWikiUser user, EntityReference entityRef) {
    Optional<EAccessLevel> eAccessLevel = EAccessLevel.getAccessLevel(right);
    if (eAccessLevel.isPresent()) {
      return hasAccessLevel(entityRef, eAccessLevel.get(), user);
    }
    return false;
  }

  @Override
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level) {
    return hasAccessLevel(ref, level, context.user().orElse(null));
  }

  @Override
  @Deprecated
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level, XWikiUser xUser) {
    return hasAccessLevel(ref, level, (xUser != null) ? modelUtils.resolveRef(xUser.getUser(),
        DocumentReference.class) : null);
  }

  @Override
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level, User user) {
    return hasAccessLevel(ref, level, (user != null) ? user.getDocRef() : null);
  }

  private boolean hasAccessLevel(EntityReference ref, EAccessLevel level,
      DocumentReference userDocRef) {
    boolean ret = false;
    DocumentReference docRef = null;
    EntityReference entityRef = randomCompleter.randomCompleteSpaceRef(ref);
    if (entityRef instanceof DocumentReference) {
      docRef = (DocumentReference) entityRef;
    } else if (entityRef instanceof AttachmentReference) {
      docRef = ((AttachmentReference) entityRef).getDocumentReference();
    }
    if ((docRef != null) && (level != null)) {
      ret = hasAccessLevelInternal(modelUtils.serializeRef(docRef), level.getIdentifier(),
          userDocRef);
    }
    LOGGER.debug("hasAccessLevel: for ref '{}', level '{}' and user '{}' returned '{}'", ref, level,
        userDocRef, ret);
    return ret;
  }

  private boolean hasAccessLevelInternal(String fullName, String level,
      DocumentReference userDocRef) {
    try {
      String accountName = GUEST_USER_FULLNAME;
      if (userDocRef != null) {
        accountName = modelUtils.serializeRef(userDocRef);
      }
      return getRightsService().hasAccessLevel(level, accountName, fullName,
          context.getXWikiContext());
    } catch (XWikiException xwe) {
      // already being catched in XWikiRightServiceImpl.hasAccessLevel()
      LOGGER.error("should not happen", xwe);
      return false;
    }
  }

  @Override
  public boolean isInGroup(DocumentReference groupDocRef, User user) {
    try {
      return (user != null) && getGroupService().getAllGroupsReferencesForMember(user.getDocRef(),
          0, 0, context.getXWikiContext()).contains(groupDocRef);
    } catch (XWikiException xwe) {
      LOGGER.warn("isInGroup: failed for user [{}], group [{}]", user, groupDocRef, xwe);
      return false;
    }
  }

  @Override
  public boolean isLoggedIn() {
    boolean ret = context.user()
        .filter(user -> !user.getDocRef().getName().equals(GUEST_USER))
        .isPresent();
    LOGGER.info("isLoggedIn: {}", ret);
    return ret;
  }

  @Override
  public boolean isAdmin() {
    return isAdmin(context.user().orElse(null));
  }

  @Override
  public boolean isAdmin(User user) {
    boolean ret = (user != null) && (hasAdminRightsOnPreferences(user) || isInGroup(
        getAdminGroupDocRef(), user));
    LOGGER.debug("isAdmin: [{}] for user [{}]", ret, user);
    return ret;
  }

  DocumentReference getAdminGroupDocRef() {
    return new RefBuilder().doc("XWikiAdminGroup").space("XWiki").with(context.getWikiRef()).build(
        DocumentReference.class);
  }

  private boolean hasAdminRightsOnPreferences(User user) {
    boolean ret = false;
    ret = hasAccessLevelInternal("XWiki.XWikiPreferences", "admin", user.getDocRef());
    if (!ret && context.getDocument().isPresent()) {
      String spacePrefFullName = context.getSpaceRef().orElseThrow().getName() + ".WebPreferences";
      ret = hasAccessLevelInternal(spacePrefFullName, "admin", user.getDocRef());
    }
    return ret;
  }

  @Override
  public boolean isAdvancedAdmin() {
    return isAdvancedAdmin(context.user().orElse(null));
  }

  @Override
  public boolean isAdvancedAdmin(User user) {
    boolean ret = isAdmin(user) && (user.isGlobal() || (user.getType() == Type.Advanced));
    LOGGER.debug("isAdvancedAdmin: [{}] for user [{}]", ret, user);
    return ret;
  }

  @Override
  public boolean isSuperAdmin() {
    return isSuperAdmin(context.user().orElse(null));
  }

  @Override
  public boolean isSuperAdmin(User user) {
    boolean ret = isAdmin(user) && user.isGlobal();
    LOGGER.debug("isSuperAdmin: [{}] for user [{}]", ret, user);
    return ret;
  }

  @Override
  public boolean isLayoutEditor() {
    return isLayoutEditor(context.user().orElse(null));
  }

  @Override
  public boolean isLayoutEditor(User user) {
    boolean ret = isAdvancedAdmin(user) || isInGroup(getLayoutEditorsGroupDocRef(), user);
    LOGGER.debug("isLayoutEditor: [{}] for user [{}]", ret, user);
    return ret;
  }

  private DocumentReference getLayoutEditorsGroupDocRef() {
    return new RefBuilder().doc("LayoutEditorsGroup").space("XWiki").with(
        context.getWikiRef()).build(DocumentReference.class);
  }

  @Override
  public Stream<DocumentReference> getGroupRefsForUser(User user) {
    try {
      Collection<DocumentReference> groupRefList = getGroupService()
          .getAllGroupsReferencesForMember(user.getDocRef(), 0, 0, context.getXWikiContext());
      return groupRefList.stream();
    } catch (XWikiException xwe) {
      LOGGER.error("generating groupRefList failed for {}", user, xwe);
      return Stream.empty();
    }

  }

}
