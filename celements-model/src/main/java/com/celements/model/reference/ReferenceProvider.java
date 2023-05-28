package com.celements.model.reference;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableSet.*;

import java.util.Collection;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.wiki.WikiProvider;
import com.google.common.collect.ImmutableSet;

@Component
public class ReferenceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceProvider.class);

  private final QueryManager queryManager;
  private final WikiProvider wikiProvider;

  @Inject
  public ReferenceProvider(QueryManager queryManager, WikiProvider wikiProvider) {
    this.queryManager = queryManager;
    this.wikiProvider = wikiProvider;
  }

  @NotNull
  public Collection<WikiReference> getAllWikis() {
    return wikiProvider.getAllWikis();
  }

  @NotNull
  public Collection<SpaceReference> getAllSpaces(WikiReference wikiRef) {
    try {
      return querySpaces(wikiRef);
    } catch (QueryException exc) {
      LOGGER.error("getAllSpaces - failed for [{}]", wikiRef, exc);
      return ImmutableSet.of();
    }
  }

  private Collection<SpaceReference> querySpaces(WikiReference wikiRef) throws QueryException {
    RefBuilder builder = RefBuilder.from(wikiRef);
    return queryManager.getNamedQuery("getSpaces")
        .setWiki(wikiRef.getName())
        .<String>execute().stream()
        .filter(not(String::isEmpty))
        .map(name -> builder.space(name).build(SpaceReference.class))
        .collect(toImmutableSet()); // keeps order
  }

  @NotNull
  public Collection<DocumentReference> getAllDocsForSpace(SpaceReference spaceRef) {
    try {
      return queryDocs(spaceRef);
    } catch (QueryException exc) {
      LOGGER.error("getAllDocsForSpace - failed for [{}]", spaceRef, exc);
      return ImmutableSet.of();
    }
  }

  private Collection<DocumentReference> queryDocs(SpaceReference spaceRef) throws QueryException {
    RefBuilder builder = RefBuilder.from(spaceRef);
    return queryManager.getNamedQuery("getSpaceDocsName")
        .setWiki(spaceRef.getParent().getName())
        .bindValue("space", spaceRef.getName())
        .<String>execute().stream()
        .filter(not(String::isEmpty))
        .map(name -> builder.doc(name).build(DocumentReference.class))
        .collect(toImmutableSet()); // keeps order
  }

}
