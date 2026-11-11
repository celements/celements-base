package com.celements.init;

import javax.validation.constraints.NotNull;

public class CelementsStartedEvent extends CelementsLifecycleEvent {

  private static final long serialVersionUID = 202307171847L;

  public static final String STATE = "STARTED";

  public CelementsStartedEvent(@NotNull Object source) {
    super(source, STATE);
  }

}
