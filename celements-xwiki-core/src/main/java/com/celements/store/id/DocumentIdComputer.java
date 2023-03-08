package com.celements.store.id;

import java.util.Iterator;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

@ComponentRole
public interface DocumentIdComputer {

  @NotNull
  IdVersion getIdVersion();

  /**
   * @return computes the document id for the given document and language
   */
  long compute(@NotNull DocumentReference docRef, String lang);

  /**
   * @return iterator over all document ids for the given document and language.
   *         implementations may generate multiple possible ids.
   */
  @NotNull
  Iterator<Long> getDocumentIdIterator(@NotNull DocumentReference docRef, String lang);

}
