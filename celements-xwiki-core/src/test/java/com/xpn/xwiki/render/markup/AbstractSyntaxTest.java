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
 */
package com.xpn.xwiki.render.markup;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderer;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public abstract class AbstractSyntaxTest extends AbstractComponentTest {

  protected XWikiContext context;

  protected XWikiRadeoxRenderer renderer;

  protected XWikiDocument document;

  @Before
  public void prepareTest() throws Exception {
    renderer = new XWikiRadeoxRenderer();
    context = getContext();

    // These are needed by the Link filter
    expect(getWikiMock().exists(anyString(), same(context))).andReturn(true).anyTimes();
    expect(getWikiMock().exists(anyObject(DocumentReference.class), same(context)))
        .andReturn(true).anyTimes();
    expect(getWikiMock().showViewAction(same(context))).andReturn(true).anyTimes();
    expect(getWikiMock().skipDefaultSpaceInURLs(same(context))).andReturn(true).anyTimes();
    expect(getWikiMock().useDefaultAction(same(context))).andReturn(true).anyTimes();
    expect(getWikiMock().getDefaultSpace(same(context))).andReturn("Main").anyTimes();
    expect(getWikiMock().getEncoding()).andReturn("UTF-8").anyTimes();
    expect(getWikiMock().getServletPath(anyString(), same(context))).andReturn("bin/").anyTimes();
    replayDefault();

    this.context
        .setURLFactory(new XWikiServletURLFactory(new URL("http://localhost/"), "xwiki/", "bin/"));

    this.document = new XWikiDocument("Main", "WebHome");

    this.context.setDoc(this.document);
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  protected void test(ArrayList<String> tests, ArrayList<String> expects) {
    for (int i = 0; i < tests.size(); ++i) {
      String result = this.renderer.render(tests.get(i).toString(), this.document, this.document,
          this.context);
      String expected = expects.get(i).toString();
      if (expected.startsWith("...")) {
        assertTrue(result.indexOf(expected.substring(3, expected.length() - 3)) > 0);
      } else {
        assertEquals(expected, result);
      }
    }
  }

  protected static void assertTrue(boolean condition) {
    org.junit.Assert.assertTrue(condition);
  }

  protected static void assertEquals(Object expected, Object actual) {
    org.junit.Assert.assertEquals(expected, actual);
  }
}
