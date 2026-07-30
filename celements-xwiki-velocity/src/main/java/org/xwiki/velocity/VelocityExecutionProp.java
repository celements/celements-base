package org.xwiki.velocity;

import org.apache.velocity.VelocityContext;
import org.xwiki.context.ExecutionContext.Property;

public final class VelocityExecutionProp {

  public static final Property<VelocityContext> VELOCITY_CONTEXT = new Property<>(
      "velocityContext", VelocityContext.class);

  private VelocityExecutionProp() {}

}
