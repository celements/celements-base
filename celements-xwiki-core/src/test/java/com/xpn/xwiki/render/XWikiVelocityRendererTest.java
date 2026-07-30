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

import java.util.Collections;

import org.apache.velocity.VelocityContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.velocity.VelocityExecutionProp;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit tests for {@link com.xpn.xwiki.render.XWikiVelocityRenderer}.
 *
 * @version $Id$
 */
public class XWikiVelocityRendererTest extends AbstractComponentTest {

  private XWikiVelocityRenderer renderer;

  private XWikiDocument document;

  private XWikiDocument contentDocument;

  @Before
  public void prepareTest() throws Exception {
    renderer = new XWikiVelocityRenderer();
    contentDocument = createDefaultMock(XWikiDocument.class);
    document = createDefaultMock(XWikiDocument.class);
    Document apiDocument = createDefaultMock(Document.class);
    expect(getWikiMock().getSkin(same(getContext()))).andReturn("default").anyTimes();
    expect(getWikiMock().getSkinFile(anyString(), same(getContext()))).andReturn(null).anyTimes();
    expect(getWikiMock().getSkinFile(anyString(), anyString(), same(getContext())))
        .andReturn(null).anyTimes();
    expect(getWikiMock().getResourceContent(anyString())).andReturn(null).anyTimes();
    getWikiMock().prepareResources(same(getContext()));
    expectLastCall().anyTimes();
    expect(getWikiMock().Param(anyString())).andReturn("").anyTimes();
    expect(getWikiMock().getIncludedMacros(anyString(), anyString(), same(getContext())))
        .andReturn(Collections.emptyList()).anyTimes();
    expect(getWikiMock().getDocument(anyObject(DocumentReference.class), same(getContext())))
        .andReturn(new XWikiDocument()).anyTimes();
    expect(contentDocument.getSpace()).andReturn("Space1").anyTimes();
    expect(document.getPrefixedFullName()).andReturn("xwiki:Space2.Document").anyTimes();
    expect(document.newDocument(same(getContext()))).andReturn(apiDocument).anyTimes();
    getBeanFactory().getBean(Execution.class).getContext()
        .set(VelocityExecutionProp.VELOCITY_CONTEXT, new VelocityContext());
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

  @Test
  public void test_render_withVelocityContent() {
    String result = this.renderer.render("#set ($test = \"hello\")\n$test world\n## comment",
        this.contentDocument,
        this.document, getContext());

    assertEquals("hello world\n", result);
  }
}
