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
 */
package com.xpn.xwiki.render;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiRadeoxRenderer}.
 *
 * @version $Id$
 */
public class XWikiRadeoxRendererTest extends AbstractComponentTest {

  private XWikiRadeoxRenderer renderer;

  private XWikiDocument document;

  private XWikiDocument contentDocument;

  @Before
  public void prepareTest() throws Exception {
    renderer = new XWikiRadeoxRenderer();
    contentDocument = new XWikiDocument();
    document = new XWikiDocument();
    expect(getWikiMock().Param(anyString())).andReturn("").anyTimes();
    expect(getWikiMock().Param(anyString(), anyString())).andReturn("").anyTimes();
    expect(getWikiMock().exists(anyString(), same(getContext()))).andReturn(false).anyTimes();
    getContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "Main", "WebHome")));
    replayDefault();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  @Test
  public void test_render_withSimpleText() {
    String result = this.renderer.render("Simple content", this.contentDocument, this.document,
        getContext());

    assertEquals("Simple content", result);
  }

  /**
   * @todo this test is too complex and show that the rendering API is not right...
   */
  @Test
  public void test_render_linkToNewPage() throws Exception {
    XWikiURLFactory urlFactory = createMock(XWikiURLFactory.class);
    expect(urlFactory.createURL(eq("Main"), eq("new link"), eq("edit"),
        eq("parent=Main.WebHome"), anyObject(String.class), same(getContext())))
        .andReturn(new URL("http://server.com/Main/new link"));
    expect(urlFactory.getURL(anyObject(URL.class), same(getContext())))
        .andReturn("/Main/new link").anyTimes();
    replay(urlFactory);
    getContext().setURLFactory(urlFactory);

    String result = this.renderer.render("This is a [new link]", this.contentDocument,
        this.document, getContext());

    assertEquals("This is a <a class=\"wikicreatelink\" href=\"/Main/new link\">"
        + "<span class=\"wikicreatelinktext\">new link</span>"
        + "<span class=\"wikicreatelinkqm\">?</span></a>",
        result);
    verify(urlFactory);
  }

  /**
   * Test that the parent is correctly escaped for links to non-existing documents.
   */
  @Test
  public void test_escapedParent_forLinkToNewPage() throws Exception {
    XWikiURLFactory urlFactory = createMock(XWikiURLFactory.class);
    expect(urlFactory.createURL(eq("A+ B"), eq("new link"), eq("edit"),
        eq("parent=A%2B+B.C%23+Examples+%26+Libs%3F+No%2C+I+prefer+C%2B%2B"),
        anyObject(String.class), same(getContext())))
        .andReturn(new URL("http://server.com/A%2B%20B/new link"));
    expect(urlFactory.getURL(anyObject(URL.class), same(getContext())))
        .andReturn("/A%2B+B/new link").anyTimes();
    replay(urlFactory);
    getContext().setURLFactory(urlFactory);

    getContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "A+ B",
        "C# Examples & Libs? No, I prefer C++")));

    String result = this.renderer.render("This is a [new link]", this.contentDocument,
        this.document, getContext());

    assertEquals("This is a <a class=\"wikicreatelink\" href=\"/A%2B+B/new link\">"
        + "<span class=\"wikicreatelinktext\">new link</span>"
        + "<span class=\"wikicreatelinkqm\">?</span></a>",
        result);
    verify(urlFactory);
  }

  @Test
  public void test_renderStyleMacro() throws Exception {
    String result = this.renderer.render("{style:type=div|align=justify}Hello{style}",
        this.contentDocument, this.document,
        getContext());
    assertEquals("<div align=\"justify\" style=\"\" >Hello</div>", result);
  }

  @Test
  public void test_renderStyleMacro_notImbricated() throws Exception {
    String result = this.renderer
        .render(
            "{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}. How fun.",
            this.contentDocument, this.document, getContext());
    assertEquals(
        "<span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>. How fun.",
        result);
  }

  @Test
  public void test_renderStyleMacro_notImbricatedInImbricated() throws Exception {
    String result = this.renderer
        .render(
            "{style:type=div|align=justify}{style:type=span|font-size=24px}One font{style} and {style:type=span|font-size=22px}another font size{style}.{style} How fun.",
            this.contentDocument, this.document, getContext());
    assertEquals(
        "<div align=\"justify\" style=\"\" ><span style=\"font-size:24px; \" >One font</span> and <span style=\"font-size:22px; \" >another font size</span>.</div> How fun.",
        result);
  }

  @Test
  public void test_renderStyleMacro_imbricated() throws Exception {
    String result = this.renderer
        .render(
            "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style}",
            this.contentDocument, this.document, getContext());
    assertEquals(
        "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div>",
        result);
  }

  @Test
  public void test_renderStyleMacro_imbricated2() throws Exception {
    String result = this.renderer
        .render(
            "{style:type=div|align=justify}Hello with {style:type=span|font-size=24px}style inside{style} the paragraph.{style} and this is very fun {style}",
            this.contentDocument, this.document, getContext());
    assertEquals(
        "<div align=\"justify\" style=\"\" >Hello with <span style=\"font-size:24px; \" >style inside</span> the paragraph.</div> and this is very fun <span style=\"\" ></span>",
        result);
  }

  @Test
  public void test_renderParagraph() throws Exception {
    String result = this.renderer.render("a\n\nb", this.contentDocument, this.document,
        getContext());
    assertEquals("a<p/>\nb", result);
  }

  @Test
  public void test_renderOneParagraph_forSeveralNewlines() throws Exception {
    String result = this.renderer.render("a\n\n\n\n\nb", this.contentDocument, this.document,
        getContext());
    assertEquals("a<p/>\nb", result);
  }

  @Test
  public void test_renderParagraph_ignoresSpaces() throws Exception {
    String result = this.renderer.render("a\n  \t\n  b", this.contentDocument, this.document,
        getContext());
    assertEquals("a<p/>\n  b", result);
  }

  @Test
  public void test_renderParagraph_withBr() throws Exception {
    String result = this.renderer.render("a\\\\\n\n\nb", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/><p/>\nb", result);
  }

  @Test
  public void test_renderNewline() throws Exception {
    String result = this.renderer.render("a\\\\b", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/>b", result);
  }

  @Test
  public void test_renderNewline_withCarriageReturn() throws Exception {
    String result = this.renderer.render("a\\\\\nb", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/>b", result);
  }

  @Test
  public void test_renderTwoNewline() throws Exception {
    String result = this.renderer.render("a\\\\\\\\b", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/><br/>b", result);
  }

  @Test
  public void test_renderTwoNewline_withCarriageReturn() throws Exception {
    String result = this.renderer.render("a\\\\\\\\\nb", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/><br/>b", result);
  }

  @Test
  public void test_renderThreeNewline() throws Exception {
    String result = this.renderer.render("a\\\\\\\\\\\\b", this.contentDocument, this.document,
        getContext());
    assertEquals("a<br/><br/><br/>b", result);
  }

  @Test
  public void test_renderEncodedBackslash() throws Exception {
    String result = this.renderer.render("\\\\\\", this.contentDocument, this.document,
        getContext());
    assertEquals("&#92;", result);
  }

  @Test
  public void test_renderEscapedCharacters() throws Exception {
    String result = this.renderer.render("\\[NotALink\\]", this.contentDocument, this.document,
        getContext());
    assertEquals("&#91;NotALink&#93;", result);
  }

  @Test
  public void test_table() throws Exception {
    String result = this.renderer.render("{table}\nA\n{table}", this.contentDocument, this.document,
        getContext());
    assertEquals(
        "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A</th></tr></table>",
        result);
  }

  @Test
  public void test_table_emptyTable() throws Exception {
    String result = this.renderer.render("{table}\n{table}", this.contentDocument, this.document,
        getContext());
    assertEquals(
        "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr></tr></table>",
        result);
  }

  @Test
  public void test_table_withCR() throws Exception {
    String result = this.renderer.render("{table}\nA\\\\B\n{table}", this.contentDocument,
        this.document, getContext());
    assertEquals(
        "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/>B</th></tr></table>",
        result);
  }

  @Test
  public void test_table_withCRWithSpace() throws Exception {
    String result = this.renderer.render("{table}\nA\\\\ \n{table}", this.contentDocument,
        this.document, getContext());
    assertEquals(
        "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/></th></tr></table>",
        result);
  }

  @Test
  public void test_table_withCRWithoutSpace() throws Exception {
    String result = this.renderer.render("{table}\nA\\\\\n{table}", this.contentDocument,
        this.document, getContext());
    assertEquals(
        "<table class=\"wiki-table\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><th>A<br/></th></tr></table>",
        result);
  }

  @Test
  public void test_macros_withWikiMarkupInUrl() throws Exception {
    String result = this.renderer.render("{image:http://www.some.server/__not__underlined.png}",
        this.contentDocument,
        this.document, getContext());
    assertTrue(result.indexOf("<em") == -1);
    result = this.renderer.render("{attach:http://www.some.server/__not__underlined.png}",
        this.contentDocument,
        this.document, getContext());
    assertTrue(result.indexOf("a href=\"http://www.some.server/__not__underlined.png") != -1);
    result = this.renderer.render(
        "{attach:this *is* __underlined__|http://www.some.server/__not__underlined.png}",
        this.contentDocument, this.document, getContext());
    assertTrue(result.indexOf("<strong>is</strong>") != -1);
  }

  @Test
  public void test_urls_withWikiMarkup() throws Exception {
    String result = this.renderer
        .render("http://www.xwiki.org/__some__URL~~with~~markup\n"
            + "[http://www.xwiki.org/__some__URL~~with~~markup]", this.contentDocument,
            this.document,
            getContext());
    assertTrue(result.indexOf("<em>") == -1);
    assertTrue(result.indexOf("wikiexternallink") != -1);
  }

  /**
   * Tests that the java syntax highlighting for the old {code} macro behaves properly when there's
   * an unclosed quote:
   * no stack overflow, reasonable rendering time, no thrown exceptions.
   */
  @Test
  public void test_javaCodeFilter_withUnclosedQuote() {
    StringBuffer source = new StringBuffer(
        "{code}private static final String S = \"This is a valid string\";\n");
    source.append("Unclosed quote: \"\n");
    for (int i = 0; i < 30; ++i) {
      source.append("private static final double D = 2.0;\n");
    }
    source.append("{code}");
    long startTime = System.currentTimeMillis();
    try {
      String result = this.renderer.render(source.toString(), this.contentDocument, this.document,
          getContext());
      // If a stack overflow occurs during rendering, then the valid quotes won't be recognized.
      assertTrue("Failed to detect strings", result.indexOf("quote\">") != -1);
    } catch (Throwable ex) {
      fail("Failed rendering: " + ex.getMessage());
    }
    // This test should definitely take less than a minute.
    assertTrue("Rendering took too much time", (System.currentTimeMillis() - startTime) < 60000);
  }
}
