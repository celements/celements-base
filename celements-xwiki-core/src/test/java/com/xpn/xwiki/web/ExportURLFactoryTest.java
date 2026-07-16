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
package com.xpn.xwiki.web;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;

public class ExportURLFactoryTest extends AbstractComponentTest {

  /** Temporary directory where to put exported files. Will be deleted at the end of the test. */
  private File tmpDir;

  /** The tested instance. */
  private ExportURLFactory urlFactory;

  private XWikiDocument exportedDocument;

  @Before
  public void prepareTest() throws Exception {
    urlFactory = new ExportURLFactory(new XWikiConfig());

    expect(getWikiMock().getWebAppPath(same(getContext()))).andReturn("/xwiki").anyTimes();
    expect(getWikiMock().Param(anyString())).andReturn(null).anyTimes();
    expect(getWikiMock().getDocument(anyString(), same(getContext())))
        .andAnswer(() -> exportedDocument).anyTimes();
    getContext().setURL(new URL("http://www.xwiki.org/"));

    // The URLFactory uses a request to determine the values for the context and servlet path.
    XWikiRequest request = createMock(XWikiRequest.class);
    expect(request.getScheme()).andReturn("http").anyTimes();
    expect(request.isSecure()).andReturn(false).anyTimes();
    expect(request.getServletPath()).andReturn("/bin").anyTimes();
    expect(request.getContextPath()).andReturn("/xwiki").anyTimes();
    expect(request.getHeader(anyString())).andReturn(null).anyTimes();
    replay(request);
    getContext().setRequest(request);

    // Since the ExportURLFactory saves requested attachments to the disk, create a temporary folder
    // to hold these
    // files, which will be deleted after the test ends.
    this.tmpDir = new File(System.getProperty("java.io.tmpdir"), "xwikitests");
    this.tmpDir.mkdirs();
    new File(this.tmpDir, "attachment").mkdir();

    replayDefault();
    this.urlFactory.init(null, this.tmpDir, getContext());
  }

  /**
   * Test that
   * {@link ExportURLFactory#createAttachmentURL(String, String, String, String, String, com.xpn.xwiki.XWikiContext)}
   * correctly escapes spaces into %20 when the exported document contains spaces in its name.
   */
  @Test
  public void test_createAttachmentURL() throws Exception {
    // Prepare the exported document and attachment.
    exportedDocument = new XWikiDocument(" Space ", "New  Page");
    XWikiAttachment attachment = new XWikiAttachment(exportedDocument, "img .jpg");
    attachment.setContent("test".getBytes());
    exportedDocument.getAttachmentList().add(attachment);

    URL url = this.urlFactory.createAttachmentURL("img .jpg", " Space ", "Pa ge", "view", "", "x",
        getContext());
    assertEquals(new URL("file://attachment/x.%20Space%20.Pa%20ge.img%20.jpg"), url);
  }

  /** When the test is over, delete the folder where the exported attachments were placed. */
  @After
  public void cleanUpTest() throws Exception {
    verifyDefault();
    FileUtils.deleteDirectory(this.tmpDir);
  }
}
