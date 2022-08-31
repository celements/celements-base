package com.celements.model.reference;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.model.util.EntityTypeUtil.*;
import static com.google.common.base.Strings.*;

import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

public class RefBuilder implements Cloneable {

  private final TreeMap<EntityType, EntityReference> refs;
  private boolean nullable;

  public static RefBuilder create() {
    return new RefBuilder();
  }

  public static RefBuilder from(EntityReference ref) {
    return new RefBuilder().with(ref);
  }

  public RefBuilder() {
    refs = new TreeMap<>();
    nullable = false;
  }

  public RefBuilder(RefBuilder other) {
    refs = new TreeMap<>(other.refs);
    nullable = other.nullable;
  }

  public int depth() {
    return refs.size();
  }

  public RefBuilder nullable() {
    this.nullable = true;
    return this;
  }

  public RefBuilder wiki(String name) {
    return with(EntityType.WIKI, name);
  }

  public RefBuilder space(String name) {
    return with(EntityType.SPACE, name);
  }

  public RefBuilder doc(String name) {
    return with(EntityType.DOCUMENT, name);
  }

  public RefBuilder att(String name) {
    return with(EntityType.ATTACHMENT, name);
  }

  public RefBuilder with(EntityReference ref) {
    while (ref != null) {
      with(ref.getType(), ref.getName());
      ref = ref.getParent();
    }
    return this;
  }

  public RefBuilder with(EntityType type, String name) {
    if (type != null) {
      refs.put(type, !isNullOrEmpty(name) ? new EntityReference(name, type) : null);
    }
    return this;
  }

  /**
   * @return absolute reference, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public EntityReference build() {
    return build(!refs.isEmpty() ? refs.descendingMap().firstKey() : null);
  }

  /**
   * @param type
   * @return absolute reference of given class, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public EntityReference build(EntityType type) {
    Class<? extends EntityReference> token = EntityReference.class;
    if (type != null) {
      token = getClassForEntityType(type);
    }
    return build(token, type);
  }

  /**
   * @param token
   * @return absolute reference of given class, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public <T extends EntityReference> T build(Class<T> token) {
    return build(token, getEntityTypeForClass(token).orNull());
  }

  private <T extends EntityReference> T build(Class<T> token, EntityType type) {
    try {
      return unwrap(buildRelativeOpt(type)
          .map(rethrowFunction(ref -> token.getConstructor(EntityReference.class)
              .newInstance(ref))));
    } catch (ReflectiveOperationException exc) {
      if (!nullable) {
        throw new IllegalArgumentException("Unsupported entity class: " + token, exc);
      } else {
        return null;
      }
    }
  }

  /**
   * @param token
   * @return optional of absolute reference of given class
   */
  public <T extends EntityReference> Optional<T> buildOpt(Class<T> token) {
    final boolean nullableTmp = this.nullable;
    try {
      return Optional.ofNullable(this.nullable().build(token));
    } catch (IllegalArgumentException iae) {
      throw new IllegalStateException("should not happend due to nullable", iae);
    } finally {
      this.nullable = nullableTmp;
    }
  }

  /**
   * @return relative reference, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public EntityReference buildRelative() {
    return buildRelative(null);
  }

  /**
   * @param type
   * @return relative reference from type, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public EntityReference buildRelative(EntityType type) {
    return unwrap(buildRelativeOpt(type));
  }

  /**
   * @param type
   * @return relative reference from type, absent if insufficient values
   */
  public Optional<EntityReference> buildRelativeOpt(EntityType type) {
    return refs.values().stream()
        .filter(Objects::nonNull)
        .filter(ref -> isAtLeastOfType(ref, type))
        .reduce((parent, ref) -> (parent != null) ? ref.appendParent(parent) : ref);
  }

  private boolean isAtLeastOfType(EntityReference ref, EntityType minType) {
    return (minType == null) || (minType.compareTo(ref.getType()) >= 0);
  }

  @Override
  public RefBuilder clone() {
    return new RefBuilder(this);
  }

  @Override
  public String toString() {
    return "RefBuilder [" + (depth() > 0 ? buildRelative() : "") + "]";
  }

  private <T extends EntityReference> T unwrap(Optional<T> opt) {
    return nullable
        ? opt.orElse(null)
        : opt.orElseThrow(() -> new IllegalArgumentException("missing data for building ref"));
  }

}
