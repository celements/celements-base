package com.xpn.xwiki.doc;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.test.AbstractComponentTest;

public class CelDocumentTest extends AbstractComponentTest {

  @Test(expected = IllegalArgumentException.class)
  public void testTranslationRejectsDefaultDocument() {
    XWikiDocument document = new XWikiDocument(
        new DocumentReference("wiki", "space", "page"));

    CelDocument.Translation.from(document);
  }

  @Test
  public void testGetXObjectsByClass() {
    DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
    DocumentReference classReference = new DocumentReference("wiki", "XWiki", "Class");
    XWikiDocument document = new XWikiDocument(documentReference);
    BaseObject defaultObject = new BaseObject();
    defaultObject.setDocumentReference(documentReference);
    defaultObject.setXClassReference(classReference);
    defaultObject.setStringValue("language", "en");
    document.addXObject(classReference, defaultObject);
    BaseObject translatedObject = new BaseObject();
    translatedObject.setDocumentReference(documentReference);
    translatedObject.setXClassReference(classReference);
    translatedObject.setStringValue("language", "de");
    document.addXObject(classReference, translatedObject);

    CelDocument.Default celDocument = CelDocument.Default.from(document);

    List<CelObject> objects = celDocument.streamXObjects(classReference).toList();
    assertEquals(2, objects.size());
    assertEquals("en", objects.get(0).getStringValue("language"));
    assertEquals("de", objects.get(1).getStringValue("language"));
  }

