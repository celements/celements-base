package com.celements.model.object.restriction;

import static com.celements.common.MoreObjectsCel.tryCast;
import static com.google.common.collect.ImmutableSet.toImmutableSet;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.LocalDocumentReference;

import one.util.streamex.StreamEx;

@NotThreadSafe
public class ObjectQuery<O> {

  private Set<Predicate<O>> restrictions = new LinkedHashSet<>();

  public ObjectQuery() {}

  public ObjectQuery(Stream<? extends Predicate<O>> stream) {
    stream.forEach(this::add);
  }

  public ObjectQuery<O> add(Predicate<O> restr) {
    if (restr != null) {
      restrictions.add(restr);
    }
    return this;
  }

  public StreamEx<Predicate<O>> streamRestrictions() {
    return StreamEx.of(restrictions);
  }

  public Predicate<O> predicate(LocalDocumentReference classRef) {
    return streamRestrictions()
        .filter(restr -> tryCast(restr, ClassRestriction.class)
            .map(classRestr -> classRestr.getClassRef().equals(classRef))
            .orElse(true))
        .reduce((p1, p2) -> p1.and(p2))
        .orElse(o -> true);
  }

  public StreamEx<LocalDocumentReference> streamClassRefs() {
    return streamRestrictions()
        .flatMap(tryCast(ClassRestriction.class))
        .map(ClassRestriction::getClassRef)
        .distinct();
  }

  public Set<ClassReference> getObjectClasses() {
    return streamClassRefs().map(ClassReference::new).collect(toImmutableSet());
  }

  public Set<FieldRestriction<O, ?>> getFieldRestrictions(LocalDocumentReference classRef) {
    return streamRestrictions()
        .flatMap(tryCast(getFieldRestrictionClass()))
        .filter(fieldRestr -> fieldRestr.getClassRef().equals(classRef))
        .collect(toImmutableSet());
  }

  @SuppressWarnings("unchecked")
  private Class<FieldRestriction<O, ?>> getFieldRestrictionClass() {
    return (Class<FieldRestriction<O, ?>>) (Class<?>) FieldRestriction.class;
  }

  @Override
  public int hashCode() {
    return Objects.hash(restrictions);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ObjectQuery) {
      ObjectQuery<?> other = (ObjectQuery<?>) obj;
      return Objects.equals(this.restrictions, other.restrictions);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ObjectQuery " + restrictions;
  }

}
