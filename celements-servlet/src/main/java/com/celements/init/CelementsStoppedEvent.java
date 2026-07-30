package com.celements.init;

import javax.validation.constraints.NotNull;

public class CelementsStoppedEvent extends CelementsLifecycleEvent {

  private static final long serialVersionUID = 202307171847L;

  public static final String STATE = "STOPPED";

  public CelementsStoppedEvent(@NotNull Object source) {
    super(source, STATE);
  }

}
