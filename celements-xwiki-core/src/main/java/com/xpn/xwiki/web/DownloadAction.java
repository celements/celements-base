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
 */
package com.xpn.xwiki.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import com.google.common.net.MediaType;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.util.Util;

public class DownloadAction extends XWikiAction {

  /**
   * The identifier of the download action.
   */
  public static final String ACTION_NAME = "download";

  public String getFileName(String path, String action) {
    path = path.substring(path.indexOf("/" + action));
    int pos = 0;
    for (int i = 0; i < 3; i++) {
      pos = path.indexOf("/", pos + 1);
    }
    if (path.indexOf("/", pos + 1) > 0) {
      return path.substring(pos + 1, path.indexOf("/", pos + 1));
    }
    return path.substring(pos + 1);
  }

  @Override
  public String render(XWikiContext context) throws XWikiException {
    XWikiRequest request = context.getRequest();
    XWikiResponse response = context.getResponse();
    XWikiDocument doc = context.getDoc();
    String path = request.getRequestURI();
    String filename = Util.decodeURI(getFileName(path, "download"), context);
    XWikiAttachment attachment;

    if (request.getParameter("id") != null) {
      int id = Integer.parseInt(request.getParameter("id"));
      attachment = doc.getAttachmentList().get(id);
    } else {
      attachment = doc.getAttachment(filename);
    }

    if (attachment == null) {
      Object[] args = { filename };
      throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
          XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, "Attachment {0} not found",
          null, args);
    }

    XWikiPluginManager plugins = context.getWiki().getPluginManager();
    attachment = plugins.downloadAttachment(attachment, context);
    // Choose the right content type
    MediaType mediaType = MediaType.parse(attachment.getMimeType(context));
    response.setContentType(mediaType.toString());
    if (Stream.of(
        MediaType.ANY_TEXT_TYPE,
        MediaType.JSON_UTF_8,
        MediaType.JAVASCRIPT_UTF_8,
        MediaType.APPLICATION_XML_UTF_8)
        .anyMatch(mediaType::is)) {
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    long lastModifiedOnClient = request.getDateHeader("If-Modified-Since");
    long lastModifiedOnServer = attachment.getDate().getTime();
    if ((lastModifiedOnClient != -1) && (lastModifiedOnClient >= lastModifiedOnServer)) {
      response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      return null;
    }

    String ofilename = Util.encodeURI(attachment.getFilename(), context).replaceAll("\\+", " ");

    // The inline attribute of Content-Disposition tells the browser that they should display
    // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
    // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
    // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
    // Save dialog box (exe, zip, xar, etc).
    String dispType = "inline";

    if ("1".equals(request.getParameter("force-download"))) {
      dispType = "attachment";
    }
    response.addHeader("Content-disposition", dispType + "; filename=\"" + ofilename + "\"");

    response.setDateHeader("Last-Modified", lastModifiedOnServer);

    // Sending the content of the attachment
    try {
      response.setContentLength(attachment.getContentSize(context));
      IOUtils.copy(attachment.getContentInputStream(context), response.getOutputStream());
    } catch (XWikiException e) {
      Object[] args = { filename };
      throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
          XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
          "Attachment content {0} not found", null, args);
    } catch (IOException e) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
          XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
          "Exception while sending response", e);
    }
    return null;
  }
}
