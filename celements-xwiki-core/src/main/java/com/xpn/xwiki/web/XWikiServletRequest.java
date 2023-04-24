/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */

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

import org.apache.struts.upload.MultipartRequestWrapper;

import com.xpn.xwiki.util.Util;

import one.util.streamex.EntryStream;

public class XWikiServletRequest implements XWikiRequest {

  private final HttpServletRequest request;

  public XWikiServletRequest(HttpServletRequest request) {
    this.request = request;
  }

  /**
   * Turn Windows CP1252 Characters to iso-8859-1 characters when possible, HTML entities when
   * needed. This filtering
   * works on Tomcat (not Jetty).
   *
   * @param text
   *          The text to filter
   * @return filtered text
   */
  public String filterString(String text) {
    if (text == null) {
      return null;
    }

    // In case we are running in ISO we need to take care or some windows-1252 characters
    // that are commonly copy pasted by users from web sites or desktop applications
    // If we don't transform these characters then some databases running in the latin charset mode
    // will drop the characters and will we only see that on server restart.
    // This happens for example using MySQL both with tomcat and Jetty
    // See bug : http://jira.xwiki.org/jira/browse/XWIKI-2422
    // Source : http://www.microsoft.com/typography/unicode/1252.htm
    if (this.request.getCharacterEncoding().startsWith("ISO-8859")) {
      // EURO SIGN
      text = text.replace("\u0080", "&euro;");
      // SINGLE LOW-9 QUOTATION MARK
      text = text.replace("\u0082", "&sbquo;");
      // LATIN SMALL LETTER F WITH HOOK
      text = text.replace("\u0083", "&fnof;");
      // DOUBLE LOW-9 QUOTATION MARK
      text = text.replace("\u0084", "&bdquo;");
      // HORIZONTAL ELLIPSIS, entity : &hellip;
      text = text.replace("\u0085", "...");
      // DAGGER
      text = text.replace("\u0086", "&dagger;");
      // DOUBLE DAGGER
      text = text.replace("\u0087", "&Dagger;");
      // MODIFIER LETTER CIRCUMFLEX ACCENT
      text = text.replace("\u0088", "&circ;");
      // PER MILLE SIGN
      text = text.replace("\u0089", "&permil;");
      // LATIN CAPITAL LETTER S WITH CARON
      text = text.replace("\u008a", "&Scaron;");
      // SINGLE LEFT-POINTING ANGLE QUOTATION MARK, entity : &lsaquo;
      text = text.replace("\u008b", "'");
      // LATIN CAPITAL LIGATURE OE
      text = text.replace("\u008c", "&OElig;");
      // LATIN CAPITAL LETTER Z WITH CARON
      text = text.replace("\u008e", "&#381;");
      // LEFT SINGLE QUOTATION MARK, entity : &lsquo;
      text = text.replace("\u0091", "'");
      // RIGHT SINGLE QUOTATION MARK, entity : &rsquo;
      text = text.replace("\u0092", "'");
      // LEFT DOUBLE QUOTATION MARK, entity : &ldquo;
      text = text.replace("\u0093", "\"");
      // RIGHT DOUBLE QUOTATION MARK, entity : &rdquo;
      text = text.replace("\u0094", "\"");
      // BULLET
      text = text.replace("\u0095", "&bull;");
      // EN DASH, entity : &ndash;
      text = text.replace("\u0096", "-");
      // EM DASH, entity : &mdash;
      text = text.replace("\u0097", "-");
      // SMALL TILDE
      text = text.replace("\u0098", "&tilde;");
      // TRADE MARK SIGN
      text = text.replace("\u0099", "&trade;");
      // LATIN SMALL LETTER S WITH CARON
      text = text.replace("\u009a", "&scaron;");
      // SINGLE RIGHT-POINTING ANGLE QUOTATION MARK, entity : &rsaquo;
      text = text.replace("\u009b", "'");
      // LATIN SMALL LIGATURE OE
      text = text.replace("\u009c", "&oelig;");
      // LATIN SMALL LETTER Z WITH CARON
      text = text.replace("\u009e", "&#382;");
      // LATIN CAPITAL LETTER Y WITH DIAERESIS
      text = text.replace("\u009f", "&Yuml;");

    }
    return text;
  }

