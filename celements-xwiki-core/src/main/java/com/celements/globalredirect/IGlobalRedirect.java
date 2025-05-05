package com.celements.globalredirect;

import javax.validation.constraints.NotNull;

import com.xpn.xwiki.web.XWikiResponse;

public interface IGlobalRedirect {

  void sendRedirect(@NotNull XWikiResponse response, @NotNull String url);

  boolean test(@NotNull String url);

}
