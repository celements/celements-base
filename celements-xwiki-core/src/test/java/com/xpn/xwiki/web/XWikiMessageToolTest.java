/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author ravenees
 */
package com.xpn.xwiki.web;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListResourceBundle;

import org.easymock.IExpectationSetters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.XWikiMessageTool} class.
 *
 * @version $Id$
 */
public class XWikiMessageToolTest extends AbstractComponentTest {

  private XWikiMessageTool tool;

  private boolean mocksReplayed;

  @Before
  public void prepareTest() throws Exception {
    tool = new XWikiMessageTool(new TestResources(), getContext());
  }

  @After
  public void verifyTest() {
    if (mocksReplayed) {
      verifyDefault();
    }
  }

  public class TestResources extends ListResourceBundle {

    private final Object[][] contents = { { "key", "value" } };

    @Override
    public Object[][] getContents() {
      return contents;
    }
  }

  /**
   * When no preference exist the returned value is the value of the key.
   */
  @Test
  public void test_get_whenPreferenceDoesNotExist() {
    expectWiki(null, null);
    replayMocks();

    assertEquals("invalid", this.tool.get("invalid"));
  }

  @Test
  public void test_get_whenNoTranslationAvailable() {
    expectWiki(null, null);
    replayMocks();

    assertEquals("value", this.tool.get("key"));
  }

  /**
   * When the key is null the returned value is null.
   */
  @Test
  public void test_get_whenKeyIsNull() {
    replayMocks();
    assertNull(this.tool.get(null));
  }

  @Test
  public void test_get_whenInXWikiPreferences() throws Exception {
    expectWiki("Space1.Doc1, Space2.Doc2", null);
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(createDocument(111111L, "Space1.Doc1", "somekey=somevalue", false));
    expect(getWikiMock().getDocument(eq("Space2.Doc2"), same(getContext())))
        .andReturn(createDocument(222222L, "Space2.Doc2",
            "someKey=someValue\nkeyInXWikiPreferences=eureka", false));
    replayMocks();

    assertEquals("eureka", this.tool.get("keyInXWikiPreferences"));
  }

  @Test
  public void test_get_whenInXWikiConfigurationFile() throws Exception {
    expectWiki(null, "Space1.Doc1");
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(createDocument(111111L, "Space1.Doc1", "keyInXWikiCfg=gotcha", false));
    replayMocks();

    assertEquals("gotcha", this.tool.get("keyInXWikiCfg"));
  }

  /**
   * Validate usage of parameters in bundles
   */
  @Test
  public void test_get_withParameters() throws Exception {
    expectWiki(null, "Space1.Doc1");
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(createDocument(111111L, "Space1.Doc1",
            "key=We have {0} new documents with {1} objects. {2}", false));
    replayMocks();

    List<String> params = new ArrayList<>();
    params.add("12");
    params.add("3");

    assertEquals("We have 12 new documents with 3 objects. {2}", this.tool.get("key", params));
  }

  /**
   * Verify that a document listed as a bundle document that doesn't exist is not returned as a
   * bundle document.
   */
  @Test
  public void test_getDocumentBundles_whenDocumentDoesNotExist() throws Exception {
    expectWiki("Space1.Doc1", null);
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(createDocument(111111L, "Space1.Doc1", "", true));
    replayMocks();
    List<XWikiDocument> docs = this.tool.getDocumentBundles();
    assertEquals(0, docs.size());
  }

  @Test
  public void test_get_returnsFromCacheWhenCalledTwice() throws Exception {
    expectWiki("Space1.Doc1", null);
    XWikiDocument document = createMockDocument(11111L, "Space1.Doc1", "key=value", false, 1);
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(document).anyTimes();
    replayMocks();

    // After this call, the value should be in cache.
    this.tool.get("key");

    // We verify that the second time the getContent method is NOT called as the value is
    // returned from cache
    this.tool.get("key");
  }

  @Test
  public void test_get_whenDocumentModifiedAfterItIsInCache() throws Exception {
    expectWiki("Space1.Doc1", null);
    XWikiDocument document = createModifiedDocument(11111L, "Space1.Doc1");
    expect(getWikiMock().getDocument(eq("Space1.Doc1"), same(getContext())))
        .andReturn(document).anyTimes();
    replayMocks();

    // First time get any key just to put the doc properties in cache
    assertEquals("modifiedKey", this.tool.get("modifiedKey"));

    // Even though the document has been cached it's reloaded because its date has changed
    assertEquals("found", this.tool.get("modifiedKey"));
  }

  // FIXME
  // public void testGetWhenWithTranslation() {
  // this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));
  // this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
  // this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING).will(
  // returnValue(createDocumentWithTrans(111111L, "Space1.Doc1",
  // "somekey=somevalue\nsomekey2=somevalue2",
  // "somekey=somevaluetrans", false)));
  //
  // getContext().setLanguage("en");
  // assertEquals("somevalue", this.tool.get("somekey"));
  // assertEquals("somevalue2", this.tool.get("somekey2"));
  //
  // // Switch to french
  // getContext().setLanguage("fr");
  // this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));
  // assertEquals("somevaluetrans", this.tool.get("somekey"));
  // assertEquals("somevalue2", this.tool.get("somekey2"));
  // }

  private void expectWiki(String preference, String param) {
    expect(getWikiMock().getDefaultLanguage(same(getContext()))).andReturn("en").anyTimes();
    expect(getWikiMock().getXWikiPreference(anyString(), same(getContext())))
        .andReturn(preference).anyTimes();
    expect(getWikiMock().Param(anyString())).andReturn(param).anyTimes();
  }

  private void replayMocks() {
    replayDefault();
    mocksReplayed = true;
  }

  private XWikiDocument createDocument(long id, String name, String content, boolean isNew)
      throws Exception {
    return createMockDocument(id, name, content, isNew, -1);
  }

  private XWikiDocument createMockDocument(long id, String name, String content, boolean isNew,
      int contentCalls) throws Exception {
    XWikiDocument document = createDefaultMock(XWikiDocument.class);
    expect(document.getTranslatedDocument(same(getContext()))).andReturn(document).anyTimes();
    expect(document.isNew()).andReturn(isNew).anyTimes();
    expect(document.getId()).andReturn(id).anyTimes();
    expect(document.getDate()).andReturn(new Date()).anyTimes();
    IExpectationSetters<String> contentExpectation = expect(document.getContent())
        .andReturn(content);
    if (contentCalls >= 0) {
      contentExpectation.times(contentCalls);
    } else {
      contentExpectation.anyTimes();
    }
    expect(document.getFullName()).andReturn(name).anyTimes();
    expect(document.getRealLanguage()).andReturn("en").anyTimes();
    return document;
  }

  private XWikiDocument createModifiedDocument(long id, String name) throws Exception {
    XWikiDocument document = createDefaultMock(XWikiDocument.class);
    Date initialDate = new Date(1);
    Date modifiedDate = new Date(2);
    expect(document.getTranslatedDocument(same(getContext()))).andReturn(document).anyTimes();
    expect(document.isNew()).andReturn(false).anyTimes();
    expect(document.getId()).andReturn(id).anyTimes();
    expect(document.getDate()).andReturn(initialDate).once();
    expect(document.getDate()).andReturn(modifiedDate).anyTimes();
    expect(document.getContent()).andReturn("modifiedKey=modifiedKey").once();
    expect(document.getContent()).andReturn("modifiedKey=found").anyTimes();
    expect(document.getFullName()).andReturn(name).anyTimes();
    expect(document.getRealLanguage()).andReturn("en").anyTimes();
    return document;
  }
}
