package com.celements.init.request;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Excluded requests must not resolve wiki-dependent beans. */
public class ExecutionContextFilterTest {

  @Test
  public void test_doFilter_anyExclusionSkipsWikiInitialization() throws Exception {
    var request = new MockHttpServletRequest("GET", "/operations/status");
    request.setServerName("unknown-tenant.invalid");
    var filter = new ExecutionContextFilter(List.of(
        candidate -> false,
        candidate -> candidate == request));
    var called = new AtomicBoolean();
    filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> called.set(true));
    assertTrue(called.get());
  }

  @Test
  public void test_doFilter_usesAllQualifiedExclusionBeans() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(
        ExecutionContextFilter.class, ExclusionConfig.class)) {
      var filter = context.getBean("executionContextFilter", ExecutionContextFilter.class);
      for (var path : new String[] { "/operations/status", "/maintenance/status" }) {
        var called = new AtomicBoolean();
        filter.doFilter(new MockHttpServletRequest("GET", path),
            new MockHttpServletResponse(), (req, res) -> called.set(true));
        assertTrue(called.get());
      }
    }
  }

  @Test
  public void test_springLifecycle_withoutExclusionBeans() {
    try (var context = new AnnotationConfigApplicationContext(ExecutionContextFilter.class)) {
      assertNotNull(context.getBean("executionContextFilter", ExecutionContextFilter.class));
    }
  }

  @Configuration
  static class ExclusionConfig {

    @Bean
    @Qualifier("executionContextExclusion")
    Predicate<HttpServletRequest> operationsRequests() {
      return request -> request.getRequestURI().equals("/operations/status");
    }

    @Bean
    @Qualifier("executionContextExclusion")
    Predicate<HttpServletRequest> maintenanceRequests() {
      return request -> request.getRequestURI().equals("/maintenance/status");
    }

    @Bean
    Predicate<HttpServletRequest> unrelatedRequests() {
      return request -> {
        throw new AssertionError("Unqualified predicates must not participate in exclusions");
      };
    }
  }
}
