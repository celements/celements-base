package com.celements.filebase;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.TestMessageTool;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.filebase.matcher.IAttFileNameMatcherRole;
import com.celements.filebase.matcher.IAttachmentMatcher;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.web.Utils;

public class AttachmentServiceTest extends AbstractComponentTest {

  private AttachmentService attService;
  private XWikiDocument doc;

  @Before
  public void setUp_AttachmentServiceTest() throws ComponentLookupException, Exception {
    registerComponentMocks(IModelAccessFacade.class, IRightsAccessFacadeRole.class);
    registerComponentMock(ConfigurationSource.class, "all", getConfigurationSource());
    registerComponentMock(ConfigurationSource.class, CelementsFromWikiConfigurationSource.NAME,
        getConfigurationSource());
    attService = (AttachmentService) Utils.getComponent(IAttachmentServiceRole.class);
    doc = new XWikiDocument(new DocumentReference("db", "space", "doc"));
    doc.setMetaDataDirty(false);
    doc.setNew(false);
    doc.setSyntax(Syntax.XWIKI_1_0);
  }

  @Test
  public void test_clearFileName_empty() {
    String name = "";
    assertEquals(name, attService.clearFileName(name));
  }

  @Test
  public void test_clearFileName_clean() {
    String name = "abc.jpg";
    assertEquals(name, attService.clearFileName(name));
  }

  @Test
  public void test_clearFileName_minus() {
    String name = "abc-123.jpg";
    assertEquals(name, attService.clearFileName(name));
  }

  @Test
  public void test_clearFileName_space() {
    String name = "abc 123.jpg";
    String target = "abc-123.jpg";
    assertEquals(target, attService.clearFileName(name));
  }

  @Test
  public void test_clearFileName_underscore() {
    String name = "abc_123.jpg";
    assertEquals(name, attService.clearFileName(name));
  }

  @Test
  public void test_deleteAttachmentList_null() {
    assertEquals(0, attService.deleteAttachmentList(null));
  }

  @Test
  public void test_deleteAttachmentList_empty() {
    assertEquals(0, attService.deleteAttachmentList(new ArrayList<AttachmentReference>()));
  }

