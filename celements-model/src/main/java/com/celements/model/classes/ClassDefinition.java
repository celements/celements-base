package com.celements.model.classes;

import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.classes.fields.ClassField;

@ComponentRole
public interface ClassDefinition extends ClassIdentity {

  List<String> LANG_FIELD_NAMES = List.of("lang", "language");

  String CFG_SRC_KEY = "celements.classdefinition.blacklist";

  /**
   * @return the name of the component and class definition, used for blacklisting
   */
  @NotEmpty
  String getName();

  /**
   * @deprecated instead use {@link #getLocalClassRef()}
   * @return the document reference on which the class is defined, using the current wiki
   */
  @Deprecated
  @NotNull
  DocumentReference getClassRef();

  /**
   * @deprecated instead use {@link #getLocalClassRef()}{@code .getDocRef(wikiRef)}
   * @param wikiRef the wiki for the returned document reference
   * @return the document reference on which the class is defined, using the given wiki
   */
  @Deprecated
  @NotNull
  DocumentReference getClassRef(@NotNull WikiReference wikiRef);

  /**
   * @return true if the class definition is blacklisted
   */
  boolean isBlacklisted();

  /**
   * @return true if the class is mapped internally (hibernate mapping)
   */
  boolean isInternalMapping();

  /**
   * @return a list of all fields defining this class
   */
  @NotNull
  List<ClassField<?>> getFields();

  /**
   * @param name
   * @return the defined field for the given name
   */
  @NotNull
  Optional<ClassField<?>> getField(@NotNull String name);

  /**
   * @param name
   * @param token
   * @return the defined field for the given name
   */
  @NotNull
  <T> Optional<ClassField<T>> getField(@NotNull String name, @NotNull Class<T> token);

  /**
   * @return the lang field for any multi-lang class definition, else absent
   */
  @NotNull
  Optional<ClassField<String>> getLangField();

}
