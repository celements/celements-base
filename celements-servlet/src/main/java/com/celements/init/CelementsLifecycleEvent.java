package com.celements.init;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.context.ApplicationEvent;

public class CelementsLifecycleEvent extends ApplicationEvent {

  private static final long serialVersionUID = 202307171847L;

  private final String state;

  public CelementsLifecycleEvent(@NotNull Object source, @NotEmpty String state) {
    super(source);
    checkArgument(!nullToEmpty(state).isEmpty());
    this.state = state;
  }

  @NotEmpty
  public String getState() {
    return state;
  }

  @Override
  public String toString() {
    return getState();
  }

}
