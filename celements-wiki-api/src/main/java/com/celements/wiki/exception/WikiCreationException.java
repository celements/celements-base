package com.celements.wiki.exception;

import org.xwiki.model.reference.WikiReference;

public class WikiCreationException extends WikiException {

  private static final long serialVersionUID = 1L;

  public WikiCreationException(WikiReference wikiRef) {
    super(wikiRef);
  }

  public WikiCreationException(WikiReference wikiRef, Throwable cause) {
    super(wikiRef, cause);
  }

}
