package com.celements.health;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;

public class AsyncHealthCheckTest {

  private static final Result UP = new Result(Status.UP, "Check succeeded");

  @Test
  public void test_check_runsOffRequestThreadAndSharesPendingStage() throws Exception {
    var requestThread = Thread.currentThread();
    var calls = new AtomicInteger();
    var acknowledgement = new CompletableFuture<Result>();
    var entered = new CountDownLatch(1);
    try (var check = new TestCheck(() -> {
      assertNotSame(requestThread, Thread.currentThread());
      assertEquals("health-check-test", Thread.currentThread().getName());
      calls.incrementAndGet();
      entered.countDown();
      return acknowledgement;
    })) {
      var pending = check.check().toCompletableFuture();
      assertTrue(entered.await(2, SECONDS));
      assertSame(pending, check.check().toCompletableFuture());
      assertFalse(pending.isDone());
      assertEquals(1, calls.get());
      acknowledgement.complete(UP);
      assertEquals(UP, pending.get(2, SECONDS));
    }
  }

  @Test
  public void test_check_concurrentRequestsStartOneExecution() throws Exception {
    var calls = new AtomicInteger();
    var acknowledgement = new CompletableFuture<Result>();
    var start = new CountDownLatch(1);
    var entered = new CountDownLatch(1);
    try (var check = new TestCheck(() -> {
      calls.incrementAndGet();
      entered.countDown();
      return acknowledgement;
    })) {
      var requests = IntStream.range(0, 16).mapToObj(ignored -> CompletableFuture.supplyAsync(() -> {
        try {
          assertTrue(start.await(2, SECONDS));
        } catch (InterruptedException failure) {
          Thread.currentThread().interrupt();
          throw new AssertionError(failure);
        }
        return check.check();
      })).toList();
      start.countDown();
      var pending = requests.get(0).get(2, SECONDS);
      for (var request : requests) {
        assertSame(pending, request.get(2, SECONDS));
      }
      assertTrue(entered.await(2, SECONDS));
      assertEquals(1, calls.get());
      acknowledgement.complete(UP);
      assertEquals(UP, pending.toCompletableFuture().get(2, SECONDS));
    }
  }

  @Test
  public void test_check_failuresAllowRetryAndCompletedResultsAreNotCached() throws Exception {
    var synchronousFailure = new IllegalStateException("Synchronous failure");
    var asynchronousFailure = new IllegalStateException("Asynchronous failure");
    var calls = new AtomicInteger();
    try (var check = new TestCheck(() -> {
      return switch (calls.getAndIncrement()) {
        case 0 -> throw synchronousFailure;
        case 1 -> CompletableFuture.failedFuture(asynchronousFailure);
        default -> CompletableFuture.completedFuture(UP);
      };
    })) {
      assertSame(synchronousFailure, assertThrows(CompletionException.class,
          () -> check.check().toCompletableFuture().join()).getCause());
      assertSame(asynchronousFailure, assertThrows(CompletionException.class,
          () -> check.check().toCompletableFuture().join()).getCause());
      assertEquals(UP, check.check().toCompletableFuture().get(2, SECONDS));
      assertEquals(UP, check.check().toCompletableFuture().get(2, SECONDS));
      assertEquals(4, calls.get());
    }
  }

  @Test
  public void test_check_timeoutDoesNotCancelSharedWork() throws Exception {
    var release = new CountDownLatch(1);
    var entered = new CountDownLatch(1);
    try (var check = new TestCheck(() -> {
      entered.countDown();
      try {
        assertTrue(release.await(5, SECONDS));
      } catch (InterruptedException failure) {
        Thread.currentThread().interrupt();
        return CompletableFuture.failedFuture(failure);
      }
      return CompletableFuture.completedFuture(UP);
    })) {
      var report = new HealthService(List.of(check), Duration.ofMillis(50))
          .check(Scope.READINESS).toCompletableFuture();
      assertTrue(entered.await(2, SECONDS));
      var pending = check.check().toCompletableFuture();
      assertFalse(report.get(2, SECONDS).ready());
      assertFalse(pending.isDone());
      assertSame(pending, check.check().toCompletableFuture());
      release.countDown();
      assertEquals(UP, pending.get(2, SECONDS));
    } finally {
      release.countDown();
    }
  }

  @Test
  public void test_close_cancelsPendingResultAndRejectsNewWork() throws Exception {
    var entered = new CountDownLatch(1);
    try (var check = new TestCheck(() -> {
      entered.countDown();
      return new CompletableFuture<>();
    })) {
      var pending = check.check().toCompletableFuture();
      assertTrue(entered.await(2, SECONDS));
      check.close();
      assertTrue(pending.isCancelled());
      assertTrue(assertThrows(CompletionException.class,
          () -> check.check().toCompletableFuture().join()).getCause()
              instanceof RejectedExecutionException);
    }
  }

  @Test
  public void test_springLifecycle_closesInheritedWorker() {
    TestCheck check;
    try (var context = new AnnotationConfigApplicationContext()) {
      context.registerBean(TestCheck.class,
          () -> new TestCheck(() -> CompletableFuture.completedFuture(UP)));
      context.refresh();
      check = context.getBean(TestCheck.class);
      assertEquals(UP, check.check().toCompletableFuture().join());
    }
    assertTrue(assertThrows(CompletionException.class,
        () -> check.check().toCompletableFuture().join()).getCause()
            instanceof RejectedExecutionException);
  }

  private static class TestCheck extends AsyncHealthCheck {

    private final Supplier<CompletionStage<Result>> check;

    TestCheck(Supplier<CompletionStage<Result>> check) {
      this.check = check;
    }

    @Override
    public String id() {
      return "test";
    }

    @Override
    public Scope scope() {
      return Scope.READINESS;
    }

    @Override
    protected CompletionStage<Result> runCheck() {
      return check.get();
    }
  }

}
