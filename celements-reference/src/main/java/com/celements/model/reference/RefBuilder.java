package com.celements.model.reference;

import static com.celements.model.util.EntityTypeUtil.*;
import static com.celements.model.util.References.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

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
      if (!isNullOrEmpty(name)) {
        refs.put(type, new EntityReference(name, type));
      } else {
        refs.remove(type);
      }
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
    return build(token);
  }

  /**
   * @param token
   * @return absolute reference of given class, null if insufficient values and {@link #nullable()}
   * @throws IllegalArgumentException
   *           if insufficient values and not {@link #nullable()}
   */
  public <T extends EntityReference> T build(Class<T> token) {
    T ref = null;
    try {
      EntityReference relativeRef = buildRelative(getEntityTypeForClass(token).orNull());
      if (relativeRef != null) {
        ref = toAbsoluteRef(relativeRef, token);
      }
    } catch (IllegalArgumentException iae) {
      if (!nullable) {
        throw iae;
      }
    }
    return ref;
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
    EntityReference ret = null;
    for (EntityReference ref : refs.values()) {
      if ((type == null) || (type.compareTo(ref.getType()) >= 0)) {
        if (ret == null) {
          ret = ref;
        } else {
          ret = new EntityReference(ref, ret);
        }
      }
    }
    checkArgument(nullable || (ret != null), "missing information for building reference");
    return ret;
  }

  @Override
  public RefBuilder clone() {
    RefBuilder clone = new RefBuilder();
    if (depth() > 0) {
      clone.with(this.buildRelative());
    }
    clone.nullable = this.nullable;
    return clone;
  }

  @Override
  public String toString() {
    return "RefBuilder [" + (depth() > 0 ? buildRelative() : "") + "]";
  }

}
