package com.celements.model.object;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.model.field.FieldAccessor;

import one.util.streamex.StreamEx;

/**
 * Bridge for effective access on document and objects, primarily used by {@link ObjectHandler}s to
 * allow generic implementations
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
@Immutable
@Singleton
@ComponentRole
public interface ObjectBridge<D, O> {

  @NotNull
  Class<D> getDocumentType();

  @NotNull
  Class<O> getObjectType();

  /**
   * @deprecated without replacement since 4.8
   */
  @Deprecated
  void checkDoc(@NotNull D doc);

  @NotNull
  DocumentReference getDocRef(@NotNull D doc);

  @NotNull
  String getLanguage(@NotNull D doc);

  @NotNull
  String getDefaultLanguage(@NotNull D doc);

  @NotNull
  StreamEx<LocalDocumentReference> getDocClasses(@NotNull D doc);

  @NotNull
  StreamEx<O> getObjects(@NotNull D doc, @NotNull LocalDocumentReference classRef);

  int getObjectNumber(@NotNull O obj);

  @NotNull
  LocalDocumentReference getObjectClass(@NotNull O obj);

  @NotNull
  O cloneObject(@NotNull O obj);

  @NotNull
  O createObject(@NotNull D doc, @NotNull LocalDocumentReference classRef);

  boolean deleteObject(@NotNull D doc, @NotNull O obj);

  @NotNull
  FieldAccessor<D> getDocumentFieldAccessor();

  @NotNull
  FieldAccessor<O> getObjectFieldAccessor();

}