  public String[] filterStringArray(String[] text) {
    if (text == null) {
      return null;
    }

    for (int i = 0; i < text.length; i++) {
      text[i] = filterString(text[i]);
    }
    return text;
  }

  @Override
  public String get(String name) {
    return filterString(this.request.getParameter(name));
  }

  @Override
  public HttpServletRequest getHttpServletRequest() {
    return this.request;
  }

  @Override
  public String getAuthType() {
    return this.request.getAuthType();
  }

  @Override
  public Cookie[] getCookies() {
    return this.request.getCookies();
  }

  @Override
  public Cookie getCookie(String cookieName) {
    return Util.getCookie(cookieName, this);
  }

  @Override
  public long getDateHeader(String s) {
    return this.request.getDateHeader(s);
  }

  @Override
  public String getHeader(String s) {
    return this.request.getHeader(s);
  }

  @Override
  public Enumeration<String> getHeaders(String s) {
    return this.request.getHeaders(s);
  }

  @Override
  public Enumeration<String> getHeaderNames() {
    return this.request.getHeaderNames();
  }

  @Override
  public int getIntHeader(String s) {
    return this.request.getIntHeader(s);
  }

  @Override
  public String getMethod() {
    return this.request.getMethod();
  }

  @Override
  public String getPathInfo() {
    return this.request.getPathInfo();
  }

  @Override
  public String getPathTranslated() {
    return this.request.getPathTranslated();
  }

  @Override
  public String getContextPath() {
    return this.request.getContextPath();
  }

  @Override
  public String getQueryString() {
    return this.request.getQueryString();
  }

  @Override
  public String getRemoteUser() {
    return this.request.getRemoteUser();
  }

  @Override
  public boolean isUserInRole(String s) {
    return this.request.isUserInRole(s);
  }

  @Override
  public Principal getUserPrincipal() {
    return this.request.getUserPrincipal();
  }

  @Override
  public String getRequestedSessionId() {
    return this.request.getRequestedSessionId();
  }

  @Override
  public String getRequestURI() {
    return this.request.getRequestURI();
  }

  @Override
  public StringBuffer getRequestURL() {
    StringBuffer requestURL = this.request.getRequestURL();
    if ((requestURL == null) && (this.request instanceof MultipartRequestWrapper)) {
      requestURL = ((MultipartRequestWrapper) this.request).getRequest().getRequestURL();
    }
    return requestURL;
  }

  @Override
  public String getServletPath() {
    return this.request.getServletPath();
  }

  @Override
  public HttpSession getSession(boolean b) {
    return this.request.getSession(b);
  }

  @Override
  public HttpSession getSession() {
    return this.request.getSession();
  }

  @Override
  public boolean isRequestedSessionIdValid() {
    return this.request.isRequestedSessionIdValid();
  }

  @Override
  public boolean isRequestedSessionIdFromCookie() {
    return this.request.isRequestedSessionIdFromCookie();
  }

  @Override
  public boolean isRequestedSessionIdFromURL() {
    return this.request.isRequestedSessionIdFromURL();
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public boolean isRequestedSessionIdFromUrl() {
    return this.request.isRequestedSessionIdFromURL();
  }

  @Override
  public Object getAttribute(String s) {
    return this.request.getAttribute(s);
  }

  @Override
  public Enumeration<String> getAttributeNames() {
    return this.request.getAttributeNames();
  }

  @Override
  public String getCharacterEncoding() {
    return this.request.getCharacterEncoding();
  }

  @Override
  public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
    this.request.setCharacterEncoding(s);
  }

  @Override
  public int getContentLength() {
    return this.request.getContentLength();
  }

  @Override
  public String getContentType() {
    return this.request.getContentType();
  }

