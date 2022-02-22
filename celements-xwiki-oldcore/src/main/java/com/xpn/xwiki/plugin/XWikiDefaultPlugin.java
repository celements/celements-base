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

package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;

public class XWikiDefaultPlugin implements XWikiPluginInterface {

  private String name;
  private String className;

  public XWikiDefaultPlugin(String name, String className, XWikiContext context) {
    setClassName(className);
    setName(name);
  }

  @Override
  public void init(XWikiContext context) {}

  @Override
  public void virtualInit(XWikiContext context) {}

  @Override
  public void flushCache(XWikiContext context) {
    flushCache();
  }

  @Override
  public void flushCache() {

  }

  @Override
  public String commonTagsHandler(String line, XWikiContext context) {
    return line;
  }

  @Override
  public String startRenderingHandler(String line, XWikiContext context) {
    return line;
  }

  @Override
  public String outsidePREHandler(String line, XWikiContext context) {
    return line;
  }

  @Override
  public String insidePREHandler(String line, XWikiContext context) {
    return line;
  }

  @Override
  public String endRenderingHandler(String line, XWikiContext context) {
    return line;
  }

  @Override
  public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
    return null;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    if (!name.equals(className)) {
      this.name = name;
    }
  }

  @Override
  public String getClassName() {
    return name;
  }

  @Override
  public void setClassName(String name) {
    this.name = name;
  }

  @Override
  public void beginRendering(XWikiContext context) {}

  @Override
  public void endRendering(XWikiContext context) {}

  @Override
  public void beginParsing(XWikiContext context) {}

  @Override
  public String endParsing(String content, XWikiContext context) {
    return content;
  }

  @Override
  public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context) {
    return attachment;
  }
}
