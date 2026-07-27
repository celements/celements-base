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
package com.xpn.xwiki.doc;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import org.apache.velocity.VelocityContext;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.velocity.VelocityManager;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.XWikiMessageTool;

/**
 * Unit tests for {@link XWikiDocument}.
 *
 * @version $Id$
 */
public class XWikiDocumentTest extends AbstractComponentTest {

  private static final String DOCWIKI = "wiki";

  private static final String DOCSPACE = "Space";

  private static final String DOCNAME = "Page";

  private static final String DOCFULLNAME = DOCSPACE + "." + DOCNAME;

  private static final String CLASSNAME = DOCFULLNAME;

  private XWikiDocument document;

  private XWikiRenderingEngine mockXWikiRenderingEngine;

  private XWikiVersioningStoreInterface mockXWikiVersioningStore;

  private XWikiStoreInterface mockXWikiStoreInterface;

  private XWikiMessageTool mockXWikiMessageTool;

  private XWikiRightService mockXWikiRightService;

  private String wikiEncoding;

  private BaseClass wikiClass;

  private BaseClass wikiXClass;

  private XWikiDocument wikiDocument;

  private BaseClass baseClass;

  private BaseObject baseObject;

  @Before
  public void prepareTest() throws Exception {
    this.document = new XWikiDocument(new DocumentReference(DOCWIKI, DOCSPACE, DOCNAME));
    this.document.setId(1, IdVersion.CELEMENTS_3);
    this.document.setSyntax(Syntax.XWIKI_1_0);
    this.document.setLanguage("en");
    this.document.setDefaultLanguage("en");
    this.document.setNew(false);

    getContext().put("isInRenderingEngine", true);

    this.mockXWikiRenderingEngine = createMock(XWikiRenderingEngine.class);
    this.mockXWikiVersioningStore = createDefaultMock(XWikiVersioningStoreInterface.class);
    this.mockXWikiStoreInterface = createDefaultMock(XWikiStoreInterface.class);
    this.document.setStore(this.mockXWikiStoreInterface);
    this.mockXWikiMessageTool = createDefaultMock(XWikiMessageTool.class);
    this.mockXWikiRightService = createDefaultMock(XWikiRightService.class);
    VelocityManager velocityManagerMock = registerComponentMock(VelocityManager.class);
    expect(velocityManagerMock.getVelocityContext()).andReturn(new VelocityContext()).anyTimes();
    getContext().put("msg", this.mockXWikiMessageTool);

    this.baseClass = this.document.getxWikiClass();
    this.baseClass.addTextField("string", "String", 30);
    this.baseClass.addTextAreaField("area", "Area", 10, 10);
    this.baseClass.addTextAreaField("puretextarea", "Pure text area", 10, 10);
    // set the text areas an non interpreted content
    ((TextAreaClass) this.baseClass.getField("puretextarea")).setContentType("puretext");
    this.baseClass.addPasswordField("passwd", "Password", 30);
    this.baseClass.addBooleanField("boolean", "Boolean", "yesno");
    this.baseClass.addNumberField("int", "Int", 10, "integer");
    this.baseClass.addStaticListField("stringlist", "StringList", "value1, value2");

    this.baseObject = this.baseClass.newCustomClassInstance(getContext());
    this.baseObject.setClassName(CLASSNAME);
    this.document.addObject(CLASSNAME, this.baseObject);
    this.baseObject.setStringValue("string", "string");
    this.baseObject.setLargeStringValue("area", "area");
    this.baseObject.setStringValue("passwd", "passwd");
    this.baseObject.setIntValue("boolean", 1);
    this.baseObject.setIntValue("int", 42);
    this.baseObject.setStringListValue("stringlist", Arrays.asList("VALUE1", "VALUE2"));

    this.wikiEncoding = "UTF-8";
    this.wikiClass = this.baseClass;
    this.wikiXClass = this.baseClass;
    this.wikiDocument = this.document;

    expect(getWikiMock().Param(anyString())).andReturn(null).anyTimes();
    expect(getWikiMock().getRenderingEngine()).andReturn(this.mockXWikiRenderingEngine).anyTimes();
    expect(getWikiMock().getVersioningStore()).andReturn(this.mockXWikiVersioningStore).anyTimes();
    expect(getWikiMock().getStore()).andReturn(this.mockXWikiStoreInterface).anyTimes();
    expect(getWikiMock().loadCelDocument(anyObject(DocumentReference.class)))
        .andReturn(Optional.empty()).anyTimes();
    expect(getWikiMock().getDocument(anyObject(DocumentReference.class), same(getContext())))
        .andAnswer(() -> this.wikiDocument).anyTimes();
    expect(getWikiMock().getLanguagePreference(same(getContext()))).andReturn("en").anyTimes();
    expect(getWikiMock().getSectionEditingDepth()).andReturn(2L).anyTimes();
    expect(getWikiMock().getRightService()).andReturn(this.mockXWikiRightService).anyTimes();
    expect(getWikiMock().getClass(anyString(), same(getContext())))
        .andAnswer(() -> this.wikiClass).anyTimes();
    expect(getWikiMock().getXClass(anyObject(DocumentReference.class), same(getContext())))
        .andAnswer(() -> this.wikiXClass).anyTimes();
    expect(getWikiMock().getEncoding()).andAnswer(() -> this.wikiEncoding).anyTimes();
    expect(getWikiMock().getConfig()).andReturn(new XWikiConfig()).anyTimes();
    expect(getWikiMock().exists(anyString(), same(getContext()))).andReturn(true).anyTimes();
    expect(getWikiMock().copyDocument(anyObject(DocumentReference.class),
        anyObject(DocumentReference.class), eq(false), same(getContext())))
        .andReturn(true).anyTimes();
    getWikiMock().saveDocument(anyObject(XWikiDocument.class), same(getContext()));
    expectLastCall().anyTimes();
    getWikiMock().deleteDocument(anyObject(XWikiDocument.class), same(getContext()));
    expectLastCall().anyTimes();
    expect(this.mockXWikiVersioningStore.getXWikiDocumentArchive(anyObject(XWikiDocument.class),
        same(getContext()))).andReturn(null).anyTimes();
    expect(this.mockXWikiMessageTool.get(anyString())).andReturn("message").anyTimes();
    expect(this.mockXWikiRightService.hasProgrammingRights(same(getContext())))
        .andReturn(true).anyTimes();
    replayDefault();
  }

