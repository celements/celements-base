package com.celements.model.access.object;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.classes.TestClassDefinition.*;
import static com.google.common.base.MoreObjects.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.access.exception.ClassDocumentLoadException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

public class ObjectEditorTest extends AbstractComponentTest {

  private WikiReference wikiRef;
  private XWikiDocument doc;
  private ClassReference classRef;
  private ClassReference classRef2;

  @Before
  public void prepareTest() throws Exception {
    wikiRef = new WikiReference("db");
    doc = new XWikiDocument(new DocumentReference(wikiRef.getName(), "space", "doc"));
    ClassDefinition classDef = Utils.getComponent(ClassDefinition.class, NAME);
    classRef = classDef.getClassReference();
    BaseClass bClass = expectNewBaseObject(classRef.getDocRef(wikiRef));
    for (ClassField<?> field : classDef.getFields()) {
      expect(bClass.get(eq(field.getName()))).andReturn(field.getXField()).anyTimes();
    }
    classRef2 = new ClassReference("class", "other");
    expectNewBaseObject(classRef2.getDocRef(wikiRef));
  }

  private XWikiObjectEditor newEditor() {
    return XWikiObjectEditor.on(doc);
  }

  @Test
  public void test_nullDoc() throws Exception {
    new ExceptionAsserter<NullPointerException>(NullPointerException.class) {

      @Override
      protected void execute() throws Exception {
        XWikiObjectEditor.on(null);
      }
    }.evaluate();
  }

