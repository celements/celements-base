package org.xwiki.model.reference;

import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.celements.model.util.References.*;
import static com.google.common.base.Preconditions.*;

import java.io.Serializable;
import java.util.Objects;

import javax.annotation.concurrent.Immutable;

import com.celements.common.MoreObjectsCel;
import com.xpn.xwiki.objects.BaseObject;

/**
 * similar to {@link ObjectReference} but immutable and doesn't extend {@link EntityReference}
 * <p>
 * TODO [CELDEV-521] Immutable References
 * let it extend EntityReference and implement ImmutableReference
 * </p>
 */
@Immutable
public class ImmutableObjectReference implements Serializable {

  private static final long serialVersionUID = 2638847219120582547L;

  private final ImmutableDocumentReference docRef;
  private final ClassReference classRef;
  private final int nb;

  public ImmutableObjectReference(DocumentReference docRef, ClassReference classRef, int nb) {
    this.docRef = cloneRef(docRef, ImmutableDocumentReference.class);
    this.classRef = checkNotNull(classRef);
    this.nb = nb;
    checkArgument(nb >= 0);
  }

  public DocumentReference getDocumentReference() {
    return docRef;
  }

  public ClassReference getClassReference() {
    return classRef;
  }

  public int getNumber() {
    return nb;
  }

  @Override
  public int hashCode() {
    return Objects.hash(docRef, classRef, nb);
  }

  @Override
  public boolean equals(Object obj) {
    return MoreObjectsCel.tryCast(obj, ImmutableObjectReference.class)
        .map(other -> Objects.equals(this.docRef, other.docRef)
            && Objects.equals(this.classRef, other.classRef)
            && Objects.equals(this.nb, other.nb))
        .orElse(false);
  }

  public String serialize() {
    return docRef.serialize(GLOBAL) + "_" + classRef.serialize() + "_" + nb;
  }

  @Override
  public String toString() {
    return serialize();
  }

  public static ImmutableObjectReference from(BaseObject xObj) {
    return new ImmutableObjectReference(xObj.getDocumentReference(),
        new ClassReference(xObj.getXClassReference()), xObj.getNumber());
  }

}
