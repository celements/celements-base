package com.celements.rights.access;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import com.celements.auth.user.User;
import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.celements.rights.publication.EPubUnpub;
import com.celements.rights.publication.IPublicationServiceRole;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

@Component(RightsAccessScriptService.NAME)
public class RightsAccessScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RightsAccessScriptService.class);

  public static final String NAME = "rightsAccess";

  private final IPublicationServiceRole pubSrv;
  private final IRightsAccessFacadeRole rightsAccess;
  private final UserService userService;

  @Inject
  public RightsAccessScriptService(IPublicationServiceRole pubSrv,
      IRightsAccessFacadeRole rightsAccess, UserService userService) {
    super();
    this.pubSrv = pubSrv;
    this.rightsAccess = rightsAccess;
    this.userService = userService;
  }

  public EPubUnpub getEPubUnpub(String name) {
    return EPubUnpub.valueOf(name);
  }

  public EAccessLevel getEAccessLevel(String xwikiRight) {
    return EAccessLevel.getAccessLevel(xwikiRight).orNull();
  }

  public XWikiUser getGuestUser() {
    return new XWikiUser(XWikiRightService.GUEST_USER_FULLNAME);
  }

  public XWikiUser getUser(String username) {
    return new XWikiUser(username);
  }

  public boolean isPublishActive(DocumentReference forDoc) {
    return pubSrv.isPublishActive(forDoc);
  }

  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level) {
    return rightsAccess.hasAccessLevel(ref, level);
  }

  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level, XWikiUser user) {
    return rightsAccess.hasAccessLevel(ref, level, user);
  }

  public boolean hasAccessLevel(DocumentReference docRef, EAccessLevel level, XWikiUser user,
      EPubUnpub unpublished) {
    pubSrv.overridePubUnpub(unpublished);
    return hasAccessLevel(docRef, level, user);
  }

  public boolean isLoggedIn() {
    return rightsAccess.isLoggedIn();
  }

  public boolean isAdmin() {
    return rightsAccess.isAdmin();
  }

  public boolean isAdvancedAdmin() {
    return rightsAccess.isAdvancedAdmin();
  }

  public boolean isSuperAdmin() {
    return rightsAccess.isSuperAdmin();
  }

  public boolean isLayoutEditor() {
    return rightsAccess.isLayoutEditor();
  }

  public List<DocumentReference> getGroupRefsForUser(DocumentReference userDocRef) {
    try {
      User user = userService.getUser(userDocRef);
      return rightsAccess.getGroupRefsForUser(user).collect(Collectors.toList());
    } catch (UserInstantiationException uie) {
      LOGGER.error("could not instantiate user for {}", userDocRef, uie);
      return new ArrayList<>();
    }
  }

}
