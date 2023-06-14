package com.celements.model.util;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.util.ReferenceSerializationMode.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.XWikiConfigSource;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.web.Utils;

public class ModelUtilsTest extends AbstractComponentTest {

  WikiReference wikiRef = new WikiReference("wiki");
  SpaceReference spaceRef = new SpaceReference("space", wikiRef);
  DocumentReference docRef = new DocumentReference("doc", spaceRef);
  AttachmentReference attRef = new AttachmentReference("att.jpg", docRef);

  ModelUtils modelUtils;

  @Before
  public void prepareTest() throws Exception {
    Utils.getComponent(ModelContext.class).setWikiRef(wikiRef);
    registerComponentMock(XWikiConfigSource.class);
    modelUtils = Utils.getComponent(ModelUtils.class);
  }

  @Test
  public void test_getMainWikiRef() {
    WikiReference mainWiki = new WikiReference("main");
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.db")).andReturn(mainWiki.getName());
    replayDefault();
    assertEquals(mainWiki, modelUtils.getMainWikiRef());
    verifyDefault();
  }

  @Test
  public void test_getDatabaseName() {
    String prefix = "cel_";
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.db.prefix", "")).andReturn(prefix);

    replayDefault();
    assertEquals(prefix + wikiRef.getName(), modelUtils.getDatabaseName(wikiRef));
    verifyDefault();
  }

  @Test
  public void test_getDatabaseName_main() {
    String dbName = "main";
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.db", "")).andReturn(dbName);
    String prefix = "cel_";
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.db.prefix", "")).andReturn(prefix);

    replayDefault();
    assertEquals(prefix + dbName, modelUtils.getDatabaseName(XWikiConstant.MAIN_WIKI));
    verifyDefault();
  }

  @Test
  public void test_resolveRef() {
    WikiReference oWikiRef = new WikiReference("otherWiki");
    assertEquals(wikiRef, modelUtils.resolveRef("wiki", WikiReference.class));
    // assertEquals(wikiRef, modelUtils.resolveRef("", WikiReference.class)); TODO
    assertEquals(spaceRef, modelUtils.resolveRef("wiki:space", SpaceReference.class));
    assertEquals(spaceRef, modelUtils.resolveRef("space", SpaceReference.class));
    assertEquals(spaceRef, modelUtils.resolveRef("wiki:space", SpaceReference.class, oWikiRef));
    assertEquals(new SpaceReference(spaceRef.getName(), oWikiRef), modelUtils.resolveRef("space",
        SpaceReference.class, oWikiRef));
    assertEquals(docRef, modelUtils.resolveRef("wiki:space.doc", DocumentReference.class));
    assertEquals(docRef, modelUtils.resolveRef("space.doc", DocumentReference.class));
    assertEquals(docRef, modelUtils.resolveRef("doc", DocumentReference.class, spaceRef));
  }

  @Test
  public void test_resolveRef_noParamChange() {
    assertEquals(spaceRef, modelUtils.resolveRef("wiki:space", SpaceReference.class, wikiRef));
    assertSame(wikiRef, spaceRef.getParent());
  }

  @Test
  public void test_resolveRef_failure() {
    try {
      modelUtils.resolveRef("doc", DocumentReference.class, wikiRef);
      fail("expecting failure, space reference missing");
    } catch (IllegalArgumentException iae) {}
    try {
      modelUtils.resolveRef("doc", DocumentReference.class);
      fail("expecting failure, space reference missing");
    } catch (IllegalArgumentException iae) {}
    try {
      modelUtils.resolveRef("", DocumentReference.class);
      fail("expecting failure for empty string");
    } catch (IllegalArgumentException iae) {}
  }

  @Test
  public void test_serialzeRef() {
    assertEquals("wiki", modelUtils.serializeRef(wikiRef));
    assertEquals("wiki:space", modelUtils.serializeRef(spaceRef));
    assertEquals("wiki:space.doc", modelUtils.serializeRef(docRef));
    assertEquals("wiki:space.doc@att.jpg", modelUtils.serializeRef(attRef));
  }

  @Test
  public void test_serialzeRefLocal() {
    assertEquals("", modelUtils.serializeRefLocal(wikiRef));
    assertEquals("space", modelUtils.serializeRefLocal(spaceRef));
    assertEquals("space.doc", modelUtils.serializeRefLocal(docRef));
    assertEquals("space.doc@att.jpg", modelUtils.serializeRefLocal(attRef));
  }

  @Test
  public void test_serialzeRef_mode() {
    EntityReference ref = attRef;
    while (ref != null) {
      assertEquals(modelUtils.serializeRef(ref), modelUtils.serializeRef(ref, GLOBAL));
      assertEquals(modelUtils.serializeRefLocal(ref), modelUtils.serializeRef(ref, LOCAL));
      ref = ref.getParent();
    }
  }

  @Test
  public void test_serialzeRef_compact_sameContextWiki() {
    assertEquals("wiki", modelUtils.serializeRef(wikiRef, COMPACT));
    assertEquals("space", modelUtils.serializeRef(spaceRef, COMPACT));
    assertEquals("space.doc", modelUtils.serializeRef(docRef, COMPACT));
    assertEquals("space.doc@att.jpg", modelUtils.serializeRef(attRef, COMPACT));
  }

  @Test
  public void test_serialzeRef_compact_differentContextWiki() {
    getContext().setDatabase("xwikidb");
    assertEquals("wiki", modelUtils.serializeRef(wikiRef, COMPACT));
    assertEquals("wiki:space", modelUtils.serializeRef(spaceRef, COMPACT));
    assertEquals("wiki:space.doc", modelUtils.serializeRef(docRef, COMPACT));
    assertEquals("wiki:space.doc@att.jpg", modelUtils.serializeRef(attRef, COMPACT));
  }

  @Test
  public void test_serialzeRef_null() {
    assertThrows(NullPointerException.class, () -> modelUtils.serializeRef(null, GLOBAL));
    assertThrows(NullPointerException.class, () -> modelUtils.serializeRef(null));
    assertThrows(NullPointerException.class, () -> modelUtils.serializeRefLocal(null));
  }

  @Test
  public void test_normalizeLang() {
    assertEquals("", modelUtils.normalizeLang(null));
    assertEquals("", modelUtils.normalizeLang(""));
    assertEquals("", modelUtils.normalizeLang("  "));
    assertEquals("", modelUtils.normalizeLang("default"));
    assertEquals("de", modelUtils.normalizeLang("de"));
    assertEquals("de_CH", modelUtils.normalizeLang("de_CH"));
    assertThrows(IllegalArgumentException.class, () -> modelUtils.normalizeLang("invalid"));
  }

  @Test
  public void test_isMainWiki() {
    WikiReference mainWiki = new WikiReference("main");
    expect(getMock(XWikiConfigSource.class).getProperty("xwiki.db")).andReturn(mainWiki.getName());
    replayDefault();
    assertTrue(modelUtils.isMainWiki(mainWiki));
    assertTrue(modelUtils.isMainWiki(XWikiConstant.MAIN_WIKI));
    assertFalse(modelUtils.isMainWiki(new WikiReference("other")));
    verifyDefault();
  }

}
