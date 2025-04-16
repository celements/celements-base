package com.celements.globalredirect;

import com.xpn.xwiki.web.XWikiResponse;

public interface IGlobalRedirect {

  void sendRedirect(XWikiResponse response, String url);

  boolean test(String url);

}
