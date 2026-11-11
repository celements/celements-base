package com.celements.init;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.celements.health.HealthCheck;
import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;

public class InitialisedHealthCheckTest {

  @Test
  public void test_check_beforeInitialization() {
    try (var context = new AnnotationConfigApplicationContext(
        InitialisedHealthCheck.class)) {
      var check = context.getBean(HealthCheck.class);
      assertEquals("celements.initialised", check.id());
      assertEquals(Scope.READINESS, check.scope());
      var result = check.check().toCompletableFuture();
      assertTrue(result.isDone());
      assertEquals(new Result(Status.DOWN, "Application initialization pending"), result.join());
    }
  }

  @Test
  public void test_check_afterInitialization() {
    try (var context = new AnnotationConfigApplicationContext(
        InitialisedHealthCheck.class)) {
      var check = context.getBean(HealthCheck.class);
      context.publishEvent(new CelementsStartedEvent(this));
      assertEquals(Status.DOWN, check.check().toCompletableFuture().join().status());

      context.publishEvent(new CelementsInitialisedEvent(this));
      var result = check.check().toCompletableFuture();
      assertTrue(result.isDone());
      assertEquals(new Result(Status.UP, "Application initialization completed"), result.join());
    }
  }

}
