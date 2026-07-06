package com.celements.wiki.exception;

import static java.util.Objects.*;

import org.xwiki.model.reference.WikiReference;

public class WikiException extends Exception {

  private static final long serialVersionUID = 1L;

  private final WikiReference wikiRef;

  public WikiException(WikiReference wikiRef) {
    super(requireNonNull(wikiRef).getName());
    this.wikiRef = wikiRef;
  }

  public WikiException(WikiReference wikiRef, Throwable cause) {
    super(requireNonNull(wikiRef).getName(), cause);
    this.wikiRef = wikiRef;
  }

  public WikiReference getWikiRef() {
    return wikiRef;
  }

}
