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
package org.xwiki.container.internal;

import java.util.Stack;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.Response;
import org.xwiki.container.Session;

/**
 * We're using ThreadLocals to store the request, response and session so that each thread
 * (i.e. each user request) has its own value for these objects. In addition we sometime need
 * to create a new request, response or session even while in the same thread. For this use case
 * we've added the possibility to push/pop different implementations for these Objects.
 */
@Component
public class DefaultContainer implements Container {

  private ApplicationContext applicationContext;
  private ThreadLocal<Stack<Request>> request = new ThreadLocal<>();
  private ThreadLocal<Stack<Response>> response = new ThreadLocal<>();
  private ThreadLocal<Stack<Session>> session = new ThreadLocal<>();

  @Override
  public ApplicationContext getApplicationContext() {
    return this.applicationContext;
  }

  @Override
  public void pushRequest(Request request) {
    this.request.get().push(request);
  }

  @Override
  public void popRequest() {
    this.request.get().pop();
  }

  @Override
  public Request getRequest() {
    return this.request.get().peek();
  }

  @Override
  public Response getResponse() {
    return this.response.get().peek();
  }

  @Override
  public void pushResponse(Response response) {
    this.response.get().push(response);
  }

  @Override
  public void popResponse() {
    this.response.get().pop();
  }

  @Override
  public Session getSession() {
    return this.session.get().peek();
  }

  @Override
  public void pushSession(Session session) {
    this.session.get().push(session);
  }

  @Override
  public void popSession() {
    this.session.get().pop();
  }

  @Override
  public void setApplicationContext(ApplicationContext context) {
    this.applicationContext = context;
  }

  @Override
  public void setRequest(Request request) {
    Stack<Request> stack = new Stack<>();
    stack.push(request);
    this.request.set(stack);
  }

  @Override
  public void removeRequest() {
    this.request.remove();
  }

  @Override
  public void setResponse(Response response) {
    Stack<Response> stack = new Stack<>();
    stack.push(response);
    this.response.set(stack);
  }

  @Override
  public void removeResponse() {
    this.response.remove();
  }

  @Override
  public void setSession(Session session) {
    Stack<Session> stack = new Stack<>();
    stack.push(session);
    this.session.set(stack);
  }

  @Override
  public void removeSession() {
    this.session.remove();
  }
}
