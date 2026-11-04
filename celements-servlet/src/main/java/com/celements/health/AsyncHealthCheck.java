package com.celements.health;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.validation.constraints.NotNull;

/**
 * Runs a check on a dedicated worker and shares one in-flight result across requests.
 * Completed results are not cached. HealthService deadlines affect request-owned copies only;
 * implementations must still bound their underlying I/O using native timeouts.
 */
public abstract class AsyncHealthCheck implements HealthCheck, AutoCloseable {

  private final AtomicReference<CompletableFuture<Result>> pending = new AtomicReference<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor(task -> {
    var worker = new Thread(task, "health-check-" + id());
    worker.setDaemon(true);
    return worker;
  });

  @Override
  @NotNull
  public final CompletionStage<@NotNull Result> check() {
    while (true) {
      var current = pending.get();
      if ((current != null) && !current.isDone()) {
        return current;
      }
      var result = new CompletableFuture<Result>();
      if (pending.compareAndSet(current, result)) {
        startCheck(result);
        return result;
      }
    }
  }

  private void startCheck(@NotNull CompletableFuture<@NotNull Result> result) {
    CompletableFuture.completedFuture(null)
        .thenComposeAsync(ignored -> runCheck(), executor)
        .whenComplete((value, failure) -> {
          if (failure == null) {
            result.complete(value);
          } else {
            result.completeExceptionally(failure);
          }
        });
  }

  /** Invoked on this check's worker; may block or return a stage awaiting asynchronous work. */
  @NotNull
  protected abstract CompletionStage<@NotNull Result> runCheck();

  @Override
  @PreDestroy
  public final void close() {
    executor.shutdownNow();
    var pending = this.pending.get();
    if (pending != null) {
      pending.cancel(false);
    }
  }

}
