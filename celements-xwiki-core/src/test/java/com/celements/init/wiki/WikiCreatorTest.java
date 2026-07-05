package com.celements.init.wiki;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.user.api.XWikiRightService.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.XWikiProvider;
import com.celements.init.update.WikiUpdater;
import com.celements.wiki.event.WikiCreatedEvent;
import com.celements.wiki.event.WikiCreatingEvent;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiUser;

public class WikiCreatorTest extends AbstractComponentTest {

  private XWikiProvider xwikiProvider;
  private XWikiStoreInterface store;
  private WikiUpdater wikiUpdater;
  private ApplicationEventPublisher eventPublisher;
  private ExecutionContext ectx;
  private XWikiContext xcontext;
  private WikiCreator wikiCreator;

  @Before
  public void prepareTest() throws Exception {
    store = registerComponentMock(XWikiStoreInterface.class, "hibernate");
    xwikiProvider = registerComponentMock(XWikiProvider.class);
    wikiUpdater = registerComponentMock(WikiUpdater.class);
    eventPublisher = registerComponentMock(ApplicationEventPublisher.class);
    ectx = getComponentManager().lookup(Execution.class).getContext();
    xcontext = getContext();
    wikiCreator = getBeanFactory().getBean(WikiCreator.class);
  }

  @Test
  public void test_createWiki() throws Exception {
    WikiReference wikiRef = new WikiReference("newwiki");
    XWikiUser previousUser = new XWikiUser("xwiki:XWiki.Admin", true);
    xcontext.setDatabase("previousdb");
    ectx.set(XWIKI_USER, previousUser);
    WikiReference previousWiki = ectx.get(WIKI).orElseThrow();

    expectStoreFromXWiki(2);
    expect(store.existsWiki(wikiRef)).andReturn(false);
    store.createWiki(wikiRef);
    expect(store.isWikiEmpty(wikiRef)).andReturn(true);
    store.initWiki(wikiRef);
    expectWikiCreationEvents(wikiRef, previousUser);

    replayDefault();
    wikiCreator.createWiki(wikiRef);
    verifyDefault();
    assertSame(previousUser, ectx.get(XWIKI_USER).orElseThrow());
    assertSame(previousWiki, ectx.get(WIKI).orElseThrow());
    assertEquals("previousdb", xcontext.getDatabase());
  }

  @Test
  public void test_createWiki_notEmpty() throws Exception {
    WikiReference wikiRef = new WikiReference("existingwiki");

    expectStoreFromXWiki(2);
    expect(store.existsWiki(wikiRef)).andReturn(true);
    expect(store.isWikiEmpty(wikiRef)).andReturn(false);

    replayDefault();
    assertThrows(WikiCreator.WikiCreationException.class, () -> wikiCreator.createWiki(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_ensureWiki() throws Exception {
    WikiReference wikiRef = new WikiReference("existingwiki");

    expectStoreFromXWiki(2);
    expect(store.existsWiki(wikiRef)).andReturn(true);
    expect(store.isWikiEmpty(wikiRef)).andReturn(false);

    replayDefault();
    assertFalse(wikiCreator.ensureWiki(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_ensureWikiDeferred() throws Exception {
    WikiReference wikiRef = new WikiReference("mainwiki");
    XWikiUser previousUser = new XWikiUser("xwiki:XWiki.Admin", true);
    xcontext.setDatabase("previousdb");
    ectx.set(XWIKI_USER, previousUser);
    WikiReference previousWiki = ectx.get(WIKI).orElseThrow();

    expectFallbackStore(2);
    expect(store.existsWiki(wikiRef)).andReturn(true);
    expect(store.isWikiEmpty(wikiRef)).andReturn(true);
    store.initWiki(wikiRef);
    expectWikiCreationEvents(wikiRef, previousUser);

    replayDefault();
    Optional<Runnable> postAction = wikiCreator.ensureWikiDeferred(wikiRef);
    assertTrue(postAction.isPresent());
    assertSame(previousUser, ectx.get(XWIKI_USER).orElseThrow());
    assertSame(previousWiki, ectx.get(WIKI).orElseThrow());

    postAction.orElseThrow().run();
    verifyDefault();
    assertSame(previousUser, ectx.get(XWIKI_USER).orElseThrow());
    assertSame(previousWiki, ectx.get(WIKI).orElseThrow());
    assertEquals("previousdb", xcontext.getDatabase());
  }

  private void expectStoreFromXWiki(int times) {
    expect(xwikiProvider.get()).andReturn(Optional.of(getWikiMock())).times(times);
    expect(getWikiMock().getStore()).andReturn(store).times(times);
  }

  private void expectFallbackStore(int times) {
    expect(xwikiProvider.get()).andReturn(Optional.empty()).times(times);
  }

  private void expectWikiCreationEvents(WikiReference wikiRef, XWikiUser previousUser) {
    eventPublisher.publishEvent(isA(WikiCreatingEvent.class));
    expectLastCall().andAnswer(() -> {
      WikiCreatingEvent event = (WikiCreatingEvent) getCurrentArguments()[0];
      assertEquals(wikiRef, event.getWiki());
      assertEquals(SUPERADMIN_FQN, ectx.get(XWIKI_USER).orElseThrow().getUser());
      assertTrue(ectx.get(XWIKI_USER).orElseThrow().isMain());
      assertEquals(wikiRef, ectx.get(WIKI).orElseThrow());
      assertEquals(wikiRef.getName(), xcontext.getDatabase());
      return null;
    }).once();
    expect(wikiUpdater.getFuture(wikiRef)).andReturn(Optional.empty()).once();
    eventPublisher.publishEvent(isA(WikiCreatedEvent.class));
    expectLastCall().andAnswer(() -> {
      WikiCreatedEvent event = (WikiCreatedEvent) getCurrentArguments()[0];
      assertEquals(wikiRef, event.getWiki());
      assertSame(previousUser, ectx.get(XWIKI_USER).orElseThrow());
      assertEquals(wikiRef, ectx.get(WIKI).orElseThrow());
      assertEquals(wikiRef.getName(), xcontext.getDatabase());
      return null;
    }).once();
  }

}
