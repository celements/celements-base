package com.celements.query;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.LogicalOperator;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.Utils;

public class QueryExecutionServiceTest extends AbstractComponentTest {

  private QueryExecutionService queryExecService;

  private XWikiHibernateStore storeMock;

  @Before
  public void setUp_CelementsWebScriptServiceTest() throws Exception {
    queryExecService = (QueryExecutionService) Utils.getComponent(IQueryExecutionServiceRole.class);
    storeMock = createDefaultMock(XWikiHibernateStore.class);
    expect(getWikiMock().getHibernateStore()).andReturn(storeMock).anyTimes();
  }

  @Test
  public void testExecuteWriteHQL() throws Exception {
    String hql = "someHQL";
    Map<String, Object> binds = new HashMap<>();
    binds.put("key", "someVal");
    int ret = 5;
    Capture<HibernateCallback<Integer>> hibCallbackCapture = newCapture();

    expect(storeMock.executeWrite(same(getContext()), eq(true), capture(
        hibCallbackCapture))).andReturn(ret).once();

    assertEquals("xwikidb", getContext().getDatabase());
    replayDefault();
    assertEquals(ret, queryExecService.executeWriteHQL(hql, binds));
    verifyDefault();
    assertEquals("xwikidb", getContext().getDatabase());
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

    expect(storeMock.executeWrite(cmp(null, (ctx, exp) -> wikiRef.getName()
        .compareTo((String) ctx.get("wiki")), LogicalOperator.EQUAL),
        eq(true), anyObject(HibernateCallback.class))).andReturn(ret).once();

    assertEquals("xwikidb", getContext().getDatabase());
    replayDefault();
    assertEquals(ret, queryExecService.executeWriteHQL(hql, binds, wikiRef));
    verifyDefault();
    assertEquals("xwikidb", getContext().getDatabase());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testExecuteWriteHQL_XWE() throws Exception {
    String hql = "someHQL";
    Map<String, Object> binds = new HashMap<>();
    WikiReference wikiRef = new WikiReference("otherdb");
    Throwable cause = new XWikiException();

    expect(storeMock.executeWrite(same(getContext()), eq(true), anyObject(
        HibernateCallback.class))).andThrow(cause).once();

    assertEquals("xwikidb", getContext().getDatabase());
    replayDefault();
    try {
      queryExecService.executeWriteHQL(hql, binds, wikiRef);
    } catch (XWikiException xwe) {
      assertSame(cause, xwe);
    }
    verifyDefault();
    assertEquals("xwikidb", getContext().getDatabase());
  }

}
