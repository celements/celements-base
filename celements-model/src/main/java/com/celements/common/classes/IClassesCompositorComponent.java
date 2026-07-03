package com.celements.common.classes;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.classes.ClassDefinition;

@ComponentRole
public interface IClassesCompositorComponent {

  /**
   * loads all {@link ClassDefinition} and {@link IClassCollectionRole} (deprecated) and creates
   * XClasses from them if they don't exist yet in the current wiki
   */
  void checkClasses();

  /**
   * loads all {@link ClassDefinition} and {@link IClassCollectionRole} (deprecated) and creates
   * XClasses from them if they don't exist yet in the given wiki
   */
  void checkClasses(@NotNull WikiReference wikiRef);

  boolean isActivated(@NotEmpty String name);

}
