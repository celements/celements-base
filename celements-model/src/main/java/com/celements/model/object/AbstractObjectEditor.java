package com.celements.model.object;

import static com.google.common.base.Preconditions.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.restriction.FieldRestriction;
import com.google.common.collect.ImmutableMap;

@NotThreadSafe
public abstract class AbstractObjectEditor<R extends AbstractObjectEditor<R, D, O>, D, O> extends
    AbstractObjectHandler<R, D, O> implements ObjectEditor<D, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectEditor.class);

  protected AbstractObjectEditor(@NotNull D doc) {
    super(doc);
  }

  @Override
  public abstract AbstractObjectEditor<?, D, O> clone();

  @Override
  public Map<ClassIdentity, O> create() {
    return create(false);
  }

  @Override
  public Map<ClassIdentity, O> createIfNotExists() {
    return create(true);
  }

  private Map<ClassIdentity, O> create(boolean ifNotExists) {
    return getQuery().getObjectClasses().stream().collect(ImmutableMap.toImmutableMap(
        Function.identity(), classId -> createObject(classId, ifNotExists)));
  }

  @Override
  public O createFirst() {
    return createFirst(false);
  }

  @Override
  public O createFirstIfNotExists() {
    return createFirst(true);
  }

  private O createFirst(boolean ifNotExists) {
    return getQuery().getObjectClasses().stream()
        .map(classId -> createObject(classId, ifNotExists))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("no class defined - " + this));
  }

  private O createObject(ClassIdentity classId, boolean ifNotExists) {
    checkArgument(classId.isValidObjectClass(),
        "unable to create object with invalid class [%s] on [%s]", classId, getDocRef());
    O obj = null;
    if (ifNotExists) {
      obj = fetch().filter(classId).stream().findFirst().orElse(null);
    }
    if (obj == null) {
      obj = getBridge().createObject(getDocument(), classId);
      for (FieldRestriction<O, ?> restriction : getQuery().getFieldRestrictions(classId)) {
        setObjectField(obj, restriction);
      }
      LOGGER.info("{} created object {} for {}", this, getBridge().getObjectNumber(obj), classId);
    }
    return obj;
  }

  private <T> void setObjectField(O obj, FieldRestriction<O, T> restriction) {
    T value = restriction.getValues().stream().findFirst().orElse(null);
    getBridge().getObjectFieldAccessor().set(obj, restriction.getField(), value);
    LOGGER.debug("{} set field {} on created object to value [{}]",
        this, restriction.getField(), value);
  }

  @Override
  public List<O> delete() {
    return fetch().stream().filter(this::deleteObject).collect(Collectors.toList());
  }

  @Override
  public Optional<O> deleteFirst() {
    return fetch().stream().findFirst().filter(this::deleteObject);
  }

  private boolean deleteObject(O obj) {
    boolean success = getBridge().deleteObject(getDocument(), obj);
    LOGGER.info("{} deleted object {} for {}: {}", this, getBridge().getObjectNumber(obj),
        getBridge().getObjectClass(obj), success);
    return success;
  }

  @Override
  public abstract AbstractObjectFetcher<?, D, O> fetch();

  @Override
  public <T> FieldEditor<T> editField(final ClassField<T> field) {
    final AbstractObjectFetcher<?, D, O> fetcher = fetch().filter(field.getClassReference());
    return new FieldEditor<T>() {

      @Override
      public boolean first(final T value) {
        return edit(() -> value, true);
      }

      @Override
      public boolean all(final T value) {
        return edit(() -> value, false);
      }

      @Override
      public boolean all(final Supplier<T> supplier) {
        return edit(supplier, false);
      }

      private boolean edit(Supplier<T> supplier, boolean onlyFirst) {
        checkNotNull(supplier);
        boolean changed = false;
        if (field.getClassReference().isValidObjectClass()) {
          Iterator<O> iter = fetcher.stream().iterator();
          boolean stop = false;
          while (!stop && iter.hasNext()) {
            changed |= getBridge().getObjectFieldAccessor()
                .set(iter.next(), field, supplier.get());
            stop = onlyFirst;
          }
        } else {
          changed = getBridge().getDocumentFieldAccessor()
              .set(getTranslationDoc().orElse(getDocument()), field, supplier.get());
        }
        return changed;
      }
    };
  }

}
