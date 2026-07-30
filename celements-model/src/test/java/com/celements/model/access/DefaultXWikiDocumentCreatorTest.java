package com.celements.model.access;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class DefaultXWikiDocumentCreatorTest extends AbstractComponentTest {

  private DefaultXWikiDocumentCreator docCreator;
  private DocumentReference docRef;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(IModelAccessFacade.class);
    registerComponentMock(ConfigurationSource.class, "all", getConfigurationSource());
    registerComponentMock(ConfigurationSource.class, CelementsFromWikiConfigurationSource.NAME,
        getConfigurationSource());
    docCreator = (DefaultXWikiDocumentCreator) Utils.getComponent(XWikiDocumentCreator.class);
    docRef = new DocumentReference("db", "space", "doc");
  }

  @Test
  public void test_create() throws Exception {
    String defaultLang = "de";
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, defaultLang);
    Date beforeCreationDate = new Date(System.currentTimeMillis() - 1000); // doc drops ms
    expectSpacePreferences(docRef.getLastSpaceReference());
    String userName = "XWiki.TestUser";
    getContext().setUser(userName);

    replayDefault();
    XWikiDocument ret = docCreator.create(docRef);
    verifyDefault();

    assertEquals(docRef, ret.getDocumentReference());
    assertTrue(ret.isNew());
    assertFalse(ret.isFromCache());
    assertEquals(defaultLang, ret.getDefaultLanguage());
    assertEquals("", ret.getLanguage());
    assertEquals(0, ret.getTranslation());
    assertTrue(beforeCreationDate.before(ret.getCreationDate()));
    assertTrue(beforeCreationDate.before(ret.getContentUpdateDate()));
    assertTrue(beforeCreationDate.before(ret.getDate()));
    assertEquals(userName, ret.getCreator());
    assertEquals(userName, ret.getAuthor());
    assertEquals("", ret.getContent());
    assertTrue(ret.isMetaDataDirty());
  }

  @Test
  public void test_create_withLang() throws Exception {
    String defaultLang = "de";
    String lang = "en";
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, defaultLang);
    expectSpacePreferences(docRef.getLastSpaceReference());

    replayDefault();
    XWikiDocument ret = docCreator.create(docRef, lang);
    verifyDefault();

    assertEquals(defaultLang, ret.getDefaultLanguage());
    assertEquals(lang, ret.getLanguage());
    assertEquals(1, ret.getTranslation());
  }

  @Test
  public void test_create_withLang_isDefaultLang() throws Exception {
    String defaultLang = "de";
    String lang = defaultLang;
    getConfigurationSource().setProperty(ModelContext.CFG_KEY_DEFAULT_LANG, defaultLang);
    expectSpacePreferences(docRef.getLastSpaceReference());

    replayDefault();
    XWikiDocument ret = docCreator.create(docRef, lang);
    verifyDefault();

    assertEquals(defaultLang, ret.getDefaultLanguage());
    assertEquals("", ret.getLanguage());
    assertEquals(0, ret.getTranslation());
  }

  @Test
  public void test_getDefaultLangForCreatingDoc_xwikiPrefDocRef() {
    DocumentReference xwikiPrefDocRef = new DocumentReference(getContext().getDatabase(),
        ModelContext.XWIKI_SPACE,
        ModelContext.XWIKI_PREF_DOC_NAME);
    replayDefault();
    assertEquals("prevent endless recursion and Stackoverflow",
        IModelAccessFacade.DEFAULT_LANG, docCreator.getDefaultLangForCreatingDoc(xwikiPrefDocRef));
    verifyDefault();
  }

  private void expectSpacePreferences(SpaceReference spaceRef) throws DocumentNotExistsException {
    XWikiDocument webPrefDoc = new XWikiDocument(new DocumentReference(
        ModelContext.WEB_PREF_DOC_NAME, spaceRef));
    expect(getMock(IModelAccessFacade.class).getDocument(webPrefDoc.getDocumentReference()))
        .andReturn(webPrefDoc).once();
  }

  @Test
  public void test_create_null() throws Exception {
    try {
      docCreator.create(null);
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

}
