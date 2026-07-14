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
package org.xwiki.context.internal;

import static com.google.common.base.Preconditions.*;

import java.util.Stack;

import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * Holds the Execution Context object. Note that we require this Execution component since we want
 * to be able to pass
 * the Execution Context to singleton components. Thus this holder is a singleton itself and the
 * Execution Context is
 * saved as a ThreadLocal variable.
 *
 * @version $Id$
 * @since 1.5M2
 */
@Component
public class DefaultExecution implements Execution {

  private final ThreadLocal<Stack<ExecutionContext>> context = new ThreadLocal<>();

  private Stack<ExecutionContext> getOrCreateStack() {
    Stack<ExecutionContext> stack = context.get();
    if (stack == null) {
      stack = new Stack<>();
      context.set(stack);
    }
    return stack;
  }

  @Override
  public void pushContext(ExecutionContext newContext) {
    getOrCreateStack().push(checkNotNull(newContext));
  }

  @Override
  public void popContext() {
    Stack<ExecutionContext> stack = context.get();
    checkState((stack != null) && !stack.isEmpty(), "no execution context to pop");
    stack.pop();
  }

  @Override
  public ExecutionContext getContext() {
    Stack<ExecutionContext> stack = context.get();
    return (stack == null) || stack.isEmpty() ? null : stack.peek();
  }

  @Override
  public void setContext(ExecutionContext newContext) {
    removeContext();
    if (newContext != null) {
      pushContext(newContext);
    }
  }

  @Override
  public void removeContext() {
    context.remove();
  }
}
