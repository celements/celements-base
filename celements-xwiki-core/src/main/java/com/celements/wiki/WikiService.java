package com.celements.wiki;

import java.net.URI;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

public interface WikiService {

  /**
   * @return true if the given wiki is configured in the main wiki
   */
  boolean hasWiki(@Nullable WikiReference wikiRef);

  /**
   * @return a stream of all wikis configured in the main wiki
   */
  @NotNull
  Stream<WikiReference> streamAllWikis();

  /**
   * @return the found wikis configured in the main wiki that match the given URL predicate
   */
  @NotNull
  Stream<WikiReference> findWikis(Predicate<URI> matcher);

  /**
   * @return a stream of URIs configured in the main wiki for the given wiki
   */
  @NotNull
  Stream<URI> streamUrisForWiki(@Nullable WikiReference wikiRef);

  /**
   * tries to determine the wiki for the given URI using multiple steps:
   * 1. always main wiki for localhost or non-virtual
   * 2. read out the wiki config with the given URI host according to {@link #findWikis}
   * 3. try to determine from the subdomain
   *
   * @throws WikiMissingException
   *           if no wiki could be determined
   */
  @NotNull
  WikiReference determineWiki(@NotNull URI uri) throws WikiMissingException;

}
