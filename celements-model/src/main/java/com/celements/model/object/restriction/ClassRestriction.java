package com.celements.model.object.restriction;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.ObjectBridge;

@Immutable
public class ClassRestriction<O> extends ObjectRestriction<O> {

  private final LocalDocumentReference classRef;

  public ClassRestriction(
      @NotNull ObjectBridge<?, O> bridge,
      @NotNull LocalDocumentReference classRef) {
    super(bridge);
    this.classRef = checkNotNull(classRef);
  }

  @Override
  public boolean apply(@NotNull O obj) {
    return classRef.equals(getBridge().getObjectClass(obj));
  }

  @NotNull
  public LocalDocumentReference getClassRef() {
    return classRef;
  }

  /**
   * @deprecated instead use {@link #getClassRef()}
   */
  @Deprecated
  @NotNull
  public ClassIdentity getClassIdentity() {
    return getClassReference();
  }

  /**
   * @deprecated instead use {@link #getClassRef()}
   */
  @Deprecated
  @NotNull
  public ClassReference getClassReference() {
    return new ClassReference(classRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getClassRef(), this.getClass());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassRestriction other) {
      return super.equals(obj) && Objects.equals(this.classRef, other.classRef);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ClassRestriction [classRef=" + classRef + "]";
  }

}
