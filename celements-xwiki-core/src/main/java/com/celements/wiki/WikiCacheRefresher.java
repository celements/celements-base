package com.celements.wiki;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.bridge.event.WikiEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class WikiCacheRefresher implements EventListener,
    ApplicationListener<WikiCacheRefresher.RefreshEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiCacheRefresher.class);

  private final QueryWikiService queryWikiService;

  @Inject
  public WikiCacheRefresher(QueryWikiService queryWikiService) {
    this.queryWikiService = queryWikiService;
  }

  public void refresh() {
    queryWikiService.refresh();
  }

  @Override
  public void onApplicationEvent(RefreshEvent event) {
    LOGGER.trace("onApplicationEvent - {}", event);
    refresh();
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public List<Event> getEvents() {
    return ImmutableList.of(
        new WikiCreatedEvent(),
        new WikiDeletedEvent(),
        new DocumentCreatedEvent(),
        new DocumentUpdatedEvent(),
        new DocumentDeletedEvent());
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    LOGGER.trace("onEvent - '{}', source '{}', data '{}'", event.getClass(), source, data);
    if (((event instanceof AbstractDocumentEvent) && hasServerObj(event, (XWikiDocument) source))
        || (event instanceof WikiEvent)) {
      refresh();
    }
  }

  private boolean hasServerObj(Event event, XWikiDocument doc) {
    return XWikiConstant.MAIN_WIKI.equals(doc.getDocumentReference().getWikiReference())
        && ((event instanceof DocumentDeletedEvent ? doc.getOriginalDocument() : doc)
            .getXObject(XWikiConstant.SERVER_CLASS_DOCREF) != null);
  }

  public class RefreshEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public RefreshEvent(Object source) {
      super(source);
    }

  }

}
