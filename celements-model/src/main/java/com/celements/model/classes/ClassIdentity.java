package com.celements.model.classes;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;

public interface ClassIdentity {

  @NotNull
  LocalDocumentReference getLocalClassRef();

  /**
   * @deprecated instead use {@link #getLocalClassRef()}
   */
  @Deprecated
  @NotNull
  ClassReference getClassReference();

  /**
   * @deprecated instead use {@link #getLocalClassRef()} or resolve the class definition via the
   *             Spring context
   * @return the class definition if it exists
   */
  @Deprecated
  @NotNull
  Optional<ClassDefinition> getClassDefinition();

  /**
   * @deprecated instead resolve {@link #getLocalClassRef()}{@code .getDocRef(wikiRef)}
   */
  @Deprecated
  @NotNull
  DocumentReference getDocRef();

  /**
   * @deprecated instead use {@link #getLocalClassRef()}{@code .getDocRef(wikiRef)}
   */
  @Deprecated
  @NotNull
  DocumentReference getDocRef(@NotNull WikiReference wikiRef);

  boolean isValidObjectClass();

  @NotNull
  String serialize();

}
