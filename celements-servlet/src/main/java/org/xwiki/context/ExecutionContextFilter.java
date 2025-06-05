package org.xwiki.context;

import static com.celements.spring.context.SpringContextProvider.*;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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

  @Override
  public void destroy() {
    // No cleanup needed
  }

}
