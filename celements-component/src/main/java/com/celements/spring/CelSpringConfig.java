package com.celements.spring;

import static com.google.common.base.Preconditions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
    "org.xwiki",
    "com.xpn.xwiki",
    "com.celements" })
public class CelSpringConfig implements ApplicationContextAware {

  private final AtomicReference<ApplicationContext> context = new AtomicReference<>();

  public ApplicationContext getContext() {
    ApplicationContext ret = context.get();
    checkState(ret != null, "Spring Context not initialized");
    return ret;
  }

  @Override
  public void setApplicationContext(ApplicationContext context) throws BeansException {
    checkState(this.context.compareAndSet(null, checkNotNull(context)),
        "Spring Context already initialized");
  }

}
