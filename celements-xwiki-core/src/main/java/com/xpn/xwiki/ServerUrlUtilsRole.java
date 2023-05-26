package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ServerUrlUtilsRole {

  URL getServerURL(XWikiConfig cfg) throws MalformedURLException;

  URL getServerURL(String wikiName, XWikiContext context) throws MalformedURLException;

}