  @Test
  public void test_constructor() {
    DocumentReference defaultReference = new DocumentReference("xwiki", "Main", "WebHome");

    XWikiDocument doc = new XWikiDocument(null);
    assertEquals(defaultReference, doc.getDocumentReference());

    doc = new XWikiDocument();
    assertEquals(defaultReference, doc.getDocumentReference());

    doc = new XWikiDocument("notused", "space.page");
    assertEquals("space", doc.getSpaceName());
    assertEquals("page", doc.getPageName());
    assertEquals("xwiki", doc.getWikiName());

    doc = new XWikiDocument("space", "page");
    assertEquals("space", doc.getSpaceName());
    assertEquals("page", doc.getPageName());
    assertEquals("xwiki", doc.getWikiName());

    doc = new XWikiDocument("wiki", "space", "page");
    assertEquals("space", doc.getSpaceName());
    assertEquals("page", doc.getPageName());
    assertEquals("wiki", doc.getWikiName());

    doc = new XWikiDocument("wiki", "notused", "notused:space.page");
    assertEquals("space", doc.getSpaceName());
    assertEquals("page", doc.getPageName());
    assertEquals("wiki", doc.getWikiName());
  }

  @Test
  public void test_getDisplayTitleWhenNoTitleAndNoContent() {
    this.document.setContent("Some content");

    assertEquals("Page", this.document.getDisplayTitle(getContext()));
  }

  @Test
  public void test_getDisplayWhenTitleExists() {
    this.document.setContent("Some content");
    this.document.setTitle("Title");
    expect(this.mockXWikiRenderingEngine.interpretText(EasyMock.eq("Title"),
        EasyMock.isA(XWikiDocument.class), EasyMock.isA(XWikiContext.class))).andReturn("Title")
        .once();
    replayDefaults();
    assertEquals("Title", this.document.getDisplayTitle(getContext()));
    verifyDefaults();
  }

  @Test
  public void test_getDisplayWhenNoTitleButSectionExists() {
    this.document.setContent("Some content\n1 Title");
    expect(this.mockXWikiRenderingEngine.interpretText(EasyMock.eq("Title"),
        EasyMock.isA(XWikiDocument.class), EasyMock.isA(XWikiContext.class))).andReturn("Title")
        .once();
    replayDefaults();
    assertEquals("Title", this.document.getDisplayTitle(getContext()));
    verifyDefaults();
  }

