package com.celements.model.access.object;

import static com.celements.model.classes.TestClassDefinition.*;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.object.cel.CelObjectBridge;
import com.celements.model.object.cel.CelObjectFetcher;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.celements.web.classes.oldcore.XWikiObjectClass;
import com.xpn.xwiki.doc.CelDocument;
import com.xpn.xwiki.doc.CelObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class CelObjectFetcherTest extends AbstractComponentTest {

  private static final ClassField<Date> FIELD_DATE = new DateField.Builder(
      CLASS_REF, "date").build();

  private WikiReference wikiRef;
  private XWikiDocument doc;

  @Before
  public void prepareTest() throws Exception {
    wikiRef = new WikiReference("db");
    doc = new XWikiDocument(new DocumentReference(wikiRef.getName(), "space", "doc"));
  }

  @Test
  public void test_fetch_and_filter() {
    addObj(CLASS_REF, FIELD_MY_STRING, "one");
    addObj(CLASS_REF, FIELD_MY_STRING, "two");
    addObj(new ClassReference("Classes", "Other"), null, null);
    CelDocument.Default celDoc = CelDocument.Default.from(doc);

    CelObject expected = celDoc.streamXObjects(CLASS_REF).findFirst().orElseThrow();
    CelObject fetched = CelObjectFetcher.on(celDoc)
        .filter(FIELD_MY_STRING, "one")
        .firstAssert();

    assertSame(expected, fetched);
    assertEquals(3, CelObjectFetcher.on(celDoc).count());
    assertEquals(2, CelObjectFetcher.on(celDoc).filter(CLASS_REF).count());
    assertEquals(List.of("one", "two"),
        CelObjectFetcher.on(celDoc).fetchField(FIELD_MY_STRING).list());
  }

  @Test
  public void test_empty() {
    assertFalse(CelObjectFetcher.empty().filter(CLASS_REF).exists());
  }

  @Test
  public void test_normalizes_string_fields() {
    addObj(CLASS_REF, FIELD_MY_STRING, "  one  ");
    addObj(CLASS_REF, FIELD_MY_STRING, "  ");
    CelDocument.Default celDoc = CelDocument.Default.from(doc);

    assertEquals(List.of("one"),
        CelObjectFetcher.on(celDoc).fetchField(FIELD_MY_STRING).list());
    assertEquals(1, CelObjectFetcher.on(celDoc).filter(FIELD_MY_STRING, "one").count());
    assertEquals(1, CelObjectFetcher.on(celDoc).filterAbsent(FIELD_MY_STRING).count());
  }

  @Test
  public void test_fetchField_types_and_pseudo_fields() {
    Date date = new Date(123456789L);
    DocumentReference valueRef = new DocumentReference("db", "target", "doc");
    BaseObject obj = addObj(CLASS_REF, FIELD_MY_INT, 42);
    obj.setDateValue(FIELD_DATE.getName(), date);
    obj.setStringListValue(FIELD_MY_LIST_MS.getName(), List.of("one", "two"));
    obj.setStringValue(FIELD_MY_DOCREF.getName(), "db:target.doc");
    CelDocument.Default celDoc = CelDocument.Default.from(doc);
    CelObjectFetcher fetcher = CelObjectFetcher.on(celDoc).filter(CLASS_REF);
    var fields = fetcher.streamFields()
        .filter(values -> values.get(XWikiObjectClass.FIELD_DOC_REF).isPresent())
        .findFirst().orElseThrow();

    assertEquals(Integer.valueOf(42), fetcher.fetchField(FIELD_MY_INT).findFirst().orElseThrow());
    assertEquals(date, fetcher.fetchField(FIELD_DATE).findFirst().orElseThrow());
    assertEquals(List.of("one", "two"),
        fetcher.fetchField(FIELD_MY_LIST_MS).findFirst().orElseThrow());
    assertEquals(valueRef, fetcher.fetchField(FIELD_MY_DOCREF).findFirst().orElseThrow());
    assertEquals(doc.getDocumentReference(),
        fields.get(XWikiObjectClass.FIELD_DOC_REF).orElseThrow());
    assertEquals(CLASS_REF,
        fields.get(XWikiObjectClass.FIELD_CLASS_REF).orElseThrow());
    assertEquals(Integer.valueOf(0),
        fields.get(XWikiObjectClass.FIELD_NUMBER).orElseThrow());
  }

  @Test
  public void test_fetchField_document_and_translation() {
    Date date = new Date(123456789L);
    doc.setTitle("default title");
    doc.setDate(date);
    CelDocument.Default celDoc = CelDocument.Default.from(doc);
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");
    transDoc.setTitle("titre");
    CelDocument.Translation celTransDoc = CelDocument.Translation.from(transDoc);
    assertEquals(date,
        CelObjectFetcher.on(celDoc).fetchField(XWikiDocumentClass.FIELD_UPDATE_DATE)
            .findFirst().orElseThrow());
    assertEquals("titre",
        CelObjectFetcher.on(celDoc).withTranslation(celTransDoc)
            .fetchField(XWikiDocumentClass.FIELD_TITLE).findFirst().orElseThrow());
  }

  @Test
  public void test_withTranslation_filters_objects() {
    doc.setDefaultLanguage("en");
    addObj(CLASS_REF, FIELD_LANG, "en");
    addObj(CLASS_REF, FIELD_LANG, "fr");
    addObj(CLASS_REF, null, null);
    CelDocument.Default celDoc = CelDocument.Default.from(doc);
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");

    List<CelObject> objects = CelObjectFetcher.on(celDoc)
        .withTranslation(CelDocument.Translation.from(transDoc))
        .filter(CLASS_REF)
        .list();

    assertEquals(1, objects.size());
    assertEquals("fr", objects.get(0).getStringValue(FIELD_LANG.getName()));
  }

  @Test
  public void test_translation_cannot_own_objects() {
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");

    IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
        () -> CelObjectFetcher.on(CelDocument.Translation.from(transDoc)).count());

    assertTrue(exc.getMessage().contains("[fr]"));
    assertTrue(exc.getMessage().contains(doc.getDocumentReference().toString()));
  }

  @Test
  public void test_mutation_is_unsupported() {
    addObj(CLASS_REF, FIELD_MY_STRING, "value");
    CelDocument.Default celDoc = CelDocument.Default.from(doc);
    CelObject obj = celDoc.getXObjects().get(0);
    CelObjectBridge bridge = getBeanFactory().getBean(CelObjectBridge.class);

    assertThrows(UnsupportedOperationException.class,
        () -> bridge.createObject(celDoc, CLASS_REF));
    assertThrows(UnsupportedOperationException.class,
        () -> bridge.deleteObject(celDoc, obj));
    assertThrows(UnsupportedOperationException.class,
        () -> bridge.getDocumentFieldAccessor().set(
            celDoc, XWikiDocumentClass.FIELD_TITLE, "changed"));
    assertThrows(UnsupportedOperationException.class,
        () -> bridge.getObjectFieldAccessor().set(obj, FIELD_MY_STRING, "changed"));
  }

  private <T> BaseObject addObj(ClassReference classRef, ClassField<T> field, T value) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classRef.getDocRef(wikiRef));
    if (field != null) {
      if (field.getType() == String.class) {
        obj.setStringValue(field.getName(), (String) value);
      } else if (field.getType() == Integer.class) {
        obj.setIntValue(field.getName(), (Integer) value);
      }
    }
    doc.addXObject(obj);
    return obj;
  }

}
