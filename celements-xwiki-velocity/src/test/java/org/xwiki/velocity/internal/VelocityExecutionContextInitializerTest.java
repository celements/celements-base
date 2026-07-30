package org.xwiki.velocity.internal;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.xwiki.velocity.VelocityExecutionProp.*;

import org.apache.velocity.VelocityContext;
import org.junit.Test;
import org.xwiki.context.ExecutionContext;
import org.xwiki.velocity.VelocityContextFactory;

public class VelocityExecutionContextInitializerTest {

  @Test
  public void test_initializeFreshContext() throws Exception {
    VelocityContextFactory factory = createMock(VelocityContextFactory.class);
    VelocityContext freshContext = new VelocityContext();
    expect(factory.createContext()).andReturn(freshContext);
    replay(factory);

    ExecutionContext context = new ExecutionContext();
    new VelocityExecutionContextInitializer(factory).initialize(context, null);

    assertSame(freshContext, context.get(VELOCITY_CONTEXT).orElseThrow());
    verify(factory);
  }

  @Test
  public void test_initializeChildContextInheritsSourceVariables() throws Exception {
    VelocityContext sourceVelocityContext = new VelocityContext();
    sourceVelocityContext.put("doc", "sourceDoc");
    sourceVelocityContext.put("services", "configuredServices");
    ExecutionContext source = new ExecutionContext();
    source.set(VELOCITY_CONTEXT, sourceVelocityContext);
    VelocityContextFactory factory = createMock(VelocityContextFactory.class);
    replay(factory);

    ExecutionContext context = new ExecutionContext();
    new VelocityExecutionContextInitializer(factory).initialize(context, source);

    VelocityContext child = context.get(VELOCITY_CONTEXT).orElseThrow();
    assertNotSame(sourceVelocityContext, child);
    assertEquals("sourceDoc", child.get("doc"));
    assertEquals("configuredServices", child.get("services"));
    verify(factory);
  }

  @Test
  public void test_initializeChildContextIsolatesWrites() throws Exception {
    VelocityContext sourceVelocityContext = new VelocityContext();
    sourceVelocityContext.put("doc", "sourceDoc");
    ExecutionContext source = new ExecutionContext();
    source.set(VELOCITY_CONTEXT, sourceVelocityContext);
    VelocityContextFactory factory = createMock(VelocityContextFactory.class);
    replay(factory);

    ExecutionContext context = new ExecutionContext();
    new VelocityExecutionContextInitializer(factory).initialize(context, source);

    VelocityContext child = context.get(VELOCITY_CONTEXT).orElseThrow();
    child.put("doc", "childDoc");
    child.put("childOnly", true);
    assertEquals("sourceDoc", sourceVelocityContext.get("doc"));
    assertNull(sourceVelocityContext.get("childOnly"));
    verify(factory);
  }

}
