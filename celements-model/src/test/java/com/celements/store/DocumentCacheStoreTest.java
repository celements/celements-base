package com.celements.store;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.References;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public class DocumentCacheStoreTest extends AbstractComponentTest {

  private static final String MOCK_STORE_HINT = "mockStoreHint";

  private DocumentCacheStore docCacheStore;
  private XWikiStoreInterface mockStore;

  @Before
  public void prepareTest() throws Exception {
    mockStore = registerComponentMock(XWikiStoreInterface.class, MOCK_STORE_HINT);
    getConfigurationSource().setProperty("celements.store.docCache.backingStore",
        MOCK_STORE_HINT);
    expect(getWikiMock().Param(eq(DocumentCacheStore.PARAM_EXIST_CACHE_CAPACITY))).andReturn(
        "100").anyTimes();
    expect(getWikiMock().Param(eq(DocumentCacheStore.PARAM_DOC_CACHE_CAPACITY))).andReturn(
        "100").anyTimes();
    docCacheStore = (DocumentCacheStore) Utils.getComponent(XWikiStoreInterface.class,
        DocumentCacheStore.COMPONENT_NAME);
  }

  @Test
  public void testSaveXWikiDoc() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    Capture<XWikiDocument> savingDocCapture = newCapture();
    mockStore.saveXWikiDoc(capture(savingDocCapture), same(getContext()), eq(true));
    expectLastCall().once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertNotSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    // Save a document
    docCacheStore.saveXWikiDoc(existingDocument, getContext());
    XWikiDocument existingDocSaved = savingDocCapture.getValue();
    assertSame(existingDocument, existingDocSaved);
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNull("on saving doc must be removed from Cache", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testSaveXWikiDoc_noTransaction() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    boolean bTransaction = false;
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    Capture<XWikiDocument> savingDocCapture = newCapture();
    mockStore.saveXWikiDoc(capture(savingDocCapture), same(getContext()), eq(bTransaction));
    expectLastCall().once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertNotSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    // Save a document
    docCacheStore.saveXWikiDoc(existingDocument, getContext(), bTransaction);
    XWikiDocument existingDocSaved = savingDocCapture.getValue();
    assertSame(existingDocument, existingDocSaved);
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNull("on saving doc must be removed from Cache", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testGetKey() {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument testDoc = new XWikiDocument(docRef);
    assertEquals("wiki:space.page", docCacheStore.getKey(docRef));
    assertEquals("wiki:space.page", docCacheStore.getKeyWithLang(testDoc));
  }

  @Test
  public void testGetKey_differentDb() {
    getContext().setDatabase("wikitest");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument testDoc = new XWikiDocument(docRef);
    assertEquals("wikitest:space.page", docCacheStore.getKey(docRef));
    assertEquals("wikitest:space.page", docCacheStore.getKeyWithLang(testDoc));
  }

  @Test
  public void testGetKey_lang() {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument testDoc = new XWikiDocument(docRef);
    testDoc.setLanguage("en");
    assertEquals("wiki:space.page:en", docCacheStore.getKeyWithLang(testDoc));
  }

  @Test
  public void testGetKey_docRef_deflang() {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    assertEquals("wiki:space.page", docCacheStore.getKeyWithLang(docRef, ""));
  }

  @Test
  public void testGetKey_docRef_lang() {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    assertEquals("wiki:space.page:fr", docCacheStore.getKeyWithLang(docRef, "fr"));
  }

  @Test
  public void testInvalidateCacheFromClusterEvent_docExists() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadXWikiDoc(isA(XWikiDocument.class), same(getContext()))).andReturn(
        savedDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue("doc expected in exists cache", docCacheStore.exists(existingDocument,
        getContext()));
    docCacheStore.removeDocFromCache(existingDocument, true);
    assertNull("doc not in cache anymore", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testInvalidateCacheFromClusterEvent_docNOTExists() throws Exception {
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadXWikiDoc(isA(XWikiDocument.class), same(getContext()))).andReturn(
        savedDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue("doc expected in exists cache", docCacheStore.exists(existingDocument,
        getContext()));
    docCacheStore.removeDocFromCache(existingDocument, false);
    assertNull("doc not in cache anymore", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testInvalidateCacheFromClusterEventString() throws Exception {
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadXWikiDoc(isA(XWikiDocument.class), same(getContext()))).andReturn(
        savedDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue("doc expected in exists cache", docCacheStore.exists(existingDocument,
        getContext()));
    docCacheStore.invalidateDocCache(key);
    assertNull("doc not in cache anymore", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testLoadXWikiDoc() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertNotSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    assertFalse(existingDocument.isNew());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));

    assertSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));
    verifyDefault();
  }

  @Test
  public void testLoadXWikiDoc_differentProvidedDB() throws Exception {
    getContext().setDatabase("ctxWiki");
    XWikiContext providedContext = (XWikiContext) getContext().clone();
    providedContext.setDatabase("providedCtxWiki");
    DocumentReference actualDocRefToLoad = new DocumentReference(providedContext.getDatabase(),
        "space", "page");
    XWikiDocument inputParamDoc = new XWikiDocument(References.adjustRef(actualDocRefToLoad,
        DocumentReference.class, new WikiReference("docWiki")));
    XWikiDocument savedDoc = new XWikiDocument(actualDocRefToLoad);
    savedDoc.setNew(false);
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(providedContext))).andReturn(
        savedDoc).once();

    replayDefault();
    docCacheStore.initalize();
    XWikiDocument ret = docCacheStore.loadXWikiDoc(inputParamDoc, providedContext);
    verifyDefault();

    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(
        serialize(actualDocRefToLoad)));
    assertEquals(actualDocRefToLoad.getWikiReference(),
        ret.getDocumentReference().getWikiReference());
    assertEquals(actualDocRefToLoad.getWikiReference(),
        querySaveDocCapture.getValue().getDocumentReference().getWikiReference());
  }

  @Test
  public void testLoadXWikiDoc_different_contextDb_WikiRef() throws Exception {
    getContext().setDatabase("xwikimyDB");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    DocumentReference expectedDocRef = References.adjustRef(docRef, DocumentReference.class,
        new WikiReference(getContext().getDatabase()));
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertNotSame(inputParamDoc, querySaveDoc);
    assertEquals(expectedDocRef, querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    assertFalse(existingDocument.isNew());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));

    assertSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));
    verifyDefault();
  }

  @Test
  public void testLoadXWikiDoc_noOverwriteExistsCache() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setLanguage("");
    savedDoc.setDefaultLanguage("");
    savedDoc.setTranslation(0);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySaveDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    Capture<XWikiDocument> queryNotExistDocCapture = newCapture();
    XWikiDocument notExistsDoc = new XWikiDocument(docRef);
    notExistsDoc.setNew(true);
    notExistsDoc.setLanguage("de");
    notExistsDoc.setDefaultLanguage("");
    notExistsDoc.setTranslation(0);
    notExistsDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadXWikiDoc(capture(queryNotExistDocCapture), same(getContext()))).andReturn(
        notExistsDoc).once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    inputParamDoc.setLanguage("");
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertNotSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    assertFalse(existingDocument.isNew());
    assertTrue(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));
    assertTrue("result must be in exists cache", docCacheStore.exists(inputParamDoc, getContext()));
    assertSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));

    // second loading with default language
    XWikiDocument inputParamDoc2 = new XWikiDocument(docRef);
    inputParamDoc2.setLanguage("de");
    XWikiDocument notExistsDocument = docCacheStore.loadXWikiDoc(inputParamDoc2, getContext());
    assertTrue("may not overwrite existing noTrans entry", docCacheStore.exists(existingDocument,
        getContext()));
    assertTrue(notExistsDocument.isNew());
    assertFalse(notExistsDocument.isFromCache());

    verifyDefault();
  }

  @Test
  public void testDeleteXWikiDoc() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    Capture<XWikiDocument> querySavedDocCapture = newCapture();
    expect(mockStore.loadXWikiDoc(capture(querySavedDocCapture), same(getContext()))).andReturn(
        savedDoc).once();
    Capture<XWikiDocument> deletingDocCapture = newCapture();
    mockStore.deleteXWikiDoc(capture(deletingDocCapture), same(getContext()));
    expectLastCall().once();
    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    XWikiDocument querySavedDoc = querySavedDocCapture.getValue();
    assertNotSame(inputParamDoc, querySavedDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySavedDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySavedDoc.getLanguage());
    // delete a document
    docCacheStore.deleteXWikiDoc(existingDocument, getContext());
    XWikiDocument existingDocDeleted = deletingDocCapture.getValue();
    assertSame(existingDocument, existingDocDeleted);
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNull("on deleting doc must be removed from Cache", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testExists_true() throws Exception {
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    boolean docExists = true;
    expect(mockStore.exists(capture(querySaveDocCapture), same(getContext())))
        .andReturn(docExists);

    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    boolean existsDoc = docCacheStore.exists(inputParamDoc, getContext());
    assertEquals(docExists, existsDoc);
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    assertEquals("result must be in exists cache", docExists, docCacheStore.exists(inputParamDoc,
        getContext()));
    verifyDefault();
  }

  @Test
  public void testExists_false() throws Exception {
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    Capture<XWikiDocument> querySaveDocCapture = newCapture();
    boolean docExists = false;
    expect(mockStore.exists(capture(querySaveDocCapture), same(getContext())))
        .andReturn(docExists);

    replayDefault();
    docCacheStore.initalize();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    boolean existsDoc = docCacheStore.exists(inputParamDoc, getContext());
    assertEquals(docExists, existsDoc);
    XWikiDocument querySaveDoc = querySaveDocCapture.getValue();
    assertSame(inputParamDoc, querySaveDoc);
    assertEquals(inputParamDoc.getDocumentReference(), querySaveDoc.getDocumentReference());
    assertEquals(inputParamDoc.getLanguage(), querySaveDoc.getLanguage());
    assertEquals("result must be in exists cache", docExists, docCacheStore.exists(inputParamDoc,
        getContext()));
    verifyDefault();
  }

  @Test
  public void testExists_differentProvidedDB() throws Exception {
    getContext().setDatabase("ctxWiki");
    XWikiContext providedContext = (XWikiContext) getContext().clone();
    providedContext.setDatabase("providedCtxWiki");
    DocumentReference actualDocRefToLoad = new ImmutableDocumentReference(
        providedContext.getDatabase(), "space", "page");
    XWikiDocument inputParamDoc = new XWikiDocument(References.adjustRef(actualDocRefToLoad,
        DocumentReference.class, new WikiReference("docWiki")));
    boolean docExists = true;
    expect(mockStore.exists(anyObject(XWikiDocument.class), anyObject(XWikiContext.class)))
        .andReturn(docExists);

    replayDefault();
    docCacheStore.initalize();
    boolean ret = docCacheStore.exists(inputParamDoc, providedContext);
    verifyDefault();

    assertEquals(docExists, ret);
    assertEquals("result must be in exists cache", true, docCacheStore.getExistFromCache(
        serialize(actualDocRefToLoad)));
  }

  private static final String serialize(DocumentReference docRef) {
    return Utils.getComponent(ModelUtils.class).serializeRef(docRef);
  }

}
