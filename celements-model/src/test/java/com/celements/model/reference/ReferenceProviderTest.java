package com.celements.model.reference;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.reference.ReferenceProvider;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiConstant;

public class ReferenceProviderTest extends AbstractComponentTest {

  ReferenceProvider provider;

  @Before
  public void setUp() throws Exception {
    registerComponentMock(QueryManager.class);
    provider = getSpringContext().getBean(ReferenceProvider.class);
    getContext().setDatabase("test");
  }

  @Test
  public void test_getAllWikis() throws Exception {
    expectWikiQuery(ImmutableList.of("XWikiServerAtest", "XWikiServerBtest"), 1);
    replayDefault();
    assertEquals(ImmutableList.of(
        XWikiConstant.MAIN_WIKI, new WikiReference("atest"), new WikiReference("btest")),
        ImmutableList.copyOf(provider.getAllWikis()));
    verifyDefault();
  }

  @Test
  public void test_getAllWikis_cached() throws Exception {
    expectWikiQuery(ImmutableList.of(), 1);
    replayDefault();
    for (int i = 0; i < 5; i++) {
      assertEquals(
          ImmutableList.of(XWikiConstant.MAIN_WIKI),
          ImmutableList.copyOf(provider.getAllWikis()));
    }
    verifyDefault();
  }

  @Test
  public void test_getAllWikis_QueryException() throws Exception {
    expect(getMock(QueryManager.class).createQuery(ReferenceProvider.XWQL_WIKI, Query.XWQL))
        .andThrow(new QueryException("", null, null)).times(2);
    replayDefault();
    for (int i = 0; i < 2; i++) {
      assertEquals(
          ImmutableList.of(XWikiConstant.MAIN_WIKI),
          ImmutableList.copyOf(provider.getAllWikis()));
    }
    verifyDefault();
  }

  @Test
  public void test_refresh() throws Exception {
    expectWikiQuery(ImmutableList.of(), 3);
    replayDefault();
    for (int i = 0; i < 3; i++) {
      assertEquals(
          ImmutableList.of(XWikiConstant.MAIN_WIKI),
          ImmutableList.copyOf(provider.getAllWikis()));
      provider.refresh();
    }
    verifyDefault();
  }

  private void expectWikiQuery(List<String> result, int times) throws QueryException {
    Query queryMock = createDefaultMock(Query.class);
    expect(getMock(QueryManager.class).createQuery(ReferenceProvider.XWQL_WIKI, Query.XWQL))
        .andReturn(queryMock).times(times);
    expect(queryMock.setWiki(XWikiConstant.MAIN_WIKI.getName())).andReturn(queryMock).times(times);
    expect(queryMock.<String>execute()).andReturn(result).times(times);
  }

}
