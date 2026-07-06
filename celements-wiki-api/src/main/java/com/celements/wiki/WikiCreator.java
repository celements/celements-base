package com.celements.wiki;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.exception.WikiCreationException;

public interface WikiCreator {

  void createWiki(@NotNull WikiReference wikiRef) throws WikiCreationException;

  boolean ensureWiki(@NotNull WikiReference wikiRef) throws WikiCreationException;

  @NotNull
  Optional<@NotNull Runnable> ensureWikiDeferred(@NotNull WikiReference wikiRef)
      throws WikiCreationException;

}