  @Test
  public void test_deleteAttachmentList_list() throws Exception {
    String name1 = "file1.jpg";
    String name2 = "file2.txt";
    String name3 = "file3.png";
    String names = name1 + ", " + name2 + ", " + name3;
    DocumentReference docRef = new DocumentReference(getContext().getDatabase(), "Space", "Doc");
    ArrayList<AttachmentReference> attList = new ArrayList<>();
    attList.add(new AttachmentReference(name1, docRef));
    attList.add(new AttachmentReference(name2, docRef));
    attList.add(new AttachmentReference(name3, docRef));
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setNew(false);
    doc.setSyntax(Syntax.XWIKI_1_0);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef)).andReturn(doc).once();
    String comment = "deleted attachments " + names;
    ((TestMessageTool) getContext().getMessageTool()).injectMessage("core.comment."
        + "deleteAttachmentComment", Arrays.asList(names), comment);
    Capture<XWikiDocument> savedDocCapture = newCapture();
    getMock(IModelAccessFacade.class).saveDocument(capture(savedDocCapture), isA(String.class));
    XWikiAttachment att1 = new XWikiAttachment(doc, name1);
    doc.getAttachmentList().add(att1);
    XWikiAttachment att2 = new XWikiAttachment(doc, name2);
    doc.getAttachmentList().add(att2);
    XWikiAttachment att3 = new XWikiAttachment(doc, name3);
    doc.getAttachmentList().add(att3);
    expect(getWikiMock().hasAttachmentRecycleBin(same(getContext()))).andReturn(false).anyTimes();
    XWikiAttachmentStoreInterface store = createDefaultMock(
        XWikiAttachmentStoreInterface.class);
    expect(getWikiMock().getAttachmentStore()).andReturn(store).anyTimes();
    Capture<XWikiAttachment> attCaptures = newCapture(CaptureType.ALL);
    store.deleteXWikiAttachment(capture(attCaptures), same(getContext()), eq(true));
    expectLastCall().times(3);
    replayDefault();
    assertEquals(3, attService.deleteAttachmentList(attList));
    verifyDefault();
    List<XWikiAttachment> delAttList = attCaptures.getValues();
    assertEquals(3, delAttList.size());
    for (XWikiAttachment delAtt : delAttList) {
      assertTrue(attList.contains(new AttachmentReference(delAtt.getFilename(),
          delAtt.getDoc().getDocumentReference())));
    }
    XWikiDocument savedDoc = savedDocCapture.getValue();
    assertEquals(getContext().getUser(), savedDoc.getAuthor());
    assertEquals(comment, savedDoc.getComment());
  }

  @Test
  public void test_deleteAttachmentList_list_with_nonexistant() throws Exception {
    String name1 = "file1.jpg";
    String name3 = "file3.png";
    String name5 = "file5.txt";
    String names = name3 + ", " + name5;
    String name2 = "file2.txt";
    String name4 = "file4.zip";
    String names2 = name2 + ", " + name4;
    DocumentReference docRef = new DocumentReference(getContext().getDatabase(), "Space", "Doc");
    DocumentReference docRef2 = new DocumentReference(getContext().getDatabase(), "Space", "Doc2");
    ArrayList<AttachmentReference> attList = new ArrayList<>();
    attList.add(new AttachmentReference(name1, docRef));
    attList.add(new AttachmentReference(name2, docRef2));
    attList.add(new AttachmentReference(name3, docRef));
    attList.add(new AttachmentReference(name4, docRef2));
    attList.add(new AttachmentReference(name5, docRef));
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setNew(false);
    doc.setSyntax(Syntax.XWIKI_1_0);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef)).andReturn(doc).once();
    String comment = "deleted attachments " + names;
    ((TestMessageTool) getContext().getMessageTool()).injectMessage("core.comment."
        + "deleteAttachmentComment", Arrays.asList(names), comment);
    Capture<XWikiDocument> savedDocsCapture = newCapture(CaptureType.ALL);
    getMock(IModelAccessFacade.class).saveDocument(capture(savedDocsCapture), isA(String.class));
    expectLastCall().atLeastOnce();
    XWikiDocument doc2 = new XWikiDocument(docRef2);
    doc2.setNew(false);
    doc2.setSyntax(Syntax.XWIKI_1_0);
    expect(getMock(IModelAccessFacade.class).getDocument(docRef2)).andReturn(doc2).once();
    String comment2 = "deleted attachments " + names2;
    ((TestMessageTool) getContext().getMessageTool()).injectMessage("core.comment."
        + "deleteAttachmentComment", Arrays.asList(names2), comment2);
    XWikiAttachment att2 = new XWikiAttachment(doc2, name2);
    doc2.getAttachmentList().add(att2);
    XWikiAttachment att3 = new XWikiAttachment(doc, name3);
    doc.getAttachmentList().add(att3);
    XWikiAttachment att4 = new XWikiAttachment(doc2, name4);
    doc2.getAttachmentList().add(att4);
    XWikiAttachment att5 = new XWikiAttachment(doc, name5);
    doc.getAttachmentList().add(att5);
    expect(getWikiMock().hasAttachmentRecycleBin(same(getContext()))).andReturn(false).anyTimes();
    XWikiAttachmentStoreInterface store = createDefaultMock(
        XWikiAttachmentStoreInterface.class);
    expect(getWikiMock().getAttachmentStore()).andReturn(store).anyTimes();
    Capture<XWikiAttachment> attCaptures = newCapture(CaptureType.ALL);
    store.deleteXWikiAttachment(capture(attCaptures), same(getContext()), eq(true));
    expectLastCall().anyTimes();
    replayDefault();
    assertEquals(4, attService.deleteAttachmentList(attList));
    verifyDefault();
    List<XWikiAttachment> delAttList = attCaptures.getValues();
    assertEquals(4, delAttList.size());
    for (XWikiAttachment delAtt : delAttList) {
      AttachmentReference attRef = new AttachmentReference(delAtt.getFilename(),
          delAtt.getDoc().getDocumentReference());
      assertTrue("attRef not found." + attRef, attList.contains(attRef));
    }
    List<XWikiDocument> savedDocsList = savedDocsCapture.getValues();
    assertEquals(2, savedDocsList.size());
    XWikiDocument savedDoc;
    XWikiDocument savedDoc2;
    if (savedDocsList.get(0).getDocumentReference().equals(docRef)) {
      savedDoc = savedDocsList.get(0);
      savedDoc2 = savedDocsList.get(1);
    } else {
      savedDoc = savedDocsList.get(1);
      savedDoc2 = savedDocsList.get(0);
    }
    assertEquals(getContext().getUser(), savedDoc.getAuthor());
    assertEquals(comment, savedDoc.getComment());
    assertEquals(getContext().getUser(), savedDoc2.getAuthor());
    assertEquals(comment2, savedDoc2.getComment());
  }

  @Test
  public void test_buildAttachmentsToDeleteMap() {
    DocumentReference docRef = new DocumentReference(getContext().getDatabase(), "Space", "Doc");
    DocumentReference docRef2 = new DocumentReference(getContext().getDatabase(), "Space", "Doc2");
    ArrayList<AttachmentReference> attList = new ArrayList<>();
    attList.add(new AttachmentReference("file1.jpg", docRef));
    attList.add(new AttachmentReference("file2.txt", docRef2));
    attList.add(new AttachmentReference("file3.png", docRef));
    attList.add(new AttachmentReference("file4.zip", docRef2));
    attList.add(new AttachmentReference("file5.txt", docRef));
    Map<DocumentReference, List<String>> attMap = attService.buildAttachmentsToDeleteMap(attList);
    assertEquals(2, attMap.keySet().size());
    assertEquals(3, attMap.get(docRef).size());
    assertEquals(2, attMap.get(docRef2).size());
  }

  @Test
  public void test_addAttachment() throws Exception {
    XWikiAttachment att = new XWikiAttachment();
    String comment = "comment";
    String filename = "file.txt";
    String author = "XWiki.test";
    att.setFilename(filename);
    doc.setAttachmentList(Arrays.asList(att));
    getMock(IModelAccessFacade.class).saveDocument(same(doc), eq(comment));
    replayDefault();
    XWikiAttachment retAtt = attService.addAttachment(doc, new byte[] { 'x' }, filename, author,
        comment);
    assertSame(att, retAtt);
    verifyDefault();
    assertEquals(filename, retAtt.getFilename());
    assertTrue(doc.isContentDirty());
    assertTrue(doc.isMetaDataDirty());
    assertEquals(author, doc.getAuthor());
  }

  @Test
  public void test_addAttachment_newFileNameIsPrefixOfExisting() throws Exception {
    String comment = "comment";
    String filename = "file.txt";
    String author = "XWiki.test";
    List<XWikiAttachment> attList = new ArrayList<>();
    XWikiAttachment att = new XWikiAttachment();
    att.setFilename(filename + ".zip");
    attList.add(att);
    doc.setAttachmentList(attList);
    getMock(IModelAccessFacade.class).saveDocument((XWikiDocument) anyObject(), eq(comment));
    replayDefault();
    XWikiAttachment retAtt = attService.addAttachment(doc, new byte[] { 'x' }, filename, author,
        comment);
    verifyDefault();
    assertEquals(2, retAtt.getDoc().getAttachmentList().size());
    assertEquals(filename, retAtt.getFilename());
  }

  @Test
  public void test_getAttachmentNameEqual() throws Exception {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    replayDefault();
    XWikiAttachment att = attService.getAttachmentNameEqual(doc, filename);
    verifyDefault();
    assertNotNull("Expected image.jpg attachment - not null", att);
    assertEquals(filename, att.getFilename());
  }

  @Test
  public void test_getAttachmentNameEqual_not_exists() {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    replayDefault();
    assertThrows(AttachmentNotExistsException.class,
        () -> attService.getAttachmentNameEqual(doc, filename));
    verifyDefault();
  }

  @Test
  public void test_getAttachmentNameEqual_attRef() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andReturn(doc).once();
    replayDefault();
    XWikiAttachment att = attService.getAttachmentNameEqual(attRef);
    verifyDefault();
    assertNotNull("Expected image.jpg attachment - not null", att);
    assertEquals(filename, att.getFilename());
  }

  @Test
  public void test_getAttachmentNameEqual_attRef_not_exists() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andReturn(doc).once();
    replayDefault();
    assertThrows(AttachmentNotExistsException.class,
        () -> attService.getAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_getAttachmentNameEqual_attRef_doc_not_exists() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    doc.setNew(true);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andThrow(new DocumentNotExistsException(doc.getDocumentReference()));
    replayDefault();
    assertThrows(DocumentNotExistsException.class, () -> attService.getAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual() {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    replayDefault();
    assertTrue(attService.existsAttachmentNameEqual(doc, filename));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual_not_exists() {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    replayDefault();
    assertFalse(attService.existsAttachmentNameEqual(doc, filename));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual_attRef() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andReturn(doc).once();
    replayDefault();
    assertTrue(attService.existsAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual_attRef_doc_not_exists() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    doc.setNew(true);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andReturn(doc).once();
    replayDefault();
    assertFalse(attService.existsAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual_attRef_not_exists() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andReturn(doc).once();
    replayDefault();
    assertFalse(attService.existsAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_existsAttachmentNameEqual_attRef_docLoadExp() throws Exception {
    String filename = "image.jpg";
    AttachmentReference attRef = new AttachmentReference(filename, doc.getDocumentReference());
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    expect(getMock(IModelAccessFacade.class).getDocument(doc.getDocumentReference()))
        .andThrow(new DocumentLoadException(doc.getDocumentReference())).once();
    replayDefault();
    assertThrows(DocumentLoadException.class, () -> attService.existsAttachmentNameEqual(attRef));
    verifyDefault();
  }

  @Test
  public void test_getAttachmentFirstNameMatch() throws AttachmentNotExistsException {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    IAttFileNameMatcherRole mockMatcher = createDefaultMock(IAttFileNameMatcherRole.class);
    expect(mockMatcher.getFileNamePattern()).andReturn(filename).anyTimes();
    expect(mockMatcher.accept(firstAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(imageAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(lastAtt)).andReturn(false).anyTimes();
    replayDefault();
    XWikiAttachment resFirstImage;
    resFirstImage = attService.getAttachmentFirstNameMatch(doc, mockMatcher);
    verifyDefault();
    assertNotNull("Expected emptyList - not null", resFirstImage);
    assertEquals(filename, resFirstImage.getFilename());
  }

  @Test
  public void test_getAttachmentFirstNameMatch_reverseOrder() throws AttachmentNotExistsException {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    IAttFileNameMatcherRole mockMatcher = createDefaultMock(IAttFileNameMatcherRole.class);
    expect(mockMatcher.getFileNamePattern()).andReturn(filename).anyTimes();
    expect(mockMatcher.accept(firstAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(imageAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(lastAtt)).andReturn(false).anyTimes();
    replayDefault();
    XWikiAttachment resFirstImage;
    resFirstImage = attService.getAttachmentFirstNameMatch(doc, mockMatcher);
    verifyDefault();
    assertNotNull("Expected emptyList - not null", resFirstImage);
    assertEquals(filename, resFirstImage.getFilename());
  }

  @Test
  public void test_getAttachmentFirstNameMatch_empty() {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, "bli.zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, "other.file");
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    IAttFileNameMatcherRole mockMatcher = createDefaultMock(IAttFileNameMatcherRole.class);
    expect(mockMatcher.getFileNamePattern()).andReturn(filename).anyTimes();
    expect(mockMatcher.accept(firstAtt)).andReturn(false).anyTimes();
    expect(mockMatcher.accept(imageAtt)).andReturn(false).anyTimes();
    expect(mockMatcher.accept(lastAtt)).andReturn(false).anyTimes();
    replayDefault();
    assertThrows(AttachmentNotExistsException.class,
        () -> attService.getAttachmentFirstNameMatch(doc, mockMatcher));
    verifyDefault();
  }

  @Test
  public void test_getAttachmentsNameMatch() throws Exception {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, imageAtt, lastAtt);
    doc.setAttachmentList(attList);
    IAttachmentMatcher mockMatcher = createDefaultMock(IAttachmentMatcher.class);
    expect(mockMatcher.accept(firstAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(imageAtt)).andReturn(true).anyTimes();
    expect(mockMatcher.accept(lastAtt)).andReturn(false).anyTimes();
    replayDefault();
    List<XWikiAttachment> resAttList = attService.getAttachmentsNameMatch(doc, mockMatcher);
    verifyDefault();
    assertNotNull("Expected emptyList - not null", resAttList);
    assertEquals(2, resAttList.size());
    XWikiAttachment resFirstImage = resAttList.get(0);
    assertEquals(firstAtt.getFilename(), resFirstImage.getFilename());
    XWikiAttachment resSecondImage = resAttList.get(1);
    assertEquals(imageAtt.getFilename(), resSecondImage.getFilename());
  }

  @Test
  public void test_getAttachmentsNameMatch_noMatch_emptyList() {
    String filename = "image.jpg";
    XWikiAttachment firstAtt = new XWikiAttachment(doc, filename + ".zip");
    XWikiAttachment imageAtt = new XWikiAttachment(doc, filename);
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(firstAtt, lastAtt);
    doc.setAttachmentList(attList);
    IAttachmentMatcher mockMatcher = createDefaultMock(IAttachmentMatcher.class);
    expect(mockMatcher.accept(firstAtt)).andReturn(false).anyTimes();
    expect(mockMatcher.accept(imageAtt)).andReturn(false).anyTimes();
    expect(mockMatcher.accept(lastAtt)).andReturn(false).anyTimes();
    replayDefault();
    List<XWikiAttachment> resAttList = attService.getAttachmentsNameMatch(doc, mockMatcher);
    assertNotNull("Expected emptyList - not null", resAttList);
    assertTrue(resAttList.isEmpty());
    verifyDefault();
  }

  @Test
  public void test_getAttachmentsNameMatch_null_matcher() {
    XWikiAttachment lastAtt = new XWikiAttachment(doc, "bli.gaga");
    List<XWikiAttachment> attList = Arrays.asList(lastAtt);
    doc.setAttachmentList(attList);
    replayDefault();
    List<XWikiAttachment> resAttList = attService.getAttachmentsNameMatch(doc, null);
    assertNotNull("Expected emptyList - not null", resAttList);
    assertTrue(resAttList.isEmpty());
    verifyDefault();
  }

  @Test
  public void test_getApiAttachment_hasAccess() throws Exception {
    XWikiAttachment xwikiAtt = new XWikiAttachment(doc, "bli.gaga");
    expect(getMock(IRightsAccessFacadeRole.class).hasAccessLevel(doc.getDocumentReference(),
        EAccessLevel.VIEW)).andReturn(true);
    replayDefault();
    Attachment attachment = attService.getApiAttachment(xwikiAtt);
    assertNotNull("Expected Attachment api object - not null", attachment);
    verifyDefault();
  }

  @Test
  public void test_getApiAttachment_noAccess() throws Exception {
    XWikiAttachment xwikiAtt = new XWikiAttachment(doc, "bli.gaga");
    expect(getMock(IRightsAccessFacadeRole.class).hasAccessLevel(doc.getDocumentReference(),
        EAccessLevel.VIEW)).andReturn(false);
    replayDefault();
    assertThrows(NoAccessRightsException.class, () -> attService.getApiAttachment(xwikiAtt));
    verifyDefault();
  }

}
