package com.celements.model.util;

import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.reference.RefBuilder;
import com.celements.model.reference.ReferenceProvider;
import com.google.common.base.Optional;
import com.xpn.xwiki.XWikiConstant;

@ComponentRole
public interface ModelUtils {

  /**
   * @deprecated since 6.0 instead use {@link XWikiConstant#MAIN_WIKI}
   */
  @Deprecated
  @NotNull
  WikiReference getMainWikiRef();

  /**
   * @deprecated since 6.0 instead use {@link ReferenceProvider#getAllWikis}
   */
  @Deprecated
  @NotNull
  Stream<WikiReference> getAllWikis();

  /**
   * @deprecated since 6.0 instead use {@link ReferenceProvider#getAllSpaces}
   */
  @Deprecated
  @NotNull
  Stream<SpaceReference> getAllSpaces(@NotNull WikiReference wikiRef);

  /**
   * @deprecated since 6.0 instead use {@link ReferenceProvider#getAllDocsForSpace}
   */
  @Deprecated
  @NotNull
  Stream<DocumentReference> getAllDocsForSpace(@NotNull SpaceReference spaceRef);

  @NotNull
  String getDatabaseName(@NotNull WikiReference wikiRef);

  /**
   * @deprecated since 4.5, instead use {@link References#isAbsoluteRef(EntityReference)}
   */
  @Deprecated
  boolean isAbsoluteRef(@NotNull EntityReference ref);

  /**
   * @deprecated since 4.5, instead use {@link References#cloneRef(EntityReference)}
   */
  @NotNull
  @Deprecated
  EntityReference cloneRef(@NotNull EntityReference ref);

  /**
   * @deprecated since 4.5, instead use {@link References#cloneRef(EntityReference, Class)}
   */
  @NotNull
  @Deprecated
  <T extends EntityReference> T cloneRef(@NotNull EntityReference ref, @NotNull Class<T> token);

  /**
   * @deprecated since 4.5, instead use {@link References#extractRef(EntityReference, Class)}
   */
  @NotNull
  @Deprecated
  <T extends EntityReference> Optional<T> extractRef(@Nullable EntityReference fromRef,
      @NotNull Class<T> token);

  /**
   * @deprecated since 4.5, instead use {@link RefBuilder} or
   *             {@link References#adjustRef(EntityReference, Class, EntityReference)}
   */
  @NotNull
  @Deprecated
  <T extends EntityReference> T adjustRef(@NotNull T ref, @NotNull Class<T> token,
      @Nullable EntityReference toRef);

  /**
   * resolves an absolute reference from the given name
   *
   * @param name
   *          to be resolved, may not be empty
   * @return a resolved reference
   * @throws IllegalArgumentException
   *           if unable to resolve absolute reference from name
   */
  @NotNull
  EntityReference resolveRef(@NotNull String name);

  /**
   * resolves an absolute reference from the given name and baseRef
   *
   * @param name
   *          to be resolved, may not be empty
   * @param baseRef
   *          a reference used as base for resolving
   * @return a resolved reference
   * @throws IllegalArgumentException
   *           if unable to resolve absolute reference from name and baseRef
   */
  @NotNull
  EntityReference resolveRef(@NotNull String name, @Nullable EntityReference baseRef);

  /**
   * resolves an absolute reference from the given name and baseRef
   *
   * @param name
   *          to be resolved, may not be empty
   * @param token
   *          for the reference type
   * @param baseRef
   *          a reference used as base for resolving
   * @return a resolved reference
   * @throws IllegalArgumentException
   *           if unable to resolve absolute reference from name and baseRef
   */
  @NotNull
  <T extends EntityReference> T resolveRef(@NotNull String name, @NotNull Class<T> token,
      @Nullable EntityReference baseRef);

  /**
   * resolves an absolute reference from the given name
   *
   * @param name
   *          to be resolved, may not be empty
   * @param token
   *          for the reference type
   * @return a resolved reference
   * @throws IllegalArgumentException
   *           if unable to resolve absolute reference from name
   */
  @NotNull
  <T extends EntityReference> T resolveRef(@NotNull String name, @NotNull Class<T> token);

  /**
   * @param ref
   * @param mode
   * @return serialised string representation of the given reference, depending on the mode
   */
  @NotNull
  String serializeRef(@NotNull EntityReference ref, @NotNull ReferenceSerializationMode mode);

  /**
   * @param ref
   * @return serialised global string representation of the given reference (e.g. "wiki:space.doc")
   */
  @NotNull
  String serializeRef(@NotNull EntityReference ref);

  /**
   * @param ref
   * @return serialised local string representation of the given reference (e.g. "space.doc")
   */
  @NotNull
  String serializeRefLocal(@NotNull EntityReference ref);

  /**
   * Normalizes the given language code. Converts the given language code to lower case and checks
   * if it's a valid an ISO 639 language code.
   *
   * <pre>
   * normalizeLanguage(null)      = ""
   * normalizeLanguage("")        = ""
   * normalizeLanguage("  ")      = ""
   * normalizeLanguage("en")      = "en"
   * normalizeLanguage("DE_ch")   = "de_CH"
   * normalizeLanguage("default") = ""
   * normalizeLanguage("invalid") throws IAE
   * </pre>
   *
   * @param lang
   *          the language code to normalize
   * @return normalized language code
   * @throws IllegalArgumentException
   *           if the code is invalid
   */
  @NotNull
  String normalizeLang(@Nullable String lang);

  /**
   * @deprecated since 6.0 instead use {@link ExecutionContext#computeIfAbsent}
   */
  @Deprecated
  <T> @Nullable T computeExecPropIfAbsent(@NotEmpty String key, @NotNull Supplier<T> defaultSetter);

}
