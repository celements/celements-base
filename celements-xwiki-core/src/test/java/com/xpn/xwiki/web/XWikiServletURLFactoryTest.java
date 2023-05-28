package com.xpn.xwiki.web;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiServletURLFactoryTest extends AbstractComponentTest {

  private static final String MAIN_WIKI_NAME = "xwiki";

  private XWikiConfig config;

  private XWikiServletURLFactory urlFactory = new XWikiServletURLFactory();

  private Map<String, Map<String, XWikiDocument>> databases = new HashMap<>();

  private Map<String, XWikiDocument> getDocuments(String database, boolean create)
      throws XWikiException {
    if (!this.databases.containsKey(database)) {
      if (create) {
        this.databases.put(database, new HashMap<String, XWikiDocument>());
      } else {
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_UNKNOWN,
            "Database " + database + " does not exists.");
      }
    }

    return this.databases.get(database);
  }

  private XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException {
    XWikiDocument document = new XWikiDocument(documentReference);

    Map<String, XWikiDocument> docs = getDocuments(document.getDatabase(), false);

    if (docs.containsKey(document.getFullName())) {
      return docs.get(document.getFullName());
    } else {
      return document;
    }
  }

  private void saveDocument(XWikiDocument document) throws XWikiException {
    document.setNew(false);
    Map<String, XWikiDocument> database = getDocuments(document.getDatabase(), true);
    database.remove(document.getFullName());
    database.put(document.getFullName(), document);
  }

  @Before
  public void setUp() throws Exception {
    this.databases.put(MAIN_WIKI_NAME, new HashMap<String, XWikiDocument>());

    XWiki xwiki = new XWiki() {

      @Override
      public XWikiDocument getDocument(String fullname, XWikiContext context)
          throws XWikiException {
        return XWikiServletURLFactoryTest.this.getDocument(Utils
            .getComponent(DocumentReferenceResolver.class, "currentmixed").resolve(fullname));
      }

      @Override
      public XWikiDocument getDocument(DocumentReference documentReference, XWikiContext context)
          throws XWikiException {
        return XWikiServletURLFactoryTest.this.getDocument(documentReference);
      }

      @Override
      public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context) {
        return defaultValue;
      }
    };
    xwiki.setConfig((this.config = new XWikiConfig()));
    xwiki.setDatabase(getContext().getDatabase());

    XWikiRequest requestMock = createDefaultMock(XWikiRequest.class);
    expect(requestMock.getScheme()).andReturn("http").anyTimes();
    expect(requestMock.getServletPath()).andReturn("").anyTimes();
    getContext().setWiki(xwiki);
    getContext().setRequest(requestMock);

    // Create sub-wikis.
    createWiki("wiki1");
    createWiki("wiki2");

    getContext().setURL(new URL("http://celements.com/view/InitialSpace/InitialPage"));
  }

  /**
   * Creates a new sub-wiki with the given name.
   *
   * @param wikiName
   *          the wiki name
   * @throws Exception
   *           if creating the wiki fails
   */
  private void createWiki(String wikiName) throws Exception {
    String wikiDocName = "XWikiServer" + wikiName.substring(0, 1).toUpperCase()
        + wikiName.substring(1);
    XWikiDocument wikiDoc = getDocument(
        new DocumentReference(MAIN_WIKI_NAME, "XWiki", wikiDocName));
    BaseObject wikiObj = wikiDoc.newObject("XWiki.XWikiServerClass", getContext());
    wikiObj.setStringValue("server", wikiName + ".celements.com");
    saveDocument(wikiDoc);
  }

  @Test
  public void testCreateURLOnMainWiki() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(false).anyTimes();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn(null);

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
        getContext());
    assertEquals(new URL("http://celements.com/view/Space/Page?param1=1#anchor"), url);
    verifyDefault();
  }

  @Test
  public void testCreateURLOnSubWiki() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(false).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn(null);

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
        getContext());
    assertEquals(new URL("http://wiki1.celements.com/view/Space/Page?param1=1#anchor"), url);
    verifyDefault();
  }

  @Test
  public void testCreateURLOnSubWikiInVirtualMode() throws MalformedURLException {
    this.config.setProperty("xwiki.virtual", "1");
    expect(getContext().getRequest().isSecure()).andReturn(false).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn(null);

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
        getContext());
    assertEquals(new URL("http://wiki1.celements.com/view/Space/Page?param1=1#anchor"), url);
    verifyDefault();
  }

  /**
   * Checks the URLs created on the main wiki when XWiki is behind a reverse proxy.
   *
   * @throws MalformedURLException
   *           shouldn't happen
   */
  @Test
  public void testCreateURLOnMainWikiInReverseProxyMode() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(true).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn("www.xwiki.org");

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
        getContext());
    assertEquals(new URL("https://www.xwiki.org/view/Space/Page?param1=1#anchor"), url);
    assertEquals("/view/Space/Page?param1=1#anchor",
        urlFactory.getURL(url, getContext()));
    verifyDefault();
  }

  @Test
  public void testCreateURLOnSubWikiInReverseProxyMode() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(false).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn("www.xwiki.org");

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
        getContext());
    assertEquals(new URL("http://wiki1.celements.com/view/Space/Page?param1=1#anchor"), url);
    // The URL remains absolute in this case.
    assertEquals("http://wiki1.celements.com/view/Space/Page?param1=1#anchor",
        urlFactory.getURL(url, getContext()));
    verifyDefault();
  }

  @Test
  public void testCreateURLOnSubWikiInVirtualModeInReverseProxyMode() throws MalformedURLException {
    config.setProperty("xwiki.virtual", "1");
    expect(getContext().getRequest().isSecure()).andReturn(true).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn("www.xwiki.org");

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
        getContext());
    assertEquals(new URL("https://wiki1.celements.com/view/Space/Page?param1=1#anchor"), url);
    // The URL remains absolute in this case.
    assertEquals("https://wiki1.celements.com/view/Space/Page?param1=1#anchor",
        urlFactory.getURL(url, getContext()));
    verifyDefault();
  }

  @Test
  public void testCreateURLOnMainWikiInPathModeInReverseProxyMode() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(false).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn("www.xwiki.org");

    replayDefault();
    urlFactory.init(getContext());
    URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
        getContext());
    assertEquals(new URL("http://www.xwiki.org/view/Space/Page?param1=1#anchor"), url);
    assertEquals("/view/Space/Page?param1=1#anchor",
        urlFactory.getURL(url, getContext()));
    verifyDefault();
  }

  /**
   * Tests how URLs are serialized when the request wiki (taken from the request URL) and the
   * context wiki (explicitly
   * set from code on the XWiki context) are different.
   */
  @Test
  public void testGetURLWhenRequestWikiAndContextWikiAreDifferent() throws MalformedURLException {
    expect(getContext().getRequest().isSecure()).andReturn(false).atLeastOnce();
    expect(getContext().getRequest().getHeader("x-forwarded-host")).andReturn(null);
    getContext().setURL(new URL("http://wiki1.celements.com/view/InitialSpace/InitialPage"));
    getContext().setDatabase("wiki2");

    replayDefault();
    urlFactory.init(getContext());
    String url = urlFactory.getURL(new URL("http://wiki1.celements.com/view/Space/Page"),
        getContext());
    assertEquals("/view/Space/Page", url);

    url = urlFactory.getURL(new URL("http://wiki2.celements.com/view/Space/Page"), getContext());
    assertEquals("http://wiki2.celements.com/view/Space/Page", url);
    verifyDefault();
  }
}
