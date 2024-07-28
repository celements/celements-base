package com.celements.wiki;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class WikiCacheRefresher implements ApplicationListener<WikiCacheRefresher.RefreshEvent> {

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

  public static class RefreshEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    public RefreshEvent(Object source) {
      super(source);
    }

  }

}
