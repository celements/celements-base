package com.celements.wiki;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableSetMultimap.*;

import java.net.URI;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.celements.common.MoreOptional;
import com.celements.common.lambda.Try;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

@Service
public class QueryWikiService implements WikiService {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryWikiService.class);

  private static final ImmutableSet<String> LOCAL_HOSTS = ImmutableSet.of(
      "localhost", "127.0.0.1", "::1");
  private static final ImmutableList<WikiReference> WIKI_PRIORITY = ImmutableList.of(
      XWikiConstant.MAIN_WIKI, XWikiConstant.CENTRAL_WIKI);
  public static final Comparator<WikiReference> WIKI_MAIN_FIRST_COMPARATOR = Ordering
      .explicit(WIKI_PRIORITY)
      .nullsLast()
      .onResultOf(wiki -> WIKI_PRIORITY.contains(wiki) ? wiki : null);

  private final QueryManager queryManager;
  private final XWikiConfigSource xwikiCfg;
  private final AtomicReference<Try<ImmutableMultimap<WikiReference, URI>, QueryException>> cache;

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
  public Stream<WikiReference> findWikis(Predicate<URI> matcher) {
    return EntryStream.of(getWikiMap().entries().stream())
        .filterValues(matcher)
        .keys();
  }

  @Override
  public Stream<URI> streamUrisForWiki(@Nullable WikiReference wikiRef) {
    return getWikiMap().get(convertMainWiki(wikiRef)).stream();
  }

  @Override
  public WikiReference determineWiki(URI url) throws WikiMissingException {
    String host = Strings.nullToEmpty(url.getHost());
    checkArgument(!host.isEmpty());
    if (!xwikiCfg.isVirtualMode() || LOCAL_HOSTS.contains(host)) {
      return XWikiConstant.MAIN_WIKI;
    }
    WikiReference wikiRef = findWikis(u -> u.getHost().equals(host)).findFirst()
        // no wiki found based on the full host name, try to use the first part as the wiki name
        .or(() -> getWikiFromDomain(host))
        .orElseThrow(() -> new WikiMissingException("The wiki " + host + " does not exist"));
    LOGGER.debug("determineWiki - [{}] for [{}]", wikiRef.getName(), url);
    return wikiRef;
  }

  private Optional<WikiReference> getWikiFromDomain(String host) {
    return Optional.of(host).filter(h -> h.indexOf(".") > 0)
        .map(h -> new WikiReference(h.substring(0, h.indexOf("."))))
        .filter(log(this::hasWiki)
            .warn(LOGGER).msg("using wiki domain fallback"));
  }

  Multimap<WikiReference, URI> getWikiMap() {
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

  private ImmutableMultimap<WikiReference, URI> queryAllWikis() throws QueryException {
    ImmutableMultimap<WikiReference, URI> ret = StreamEx.of(queryManager
        .getNamedQuery("getAllWikis")
        .setWiki(XWikiConstant.MAIN_WIKI.getName())
        .<Object[]>execute())
        .mapToEntry(
            row -> row[0].toString(), // doc.name
            row -> toUri(
                (Integer) row[1], // secure.value
                row[2].toString())) // host.value
        .flatMapKeys(this::toWikiRef)
        .flatMapValues(MoreOptional::stream)
        .collect(toImmutableSetMultimap(Entry::getKey, Entry::getValue));
    LOGGER.info("queryAllWikis - {}", ret);
    return ret;
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

  private Optional<URI> toUri(Integer secure, String host) {
    try {
      return Optional.of(UriComponentsBuilder.newInstance()
          .scheme(Optional.ofNullable(secure)
              .map(i -> (i > 0) ? "https" : "http")
              .orElseGet(() -> xwikiCfg.getProperty("xwiki.url.protocol", "http")))
          .host(StreamEx.ofReversed(host.split("://")).findFirst().orElse(host))
          .port(Integer.parseInt(xwikiCfg.getProperty("xwiki.url.port", "-1")))
          .build().toUri());
    } catch (IllegalArgumentException | IllegalStateException exc) {
      LOGGER.warn("toUri - failed for [{}]", host, exc);
      return Optional.empty();
    }
  }

  public void refresh() {
    LOGGER.info("refresh");
    cache.set(null);
  }

}
