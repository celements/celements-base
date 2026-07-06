package com.celements.wiki.event;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

public class WikiCreatedEvent extends WikiEvent {

  private static final long serialVersionUID = 543985436226095919L;

  public WikiCreatedEvent(@NotNull WikiReference wikiRef) {
    super(wikiRef);
  }

}
