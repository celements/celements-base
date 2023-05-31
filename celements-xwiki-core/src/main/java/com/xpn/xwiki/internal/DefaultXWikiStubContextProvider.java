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
package com.xpn.xwiki.internal;

import java.net.MalformedURLException;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.ServerUrlUtilsRole;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactoryService;

/**
 * Default implementation of XWikiStubContextProvider.
 *
 * @since 2.0M3
 */
@Component
public class DefaultXWikiStubContextProvider implements XWikiStubContextProvider {

  private final ServletContext servletContext;
  private final ServerUrlUtilsRole serverUrlUtils;
  private final XWikiURLFactoryService urlFactoryService;

  @Inject
  public DefaultXWikiStubContextProvider(
      ServletContext servletContext,
      ServerUrlUtilsRole serverUrlUtils,
      XWikiURLFactoryService urlFactoryService) {
    this.servletContext = servletContext;
    this.serverUrlUtils = serverUrlUtils;
    this.urlFactoryService = urlFactoryService;
  }

  @Override
  public XWikiContext createStubContext() {
    try {
      XWikiContext ctx = new XWikiContext();
      ctx.setMode(XWikiContext.MODE_SERVLET);
      ctx.setEngineContext(new XWikiServletContext(servletContext));
      ctx.setMainXWiki(XWikiConstant.MAIN_WIKI.getName());
      ctx.setDatabase(XWikiConstant.MAIN_WIKI.getName());
      ctx.setURL(serverUrlUtils.getServerURL());
      ctx.setURLFactory(urlFactoryService.createURLFactory(ctx));
      XWikiServletRequestStub requestStub = new XWikiServletRequestStub();
      requestStub.setHost(ctx.getURL().getHost());
      requestStub.setScheme(ctx.getURL().getProtocol());
      ctx.setRequest(new XWikiServletRequest(requestStub));
      ctx.setResponse(new XWikiServletResponseStub());
      ctx.setDoc(new XWikiDocument());
      return ctx;
    } catch (MalformedURLException exc) {
      throw new IllegalStateException("failing to create initial xwiki context", exc);
    }
  }

}
