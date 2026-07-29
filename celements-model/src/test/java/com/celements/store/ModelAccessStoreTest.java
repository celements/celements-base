package com.celements.store;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsAllPropertiesConfigurationSource;
import com.celements.model.access.IModelAccessFacade;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

public class ModelAccessStoreTest extends AbstractComponentTest {

  private IModelAccessFacade modelAccess;
  private ModelAccessStore store;

  @Before
  public void prepareTest() throws Exception {
    modelAccess = registerComponentMock(IModelAccessFacade.class);
    registerComponentMock(ConfigurationSource.class, CelementsAllPropertiesConfigurationSource.NAME,
        getConfigurationSource());
    store = (ModelAccessStore) getBeanFactory()
        .getBean(ModelAccessStore.NAME, XWikiStoreInterface.class);
  }

  @Test
  public void test_loadCelDocument() throws Exception {
    DocumentReference docRef = new DocumentReference("wiki", "space", "doc");
    XWikiDocument document = new XWikiDocument(docRef);
    document.setNew(false);
    CelDocument cachedDoc = CelDocument.from(document);
    expect(modelAccess.getCelDocument(docRef, ""))
        .andReturn(Optional.of(cachedDoc));

    replayDefault();
    CelDocument ret = store.loadCelDocument(docRef, "").orElseThrow();
    verifyDefault();

    assertSame(cachedDoc, ret);
  }

}
