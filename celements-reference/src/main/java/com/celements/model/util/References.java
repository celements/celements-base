package com.celements.model.util;

import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.Preconditions.*;

import java.util.Iterator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.celements.model.reference.RefBuilder;
import com.google.common.base.Optional;

import one.util.streamex.StreamEx;

public final class References {

  /**
   * @param ref
   * @return false if the given reference is relative
   */
  public static boolean isAbsoluteRef(@NotNull EntityReference ref) {
    Iterator<EntityType> iter = createIteratorFrom(checkNotNull(ref).getType());
    while (iter.hasNext()) {
      ref = ref.getParent();
      if ((ref == null) || (ref.getType() != iter.next())) {
        // incomplete or wrong type order
        return false;
      }
    }
    return ref.getParent() == null; // has to be iterated to root level
  }

  @NotNull
  public static Class<? extends EntityReference> determineClass(@NotNull EntityReference ref) {
    Class<? extends EntityReference> token = ref.getClass();
    if ((token == EntityReference.class) && isAbsoluteRef(ref)) {
      token = getClassForEntityType(ref.getType());
    }
    return token;
  }

  /**
   * @param ref
   *          the reference to be cloned
   * @return a cloned instance of the reference
   * @deprecated since 5.4, references are immutable
   */
  @Deprecated
  @NotNull
  public static EntityReference cloneRef(@NotNull EntityReference ref) {
    return cloneRef(ref, EntityReference.class);
  }

  /**
   * @param ref
   *          the reference to be cloned
   * @param token
   *          type of the reference
   * @return a cloned instance of the reference of type T
   * @throws IllegalArgumentException
   *           when relative references are being cloned as subtypes of {@link EntityReference}
   * @deprecated since 5.4, references are immutable. drop or use
   *             {@link #asCompleteRef(EntityReference, Class)} instead
   */
  @Deprecated
  @NotNull
  public static <T extends EntityReference> T cloneRef(@NotNull EntityReference ref,
      @NotNull Class<T> token) {
    // we've misused the functionality of cloneRef instead of using asCompleteRef in the past,
    // thus we require this redirect for all clients
    return asCompleteRef(ref, token);
  }

