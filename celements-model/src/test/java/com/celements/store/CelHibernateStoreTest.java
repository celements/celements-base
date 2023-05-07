package com.celements.store;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.store.TestHibernateQuery.*;
import static com.celements.store.part.CelHibernateStoreDocumentPart.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Collections;

import org.easymock.Capture;
import org.easymock.LogicalOperator;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;
import org.xwiki.model.reference.ImmutableReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.IModelAccessFacade;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.celements.store.id.IdVersion;
import com.celements.store.part.XWikiDummyDocComparator;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public class CelHibernateStoreTest extends AbstractComponentTest {

  private SessionFactory sessionFactoryMock;
  private XWikiHibernateStore primaryStoreMock;

  private final DocumentReference docRef = new ImmutableDocumentReference(
      "xwikidb", "space", "doc");
  private XWikiDocument doc;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(IModelAccessFacade.class);
    sessionFactoryMock = createMockAndAddToDefault(SessionFactory.class);
    primaryStoreMock = createMockAndAddToDefault(XWikiHibernateStore.class);
    expect(getWikiMock().getStore()).andReturn(primaryStoreMock).anyTimes();
    expect(getWikiMock().getConfig()).andReturn(new XWikiConfig()).anyTimes();
    expect(getWikiMock().getPlugin("monitor", getContext())).andReturn(null).anyTimes();
    expect(getWikiMock().hasDynamicCustomMappings()).andReturn(false).anyTimes();
    expect(getWikiMock().hasVersioning(getContext())).andReturn(false).anyTimes();
    expect(getWikiMock().hasBacklinks(getContext())).andReturn(false).anyTimes();
    expect(getWikiMock().Param(eq("xwiki.store.hibernate.useclasstables.read"), eq("1"))).andReturn(
        "0").anyTimes();
    doc = new XWikiDocument(docRef);
  }

  @Test
  public void test_loadXWikiDoc() throws Exception {
    long docId = computeDocId(START_COLLISION_COUNT_DEFAULT);
    Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { docId, doc.getFullName(), doc.getLanguage() }));
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, Collections.<BaseObject>emptyList());
    sessionMock.load(same(doc), eq(docId));

    replayDefault();
    XWikiDocument ret = getStore(sessionMock).loadXWikiDoc(doc, getContext());
    verifyDefault();

    assertSame(doc, ret);
    assertFalse(doc.isNew());
    assertFalse(doc.isContentDirty());
    // FIXME can be set to 'assertFalse' after CELDEV-784, see CELDEV-785
    assertTrue(doc.isMetaDataDirty());
  }

  @Test
  public void test_loadXWikiDoc_immutability() throws Exception {
    long docId = computeDocId(START_COLLISION_COUNT_DEFAULT);
    Capture<XWikiDocument> docCapture = newCapture();
    Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { docId, doc.getFullName(), doc.getLanguage() }));
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, Collections.<BaseObject>emptyList());
    sessionMock.load(capture(docCapture), eq(docId));
    expectLastCall().once();

    replayDefault();
    XWikiDocument ret = getStore(sessionMock).loadXWikiDoc(doc, getContext());
    verifyDefault();

    assertSame(doc, ret);
    assertSame(doc, docCapture.getValue());
    DocumentReference cptDocRef = docCapture.getValue().getDocumentReference();
    assertEquals(docRef, cptDocRef);
    assertTrue("provided docRef has to be immutable", cptDocRef instanceof ImmutableReference);
    assertTrue("after execution docRef has to be immutable",
        doc.getDocumentReference() instanceof ImmutableReference);
  }

  @Test
  public void test_loadXWikiDoc_collision() throws Exception {
    long docId = computeDocId(START_COLLISION_COUNT_DEFAULT);
    Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { docId, doc.getFullName(), doc.getLanguage() }));
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, Collections.<BaseObject>emptyList());
    sessionMock.load(cmp(doc, (actual, expected) -> {
      actual.setFullName("space.other"); // simulate getting different fullName from DB
      return actual == expected ? 0 : 1;
    }, LogicalOperator.EQUAL), eq(docId));

    replayDefault();
    XWikiException xwe = assertThrows(XWikiException.class,
        () -> getStore(sessionMock).loadXWikiDoc(doc, getContext()));
    assertTrue(xwe.getMessage(), xwe.getMessage().toLowerCase().contains("collision"));
    verifyDefault();
  }

  @Test
  public void test_saveXWikiDoc_immutability() throws Exception {
    Capture<XWikiDocument> docCapture = newCapture();
    Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of());
    expect(sessionMock.save(capture(docCapture))).andReturn(null).once();
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    getStore(sessionMock).saveXWikiDoc(doc, getContext());
    verifyDefault();

    assertSame(doc, docCapture.getValue());
    DocumentReference cptDocRef = docCapture.getValue().getDocumentReference();
    assertEquals(docRef, cptDocRef);
    assertTrue("provided docRef has to be immutable", cptDocRef instanceof ImmutableReference);
    assertTrue("after execution docRef has to be immutable",
        doc.getDocumentReference() instanceof ImmutableReference);
  }

  @Test
  public void test_saveXWikiDoc_save() throws Exception {
    Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of());
    expect(sessionMock.save(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL)))
        .andReturn(null);
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    getStore(sessionMock).saveXWikiDoc(doc, getContext());
    verifyDefault();

    assertFalse(doc.isNew());
    assertFalse(doc.isContentDirty());
    assertFalse(doc.isMetaDataDirty());
  }

  @Test
  public void test_saveXWikiDoc_update() throws Exception {
    long docId = computeDocId(START_COLLISION_COUNT_DEFAULT);
    doc.setId(docId, IdVersion.CELEMENTS_3);
    doc.setNew(false);
    Session sessionMock = createSessionMock(doc);
    sessionMock.update(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL));
    expectLastCall();
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    getStore(sessionMock).saveXWikiDoc(doc, getContext());
    verifyDefault();

    assertEquals(docId, doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
    assertFalse(doc.isNew());
    assertFalse(doc.isContentDirty());
    assertFalse(doc.isMetaDataDirty());
  }

  @Test
  public void test_saveXWikiDoc_collision_single() throws Exception {
    final Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { computeDocId(START_COLLISION_COUNT_DEFAULT), "space.other", "" }));
    expect(sessionMock.save(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL)))
        .andReturn(null);
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    getStore(sessionMock).saveXWikiDoc(doc, getContext());
    verifyDefault();

    assertEquals(computeDocId(START_COLLISION_COUNT_DEFAULT + 1), doc.getId());
    assertSame(IdVersion.CELEMENTS_3, doc.getIdVersion());
  }

  @Test
  public void test_saveXWikiDoc_collision_exhausted() throws Exception {
    final Session sessionMock = createSessionMock(doc);
    expectLoadExistingDocs(sessionMock, ImmutableList.of(
        new Object[] { computeDocId(0), "space.other1", "" },
        new Object[] { computeDocId(1), "space.other2", "" },
        new Object[] { computeDocId(2), "space.other3", "" },
        new Object[] { computeDocId(3), "space.other4", "" }));
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    assertThrows("fail on saving collision", XWikiException.class,
        () -> getStore(sessionMock).saveXWikiDoc(doc, getContext()));
    verifyDefault();
  }

  @Test
  public void test_delete_invalidWiki() throws Exception {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("otherWiki", "space", "doc"));
    Session sessionMock = createSessionMock(doc);

    replayDefault();
    final CelHibernateStore store = getStore(sessionMock);
    doc.setStore(store);
    assertThrows("different wiki than context db should fast fail", IllegalArgumentException.class,
        () -> store.deleteXWikiDoc(doc, getContext()));
    verifyDefault();
  }

  @Test
  public void test_getSchemaFromWikiName_virtual() {
    expect(getWikiMock().isVirtualMode()).andReturn(true).anyTimes();
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn("pref_").anyTimes();
    replayDefault();
    CelHibernateStore store = getStore(null);
    assertNull(store.getSchemaFromWikiName(null, getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("as5df", getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("AS5DF", getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("a$s5(DF)", getContext()));
    assertEquals("pref_as5df_suf", store.getSchemaFromWikiName("AS5DF-SUF", getContext()));
    verifyDefault();
  }

  @Test
  public void test_getSchemaFromWikiName_main() {
    expect(getWikiMock().isVirtualMode()).andReturn(false).anyTimes();
    expect(getWikiMock().Param("xwiki.db")).andReturn("main").anyTimes();
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn("pref_").anyTimes();
    replayDefault();
    CelHibernateStore store = getStore(null);
    assertNull(store.getSchemaFromWikiName(null, getContext()));
    assertEquals("pref_main", store.getSchemaFromWikiName("as5df", getContext()));
    verifyDefault();
  }

  private CelHibernateStore getStore(Session session) {
    CelHibernateStore store = (CelHibernateStore) Utils.getComponent(XWikiStoreInterface.class);
    store.setSessionFactory(sessionFactoryMock);
    store.setSession(session, getContext());
    return store;
  }

  private Session createSessionMock(XWikiDocument doc) {
    Session sessionMock = createMockAndAddToDefault(Session.class);
    sessionMock.setFlushMode(anyObject(FlushMode.class));
    expectLastCall().anyTimes();
    sessionMock.flush();
    expectLastCall().anyTimes();
    sessionMock.clear();
    expectLastCall().anyTimes();
    return sessionMock;
  }

  private long computeDocId(int collisionCount) throws IdComputationException {
    return Utils.getComponent(CelementsIdComputer.class)
        .computeDocumentId(docRef, "", (byte) collisionCount);
  }

}
