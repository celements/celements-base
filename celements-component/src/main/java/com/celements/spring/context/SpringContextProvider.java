package com.celements.spring.context;

import static com.google.common.base.Preconditions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public final class SpringContextProvider implements ApplicationContextAware {

  private static final AtomicReference<ApplicationContext> CONTEXT = new AtomicReference<>();

  public static ApplicationContext getSpringContext() {
    var context = CONTEXT.get();
    checkState(context != null, "context not set");
    return context;
  }

  public static ListableBeanFactory getBeanFactory() {
    return getSpringContext();
  }

  public static ApplicationEventPublisher getEventPublisher() {
    return getSpringContext();
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    CONTEXT.set(checkNotNull(context));
  }

}
