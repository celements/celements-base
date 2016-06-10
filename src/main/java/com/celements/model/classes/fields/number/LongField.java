package com.celements.model.classes.fields.number;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

public class LongField extends NumberField<Long> {

  public LongField(@NotNull DocumentReference classRef, @NotNull String name) {
    super(classRef, name);
  }

  @NotNull
  @Override
  public NumberType getNumberType() {
    return NumberType.LONG;
  }

}
