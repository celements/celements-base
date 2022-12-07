package com.celements.model.classes.fields;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.objects.PropertyInterface;

public interface ClassField<T> {

  /**
   * @deprecated instead use {@link #getClassReference()}
   */
  @Deprecated
  @NotNull
  ClassDefinition getClassDef();

  @NotNull
  ClassReference getClassReference();

  @NotNull
  String getName();

  @NotNull
  Class<T> getType();

  @NotNull
  PropertyInterface getXField();

  @NotNull
  String serialize();

}
