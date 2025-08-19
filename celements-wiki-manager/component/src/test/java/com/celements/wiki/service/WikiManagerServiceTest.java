package com.celements.wiki.service;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiConstant;

public class WikiManagerServiceTest extends AbstractComponentTest {

  private WikiManagerService service;

  @Before
  public void prepare() throws Exception {
    registerComponentMock(ModelUtils.class);
    service = getSpringContext().getBean(WikiManagerService.class);
  }

  @Test
  public void test_loadingService() throws Exception {
    replayDefault();
    assertNotNull(service);
    verifyDefault();
  }

  @Test
  public void test_getWikiConfigDocRef_mainWiki() {
    expect(getMock(ModelUtils.class).getMainWikiRef()).andReturn(new WikiReference("unkownMain"));
    expect(getMock(ModelUtils.class).getDatabaseNameWithoutPrefix(eq(XWikiConstant.MAIN_WIKI)))
        .andReturn("main");
    replayDefault();
    DocumentReference configDocRef = service.getWikiConfigDocRef(XWikiConstant.MAIN_WIKI);
    verifyDefault();
    assertEquals("XWikiServerMain", configDocRef.getName());
  }

}
