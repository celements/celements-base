package com.celements.model.object;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.ImmutableList.*;
import static com.google.common.collect.ImmutableSet.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessor;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

@NotThreadSafe
public abstract class AbstractObjectFetcher<R extends AbstractObjectFetcher<R, D, O>, D, O> extends
    AbstractObjectHandler<R, D, O> implements ObjectFetcher<D, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFetcher.class);

  private boolean clone;

  protected AbstractObjectFetcher(@NotNull D doc) {
    super(doc);
    this.clone = true;
  }

  @Override
  public abstract AbstractObjectFetcher<?, D, O> clone();

  @Override
  public boolean exists() {
    return count() > 0;
  }

  @Override
  public int count() {
    long count = stream().count();
    checkArgument(count <= Integer.MAX_VALUE, count);
    return (int) count;
  }

  @Override
  @Deprecated
  public com.google.common.base.Optional<O> first() {
    return com.google.common.base.Optional.fromJavaUtil(stream().findFirst());
  }

  @Override
  public O firstAssert() {
    Optional<O> ret = stream().findFirst();
    checkArgument(ret.isPresent(), "empty - %s", this);
    return ret.get();
  }

  @Override
  public O unique() {
    Iterator<O> iter = stream().iterator();
    checkArgument(iter.hasNext(), "empty - %s", this);
    O ret = iter.next();
    checkArgument(!iter.hasNext(), "non unique - %s", this);
    return ret;
  }

  @Override
  public List<O> list() {
    return stream().collect(toImmutableList());
  }

  @Override
  public FluentIterable<O> iter() {
    return FluentIterable.from(stream()::iterator);
  }

  @Override
  public Stream<O> stream() {
    return getObjectClasses().stream().flatMap(this::getObjects);
  }

  @Override
  public Map<ClassIdentity, List<O>> map() {
    ImmutableMap.Builder<ClassIdentity, List<O>> builder = ImmutableMap.builder();
    for (ClassIdentity classId : getObjectClasses()) {
      builder.put(classId, getObjects(classId).collect(toImmutableList()));
    }
    return builder.build();
  }

  protected Set<? extends ClassIdentity> getObjectClasses() {
    Set<? extends ClassIdentity> classes = getQuery().getObjectClasses();
    if (classes.isEmpty()) {
      classes = getBridge().getDocClasses(getDocument()).toSet();
    }
    return classes;
  }

  protected Stream<O> getObjects(ClassIdentity classId) {
    Stream<O> objects = getBridge().getObjects(getDocument(), classId).stream()
        .filter(getQuery().predicate(classId));
    if (clone) {
      LOGGER.debug("{} clone objects", this);
      objects = objects.map(getBridge()::cloneObject);
    }
    LOGGER.info("{} fetching for {}", this, classId);
    return objects
        .peek(o -> LOGGER.trace("fetched: {}", o));
  }

  /**
   * disables cloning for the fetcher. use with caution!
   */
  protected R disableCloning() {
    clone = false;
    return getThis();
  }

  @Override
  public <T> FieldFetcher<T> fetchField(final ClassField<T> field) {
    final AbstractObjectFetcher<?, D, O> fetcher = clone().filter(field.getClassDef());
    return new FieldFetcher<T>() {

      @Override
      @Deprecated
      public com.google.common.base.Optional<T> first() {
        return com.google.common.base.Optional.fromJavaUtil(stream().findFirst());
      }

      @Override
      public List<T> list() {
        return stream().collect(toImmutableList());
      }

      @Override
      public Set<T> set() {
        return stream().collect(toImmutableSet());
      }

      @Override
      @Deprecated
      public FluentIterable<T> iter() {
        return FluentIterable.from(stream()::iterator);
      }

      @Override
      @Deprecated
      public FluentIterable<T> iterNullable() {
        return FluentIterable.from(streamNullable()::iterator);
      }

      @Override
      public @NotNull Stream<T> stream() {
        return streamNullable().filter(Objects::nonNull);
      }

      @Override
      public @NotNull Stream<T> streamNullable() {
        Stream<T> stream;
        if (field.getClassDef().isValidObjectClass()) {
          FieldAccessor<O> accessor = getBridge().getObjectFieldAccessor();
          stream = fetcher.stream().map(obj -> accessor.get(obj, field).orElse(null));
        } else {
          FieldAccessor<D> accessor = getBridge().getDocumentFieldAccessor();
          stream = accessor.get(getTranslationDoc().orElse(getDocument()), field)
              .map(Stream::of).orElseGet(Stream::empty);
        }
        return stream;
      }
    };
  }

}
