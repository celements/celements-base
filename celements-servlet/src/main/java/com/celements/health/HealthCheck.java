package com.celements.health;

import static java.util.Objects.*;

import java.util.concurrent.CompletionStage;

import javax.validation.constraints.NotNull;

/**
 * One health check, discovered as a Spring bean and requested by {@link HealthService}.
 *
 * <p>Checks run on each health request. Readiness requests invoke checks required for routing;
 * status and metrics requests also invoke monitoring checks.
 *
 * <p>{@link #check()} must return promptly. Local checks can return a completed future; blocking
 * I/O must run asynchronously with native timeouts. Implementations own any detached work,
 * caching, data freshness, and reuse of ongoing checks. Returned stages may be shared across
 * requests: service deadlines affect only dependent stages, leaving shared work untouched.
 * {@link AsyncHealthCheck} provides a worker and reuse of one in-flight execution for blocking checks.
 *
 * <p>Disabled checks do not block readiness. Keep identifiers stable and diagnostic reasons
 * human-readable: only check IDs and fixed status/scope values should become metric labels;
 * reasons belong in diagnostic JSON.
 */
public interface HealthCheck {

  /** Stable, unique identifier, such as {@code kafka.consumer}. */
  @NotNull
  String id();

  /** Stable for this check's lifetime; also applies when execution fails or times out. */
  @NotNull
  Scope scope();

  /**
   * Returns promptly with a stage providing the check's status and reason. Execution failures
   * must be represented by exceptional completion of the stage, rather than thrown directly.
   * The stage may already be completed when returned.
   */
  @NotNull
  CompletionStage<@NotNull Result> check();

  /** Status and reason for one health check, without execution metadata. */
  record Result(@NotNull Status status, @NotNull String reason) {

    public Result {
      requireNonNull(status);
      requireNonNull(reason);
    }
  }

  /** Ordered by inclusion: monitoring reports also include readiness checks. */
  enum Scope {
    READINESS, MONITORING;

    /** Whether a report with this scope includes checks with the given scope. */
    public boolean includes(@NotNull Scope other) {
      return compareTo(other) >= 0;
    }
  }

  enum Status {
    UP, DOWN, DISABLED;

    public boolean isHealthy() {
      return (this == UP) || (this == DISABLED);
    }
  }

}