  @Test
  public void testRoundTripAndIsolation() {
    DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
    DocumentReference classReference = new DocumentReference("wiki", "XWiki", "Class");
    XWikiDocument source = new XWikiDocument(documentReference);
    source.setId(42, IdVersion.CELEMENTS_3);
    source.setNew(false);
    source.setDefaultLanguage("en");
    source.setContent("content");
    source.setTitle("title");
    source.setCreator("creator");
    source.setAuthor("author");
    source.setContentAuthor("contentAuthor");
    source.setDate(new Date(1_000));
    source.setContentUpdateDate(new Date(2_000));
    source.setCreationDate(new Date(3_000));

    BaseClass xClass = new BaseClass();
    xClass.setDocumentReference(documentReference);
    xClass.addTextField("field", "Field", 42);
    xClass.setValidationScript("validate");
    String xClassXml = "raw XClass XML";
    source.setXClass(xClass);
    source.setXClassXML(xClassXml);

    BaseObject object = new BaseObject();
    object.setDocumentReference(documentReference);
    object.setXClassReference(classReference);
    object.setNumber(2);
    object.setGuid("guid");
    put(object, "base", new BaseProperty(), "base");
    put(object, "baseString", new BaseStringProperty(), "baseString");
    put(object, "string", new StringProperty(), "value");
    put(object, "largeString", new LargeStringProperty(), "large");
    put(object, "integer", new IntegerProperty(), 1);
    put(object, "long", new LongProperty(), 2L);
    put(object, "float", new FloatProperty(), 3.5F);
    put(object, "double", new DoubleProperty(), 4.5D);
    put(object, "date", new DateProperty(), new Date(5_000));
    put(object, "rawList", new ListProperty(), List.of("raw"));
    put(object, "list", new StringListProperty(), List.of("a", "b"));
    put(object, "dbList", new DBStringListProperty(), List.of("c", "d"));
    source.setXObject(2, object);

    XWikiAttachment attachment = new XWikiAttachment(source, "file.txt");
    attachment.setAuthor("attachmentAuthor");
    attachment.setFilesize(123);
    attachment.setVersion("2.1");
    attachment.setComment("comment");
    attachment.setDate(new Date(6_000));
    source.setAttachmentList(List.of(attachment));

    CelDocument celDocument = CelDocument.from(source);
    source.setContent("poisoned");
    ((StringClass) xClass.safeget("field")).setSize(99);
    object.setStringValue("string", "poisoned");
    attachment.setComment("poisoned");

    assertTrue(celDocument instanceof CelDocument.Default);
    CelDocument.Default defaultDocument = (CelDocument.Default) celDocument;
    CelDocument.MetaData documentMeta = celDocument.getMetaData();
    CelDocument.Identity identity = celDocument.getIdentity();
    assertSame(documentReference, identity.docRef());
    assertEquals(42, identity.id());
    assertEquals(IdVersion.CELEMENTS_3, identity.idVersion());
    assertEquals("", identity.language());
    assertEquals("1.1", identity.version());
    assertEquals("en", documentMeta.defaultLanguage());
    assertEquals("content", documentMeta.content());
    assertEquals("title", documentMeta.title());
    assertEquals("creator", documentMeta.creator());
    assertEquals("author", documentMeta.author());
    assertEquals("contentAuthor", documentMeta.contentAuthor());
    assertEquals(xClassXml, documentMeta.xClassXML());
    assertSame(documentReference, celDocument.getDocumentReference());
    assertEquals("content", celDocument.getContent());
    assertEquals("value", defaultDocument.getXObjects().get(0).getStringValue("string"));
    assertEquals(new LocalDocumentReference(classReference),
        defaultDocument.getXObjects().get(0).getClassReference());
    assertEquals("comment", defaultDocument.getAttachmentList().get(0).getComment());

    XWikiDocument first = XWikiDocument.from(celDocument);
    XWikiDocument second = XWikiDocument.from(celDocument);
    assertNotSame(first, second);
    assertNull(getXClassField(first));
    assertNull(getXClassField(second));
    assertNotSame(first.getXObject(classReference), second.getXObject(classReference));
    assertNull(getOriginalDocumentField(first));
    first.setContent("changed before original access");
    XWikiDocument originalDocument = first.getOriginalDocument();
    assertSame(originalDocument, first.getOriginalDocument());
    assertNull(originalDocument.getOriginalDocument());
    assertEquals("content", originalDocument.getContent());
    assertNotSame(originalDocument, first);
    assertNotSame(originalDocument, second.getOriginalDocument());
    assertNotSame(first.getXClass(), originalDocument.getXClass());
    assertNotSame(first.getXObject(classReference), originalDocument
        .getXObject(classReference));
    assertNotSame(first.getAttachmentList().get(0), originalDocument
        .getAttachmentList().get(0));
    assertFalse(first.isFromCache());
    assertEquals(xClassXml, first.getXClassXML());
    assertEquals(documentReference, first.getXClass().getDocumentReference());
    assertEquals(42, ((StringClass) first.getXClass().safeget("field")).getSize());
    assertEquals("validate", first.getXClass().getValidationScript());
    assertEquals(BaseProperty.class,
        first.getXObject(classReference, 2).getField("base").getClass());
    assertEquals(BaseStringProperty.class,
        first.getXObject(classReference, 2).getField("baseString").getClass());
    assertEquals(StringProperty.class,
        first.getXObject(classReference, 2).getField("string").getClass());
    assertEquals(LargeStringProperty.class,
        first.getXObject(classReference, 2).getField("largeString").getClass());
    assertEquals(IntegerProperty.class,
        first.getXObject(classReference, 2).getField("integer").getClass());
    assertEquals(LongProperty.class,
        first.getXObject(classReference, 2).getField("long").getClass());
    assertEquals(FloatProperty.class,
        first.getXObject(classReference, 2).getField("float").getClass());
    assertEquals(DoubleProperty.class,
        first.getXObject(classReference, 2).getField("double").getClass());
    assertEquals(DateProperty.class,
        first.getXObject(classReference, 2).getField("date").getClass());
    assertEquals(ListProperty.class,
        first.getXObject(classReference, 2).getField("rawList").getClass());
    assertEquals(StringListProperty.class,
        first.getXObject(classReference, 2).getField("list").getClass());
    assertEquals(DBStringListProperty.class,
        first.getXObject(classReference, 2).getField("dbList").getClass());

    first.setContent("changed");
    first.getXObject(classReference, 2).setStringValue("string", "changed");
    first.getAttachmentList().get(0).setComment("changed");
    ((StringClass) first.getXClass().safeget("field")).setSize(7);
    first.getOriginalDocument().setContent("changed");
    assertEquals("content", second.getContent());
    assertEquals(42, ((StringClass) second.getXClass().safeget("field")).getSize());
    assertEquals("value", second.getXObject(classReference, 2).getStringValue("string"));
    assertEquals("comment", second.getAttachmentList().get(0).getComment());
    assertEquals("content", second.getOriginalDocument().getContent());

    XWikiDocument withoutOriginal = XWikiDocument.from(celDocument);
    withoutOriginal.setOriginalDocument(null);
    assertNull(withoutOriginal.getOriginalDocument());
  }

  @Test
  public void testTranslationHasNoDefaultDocumentState() {
    XWikiDocument source = new XWikiDocument(
        new DocumentReference("wiki", "space", "translation"));
    source.setLanguage("de");
    source.setTranslation(1);

    CelDocument celDocument = CelDocument.from(source);

    assertTrue(celDocument instanceof CelDocument.Translation);
  }

  private void put(BaseObject object, String name, BaseProperty property, Object value) {
    property.setValue(value);
    object.safeput(name, property);
  }

  private XWikiDocument getOriginalDocumentField(XWikiDocument document) {
    try {
      Field field = XWikiDocument.class.getDeclaredField("originalDocument");
      field.setAccessible(true);
      return (XWikiDocument) field.get(document);
    } catch (ReflectiveOperationException exc) {
      throw new AssertionError(exc);
    }
  }

  private BaseClass getXClassField(XWikiDocument document) {
    try {
      Field field = XWikiDocument.class.getDeclaredField("xClass");
      field.setAccessible(true);
      return (BaseClass) field.get(document);
    } catch (ReflectiveOperationException exc) {
      throw new AssertionError(exc);
    }
  }
}
