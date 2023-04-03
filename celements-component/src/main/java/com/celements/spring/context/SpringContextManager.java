package com.celements.spring.context;

import java.util.function.Supplier;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

public final class SpringContextManager {

  private static final SpringContextManager INSTANCE = new SpringContextManager();

  private final AnnotationConfigApplicationContext context;

  private SpringContextManager() {
    context = new CelementsAnnotationConfigApplicationContext("com.celements.spring");
  }

  public AnnotationConfigApplicationContext getContext() {
    return this.context;
  }

  public static Supplier<GenericApplicationContext> supply() {
    return INSTANCE::getContext;
  }

  public static GenericApplicationContext get() {
    return INSTANCE.getContext();
  }

}
