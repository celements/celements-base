package com.celements.model.access;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Supplier;

import org.xwiki.model.reference.WikiReference;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingSupplier;
import com.celements.model.context.Contextualiser;
import com.celements.model.context.ModelContext;

/**
 * ContextExecutor is used to execute code within an altered {@link ModelContext} and set it back
 * after execution. This behaviour is guaranteed within {@link #call()}, for which an implementation
 * has to be provided when subclassing or instantiating.
 *
 * @deprecated instead use {@link Contextualiser}
 *
 * @param <T>
 *          return parameter of {@link #call()} and {@link #execute()}
 * @param <E>
 *          subclass of {@link Exception} thrown by {@link #call()} and {@link #execute()}
 * @author Marc Sladek
 */
@Deprecated
public abstract class ContextExecutor<T, E extends Exception> {

  private WikiReference wikiRef;

  public WikiReference getWikiRef() {
    return wikiRef;
  }

  public ContextExecutor<T, E> inWiki(WikiReference wiki) {
    this.wikiRef = wiki;
    return this;
  }

  public T execute() throws E {
    checkState(wikiRef != null, "No wiki set for ContextExecutor");
    return new Contextualiser()
        .withWiki(wikiRef)
        .execute(rethrow(this::call));
  }

  protected abstract T call() throws E;

  /*
   * @deprecated instead use {@link Contextualiser}
   */
  @Deprecated
  public static <T> T executeInWiki(WikiReference wikiRef, Supplier<T> supplier) {
    return executeInWikiThrows(wikiRef, supplier::get);
  }

  /*
   * @deprecated instead use {@link Contextualiser}
   */
  @Deprecated
  public static <T, E extends Exception> T executeInWikiThrows(WikiReference wikiRef,
      ThrowingSupplier<T, E> supplier) throws E {
    return new ContextExecutor<T, E>() {

      @Override
      protected T call() throws E {
        return supplier.get();
      }
    }.inWiki(wikiRef).execute();
  }
}
