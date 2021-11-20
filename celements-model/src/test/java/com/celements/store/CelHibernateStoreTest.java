package com.celements.store;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.store.TestHibernateQuery.*;
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
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.access.IModelAccessFacade;
import com.celements.store.part.XWikiDummyDocComparator;
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
  }

  @Test
  public void test_loadXWikiDoc() throws Exception {
    DocumentReference docRef = new ImmutableDocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    Session sessionMock = createSessionMock(doc);
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, Collections.<BaseObject>emptyList());
    sessionMock.load(same(doc), eq(doc.getId()));

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
    DocumentReference docRef = new ImmutableDocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    Capture<XWikiDocument> docCapture = newCapture();
    Session sessionMock = createSessionMock(doc);
    expectLoadAttachments(sessionMock, Collections.<XWikiAttachment>emptyList());
    expectLoadObjects(sessionMock, Collections.<BaseObject>emptyList());
    sessionMock.load(capture(docCapture), eq(doc.getId()));
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
  public void test_saveXWikiDoc_immutability() throws Exception {
    DocumentReference docRef = new ImmutableDocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    Capture<XWikiDocument> docCapture = newCapture();
    Session sessionMock = createSessionMock(doc);
    expectSaveDocExists(sessionMock, null);
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
    XWikiDocument doc = new XWikiDocument(new DocumentReference("xwikidb", "space", "doc"));
    Session sessionMock = createSessionMock(doc);
    expectSaveDocExists(sessionMock, null);
    expect(sessionMock.save(cmp(doc, new XWikiDummyDocComparator(),
        LogicalOperator.EQUAL))).andReturn(null);
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
    XWikiDocument doc = new XWikiDocument(new DocumentReference("xwikidb", "space", "doc"));
    Session sessionMock = createSessionMock(doc);
    expectSaveDocExists(sessionMock, doc.getDocumentReference());
    sessionMock.update(cmp(doc, new XWikiDummyDocComparator(), LogicalOperator.EQUAL));
    expectLastCall();
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    getStore(sessionMock).saveXWikiDoc(doc, getContext());
    verifyDefault();

    assertFalse(doc.isNew());
    assertFalse(doc.isContentDirty());
    assertFalse(doc.isMetaDataDirty());
  }

  @Test
  public void test_saveXWikiDoc_collision() throws Exception {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("xwikidb", "space", "doc"));
    final Session sessionMock = createSessionMock(doc);
    expectSaveDocExists(sessionMock, new DocumentReference("xwikidb", "space", "other"));
    expect(sessionMock.close()).andReturn(null);

    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class) {

      @Override
      protected void execute() throws XWikiException {
        getStore(sessionMock).saveXWikiDoc(doc, getContext());
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_delete_invalidWiki() throws Exception {
    final XWikiDocument doc = new XWikiDocument(new DocumentReference("otherWiki", "space", "doc"));
    Session sessionMock = createSessionMock(doc);

    replayDefault();
    final CelHibernateStore store = getStore(sessionMock);
    doc.setStore(store);
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class,
        "different wiki than context db should fast fail") {

      @Override
      protected void execute() throws Exception {
        store.deleteXWikiDoc(doc, getContext());
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_getSchemaFromWikiName_virtual() {
    expect(getWikiMock().isVirtualMode()).andReturn(true).anyTimes();
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn("pref_").anyTimes();
    replayDefault();
    CelHibernateStore store = getStore(null);
    assertNull(store.getSchemaFromWikiName(null, null, getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("as5df", null, getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("AS5DF", null, getContext()));
    assertEquals("pref_as5df", store.getSchemaFromWikiName("a$s5(DF)", null, getContext()));
    assertEquals("pref_as5df_suf", store.getSchemaFromWikiName("AS5DF-SUF", null, getContext()));
    verifyDefault();
  }

  @Test
  public void test_getSchemaFromWikiName_main() {
    expect(getWikiMock().isVirtualMode()).andReturn(false).anyTimes();
    expect(getWikiMock().Param("xwiki.db")).andReturn("main").anyTimes();
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn("pref_").anyTimes();
    replayDefault();
    CelHibernateStore store = getStore(null);
    assertNull(store.getSchemaFromWikiName(null, null, getContext()));
    assertEquals("pref_main", store.getSchemaFromWikiName("as5df", null, getContext()));
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

}
