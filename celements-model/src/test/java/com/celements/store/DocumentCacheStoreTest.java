package com.celements.store;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.easymock.Capture;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsAllPropertiesConfigurationSource;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.References;
import com.celements.store.id.IdVersion;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public class DocumentCacheStoreTest extends AbstractComponentTest {

  private static final String MOCK_STORE_HINT = "mockStoreHint";

  private DocumentCacheStore docCacheStore;
  private XWikiStoreInterface mockStore;

  @Before
  public void prepareTest() throws Exception {
    mockStore = registerComponentMock(XWikiStoreInterface.class, MOCK_STORE_HINT);
    registerComponentMock(ConfigurationSource.class, CelementsAllPropertiesConfigurationSource.NAME,
        getConfigurationSource());
    getConfigurationSource().setProperty("celements.store.docCache.backingStore",
        MOCK_STORE_HINT);
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_EXIST_CACHE_CAPACITY, 10000);
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_DOC_CACHE_CAPACITY, 100);
    docCacheStore = getBeanFactory().getBean(DocumentCacheStore.class);
  }

  @Test
  public void testSaveXWikiDoc() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadCelDocument(same(docRef), eq("")))
        .andReturn(Optional.of(CelDocument.from(savedDoc))).once();
    Capture<XWikiDocument> savingDocCapture = newCapture();
    mockStore.saveXWikiDoc(capture(savingDocCapture), same(getContext()), eq(true));
    expectLastCall().once();
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
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
    boolean bTransaction = false;
    expectLoad(savedDoc);
    Capture<XWikiDocument> savingDocCapture = newCapture();
    mockStore.saveXWikiDoc(capture(savingDocCapture), same(getContext()), eq(bTransaction));
    expectLastCall().once();
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    // Save a document
    docCacheStore.saveXWikiDoc(existingDocument, getContext(), bTransaction);
    XWikiDocument existingDocSaved = savingDocCapture.getValue();
    assertSame(existingDocument, existingDocSaved);
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNull("on saving doc must be removed from Cache", docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testSaveXWikiDoc_invalidatesMainDocumentWithNamedDefaultLanguage() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setLanguage("en");
    savedDoc.setDefaultLanguage("en");
    expect(mockStore.loadCelDocument(eq(docRef), eq("")))
        .andReturn(Optional.of(CelDocument.from(savedDoc))).once();
    mockStore.saveXWikiDoc(anyObject(XWikiDocument.class), same(getContext()), eq(true));
    expectLastCall().once();
    replayDefault();

    XWikiDocument existingDocument = docCacheStore
        .loadXWikiDoc(new XWikiDocument(docRef), getContext());
    String key = docCacheStore.getKeyWithLang(docRef, "");
    assertNotNull(docCacheStore.getDocFromCache(key));

    docCacheStore.saveXWikiDoc(existingDocument, getContext());

    assertNull("saving the main document must invalidate its exact cache key",
        docCacheStore.getDocFromCache(key));
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
    assertEquals("wiki:space.page", docCacheStore.getKey(docRef));
    assertEquals("wiki:space.page", docCacheStore.getKeyWithLang(testDoc));
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
    expectLoad(savedDoc);
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isFromCache());
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
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expectLoad(savedDoc);
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isFromCache());
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
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expectLoad(savedDoc);
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isFromCache());
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
    expectLoad(savedDoc);
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isNew());
    assertFalse(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));

    CelDocument celDocument = docCacheStore
        .loadCelDocument(docRef, "").orElseThrow();
    assertSame(celDocument, docCacheStore.loadCelDocument(docRef, "").orElseThrow());
    assertSame(celDocument, docCacheStore.loadCelDocument(docRef).orElseThrow());
    assertNotSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));
    verifyDefault();
  }

  @Test
  public void testLoadXWikiDoc_different_contextDb_WikiRef() throws Exception {
    getContext().setDatabase("xwikimydb");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    DocumentReference expectedDocRef = References.adjustRef(docRef, DocumentReference.class,
        new WikiReference(getContext().getDatabase()));
    XWikiDocument savedDoc = new XWikiDocument(expectedDocRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expect(mockStore.loadCelDocument(eq(expectedDocRef), eq("")))
        .andReturn(Optional.of(CelDocument.from(savedDoc))).once();
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isNew());
    assertFalse(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));

    assertNotSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));
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
    expectLoad(savedDoc);
    XWikiDocument notExistsDoc = new XWikiDocument(docRef);
    notExistsDoc.setNew(true);
    notExistsDoc.setLanguage("de");
    notExistsDoc.setDefaultLanguage("");
    notExistsDoc.setTranslation(0);
    notExistsDoc.setOriginalDocument(savedDoc.clone());
    expectLoad(notExistsDoc);
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    inputParamDoc.setLanguage("");
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
    assertFalse(existingDocument.isNew());
    assertFalse(existingDocument.isFromCache());
    String key = docCacheStore.getKeyWithLang(existingDocument);
    assertNotNull("doc expected in cache", docCacheStore.getDocFromCache(key));
    assertTrue(docCacheStore.exists(existingDocument, getContext()));
    assertTrue("result must be in exists cache", docCacheStore.exists(inputParamDoc, getContext()));
    assertNotSame(existingDocument, docCacheStore.loadXWikiDoc(inputParamDoc, getContext()));

    // second loading with default language
    XWikiDocument inputParamDoc2 = new XWikiDocument(docRef);
    inputParamDoc2.setLanguage("de");
    XWikiDocument notExistsDocument = docCacheStore.loadXWikiDoc(inputParamDoc2, getContext());
    assertTrue("may not overwrite existing noTrans entry", docCacheStore.exists(existingDocument,
        getContext()));
    assertTrue(notExistsDocument.isNew());
    assertFalse(notExistsDocument.isFromCache());
    assertTrue(docCacheStore.loadCelDocument(docRef, "de").isEmpty());

    verifyDefault();
  }

  @Test
  public void testCeldev693_contextMutationDoesNotPoisonCachedIds() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    DocumentReference classRef = new DocumentReference("wiki", "XWiki", "Class");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setId(42, IdVersion.CELEMENTS_3);
    savedDoc.setNew(false);
    BaseObject persistedObject = new BaseObject();
    persistedObject.setDocumentReference(docRef);
    persistedObject.setXClassReference(classRef);
    persistedObject.setNumber(0);
    persistedObject.setId(43, IdVersion.CELEMENTS_3);
    savedDoc.setXObject(0, persistedObject);
    expectLoad(savedDoc);
    mockStore.saveXWikiDoc(anyObject(XWikiDocument.class), same(getContext()), eq(true));
    expectLastCall().once();
    replayDefault();

    XWikiDocument contextDocument = docCacheStore
        .loadXWikiDoc(new XWikiDocument(docRef), getContext());
    String key = docCacheStore.getKeyWithLang(docRef, "");
    CelDocument.Default cachedDocument = (CelDocument.Default) docCacheStore
        .getDocFromCache(key);
    BaseObject replacement = new BaseObject();
    replacement.setDocumentReference(docRef);
    replacement.setXClassReference(classRef);
    replacement.setNumber(0);
    contextDocument.setXObject(0, replacement);

    assertSame(IdVersion.CELEMENTS_3, cachedDocument.getXObjects().get(0).getIdVersion());
    assertSame(IdVersion.CELEMENTS_3,
        contextDocument.getOriginalDocument().getXObject(classRef).getIdVersion());
    docCacheStore.saveXWikiDoc(contextDocument, getContext());

    assertNull(docCacheStore.getDocFromCache(key));
    verifyDefault();
  }

  @Test
  public void testDeleteXWikiDoc() throws Exception {
    getContext().setDatabase("wiki");
    DocumentReference docRef = new DocumentReference("wiki", "space", "page");
    XWikiDocument savedDoc = new XWikiDocument(docRef);
    savedDoc.setNew(false);
    savedDoc.setOriginalDocument(savedDoc.clone());
    expectLoad(savedDoc);
    Capture<XWikiDocument> deletingDocCapture = newCapture();
    mockStore.deleteXWikiDoc(capture(deletingDocCapture), same(getContext()));
    expectLastCall().once();
    replayDefault();
    XWikiDocument inputParamDoc = new XWikiDocument(docRef);
    XWikiDocument existingDocument = docCacheStore.loadXWikiDoc(inputParamDoc, getContext());
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
  public void testConfiguredCacheCapacities() throws Exception {
    getContext().setDatabase("wiki");
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_DOC_CACHE_CAPACITY, 1);
    getConfigurationSource().setProperty(DocumentCacheStore.PARAM_EXIST_CACHE_CAPACITY, 2);
    for (int i = 1; i <= 3; i++) {
      DocumentReference docRef = new DocumentReference("wiki", "space", "page" + i);
      XWikiDocument savedDoc = new XWikiDocument(docRef);
      savedDoc.setNew(false);
      expect(mockStore.loadCelDocument(eq(docRef), eq("")))
          .andReturn(Optional.of(CelDocument.from(savedDoc))).once();
    }
    replayDefault();

    for (int i = 1; i <= 3; i++) {
      docCacheStore.loadCelDocument(
          new DocumentReference("wiki", "space", "page" + i), "");
    }

    DocumentCacheStore.Metrics metrics = docCacheStore.getMetrics();
    assertEquals(1, metrics.documents().size());
    assertEquals(1, metrics.documents().capacity());
    assertEquals(2, metrics.exists().size());
    assertEquals(2, metrics.exists().capacity());
    assertEquals(0, metrics.activeLoads());

    docCacheStore.loadCelDocument(new DocumentReference("wiki", "space", "page3"), "");
    metrics = docCacheStore.getMetrics();
    assertTrue(metrics.documents().hitCount() > 0);
    assertTrue(metrics.documents().missCount() > 0);
    assertTrue(metrics.documents().evictionCount() > 0);
    assertTrue(metrics.exists().hitCount() > 0);
    assertTrue(metrics.exists().missCount() > 0);
    assertTrue(metrics.exists().evictionCount() > 0);
    verifyDefault();
  }

  private void expectLoad(XWikiDocument document) throws Exception {
    Optional<CelDocument> result = document.isNew()
        ? Optional.empty()
        : Optional.of(CelDocument.from(document));
    expect(mockStore.loadCelDocument(anyObject(DocumentReference.class),
        eq(document.getLanguage())))
        .andReturn(result).once();
  }

}
