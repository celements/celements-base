package com.celements.health;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;
import com.celements.health.HealthReport.CheckSnapshot;

public class HealthServiceTest {

  @Test
  public void test_check_noChecks() throws Exception {
    HealthReport report = await(service().check(Scope.MONITORING));
    assertTrue(report.checks().isEmpty());
    assertTrue(await(service().check(Scope.READINESS)).checks().isEmpty());
  }

  @Test
  public void test_check_recordsResultAndTiming() throws Exception {
    Instant before = Instant.now();
    HealthReport report = await(service(check("local", Scope.READINESS, Status.UP))
        .check(Scope.MONITORING));
    assertEquals(1, report.checks().size());
    CheckSnapshot snapshot = report.checks().get("local");
    assertEquals(new Result(Status.UP, ""), snapshot.result());
    assertEquals(Scope.READINESS, snapshot.scope());
    assertFalse(snapshot.checkedAt().isBefore(before));
    assertFalse(snapshot.duration().isNegative());
  }

  @Test
  public void test_check_startsAllChecksBeforeWaitingAndRunsPerRequest() throws Exception {
    CompletableFuture<Result> first = new CompletableFuture<>();
    CompletableFuture<Result> second = new CompletableFuture<>();
    AtomicInteger calls = new AtomicInteger();
    HealthService service = service(
        new Check("first", Scope.READINESS, () -> {
          calls.incrementAndGet();
          return first;
        }),
        new Check("second", Scope.MONITORING, () -> {
          calls.incrementAndGet();
          return second;
        }));
    CompletionStage<HealthReport> pending = service.check(Scope.MONITORING);
    assertEquals(2, calls.get());
    assertFalse(pending.toCompletableFuture().isDone());
    first.complete(new Result(Status.UP, ""));
    assertFalse(pending.toCompletableFuture().isDone());
    second.complete(new Result(Status.UP, ""));
    assertTrue(await(pending).isHealthy());
    assertTrue(await(service.check(Scope.MONITORING)).isHealthy());
    assertEquals(4, calls.get());
  }

  @Test
  public void test_check_readinessDoesNotInvokeMonitoringChecks() throws Exception {
    HealthReport report = await(service(
        check("local", Scope.READINESS, Status.UP),
        new Check("database", Scope.MONITORING, () -> {
          throw new AssertionError("Readiness must not start a database probe");
        })).check(Scope.READINESS));
    assertEquals(1, report.checks().size());
    assertTrue(report.checks().containsKey("local"));
  }

  @Test
  public void test_check_readinessMonitoringOnlySelectsNoChecks() throws Exception {
    HealthReport report = await(service(new Check("database", Scope.MONITORING, () -> {
      throw new AssertionError("Readiness must not start a database probe");
    })).check(Scope.READINESS));
    assertTrue(report.checks().isEmpty());
  }

  @Test
  public void test_check_failuresDoNotDiscardOtherResults() throws Exception {
    HealthReport report = await(service(
        check("healthy", Scope.READINESS, Status.UP),
        new Check("async", Scope.MONITORING, () -> CompletableFuture.failedFuture(
            new CompletionException(new IllegalStateException("private connection details")))),
        new Check("sync", Scope.MONITORING, () -> {
          throw new IllegalArgumentException("private configuration details");
        }),
        new Check("nullStage", Scope.MONITORING, () -> null),
        new Check("nullResult", Scope.MONITORING,
            () -> CompletableFuture.completedFuture(null))).check(Scope.MONITORING));
    assertEquals(5, report.checks().size());
    assertEquals(Status.UP, report.checks().get("healthy").result().status());
    assertEquals("Check failed: IllegalStateException", report.checks().get("async").result().reason());
    assertEquals("Check failed: IllegalArgumentException", report.checks().get("sync").result().reason());
    assertEquals(Status.DOWN, report.checks().get("nullStage").result().status());
    assertEquals("Check returned null", report.checks().get("nullResult").result().reason());
  }

  @Test
  public void test_check_deadlineBoundsAllChecksWithoutCompletingSharedWork() throws Exception {
    CompletableFuture<Result> first = new CompletableFuture<>();
    CompletableFuture<Result> second = new CompletableFuture<>();
    HealthReport report = await(service(
        new Check("first", Scope.READINESS, () -> first),
        new Check("second", Scope.MONITORING, () -> second)).check(Scope.MONITORING));
    assertEquals(2, report.checks().size());
    report.checks().values().forEach(snapshot -> {
      assertEquals(Status.DOWN, snapshot.result().status());
      assertEquals("Check timed out", snapshot.result().reason());
    });
    assertFalse(first.isDone());
    assertFalse(second.isDone());
  }

  @Test
  public void test_check_sharedFutureSurvivesOneRequestsTimeout() throws Exception {
    CompletableFuture<Result> shared = new CompletableFuture<>();
    HealthService service = service(new Check("shared", Scope.READINESS, () -> shared));
    assertEquals(Status.DOWN, await(service.check(Scope.MONITORING)).status());
    assertFalse(shared.isDone());
    CompletionStage<HealthReport> next = service.check(Scope.MONITORING);
    shared.complete(new Result(Status.UP, "Recovered"));
    assertEquals(Status.UP, await(next).status());
  }

  @Test
  public void test_constructor_duplicateIdsAndInvalidDeadline() {
    HealthCheck check = check("duplicate", Scope.READINESS, Status.UP);
    assertThrows(IllegalArgumentException.class, () -> service(check, check));
    assertThrows(IllegalArgumentException.class, () -> new HealthService(List.of(), Duration.ZERO));
  }

  @Test
  public void test_springLifecycle_noChecksNeedsNoConfigurationOrWorkers() throws Exception {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(HealthService.class);
      context.refresh();
      assertEquals(Status.DOWN,
          await(context.getBean(HealthService.class).check(Scope.MONITORING)).status());
    }
  }

  private static HealthService service(HealthCheck... checks) {
    return new HealthService(List.of(checks), Duration.ofMillis(250));
  }

  private static HealthCheck check(String id, Scope scope, Status status) {
    return new Check(id, scope, () -> CompletableFuture.completedFuture(new Result(status, "")));
  }

  private static HealthReport await(CompletionStage<HealthReport> stage) throws Exception {
    return stage.toCompletableFuture().get(3, TimeUnit.SECONDS);
  }

  private record Check(String id, Scope scope,
      Supplier<CompletionStage<Result>> action) implements HealthCheck {

    @Override
    public CompletionStage<Result> check() {
      return action.get();
    }
  }
}
