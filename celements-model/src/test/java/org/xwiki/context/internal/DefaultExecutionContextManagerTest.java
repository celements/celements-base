package org.xwiki.context.internal;

import static com.celements.execution.XWikiExecutionProp.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.celements.common.test.AbstractComponentTest;

public final class DefaultExecutionContextManagerTest extends AbstractComponentTest {

  private DefaultExecutionContextManager contextManager;
  private Execution execution;

  @Before
  public void prepareTest() throws Exception {
    contextManager = getBeanFactory().getBean(DefaultExecutionContextManager.class);
    execution = getBeanFactory().getBean(Execution.class);
  }

  @Test
  public void test_cloneContext() throws Exception {
    ExecutionContext context = execution.getContext();
    ExecutionContext clonedContext = contextManager.clone(context);

    assertNotSame(context, clonedContext);
    assertTrue(clonedContext.get(XWIKI_CONTEXT).isPresent());
    assertSame(context, execution.getContext());
  }

  @Test
  public void test_cloneWithoutCurrentContext() throws Exception {
    ExecutionContext originalContext = execution.getContext();
    ExecutionContext context = new ExecutionContext();
    execution.removeContext();
    try {
      ExecutionContext clonedContext = contextManager.clone(context);

      assertNotNull(clonedContext);
      assertTrue(clonedContext.get(XWIKI_CONTEXT).isPresent());
      assertNull(execution.getContext());
    } finally {
      execution.setContext(originalContext);
    }
  }

}
