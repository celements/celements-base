package com.celements.wiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiConstant;

public class XWikiServerDescriptorServiceTest extends AbstractComponentTest {

  private WikiDescriptorService service;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(
        ModelUtils.class,
        IModelAccessFacade.class);
    service = getBeanFactory().getBean(WikiDescriptorService.class);
  }

  @Test
  public void test_loadingService() throws Exception {
    replayDefault();
    assertNotNull(service);
    verifyDefault();
  }

  @Test
  public void test_getDescriptorDocRef_mainWiki() {
    expect(getMock(ModelUtils.class).getMainWikiRef()).andReturn(new WikiReference("unkownMain"));
    expect(getMock(ModelUtils.class).normalizeWikiRef(eq(XWikiConstant.MAIN_WIKI)))
        .andReturn(new WikiReference("main"));
    replayDefault();
    DocumentReference configDocRef = service.getDescriptorDocRef(XWikiConstant.MAIN_WIKI);
    verifyDefault();
    assertEquals("XWikiServerMain", configDocRef.getName());
  }

}
