package com.celements.web.classes.oldcore;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.XWikiConstant;

/**
 * Do NOT add this to a package. Components for this role are solely for the purpose of having a
 * class and constants where used in Java. Loading this class would interfere with classes handled
 * by XWiki.
 */
@ComponentRole
public interface IOldCoreClassDef extends ClassDefinition {

  String CLASS_SPACE = XWikiConstant.XWIKI_SPACE;

  String FIELD_GROUPS_NAME = "groups";
  String FIELD_GROUPS_PRETTY_NAME = "Groups";
  String FIELD_ACCESSLVL_NAME = "levels";
  String FIELD_ACCESSLVL_PRETTY_NAME = "Levels";
  String FIELD_USERS_NAME = "users";
  String FIELD_USERS_PRETTY_NAME = "Users";
  String FIELD_ALLOW_NAME = "allow";
  String FIELD_ALLOW_PRETTY_NAME = "Allow/Deny";

}
