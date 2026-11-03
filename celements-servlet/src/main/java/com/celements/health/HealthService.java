package com.celements.health;

import static com.google.common.base.Preconditions.*;
import static java.util.Objects.*;
import static java.util.concurrent.TimeUnit.*;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Service;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;
import com.celements.health.HealthReport.CheckSnapshot;

import one.util.streamex.EntryStream;

/**
 * Requests checks in parallel and assembles reports within one response deadline.
 *
 * <p>Starts the selected {@link HealthCheck} stages without waiting between them and attaches
 * per-request observation times and durations in {@link CheckSnapshot}. The observations form
 * one {@link HealthReport} shared by readiness, JSON status, and metrics consumers. This service
 * neither schedules checks nor caches results.
 *
 * <p>The deadline applies to dependent copies of the returned stages. Timeouts and failures become
 * {@link Status#DOWN}. Disabled checks are healthy and do not block readiness.
 */
@Service
public class HealthService {

  private final Map<String, HealthCheck> checks;
  private final Duration timeout;

  @Inject
  public HealthService(@NotNull List<@NotNull HealthCheck> checks) {
    this(checks, Duration.ofSeconds(2));
  }

  HealthService(@NotNull List<@NotNull HealthCheck> checks, @NotNull Duration timeout) {
    this.checks = validateChecks(checks);
    this.timeout = requireNonNull(timeout);
    checkArgument(timeout.compareTo(Duration.ZERO) > 0, "Response timeout must be positive");
  }

  @NotNull
  private Map<String, HealthCheck> validateChecks(@NotNull List<@NotNull HealthCheck> checks) {
    var validated = new LinkedHashMap<String, HealthCheck>();
    for (var check : checks) {
      var id = requireNonNull(check.id());
      checkArgument(!id.isBlank(), "Health check id must not be blank");
      requireNonNull(check.scope());
      checkArgument(validated.putIfAbsent(id, check) == null, "Duplicate health check id [%s]", id);
    }
    return Map.copyOf(validated);
  }

  /** Runs checks included in the scope; READINESS excludes monitoring-only dependency probes. */
  @NotNull
  public CompletionStage<@NotNull HealthReport> check(@NotNull Scope scope) {
    requireNonNull(scope);
    var startedAt = System.nanoTime();
    var pending = EntryStream.of(checks)
        .filterValues(check -> scope.includes(check.scope()))
        .mapValues(check -> startCheck(check, startedAt))
        .toImmutableMap();
    return awaitAllChecks(pending).thenApply(ignored -> assembleReport(pending));
  }

  @NotNull
  private CompletableFuture<CheckSnapshot> startCheck(@NotNull HealthCheck check, long reportStartedAt) {
    var startedAt = System.nanoTime();
    var result = invokeCheck(check);
    var remaining = timeout.minus(elapsedSince(reportStartedAt));
    return result
        .orTimeout(Math.max(0L, remaining.toNanos()), NANOSECONDS)
        .handle((value, failure) -> new CheckSnapshot(
            toResult(value, failure),
            check.scope(),
            Instant.now(),
            elapsedSince(startedAt)));
  }

  @NotNull
  private CompletableFuture<Result> invokeCheck(@NotNull HealthCheck check) {
    try {
      // The returned stage may represent work shared with other requests.
      return requireNonNull(check.check()).toCompletableFuture().copy();
    } catch (Exception exc) {
      return CompletableFuture.failedFuture(exc);
    }
  }

  @NotNull
  private Result toResult(@Nullable Result value, @Nullable Throwable failure) {
    while ((failure instanceof CompletionException) && (failure.getCause() != null)) {
      failure = failure.getCause();
    }
    if (failure instanceof TimeoutException) {
      return new Result(Status.DOWN, "Check timed out");
    } else if (failure != null) {
      return new Result(Status.DOWN, "Check failed: " + failure.getClass().getSimpleName());
    } else if (value == null) {
      return new Result(Status.DOWN, "Check returned null");
    } else {
      return value;
    }
  }

  @NotNull
  private CompletableFuture<Void> awaitAllChecks(@NotNull Map<String, CompletableFuture<CheckSnapshot>> pending) {
    return CompletableFuture.allOf(pending.values().toArray(CompletableFuture<?>[]::new));
  }

  @NotNull
  private HealthReport assembleReport(@NotNull Map<String, CompletableFuture<CheckSnapshot>> pending) {
    var snapshots = EntryStream.of(pending)
        .mapValues(CompletableFuture::join)
        .toImmutableMap();
    return new HealthReport(snapshots);
  }

  @NotNull
  private Duration elapsedSince(long startedAt) {
    return Duration.ofNanos(System.nanoTime() - startedAt);
  }
}
