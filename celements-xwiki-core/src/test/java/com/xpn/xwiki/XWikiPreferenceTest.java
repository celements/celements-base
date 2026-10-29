package com.xpn.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractComponentTest;

public class XWikiPreferenceTest extends AbstractComponentTest {

  @Test
  public void test_getXWikiPreference_fromCelDocument() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    getContext().setDatabase("wiki");
    getContext().setLanguage("de");
    DocumentReference prefRef = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
    XWikiDocument document = new XWikiDocument(prefRef);
    addPreferenceObject(document, prefRef, "en", "default");
    addPreferenceObject(document, prefRef, "de", "");
    CelDocument.Default celDocument = CelDocument.Default.from(document);
    expect(store.loadCelDocument(prefRef)).andReturn(Optional.of(celDocument));
    replayDefault();

    assertEquals("default", xwiki.getXWikiPreference("preference", getContext()));

    verifyDefault();
  }

  @Test
  public void test_getXWikiPreference_usesExplicitWikiAndLanguage() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    WikiReference callerWiki = getContext().getWikiRef();
    getContext().setLanguage("fr");
    WikiReference targetWiki = new WikiReference("other-wiki");
    DocumentReference prefRef = new DocumentReference("other-wiki", "XWiki", "XWikiPreferences");
    XWikiDocument document = new XWikiDocument(prefRef);
    addPreferenceObject(document, prefRef, "en", "default");
    addPreferenceObject(document, prefRef, "de", "german");
    expect(store.loadCelDocument(prefRef))
        .andReturn(Optional.of(CelDocument.Default.from(document))).times(2);
    replayDefault();

    assertEquals("german",
        xwiki.getXWikiPreference("preference", "", "fallback", targetWiki, "de"));
    assertEquals("default",
        xwiki.getXWikiPreference("preference", "", "fallback", targetWiki, "es"));
    assertEquals(callerWiki, getContext().getWikiRef());
    assertEquals("fr", getContext().getLanguage());

    verifyDefault();
  }

  @Test
  public void test_getSpacePreference_fromCelDocument() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    getContext().setDatabase("wiki");
    getContext().setLanguage("de");
    DocumentReference prefClassRef = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
    DocumentReference webPrefRef = new DocumentReference("wiki", "space", "WebPreferences");
    XWikiDocument document = new XWikiDocument(webPrefRef);
    addPreferenceObject(document, prefClassRef, "de", "space");
    CelDocument.Default celDocument = CelDocument.Default.from(document);
    expect(store.loadCelDocument(webPrefRef)).andReturn(Optional.of(celDocument));
    replayDefault();

    assertEquals("space",
        xwiki.getSpacePreference("preference", "space", "", getContext()));

    verifyDefault();
  }

  @Test
  public void test_getXWikiPreferenceAsInt_usesExplicitWiki() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    WikiReference callerWiki = getContext().getWikiRef();
    getContext().setLanguage("de");
    WikiReference targetWiki = new WikiReference("other-wiki");
    DocumentReference prefRef = new DocumentReference("other-wiki", "XWiki", "XWikiPreferences");
    XWikiDocument document = new XWikiDocument(prefRef);
    addPreferenceObject(document, prefRef, "en", "300");
    addPreferenceObject(document, prefRef, "de", "120");
    expect(store.loadCelDocument(prefRef))
        .andReturn(Optional.of(CelDocument.Default.from(document)));
    replayDefault();
    assertEquals(120, xwiki.getXWikiPreferenceAsInt(targetWiki, "preference", 1800));
    assertEquals(callerWiki, getContext().getWikiRef());
    verifyDefault();
  }

  @Test
  public void test_getXWikiPreferenceAsInt_invalidValueUsesDefault() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    WikiReference targetWiki = new WikiReference("other-wiki");
    DocumentReference prefRef = new DocumentReference("other-wiki", "XWiki", "XWikiPreferences");
    XWikiDocument document = new XWikiDocument(prefRef);
    addPreferenceObject(document, prefRef, "en", "invalid");
    expect(store.loadCelDocument(prefRef))
        .andReturn(Optional.of(CelDocument.Default.from(document)));
    replayDefault();
    assertEquals(1800, xwiki.getXWikiPreferenceAsInt(targetWiki, "preference", 1800));
    verifyDefault();
  }

  @Test
  public void test_getXWikiPreferenceAsInt_missingValueUsesDefault() throws Exception {
    XWiki xwiki = new XWiki();
    XWikiStoreInterface store = createDefaultMock(XWikiStoreInterface.class);
    xwiki.setStore(store);
    WikiReference targetWiki = new WikiReference("other-wiki");
    DocumentReference prefRef = new DocumentReference("other-wiki", "XWiki", "XWikiPreferences");
    expect(store.loadCelDocument(prefRef)).andReturn(Optional.empty());
    replayDefault();
    assertEquals(1800, xwiki.getXWikiPreferenceAsInt(targetWiki, "lock_Timeout", 1800));
    verifyDefault();
  }

  private void addPreferenceObject(XWikiDocument document, DocumentReference classRef,
      String language, String preference) {
    BaseObject object = new BaseObject();
    object.setDocumentReference(document.getDocumentReference());
    object.setXClassReference(classRef);
    object.setStringValue("default_language", language);
    object.setStringValue("preference", preference);
    document.addXObject(classRef, object);
  }
}
