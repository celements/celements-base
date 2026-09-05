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
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import one.util.streamex.StreamEx;

@NotThreadSafe
public abstract class AbstractObjectFetcher<R extends AbstractObjectFetcher<R, D, O>, D, O> extends
    AbstractObjectHandler<R, D, O> implements ObjectFetcher<D, O> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ObjectFetcher.class);

  protected static final DocumentReference EMPTY_DOC_REF = new DocumentReference("$", "$", "$");

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
    return com.google.common.base.Optional.fromJavaUtil(findFirst());
  }

  @Override
  public Optional<O> findFirst() {
    return stream().findFirst();
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
    return streamClassRefs().flatMap(this::getObjects);
  }

  @Override
  public Map<ClassIdentity, List<O>> map() {
    ImmutableMap.Builder<ClassIdentity, List<O>> builder = ImmutableMap.builder();
    for (ClassReference classRef : streamClassRefs().map(ClassReference::new)) {
      builder.put(classRef, getObjects(classRef).collect(toImmutableList()));
    }
    return builder.build();
  }

  protected StreamEx<LocalDocumentReference> streamClassRefs() {
    Supplier<Stream<LocalDocumentReference>> getDocClasses =
        () -> getBridge().getDocClasses(getDocument());
    return getQuery().streamClassRefs()
        .ifEmpty(StreamEx.of(getDocClasses).flatMap(Supplier::get));
  }

  protected Stream<O> getObjects(LocalDocumentReference classRef) {
    Stream<O> objects = getBridge().getObjects(getDocument(), classRef)
        .filter(getQuery().predicate(classRef));
    if (clone) {
      LOGGER.debug("{} clone objects", this);
      objects = objects.map(getBridge()::cloneObject);
    }
    LOGGER.info("{} fetching for {}", this, classRef);
    return objects.peek(o -> LOGGER.trace("fetched: {}", o));
  }

  /**
   * disables cloning for the fetcher. use with caution!
   */
  protected R disableCloning() {
    clone = false;
    return getThis();
  }

  @Override
  @Deprecated
  public <T> FieldFetcher<T> fetchField(final ClassField<T> field) {
    final AbstractObjectFetcher<?, D, O> fetcher = clone().filter(field.getClassReference());
    return new FieldFetcher<T>() {

      @Override
      @Deprecated
      public com.google.common.base.Optional<T> first() {
        return com.google.common.base.Optional.fromJavaUtil(findFirst());
      }

      @Override
      public Optional<T> findFirst() {
        return stream().findFirst();
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
        if (field.getClassReference().isValidObjectClass()) {
          return fetcher.stream()
              .map(ObjectFieldView::new)
              .map(x -> x.get(field).orElse(null));
        } else {
          return new DocumentFieldView().get(field).stream();
        }
      }
    };
  }

  @Override
  public StreamEx<FieldView> streamFields() {
    StreamEx<FieldView> fields = StreamEx.of(stream()).map(ObjectFieldView::new);
    var classRefs = getQuery().streamClassRefs().toImmutableSet();
    if (classRefs.isEmpty() || classRefs.contains(XWikiDocumentClass.CLASS_REF)) {
      fields = fields.prepend(new DocumentFieldView());
    }
    return fields;
  }

  private final class ObjectFieldView implements FieldView {

    private final O object;

    private ObjectFieldView(O object) {
      this.object = object;
    }

    @Override
    public LocalDocumentReference getClassRef() {
      return getBridge().getObjectClass(object);
    }

    @Override
    public <T> Optional<T> get(ClassField<T> field) {
      if (XWikiDocumentClass.CLASS_REF.equals(field.getClassReference())) {
        return Optional.empty();
      }
      if (field.getClassReference().isValidObjectClass()
          && !getBridge().getObjectClass(object).equals(field.getClassReference())) {
        return Optional.empty();
      }
      return getBridge().getObjectFieldAccessor().get(object, field);
    }
  }

  private final class DocumentFieldView implements FieldView {

    @Override
    public LocalDocumentReference getClassRef() {
      return XWikiDocumentClass.CLASS_REF;
    }

    @Override
    public <T> Optional<T> get(ClassField<T> field) {
      if (!XWikiDocumentClass.CLASS_REF.equals(field.getClassReference())) {
        return Optional.empty();
      }
      return StreamEx.of(getTranslationDoc())
          .append(getDocument())
          .mapPartial(doc -> getBridge().getDocumentFieldAccessor().get(doc, field))
          .findFirst();
    }
  }

}
