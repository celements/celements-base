package com.celements.observation.remote.kafka;

import static java.util.concurrent.CompletableFuture.*;

import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Component;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;

import com.celements.health.AsyncHealthCheck;

/**
 * Checks the existing producer by awaiting acknowledgement of a transport-only record,
 * which receivers acknowledge and ignore.
 */
@Component
public class KafkaProducerHealthCheck extends AsyncHealthCheck {

  private final RemoteObservationManagerConfiguration configuration;
  private final KafkaNetworkAdapter adapter;

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
  protected CompletionStage<@NotNull Result> runCheck() {
    if (configuration.getImplementation().filter(KafkaNetworkAdapter.NAME::equals).isEmpty()) {
      return completedFuture(new Result(Status.DISABLED, "Kafka remote observation is disabled"));
    } else if (!adapter.isProducerRunning()) {
      return completedFuture(new Result(Status.DOWN, "Kafka producer is not started"));
    } else {
      return adapter.probeProducer()
          .thenApply(ignored -> new Result(Status.UP, "Kafka producer check succeeded"));
    }
  }

}
