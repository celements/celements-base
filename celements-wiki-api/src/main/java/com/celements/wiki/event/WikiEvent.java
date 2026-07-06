package com.celements.wiki.event;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.context.ApplicationEvent;
import org.xwiki.model.reference.WikiReference;

public class WikiEvent extends ApplicationEvent {

  private static final long serialVersionUID = -320825853038420795L;

  private final WikiReference wikiRef;

  public WikiEvent(@NotNull WikiReference wikiRef) {
    super(wikiRef);
    checkNotNull(wikiRef);
    this.wikiRef = wikiRef;
  }

  @NotEmpty
  public WikiReference getWiki() {
    return wikiRef;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [wiki=" + wikiRef + "]";
  }

}
