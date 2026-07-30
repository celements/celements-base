package org.xwiki.context.internal;

import org.xwiki.context.ExecutionContext;

import junit.framework.TestCase;

public class DefaultExecutionTest extends TestCase {

  public void testPushAndPopContext() {
    DefaultExecution execution = new DefaultExecution();
    ExecutionContext parent = new ExecutionContext();
    ExecutionContext child = new ExecutionContext();

    assertNull(execution.getContext());
    execution.pushContext(parent);
    assertSame(parent, execution.getContext());
    execution.pushContext(child);
    assertSame(child, execution.getContext());
    execution.popContext();
    assertSame(parent, execution.getContext());
    execution.popContext();
    assertNull(execution.getContext());
  }

  public void testSetContextReplacesStack() {
    DefaultExecution execution = new DefaultExecution();
    execution.pushContext(new ExecutionContext());
    execution.pushContext(new ExecutionContext());
    ExecutionContext replacement = new ExecutionContext();

    execution.setContext(replacement);

    assertSame(replacement, execution.getContext());
    execution.popContext();
    assertNull(execution.getContext());
  }

  public void testPopWithoutContextFails() {
    DefaultExecution execution = new DefaultExecution();
    try {
      execution.popContext();
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
      assertEquals("no execution context to pop", expected.getMessage());
    }
  }

}
