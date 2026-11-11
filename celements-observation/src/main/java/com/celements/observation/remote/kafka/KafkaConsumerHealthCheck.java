package com.celements.observation.remote.kafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.celements.health.HealthCheck;

/** Local worker state only; running does not establish broker connectivity or delivery. */
@Component
public class KafkaConsumerHealthCheck implements HealthCheck {

  private final RemoteObservationManagerConfiguration configuration;
  private final KafkaNetworkAdapter adapter;

  @Inject
  public KafkaConsumerHealthCheck(
      @NotNull RemoteObservationManagerConfiguration configuration,
      @NotNull KafkaNetworkAdapter adapter) {
    this.configuration = configuration;
    this.adapter = adapter;
  }

  @Override
  @NotNull
  public String id() {
    return "observation.kafka.consumer";
  }

  @Override
  @NotNull
  public Scope scope() {
    return Scope.READINESS;
  }

  @Override
  @NotNull
  public CompletionStage<@NotNull Result> check() {
    return CompletableFuture.completedFuture(runChecks());
  }

  @NotNull
  private Result runChecks() {
    if (configuration.getImplementation().filter(KafkaNetworkAdapter.NAME::equals).isEmpty()) {
      return new Result(Status.DISABLED, "Kafka remote observation is disabled");
    }
    return adapter.isConsumerRunning()
        ? new Result(Status.UP, "Kafka consumer worker is running")
        : new Result(Status.DOWN, "Kafka consumer worker is not running");
  }

}
