package com.celements.spring.mvc;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.xwiki.container.servlet.ServletContainerInitializer;

import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

@Component
public class XWikiSpringInterceptor implements CelMvcInterceptor {

  private final ServletContext servletContext;
  private final ServletContainerInitializer initializer;

  @Inject
  public XWikiSpringInterceptor(
      ServletContext servletContext,
      ServletContainerInitializer initializer) {
    this.servletContext = servletContext;
    this.initializer = initializer;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    initializer.initializeRequest(request, Utils.prepareContext("spring",
        new XWikiServletRequest(request),
        new XWikiServletResponse(response),
        new XWikiServletContext(servletContext)));
    initializer.initializeResponse(response);
    initializer.initializeSession(request);
    return true;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, Exception ex) throws Exception {
    initializer.cleanupSession();
  }
}
