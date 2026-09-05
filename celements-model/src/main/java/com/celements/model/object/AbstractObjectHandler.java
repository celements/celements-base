package com.celements.model.object;

import static com.celements.model.access.IModelAccessFacade.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;

import java.util.Optional;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.FieldRestriction;
import com.celements.model.object.restriction.ObjectQuery;
import com.celements.model.object.restriction.ObjectQueryBuilder;

@NotThreadSafe
public abstract class AbstractObjectHandler<R extends AbstractObjectHandler<R, D, O>, D, O> extends
    ObjectQueryBuilder<R, O> implements ObjectHandler<D, O> {

  private final D doc;
  private D transDoc;

  public AbstractObjectHandler(@NotNull D doc) {
    this.doc = checkNotNull(doc);
  }

  @Override
  public D getDocument() {
    return doc;
  }

  @Override
  public Optional<D> getTranslationDoc() {
    return Optional.ofNullable(transDoc);
  }

  @Override
  public R withTranslation(D transDoc) {
    if (transDoc != null) {
      checkArgument(getBridge().getDocRef(transDoc).equals(getDocRef()));
    }
    this.transDoc = transDoc;
    return getThis();
  }

  @Override
  public DocumentReference getDocRef() {
    return getBridge().getDocRef(getDocument());
  }

  @Override
  public ObjectQuery<O> getQuery() {
    ObjectQuery<O> query = super.getQuery();
    if (getTranslationDoc().isPresent()) {
      query.getObjectClasses().stream()
          .map(this::asObjectLangRestriction)
          .forEach(restriction -> restriction.ifPresent(query::add));
    }
    return query;
  }

  private Optional<FieldRestriction<O, ?>> asObjectLangRestriction(ClassReference classRef) {
    return classRef.getClassDefinition()
        .filter(ClassDefinition::isValidObjectClass)
        .flatMap(ClassDefinition::getLangField)
        .map(langField -> new FieldRestriction<>(getBridge(), langField, getObjectLanguage()));
  }

  private String getObjectLanguage() {
    return getTranslationDoc()
        .map(getBridge()::getLanguage)
        .filter(not(DEFAULT_LANG::equals))
        .orElseGet(() -> getBridge().getDefaultLanguage(getDocument()));
  }

  @Override
  protected abstract @NotNull ObjectBridge<D, O> getBridge();

  @Override
  public abstract AbstractObjectHandler<?, D, O> clone();

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [doc=" + getDocRef() + ", query=" + getQuery() + "]";
  }

}
