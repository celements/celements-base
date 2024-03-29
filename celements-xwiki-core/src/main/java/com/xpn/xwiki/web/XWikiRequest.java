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

import java.net.URI;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;

public interface XWikiRequest extends HttpServletRequest {

  String EXEC_CONTEXT_KEY = "xwiki.request";
  String URI_EXEC_CONTEXT_KEY = "xwiki.request.uri";
  String ACTION_EXEC_CONTEXT_KEY = "xwiki.request.action";

  String get(String name);

  HttpServletRequest getHttpServletRequest();

  Cookie getCookie(String cookieName);

  default URI getUri() {
    String requestURL = getRequestURL().toString();
    if (!Strings.isNullOrEmpty(getQueryString())) {
      requestURL += "?" + getQueryString();
    }
    return URI.create(requestURL);
  }

}
