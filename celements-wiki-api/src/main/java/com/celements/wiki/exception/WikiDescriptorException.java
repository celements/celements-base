package com.celements.wiki.exception;

import org.xwiki.model.reference.WikiReference;

public class WikiDescriptorException extends WikiException {

  private static final long serialVersionUID = 1L;

  public WikiDescriptorException(WikiReference wikiRef, Throwable cause) {
    super(wikiRef, cause);
  }

}
