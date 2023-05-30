package com.celements.wiki;

import java.util.Map;
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
  Optional<WikiReference> getWikiForHost(@Nullable String host);

  @NotNull
  Map<String, WikiReference> getWikisByHost();

}
