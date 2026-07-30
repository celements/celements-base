package com.celements.init;

import javax.validation.constraints.NotNull;

public class CelementsInitialisedEvent extends CelementsLifecycleEvent {

  private static final long serialVersionUID = 202307171847L;

  public static final String STATE = "INITIALISED";

  public CelementsInitialisedEvent(@NotNull Object source) {
    super(source, STATE);
  }

}
