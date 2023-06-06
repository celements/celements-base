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

import static com.xpn.xwiki.XWikiExecutionProp.*;

import java.net.URI;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.WikiService;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.ViewAction;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;
import com.xpn.xwiki.web.XWikiURLFactoryService;

/**
 * Default implementation of XWikiStubContextProvider.
 *
 * @since 2.0M3
 */
@Component
public class DefaultXWikiStubContextProvider implements XWikiStubContextProvider {

  private static final URI LOCALHOST = URI.create("http://localhost");

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
  public XWikiContext createStubContext(ExecutionContext execContext) {
    XWikiContext ctx = new XWikiContext();
    ctx.setEngineContext(new XWikiServletContext(servletContext));
    ctx.setMainXWiki(XWikiConstant.MAIN_WIKI.getName());
    WikiReference wikiRef = execContext.computeIfAbsent(WIKI, () -> XWikiConstant.MAIN_WIKI);
    ctx.setDatabase(wikiRef.getName());
    ctx.setUri(execContext.computeIfAbsent(XWIKI_REQUEST_URI,
        () -> wikiService.streamUrisForWiki(wikiRef).findFirst().orElse(LOCALHOST)));
    ctx.setRequest(execContext.computeIfAbsent(XWIKI_REQUEST, () -> {
      XWikiServletRequestStub stub = new XWikiServletRequestStub();
      stub.setHost(ctx.getUri().getHost());
      stub.setScheme(ctx.getUri().getScheme());
      return new XWikiServletRequest(stub);
    }));
    ctx.setResponse(execContext.computeIfAbsent(XWIKI_RESPONSE, () -> {
      XWikiServletResponseStub stub = new XWikiServletResponseStub();
      return new XWikiServletResponse(stub);
    }));
    ctx.setAction(execContext.computeIfAbsent(XWIKI_REQUEST_ACTION, () -> ViewAction.VIEW_ACTION));
    ctx.setDoc(execContext.computeIfAbsent(DOC, XWikiDocument::new));
    ctx.setURLFactory(urlFactoryService.createURLFactory(ctx));
    return ctx;
  }

}
