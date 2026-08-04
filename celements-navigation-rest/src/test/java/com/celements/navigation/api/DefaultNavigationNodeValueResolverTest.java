package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.url.UrlService;
import com.xpn.xwiki.doc.XWikiDocument;

public class DefaultNavigationNodeValueResolverTest extends AbstractComponentTest {

  private ModelUtils modelUtils;
  private ModelContext modelContext;
  private UrlService urlService;
  private IModelAccessFacade modelAccess;
  private DefaultNavigationNodeValueResolver resolver;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(ModelUtils.class, ModelContext.class, UrlService.class,
        IModelAccessFacade.class);
    modelUtils = getMock(ModelUtils.class);
    modelContext = getMock(ModelContext.class);
    urlService = getMock(UrlService.class);
    modelAccess = getMock(IModelAccessFacade.class);
    var beanFactory = (DefaultListableBeanFactory) getBeanFactory();
    beanFactory.destroySingleton(DefaultNavigationNodeValueResolver.class.getName());
    beanFactory.registerResolvableDependency(ModelUtils.class, modelUtils);
    beanFactory.registerResolvableDependency(ModelContext.class, modelContext);
    beanFactory.registerResolvableDependency(UrlService.class, urlService);
    resolver = getBeanFactory().getBean(DefaultNavigationNodeValueResolver.class.getName(),
        DefaultNavigationNodeValueResolver.class);
  }

  @Test
  public void test_resolvesCanonicalTitleAndLanguageStableViewUrl() {
    var docRef = new DocumentReference("Home",
        new SpaceReference("Content", new WikiReference("xwiki")));
    XWikiDocument document = createDefaultMock(XWikiDocument.class);
    expect(modelUtils.serializeRefLocal(docRef)).andReturn("Content.Home").times(2);
    expect(modelContext.getXWikiContext()).andReturn(getXContext());
    expect(modelUtils.resolveRef("Content.Home", DocumentReference.class)).andReturn(docRef);
    expect(modelAccess.getOrCreateDocument(docRef)).andReturn(document);
    expect(getXContext().getWiki().isMultiLingual(getXContext())).andReturn(false);
    expect(document.getObject("Celements2.MenuName")).andReturn(null);
    expect(modelAccess.getDocumentOpt(docRef, "de")).andReturn(Optional.of(document));
    expect(document.getTitle()).andReturn("Startseite");
    expect(urlService.getURL(docRef, "view", "language=de")).andReturn("/Content/Home?language=de");
    replayDefault();
    assertEquals("Content.Home", resolver.serialize(docRef));
    assertEquals("Startseite", resolver.resolveTitle(docRef, "de"));
    assertEquals("/Content/Home?language=de", resolver.resolveUrl(docRef, "de"));
    verifyDefault();
  }

}
