package com.celements.model.object;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.google.common.collect.FluentIterable;

import one.util.streamex.StreamEx;

/**
 * Fetches objects O on a document D for the defined query. Returned objects are intended for
 * read-only operations. Use {@link ObjectEditor} instead for manipulations.
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
public interface ObjectFetcher<D, O> extends ObjectHandler<D, O> {

  @NotNull
  @Override
  ObjectFetcher<D, O> clone();

  /**
   * @return true if an object to fetch exists
   */
  boolean exists();

  /**
   * @return amount of fetched objects
   */
  int count();

  /**
   * @deprecated instead use {@link #findFirst()}
   * @return the first fetched object
   */
  @NotNull
  @Deprecated
  com.google.common.base.Optional<O> first();

  /**
   * @return the first fetched object
   */
  @NotNull
  Optional<O> findFirst();

  /**
   * @return the first fetched object
   * @throws IllegalArgumentException
   *           if there is no object to fetch
   */
  @NotNull
  O firstAssert();

  /**
   * @return the sole object to fetch
   * @throws IllegalArgumentException
   *           if there is no unique object to fetch
   */
  @NotNull
  O unique();

  /**
   * @return a {@link List} of all fetched objects
   */
  @NotNull
  List<O> list();

  /**
   * @return an {@link Iterable} of all fetched objects
   */
  @NotNull
  FluentIterable<O> iter();

  /**
   * @return streams all fetched objects
   */
  @NotNull
  Stream<O> stream();

  /**
   * @return a {@link Map} of all fetched objects indexed by their {@link ClassIdentity}
   */
  @NotNull
  Map<ClassIdentity, List<O>> map();

  /**
   * Streams one field view for the document, followed by one for each object matching the current
   * query. The document view is included if the query has no class restriction or explicitly
   * includes {@code XWikiDocumentClass}. Each matching entity produces exactly one view; absent
   * fields do not affect stream cardinality.
   *
   * @return document and object field views
  */
  @NotNull
  StreamEx<FieldView> streamFields();

  /**
   * Read-only field view of one document or object entity.
   */
  interface FieldView {

    /**
     * @return the entity's class reference
     */
    @NotNull
    LocalDocumentReference getClassRef();

    /**
     * @return the value denoted by {@code field}, or empty if the field is unsupported or absent
     */
    @NotNull
    <T> Optional<T> get(@NotNull ClassField<T> field);

  }

  /**
   * @deprecated use {@link #streamFields()} and {@link FieldView#get(ClassField)}. Unlike this
   *             method, {@code streamFields()} has entity cardinality and retains views whose
   *             requested field is absent.
   * @param field
   * @return {@link FieldFetcher} which gets values for {@code field} from the queried objects
   */
  @NotNull
  @Deprecated
  <T> FieldFetcher<T> fetchField(@NotNull ClassField<T> field);

  @Deprecated
  interface FieldFetcher<T> {

    /**
     * @deprecated instead use {@link #findFirst()}
     * @return the first field value
     */
    @Deprecated
    @NotNull
    com.google.common.base.Optional<T> first();

    /**
     * @return the first field value
     */
    @NotNull
    Optional<T> findFirst();

    /**
     * @return a {@link List} of all field values
     */
    @NotNull
    List<T> list();

    /**
     * @return a {@link Set} of all field values
     */
    @NotNull
    Set<T> set();

    /**
     * @deprecated instead use {@link #stream()}
     * @return an {@link Iterable} of all not-null field values
     */
    @Deprecated
    @NotNull
    FluentIterable<T> iter();

    /**
     * @deprecated instead use {@link #streamNullable()}
     * @return an {@link Iterable} of all field values, may contain null
     */
    @Deprecated
    @NotNull
    FluentIterable<T> iterNullable();

    /**
     * @return streams all not-null field values
     */
    @NotNull
    Stream<T> stream();

    /**
     * @return streams all field values, may contain null
     */
    @NotNull
    Stream<T> streamNullable();

  }

}
