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
import java.net.URL;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;

import com.celements.wiki.WikiService;
import com.google.common.base.Suppliers;
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

  private static final Supplier<URL> LOCALHOST = Suppliers.memoize(() -> {
    try {
      return new URL("localhost");
    } catch (MalformedURLException exc) {
      throw new IllegalArgumentException(exc);
    }
  });

  private final ServletContext servletContext;
  private final WikiService wikiService;
  private final XWikiURLFactoryService urlFactoryService;

  @Inject
  public DefaultXWikiStubContextProvider(
      ServletContext servletContext,
      WikiService wikiService,
      XWikiURLFactoryService urlFactoryService) {
    this.servletContext = servletContext;
    this.wikiService = wikiService;
    this.urlFactoryService = urlFactoryService;
  }

  @Override
  public XWikiContext createStubContext() {
    XWikiContext ctx = new XWikiContext();
    ctx.setMode(XWikiContext.MODE_SERVLET);
    ctx.setEngineContext(new XWikiServletContext(servletContext));
    ctx.setMainXWiki(XWikiConstant.MAIN_WIKI.getName());
    ctx.setDatabase(XWikiConstant.MAIN_WIKI.getName());
    ctx.setURL(wikiService.streamUrlsForWiki(XWikiConstant.MAIN_WIKI)
        .findFirst().orElseGet(LOCALHOST));
    ctx.setURLFactory(urlFactoryService.createURLFactory(ctx));
    XWikiServletRequestStub requestStub = new XWikiServletRequestStub();
    requestStub.setHost(ctx.getURL().getHost());
    requestStub.setScheme(ctx.getURL().getProtocol());
    ctx.setRequest(new XWikiServletRequest(requestStub));
    ctx.setResponse(new XWikiServletResponseStub());
    ctx.setDoc(new XWikiDocument());
    return ctx;
  }

}
