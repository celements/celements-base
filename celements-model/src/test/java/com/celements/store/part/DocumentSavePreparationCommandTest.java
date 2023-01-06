package com.celements.store.part;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.store.part.CelHibernateStoreDocumentPart.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;

import org.easymock.LogicalOperator;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.celements.store.id.IdVersion;
import com.celements.store.id.UniqueHashIdComputer;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class DocumentSavePreparationCommandTest extends AbstractComponentTest {

  private CelHibernateStore storeMock;
  private Session sessionMock;

  @Before
  public void prepareTest() throws Exception {
    storeMock = createMockAndAddToDefault(CelHibernateStore.class);
    expect(getWikiMock().getStore()).andReturn(storeMock).anyTimes();
    expect(storeMock.getIdComputer()).andReturn(Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME)).anyTimes();
    sessionMock = createMockAndAddToDefault(Session.class);
    sessionMock.setFlushMode(FlushMode.COMMIT);
    expectLastCall().anyTimes();
    expect(storeMock.getSession(getContext())).andReturn(sessionMock).anyTimes();
  }

  private DocumentSavePreparationCommand newCommand(XWikiDocument doc) {
    return new DocumentSavePreparationCommand(doc, storeMock, getContext());
  }

  @Test
  public void test_execute() throws Exception {
    XWikiDocument doc = createDoc("");

    replayDefault();
    DocumentSavePreparationCommand cmd = newCommand(doc).execute(false);
    verifyDefault();
    assertSame(sessionMock, cmd.getSession());
    assertFalse("doc with id already set must stem from db", doc.isNew());
    assertSame("store should be set", storeMock, doc.getStore());
    assertEquals("doc should be set to context db", getContext().getDatabase(),
        doc.getDocumentReference().getWikiReference().getName());
    assertFalse(doc.hasElement(XWikiDocument.HAS_OBJECTS));
    assertFalse(doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
  }

  @Test
  public void test_execute_transaction() throws Exception {
    XWikiDocument doc = createDoc("");

    storeMock.checkHibernate(same(getContext()));
    expectLastCall();
    SessionFactory sfactoryMock = createMockAndAddToDefault(SessionFactory.class);
    expect(storeMock.injectCustomMappingsInSessionFactory(same(doc), same(getContext()))).andReturn(
        sfactoryMock);
    expect(storeMock.beginTransaction(same(sfactoryMock), same(getContext()))).andReturn(true);

    replayDefault();
    DocumentSavePreparationCommand cmd = newCommand(doc).execute(true);
    verifyDefault();
    assertSame(sessionMock, cmd.getSession());
  }

  @Test
  public void test_execute_hasAlreadyValidId() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    long docId = computeDocId(doc, START_COLLISION_COUNT_DEFAULT);
    doc.setId(docId, IdVersion.CELEMENTS_3);

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertFalse(doc.isNew());
    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_execute_setId_alreadyExists() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    long docId = computeDocId(doc, START_COLLISION_COUNT_DEFAULT);
    doc.setNew(true); // test with wrong new-flag, it should be corrected
    expectExistingDocs(doc, ImmutableMap.of(
        docId, doc.getFullName() + "." + doc.getLanguage()));

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertFalse(doc.isNew());
    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_execute_setId_notExists() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    long docId = computeDocId(doc, START_COLLISION_COUNT_DEFAULT);
    expectExistingDocs(doc, ImmutableMap.of());

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertTrue(doc.isNew());
    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_execute_setId_collision_notExists() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    long docId = computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 2);
    expectExistingDocs(doc, ImmutableMap.of(
        computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 0), "space.other1",
        computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 1), "space.other2"));

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertTrue(doc.isNew());
    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_execute_setId_collision_alreadyExists() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    long docId = computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 2);
    expectExistingDocs(doc, ImmutableMap.of(
        computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 0), "space.other1",
        computeDocId(doc, START_COLLISION_COUNT_DEFAULT + 1), "space.other2",
        docId, doc.getFullName() + "." + doc.getLanguage()));

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertFalse(doc.isNew());
    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_execute_setId_collision_countExhausted() throws Exception {
    XWikiDocument doc = createDocWithoutId("");
    expectExistingDocs(doc, ImmutableMap.of(
        computeDocId(doc, 0), "space.other1",
        computeDocId(doc, 1), "space.other2",
        computeDocId(doc, 2), "space.other3",
        computeDocId(doc, 3), "space.other4"));

    replayDefault();
    assertThrows(XWikiException.class, () -> newCommand(doc).execute(false));
    verifyDefault();
  }

  @Test
  public void test_execute_object() throws Exception {
    XWikiDocument doc = createDoc("");
    doc.setNew(true);
    BaseObject obj = addObject(doc);
    expect(storeMock.exists(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL), same(
        getContext()))).andReturn(false).once();

    assertTrue(obj.getGuid().isEmpty());
    assertEquals(0, obj.getId());
    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertTrue(doc.hasElement(XWikiDocument.HAS_OBJECTS));
    assertFalse(obj.getGuid().isEmpty());
    assertEquals(-974136870929809407L, obj.getId());
  }

  @Test
  public void test_execute_translation() throws Exception {
    XWikiDocument doc = createDoc("ch");
    BaseObject obj = addObject(doc);

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertFalse(doc.hasElement(XWikiDocument.HAS_OBJECTS));
    assertTrue(obj.getGuid().isEmpty());
    assertEquals(0, obj.getId());
  }

  @Test
  public void test_execute_object_load() throws Exception {
    XWikiDocument doc = createDoc("");
    BaseObject obj = addObject(doc);
    XWikiDocument origDoc = createDoc("");
    BaseObject origObj = addObject(origDoc);
    origObj.setId(1234, IdVersion.CELEMENTS_3);
    expect(storeMock.exists(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL), same(
        getContext()))).andReturn(true).once();
    expect(storeMock.loadXWikiDoc(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL),
        same(getContext()))).andReturn(origDoc);

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertEquals(origObj.getId(), obj.getId());
  }

  @Test
  public void test_execute_object_load_noObj() throws Exception {
    XWikiDocument doc = createDoc("");
    BaseObject obj = addObject(doc);
    expect(storeMock.exists(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL), same(
        getContext()))).andReturn(true).once();
    expect(storeMock.loadXWikiDoc(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL),
        same(getContext()))).andReturn(doc);

    replayDefault();
    assertThrows(IllegalStateException.class, () -> newCommand(doc).execute(false));
    verifyDefault();
  }

  @Test
  public void test_execute_object_load_notExists() throws Exception {
    XWikiDocument doc = createDoc("");
    doc.setNew(true);
    BaseObject obj = addObject(doc);
    expect(storeMock.exists(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL), same(
        getContext()))).andReturn(false).once();

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertEquals(-974136870929809407L, obj.getId());
  }

  @Test
  public void test_execute_object_load_XWE() throws Exception {
    final XWikiDocument doc = createDoc("");
    addObject(doc);
    XWikiException cause = new XWikiException();
    expect(storeMock.exists(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL), same(
        getContext()))).andThrow(cause);

    replayDefault();
    assertThrows(XWikiException.class, () -> newCommand(doc).execute(false));
    verifyDefault();
  }

  private BaseObject addObject(XWikiDocument doc) {
    BaseObject obj = new BaseObject();
    obj.setDocumentReference(doc.getDocumentReference());
    obj.setXClassReference(new DocumentReference("xwikidb", "space", "class"));
    obj.setGuid("");
    doc.addXObject(obj);
    return obj;
  }

  @Test
  public void test_execute_attachment() throws Exception {
    XWikiDocument doc = createDoc("");
    doc.setAttachmentList(Arrays.asList(new XWikiAttachment(doc, "file")));

    replayDefault();
    newCommand(doc).execute(false);
    verifyDefault();
    assertTrue(doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
  }

  private XWikiDocument createDoc(String lang) {
    XWikiDocument doc = createDocWithoutId(lang);
    doc.setId(1L, IdVersion.XWIKI_2);
    return doc;
  }

  private XWikiDocument createDocWithoutId(String lang) {
    DocumentReference docRef = new DocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setNew(false);
    doc.setLanguage(lang);
    doc.setTranslation(lang.equals("") ? 0 : 1);
    expect(storeMock.getDocKey(docRef, lang)).andReturn("space.doc." + lang).anyTimes();
    return doc;
  }

  private void expectExistingDocs(XWikiDocument doc, final Map<Long, String> existingDocs) {
    expect(storeMock.loadExistingDocKeys(sessionMock, doc.getDocumentReference(),
        doc.getLanguage())).andReturn(ImmutableBiMap.copyOf(existingDocs));
  }

  private long computeDocId(XWikiDocument doc, int collisionCount)
      throws IdComputationException {
    return Utils.getComponent(CelementsIdComputer.class)
        .computeDocumentId(doc.getDocumentReference(), doc.getLanguage(), (byte) collisionCount);
  }

}
