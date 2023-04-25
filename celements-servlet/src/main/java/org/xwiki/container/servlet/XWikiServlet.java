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
package org.xwiki.container.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.web.context.WebApplicationContext;
import org.xwiki.action.ActionException;
import org.xwiki.action.ActionManager;

/**
 * XWiki servlet implementation.
 *
 * @version $Id$
 */
public class XWikiServlet extends HttpServlet {

  /** Serial version ID. */
  private static final long serialVersionUID = 1L;

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    WebApplicationContext springCtx = (WebApplicationContext) getServletContext()
        .getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    if (springCtx == null) {
      throw new ServletException("Plexus container is not initialized");
    }
    ActionManager manager;
    try {
      manager = springCtx.getBean(ActionManager.class);
    } catch (NoSuchBeanDefinitionException e) {
      throw new ServletException("Failed to locate Action Manager component.", e);
    }
    try {
      ServletContainerInitializer containerInitializer = springCtx
          .getBean(ServletContainerInitializer.class);
      containerInitializer.initializeRequest(request);
      containerInitializer.initializeResponse(response);
      containerInitializer.initializeSession(request);
    } catch (Exception e) {
      try {
        manager.handleRequest("error", e);
        return;
      } catch (ActionException ae) {
        throw new ServletException("Failed to call the error Action", ae);
      }
    }
    try {
      manager.handleRequest();
    } catch (ActionException e) {
      throw new ServletException("Failed to handle request", e);
    }
  }
}
