package com.celements.wiki;

import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.WikiReference;

public interface WikiService {

  boolean hasWiki(@Nullable WikiReference wikiRef);

  @NotNull
  Stream<WikiReference> streamAllWikis();

  @NotNull
  Stream<URL> streamUrlsForWiki(@Nullable WikiReference wikiRef);

  @NotNull
  Optional<WikiReference> getWikiForHost(@Nullable String host);

}
