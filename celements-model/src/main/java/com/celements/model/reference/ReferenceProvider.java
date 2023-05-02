package com.celements.model.reference;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableSet.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.common.lambda.Try;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWikiConstant;

import one.util.streamex.StreamEx;

@Component
@Lazy
public class ReferenceProvider implements EventListener,
    ApplicationListener<ReferenceProvider.RefreshEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceProvider.class);

  static final String XWQL_WIKI = "select distinct doc.name "
      + "from XWikiDocument as doc, BaseObject as obj "
      + "where doc.space = 'XWiki' "
      + "and doc.name <> 'XWikiServerClassTemplate' "
      + "and obj.name=doc.fullName "
      + "and obj.className='XWiki.XWikiServerClass'";

  private final QueryManager queryManager;
  private final AtomicReference<Try<ImmutableSet<WikiReference>, QueryException>> wikisCache;

  @Inject
  public ReferenceProvider(QueryManager queryManager) {
    this.queryManager = queryManager;
    this.wikisCache = new AtomicReference<>();
  }

  @NotNull
  public Collection<WikiReference> getAllWikis() {
    try {
      return wikisCache.updateAndGet(trying -> (trying != null) && trying.isSuccess()
          ? trying
          : Try.to(this::queryWikis))
          .getOrThrow();
    } catch (QueryException exc) {
      LOGGER.error("getAllWikis - failed", exc);
    }
    return ImmutableSet.of(XWikiConstant.MAIN_WIKI);
  }

  private ImmutableSet<WikiReference> queryWikis() throws QueryException {
    return StreamEx.of(XWikiConstant.MAIN_WIKI)
        .append(queryManager.createQuery(XWQL_WIKI, Query.XWQL)
            .setWiki(XWikiConstant.MAIN_WIKI.getName())
            .<String>execute().stream()
            .map(name -> name.replaceFirst("^XWikiServer", ""))
            .filter(not(String::isEmpty))
            .map(String::toLowerCase)
            .map(WikiReference::new))
        .collect(toImmutableSet()); // keeps order
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

  public void refresh() {
    LOGGER.trace("refresh");
    wikisCache.set(null);
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public List<Event> getEvents() {
    return ImmutableList.of(
        new WikiCreatedEvent(),
        new WikiDeletedEvent());
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    LOGGER.trace("onEvent - '{}', source '{}', data '{}'", event.getClass(), source, data);
    refresh();
  }

  @Override
  public void onApplicationEvent(RefreshEvent event) {
    LOGGER.trace("onApplicationEvent - {}", event);
    refresh();
  }

  public class RefreshEvent extends ApplicationEvent {

    private static final long serialVersionUID = -6948726248642122691L;

    public RefreshEvent(Object source) {
      super(source);
    }

  }

}
