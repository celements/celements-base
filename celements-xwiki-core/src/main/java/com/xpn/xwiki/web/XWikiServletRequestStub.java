package com.xpn.xwiki.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

/**
 * This stub is intended to simulate a servlet request in a daemon context, in order to be able to
 * create a custom XWiki
 * context. This trick is used in to give a daemon thread access to the XWiki api.
 *
 * @version $Id$
 */
public class XWikiServletRequestStub implements XWikiRequest {

  /**
   * The scheme used by the runtime instance. This is required for creating URLs from daemon thread.
   */
  private String scheme;

  public XWikiServletRequestStub() {
    this.host = "";
  }

  private String host;

  public void setHost(String host) {
    this.host = host;
  }

  @Override
  public String getHeader(String s) {
    if (s.equals("x-forwarded-host")) {
      return this.host;
    }
    return "";
  }

  @Override
  public String get(String name) {
    return "";
  }

  @Override
  public HttpServletRequest getHttpServletRequest() {
    return null;
  }

  @Override
  public Cookie getCookie(String cookieName) {
    return null;
  }

  @Override
  public String getAuthType() {
    return "";
  }

  @Override
  public Cookie[] getCookies() {
    return new Cookie[0];
  }

  @Override
  public long getDateHeader(String s) {
    return 0;
  }

  @Override
  public Enumeration<String> getHeaders(String s) {
    return null;
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return null;
  }

  @Override
  public int getIntHeader(String s) {
    return 0;
  }

  @Override
  public String getMethod() {
    return null;
  }

  @Override
  public String getPathInfo() {
    return null;
  }

  @Override
  public String getPathTranslated() {
    return null;
  }

  @Override
  public String getContextPath() {
    return null;
  }

  @Override
  public String getQueryString() {
    return "";
  }

  @Override
  public String getRemoteUser() {
    return null;
  }

  @Override
  public boolean isUserInRole(String s) {
    return false;
  }

  @Override
  public Principal getUserPrincipal() {
    return null;
  }

  @Override
  public String getRequestedSessionId() {
    return null;
  }

  @Override
  public String getRequestURI() {
    return null;
  }

  @Override
  public StringBuffer getRequestURL() {
    return new StringBuffer();
  }

  @Override
  public String getServletPath() {
    return null;
  }

  @Override
  public HttpSession getSession(boolean b) {
    return null;
  }

  @Override
  public HttpSession getSession() {
    return null;
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return false;
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return false;
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public boolean isRequestedSessionIdFromUrl() {
    return false;
  }

  @Override
  public Object getAttribute(String s) {
    return null;
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return null;
  }

  @Override
  public String getCharacterEncoding() {
    return null;
  }

  @Override
  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {

  }

  @Override
  public int getContentLength() {
    return 0;
  }

  @Override
  public String getContentType() {
    return null;
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return null;
  }

  @Override
  public String getParameter(String s) {
    return null;
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return null;
  }

  @Override
  public String[] getParameterValues(String s) {
    return new String[0];
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return null;
  }

  @Override
  public String getProtocol() {
    return null;
  }

  public void setScheme(String scheme) {
    this.scheme = scheme;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public String getServerName() {
    return null;
  }

  @Override
  public int getServerPort() {
    return 0;
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return null;
  }

  @Override
  public String getRemoteAddr() {
    return null;
  }

  @Override
  public String getRemoteHost() {
    return null;
  }

  @Override
  public void setAttribute(String s, Object o) {}

  @Override
  public void removeAttribute(String s) {}

  @Override
  public Locale getLocale() {
    return null;
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return null;
  }

  @Override
  public boolean isSecure() {
    return false;
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String s) {
    return null;
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public String getRealPath(String s) {
    return null;
  }

  @Override
  public int getRemotePort() {
    return 0;
  }

  @Override
  public String getLocalName() {
    return null;
  }

  @Override
  public String getLocalAddr() {
    return null;
  }

  @Override
  public int getLocalPort() {
    return 0;
  }

  @Override
  public String changeSessionId() {
    return null;
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return false;
  }

  @Override
  public void login(String username, String password) throws ServletException {}

  @Override
  public void logout() throws ServletException {}

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return null;
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
      throws IOException, ServletException {
    return null;
  }

  @Override
  public long getContentLengthLong() {
    return 0;
  }

  @Override
  public ServletContext getServletContext() {
    return null;
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    return null;
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    return null;
  }

  @Override
  public boolean isAsyncStarted() {
    return false;
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

  @Override
  public AsyncContext getAsyncContext() {
    return null;
  }

  @Override
  public DispatcherType getDispatcherType() {
    return null;
  }

}
