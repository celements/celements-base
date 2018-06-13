package com.celements.model.object;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.model.classes.ClassIdentity;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

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
  public boolean exists() {
    return count() > 0;
  }

  @Override
  public int count() {
    return iter().size();
  }

  @Override
  public Optional<O> first() {
    return iter().first();
  }

  @Override
  public List<O> list() {
    return iter().toList();
  }

  @Override
  public FluentIterable<O> iter() {
    return FluentIterable.from(Iterables.concat(map().values()));
  }

  @Override
  public Map<ClassIdentity, List<O>> map() {
    ImmutableMap.Builder<ClassIdentity, List<O>> builder = ImmutableMap.builder();
    for (ClassIdentity classId : getObjectClasses()) {
      builder.put(classId, getObjects(classId).toList());
    }
    return builder.build();
  }

  private Set<ClassIdentity> getObjectClasses() {
    Set<ClassIdentity> classes = getQuery().getObjectClasses();
    if (classes.isEmpty()) {
      classes = ImmutableSet.copyOf(getBridge().getDocClasses(getDocument()));
    }
    return classes;
  }

  private FluentIterable<O> getObjects(ClassIdentity classId) {
    FluentIterable<O> objIter = FluentIterable.from(getBridge().getObjects(getDocument(), classId));
    objIter = objIter.filter(Predicates.and(getQuery().getRestrictions(classId)));
    if (clone) {
      LOGGER.debug("{} clone objects", this);
      objIter = objIter.transform(new ObjectCloner());
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("{} fetched for {}: {}", this, classId, objIter);
    } else {
      LOGGER.info("{} fetched for {} {} objects", this, classId, objIter.size());
    }
    return objIter;
  }

  /**
   * disables cloning for the fetcher. use with caution!
   */
  protected R disableCloning() {
    clone = false;
    return getThis();
  }

  private class ObjectCloner implements Function<O, O> {

    @Override
    public O apply(O obj) {
      return getBridge().cloneObject(obj);
    }
  }

}
