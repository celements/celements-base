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

import java.net.MalformedURLException;
import java.net.URL;

import com.xpn.xwiki.XWikiContext;

public interface XWikiURLFactory {

  void init(XWikiContext context);

  URL createURL(String web, String name, XWikiContext context);

  URL createURL(String web, String name, String action, XWikiContext context);

  URL createURL(String web, String name, String action, boolean redirect,
      XWikiContext context);

  URL createURL(String web, String name, String action, String querystring, String anchor,
      XWikiContext context);

  URL createExternalURL(String web, String name, String action, String querystring,
      String anchor,
      XWikiContext context);

  URL createURL(String web, String name, String action, String querystring, String anchor,
      String xwikidb,
      XWikiContext context);

  URL createExternalURL(String web, String name, String action, String querystring,
      String anchor,
      String xwikidb, XWikiContext context);

  URL createSkinURL(String filename, String skin, XWikiContext context);

  URL createSkinURL(String filename, String web, String name, XWikiContext context);

  URL createSkinURL(String filename, String web, String name, String xwikidb,
      XWikiContext context);

  URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context);

  URL createAttachmentURL(String filename, String web, String name, String action,
      String querystring,
      XWikiContext context);

  URL createAttachmentURL(String filename, String web, String name, String action,
      String querystring,
      String xwikidb, XWikiContext context);

  URL createAttachmentRevisionURL(String filename, String web, String name, String revision,
      String querystring, XWikiContext context);

  URL createAttachmentRevisionURL(String filename, String web, String name, String revision,
      String querystring, String xwikidb, XWikiContext context);

  URL getRequestURL(XWikiContext context);

  String getURL(URL url, XWikiContext context);

  /**
   * Generate the base external URL to access this server.
   *
   * @param context
   *          the XWiki context.
   * @return the URL of the server.
   * @throws MalformedURLException
   *           error when creating the {@link URL}.
   */
  URL getServerURL(XWikiContext context) throws MalformedURLException;
}
