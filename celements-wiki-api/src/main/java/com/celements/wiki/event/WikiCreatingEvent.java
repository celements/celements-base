package com.celements.wiki.event;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

public class WikiCreatingEvent extends WikiEvent {

  private static final long serialVersionUID = 5451626440721444998L;

  public WikiCreatingEvent(@NotNull WikiReference wikiRef) {
    super(wikiRef);
  }

}
