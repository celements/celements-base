package com.celements.observation.remote.kafka;

import static java.util.concurrent.CompletableFuture.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.celements.health.HealthCheck;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Probes the existing producer by awaiting acknowledgement of a transport-only record.
 * Concurrent requests share one in-flight probe, reserved before sending. A dedicated worker
 * performs potentially blocking sends, so every caller immediately receives a stage.
 * HealthService bounds each request's wait without cancelling the shared probe.
 * Receivers acknowledge and ignore probes.
 */
@Component
public class KafkaProducerHealthCheck implements HealthCheck {

  private final RemoteObservationManagerConfiguration configuration;
  private final KafkaNetworkAdapter adapter;
  private final AtomicReference<CompletableFuture<Result>> pendingProbe = new AtomicReference<>();
  private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
      .setNameFormat("health-check-kakfa-%d")
      .setDaemon(true)
      .build());

  @Inject
  public KafkaProducerHealthCheck(
      @NotNull RemoteObservationManagerConfiguration configuration,
      @NotNull KafkaNetworkAdapter adapter) {
    this.configuration = configuration;
    this.adapter = adapter;
  }

  @Override
  @NotNull
  public String id() {
    return "observation.kafka.producer";
  }

  @Override
  @NotNull
  public Scope scope() {
    return Scope.MONITORING;
  }

  @Override
  @NotNull
  public CompletionStage<@NotNull Result> check() {
    if (configuration.getImplementation().filter("kafka"::equals).isEmpty()) {
      return completedFuture(new Result(Status.DISABLED, "Kafka remote observation is disabled"));
    } else if (!adapter.isProducerRunning()) {
      return completedFuture(new Result(Status.DOWN, "Kafka producer is not started"));
    } else {
      return getProbeResult();
    }
  }

  @NotNull
  private CompletableFuture<@NotNull Result> getProbeResult() {
    while (true) {
      var current = pendingProbe.get();
      if ((current != null) && !current.isDone()) {
        return current;
      }
      var result = new CompletableFuture<Result>();
      if (pendingProbe.compareAndSet(current, result)) {
        sendProbe(result);
        return result;
      }
    }
  }

  private void sendProbe(@NotNull CompletableFuture<@NotNull Result> result) {
    try {
      completedFuture(null)
          .thenComposeAsync(ignored -> adapter.probeProducer(), executor)
          .whenComplete((ignored, failure) -> {
            if (failure == null) {
              result.complete(new Result(Status.UP, "Kafka producer probe acknowledged"));
            } else {
              result.completeExceptionally(failure);
            }
          });
    } catch (Exception failure) {
      result.completeExceptionally(failure);
    }
  }

  @PreDestroy
  void shutdownExecutor() {
    executor.shutdownNow();
  }

}
