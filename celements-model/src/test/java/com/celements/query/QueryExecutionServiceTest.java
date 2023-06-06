package com.celements.query;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.Utils;

public class QueryExecutionServiceTest extends AbstractComponentTest {

  private QueryExecutionService queryExecService;
  private XWikiHibernateStore storeMock;

  @Before
  public void setUp_CelementsWebScriptServiceTest() throws Exception {
    storeMock = registerComponentMock(XWikiHibernateStore.class);
    queryExecService = (QueryExecutionService) Utils.getComponent(IQueryExecutionServiceRole.class);
    expect(getMock(XWiki.class).getHibernateStore()).andReturn(storeMock).anyTimes();
  }

  @Test
  public void testExecuteWriteHQL() throws Exception {
    String hql = "someHQL";
    WikiReference wikiRef = new WikiReference(getXContext().getDatabase());
    Map<String, Object> binds = new HashMap<>();
    binds.put("key", "someVal");
    int ret = 5;
    Capture<HibernateCallback<Integer>> hibCallbackCapture = newCapture();

    expect(storeMock.executeWrite(eq(wikiRef), eq(true), capture(hibCallbackCapture)))
        .andReturn(ret).once();

    assertEquals("xwikidb", getXContext().getDatabase());
    replayDefault();
    assertEquals(ret, queryExecService.executeWriteHQL(hql, binds));
    verifyDefault();
    assertEquals("xwikidb", getXContext().getDatabase());
    ExecuteWriteCallback callback = (ExecuteWriteCallback) hibCallbackCapture.getValue();
    assertEquals(hql, callback.getHQL());
    assertNotSame(binds, callback.getBinds());
    assertEquals(binds, callback.getBinds());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExecuteWriteHQL_otherWiki() throws Exception {
    String hql = "someHQL";
    Map<String, Object> binds = new HashMap<>();
    binds.put("key", "someVal");
    WikiReference wikiRef = new WikiReference("otherdb");
    int ret = 5;

    expect(storeMock.executeWrite(eq(wikiRef), eq(true), anyObject(HibernateCallback.class)))
        .andReturn(ret).once();

    assertEquals("xwikidb", getXContext().getDatabase());
    replayDefault();
    assertEquals(ret, queryExecService.executeWriteHQL(hql, binds, wikiRef));
    verifyDefault();
    assertEquals("xwikidb", getXContext().getDatabase());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExecuteWriteHQL_XWE() throws Exception {
    String hql = "someHQL";
    Map<String, Object> binds = new HashMap<>();
    WikiReference wikiRef = new WikiReference("otherdb");
    Throwable cause = new XWikiException();

    expect(storeMock.executeWrite(eq(wikiRef), eq(true), anyObject(HibernateCallback.class)))
        .andThrow(cause).once();

    assertEquals("xwikidb", getXContext().getDatabase());
    replayDefault();
    try {
      queryExecService.executeWriteHQL(hql, binds, wikiRef);
    } catch (XWikiException xwe) {
      assertSame(cause, xwe);
    }
    verifyDefault();
    assertEquals("xwikidb", getXContext().getDatabase());
  }

}
