package com.celements.init.wiki;

import static com.celements.execution.XWikiExecutionProp.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.event.WikiCreatingEvent;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractComponentTest;

public class WikiMandatoryClassesListenerTest extends AbstractComponentTest {

  private WikiMandatoryClassesListener listener;

  @Before
  public void prepareTest() throws Exception {
    Execution execution = getComponentManager().lookup(Execution.class);
    execution.getContext().set(XWIKI, getWikiMock());
    listener = new WikiMandatoryClassesListener(execution);
  }

  @Test
  public void test_getOrder() {
    assertEquals(-1000, listener.getOrder());
  }

  @Test
  public void test_onApplicationEvent() throws Exception {
    String database = "mywiki";

    getWikiMock().initializeMandatoryClasses(isNull(XWikiContext.class));
    expectLastCall().once();

    replayDefault();
    listener.onApplicationEvent(new WikiCreatingEvent(new WikiReference(database)));
    verifyDefault();
  }

}
