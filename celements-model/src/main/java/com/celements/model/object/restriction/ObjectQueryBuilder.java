package com.celements.model.object.restriction;

import static com.google.common.base.Preconditions.*;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.list.ListField;
import com.celements.model.object.ObjectBridge;
import com.google.common.collect.Lists;

@NotThreadSafe
public abstract class ObjectQueryBuilder<B extends ObjectQueryBuilder<B, O>, O> {

  private final ObjectQuery<O> query;

  public ObjectQueryBuilder() {
    this.query = new ObjectQuery<>();
  }

  protected abstract @NotNull ObjectBridge<?, O> getBridge();

  /**
   * adds all restrictions from the the given {@link ObjectQuery}
   */
  public final @NotNull B with(@NotNull ObjectQuery<O> query) {
    query.streamRestrictions().forEach(this::filter);
    return getThis();
  }

  /**
   * restricts to objects for the given {@link Predicate}
   */
  public final @NotNull B filter(@NotNull Predicate<O> restriction) {
    query.add(checkNotNull(restriction));
    return getThis();
  }

  /**
   * restricts to objects for the given {@link Predicate}s
   */
  public final @NotNull B filter(@NotNull Iterable<? extends Predicate<O>> restrictions) {
    restrictions.forEach(this::filter);
    return getThis();
  }

  /**
   * restricts to objects with the given {@link ClassIdentity}
   */
  public final @NotNull B filter(@NotNull ClassIdentity classId) {
    return filter(new ClassRestriction<>(getBridge(), classId));
  }

  /**
   * restricts to objects for the given {@link ClassField} and value<br>
   * <br>
   * NOTE: value may not be null, instead use {@link #filterAbsent(ClassField)}
   */
  public final @NotNull <T> B filter(@NotNull ClassField<T> field, @NotNull T value) {
    if (isSingleSelectListField(field)) {
      return filter(newListFieldRestriction(field, value));
    } else {
      return filter(new FieldRestriction<>(getBridge(), field, value));
    }
  }

  private boolean isSingleSelectListField(ClassField<?> field) {
    return (field instanceof ListField) && !((ListField<?>) field).isMultiSelect();
  }

  /**
   * unchecked conversion warning since compiler is unable to perform type check for the generic
   * relationship {@code ListField<T> extends ClassField<List<T>>}
   */
  @SuppressWarnings("unchecked")
  private <T, S> ObjectRestriction<O> newListFieldRestriction(ClassField<T> field, T value) {
    ListField<S> listField = (ListField<S>) field;
    List<S> values = (List<S>) value;
    return new FieldRestriction<>(getBridge(), listField, Lists.partition(values, 1));
  }

  /**
   * restricts to objects for the given {@link ClassField} and optional value
   * <p>
   * if value is absent, restricts to objects with no value for the given field
   */
  public final @NotNull <T> B filter(@NotNull ClassField<T> field,
      @NotNull Optional<? extends T> value) {
    return checkNotNull(value)
        .map(v -> filter(field, v))
        .orElseGet(() -> filterAbsent(field));
  }

  /**
   * restricts to objects for the given {@link ClassField} and possible values (logical OR)
   */
  public final @NotNull <T> B filter(@NotNull ClassField<T> field,
      @NotNull Collection<? extends T> values) {
    return filter(new FieldRestriction<>(getBridge(), field, values));
  }

  /**
   * restricts to objects with no value for the given {@link ClassField}
   */
  public final @NotNull B filterAbsent(@NotNull ClassField<?> field) {
    return filter(new FieldAbsentRestriction<>(getBridge(), field));
  }

  /**
   * restricts to objects with present value(s) for the given {@link ClassField}
   */
  public final @NotNull B filterPresent(@NotNull ClassField<?> field) {
    return filter(new FieldPresentRestriction<>(getBridge(), field));
  }

  /**
   * restricts to objects with the given number
   */
  public final @NotNull B filter(int number) {
    return filter(new NumberRestriction<>(getBridge(), number));
  }

  /**
   * restricts to the given object
   */
  public final @NotNull B filter(@NotNull O obj) {
    return filter(new IdentityRestriction<>(getBridge(), obj));
  }

  /**
   * restricts to the given objects
   */
  public final @NotNull B filterIdentity(@NotNull Iterable<O> objects) {
    return filter(new IdentityRestriction<>(getBridge(), objects));
  }

  /**
   * @return a new {@link ObjectQuery} for the current builder state
   */
  public ObjectQuery<O> getQuery() {
    return new ObjectQuery<>(query.streamRestrictions());
  }

  protected abstract B getThis();

  @Override
  public String toString() {
    return query.toString();
  }

}
