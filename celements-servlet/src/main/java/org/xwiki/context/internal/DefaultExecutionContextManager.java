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

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.context.ExecutionContextManager;

@Component
public class DefaultExecutionContextManager implements ExecutionContextManager {

  private final List<ExecutionContextInitializer> initializers;
  private final Execution execution;

  @Inject
  public DefaultExecutionContextManager(
      List<ExecutionContextInitializer> initializers,
      Execution execution) {
    this.initializers = List.copyOf(initializers);
    this.execution = execution;
  }

  @Override
  public ExecutionContext clone(ExecutionContext context) throws ExecutionContextException {
    var clone = new ExecutionContext();
    execution.pushContext(clone);
    try {
      initialize(clone, context);
    } finally {
      execution.popContext();
    }
    return clone;
  }

  @Override
  public void initialize(ExecutionContext context) throws ExecutionContextException {
    initialize(context, null);
  }

  private void initialize(ExecutionContext context, ExecutionContext source)
      throws ExecutionContextException {
    for (ExecutionContextInitializer initializer : this.initializers) {
      initializer.initialize(context, source);
    }
  }

}
