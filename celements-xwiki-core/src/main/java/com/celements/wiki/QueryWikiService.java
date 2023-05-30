package com.celements.wiki;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableMap.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.common.lambda.Try;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;
import com.xpn.xwiki.XWikiConstant;

import one.util.streamex.StreamEx;

@Service
public class QueryWikiService implements WikiService,
    EventListener, ApplicationListener<QueryWikiService.RefreshEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryWikiService.class);

  private static final ImmutableList<WikiReference> WIKI_PRIORITY = ImmutableList.of(
      XWikiConstant.MAIN_WIKI, XWikiConstant.CENTRAL_WIKI);
  public static final Comparator<WikiReference> WIKI_MAIN_FIRST_COMPARATOR = Ordering
      .explicit(WIKI_PRIORITY)
      .nullsLast()
      .onResultOf(wiki -> WIKI_PRIORITY.contains(wiki) ? wiki : null);

  private final QueryManager queryManager;
  private final ConfigurationSource cfgSrc;
  private final AtomicReference<Try<ImmutableMap<String, WikiReference>, QueryException>> cache;

  @Inject
  public QueryWikiService(
      QueryManager queryManager,
      @Named("xwikicfg") ConfigurationSource cfgSrc) {
    this.queryManager = queryManager;
    this.cfgSrc = cfgSrc;
    this.cache = new AtomicReference<>();
  }

  @Override
  public boolean hasWiki(WikiReference wikiRef) {
    return XWikiConstant.MAIN_WIKI.equals(wikiRef)
        || getWikisByHost().containsValue(convertMainWiki(wikiRef));
  }

  @Override
  public Stream<WikiReference> streamAllWikis() {
    Collection<WikiReference> wikis = getWikisByHost().values();
    return wikis.isEmpty()
        ? Stream.of(XWikiConstant.MAIN_WIKI)
        : wikis.stream()
            .distinct()
            .sorted(WIKI_MAIN_FIRST_COMPARATOR);
  }

  @Override
  public Optional<WikiReference> getWikiForHost(String host) {
    return Optional.ofNullable(getWikisByHost()
        .get(Strings.nullToEmpty(host).trim()));
  }

  @Override
  public Map<String, WikiReference> getWikisByHost() {
    try {
      return cache.updateAndGet(trying -> (trying != null) && trying.isSuccess()
          ? trying
          : Try.to(this::queryWikisByHost))
          .getOrThrow();
    } catch (QueryException exc) {
      LOGGER.error("getAllWikis - failed", exc);
    }
    return ImmutableMap.of();
  }

  private ImmutableMap<String, WikiReference> queryWikisByHost() throws QueryException {
    return StreamEx.of(queryManager.getNamedQuery("getWikisByHost")
        .setWiki(XWikiConstant.MAIN_WIKI.getName())
        .<Object[]>execute())
        .mapToEntry(
            row -> row[0].toString(),
            row -> row[1].toString().replace("XWikiServer", ""))
        .filterKeys(not(String::isEmpty))
        .filterValues(not(String::isEmpty))
        .distinctKeys()
        .mapValues(WikiReference::new)
        .mapValues(this::convertMainWiki)
        .collect(toImmutableMap(Entry::getKey, Entry::getValue));
  }

  private WikiReference convertMainWiki(WikiReference wikiRef) {
    if ((wikiRef != null) && cfgSrc.getProperty("xwiki.db", "").equals(wikiRef.getName())) {
      return XWikiConstant.MAIN_WIKI;
    }
    return wikiRef;
  }

  public void refresh() {
    LOGGER.trace("refresh");
    cache.set(null);
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

    private static final long serialVersionUID = 1L;

    public RefreshEvent(Object source) {
      super(source);
    }

  }

}
