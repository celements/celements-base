package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.navigation.cmd.MultilingualMenuNameCommand;
import com.celements.url.UrlService;
import com.xpn.xwiki.XWikiContext;

public class DefaultNavigationNodeValueResolverTest {

  @Test
  public void resolvesCanonicalTitleAndLanguageStableViewUrl() {
    ModelUtils modelUtils = createMock(ModelUtils.class);
    ModelContext modelContext = createMock(ModelContext.class);
    UrlService urlService = createMock(UrlService.class);
    MultilingualMenuNameCommand menuNameCommand = createMock(MultilingualMenuNameCommand.class);
    XWikiContext xwikiContext = createMock(XWikiContext.class);
    var docRef = new DocumentReference("Home",
        new SpaceReference("Content", new WikiReference("xwiki")));
    expect(modelUtils.serializeRefLocal(docRef)).andReturn("Content.Home").times(2);
    expect(modelContext.getXWikiContext()).andReturn(xwikiContext);
    expect(menuNameCommand.getMultilingualMenuName("Content.Home", "de", xwikiContext))
        .andReturn("Startseite");
    expect(urlService.getURL(docRef, "view", "language=de")).andReturn("/Content/Home?language=de");
    replay(modelUtils, modelContext, urlService, menuNameCommand, xwikiContext);
    var resolver = new DefaultNavigationNodeValueResolver(modelUtils, modelContext, urlService,
        menuNameCommand);
    assertEquals("Content.Home", resolver.serialize(docRef));
    assertEquals("Startseite", resolver.resolveTitle(docRef, "de"));
    assertEquals("/Content/Home?language=de", resolver.resolveUrl(docRef, "de"));
    verify(modelUtils, modelContext, urlService, menuNameCommand, xwikiContext);
  }

}
