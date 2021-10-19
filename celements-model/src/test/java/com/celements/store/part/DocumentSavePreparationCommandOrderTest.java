package com.celements.store.part;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.UniqueHashIdComputer;
import com.google.common.collect.ImmutableSortedMap;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class DocumentSavePreparationCommandOrderTest extends AbstractComponentTest {

  private CelHibernateStore storeStrictMock;
  private Session sessionMock;

  @Before
  public void prepareTest() throws Exception {
    storeStrictMock = createStrictMock(CelHibernateStore.class);
    getDefaultMocks().add(storeStrictMock);
    expect(getWikiMock().getStore()).andReturn(storeStrictMock).anyTimes();
    sessionMock = createMockAndAddToDefault(Session.class);
    sessionMock.setFlushMode(FlushMode.COMMIT);
    expectLastCall().anyTimes();
  }

  @Test
  public void test_execute_order() throws Exception {
    XWikiDocument doc = createDoc(false, null);
    BaseObject obj = addObject(doc);

    storeStrictMock.checkHibernate(same(getContext()));
    SessionFactory sfactoryMock = createMockAndAddToDefault(SessionFactory.class);
    expect(storeStrictMock.injectCustomMappingsInSessionFactory(same(doc), same(getContext())))
        .andReturn(sfactoryMock);
    expect(storeStrictMock.beginTransaction(same(sfactoryMock), same(getContext())))
        .andReturn(true);
    expect(storeStrictMock.getSession(getContext())).andReturn(sessionMock);
    expect(storeStrictMock.getDocKey(doc.getDocumentReference(), doc.getLanguage()))
        .andReturn("space.doc");
    expect(storeStrictMock.loadExistingDocKeys(sessionMock, doc.getDocumentReference(),
        doc.getLanguage())).andReturn(ImmutableSortedMap.of());
    expect(storeStrictMock.getIdComputer()).andReturn(Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME)).times(2);
    expect(storeStrictMock.exists(anyObject(XWikiDocument.class), same(getContext())))
        .andReturn(true);
    expect(storeStrictMock.loadXWikiDoc(anyObject(XWikiDocument.class), same(getContext())))
        .andReturn(createDoc(false, null));
    expect(storeStrictMock.getIdComputer()).andReturn(Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME)).times(2);

    replayDefault();
    DocumentSavePreparationCommand ret = new DocumentSavePreparationCommand(
        doc, storeStrictMock, getContext()).execute(true);
    verifyDefault();
    assertSame(sessionMock, ret.getSession());
  }

  private BaseObject addObject(XWikiDocument doc) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(new DocumentReference("xwikidb", "space", "class"));
    obj.setGuid("");
    doc.addXObject(obj);
    return obj;
  }

  private XWikiDocument createDoc(boolean isNew, String language) {
    DocumentReference docRef = new DocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(language);
    doc.setNew(isNew);
    return doc;
  }

}
