package com.celements.wiki.event;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

public class WikiDeletedEvent extends WikiEvent {

  private static final long serialVersionUID = 6824857470080402067L;

  public WikiDeletedEvent(@NotNull WikiReference wikiRef) {
    super(wikiRef);
  }

}
