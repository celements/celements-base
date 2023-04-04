package com.celements.spring.context;

import static com.google.common.base.Preconditions.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.springframework.context.support.GenericApplicationContext;

public final class SpringContext {

  public static final SpringContext INSTANCE = new SpringContext();

  private final AtomicReference<GenericApplicationContext> context;

  private SpringContext() {
    context = new AtomicReference<>();
  }

  public GenericApplicationContext getContext() {
    GenericApplicationContext ret = context.get();
    checkState(ret != null, "Spring Context not initialized");
    return ret;
  }

  public void setContext(GenericApplicationContext context) {
    checkState(this.context.compareAndSet(null, checkNotNull(context)),
        "Spring Context already initialized");
  }

  public static Supplier<GenericApplicationContext> supply() {
    return INSTANCE::getContext;
  }

  public static GenericApplicationContext get() {
    return INSTANCE.getContext();
  }

}
