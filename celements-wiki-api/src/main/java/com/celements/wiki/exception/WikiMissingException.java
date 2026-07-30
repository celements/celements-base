package com.celements.wiki.exception;

import org.xwiki.model.reference.WikiReference;

public class WikiMissingException extends WikiException {

  private static final long serialVersionUID = 1L;

  public WikiMissingException(WikiReference wikiRef) {
    super(wikiRef);
  }

  public WikiMissingException(WikiReference wikiRef, Throwable cause) {
    super(wikiRef, cause);
  }

}
