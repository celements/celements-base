package com.celements.init;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.validation.constraints.NotNull;

import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.celements.health.HealthCheck;

@Component
public class InitialisedHealthCheck implements HealthCheck, ApplicationListener<CelementsInitialisedEvent> {

  private volatile boolean initialised;

  @Override
  @NotNull
  public String id() {
    return "celements.initialised";
  }

  @Override
  @NotNull
  public Scope scope() {
    return Scope.READINESS;
  }

  @Override
  @NotNull
  public CompletionStage<@NotNull Result> check() {
    return CompletableFuture.completedFuture(initialised
        ? new Result(Status.UP, "Application initialization completed")
        : new Result(Status.DOWN, "Application initialization pending"));
  }

  @Override
  public void onApplicationEvent(@NotNull CelementsInitialisedEvent event) {
    initialised = true;
  }

}