  @Override
  public ServletInputStream getInputStream() throws IOException {
    return this.request.getInputStream();
  }

  @Override
  public String getParameter(String s) {
    return filterString(this.request.getParameter(s));
  }

  @Override
  public Enumeration<String> getParameterNames() {
    return this.request.getParameterNames();
  }

  @Override
  public String[] getParameterValues(String s) {
    String[] origResult = this.request.getParameterValues(s);
    return filterStringArray(origResult);
  }

  @Override
  public Map<String, String[]> getParameterMap() {
    return EntryStream.of(request.getParameterMap())
        .mapValues(this::filterStringArray)
        .toMap();
  }

  @Override
  public String getProtocol() {
    return this.request.getProtocol();
  }

  @Override
  public String getScheme() {
    return this.request.getScheme();
  }

  @Override
  public String getServerName() {
    return this.request.getServerName();
  }

  @Override
  public int getServerPort() {
    return this.request.getServerPort();
  }

  @Override
  public BufferedReader getReader() throws IOException {
    return this.request.getReader();
  }

  @Override
  public String getRemoteAddr() {
    if (this.request.getHeader("x-forwarded-for") != null) {
      return this.request.getHeader("x-forwarded-for");
    }
    return this.request.getRemoteAddr();
  }

  @Override
  public String getRemoteHost() {
    if (this.request.getHeader("x-forwarded-for") != null) {
      return this.request.getHeader("x-forwarded-for");
    }
    return this.request.getRemoteHost();
  }

  @Override
  public void setAttribute(String s, Object o) {
    this.request.setAttribute(s, o);
  }

  @Override
  public void removeAttribute(String s) {
    this.request.removeAttribute(s);
  }

  @Override
  public Locale getLocale() {
    return this.request.getLocale();
  }

  @Override
  public Enumeration<Locale> getLocales() {
    return this.request.getLocales();
  }

  @Override
  public boolean isSecure() {
    return this.request.isSecure();
  }

  @Override
  public RequestDispatcher getRequestDispatcher(String s) {
    return this.request.getRequestDispatcher(s);
  }

  /**
   * @deprecated
   */
  @Override
  @Deprecated
  public String getRealPath(String s) {
    return this.request.getRealPath(s);
  }

  @Override
  public int getRemotePort() {
    return this.request.getRemotePort();
  }

  @Override
  public String getLocalName() {
    return this.request.getLocalName();
  }

  @Override
  public String getLocalAddr() {
    return this.request.getLocalAddr();
  }

  @Override
  public int getLocalPort() {
    return this.request.getLocalPort();
  }

  @Override
  public String changeSessionId() {
    return request.changeSessionId();
  }

  @Override
  public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
    return request.authenticate(response);
  }

  @Override
  public void login(String username, String password) throws ServletException {
    request.login(username, password);
  }

  @Override
  public void logout() throws ServletException {
    request.logout();
  }

  @Override
  public Collection<Part> getParts() throws IOException, ServletException {
    return request.getParts();
  }

  @Override
  public Part getPart(String name) throws IOException, ServletException {
    return request.getPart(name);
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
      throws IOException, ServletException {
    return request.upgrade(handlerClass);
  }

  @Override
  public long getContentLengthLong() {
    return request.getContentLengthLong();
  }

  @Override
  public ServletContext getServletContext() {
    return request.getServletContext();
  }

  @Override
  public AsyncContext startAsync() throws IllegalStateException {
    return request.startAsync();
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
      throws IllegalStateException {
    return request.startAsync(servletRequest, servletResponse);
  }

  @Override
  public boolean isAsyncStarted() {
    return request.isAsyncStarted();
  }

  @Override
  public boolean isAsyncSupported() {
    return request.isAsyncSupported();
  }

  @Override
  public AsyncContext getAsyncContext() {
    return request.getAsyncContext();
  }

  @Override
  public DispatcherType getDispatcherType() {
    return request.getDispatcherType();
  }
}
