package com.xpn.xwiki.render;

import static com.celements.execution.XWikiExecutionProp.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.xwiki.velocity.VelocityExecutionProp.*;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.Optional;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.internal.XWikiExecutionContextInitializer;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.web.XWikiMessageTool;

public class DefaultVelocityManagerTest extends AbstractComponentTest {

  private Execution execution;
  private ExecutionContextManager contextManager;
  private DefaultVelocityManager velocityManager;

  @Before
  public void prepareTest() throws Exception {
    expect(getWikiMock().getCelDocument(anyObject(DocumentReference.class)))
        .andReturn(Optional.empty()).anyTimes();
    replayDefault();
    execution = getBeanFactory().getBean(Execution.class);
    VelocityExecutionContextInitializer velocityContextInitializer = getBeanFactory()
        .getBean(VelocityExecutionContextInitializer.class);
    contextManager = new DefaultExecutionContextManager(List.of(
        getBeanFactory().getBean(XWikiExecutionContextInitializer.class),
        velocityContextInitializer), execution);
    velocityManager = getBeanFactory().getBean(DefaultVelocityManager.class);
    execution.getContext().set(XWIKI, getWikiMock());
    execution.getContext().set(VELOCITY_CONTEXT, new VelocityContext());
  }

  @Test
  public void test_getVelocityContext_isolatesClonedContext() throws Exception {
    ExecutionContext context = execution.getContext();
    VelocityContext velocityContext = velocityManager.getVelocityContext();
    XWikiContext xcontext = context.get(XWIKI_CONTEXT).orElseThrow();
    XWikiMessageTool messageTool = new XWikiMessageTool(new TestResources(), xcontext);
    xcontext.put("msg", messageTool);
    velocityContext.put("msg", messageTool);
    ExecutionContext clonedContext = contextManager.clone(context);

    execution.pushContext(clonedContext);
    try {
      VelocityContext clonedVelocityContext = velocityManager.getVelocityContext();
      XWikiContext clonedXcontext = clonedContext.get(XWIKI_CONTEXT).orElseThrow();

      assertNotSame(velocityContext, clonedVelocityContext);
      assertSame(clonedVelocityContext, clonedXcontext.get("vcontext"));
      assertNotSame(velocityContext.get("util"), clonedVelocityContext.get("util"));
      assertNotSame(velocityContext.get("xwiki"), clonedVelocityContext.get("xwiki"));
      assertNotSame(velocityContext.get("xcontext"), clonedVelocityContext.get("xcontext"));
      assertNotSame(messageTool, clonedVelocityContext.get("msg"));
      assertSame(clonedVelocityContext.get("msg"), clonedXcontext.get("msg"));

      ((Context) clonedVelocityContext.get("xcontext")).setFinished(true);
      assertTrue(clonedXcontext.isFinished());
      assertFalse(xcontext.isFinished());
    } finally {
      execution.popContext();
    }
    verifyDefault();
  }

  private static class TestResources extends ListResourceBundle {

    @Override
    protected Object[][] getContents() {
      return new Object[0][0];
    }

  }

}
