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

package com.xpn.xwiki.render;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiPluginRenderer implements XWikiRenderer {

  public XWikiPluginRenderer() {}

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.render.XWikiRenderer#render(java.lang.String,
   *      com.xpn.xwiki.doc.XWikiDocument,
   *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public String render(String content, XWikiDocument contentdoc, XWikiDocument doc,
      XWikiContext context) {
    return content;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.render.XWikiRenderer#flushCache()
   */
  @Override
  public void flushCache() {
    // To change body of implemented methods use File | Settings | File Templates.
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.render.XWikiRenderer#convertMultiLine(java.lang.String, java.lang.String,
   *      java.lang.String,
   *      java.lang.String, com.xpn.xwiki.render.XWikiVirtualMacro, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public String convertMultiLine(String macroname, String params, String data, String allcontent,
      XWikiVirtualMacro macro, XWikiContext context) {
    return allcontent;
  }

  /**
   * {@inheritDoc}
   *
   * @see com.xpn.xwiki.render.XWikiRenderer#convertSingleLine(java.lang.String, java.lang.String,
   *      java.lang.String,
   *      com.xpn.xwiki.render.XWikiVirtualMacro, com.xpn.xwiki.XWikiContext)
   */
  @Override
  public String convertSingleLine(String macroname, String params, String allcontent,
      XWikiVirtualMacro macro,
      XWikiContext context) {
    return allcontent;
  }
}