  /**
   * Verify that if an error happens when evaluation the title, we fallback to the computed title.
   */
  @Test
  public void test_getDisplayTitleWhenVelocityError() throws Exception {
    this.document.setContent("Some content");
    this.document.setTitle("some content that generate a velocity error");
    expect(this.mockXWikiRenderingEngine.interpretText(EasyMock.isA(String.class),
        EasyMock.isA(XWikiDocument.class), EasyMock.isA(XWikiContext.class)))
        .andReturn("... blah blah ... <div id=\"xwikierror105\" ... blah blah ...")
        .once();
    replayDefaults();
    assertEquals("Page", this.document.getDisplayTitle(getContext()));
    verifyDefaults();
  }

  @Test
  public void test_minorMajorVersions() {
    // there is no version in doc yet, so 1.1
    assertEquals("1.1", this.document.getVersion());

    this.document.setMinorEdit(false);
    this.document.incrementVersion();
    // no version => incrementVersion sets 1.1
    assertEquals("1.1", this.document.getVersion());

    this.document.setMinorEdit(false);
    this.document.incrementVersion();
    // increment major version
    assertEquals("2.1", this.document.getVersion());

    this.document.setMinorEdit(true);
    this.document.incrementVersion();
    // increment minor version
    assertEquals("2.2", this.document.getVersion());
  }

  @Test
  public void test_getPreviousVersion() throws XWikiException {
    XWikiContext context = this.getContext();
    Date now = new Date();
    XWikiDocumentArchive archiveDoc = new XWikiDocumentArchive(this.document.getId());
    this.document.setDocumentArchive(archiveDoc);

    assertEquals("1.1", this.document.getVersion());
    assertNull(this.document.getPreviousVersion());

    this.document.incrementVersion();
    archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
        context);
    assertEquals("1.1", this.document.getVersion());
    assertNull(this.document.getPreviousVersion());

