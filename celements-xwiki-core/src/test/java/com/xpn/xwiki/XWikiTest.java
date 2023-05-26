package com.xpn.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.web.Utils;

public class XWikiTest extends AbstractBridgedComponentTestCase {

  private ServerUrlUtils serverUtils;
  private XWiki wikiMock;

  @Before
  public void setUp_XWikiTest() throws Exception {
    serverUtils = (ServerUrlUtils) Utils.getComponent(ServerUrlUtilsRole.class);
    wikiMock = createMock(XWiki.class);
    getContext().setWiki(wikiMock);
  }

  @Test
  public void test_getServerObject_nullContext() throws Exception {
    String theDatabase = "myWiki";
    String mainDatabase = "main";
    DocumentReference docRef = new DocumentReference(mainDatabase, "XWiki", "XWikiServerMyWiki");
    XWikiDocument configWikiDoc = new XWikiDocument(docRef);
    expect(wikiMock.getDatabase()).andReturn(mainDatabase).anyTimes();
    expect(wikiMock.getDocument(eq(docRef), same(getContext()))).andReturn(configWikiDoc);
    replay(wikiMock);
    getContext().setURL(null);
    assertNull(serverUtils.getServerObject(theDatabase, getContext()));
    verify(wikiMock);
  }

  @Test
  public void test_getServerURL_getPort_nullContextUrl() throws Exception {
    String theDatabase = "myWiki";
    String mainDatabase = "xwiki";
    DocumentReference docRef = new DocumentReference(mainDatabase, "XWiki", "XWikiServerMyWiki");
    XWikiDocument configWikiDoc = new XWikiDocument(docRef);
    BaseObject serverCfgObj = new BaseObject();
    serverCfgObj.setXClassReference(XWiki.VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
    configWikiDoc.addXObject(serverCfgObj);
    expect(wikiMock.getDatabase()).andReturn(mainDatabase).anyTimes();
    expect(wikiMock.getDocument(eq(docRef), same(getContext()))).andReturn(configWikiDoc);
    expect(wikiMock.Param(eq("xwiki.url.protocol"), (String) isNull())).andReturn("http");
    replay(wikiMock);
    getContext().setURL(null);
    assertNotNull(serverUtils.getServerURL(theDatabase, getContext()));
    verify(wikiMock);
  }

}
