package com.xpn.xwiki;

import org.xwiki.model.reference.WikiReference;

public final class XWikiConstant {

  public static final WikiReference MAIN_WIKI = new WikiReference("main");

  public static final String XWIKI_SPACE = "XWiki";
  public static final String WEB_PREF_DOC_NAME = "WebPreferences";
  public static final String XWIKI_PREF_DOC_NAME = "XWikiPreferences";

  public static final String TAG_CLASS = XWIKI_SPACE + ".TagClass";
  public static final String TAG_CLASS_PROP_TAGS = "tags";
  public static final String SHEET_CLASS = XWIKI_SPACE + ".SheetClass";

  private XWikiConstant() {}

}
