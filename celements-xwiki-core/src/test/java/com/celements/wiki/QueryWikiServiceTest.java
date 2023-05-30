package com.celements.wiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.test.AbstractComponentTest;

public class QueryWikiServiceTest extends AbstractComponentTest {

  QueryWikiService service;

  @Before
  public void setUp() throws Exception {
    registerComponentMock(QueryManager.class);
    service = getSpringContext().getBean(QueryWikiService.class);
    getContext().setDatabase("test");
  }

  @Test
  public void test_getWikisByHost() throws Exception {
    expectWikiQuery(ImmutableList.of(
        new String[] { "a1.host", "XWikiServerAtest" },
        new String[] { "b1.host", "XWikiServerBtest" },
        new String[] { "a2.host", "XWikiServerAtest" }),
        1);
    replayDefault();
    assertEquals(ImmutableMap.of(
        "a1.host", new WikiReference("atest"),
        "b1.host", new WikiReference("btest"),
        "a2.host", new WikiReference("atest")),
        service.getWikisByHost());
    verifyDefault();
  }

  @Test
  public void test_getWikisByHost_cached() throws Exception {
    expectWikiQuery(ImmutableList.of(), 1);
    replayDefault();
    for (int i = 0; i < 5; i++) {
      assertEquals(ImmutableMap.of(), service.getWikisByHost());
    }
    verifyDefault();
  }

  @Test
  public void test_getWikisByHost_QueryException() throws Exception {
    expect(getMock(QueryManager.class).getNamedQuery("getWikisByHost"))
        .andThrow(new QueryException("", null, null)).times(2);
    replayDefault();
    for (int i = 0; i < 2; i++) {
      assertEquals(ImmutableMap.of(), service.getWikisByHost());
    }
    verifyDefault();
  }

  @Test
  public void test_refresh() throws Exception {
    expectWikiQuery(ImmutableList.of(), 3);
    replayDefault();
    for (int i = 0; i < 3; i++) {
      assertEquals(ImmutableMap.of(), service.getWikisByHost());
      service.refresh();
    }
    verifyDefault();
  }

  private void expectWikiQuery(List<String[]> result, int times) throws QueryException {
    Query queryMock = createDefaultMock(Query.class);
    expect(getMock(QueryManager.class).getNamedQuery("getWikisByHost"))
        .andReturn(queryMock).times(times);
    expect(queryMock.setWiki(XWikiConstant.MAIN_WIKI.getName())).andReturn(queryMock).times(times);
    expect(queryMock.<String[]>execute()).andReturn(result).times(times);
  }

}
