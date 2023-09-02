package com.celements.model.reference;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;

import com.celements.common.test.AbstractComponentTest;
import com.celements.wiki.WikiService;
import com.google.common.collect.ImmutableSet;

public class ReferenceProviderTest extends AbstractComponentTest {

  ReferenceProvider provider;

  @Before
  public void setUp() throws Exception {
    registerComponentMocks(QueryManager.class, WikiService.class);
    provider = getSpringContext().getBean(ReferenceProvider.class);
    getContext().setDatabase("test");
  }

  @Test
  public void test_getAllWikis() throws Exception {
    ImmutableSet<WikiReference> wikis = ImmutableSet.of(
        new WikiReference("atest"),
        new WikiReference("btest"));
    expect(getMock(WikiService.class).streamAllWikis()).andAnswer(wikis::stream);
    replayDefault();
    assertEquals(wikis, provider.getAllWikis());
    verifyDefault();
  }

}