    this.document.setMinorEdit(true);
    this.document.incrementVersion();
    archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
        context);
    assertEquals("1.2", this.document.getVersion());
    assertEquals("1.1", this.document.getPreviousVersion());

    this.document.setMinorEdit(false);
    this.document.incrementVersion();
    archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
        context);
    assertEquals("2.1", this.document.getVersion());
    assertEquals("1.2", this.document.getPreviousVersion());

    this.document.setMinorEdit(true);
    this.document.incrementVersion();
    archiveDoc.updateArchive(this.document, "Admin", now, "", this.document.getRCSVersion(),
        context);
    assertEquals("2.2", this.document.getVersion());
    assertEquals("2.1", this.document.getPreviousVersion());

    archiveDoc.resetArchive();

    assertEquals("2.2", this.document.getVersion());
    assertNull(this.document.getPreviousVersion());
  }

  @Test
  public void test_authorAfterDocumentCopy() throws XWikiException {
    String author = "Albatross";
    this.document.setAuthor(author);
    XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy",
        getContext());

    assertTrue(author.equals(copy.getAuthor()));
  }

  @Test
  public void test_creatorAfterDocumentCopy() throws XWikiException {
    String creator = "Condor";
    this.document.setCreator(creator);
    XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy",
        getContext());

    assertTrue(creator.equals(copy.getCreator()));
  }

  @Test
  public void test_creationDateAfterDocumentCopy() throws Exception {
    Date sourceCreationDate = this.document.getCreationDate();
    Thread.sleep(1000);
    XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy",
        getContext());

    assertTrue(copy.getCreationDate().equals(sourceCreationDate));
  }

  @Test
  public void test_objectGuidsAfterDocumentCopy() throws Exception {
    assertTrue(this.document.getXObjects().size() > 0);

    List<String> originalGuids = new ArrayList<>();
    for (Map.Entry<DocumentReference, List<BaseObject>> entry : this.document.getXObjects()
        .entrySet()) {
      for (BaseObject baseObject : entry.getValue()) {
        originalGuids.add(baseObject.getGuid());
      }
    }

    XWikiDocument copy = this.document.copyDocument(this.document.getName() + " Copy",
        getContext());

    // Verify that the cloned objects have different GUIDs
    for (Map.Entry<DocumentReference, List<BaseObject>> entry : copy.getXObjects().entrySet()) {
      for (BaseObject baseObject : entry.getValue()) {
        assertFalse("Non unique object GUID found!", originalGuids.contains(baseObject.getGuid()));
      }
    }
  }

  @Test
  public void test_relativeObjectReferencesAfterDocumentCopy() throws Exception {
    XWikiDocument copy = this.document.copyDocument(
        new DocumentReference("copywiki", "copyspace", "copypage"),
        getContext());

    // Verify that the XObject's XClass reference points to the target wiki and not the old wiki.
    // This tests the XObject cache.
    DocumentReference targetXClassReference = new DocumentReference("copywiki", DOCSPACE, DOCNAME);
    assertNotNull(copy.getXObject(targetXClassReference));

    // Also verify that actual XObject's reference (not from the cache).
    assertEquals(1, copy.getXObjects().size());
    BaseObject bobject = copy.getXObjects().get(copy.getXObjects().keySet().iterator().next())
        .get(0);
    assertEquals(new DocumentReference("copywiki", DOCSPACE, DOCNAME),
        bobject.getXClassReference());
  }

  @Test
  public void test_cloneNullObjects() throws XWikiException {
    XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", DOCSPACE, DOCNAME));

    EntityReference relativeClassReference = new EntityReference(DOCNAME, EntityType.DOCUMENT,
        new EntityReference(DOCSPACE, EntityType.SPACE));
    DocumentReference classReference = new DocumentReference("wiki", DOCSPACE, DOCNAME);
    DocumentReference duplicatedClassReference = new DocumentReference("otherwiki", DOCSPACE,
        DOCNAME);

    // no object
    XWikiDocument clonedDocument = document.clone();
    assertTrue(clonedDocument.getXObjects().isEmpty());

    XWikiDocument duplicatedDocument = document
        .duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
    assertTrue(duplicatedDocument.getXObjects().isEmpty());

    // 1 null object

    document.addXObject(classReference, null);

    clonedDocument = document.clone();
    assertEquals(1, clonedDocument.getXObjects(classReference).size());
    assertEquals(document.getXObjects(classReference), clonedDocument.getXObjects(classReference));

    duplicatedDocument = document.duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
    assertTrue(duplicatedDocument.getXObjects().isEmpty());

    // 1 null object and 1 object

    BaseObject object = new BaseObject();
    object.setXClassReference(relativeClassReference);
    document.addXObject(object);

    clonedDocument = document.clone();
    assertEquals(2, clonedDocument.getXObjects(classReference).size());
    assertEquals(document.getXObjects(classReference), clonedDocument.getXObjects(classReference));

    duplicatedDocument = document.duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
    assertEquals(2, duplicatedDocument.getXObjects(duplicatedClassReference).size());
  }

  @Test
  public void test_cloneWithAbsoluteClassReference() {
    XWikiDocument document = new XWikiDocument(new DocumentReference("wiki", DOCSPACE, DOCNAME));

    EntityReference relativeClassReference = new EntityReference(DOCNAME, EntityType.DOCUMENT,
        new EntityReference(DOCSPACE, EntityType.SPACE));
    DocumentReference classReference = new DocumentReference("wiki", DOCSPACE, DOCNAME);
    DocumentReference duplicatedClassReference = new DocumentReference("otherwiki", DOCSPACE,
        DOCNAME);

    BaseObject object = new BaseObject();
    object.setXClassReference(relativeClassReference);
    document.addXObject(object);
    BaseObject object2 = new BaseObject();
    object2.setXClassReference(classReference);
    document.addXObject(object2);
    BaseObject object3 = new BaseObject();
    object3.setXClassReference(relativeClassReference);
    document.addXObject(object3);

    XWikiDocument clonedDocument = document.clone();
    assertEquals(3, clonedDocument.getXObjects(classReference).size());
    assertEquals(document.getXObjects(classReference), clonedDocument.getXObjects(classReference));

    XWikiDocument duplicatedDocument = document
        .duplicate(new DocumentReference("otherwiki", DOCSPACE, DOCNAME));
    assertNull(duplicatedDocument.getXObjects(classReference));
    assertNotNull(duplicatedDocument.getXObjects(duplicatedClassReference));
    assertNotNull(duplicatedDocument.getXObject(duplicatedClassReference, 0));
    assertNotNull(duplicatedDocument.getXObject(duplicatedClassReference, 1));
    assertNotNull(duplicatedDocument.getXObject(duplicatedClassReference, 2));
  }

  @Test
  public void test_toStringReturnsFullName() {
    assertEquals("Space.Page", this.document.toString());
    assertEquals("Main.WebHome", new XWikiDocument().toString());
  }

  @Test
  public void test_cloneSaveVersions() {
    XWikiDocument doc1 = new XWikiDocument("qwe", "qwe");
    XWikiDocument doc2 = doc1.clone();
    doc1.incrementVersion();
    doc2.incrementVersion();
    assertEquals(doc1.getVersion(), doc2.getVersion());
  }

  @Test
  public void test_addObject() throws XWikiException {
    XWikiDocument doc = new XWikiDocument("test", "document");
    this.wikiClass = new BaseClass();
    BaseObject object = BaseClass.newCustomClassInstance("XWiki.XWikiUsers", getContext());
    doc.addObject("XWiki.XWikiUsers", object);
    assertEquals("XWikiDocument.addObject does not set the object's name", doc.getFullName(),
        object.getName());
  }

  @Test
  public void test_objectNumbersAfterXMLRoundrip() throws XWikiException {
    String classname = XWikiConstant.TAG_CLASS;
    BaseClass tagClass = new BaseClass();
    tagClass.setName(classname);
    tagClass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, "",
        "checkbox");

    XWikiDocument doc = new XWikiDocument("test", "document");
    this.wikiXClass = tagClass;
    this.wikiEncoding = "iso-8859-1";

    BaseObject object = BaseClass.newCustomClassInstance(classname, getContext());
    object.setClassName(classname);
    doc.addObject(classname, object);

    object = BaseClass.newCustomClassInstance(classname, getContext());
    object.setClassName(classname);
    doc.addObject(classname, object);

    object = BaseClass.newCustomClassInstance(classname, getContext());
    object.setClassName(classname);
    doc.addObject(classname, object);

    doc.setObject(classname, 1, null);

    String docXML = doc.toXML(getContext());
    XWikiDocument docFromXML = new XWikiDocument();
    docFromXML.fromXML(docXML);

    Vector<BaseObject> objects = doc.getObjects(classname);
    Vector<BaseObject> objectsFromXML = docFromXML.getObjects(classname);

    assertNotNull(objects);
    assertNotNull(objectsFromXML);

    assertTrue(objects.size() == objectsFromXML.size());

    for (int i = 0; i < objects.size(); i++) {
      if (objects.get(i) == null) {
        assertNull(objectsFromXML.get(i));
      } else {
        assertTrue(objects.get(i).getNumber() == objectsFromXML.get(i).getNumber());
      }
    }
  }

  @Test
  public void test_getUniqueLinkedPages10() {
    XWikiDocument contextDocument = new XWikiDocument("contextdocspace", "contextdocpage");
    getContext().setDoc(contextDocument);

    this.document.setContent("[TargetPage][TargetLabel>TargetPage][TargetSpace.TargetPage]"
        +
        "[TargetLabel>TargetSpace.TargetPage?param=value#anchor][http://externallink][mailto:mailto][label>]");

    Set<String> linkedPages = this.document.getUniqueLinkedPages(getContext());

    assertEquals(new HashSet<>(Arrays.asList("TargetPage", "TargetSpace.TargetPage")),
        new HashSet<>(
            linkedPages));
  }

  @Test
  public void test_getSections10() throws XWikiException {
    this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
        + "1.1 header 2\nheader 2 content");

    List<DocumentSection> headers = this.document.getSections();

    assertEquals(2, headers.size());

    DocumentSection header1 = headers.get(0);
    DocumentSection header2 = headers.get(1);

    assertEquals("header 1", header1.getSectionTitle());
    assertEquals(23, header1.getSectionIndex());
    assertEquals(1, header1.getSectionNumber());
    assertEquals("1", header1.getSectionLevel());
    assertEquals("header 2", header2.getSectionTitle());
    assertEquals(51, header2.getSectionIndex());
    assertEquals(2, header2.getSectionNumber());
    assertEquals("1.1", header2.getSectionLevel());
  }

  @Test
  public void test_getDocumentSection10() throws XWikiException {
    this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
        + "1.1 header 2\nheader 2 content");

    DocumentSection header1 = this.document.getDocumentSection(1);
    DocumentSection header2 = this.document.getDocumentSection(2);

    assertEquals("header 1", header1.getSectionTitle());
    assertEquals(23, header1.getSectionIndex());
    assertEquals(1, header1.getSectionNumber());
    assertEquals("1", header1.getSectionLevel());
    assertEquals("header 2", header2.getSectionTitle());
    assertEquals(51, header2.getSectionIndex());
    assertEquals(2, header2.getSectionNumber());
    assertEquals("1.1", header2.getSectionLevel());
  }

  @Test
  public void test_getContentOfSection10() throws XWikiException {
    this.document.setContent("content not in section\n" + "1 header 1\nheader 1 content\n"
        + "1.1 header 2\nheader 2 content");

    String content1 = this.document.getContentOfSection(1);
    String content2 = this.document.getContentOfSection(2);

    assertEquals("1 header 1\nheader 1 content\n1.1 header 2\nheader 2 content", content1);
    assertEquals("1.1 header 2\nheader 2 content", content2);
  }

  @Test
  public void test_sectionSplit10() throws XWikiException {
    List<DocumentSection> sections;
    // Simple test
    this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
        + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
    sections = this.document.getSections();
    assertEquals(3, sections.size());
    assertEquals("Section 1", sections.get(0).getSectionTitle());
    assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
        + "Content of second section\n", this.document.getContentOfSection(1));
    assertEquals("1.1", sections.get(1).getSectionLevel());
    assertEquals("1.1 Subsection 2\nContent of second section\n",
        this.document.getContentOfSection(2));
    assertEquals(3, sections.get(2).getSectionNumber());
    assertEquals(80, sections.get(2).getSectionIndex());
    assertEquals("1 Section 3\nContent of section 3", this.document.getContentOfSection(3));
    // Test comments don't break the section editing
    this.document
        .setContent("1 Section 1\n" + "Content of first section\n" + "## 1.1 Subsection 2\n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
    sections = this.document.getSections();
    assertEquals(2, sections.size());
    assertEquals("Section 1", sections.get(0).getSectionTitle());
    assertEquals("1", sections.get(1).getSectionLevel());
    assertEquals(2, sections.get(1).getSectionNumber());
    assertEquals(83, sections.get(1).getSectionIndex());
    // Test spaces are ignored
    this.document
        .setContent("1 Section 1\n" + "Content of first section\n" + " 1.1 Subsection 2 \n"
            + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
    sections = this.document.getSections();
    assertEquals(3, sections.size());
    assertEquals("Subsection 2 ", sections.get(1).getSectionTitle());
    assertEquals("1.1", sections.get(1).getSectionLevel());
    // Test lower headings are ignored
    this.document
        .setContent("1 Section 1\n" + "Content of first section\n" + "1.1.1 Lower subsection\n"
            + "This content is not important\n" + " 1.1 Subsection 2 \n"
            + "Content of second section\n"
            + "1 Section 3\n" + "Content of section 3");
    sections = this.document.getSections();
    assertEquals(3, sections.size());
    assertEquals("Section 1", sections.get(0).getSectionTitle());
    assertEquals("Subsection 2 ", sections.get(1).getSectionTitle());
    assertEquals("1.1", sections.get(1).getSectionLevel());
    // Test blank lines are preserved
    this.document.setContent("\n\n1 Section 1\n\n\n" + "Content of first section\n\n\n"
        + " 1.1 Subsection 2 \n\n" + "Content of second section\n" + "1 Section 3\n"
        + "Content of section 3");
    sections = this.document.getSections();
    assertEquals(3, sections.size());
    assertEquals(2, sections.get(0).getSectionIndex());
    assertEquals("Subsection 2 ", sections.get(1).getSectionTitle());
    assertEquals(43, sections.get(1).getSectionIndex());
  }

  @Test
  public void test_updateDocumentSection10() throws XWikiException {
    List<DocumentSection> sections;
    // Fill the document
    this.document.setContent("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
        + "Content of second section\n" + "1 Section 3\n" + "Content of section 3");
    String content = this.document.updateDocumentSection(3,
        "1 Section 3\n" + "Modified content of section 3");
    assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
        + "Content of second section\n" + "1 Section 3\n" + "Modified content of section 3",
        content);
    this.document.setContent(content);
    sections = this.document.getSections();
    assertEquals(3, sections.size());
    assertEquals("Section 1", sections.get(0).getSectionTitle());
    assertEquals("1 Section 1\n" + "Content of first section\n" + "1.1 Subsection 2\n"
        + "Content of second section\n", this.document.getContentOfSection(1));
    assertEquals("1.1", sections.get(1).getSectionLevel());
    assertEquals("1.1 Subsection 2\nContent of second section\n",
        this.document.getContentOfSection(2));
    assertEquals(3, sections.get(2).getSectionNumber());
    assertEquals(80, sections.get(2).getSectionIndex());
    assertEquals("1 Section 3\nModified content of section 3",
        this.document.getContentOfSection(3));
  }

  @Test
  public void test_getRenderedContentWithSourceSyntax() throws XWikiException {
    this.document.setSyntaxId("xwiki/1.0");
    String inputText = "**bold**";
    expect(mockXWikiRenderingEngine.renderText(EasyMock.eq(inputText),
        EasyMock.isA(XWikiDocument.class), EasyMock.same(getContext()))).andReturn("**bold**")
        .once();
    replayDefaults();
    assertEquals(inputText,
        this.document.getRenderedContent(inputText, "xwiki/2.0",
            getContext()));
    verifyDefaults();
  }

  /**
   * Validate rename does not crash when the document has 1.0 syntax (it does not support
   * everything
   * but it does not crash).
   */
  @Test
  public void test_rename10() throws XWikiException {
    DocumentReference sourceReference = new DocumentReference(this.document.getDocumentReference());
    this.document.setContent("[pageinsamespace]");
    this.document.setSyntax(Syntax.XWIKI_1_0);
    DocumentReference targetReference = new DocumentReference("newwikiname", "newspace",
        "newpage");
    XWikiDocument targetDocument = this.document.duplicate(targetReference);
    this.wikiDocument = targetDocument;

    this.document.rename(new DocumentReference("newwikiname", "newspace", "newpage"),
        Collections.<DocumentReference>emptyList(),
        Collections.<DocumentReference>emptyList(), getContext());

    // Test links
    assertEquals("[pageinsamespace]", this.document.getContent());
  }

  /**
   * Normally the xobject vector has the Nth object on the Nth position, but in case an object
   * gets
   * misplaced, trying to remove it should indeed remove that object, and no other.
   */
  @Test
  public void test_removingObjectWithWrongObjectVector() {
    // Setup: Create a document and two xobjects
    BaseObject o1 = new BaseObject();
    BaseObject o2 = new BaseObject();
    o1.setClassName(CLASSNAME);
    o2.setClassName(CLASSNAME);

    // Test: put the second xobject on the third position
    // addObject creates the object vector and configures the objects
    // o1 is added at position 0
    // o2 is added at position 1
    XWikiDocument doc = new XWikiDocument();
    doc.addObject(CLASSNAME, o1);
    doc.addObject(CLASSNAME, o2);

    // Modify the o2 object's position to ensure it can still be found and removed by the
    // removeObject method.
    assertEquals(1, o2.getNumber());
    o2.setNumber(0);
    // Set a field on o1 so that when comparing it with o2 they are different. This is needed so
    // that the remove
    // will pick the right object to remove (since we've voluntarily set a wrong number of o2 it
    // would pick o1
    // if they were equals).
    o1.addField("somefield", new StringProperty());

    // Call the tested method, removing o2 from position 2 which is set to null
    boolean result = doc.removeObject(o2);

    // Check the correct behavior:
    assertTrue(result);
    Vector<BaseObject> objects = doc.getObjects(CLASSNAME);
    assertTrue(objects.contains(o1));
    assertFalse(objects.contains(o2));
    assertNull(objects.get(1));

    // Second test: swap the two objects, so that the first object is in the position the second
    // should have
    // Start over, re-adding the two objects
    doc = new XWikiDocument();
    doc.addObject(CLASSNAME, o1);
    doc.addObject(CLASSNAME, o2);
  }

  @Test
  public void test_copyDocument() throws XWikiException {
    XWikiDocument doc = new XWikiDocument();
    BaseObject o = new BaseObject();
    o.setClassName(CLASSNAME);
    doc.addObject(CLASSNAME, o);

    XWikiDocument newDoc = doc.copyDocument("newdoc", getContext());
    BaseObject newO = newDoc.getObject(CLASSNAME);

    assertNotSame(o, newDoc.getObject(CLASSNAME));
    assertFalse(newO.getGuid().equals(o.getGuid()));
  }

  @Test
  public void test_resolveClassReference() throws Exception {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

    DocumentReference expected1 = new DocumentReference("docwiki", "XWiki", "docpage");
    assertEquals(expected1, doc.resolveClassReference(""));

    DocumentReference expected2 = new DocumentReference("docwiki", "XWiki", "page");
    assertEquals(expected2, doc.resolveClassReference("page"));

    DocumentReference expected3 = new DocumentReference("docwiki", "space", "page");
    assertEquals(expected3, doc.resolveClassReference("space.page"));

    DocumentReference expected4 = new DocumentReference("wiki", "space", "page");
    assertEquals(expected4, doc.resolveClassReference("wiki:space.page"));
  }

  /**
   * Test that the parent remain the same relative value whatever the context.
   */
  @Test
  public void test_getParent() {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

    assertEquals("", doc.getParent());
    doc.setParent(null);
    assertEquals("", doc.getParent());

    doc.setParent("page");
    assertEquals("page", doc.getParent());

    getContext().setDatabase("otherwiki");
    assertEquals("page", doc.getParent());

    doc.setDocumentReference(new DocumentReference("otherwiki", "otherspace", "otherpage"));
    assertEquals("page", doc.getParent());
  }

  @Test
  public void test_getParentReference() {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

    assertNull(doc.getParentReference());

    doc.setParent("parentpage");

    //////////////////////////////////////////////////////////////////
    // The following tests are checking that document reference cache is properly cleaned something
    ////////////////////////////////////////////////////////////////// could make the
    // parent change

    assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"),
        doc.getParentReference());

    doc.setName("docpage2");
    assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"),
        doc.getParentReference());

    doc.setSpace("docspace2");
    assertEquals(new DocumentReference("docwiki", "docspace2", "parentpage"),
        doc.getParentReference());

    doc.setDatabase("docwiki2");
    assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage"),
        doc.getParentReference());

    doc.setDocumentReference(new DocumentReference("docwiki", "docspace", "docpage"));
    assertEquals(new DocumentReference("docwiki", "docspace", "parentpage"),
        doc.getParentReference());

    doc.setFullName("docwiki2:docspace2.docpage2", getContext());
    assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage"),
        doc.getParentReference());

    doc.setParent("parentpage2");
    assertEquals(new DocumentReference("docwiki2", "docspace2", "parentpage2"),
        doc.getParentReference());
  }

  @Test
  public void test_setAbsoluteParentReference() {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

    doc.setParentReference(new DocumentReference("docwiki", "docspace", "docpage2"));
    assertEquals("docspace.docpage2", doc.getParent());
  }

  @Test
  public void test_setRelativeParentReference() {
    XWikiDocument doc = new XWikiDocument(new DocumentReference("docwiki", "docspace", "docpage"));

    doc.setParentReference(new EntityReference("docpage2", EntityType.DOCUMENT));
    assertEquals(new DocumentReference("docwiki", "docspace", "docpage2"),
        doc.getParentReference());
    assertEquals("docpage2", doc.getParent());
  }

  /**
   * Verify that cloning objects modify their references to point to the document in which they
   * are
   * cloned into.
   */
  @Test
  public void test_cloneObjectsHaveCorrectReference() {
    XWikiDocument doc = new XWikiDocument(
        new DocumentReference("somewiki", "somespace", "somepage"));
    doc.cloneXObjects(this.document);
    assertTrue(doc.getXObjects().size() > 0);

    // Verify that the object references point to the doc in which it's cloned.
    for (Map.Entry<DocumentReference, List<BaseObject>> entry : doc.getXObjects().entrySet()) {
      for (BaseObject baseObject : entry.getValue()) {
        assertEquals(doc.getDocumentReference(), baseObject.getDocumentReference());
      }
    }
  }

  /**
   * Verify that merging objects modify their references to point to the document in which they
   * are
   * cloned into and that GUID fors merged objects are different from the original GUIDs.
   */
  @Test
  public void test_mergeObjectsHaveCorrectReferenceAndDifferentGuids() {
    List<String> originalGuids = new ArrayList<>();
    for (Map.Entry<DocumentReference, List<BaseObject>> entry : this.document.getXObjects()
        .entrySet()) {
      for (BaseObject baseObject : entry.getValue()) {
        originalGuids.add(baseObject.getGuid());
      }
    }

    XWikiDocument doc = new XWikiDocument(
        new DocumentReference("somewiki", "somespace", "somepage"));
    doc.mergeXObjects(this.document);

    assertTrue(doc.getXObjects().size() > 0);

    // Verify that the object references point to the doc in which it's cloned.
    // Verify that GUIDs are not the same as the original ones
    for (Map.Entry<DocumentReference, List<BaseObject>> entry : doc.getXObjects().entrySet()) {
      for (BaseObject baseObject : entry.getValue()) {
        assertEquals(doc.getDocumentReference(), baseObject.getDocumentReference());
        assertFalse("Non unique object GUID found!", originalGuids.contains(baseObject.getGuid()));
      }
    }
  }

  /**
   * Verify that no ConcurrentModificationException is thrown, see CELDEV-725
   */
  @Test
  public void test_mergeObjectsConcurrentModificationException() throws Exception {
    XWikiDocument doc = new XWikiDocument(
        new DocumentReference("somewiki", "somespace", "somepage"));
    doc.newObject(CLASSNAME, getContext());
    doc.mergeXObjects(this.document);
    assertEquals(2, doc.getObjects(CLASSNAME).size());
  }

  /** Check that a new empty document has empty content (used to have a new line before 2.5). */
  @Test
  public void test_initialContent() {
    XWikiDocument doc = new XWikiDocument(
        new DocumentReference("somewiki", "somespace", "somepage"));
    assertEquals("", doc.getContent());
  }

  private void replayDefaults() {
    EasyMock.replay(mockXWikiRenderingEngine);
  }

  private void verifyDefaults() {
    EasyMock.verify(mockXWikiRenderingEngine);
  }

}
