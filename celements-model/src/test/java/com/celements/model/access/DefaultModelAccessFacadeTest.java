package com.celements.model.access;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.access.DefaultModelAccessFacade.*;
import static com.celements.model.classes.TestClassDefinition.*;
import static com.google.common.collect.ImmutableList.*;
import static java.util.stream.Collectors.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.easymock.Capture;
import org.easymock.LogicalOperator;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;

import com.celements.auth.user.UserService;
import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsAllPropertiesConfigurationSource;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.ClassDocumentLoadException;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.ListField;
import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessException;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ClassFieldValue;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.celements.store.ModelAccessStore;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public class DefaultModelAccessFacadeTest extends AbstractComponentTest {

  private DefaultModelAccessFacade modelAccess;
  private XWikiDocument doc;
  private DocumentReference classRef;
  private DocumentReference classRef2;
  private XWikiStoreInterface storeMock;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(XWikiRecycleBinStoreInterface.class, ObservationManager.class,
        IRightsAccessFacadeRole.class, UserService.class);
    registerComponentMock(XWikiDocumentCreator.class, "default", new TestXWikiDocumentCreator());
    registerComponentMock(ConfigurationSource.class, "all", getConfigurationSource());
    registerComponentMock(ConfigurationSource.class, CelementsFromWikiConfigurationSource.NAME,
        getConfigurationSource());
    registerComponentMock(ConfigurationSource.class, CelementsAllPropertiesConfigurationSource.NAME,
        getConfigurationSource());
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, "en");
    doc = new XWikiDocument(new DocumentReference("db", "space", "doc"));
    doc.setDefaultLanguage(getConfigurationSource().getProperty(ModelContext.CFG_KEY_DEFAULT_LANG));
    doc.setSyntax(Syntax.XWIKI_1_0);
    doc.setMetaDataDirty(false);
    storeMock = createMockAndAddToDefault(XWikiStoreInterface.class);
    doc.setStore(storeMock);
    doc.setNew(false);
    doc.setOriginalDocument(new XWikiDocument(doc.getDocumentReference()));
    doc.getOriginalDocument().setNew(false);
    ModelAccessStore modelAccessStoreMock = createMockAndAddToDefault(ModelAccessStore.class);
    registerComponentMock(XWikiStoreInterface.class, ModelAccessStore.NAME, modelAccessStoreMock);
    getConfigurationSource().setProperty("celements.store.main", ModelAccessStore.NAME);
    expect(modelAccessStoreMock.getBackingStore()).andReturn(storeMock).anyTimes();
    classRef = new DocumentReference("db", "class", "any");
    classRef2 = new DocumentReference("db", "class", "other");
    // important for unstable-2.0 set database because class references are checked for db
    getContext().setDatabase("db");
    getContext().setUser("user");
    modelAccess = (DefaultModelAccessFacade) Utils.getComponent(IModelAccessFacade.class);
  }

  @Test
  public void test_getDocument() throws Exception {
    doc.setFromCache(false);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument ret = modelAccess.getDocument(doc.getDocumentReference());
    verifyDefault();
    assertEquals(doc, ret);
    assertFalse(ret.isNew());
    assertFalse(ret.isFromCache());
    assertSame("do not clone if isNew", doc, ret);
  }

  @Test
  public void test_getDocument_failed() throws Exception {
    Throwable cause = new XWikiException();
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andThrow(cause);
    replayDefault();
    try {
      modelAccess.getDocument(doc.getDocumentReference());
      fail("expecting DocumentLoadException");
    } catch (DocumentLoadException exc) {
      assertSame(cause, exc.getCause());
    }
    verifyDefault();
  }

  @Test
  public void test_getDocument_notExists() throws Exception {
    doc.setNew(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    try {
      modelAccess.getDocument(doc.getDocumentReference());
      fail("expecting DocumentNotExistsException");
    } catch (DocumentNotExistsException exc) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getDocument_null() throws Exception {
    try {
      modelAccess.getDocument(null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_getDocument_cloneFromCache() throws Exception {
    String lang = "";
    doc.setDefaultLanguage("");
    doc.setLanguage("");
    doc.setFromCache(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument theDoc = modelAccess.getDocument(doc.getDocumentReference(), lang);
    verifyDefault();
    assertNotSame(doc, theDoc);
    assertSame("docRef clone is not required anymore due to immutable DocumentReference",
        doc.getDocumentReference(), theDoc.getDocumentReference());
  }

  @Test
  public void test_getDocument_translatedDocument_defaultLanguage_empty() throws Exception {
    String lang = "de";
    XWikiDocument mainDoc = new XWikiDocument(doc.getDocumentReference());
    mainDoc.setNew(false);
    mainDoc.setDefaultLanguage("");
    doc.setLanguage(lang);
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, lang);
    expect(storeMock.loadXWikiDoc(eqRefLang(mainDoc), same(getContext()))).andReturn(mainDoc);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument theDoc = modelAccess.getDocument(doc.getDocumentReference(), lang);
    verifyDefault();
    assertSame(doc, theDoc);
  }

  @Test
  public void test_getDocument_translatedDocument_noTranslation() throws Exception {
    String defaultLang = "de";
    String lang = "en";
    XWikiDocument mainDoc = new XWikiDocument(doc.getDocumentReference());
    mainDoc.setNew(false);
    mainDoc.setDefaultLanguage(defaultLang);
    doc.setLanguage("");
    doc.setNew(true);
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, defaultLang);
    expect(storeMock.loadXWikiDoc(eqRefLang(mainDoc), same(getContext()))).andReturn(mainDoc);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    try {
      modelAccess.getDocument(doc.getDocumentReference(), lang);
      fail("expecting DocumentNotExistsException");
    } catch (DocumentNotExistsException exc) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getDocument_translatedDocument() throws Exception {
    String defaultLang = "de";
    String lang = "en";
    XWikiDocument mainDoc = new XWikiDocument(doc.getDocumentReference());
    mainDoc.setNew(false);
    mainDoc.setDefaultLanguage(defaultLang);
    doc.setLanguage(lang);
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, defaultLang);
    expect(storeMock.loadXWikiDoc(eqRefLang(mainDoc), same(getContext()))).andReturn(mainDoc);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument theDoc = modelAccess.getDocument(doc.getDocumentReference(), lang);
    verifyDefault();
    assertSame(doc, theDoc);
  }

  @Test
  public void test_getDocument_mainDocByDefaultLang() throws Exception {
    String lang = "de";
    doc.setDefaultLanguage(lang);
    doc.setLanguage("");
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, lang);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument theDoc = modelAccess.getDocument(doc.getDocumentReference(), lang);
    verifyDefault();
    assertSame(doc, theDoc);
  }

  @Test
  public void test_createDocument() throws Exception {
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(false);
    replayDefault();
    XWikiDocument ret = modelAccess.createDocument(doc.getDocumentReference());
    verifyDefault();
    assertEquals(doc.getDocumentReference(), ret.getDocumentReference());
    assertEquals("", ret.getLanguage());
    assertFalse(ret.isFromCache());
  }

  @Test
  public void test_createDocument_translation() throws Exception {
    String lang = "de";
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage(lang);
    transDoc.setNew(true);
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, lang);
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
    replayDefault();
    XWikiDocument ret = modelAccess.createDocument(doc.getDocumentReference(), lang);
    verifyDefault();
    assertEquals(transDoc.getDocumentReference(), ret.getDocumentReference());
    assertEquals(lang, ret.getLanguage());
    assertFalse(ret.isFromCache());
  }

  @Test
  public void test_createDocument_alreadyExists() throws Exception {
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    replayDefault();
    try {
      modelAccess.createDocument(doc.getDocumentReference());
      fail("expecting DocumentAlreadyExistsException");
    } catch (DocumentAlreadyExistsException exc) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_createDocument_null() throws Exception {
    try {
      modelAccess.createDocument(null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_getOrCreateDocument_get() throws Exception {
    doc.setNew(false);
    doc.setFromCache(false);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument ret = modelAccess.getOrCreateDocument(doc.getDocumentReference());
    verifyDefault();
    assertEquals(doc, ret);
    assertFalse(ret.isNew());
    assertFalse(ret.isFromCache());
    assertSame("do not clone if isNew", doc, ret);
    assertFalse(doc.isMetaDataDirty());
  }

  @Test
  public void test_getOrCreateDocument_get_failed() throws Exception {
    Throwable cause = new DocumentLoadException(doc.getDocumentReference());
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andThrow(cause);
    replayDefault();
    try {
      modelAccess.getOrCreateDocument(doc.getDocumentReference());
      fail("expecting DocumentLoadException");
    } catch (DocumentLoadException exc) {
      assertSame(cause, exc);
    }
    verifyDefault();
  }

  @Test
  public void test_getOrCreateDocument_create() throws Exception {
    doc.setNew(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    XWikiDocument ret = modelAccess.getOrCreateDocument(doc.getDocumentReference());
    verifyDefault();
    assertEquals(doc.getDocumentReference(), ret.getDocumentReference());
    assertTrue(ret.isNew());
    assertFalse(ret.isFromCache());
  }

  @Test
  public void test_getOrCreateDocument_null() throws Exception {
    try {
      modelAccess.getOrCreateDocument(null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_exists_true() throws Exception {
    expect(storeMock.exists(eqRefLang(doc), same(getContext())))
        .andReturn(true);
    replayDefault();
    boolean ret = modelAccess.exists(doc.getDocumentReference());
    verifyDefault();
    assertTrue(ret);
  }

  @Test
  public void test_exists_false() throws Exception {
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(false);
    replayDefault();
    boolean ret = modelAccess.exists(doc.getDocumentReference());
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_exists_null() throws Exception {
    replayDefault();
    boolean ret = modelAccess.exists(null);
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_existsLang() throws Exception {
    String lang = "fr";
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage(lang);
    transDoc.setNew(false);
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
    replayDefault();
    boolean ret = modelAccess.existsLang(doc.getDocumentReference(), lang);
    verifyDefault();
    assertTrue(ret);
  }

  @Test
  public void test_existsLang_isMainDoc() throws Exception {
    String lang = "en";
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    boolean ret = modelAccess.existsLang(doc.getDocumentReference(), lang);
    verifyDefault();
    assertTrue(ret);
  }

  @Test
  public void test_existsLang_false() throws Exception {
    String lang = "fr";
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage(lang);
    transDoc.setNew(true);
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
    replayDefault();
    boolean ret = modelAccess.existsLang(doc.getDocumentReference(), lang);
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_existsLang_false_noMainDoc() throws Exception {
    String lang = "fr";
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(false);
    replayDefault();
    boolean ret = modelAccess.existsLang(doc.getDocumentReference(), lang);
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_existsLang_none() throws Exception {
    // empty lang instead of null
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(false);
    replayDefault();
    boolean ret = modelAccess.existsLang(doc.getDocumentReference(), null);
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_existsLang_null() throws Exception {
    replayDefault();
    boolean ret = modelAccess.existsLang(null, null);
    verifyDefault();
    assertFalse(ret);
  }

  @Test
  public void test_saveDocument() throws Exception {
    doc.setMinorEdit(true);
    doc.setComment("ignoreme");
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    assertEquals("", doc.getComment());
    assertFalse(doc.isMinorEdit());
  }

  @Test
  public void test_saveDocument_create() throws Exception {
    doc.setNew(true);
    getMock(ObservationManager.class).notify(isA(DocumentCreatingEvent.class), same(doc),
        same(getContext()));
    storeMock.saveXWikiDoc(same(doc), same(getContext()));
    getMock(ObservationManager.class).notify(isA(DocumentCreatedEvent.class), eqRefLang(doc),
        same(getContext()));
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
  }

  @Test
  public void test_saveDocument_saveException() throws Exception {
    Throwable cause = new XWikiException();
    getMock(ObservationManager.class).notify(isA(DocumentUpdatingEvent.class), same(doc),
        same(getContext()));
    storeMock.saveXWikiDoc(same(doc), same(getContext()));
    expectLastCall().andThrow(cause);
    replayDefault();
    try {
      modelAccess.saveDocument(doc);
      fail("expecting DocumentSaveException");
    } catch (DocumentSaveException exc) {
      assertSame(cause, exc.getCause());
    }
    verifyDefault();
  }

  @Test
  public void test_saveDocument_checkAuthor() throws Exception {
    String username = "XWiki.TestUser";
    getContext().setUser(username);
    doc.setAuthor("XWiki.OldAuthor");
    String oldCreator = "XWiki.OldCreator";
    doc.setCreator(oldCreator);
    Capture<XWikiDocument> docCapture = newCapture();
    getMock(ObservationManager.class).notify(isA(DocumentUpdatingEvent.class), same(doc),
        same(getContext()));
    storeMock.saveXWikiDoc(capture(docCapture), same(getContext()));
    getMock(ObservationManager.class).notify(isA(DocumentUpdatedEvent.class), eqRefLang(doc),
        same(getContext()));
    doc.setMetaDataDirty(false);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    XWikiDocument docSaved = docCapture.getValue();
    assertEquals(username, docSaved.getAuthor());
    assertEquals(oldCreator, docSaved.getCreator());
    assertTrue(docSaved.isMetaDataDirty());
  }

  @Test
  public void test_saveDocument_checkAuthor_Creator_isNew() throws Exception {
    String username = "XWiki.TestUser";
    getContext().setUser(username);
    doc.setNew(true);
    doc.setAuthor("XWiki.OldAuthor");
    String oldCreator = "XWiki.OldCreator";
    doc.setCreator(oldCreator);
    Capture<XWikiDocument> docCapture = newCapture();
    getMock(ObservationManager.class).notify(isA(DocumentCreatingEvent.class), same(doc),
        same(getContext()));
    storeMock.saveXWikiDoc(capture(docCapture), same(getContext()));
    getMock(ObservationManager.class).notify(isA(DocumentCreatedEvent.class), eqRefLang(doc),
        same(getContext()));
    doc.setMetaDataDirty(false);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    XWikiDocument docSaved = docCapture.getValue();
    assertEquals(username, docSaved.getAuthor());
    assertEquals(username, docSaved.getCreator());
    assertTrue(docSaved.isMetaDataDirty());
  }

  @Test
  public void test_saveDocument_noDefaultLang() throws Exception {
    doc.setDefaultLanguage("");
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    assertEquals("en", doc.getDefaultLanguage());
  }

  @Test
  public void test_saveDocument_langSameAsDefault() throws Exception {
    String lang = "de";
    doc.setDefaultLanguage(lang);
    doc.setLanguage(lang);
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    assertEquals("", doc.getLanguage());
  }

  @Test
  public void test_saveDocument_mainDocWithLang() throws Exception {
    doc.setTranslation(0);
    doc.setLanguage("de");
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
    assertEquals("", doc.getLanguage());
  }

  @Test
  public void test_saveDocument_translationWithoutLang() throws Exception {
    doc.setTranslation(1);
    doc.setLanguage("");
    replayDefault();
    try {
      modelAccess.saveDocument(doc);
      fail("expecting DocumentSaveException");
    } catch (DocumentSaveException exc) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_saveDocument_null() throws Exception {
    try {
      modelAccess.saveDocument(null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_saveDocument_translation() throws Exception {
    doc.setTranslation(1);
    doc.setLanguage("de");
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc);
    verifyDefault();
  }

  @Test
  public void test_saveDocument_translation_mainInexistent() throws Exception {
    doc.setTranslation(1);
    doc.setLanguage("de");
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(false);
    replayDefault();
    try {
      modelAccess.saveDocument(doc);
      fail("expecting DocumentSaveException");
    } catch (DocumentSaveException exc) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_saveDocument_comment() throws Exception {
    String comment = "myComment";
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc, comment);
    verifyDefault();
    assertEquals(comment, doc.getComment());
  }

  @Test
  public void test_saveDocument_comment_isMinorEdit() throws Exception {
    doc.setMinorEdit(false);
    String comment = "myComment";
    boolean isMinorEdit = true;
    expectSaveWithNotify(doc);
    replayDefault();
    modelAccess.saveDocument(doc, comment, isMinorEdit);
    verifyDefault();
    assertTrue(doc.isMinorEdit());
  }

  private void expectSaveWithNotify(XWikiDocument doc) throws XWikiException {
    getMock(ObservationManager.class).notify(isA(DocumentUpdatingEvent.class), same(doc),
        same(getContext()));
    storeMock.saveXWikiDoc(same(doc), same(getContext()));
    getMock(ObservationManager.class).notify(isA(DocumentUpdatedEvent.class), eqRefLang(doc),
        same(getContext()));
  }

  @Test
  public void test_deleteDocumentWithoutTranslations() throws Exception {
    expectDeleteWithNotify(doc);
    replayDefault();
    modelAccess.deleteDocumentWithoutTranslations(doc, false);
    verifyDefault();
  }

  @Test
  public void test_deleteDocumentWithoutTranslations_totrash() throws Exception {
    getConfigurationSource().setProperty(CFG_RECYCLEBIN, "true");
    expectDeleteWithNotify(doc);
    getMock(XWikiRecycleBinStoreInterface.class).saveToRecycleBin(
        same(doc), eq("user"), geq(new Date()), same(getContext()), eq(true));
    replayDefault();
    modelAccess.deleteDocumentWithoutTranslations(doc, true);
    verifyDefault();
  }

  @Test
  public void test_deleteDocumentWithoutTranslations_XWE() throws Exception {
    expectDeleteWithNotify(doc);
    replayDefault();
    modelAccess.deleteDocumentWithoutTranslations(doc, false);
    verifyDefault();
  }

  @Test
  public void test_deleteDocument() throws Exception {
    List<String> transLangs = ImmutableList.of("en", "fr", "it");
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext()))).andReturn(transLangs);
    doc.setNew(false);
    doc.setDefaultLanguage("de");
    List<XWikiDocument> transDocs = transLangs.stream().map(lang -> {
      XWikiDocument transDoc = doc.clone();
      transDoc.setLanguage(lang);
      return transDoc;
    }).collect(toImmutableList());
    for (XWikiDocument transDoc : transDocs) {
      expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
      expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
      expectDeleteWithNotify(transDoc);
    }
    expectDeleteWithNotify(doc);
    replayDefault();
    modelAccess.deleteDocument(doc, false);
    verifyDefault();
  }

  private void expectDeleteWithNotify(XWikiDocument doc) throws XWikiException {
    getMock(ObservationManager.class).notify(isA(DocumentDeletingEvent.class), same(doc),
        same(getContext()));
    storeMock.deleteXWikiDoc(same(doc), same(getContext()));
    getMock(ObservationManager.class).notify(isA(DocumentDeletedEvent.class), eqRefLang(doc),
        same(getContext()));
  }

  @Test
  public void test_getExistingLangs() throws Exception {
    List<String> langs = ImmutableList.of("de", "en");
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext()))).andReturn(langs);
    replayDefault();
    List<String> ret = modelAccess.getExistingLangs(doc.getDocumentReference());
    verifyDefault();
    assertEquals(langs, ret);
  }

  @Test
  public void test_getExistingLangs_none() throws Exception {
    List<String> langs = ImmutableList.of();
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext()))).andReturn(langs);
    replayDefault();
    List<String> ret = modelAccess.getExistingLangs(doc.getDocumentReference());
    verifyDefault();
    assertEquals(langs, ret);
  }

  @Test
  public void test_getExistingLangs_DLE() throws Exception {
    DocumentReference docRef = doc.getDocumentReference();
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext())))
        .andThrow(new XWikiException());
    replayDefault();
    assertThrows(DocumentLoadException.class, () -> modelAccess.getExistingLangs(docRef));
    verifyDefault();
  }

  @Test
  public void test_getTranslations() throws Exception {
    List<String> transLangs = ImmutableList.of("en", "fr", "it");
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext()))).andReturn(transLangs);
    doc.setNew(false);
    doc.setDefaultLanguage("de");
    List<XWikiDocument> transDocs = transLangs.stream().map(lang -> {
      XWikiDocument transDoc = doc.clone();
      transDoc.setLanguage(lang);
      return transDoc;
    }).collect(toImmutableList());
    for (XWikiDocument transDoc : transDocs) {
      expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
      expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
    }
    replayDefault();
    Map<String, XWikiDocument> ret = modelAccess.getTranslations(doc.getDocumentReference());
    verifyDefault();
    assertEquals(transLangs.size(), ret.size());
    for (XWikiDocument langDoc : transDocs) {
      assertSame(langDoc, ret.get(langDoc.getLanguage()));
    }
  }

  @Test
  public void test_getTranslations_notExists() throws Exception {
    String transLang = "fr";
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext())))
        .andReturn(ImmutableList.of(transLang));
    XWikiDocument mainDoc = new XWikiDocument(doc.getDocumentReference());
    mainDoc.setNew(false);
    mainDoc.setDefaultLanguage("de");
    expect(storeMock.loadXWikiDoc(eqRefLang(mainDoc), same(getContext()))).andReturn(mainDoc);
    XWikiDocument transDoc = mainDoc.clone();
    transDoc.setNew(true);
    transDoc.setLanguage(transLang);
    expect(storeMock.loadXWikiDoc(eqRefLang(transDoc), same(getContext()))).andReturn(transDoc);
    replayDefault();
    Map<String, XWikiDocument> ret = modelAccess.getTranslations(doc.getDocumentReference());
    verifyDefault();
    assertTrue(ret.isEmpty());
  }

  @Test
  public void test_getTranslations_DLE() throws Exception {
    DocumentReference docRef = doc.getDocumentReference();
    expect(storeMock.getTranslationList(eqRefLang(doc), same(getContext())))
        .andThrow(new XWikiException());
    replayDefault();
    assertThrows(DocumentLoadException.class, () -> modelAccess.getTranslations(docRef));
    verifyDefault();
  }

  @Test
  public void test_isTranslation() throws Exception {
    assertFalse(modelAccess.isTranslation(doc));
  }

  @Test
  public void test_isTranslation_false() throws Exception {
    doc.setTranslation(1);
    assertTrue(modelAccess.isTranslation(doc));
  }

  @Test
  public void test_streamParents_none() {
    replayDefault();
    List<XWikiDocument> parents = modelAccess.streamParents(doc).collect(toList());
    verifyDefault();
    assertTrue(parents.isEmpty());
  }

  @Test
  public void test_streamParents_one() throws Exception {
    XWikiDocument pDoc = expectParent(doc, true);
    replayDefault();
    List<XWikiDocument> parents = modelAccess.streamParents(doc).collect(toList());
    verifyDefault();
    assertEquals(1, parents.size());
    assertSame(pDoc, parents.get(0));
  }

  @Test
  public void test_streamParents_multiple() throws Exception {
    XWikiDocument pDoc = expectParent(doc, true);
    XWikiDocument ppDoc = expectParent(pDoc, true);
    expectParent(ppDoc, false);
    replayDefault();
    List<XWikiDocument> parents = modelAccess.streamParents(doc).collect(toList());
    verifyDefault();
    assertEquals(2, parents.size());
    assertSame(pDoc, parents.get(0));
    assertSame(ppDoc, parents.get(1));
  }

  @Test
  public void test_streamParents_cyclic() throws Exception {
    XWikiDocument pDoc = expectParent(doc, true);
    pDoc.setParentReference((EntityReference) doc.getDocumentReference());
    expect(storeMock.exists(eqRefLang(doc), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    Stream<XWikiDocument> stream = modelAccess.streamParents(doc);
    assertThrows(IllegalStateException.class, stream::count);
    verifyDefault();
  }

  private XWikiDocument expectParent(XWikiDocument doc, boolean exists) throws Exception {
    DocumentReference parentDocRef = RefBuilder.from(doc.getDocumentReference())
        .doc(doc.getDocumentReference().getName() + "-parent")
        .build(DocumentReference.class);
    XWikiDocument parentDoc = new XWikiDocument(parentDocRef);
    parentDoc.setNew(!exists);
    doc.setParentReference((EntityReference) parentDocRef);
    expect(storeMock.exists(eqRefLang(parentDoc), same(getContext())))
        .andReturn(exists).atLeastOnce();
    if (exists) {
      expect(storeMock.loadXWikiDoc(eqRefLang(parentDoc), same(getContext())))
          .andReturn(parentDoc);
    }
    return parentDoc;
  }

  @Test
  public void test_getXObjects_nullDoc() {
    try {
      modelAccess.getXObjects((XWikiDocument) null, null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_getXObjects_nullClassRef() {
    try {
      modelAccess.getXObjects(doc, null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_getXObjects_emptyDoc() {
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef);
    assertEquals(ret.size(), 0);
  }

  @Test
  public void test_getXObjects_withObj() {
    BaseObject obj = addObj(classRef, null, null);
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef);
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(0));
  }

  @Test
  public void test_getXObjects_mutlipleObj() {
    BaseObject obj1 = addObj(classRef, null, null);
    addObj(classRef2, null, null);
    BaseObject obj2 = addObj(classRef, null, null);
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef);
    assertEquals(2, ret.size());
    assertSame(obj1, ret.get(0));
    assertSame(obj2, ret.get(1));
  }

  @Test
  public void test_getXObjects_otherWikiRef() {
    BaseObject obj = addObj(classRef, null, null);
    // IMPORTANT do not use setWikiReference, because it is dropped in xwiki 4.5.4
    classRef = new DocumentReference("otherWiki",
        classRef.getLastSpaceReference().getName(), classRef.getName());
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef);
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(0));
  }

  @Test
  public void test_getXObjects_key() {
    String key = "field";
    String val = "val";
    addObj(classRef, null, null);
    addObj(classRef2, key, val);
    BaseObject obj1 = addObj(classRef, key, val);
    addObj(classRef, key, null);
    BaseObject obj2 = addObj(classRef, key, val);
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef, key, val);
    assertEquals(2, ret.size());
    assertSame(obj1, ret.get(0));
    assertSame(obj2, ret.get(1));
  }

  @Test
  public void test_getXObjects_key_values() {
    String key = "field";
    List<String> vals = Arrays.asList("val1", "val2");
    addObj(classRef, null, null);
    addObj(classRef2, key, vals.get(0));
    BaseObject obj1 = addObj(classRef, key, vals.get(0));
    addObj(classRef, key, null);
    BaseObject obj2 = addObj(classRef, key, vals.get(1));
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef, key, vals);
    assertEquals(2, ret.size());
    assertSame(obj1, ret.get(0));
    assertSame(obj2, ret.get(1));
  }

  @Test
  public void test_getXObjects_isTranslation() throws Exception {
    doc.setTranslation(1);
    replayDefault();
    try {
      modelAccess.getXObjects(doc, classRef);
      fail("expecting IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getXObjects_undmodifiable() throws Exception {
    addObj(classRef, null, null);
    List<BaseObject> ret = modelAccess.getXObjects(doc, classRef);
    assertEquals(1, ret.size());
    try {
      ret.remove(0);
      fail("expecting UnsupportedOperationException");
    } catch (UnsupportedOperationException exc) {
      // expected
    }
  }

  @Test
  public void test_getXObjects_docRef() throws Exception {
    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);
    replayDefault();
    List<BaseObject> ret = modelAccess.getXObjects(doc.getDocumentReference(), classRef);
    verifyDefault();
    assertEquals(0, ret.size());
  }

  @Test
  public void test_getXObjects_map() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef, null, null);
    BaseObject obj3 = addObj(classRef, null, null);
    BaseObject obj4 = addObj(classRef2, null, null);
    Map<DocumentReference, List<BaseObject>> ret = modelAccess.getXObjects(doc);
    assertEquals(2, ret.size());
    assertTrue(ret.containsKey(classRef));
    assertEquals(3, ret.get(classRef).size());
    assertEquals(obj1, ret.get(classRef).get(0));
    assertEquals(obj2, ret.get(classRef).get(1));
    assertEquals(obj3, ret.get(classRef).get(2));
    assertTrue(ret.containsKey(classRef2));
    assertEquals(1, ret.get(classRef2).size());
    assertEquals(obj4, ret.get(classRef2).get(0));
    try {
      ret.remove(classRef);
      fail("expecting UnsupportedOperationException");
    } catch (UnsupportedOperationException exc) {
      // expected
    }
  }

  @Test
  public void test_getXObjects_map_isTranslation() throws Exception {
    doc.setTranslation(1);
    replayDefault();
    try {
      modelAccess.getXObjects(doc);
      fail("expecting IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_newXObject() throws Exception {
    XWikiDocument docMock = createDocMock(doc.getDocumentReference());
    expectDefaultLang(docMock);
    BaseObject obj = createObj(classRef);
    expect(docMock.newXObject(eq(classRef), same(getContext()))).andReturn(obj).once();
    replayDefault();
    BaseObject ret = modelAccess.newXObject(docMock, classRef);
    verifyDefault();
    assertEquals(obj, ret);
  }

  @Test
  public void test_newXObject_loadException() throws Exception {
    Throwable cause = new XWikiException();
    XWikiDocument docMock = createDocMock(doc.getDocumentReference());
    expectDefaultLang(docMock);
    expect(docMock.newXObject(eq(classRef), same(getContext()))).andThrow(cause).once();
    replayDefault();
    try {
      modelAccess.newXObject(docMock, classRef);
      fail("expecting ClassDocumentLoadException");
    } catch (ClassDocumentLoadException exc) {
      assertSame(cause, exc.getCause());
    }
    verifyDefault();
  }

  @Test
  public void test_newXObject_otherWikiRef() throws Exception {
    XWikiDocument docMock = createDocMock(doc.getDocumentReference());
    expectDefaultLang(docMock);
    BaseObject obj = createObj(classRef);
    expect(docMock.newXObject(eq(classRef), same(getContext()))).andReturn(obj).once();
    classRef = new DocumentReference("otherWiki",
        classRef.getLastSpaceReference().getName(), classRef.getName());
    replayDefault();
    BaseObject ret = modelAccess.newXObject(docMock, classRef);
    verifyDefault();
    assertEquals(obj, ret);
  }

  @Test
  public void test_newXObject_nullDoc() throws Exception {
    replayDefault();
    try {
      modelAccess.newXObject((XWikiDocument) null, classRef);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_newXObject_nullClassRef() throws Exception {
    replayDefault();
    try {
      modelAccess.newXObject(doc, null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getOrgCreateXObject_get() throws Exception {
    BaseObject obj = addObj(classRef, null, null);

    replayDefault();
    BaseObject ret = modelAccess.getOrCreateXObject(doc, classRef);
    verifyDefault();

    assertSame(obj, ret);
  }

  @Test
  public void test_getOrgCreateXObject_create() throws Exception {
    expectNewBaseObject(classRef);

    replayDefault();
    BaseObject ret = modelAccess.getOrCreateXObject(doc, classRef);
    verifyDefault();

    assertEquals(classRef, ret.getXClassReference());
  }

  @Test
  public void test_getOrgCreateXObject_create_set() throws Exception {
    String key = "field";
    String val = "val";
    BaseClass bClassMock = expectNewBaseObject(classRef);
    expect(bClassMock.get(eq(key))).andReturn(new StringClass()).anyTimes();

    replayDefault();
    BaseObject ret = modelAccess.getOrCreateXObject(doc, classRef, key, val);
    verifyDefault();

    assertEquals(classRef, ret.getXClassReference());
    assertEquals(val, ret.getStringValue(key));
  }

  @Test
  public void test_removeXObjects() {
    addObj(classRef, null, null);
    assertTrue(modelAccess.removeXObjects(doc, classRef));
    assertEquals(0, modelAccess.getXObjects(doc, classRef).size());
  }

  @Test
  public void test_removeXObjects_noChange() {
    addObj(classRef2, null, null);
    assertFalse(modelAccess.removeXObjects(doc, classRef));
    assertEquals(0, modelAccess.getXObjects(doc, classRef).size());
    assertEquals(1, modelAccess.getXObjects(doc, classRef2).size());
  }

  @Test
  public void test_removeXObjects_mutlipleObj() {
    addObj(classRef, null, null);
    addObj(classRef2, null, null);
    addObj(classRef, null, null);
    assertTrue(modelAccess.removeXObjects(doc, classRef));
    assertEquals(0, modelAccess.getXObjects(doc, classRef).size());
    assertEquals(1, modelAccess.getXObjects(doc, classRef2).size());
  }

  @Test
  public void test_getProperty_String() throws Exception {
    String name = "name";
    String val = "val";
    BaseObject obj = createObj(classRef, name, val);

    replayDefault();
    Object ret = modelAccess.getProperty(obj, name);
    verifyDefault();

    assertEquals(val, ret);
  }

  @Test
  public void test_getProperty_String_emptyString() throws Exception {
    String name = "name";
    String val = "";
    BaseObject obj = createObj(classRef, name, val);

    replayDefault();
    Object ret = modelAccess.getProperty(obj, name);
    verifyDefault();

    assertNull(ret);
  }

  @Test
  public void test_getProperty_Number() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    int val = 5;
    obj.setIntValue(name, val);

    replayDefault();
    Object ret = modelAccess.getProperty(obj, name);
    verifyDefault();

    assertEquals(val, ret);
  }

  @Test
  public void test_getProperty_Date() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    Date val = new Date();
    obj.setDateValue(name, val);

    replayDefault();
    Object ret = modelAccess.getProperty(obj, name);
    verifyDefault();

    assertEquals(val, ret);
  }

  @Test
  public void test_getProperty_Date_Timestamp() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    Date date = new Date();
    Timestamp val = new Timestamp(date.getTime());
    obj.setDateValue(name, val);

    replayDefault();
    Object ret = modelAccess.getProperty(obj, name);
    verifyDefault();

    assertEquals(date, ret);
  }

  @Test
  public void test_getProperty_ClassField() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);

    replayDefault();
    String ret = modelAccess.getProperty(doc, field);
    verifyDefault();

    assertEquals(val, ret);
  }

  @Test
  public void test_getProperty_ClassField_illegalField() throws Exception {
    ClassField<Date> field = new DateField.Builder(CLASS_REF, "name").build();
    addObj(field.getClassReference().getDocRef(), field.getName(), "val");

    replayDefault();
    try {
      modelAccess.getProperty(doc, field);
      fail("expecting FieldAccessException");
    } catch (FieldAccessException exc) {
      assertTrue(exc.getMessage().contains(field.toString()));
      assertTrue(exc.getCause().getClass().equals(ClassCastException.class));
    } finally {
      verifyDefault();
    }

  }

  @Test
  public void test_getProperty_ClassField_docRef() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);

    expect(storeMock.loadXWikiDoc(eqRefLang(doc), same(getContext()))).andReturn(doc);

    replayDefault();
    String ret = modelAccess.getProperty(doc.getDocumentReference(), field);
    verifyDefault();

    assertEquals(val, ret);
  }

  public void test_getFieldValue_doc() {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc, field);
    verifyDefault();
    assertNotNull(ret);
    assertTrue(ret.isPresent());
    assertEquals(val, ret.get());
  }

  public void test_getFieldValue_doc_null() {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    addObj(field.getClassReference().getDocRef(), field.getName(), null);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc, field);
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  public void test_getFieldValue_docRef() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), null);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc.getDocumentReference(), field);
    verifyDefault();
    assertNotNull(ret);
    assertTrue(ret.isPresent());
    assertEquals(val, ret.get());
  }

  public void test_getFieldValue_docRef_null() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    addObj(field.getClassReference().getDocRef(), field.getName(), null);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc.getDocumentReference(), field);
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  public void test_getFieldValue_doc_ignore_hasValue() {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc, field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertTrue(ret.isPresent());
    assertEquals(val, ret.get());
  }

  public void test_getFieldValue_doc_ignore_noValue() {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    addObj(field.getClassReference().getDocRef(), field.getName(), null);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc, field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  public void test_getFieldValue_doc_ignore_hasIgnoreValue() {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "test";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc, field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  public void test_getFieldValue_docRef_ignore_hasValue() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "val";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc.getDocumentReference(), field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertTrue(ret.isPresent());
    assertEquals(val, ret.get());
  }

  public void test_getFieldValue_docRef_ignore_noValue() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    addObj(field.getClassReference().getDocRef(), field.getName(), null);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc.getDocumentReference(), field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  public void test_getFieldValue_docRef_ignore_hasIgnoreValue() throws Exception {
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    String val = "test";
    addObj(field.getClassReference().getDocRef(), field.getName(), val);
    replayDefault();
    Optional<String> ret = modelAccess.getFieldValue(doc.getDocumentReference(), field, "test");
    verifyDefault();
    assertNotNull(ret);
    assertFalse(ret.isPresent());
  }

  @Test
  public void test_getProperties() {
    ClassDefinition classDef = Utils.getComponent(ClassDefinition.class, TestClassDefinition.NAME);
    String val = "value";
    addObj(classDef.getClassRef(), FIELD_MY_STRING.getName(), val);

    replayDefault();
    List<ClassFieldValue<?>> ret = modelAccess.getProperties(doc, classDef);
    verifyDefault();

    assertEquals(classDef.getFields().size(), ret.size());
    for (int i = 0; i < ret.size(); i++) {
      ClassField<?> field = classDef.getFields().get(i);
      assertEquals(field, ret.get(i).getField());
      Object value = ret.get(i).getValue();
      if (field.equals(FIELD_MY_STRING)) {
        assertEquals(val, value);
      } else if (field instanceof ListField) {
        assertTrue(ret.get(i).toString(), ((List<?>) value).isEmpty());
      } else {
        assertNull(ret.get(i).toString(), value);
      }
    }
  }

  @Test
  public void test_setProperty_String() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    String val = "val";

    expectPropertyClass(classRef, name, new StringClass());

    replayDefault();
    modelAccess.setProperty(obj, name, val);
    verifyDefault();

    assertEquals(1, obj.getFieldList().size());
    assertEquals(val, ((BaseProperty) obj.get(name)).getValue());
  }

  @Test
  public void test_setProperty_Number() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    long val = 5;

    expectPropertyClass(classRef, name, new NumberClass());

    replayDefault();
    modelAccess.setProperty(obj, name, val);
    verifyDefault();

    assertEquals(1, obj.getFieldList().size());
    assertEquals(val, ((BaseProperty) obj.get(name)).getValue());
  }

  @Test
  public void test_setProperty_Date() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";
    Date val = new Date();

    expectPropertyClass(classRef, name, new DateClass());

    replayDefault();
    modelAccess.setProperty(obj, name, val);
    verifyDefault();

    assertEquals(1, obj.getFieldList().size());
    assertEquals(val, ((BaseProperty) obj.get(name)).getValue());
  }

  @Test
  public void test_setProperty_List() throws Exception {
    BaseObject obj = createObj(classRef);
    String name = "name";

    expectPropertyClass(classRef, name, new StringClass());

    replayDefault();
    modelAccess.setProperty(obj, name, Arrays.asList("A", "B"));
    verifyDefault();

    assertEquals(1, obj.getFieldList().size());
    assertEquals("A|B", ((BaseProperty) obj.get(name)).getValue());
  }

  @Test
  public void test_setProperty_ClassField() throws Exception {
    String val = "val";
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    ClassFieldValue<String> fieldValue = new ClassFieldValue<>(field, val);
    BaseObject obj = addObj(field.getClassReference().getDocRef(), field.getName(), "");

    expectPropertyClass(field.getClassReference().getDocRef(), field.getName(), new StringClass());

    replayDefault();
    modelAccess.setProperty(doc, fieldValue);
    verifyDefault();

    assertEquals(1, obj.getFieldList().size());
    assertEquals(val, obj.getStringValue(field.getName()));
  }

  @Test
  public void test_setProperty_ClassField_illegalField() throws Exception {
    ClassField<Date> field = new DateField.Builder(CLASS_REF, "name").build();
    ClassFieldValue<Date> fieldValue = new ClassFieldValue<>(field, new Date());
    BaseClass bClass = expectNewBaseObject(field.getClassReference().getDocRef());

    expectPropertyClass(bClass, field.getName(), new StringClass());

    replayDefault();
    try {
      modelAccess.setProperty(doc, fieldValue);
      fail("expecting FieldAccessException");
    } catch (FieldAccessException exc) {
      assertTrue(exc.getMessage().contains(field.toString()));
      assertTrue(exc.getCause().getClass().equals(ClassCastException.class));
    } finally {
      verifyDefault();
    }
  }

  @Test
  public void test_setProperty_ClassField_newObj() throws Exception {
    String val = "val";
    ClassField<String> field = new StringField.Builder(CLASS_REF, "name").build();
    ClassFieldValue<String> fieldValue = new ClassFieldValue<>(field, val);
    BaseClass bClass = expectNewBaseObject(field.getClassReference().getDocRef());
    expectPropertyClass(bClass, field.getName(), new StringClass());

    replayDefault();
    modelAccess.setProperty(doc, fieldValue);
    verifyDefault();

    BaseObject obj = doc.getXObject(field.getClassReference().getDocRef());
    assertEquals(1, obj.getFieldList().size());
    assertEquals(val, obj.getStringValue(field.getName()));
  }

  @Test
  public void test_setProperty_getProperty_customField() throws Exception {
    ClassField<DocumentReference> field = FIELD_MY_DOCREF;
    DocumentReference toStoreRef = new DocumentReference("myDB", "mySpace", "myDoc");

    BaseClass bClass = expectNewBaseObject(field.getClassReference().getDocRef());
    expectPropertyClass(bClass, field.getName(), new StringClass());

    replayDefault();
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, toStoreRef));
    DocumentReference ret = modelAccess.getProperty(doc, field);
    verifyDefault();

    assertEquals(toStoreRef, ret);
    String objValue = modelAccess.getXObject(doc, field.getClassReference().getDocRef())
        .getStringValue(field.getName());
    assertEquals(modelAccess.modelUtils.serializeRef(toStoreRef), objValue);
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
    XWikiAttachment att = modelAccess.getAttachmentNameEqual(doc, filename);
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
    try {
      modelAccess.getAttachmentNameEqual(doc, filename);
      fail("AttachmentNotExistsException expected");
    } catch (AttachmentNotExistsException exp) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getApiDocument_hasAccess() throws Exception {
    expect(getMock(IRightsAccessFacadeRole.class).hasAccessLevel(doc.getDocumentReference(),
        EAccessLevel.VIEW)).andReturn(true);
    replayDefault();
    Document apiDoc = modelAccess.getApiDocument(doc);
    assertNotNull("Expected Attachment api object - not null", apiDoc);
    verifyDefault();
  }

  @Test
  public void test_getApiDocument_noAccess() throws Exception {
    expect(getMock(IRightsAccessFacadeRole.class).hasAccessLevel(doc.getDocumentReference(),
        EAccessLevel.VIEW)).andReturn(false);
    replayDefault();
    try {
      modelAccess.getApiDocument(doc);
      fail("NoAccessRightsException expected");
    } catch (NoAccessRightsException exp) {
      // expected
    }
    verifyDefault();
  }

  private void expectDefaultLang(XWikiDocument docMock) {
    expect(docMock.getTranslation()).andReturn(0).once();
  }

  private BaseObject addObj(DocumentReference classRef, String key, String value) {
    BaseObject obj = createObj(classRef, key, value);
    doc.addXObject(obj);
    return obj;
  }

  private BaseObject createObj(DocumentReference classRef) {
    return createObj(classRef, null, null);
  }

  private BaseObject createObj(DocumentReference classRef, String key, String value) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classRef);
    if (key != null) {
      obj.setStringValue(key, value);
    }
    return obj;
  }

  private class TestXWikiDocumentCreator implements XWikiDocumentCreator {

    @Override
    public XWikiDocument createWithoutDefaults(DocumentReference docRef, String lang) {
      XWikiDocument doc = new XWikiDocument(docRef);
      doc.setLanguage(lang);
      return doc;
    }

    @Override
    public XWikiDocument createWithoutDefaults(DocumentReference docRef) {
      return createWithoutDefaults(docRef, IModelAccessFacade.DEFAULT_LANG);
    }

    @Override
    public XWikiDocument create(DocumentReference docRef, String lang) {
      return createWithoutDefaults(docRef, lang);
    }

    @Override
    public XWikiDocument create(DocumentReference docRef) {
      return create(docRef, IModelAccessFacade.DEFAULT_LANG);
    }

  }

  private static XWikiDocument eqRefLang(XWikiDocument doc) {
    return cmp(doc, DocRefLangComparator.INSTANCE, LogicalOperator.EQUAL);
  }

  private static class DocRefLangComparator implements Comparator<XWikiDocument> {

    static final DocRefLangComparator INSTANCE = new DocRefLangComparator();

    @Override
    public int compare(XWikiDocument o1, XWikiDocument o2) {
      return ((o1 != o2)
          && Objects.equals(o1.getDocumentReference(), o2.getDocumentReference())
          && Objects.equals(o1.getLanguage(), o1.getLanguage()))
              ? 0
              : 1;
    }
  }

}
