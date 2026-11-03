package com.celements.health;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.junit.Test;

import com.celements.health.HealthCheck.Result;
import com.celements.health.HealthCheck.Scope;
import com.celements.health.HealthCheck.Status;
import com.celements.health.HealthReport.CheckSnapshot;

public class HealthReportTest {

  @Test
  public void test_status_noChecks() {
    HealthReport report = new HealthReport(Map.of());
    assertEquals(Status.DOWN, report.status());
    assertFalse(report.ready());
    assertFalse(report.isHealthy());
  }

  @Test
  public void test_status_allStatusesAndScopes() {
    for (Scope scope : Scope.values()) {
      for (Status status : Status.values()) {
        CheckSnapshot snapshot = snapshot(status, scope);
        HealthReport report = new HealthReport(Map.of("check", snapshot));
        boolean healthy = (status == Status.UP) || (status == Status.DISABLED);
        assertEquals(status, report.status());
        assertEquals(healthy, report.isHealthy());
        assertEquals(healthy, snapshot.isHealthy());
        assertEquals(healthy || (scope == Scope.MONITORING), report.ready());
      }
    }
  }

  @Test
  public void test_ready_monitoringFailureDoesNotBlockRouting() {
    HealthReport report = new HealthReport(Map.of(
        "local", snapshot(Status.UP, Scope.READINESS),
        "database", snapshot(Status.DOWN, Scope.MONITORING)));
    assertEquals(Status.DOWN, report.status());
    assertTrue(report.ready());
  }

  @Test
  public void test_status_disabledDoesNotMaskReadinessFailure() {
    HealthReport report = new HealthReport(Map.of(
        "local", snapshot(Status.DOWN, Scope.READINESS),
        "database", snapshot(Status.DISABLED, Scope.MONITORING)));
    assertEquals(Status.DOWN, report.status());
    assertFalse(report.ready());
  }

  @Test
  public void test_constructor_immutableChecks() {
    HealthReport report = new HealthReport(Map.of("local", snapshot(Status.UP, Scope.READINESS)));
    assertThrows(UnsupportedOperationException.class, () -> report.checks().clear());
  }

  private static CheckSnapshot snapshot(Status status, Scope scope) {
    return new CheckSnapshot(new Result(status, ""), scope, Instant.EPOCH, Duration.ZERO);
  }
}
