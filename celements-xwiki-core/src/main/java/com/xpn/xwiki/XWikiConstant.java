package com.xpn.xwiki;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.reference.RefBuilder;

public final class XWikiConstant {

  public static final WikiReference MAIN_WIKI = new WikiReference("xwiki");
  public static final WikiReference CENTRAL_WIKI = new WikiReference("celements2web");

  public static final String XWIKI_SPACE = "XWiki";
  public static final String WEB_PREF_DOC_NAME = "WebPreferences";
  public static final String XWIKI_PREF_DOC_NAME = "XWikiPreferences";

  public static final String TAG_CLASS = XWIKI_SPACE + ".TagClass";
  public static final String TAG_CLASS_PROP_TAGS = "tags";
  public static final String SHEET_CLASS = XWIKI_SPACE + ".SheetClass";

  public static final DocumentReference SERVER_CLASS_DOCREF = RefBuilder.create()
      .with(XWikiConstant.MAIN_WIKI)
      .space(XWikiConstant.XWIKI_SPACE)
      .doc("XWikiServerClass")
      .build(DocumentReference.class);

  private XWikiConstant() {}

}
