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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @version $Id$
 */
public class XWikiRequestProcessor extends org.apache.struts.action.RequestProcessor {

  protected static final Log LOG = LogFactory.getLog(XWikiRequestProcessor.class);

  /**
   * {@inheritDoc}
   *
   * @see org.apache.struts.action.RequestProcessor#processPath(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected String processPath(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse)
      throws IOException {
    String result = super.processPath(httpServletRequest, httpServletResponse);
    if (StringUtils.countMatches(result, "/") <= 2) {
      return "/view/";
    } else {
      return result.substring(0, result.indexOf("/", 1) + 1);
    }
  }
}
