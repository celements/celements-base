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

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Deprecated
public class XWikiPortlet extends GenericPortlet {

  protected final Log logger = LogFactory.getLog(getClass());

  private String name = "XWiki Portlet";
  public static final PortletMode CONFIG_PORTLET_MODE = new PortletMode("config");
  public static final String ROOT_SPACE_PARAM_NAME = "rootSpace";

  @Override
  protected String getTitle(RenderRequest renderRequest) {
    return name;
  }

  @Override
  protected void doDispatch(RenderRequest aRenderRequest, RenderResponse aRenderResponse)
      throws PortletException, IOException {
    throw new UnsupportedOperationException("Portlet not supported.");
  }

  @Override
  public void processAction(ActionRequest actionRequest, ActionResponse actionResponse)
      throws PortletException, IOException {
    throw new UnsupportedOperationException("Portlet not supported.");
  }

  @Override
  protected void doView(RenderRequest renderRequest, RenderResponse renderResponse)
      throws PortletException, IOException {
    throw new UnsupportedOperationException("Portlet not supported.");
  }

}
