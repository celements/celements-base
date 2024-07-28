package com.celements.wiki;

import org.xwiki.model.reference.WikiReference;

public class WikiMissingException extends Exception {

  private static final long serialVersionUID = 1L;

  public WikiMissingException(String msg) {
    super(msg);
  }

  public WikiMissingException(WikiReference wikiRef, Throwable cause) {
    super(wikiRef.getName(), cause);
  }

}
