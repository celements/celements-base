package com.celements.wiki;

import java.net.URI;

import org.xwiki.model.reference.WikiReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WikiDescriptor(
    WikiReference wiki,
    String prettyName,
    String server,
    Visibility visibility,
    State state,
    String language,
    Boolean secure,
    Boolean oicd,
    URI uri) {

  @JsonIgnore
  public WikiReference wiki() {
    return wiki;
  }

  @JsonProperty("wiki")
  public String wikiName() {
    return (wiki != null) ? wiki.getName() : null;
  }

  public enum Visibility {
    PUBLIC, PRIVATE
  }

  public enum State {
    ACTIVE, INACTIVE, LOCKED
  }

}
