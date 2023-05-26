package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.BinaryOperator;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ServerUrlUtilsRole {

  URL getServerURL(BinaryOperator<String> cfg) throws MalformedURLException;

  URL getServerURL(String wikiName, XWikiContext context) throws MalformedURLException;

}
