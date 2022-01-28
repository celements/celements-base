package com.celements.model.classes;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

@ComponentRole
public interface ClassPackage {

  public static final String CFG_SRC_KEY = "celements.classdefinition.active";

  /**
   * @return the name of the class definition package
   */
  @NotEmpty
  public String getName();

  /**
   * @return true if the package is activated
   */
  public boolean isActivated();

  /**
   * @return the class definitions contained in this package
   */
  @NotNull
  public List<? extends ClassDefinition> getClassDefinitions();

}
