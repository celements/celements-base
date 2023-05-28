package com.celements.model.reference;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;

import com.celements.common.test.AbstractComponentTest;
import com.celements.wiki.WikiProvider;
import com.google.common.collect.ImmutableList;

public class ReferenceProviderTest extends AbstractComponentTest {

  ReferenceProvider provider;

  @Before
  public void setUp() throws Exception {
    registerComponentMocks(QueryManager.class, WikiProvider.class);
    provider = getSpringContext().getBean(ReferenceProvider.class);
    getContext().setDatabase("test");
  }

  @Test
  public void test_getAllWikis() throws Exception {
    List<WikiReference> wikis = ImmutableList.of(
        new WikiReference("atest"),
        new WikiReference("btest"));
    expect(getMock(WikiProvider.class).getAllWikis()).andReturn(wikis);
    replayDefault();
    assertEquals(wikis, provider.getAllWikis());
    verifyDefault();
  }

}
