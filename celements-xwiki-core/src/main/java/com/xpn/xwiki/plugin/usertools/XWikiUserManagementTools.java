package com.xpn.xwiki.plugin.usertools;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public interface XWikiUserManagementTools {

  String inviteUser(String name, String email, XWikiContext context) throws XWikiException;

  boolean resendInvitation(String email, XWikiContext context) throws XWikiException;

  String getUserSpace(XWikiContext context);

  String getUserPage(String email, XWikiContext context);

  boolean isValidEmail(String email);

  String getUserName(String userPage, XWikiContext context) throws XWikiException;

  String getEmail(String userPage, XWikiContext context) throws XWikiException;
}
