package com.celements.globalredirect;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(GlobalRedirectChangeListener.NAME)
public class GlobalRedirectChangeListener implements EventListener {

  public static final String NAME = "globalRedirectChangeListener";

  private final GlobalRedirectService globalRedirectSrv;

  @Inject
  public GlobalRedirectChangeListener(GlobalRedirectService globalRedirectSrv) {
    this.globalRedirectSrv = globalRedirectSrv;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<Event> getEvents() {
    return List.of(new DocumentUpdatedEvent(), new DocumentCreatedEvent(),
        new DocumentDeletedEvent());
  }

  @Override
  public void onEvent(Event event, Object source, Object data) {
    XWikiDocument sourceDocument = (XWikiDocument) source;
    if ((sourceDocument != null)
        && (sourceDocument.getDocRef() == globalRedirectSrv.getGlobalRedirectDocRef())) {
      globalRedirectSrv.refresh();
    }
  }
}
