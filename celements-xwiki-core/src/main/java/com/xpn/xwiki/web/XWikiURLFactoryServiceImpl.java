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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

public class XWikiURLFactoryServiceImpl implements XWikiURLFactoryService {

  private static final Logger LOG = LoggerFactory.getLogger(XWikiURLFactoryServiceImpl.class);

  public XWikiURLFactoryServiceImpl() {}

  @Deprecated
  public XWikiURLFactoryServiceImpl(XWiki xwiki) {}

  @Deprecated
  @Override
  public XWikiURLFactory createURLFactory(int mode, XWikiContext context) {
    return createURLFactory(context);
  }

  @Override
  public XWikiURLFactory createURLFactory(XWikiContext context) {
    XWikiURLFactory urlf = null;
    try {
      urlf = new XWikiServletURLFactory();
      urlf.init(context);
    } catch (Exception e) {
      LOG.error("Failed to create url factory", e);
    }
    return urlf;
  }

}