  @Test
  public void test_fetch_noClone() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    BaseObject ret = newEditor().fetch().first().get();
    assertSame(obj, ret);
  }

  @Test
  public void test_fetch_immutability() throws Exception {
    XWikiObjectEditor builder = newEditor();
    builder.filter(classRef);
    XWikiObjectFetcher fetcher = builder.fetch();
    builder.filter(classRef2);
    assertEquals(1, fetcher.getQuery().streamRestrictions().count());
  }

  @Test
  public void test_isTranslation() throws Exception {
    doc.setLanguage("en");
    doc.setTranslation(1);
    IllegalArgumentException iae = new ExceptionAsserter<IllegalArgumentException>(
        IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newEditor().fetch().stream();
      }
    }.evaluate();
    assertTrue("wrong message: " + iae.getMessage(), iae.getMessage().contains("[en]"));
    assertTrue("wrong message: " + iae.getMessage(), iae.getMessage().contains("["
        + doc.getDocumentReference() + "]"));
  }

  @Test
  public void test_create() throws Exception {
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(classRef).create();
    verifyDefault();
    assertEquals(1, ret.size());
    assertEquals(classRef.getDocRef(wikiRef), ret.get(classRef).getXClassReference());
    assertObjs(newEditor(), ret.get(classRef));
  }

  @Test
  public void test_create_multiple() throws Exception {
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(classRef).filter(classRef2).create();
    verifyDefault();
    assertEquals(2, ret.size());
    assertEquals(classRef.getDocRef(wikiRef), ret.get(classRef).getXClassReference());
    assertEquals(classRef2.getDocRef(wikiRef), ret.get(classRef2).getXClassReference());
    assertObjs(newEditor(), ret.get(classRef), ret.get(classRef2));
  }

  @Test
  public void test_create_notClone() throws Exception {
    replayDefault();
    BaseObject ret = newEditor().filter(classRef).create().get(classRef);
    verifyDefault();
    // manipulating created object also affects the doc
    ret.setStringValue(FIELD_MY_STRING.getName(), "asdf");
    assertObjs(newEditor(), ret);
  }

  @Test
  public void test_create_keyValue() throws Exception {
    ClassField<String> field1 = FIELD_MY_STRING;
    List<String> vals = Arrays.asList("val1", "val2");
    ClassField<Integer> field2 = FIELD_MY_INT;
    int val = 2;
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(field1, vals).filter(field2,
        val).filter(classRef2).create();
    verifyDefault();
    assertEquals(2, ret.size());
    assertEquals(classRef.getDocRef(wikiRef), ret.get(classRef).getXClassReference());
    assertTrue(vals.contains(ret.get(classRef).getStringValue(field1.getName())));
    assertEquals(val, ret.get(classRef).getIntValue(field2.getName()));
    assertEquals(classRef2.getDocRef(wikiRef), ret.get(classRef2).getXClassReference());
    assertObjs(newEditor(), ret.get(classRef), ret.get(classRef2));
  }

  @Test
  public void test_create_none() throws Exception {
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().create();
    verifyDefault();
    assertEquals(0, ret.size());
  }

  @Test
  public void test_create_ClassDocumentLoadException() throws Exception {
    WikiReference wikiRef = new WikiReference("other");
    doc = new XWikiDocument(new DocumentReference(wikiRef.getName(), "space", "doc"));
    Throwable cause = new XWikiException();
    expect(createBaseClassMock(classRef.getDocRef(wikiRef)).newCustomClassInstance(same(
        getContext()))).andThrow(cause).once();
    replayDefault();
    ClassDocumentLoadException exc = new ExceptionAsserter<ClassDocumentLoadException>(
        ClassDocumentLoadException.class) {

      @Override
      protected void execute() throws ClassDocumentLoadException {
        newEditor().filter(classRef).create();
      }
    }.evaluate();
    assertSame(cause, exc.getCause());
  }

  @Test
  public void test_create_docField() throws Exception {
    replayDefault();
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newEditor().filter(XWikiDocumentClass.FIELD_CONTENT.getClassReference()).create();
      }
    }.evaluate();
  }

  @Test
  public void test_createIfNotExists_create() throws Exception {
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(classRef).createIfNotExists();
    verifyDefault();
    assertEquals(1, ret.size());
    assertEquals(classRef.getDocRef(wikiRef), ret.get(classRef).getXClassReference());
    assertObjs(newEditor(), ret.get(classRef));
  }

  @Test
  public void test_createIfNotExists_create_field() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    BaseObject obj = addObj(classRef, field, "otherval");
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(field, val).createIfNotExists();
    verifyDefault();
    assertEquals(1, ret.size());
    assertNotSame(obj, ret.get(classRef));
    assertEquals(classRef.getDocRef(wikiRef), ret.get(classRef).getXClassReference());
    assertEquals(val, ret.get(classRef).getStringValue(field.getName()));
    assertObjs(newEditor(), obj, ret.get(classRef));
  }

  @Test
  public void test_createIfNotExists_exists() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(classRef).createIfNotExists();
    verifyDefault();
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(classRef));
    assertObjs(newEditor(), obj);
  }

  @Test
  public void test_createIfNotExists_exists_field() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    BaseObject obj = addObj(classRef, field, val);
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().filter(field, val).createIfNotExists();
    verifyDefault();
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(classRef));
    assertObjs(newEditor(), obj);
  }

  @Test
  public void test_createIfNotExists_none() throws Exception {
    replayDefault();
    Map<ClassIdentity, BaseObject> ret = newEditor().createIfNotExists();
    verifyDefault();
    assertEquals(0, ret.size());
  }

  @Test
  public void test_createFirst() throws Exception {
    replayDefault();
    BaseObject ret = newEditor().filter(classRef).createFirst();
    verifyDefault();
    assertEquals(classRef.getDocRef(wikiRef), ret.getXClassReference());
    assertObjs(newEditor(), ret);
  }

  @Test
  public void test_createFirst_none() throws Exception {
    replayDefault();
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        newEditor().createFirst();
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_createFirstIfNotExists_create() throws Exception {
    replayDefault();
    BaseObject ret = newEditor().filter(classRef).createFirstIfNotExists();
    verifyDefault();
    assertEquals(classRef.getDocRef(wikiRef), ret.getXClassReference());
    assertObjs(newEditor(), ret);
  }

  @Test
  public void test_createFirstIfNotExists_exists() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    replayDefault();
    BaseObject ret = newEditor().filter(classRef).createFirstIfNotExists();
    verifyDefault();
    assertSame(obj, ret);
    assertObjs(newEditor(), ret);
  }

  @Test
  public void test_createFirstIfNotExists_none() throws Exception {
    replayDefault();
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        newEditor().createFirstIfNotExists();
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_delete() {
    BaseObject obj = addObj(classRef, null, null);
    List<BaseObject> ret = newEditor().delete();
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(0));
    assertObjs(newEditor());
  }

  @Test
  public void test_delete_classRef() {
    BaseObject obj = addObj(classRef, null, null);
    List<BaseObject> ret = newEditor().filter(classRef).delete();
    assertEquals(1, ret.size());
    assertSame(obj, ret.get(0));
    assertObjs(newEditor());
  }

  @Test
  public void test_delete_none() {
    BaseObject obj = addObj(classRef2, null, null);
    List<BaseObject> ret = newEditor().filter(classRef).delete();
    assertEquals(0, ret.size());
    assertObjs(newEditor(), obj);
  }

  @Test
  public void test_delete_multiple() {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, null, null);
    BaseObject obj3 = addObj(classRef, null, null);
    List<BaseObject> ret = newEditor().filter(classRef).delete();
    assertEquals(2, ret.size());
    assertSame(obj1, ret.get(0));
    assertSame(obj3, ret.get(1));
    assertObjs(newEditor(), obj2);
  }

  @Test
  public void test_delete_keyValue() {
    ClassField<String> field = FIELD_MY_STRING;
    List<String> vals = Arrays.asList("val1", "val2");
    BaseObject obj1 = addObj(classRef, field, vals.get(0));
    BaseObject obj2 = addObj(classRef, null, null);
    BaseObject obj3 = addObj(classRef, field, vals.get(1));
    BaseObject obj4 = addObj(classRef2, field, vals.get(0));
    List<BaseObject> ret = newEditor().filter(field, vals).delete();
    assertEquals(2, ret.size());
    assertSame(obj1, ret.get(0));
    assertSame(obj3, ret.get(1));
    assertObjs(newEditor(), obj2, obj4);
  }

  @Test
  public void test_deleteFirst() {
    BaseObject obj = addObj(classRef, null, null);
    BaseObject objNotDelted = addObj(classRef, null, null);
    java.util.Optional<BaseObject> ret = newEditor().deleteFirst();
    assertTrue(ret.isPresent());
    assertSame(obj, ret.get());
    assertObjs(newEditor(), objNotDelted);
  }

  @Test
  public void test_deleteFirst_none() {
    java.util.Optional<BaseObject> ret = newEditor().deleteFirst();
    assertFalse(ret.isPresent());
  }

  @Test
  public void test_editField_noObj() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    addObj(classRef2, null, null);

    assertEquals(newEditor().editField(field).first(val), false);
    assertEquals(newEditor().editField(field).all(val), false);

  }

  @Test
  public void test_editField_first() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    addObj(classRef2, null, null);
    addObj(classRef, null, null);
    addObj(classRef, null, null);
    XWikiObjectEditor editor = newEditor();

    replayDefault();
    assertEquals(editor.editField(field).first(val), true);
    verifyDefault();
    assertEquals(editor.fetch().fetchField(field).list(), Arrays.asList(val));
  }

  @Test
  public void test_editField_all() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    addObj(classRef2, null, null);
    addObj(classRef, null, null);
    addObj(classRef, null, null);
    XWikiObjectEditor editor = newEditor();

    replayDefault();
    assertEquals(editor.editField(field).all(val), true);
    verifyDefault();
    assertEquals(editor.fetch().fetchField(field).list(), Arrays.asList(val, val));
  }

  @Test
  public void test_editField_docField_first() throws Exception {
    ClassField<String> field = XWikiDocumentClass.FIELD_CONTENT;
    String val = "val";

    replayDefault();
    assertEquals(newEditor().editField(field).first(val), true);
    verifyDefault();
    assertEquals(val, doc.getContent());
    assertEquals(0, doc.getXObjects().size());
  }

  @Test
  public void test_editField_docField_all() throws Exception {
    ClassField<String> field = XWikiDocumentClass.FIELD_CONTENT;
    String val = "val";

    replayDefault();
    assertEquals(newEditor().editField(field).all(val), true);
    verifyDefault();
    assertEquals(val, doc.getContent());
    assertEquals(0, doc.getXObjects().size());
  }

  private <T> BaseObject addObj(ClassReference classRef, ClassField<T> field, T value) {
    BaseObject obj = createObj(classRef, field, value);
    doc.addXObject(obj);
    return obj;
  }

  private <T> BaseObject createObj(ClassReference classRef, ClassField<T> field, T value) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classRef.getDocRef(wikiRef));
    if (field != null) {
      if (field.getType() == String.class) {
        obj.setStringValue(field.getName(), (String) value);
      } else if (field.getType() == Integer.class) {
        obj.setIntValue(field.getName(), firstNonNull((Integer) value, 0));
      }
    }
    return obj;
  }

  private static void assertObjs(XWikiObjectEditor editor, BaseObject... expObjs) {
    List<BaseObject> ret = editor.fetch().list();
    assertEquals("not same size, objs: " + ret, expObjs.length, ret.size());
    for (int i = 0; i < ret.size(); i++) {
      assertSame(expObjs[i], ret.get(i));
    }
  }

}