  /**
   * @param ref
   *          the reference to be cloned
   * @param token
   *          type of the reference
   * @return an absolute instance of the reference of type T
   * @throws IllegalArgumentException
   *           when calling with incomplete references for subtypes of {@link EntityReference}
   */
  @NotNull
  public static <T extends EntityReference> T asCompleteRef(@NotNull EntityReference ref,
      @NotNull Class<T> token) {
    checkNotNull(ref);
    checkNotNull(token);
    Class<T> determinedToken = assertAssignability(ref, token);
    if (token == EntityReference.class) {
      token = determinedToken;
    }
    T ret;
    try {
      ret = token.getConstructor(EntityReference.class).newInstance(ref);
    } catch (ReflectiveOperationException | SecurityException exc) {
      throw new IllegalArgumentException("Unsupported entity class: " + token, exc);
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  private static <T extends EntityReference> Class<T> assertAssignability(EntityReference ref,
      Class<T> token) throws IllegalArgumentException {
    Class<? extends EntityReference> determinedToken = determineClass(ref);
    if ((token != EntityReference.class) && !token.isAssignableFrom(determinedToken)) {
      String msg = "Given " + (isAbsoluteRef(ref) ? "absolute reference (" + determineClass(
          ref).getSimpleName() + ")" : "relative reference") + " is not assignable to '"
          + token.getSimpleName() + "' - " + ref;
      throw new IllegalArgumentException(msg);
    }
    return (Class<T>) determinedToken;
  }

  /**
   * @param fromRef
   *          the reference to extract from
   * @param token
   *          reference class to extract
   * @return optional of the extracted reference
   * @deprecated since 5.4, instead use {@link EntityReference#extractRef(Class)}
   */
  @Deprecated
  public static <T extends EntityReference> Optional<T> extractRef(
      @Nullable EntityReference fromRef, @NotNull Class<T> token) {
    if (fromRef != null) {
      return Optional.fromJavaUtil(fromRef.extractRef(token));
    }
    return Optional.absent();
  }

  /**
   * @deprecated since 5.4, instead use {@link EntityReference#extractRef(EntityType)}
   */
  @Deprecated
  public static Optional<EntityReference> extractRef(@Nullable EntityReference fromRef,
      @NotNull EntityType type) {
    if (fromRef != null) {
      return Optional.fromJavaUtil(fromRef.extractRef(type));
    }
    return Optional.absent();
  }

  /**
   * adjusts a relative or absolute reference to another one of higher order, e.g. a docRef to
   * another wikiRef.
   *
   * @param ref
   *          to be adjusted
   * @param token
   *          for the reference type
   * @param toRef
   *          it is adjusted to
   * @return a new instance of the adjusted reference
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  @NotNull
  public static <T extends EntityReference> T adjustRef(@NotNull T ref,
      @NotNull Class<? extends T> token, @Nullable EntityReference toRef) {
    return RefBuilder.from(ref).with(toRef).build(token);
  }

  /**
   * builds an absolute reference of the given token with the provided references (FIFO)
   *
   * @param token
   *          for the reference type
   * @param refs
   * @return a new, absolute instance of the combined references
   * @throws IllegalArgumentException
   *           if token is {@link EntityReference}, instead use
   *           {@link #combineRef(EntityReference...)} for relative references
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  @NotNull
  public static <T extends EntityReference> Optional<T> completeRef(@NotNull Class<T> token,
      EntityReference... refs) {
    EntityReference combinedRef = combineRef(token, getEntityTypeForClassOrThrow(token),
        refs).orNull();
    return castOrAbsent(combinedRef, token);
  }

  /**
   * builds a relative reference with the provided references (FIFO)
   *
   * @param refs
   * @return a new, relative instance of the combined references
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  @NotNull
  public static Optional<EntityReference> combineRef(EntityReference... refs) {
    return combineRef(EntityReference.class, null, refs);
  }

  /**
   * builds a relative reference from the given type (bottom-up) with the provided references (FIFO)
   *
   * @param type
   *          for the reference type
   * @param refs
   * @return a new, relative instance of the combined references
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  @NotNull
  public static Optional<EntityReference> combineRef(@Nullable EntityType type,
      EntityReference... refs) {
    return combineRef(EntityReference.class, type, refs);
  }

  @Deprecated
  @NotNull
  private static Optional<EntityReference> combineRef(@NotNull Class<?> token,
      @Nullable EntityType type, EntityReference... refs) {
    RefBuilder builder = RefBuilder.create().nullable();
    if (refs != null) {
      StreamEx.ofReversed(refs).forEach(builder::with);
    }
    return Optional.fromNullable(builder.build(type));
  }

  /**
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  public static EntityReference create(@NotNull EntityType type, @NotNull String name) {
    return create(type, name, null);
  }

  /**
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  public static EntityReference create(@NotNull EntityType type, @NotNull String name,
      @Nullable EntityReference parent) {
    return new RefBuilder().with(type, name).with(parent).buildRelative();
  }

  /**
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  public static <T extends EntityReference> T create(@NotNull Class<T> token,
      @NotNull String name) {
    return create(token, name, null);
  }

  /**
   * @deprecated since 5.4, instead use {@link RefBuilder}
   */
  @Deprecated
  public static <T extends EntityReference> T create(@NotNull Class<T> token, @NotNull String name,
      @Nullable EntityReference parent) {
    return new RefBuilder().with(getEntityTypeForClassOrThrow(token), name).with(parent).build(
        token);
  }

  private static <T extends EntityReference> Optional<T> castOrAbsent(EntityReference ref,
      Class<T> token) {
    if ((ref != null) && (checkNotNull(token).isAssignableFrom(ref.getClass()))) {
      return Optional.of(token.cast(ref));
    } else {
      return Optional.absent();
    }
  }

}
