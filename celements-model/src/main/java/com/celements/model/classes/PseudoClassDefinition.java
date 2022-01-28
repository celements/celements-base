package com.celements.model.classes;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

public abstract class PseudoClassDefinition extends AbstractClassDefinition {

  public static final String CLASS_SPACE = "PseudoClass";

  protected PseudoClassDefinition(@NotNull ClassReference classRef) {
    super(classRef);
    checkArgument(getClassSpaceName().equals(CLASS_SPACE));
  }

  @Override
  public boolean isBlacklisted() {
    // pseudo classes should never be created
    return true;
  }

  @Override
  public boolean isValidObjectClass() {
    return false;
  }

  @Override
  public boolean isInternalMapping() {
    return false;
  }

}
