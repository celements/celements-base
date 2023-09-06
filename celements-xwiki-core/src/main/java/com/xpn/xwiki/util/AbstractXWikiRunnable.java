package com.xpn.xwiki.util;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for any XWiki daemon class. It provide tools to initialize execution context.
 *
 * @since 1.8.4,1.9RC1,2.0M1
 * @version $Id$
 */
public abstract class AbstractXWikiRunnable implements Runnable {

  /**
   * Logging tools.
   */
  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Map<String, Object> properties;

  protected AbstractXWikiRunnable() {
    properties = Map.of();
  }

  /**
   * @param propertyName
   *          the name of the property to put in the initialized context
   * @param propertyValue
   *          the value of the property to put in the initialized context
   */
  protected AbstractXWikiRunnable(String propertyName, Object propertyValue) {
    properties = Map.of(propertyName, propertyValue);
  }

  /**
   * @param properties
   *          properties to put in the initialized context
   */
  protected AbstractXWikiRunnable(Map<String, Object> properties) {
    this.properties = ImmutableMap.copyOf(properties);
  }

  /**
   * Initialize execution context for the current thread.
   *
   * @return the new execution context
   * @throws ExecutionContextException
   *           error when try to initialize execution context
   */
  protected ExecutionContext initExecutionContext() throws ExecutionContextException {
    ExecutionContext executionContext = new ExecutionContext();
    executionContext.setProperties(properties);
    Utils.getComponent(Execution.class).setContext(executionContext);
    Utils.getComponent(ExecutionContextManager.class).initialize(executionContext);
    return executionContext;
  }

  protected void cleanupExecutionContext() {
    // We must ensure we clean the ThreadLocal variables located in the Execution
    // component as otherwise we will have a potential memory leak.
    Utils.getComponent(Execution.class).removeContext();
  }

  @Override
  public final void run() {
    try {
      initExecutionContext();
      runInternal();
    } catch (Exception e) {
      logger.error("Failed to initialize execution context", e);
      throw new RuntimeException(e);
    } finally {
      cleanupExecutionContext();
    }
  }

  protected abstract void runInternal() throws Exception;

}
