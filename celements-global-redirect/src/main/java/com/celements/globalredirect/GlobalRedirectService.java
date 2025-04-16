package com.celements.globalredirect;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;

import com.celements.execution.XWikiExecutionProp;
import com.celements.init.XWikiProvider;
import com.celements.model.reference.RefBuilder;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class GlobalRedirectService {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalRedirectService.class);

  private final AtomicReference<List<GlobalRedirect>> globalRedirectList = new AtomicReference<>();

  private final XWikiProvider xwikiProvider;
  private final XWikiConfigSource configSource;
  private final Execution execution;

  @Inject
  public GlobalRedirectService(XWikiProvider xwikiProvider, XWikiConfigSource configSource,
      Execution execution) {
    this.xwikiProvider = xwikiProvider;
    this.configSource = configSource;
    this.execution = execution;
  }

  public boolean isActivated() {
    return ("1".equals(configSource.getProperty("xwiki.preferences.redirect")));
  }

  public DocumentReference getGlobalRedirectDocRef() {
    return RefBuilder.create().wiki("xwiki").doc("XWiki").doc("XWikiPreferences")
        .build(DocumentReference.class);
  }

  private Optional<List<GlobalRedirect>> computeGlobalRedirectList() {
    try {
      Optional<XWiki> xwikiOpt = xwikiProvider.get();
      if (xwikiOpt.isPresent()) {
        XWikiDocument globalPreferences = xwikiOpt.get()
            .getDocument(getGlobalRedirectDocRef(), getXWikiContext());
        DocumentReference globalRedirectClassRef = new DocumentReference("xwiki", "XWiki",
            "GlobalRedirect");
        List<BaseObject> redirects = globalPreferences.getXObjects(globalRedirectClassRef);
        if (redirects != null) {
          return Optional.of(redirects.stream().filter(o -> o != null)
              .map(redir -> new GlobalRedirect(redir.getStringValue("pattern"),
                  redir.getStringValue("destination")))
              .filter(GlobalRedirect::isValid)
              .collect(Collectors.toUnmodifiableList()));
        }
        return Optional.of(List.of());
      }
    } catch (XWikiException exp) {
      LOGGER.info("failed to compute global redirect cache");
    }
    return Optional.empty();
  }

  public List<GlobalRedirect> getGlobalRedirect() {
    List<GlobalRedirect> currentList = globalRedirectList.get();
    if (currentList == null) {
      currentList = computeGlobalRedirectList().orElse(null);
      globalRedirectList.compareAndSet(null, currentList);
    }
    return currentList != null ? currentList : List.of();
  }

  /**
   * Get XWiki context from execution context.
   *
   * @return the XWiki context for the current thread
   */
  private XWikiContext getXWikiContext() {
    return execution.getContext()
        .get(XWikiExecutionProp.XWIKI_CONTEXT).orElseThrow();
  }

  public void refresh() {
    globalRedirectList.set(null);
  }

}
