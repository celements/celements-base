package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;

import com.xpn.xwiki.XWikiContext;

public class XWikiDebugGWTURLFactory extends XWikiServletURLFactory {

  @Override
  public void init(XWikiContext context) {
    URL url = context.getURL();

    contextPath = "xwiki/";

    try {
      serverURL = new URL(url.getProtocol(), url.getHost(), 1025, "/");
    } catch (MalformedURLException e) {
      // This can't happen
    }
  }

}
