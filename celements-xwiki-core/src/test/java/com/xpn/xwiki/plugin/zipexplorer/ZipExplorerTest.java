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
package com.xpn.xwiki.plugin.zipexplorer;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin} class.
 *
 * @version $Id$
 */
public class ZipExplorerTest extends AbstractComponentTest {

  private ZipExplorerPlugin plugin;

  private boolean mocksReplayed;

  @Before
  public void prepareTest() throws Exception {
    plugin = new ZipExplorerPlugin("zipexplorer", ZipExplorerPlugin.class.getName(), null);
  }

  @After
  public void verifyTest() {
    if (mocksReplayed) {
      verifyDefault();
    }
  }

  @Test
  public void test_isZipFile() throws Exception {
    byte txtbuf[] = { 0x00, 0x01, 0x02, 0x03, 0x06, 0x07 };
    ByteArrayInputStream txtBais = new ByteArrayInputStream(txtbuf);
    assertFalse(this.plugin.isZipFile(txtBais));

    byte tinybuf[] = { 0x00 };
    ByteArrayInputStream tinyBais = new ByteArrayInputStream(tinybuf);
    assertFalse(this.plugin.isZipFile(tinyBais));

    byte zipbuf[] = createZipFile("test");
    ByteArrayInputStream zipBais = new ByteArrayInputStream(zipbuf);
    assertTrue(this.plugin.isZipFile(zipBais));

  }

  @Test
  public void test_isValidZipURL() {
    assertTrue(this.plugin.isValidZipURL(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt",
        "download"));
    assertFalse(this.plugin.isValidZipURL(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt", "view"));
    assertFalse(this.plugin
        .isValidZipURL("http://server/xwiki/bin/download/Main/Document/zipfile.zip", "download"));
    assertFalse(
        this.plugin.isValidZipURL("http://server/xwiki/bin/download/Main/Document", "download"));

    // These tests should normally fail but we haven't implemented the check to verify if the
    // ZIP URL points to a file rather than a dir.
    assertTrue(this.plugin.isValidZipURL(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2/", "download"));
    assertTrue(this.plugin.isValidZipURL(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2", "download"));
  }

  @Test
  public void test_downloadAttachment_withInvalidZipURL() throws Exception {
    XWikiAttachment originalAttachment = createAttachment("someFile.txt", "Some text".getBytes(),
        new XWikiDocument());
    XWikiContext context = createXWikiContext(
        "http://server/xwiki/bin/download/Main/Document/someFile.txt");

    XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

    assertSame(originalAttachment, newAttachment);
  }

  @Test
  public void test_downloadAttachment() throws Exception {
    String zipFileContent = "File.txt content";
    XWikiAttachment originalAttachment = createAttachment("zipfile.zip",
        createZipFile(zipFileContent),
        new XWikiDocument());

    XWikiContext context = createXWikiContext(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt");

    XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

    assertEquals("Directory/File.txt", newAttachment.getFilename());
    assertEquals(zipFileContent.length(), newAttachment.getFilesize());
    assertEquals(zipFileContent.length(), newAttachment.getContentSize(context));
    assertEquals(zipFileContent, new String(newAttachment.getContent(context)));
  }

  @Test
  public void test_downloadAttachment_whenURLIsNotZipFile() throws Exception {
    XWikiAttachment originalAttachment = createAttachment("somefile.whatever", null,
        new XWikiDocument());

    XWikiContext context = createXWikiContext(
        "http://server/xwiki/bin/download/Main/Document/somefile.whatever");

    XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

    assertSame(originalAttachment, newAttachment);
  }

  @Test
  public void test_downloadAttachment_whenURLIsZipButNotPointingInsideZip() throws Exception {
    XWikiAttachment originalAttachment = createAttachment("zipfile.zip", null,
        new XWikiDocument());

    XWikiContext context = createXWikiContext(
        "http://server/xwiki/bin/download/Main/Document/zipfile.zip");

    XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

    assertSame(originalAttachment, newAttachment);
  }

  @Test
  public void test_getFileList() throws Exception {
    XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

    List<String> entries = this.plugin.getFileList(new Document(document, null), "zipfile.zip",
        null);

    assertEquals(2, entries.size());
    assertEquals("Directory/File.txt", entries.get(0));
    assertEquals("File2.txt", entries.get(1));
  }

  @Test
  public void test_getFileTreeList() throws Exception {
    XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

    List<ListItem> entries = this.plugin.getFileTreeList(new Document(document, null),
        "zipfile.zip", null);

    assertEquals(3, entries.size());

    assertEquals("Directory/", entries.get(0).getId());
    assertEquals("Directory", entries.get(0).getValue());
    assertEquals("", entries.get(0).getParent());

    assertEquals("Directory/File.txt", entries.get(1).getId());
    assertEquals("File.txt", entries.get(1).getValue());
    assertEquals("Directory/", entries.get(1).getParent());

    assertEquals("File2.txt", entries.get(2).getId());
    assertEquals("File2.txt", entries.get(2).getValue());
    assertEquals("", entries.get(2).getParent());
  }

  @Test
  public void test_getFileLink() throws Exception {
    XWikiDocument xdoc = createDefaultMock(XWikiDocument.class);
    expect(xdoc.getAttachmentURL(anyString(), eq("download"), isNull()))
        .andReturn("http://server/xwiki/bin/download/Main/Document/zipfile.zip");
    replayMocks();
    Document document = new Document(xdoc, null);

    String link = this.plugin.getFileLink(document, "zipfile.zip", "filename", null);

    assertEquals("http://server/xwiki/bin/download/Main/Document/zipfile.zip/filename", link);
  }

  @Test
  public void test_getFileLocationFromZipURL() {
    String urlPrefix = "server/xwiki/bin/download/Main/Document/zipfile.zip";

    assertEquals("Directory/File.txt",
        this.plugin.getFileLocationFromZipURL(urlPrefix + "/Directory/File.txt",
            "download"));
    assertEquals("", this.plugin.getFileLocationFromZipURL(urlPrefix, "download"));
    assertEquals("Some Directory/File WithSpace.txt",
        this.plugin.getFileLocationFromZipURL(urlPrefix
            + "/Some%20Directory/File%20WithSpace.txt", "download"));
  }

  private XWikiDocument createXWikiDocumentWithZipFileAttachment() throws Exception {
    XWikiDocument document = new XWikiDocument();
    XWikiAttachment attachment = createAttachment("zipfile.zip", createZipFile("Some content"),
        document);
    document.getAttachmentList().add(attachment);
    return document;
  }

  private XWikiContext createXWikiContext(String url) {
    XWikiRequest request = createDefaultMock(XWikiRequest.class);
    expect(request.getRequestURI()).andReturn(url);
    replayMocks();
    XWikiContext context = new XWikiContext();
    context.setRequest(request);
    context.setAction("download");
    return context;
  }

  private XWikiAttachment createAttachment(String filename, byte[] content, XWikiDocument document)
      throws Exception {
    XWikiAttachment attachment = new XWikiAttachment(document, filename);
    attachment.setAuthor("Vincent");
    attachment.setContent((content == null) ? new byte[0] : content);
    return attachment;
  }

  private void replayMocks() {
    replayDefault();
    mocksReplayed = true;
  }

  private byte[] createZipFile(String content) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    ZipEntry zipe = new ZipEntry("Directory/File.txt");
    zos.putNextEntry(zipe);
    zos.write(content.getBytes());
    ZipEntry zipe2 = new ZipEntry("File2.txt");
    zos.putNextEntry(zipe2);
    zos.write(content.getBytes());
    zos.closeEntry();
    return baos.toByteArray();
  }
}
