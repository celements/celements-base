package com.celements.init.request;

import static com.celements.logging.LogUtils.*;
import static com.celements.spring.context.SpringContextProvider.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.context.ExecutionContextException;

import com.celements.wiki.exception.WikiMissingException;

@Component("executionContextFilter")
public class ExecutionContextFilter extends GenericFilterBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionContextFilter.class);
  private final List<Predicate<HttpServletRequest>> excludedRequests;

  @Inject
  public ExecutionContextFilter(
      @Qualifier("executionContextExclusion")
      @NotNull List<@NotNull Predicate<HttpServletRequest>> excludedRequests) {
    this.excludedRequests = List.copyOf(excludedRequests);
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    if ((request instanceof HttpServletRequest httpRequest)
        && excludedRequests.stream().anyMatch(exclusion -> exclusion.test(httpRequest))) {
      chain.doFilter(request, response);
      return;
    }
    CelementsRequestFilter requestFilter = getBeanFactory().getBean(CelementsRequestFilter.class);
    try {
      LOGGER.debug("setup execution context for request [{}], dispatcher [{}]",
          defer(() -> getRequestUrl(request).orElse("'no HttpServletRequest'")),
          request.getDispatcherType());
      if ((request instanceof HttpServletRequest) && (response instanceof HttpServletResponse)) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        requestFilter.preExecute(httpRequest, httpResponse);
      }
      chain.doFilter(request, response);
    } catch (ExecutionContextException | WikiMissingException | ExecutionException
        | ServletContainerException exp) {
      LOGGER.error("Failed to execute request because initialize execution context failed", exp);
    } finally {
      requestFilter.postExecute();
    }
  }

  private Optional<String> getRequestUrl(ServletRequest request) {
    if (!(request instanceof HttpServletRequest)) {
      return Optional.empty();
    }
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    StringBuilder url = new StringBuilder(httpRequest.getRequestURL());
    String query = httpRequest.getQueryString();
    if ((query != null) && !query.isEmpty()) {
      url.append('?').append(query);
    }
    return Optional.of(url.toString());
  }

}
