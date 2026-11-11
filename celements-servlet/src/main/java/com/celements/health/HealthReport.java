package com.celements.health;

import static java.util.Objects.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.function.Predicate;

import javax.validation.constraints.NotNull;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;

public record HealthReport(@NotNull Map<@NotNull String, @NotNull CheckSnapshot> checks) {

  public HealthReport {
    checks = Map.copyOf(checks);
  }

  public boolean ready() {
    return !checks.isEmpty() && !anyCheckMatches(CheckSnapshot::blocksReadiness);
  }

  @NotNull
  public Status status() {
    if (checks.isEmpty()) {
      return Status.DOWN;
    } else if (anyCheckMatches(x -> x.result().status() == Status.DOWN)) {
      return Status.DOWN;
    } else if (allChecksMatch(x -> x.result().status() == Status.DISABLED)) {
      return Status.DISABLED;
    } else {
      return Status.UP;
    }
  }

  public boolean isHealthy() {
    return status().isHealthy();
  }

  private boolean anyCheckMatches(@NotNull Predicate<CheckSnapshot> predicate) {
    return checks.values().stream().anyMatch(predicate);
  }

  private boolean allChecksMatch(@NotNull Predicate<CheckSnapshot> predicate) {
    return checks.values().stream().allMatch(predicate);
  }

  /**
   * One request's check result and timing. {@code checkedAt} is when the request observed the
   * answer, and {@code duration} is how long obtaining it took. Cached checks own data freshness.
   */
  public record CheckSnapshot(
      @NotNull Result result,
      @NotNull Scope scope,
      @NotNull Instant checkedAt,
      @NotNull Duration duration) {

    public CheckSnapshot {
      requireNonNull(result);
      requireNonNull(scope);
      requireNonNull(checkedAt);
      requireNonNull(duration);
    }

    public boolean isHealthy() {
      return result.status().isHealthy();
    }

    public boolean blocksReadiness() {
      return (scope == Scope.READINESS) && !isHealthy();
    }

    public double durationSeconds() {
      return duration.getSeconds() + (duration.getNano() / 1_000_000_000.0);
    }
  }
}
