package com.celements.wiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
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
  public void test_getWikiMap() throws Exception {
    expectWikiQuery(ImmutableList.of(
        new String[] { "XWikiServerAtest", "a1.host" },
        new String[] { "XWikiServerBtest", "b1.host" },
        new String[] { "XWikiServerAtest", "a2.host" }),
        1);
    replayDefault();
    assertEquals(ImmutableSetMultimap.of(
        new WikiReference("atest"), new URL("http://a1.host"),
        new WikiReference("btest"), new URL("http://b1.host"),
        new WikiReference("atest"), new URL("http://a2.host")),
        service.getWikiMap());
    verifyDefault();
  }

  @Test
  public void test_getWikiMap_cached() throws Exception {
    expectWikiQuery(ImmutableList.of(), 1);
    replayDefault();
    for (int i = 0; i < 5; i++) {
      assertEquals(ImmutableMultimap.of(), service.getWikiMap());
    }
    verifyDefault();
  }

  @Test
  public void test_getWikiMap_QueryException() throws Exception {
    expect(getMock(QueryManager.class).getNamedQuery("getAllWikis"))
        .andThrow(new QueryException("", null, null)).times(2);
    replayDefault();
    for (int i = 0; i < 2; i++) {
      assertEquals(ImmutableMultimap.of(), service.getWikiMap());
    }
    verifyDefault();
  }

  @Test
  public void test_refresh() throws Exception {
    expectWikiQuery(ImmutableList.of(), 3);
    replayDefault();
    for (int i = 0; i < 3; i++) {
      assertEquals(ImmutableMultimap.of(), service.getWikiMap());
      service.refresh();
    }
    verifyDefault();
  }

  private void expectWikiQuery(List<String[]> result, int times) throws QueryException {
    Query queryMock = createDefaultMock(Query.class);
    expect(getMock(QueryManager.class).getNamedQuery("getAllWikis"))
        .andReturn(queryMock).times(times);
    expect(queryMock.setWiki(XWikiConstant.MAIN_WIKI.getName())).andReturn(queryMock).times(times);
    expect(queryMock.<String[]>execute()).andReturn(result).times(times);
  }

}
