package com.celements.wiki.event;

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

import com.celements.init.update.WikiUpdater;
import com.celements.init.wiki.WikiCreationCoordinator;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiUser;

public class WikiCreationCoordinatorTest extends AbstractComponentTest {

  private ApplicationEventPublisher eventPublisher;
  private WikiUpdater wikiUpdater;
  private WikiCreationCoordinator coordinator;
  private ExecutionContext context;
  private XWikiContext xcontext;

  @Before
  public void prepareTest() throws Exception {
    eventPublisher = registerComponentMock(ApplicationEventPublisher.class);
    wikiUpdater = registerComponentMock(WikiUpdater.class);
    Execution execution = getComponentManager().lookup(Execution.class);
    context = execution.getContext();
    xcontext = getContext();
    coordinator = getBeanFactory().getBean(WikiCreationCoordinator.class);
  }

  @Test
  public void test_publishCreated() {
    WikiReference wikiRef = new WikiReference("mywiki");
    XWikiUser previousUser = new XWikiUser("xwiki:XWiki.Admin", true);
    String previousDatabase = "previousdb";
    context.set(XWIKI_USER, previousUser);
    xcontext.setDatabase(previousDatabase);
    WikiReference previousWiki = context.get(WIKI).orElseThrow();

    eventPublisher.publishEvent(isA(WikiCreatingEvent.class));
    expectLastCall().andAnswer(() -> {
      assertEquals(SUPERADMIN_FQN, context.get(XWIKI_USER).orElseThrow().getUser());
      assertTrue(context.get(XWIKI_USER).orElseThrow().isMain());
      assertEquals(wikiRef, context.get(WIKI).orElseThrow());
      assertEquals(wikiRef.getName(), xcontext.getDatabase());
      return null;
    }).once();
    expect(wikiUpdater.getFuture(wikiRef)).andReturn(Optional.empty()).once();
    eventPublisher.publishEvent(isA(WikiCreatedEvent.class));
    expectLastCall().andAnswer(() -> {
      assertSame(previousUser, context.get(XWIKI_USER).orElseThrow());
      assertEquals(wikiRef, context.get(WIKI).orElseThrow());
      assertEquals(wikiRef.getName(), xcontext.getDatabase());
      return null;
    }).once();

    replayDefault();
    coordinator.publishCreated(wikiRef);
    verifyDefault();
    assertSame(previousUser, context.get(XWIKI_USER).orElseThrow());
    assertSame(previousWiki, context.get(WIKI).orElseThrow());
    assertEquals(previousDatabase, xcontext.getDatabase());
  }

  @Test
  public void test_publishCreated_restoreAfterCreatingFailure() {
    WikiReference wikiRef = new WikiReference("mywiki");
    String previousDatabase = xcontext.getDatabase();

    eventPublisher.publishEvent(isA(WikiCreatingEvent.class));
    expectLastCall().andThrow(new IllegalStateException("fail")).once();

    replayDefault();
    assertThrows(IllegalStateException.class, () -> coordinator.publishCreated(wikiRef));
    verifyDefault();
    assertFalse(context.get(XWIKI_USER).isPresent());
    assertEquals(new WikiReference(previousDatabase), context.get(WIKI).orElseThrow());
    assertEquals(previousDatabase, xcontext.getDatabase());
  }

}
