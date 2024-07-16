package com.celements.wiki;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.celements.wiki.WikiCacheRefresher.RefreshEvent;
import com.celements.wiki.classes.XWikiServerClass;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listens on changes of wiki config docs and triggers a refresh of the wiki cache.
 */
@Component
public class WikiConfigChangeListener implements EventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiConfigChangeListener.class);

  private final ModelUtils modelUtils;
  private final ApplicationEventPublisher eventPublisher;

  @Inject
  public WikiConfigChangeListener(
      ModelUtils modelUtils,
      ApplicationEventPublisher eventPublisher) {
    this.modelUtils = modelUtils;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public List<Event> getEvents() {
    return List.of(
        new DocumentCreatedEvent(),
        new DocumentUpdatedEvent(),
        new DocumentDeletedEvent());
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    LOGGER.trace("onEvent - '{}', source '{}', data '{}'", event.getClass(), source, data);
    if (isWikiConfigDocEvent(event, (XWikiDocument) source)) {
      LOGGER.info("onEvent - refreshing for [{}]", event);
      eventPublisher.publishEvent(new RefreshEvent(this));
    }
  }

  private boolean isWikiConfigDocEvent(Event event, XWikiDocument doc) {
    return ((event instanceof AbstractDocumentEvent)
        && modelUtils.isMainWiki(doc.getDocumentReference().getWikiReference())
        && hasServerObj(event, doc));
  }

  private boolean hasServerObj(Event event, XWikiDocument doc) {
    return XWikiObjectFetcher
        .on((event instanceof DocumentDeletedEvent ? doc.getOriginalDocument() : doc))
        .filter(XWikiServerClass.CLASS_REF)
        .exists();
  }

}
