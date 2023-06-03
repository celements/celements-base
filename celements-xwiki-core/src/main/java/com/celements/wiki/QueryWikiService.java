package com.celements.wiki;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableSetMultimap.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.common.lambda.Try;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;

import one.util.streamex.EntryStream;
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
  private final XWikiConfigSource xwikiCfg;
  private final AtomicReference<Try<ImmutableMultimap<WikiReference, URL>, QueryException>> cache;

  @Inject
  public QueryWikiService(
      QueryManager queryManager,
      XWikiConfigSource xwikiCfg) {
    this.queryManager = queryManager;
    this.xwikiCfg = xwikiCfg;
    this.cache = new AtomicReference<>();
  }

  @Override
  public boolean hasWiki(WikiReference wikiRef) {
    return XWikiConstant.MAIN_WIKI.equals(wikiRef)
        || getWikiMap().containsKey(convertMainWiki(wikiRef));
  }

  @Override
  public Stream<WikiReference> streamAllWikis() {
    Set<WikiReference> wikis = getWikiMap().keySet();
    return wikis.isEmpty()
        ? Stream.of(XWikiConstant.MAIN_WIKI)
        : wikis.stream()
            .sorted(WIKI_MAIN_FIRST_COMPARATOR);
  }

  @Override
  public Stream<URL> streamUrlsForWiki(@Nullable WikiReference wikiRef) {
    return getWikiMap().get(convertMainWiki(wikiRef)).stream();
  }

  @Override
  public Optional<WikiReference> getWikiForHost(String host) {
    return EntryStream.of(getWikiMap().entries().stream())
        .filterValues(url -> url.getHost().equals(host))
        .keys()
        .findFirst();
  }

  Multimap<WikiReference, URL> getWikiMap() {
    try {
      return cache.updateAndGet(trying -> (trying != null) && trying.isSuccess()
          ? trying
          : Try.to(this::queryAllWikis))
          .getOrThrow();
    } catch (QueryException exc) {
      LOGGER.error("getAllWikis - failed", exc);
    }
    return ImmutableMultimap.of();
  }

  private ImmutableMultimap<WikiReference, URL> queryAllWikis() throws QueryException {
    return StreamEx.of(queryManager.getNamedQuery("getAllWikis")
        .setWiki(XWikiConstant.MAIN_WIKI.getName())
        .<Object[]>execute())
        .mapToEntry(
            row -> row[0].toString(), // doc.name
            row -> row[1].toString()) // host.value
        .flatMapKeys(this::toWikiRef)
        .flatMapValues(this::toURL)
        .collect(toImmutableSetMultimap(Entry::getKey, Entry::getValue));
  }

  private Stream<WikiReference> toWikiRef(String name) {
    return Stream.of(name.replace("XWikiServer", "").trim())
        .filter(not(String::isEmpty))
        .map(WikiReference::new)
        .map(this::convertMainWiki);
  }

  private WikiReference convertMainWiki(WikiReference wikiRef) {
    if ((wikiRef != null) && wikiRef.getName().equals(xwikiCfg.getProperty("xwiki.db"))) {
      return XWikiConstant.MAIN_WIKI;
    }
    return wikiRef;
  }

  private Stream<URL> toURL(String host) {
    try {
      return Stream.of(UriComponentsBuilder.newInstance()
          .scheme(xwikiCfg.getProperty("xwiki.url.protocol", "http"))
          .host(StreamEx.ofReversed(host.split("://")).findFirst().orElse(host))
          .port(Integer.parseInt(xwikiCfg.getProperty("xwiki.url.port", "-1")))
          .build().toUri().toURL());
    } catch (MalformedURLException e) {
      return Stream.empty();
    }
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
