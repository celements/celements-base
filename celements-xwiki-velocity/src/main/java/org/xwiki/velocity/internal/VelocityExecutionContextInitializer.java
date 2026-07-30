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
package org.xwiki.velocity.internal;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static org.xwiki.velocity.VelocityExecutionProp.*;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.velocity.VelocityContext;
import org.springframework.stereotype.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Allow registering the Velocity Context in the Execution Context object since it's shared during
 * the whole execution of the current request.
 */
@Component
public class VelocityExecutionContextInitializer implements ExecutionContextInitializer {

  private final VelocityContextFactory factory;

  @Inject
  public VelocityExecutionContextInitializer(VelocityContextFactory factory) {
    this.factory = factory;
  }

  @Override
  public void initialize(ExecutionContext context, ExecutionContext source)
      throws ExecutionContextException {
    try {
      var vCtx = Optional.ofNullable(source)
          .flatMap(eCtx -> eCtx.get(VELOCITY_CONTEXT))
          .map(VelocityContext::new)
          .orElseGet(rethrowSupplier(factory::createContext));
      context.set(VELOCITY_CONTEXT, vCtx);
    } catch (XWikiVelocityException e) {
      throw new ExecutionContextException("Failed to initialize Velocity Context", e);
    }
  }
}
