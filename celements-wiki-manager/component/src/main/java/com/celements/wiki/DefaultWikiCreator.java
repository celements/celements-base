package com.celements.wiki;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.xpn.xwiki.user.api.XWikiRightService.*;
import static java.util.Objects.*;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.init.XWikiProvider;
import com.celements.init.update.WikiUpdater;
import com.celements.wiki.event.WikiCreatedEvent;
import com.celements.wiki.event.WikiCreatingEvent;
import com.celements.wiki.exception.WikiCreationException;
import com.celements.wiki.exception.WikiExistsException;
import com.celements.wiki.exception.WikiMissingException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class DefaultWikiCreator implements WikiCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultWikiCreator.class);

  private final XWikiStoreInterface hibStore;
  private final XWikiProvider xwikiProvider;
  private final WikiUpdater wikiUpdater;
  private final ApplicationEventPublisher eventPublisher;
  private final Execution execution;

  @Inject
  public DefaultWikiCreator(
      @Named("hibernate") XWikiStoreInterface hibStore,
      XWikiProvider xwikiProvider,
      WikiUpdater wikiUpdater,
      ApplicationEventPublisher eventPublisher,
      Execution execution) {
    this.hibStore = hibStore;
    this.xwikiProvider = xwikiProvider;
    this.wikiUpdater = wikiUpdater;
    this.eventPublisher = eventPublisher;
    this.execution = execution;
  }

  private XWikiStoreInterface getStore() {
    return xwikiProvider.get().map(XWiki::getStore).orElse(hibStore);
  }

  @Override
  public void createWiki(WikiReference wikiRef) throws WikiCreationException {
    ensureWikiDeferred(wikiRef)
        .orElseThrow(() -> new WikiExistsException(wikiRef))
        .run();
  }

  @Override
  public boolean ensureWiki(WikiReference wikiRef) throws WikiCreationException {
    var postAction = ensureWikiDeferred(wikiRef);
    postAction.ifPresent(Runnable::run);
    return postAction.isPresent();
  }

  @Override
  public Optional<Runnable> ensureWikiDeferred(WikiReference wikiRef) throws WikiCreationException {
    requireNonNull(wikiRef);
    createWikiIfMissing(wikiRef);
    if (initWiki(wikiRef)) {
      return Optional.of(() -> notifyWikiCreation(wikiRef));
    }
    return Optional.empty();
  }

  private void createWikiIfMissing(WikiReference wikiRef) throws WikiCreationException {
    try {
      var store = getStore();
      if (!store.existsWiki(wikiRef)) {
        store.createWiki(wikiRef);
      } else {
        LOGGER.debug("skipped wiki creation [{}], already exists", wikiRef);
      }
    } catch (XWikiException | HibernateException e) {
      throw new WikiCreationException(wikiRef, e);
    }
  }

  private boolean initWiki(WikiReference wikiRef) throws WikiCreationException {
    try {
      var store = getStore();
      var empty = store.isWikiEmpty(wikiRef);
      if (empty) {
        store.initWiki(wikiRef);
        return true;
      }
      return false;
    } catch (WikiMissingException | XWikiException | HibernateException e) {
      throw new WikiCreationException(wikiRef, e);
    }
  }

  // listeners create wiki descriptor, classes, mandatory documents, ...
  private void notifyWikiCreation(WikiReference wikiRef) {
    var ectx = execution.getContext();
    WikiReference wikiPrev = ectx.set(WIKI, wikiRef);
    try {
      notifyWikiCreatingEvent(wikiRef);
      eventPublisher.publishEvent(new WikiCreatedEvent(wikiRef));
    } finally {
      ectx.set(WIKI, wikiPrev);
    }
    LOGGER.info("created wiki [{}]", wikiRef);
  }

  void notifyWikiCreatingEvent(WikiReference wikiRef) {
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
