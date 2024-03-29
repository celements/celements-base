package com.celements.store.id;

import java.util.Iterator;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

@ComponentRole
public interface CelementsIdComputer extends DocumentIdComputer {

  /**
   * @return computes the id for the given document and language
   */
  long computeDocumentId(@NotNull DocumentReference docRef, @Nullable String lang);

  /**
   * @return computes the maximum id (regarding collision detection) for the given document and
   *         language
   */
  long computeMaxDocumentId(@NotNull DocumentReference docRef, @Nullable String lang);

  /**
   * @return iterator over all document ids for the given document and language starting from given
   *         startCollisionCount
   */
  @NotNull
  Iterator<Long> getDocumentIdIterator(@NotNull DocumentReference docRef, String lang,
      byte startCollisionCount);

  /**
   * @return computes the id for the given document, language and collision count
   * @throws IdComputationException
   *           if unable to compute an id for the given collision count
   */
  long computeDocumentId(@NotNull DocumentReference docRef, @Nullable String lang,
      byte collisionCount) throws IdComputationException;

  /**
   * @return computes the id for the given document
   */
  long computeDocumentId(@NotNull XWikiDocument doc);

  /**
   * @return computes the next object id for the given document
   * @throws IdComputationException
   *           if unable to compute an id
   */
  long computeNextObjectId(@NotNull XWikiDocument doc) throws IdComputationException;

  public class IdComputationException extends Exception {

    private static final long serialVersionUID = 1L;

    public IdComputationException(String message) {
      super(message);
    }

    public IdComputationException(Throwable cause) {
      super(cause);
    }

    public IdComputationException(String message, Throwable cause) {
      super(message, cause);
    }

  }

}
