package com.celements.wiki.exception;

import org.xwiki.model.reference.WikiReference;

public class WikiExistsException extends WikiCreationException {

  private static final long serialVersionUID = 1L;

  public WikiExistsException(WikiReference wikiRef) {
    super(wikiRef);
  }

}
