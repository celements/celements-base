package com.celements.store;

import static java.util.concurrent.TimeUnit.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.hibernate.HibernateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;
import com.celements.health.HealthService;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;

public class DatabaseHealthCheckTest extends AbstractComponentTest {

  private DatabaseHealthCheck check;
  private XWikiHibernateStore store;

  @Before
  public void prepareTest() throws Exception {
    store = createDefaultMock(XWikiHibernateStore.class);
    registerComponentMock(XWikiStoreInterface.class, "hibernate", store);
    check = getBeanFactory().getBean(DatabaseHealthCheck.class);
  }

  @After
  public void close() {
    check.close();
  }

  @Test
  public void test_check_successIsReady() throws Exception {
    store.checkConnection(Duration.ofSeconds(1));

    replayDefault();
    assertEquals("database", check.id());
    assertEquals(Scope.READINESS, check.scope());
    var report = new HealthService(List.of(check)).check(Scope.READINESS)
        .toCompletableFuture().get(2, SECONDS);
    assertTrue(report.ready());
    assertEquals(new Result(Status.UP, "Database check succeeded"),
        report.checks().get(check.id()).result());
    verifyDefault();
  }

  @Test
  public void test_check_failureIsDownWithoutLeakingDetails() throws Exception {
    store.checkConnection(Duration.ofSeconds(1));
    expectLastCall().andThrow(new HibernateException("jdbc:mysql://private-host secret"));

    replayDefault();
    assertEquals(new Result(Status.DOWN, "Database check failed: HibernateException"), result());
    verifyDefault();
  }

  @Test
  public void test_check_failedCheckCanBeRetried() throws Exception {
    store.checkConnection(Duration.ofSeconds(1));
    expectLastCall().andThrow(new HibernateException("Unavailable"));
    store.checkConnection(Duration.ofSeconds(1));

    replayDefault();
    assertEquals(Status.DOWN, result().status());
    assertEquals(Status.UP, result().status());
    verifyDefault();
  }

  @Test
  public void test_check_blockedCheckIsSharedAndReadinessTimesOut() throws Exception {
    var entered = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    var requestThread = Thread.currentThread();
    store.checkConnection(Duration.ofSeconds(1));
    expectLastCall().andAnswer(() -> {
      assertNotSame(requestThread, Thread.currentThread());
      entered.countDown();
      assertTrue(release.await(5, SECONDS));
      throw new HibernateException("Pool exhausted");
    });

    replayDefault();
    try {
      var report = new HealthService(List.of(check)).check(Scope.READINESS).toCompletableFuture();
      assertTrue(entered.await(2, SECONDS));
      var pending = check.check().toCompletableFuture();
      assertSame(pending, check.check().toCompletableFuture());
      var timedOut = report.get(3, SECONDS);
      assertFalse(timedOut.ready());
      assertEquals(new Result(Status.DOWN, "Check timed out"),
          timedOut.checks().get(check.id()).result());
      assertFalse(pending.isDone());
      release.countDown();
      assertEquals(Status.DOWN, pending.get(2, SECONDS).status());
    } finally {
      release.countDown();
    }
    verifyDefault();
  }

  private Result result() throws Exception {
    return check.check().toCompletableFuture().get(2, SECONDS);
  }

}
