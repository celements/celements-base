package org.xwiki.context;

import static com.celements.logging.LogUtils.*;
import static com.celements.spring.context.SpringContextProvider.*;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionContextFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionContextFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // No initialization needed
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    Execution execution = getBeanFactory().getBean(Execution.class);
    try {
      LOGGER.debug("setup execution context for request {}",
          defer(() -> getRequestUrl(request).orElse("'no HttpServletRequest'")));
      ExecutionContext ec = new ExecutionContext();
      execution.setContext(ec);
      ExecutionContextManager ecm = getBeanFactory().getBean(ExecutionContextManager.class);
      ecm.initialize(ec);
      chain.doFilter(request, response);
    } catch (ExecutionContextException exp) {
      LOGGER.error("Failed to execute request becuase initialize execution context failed", exp);
    } finally {
      execution.removeContext();
    }
  }

  private Optional<String> getRequestUrl(ServletRequest request) {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      StringBuffer requestURL = httpRequest.getRequestURL();
      String queryString = httpRequest.getQueryString();
      return Optional.of(requestURL.toString() + (queryString != null ? "?" + queryString : ""));
    }
    return Optional.empty();
  }

  @Override
  public void destroy() {
    // No cleanup needed
  }

}
