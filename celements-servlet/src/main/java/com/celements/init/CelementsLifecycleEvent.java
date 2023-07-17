package com.celements.servlet;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import org.springframework.context.ApplicationEvent;

public class CelementsLifecycleEvent extends ApplicationEvent {

  private static final long serialVersionUID = 20230430012323L;

  public enum State {
    STARTED, INITIALISED, STOPPED;
  }

  private final State state;

  public CelementsLifecycleEvent(@NotNull Object source, @NotNull State type) {
    super(source);
    this.state = checkNotNull(type);
  }

  @NotNull
  public State getType() {
    return state;
  }

}
