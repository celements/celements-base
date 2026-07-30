package com.xpn.xwiki;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

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
