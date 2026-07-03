package com.celements.init.wiki;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.user.api.XWikiRightService.*;

import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.update.WikiUpdater;
import com.celements.wiki.event.WikiCreatedEvent;
import com.celements.wiki.event.WikiCreatingEvent;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class WikiCreationCoordinator {

  private final ApplicationEventPublisher eventPublisher;
  private final Execution execution;
  private final WikiUpdater wikiUpdater;

  @Inject
  public WikiCreationCoordinator(
      ApplicationEventPublisher eventPublisher,
      Execution execution,
      WikiUpdater wikiUpdater) {
    this.eventPublisher = eventPublisher;
    this.execution = execution;
    this.wikiUpdater = wikiUpdater;
  }

  public void publishCreated(WikiReference wikiRef) {
    var ectx = execution.getContext();
    WikiReference wikiPrev = ectx.set(WIKI, wikiRef);
    try {
      publishWikiCreatingEvent(wikiRef);
      eventPublisher.publishEvent(new WikiCreatedEvent(wikiRef));
    } finally {
      ectx.set(WIKI, wikiPrev);
    }
  }

  void publishWikiCreatingEvent(WikiReference wikiRef) {
    var ectx = execution.getContext();
    XWikiUser userPrev = ectx.set(XWIKI_USER, new XWikiUser(SUPERADMIN_FQN, true));
    try {
      eventPublisher.publishEvent(new WikiCreatingEvent(wikiRef));
      wikiUpdater.getFuture(wikiRef).ifPresent(CompletableFuture::join);
    } finally {
      ectx.set(XWIKI_USER, userPrev);
    }
  }

}
