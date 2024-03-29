package com.celements.model.access.object;

import static com.celements.model.classes.TestClassDefinition.*;
import static com.google.common.base.MoreObjects.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.web.Utils;

public class ObjectFetcherTest extends AbstractComponentTest {

  private WikiReference wikiRef;
  private XWikiDocument doc;
  private ClassReference classRef;
  private ClassReference classRef2;

  @Before
  public void prepareTest() throws Exception {
    wikiRef = new WikiReference("db");
    doc = new XWikiDocument(new DocumentReference(wikiRef.getName(), "space", "doc"));
    classRef = Utils.getComponent(ClassDefinition.class, NAME).getClassReference();
    classRef2 = new ClassReference("class", "other");
  }

  private XWikiObjectFetcher newFetcher() {
    return XWikiObjectFetcher.on(doc);
  }

  @Test
  public void test_nullDoc() throws Exception {
    new ExceptionAsserter<NullPointerException>(NullPointerException.class) {

      @Override
      protected void execute() throws Exception {
        XWikiObjectFetcher.on(null);
      }
    }.evaluate();
  }

  @Test
  public void test_isTranslation() throws Exception {
    doc.setLanguage("en");
    doc.setTranslation(1);
    IllegalArgumentException iae = new ExceptionAsserter<IllegalArgumentException>(
        IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newFetcher().stream();
      }
    }.evaluate();
    assertTrue("wrong message: " + iae.getMessage(), iae.getMessage().contains("[en]"));
    assertTrue("wrong message: " + iae.getMessage(), iae.getMessage().contains("["
        + doc.getDocumentReference() + "]"));
  }

  @Test
  public void test_fetch_emptyDoc() throws Exception {
    assertObjs(newFetcher());
  }

  @Test
  public void test_fetch_clone() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    BaseObject ret = newFetcher().first().get();
    assertEqualObjs(obj, ret);
  }

  @Test
  public void test_fetch_oneObj() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    BaseObject obj = addObj(classRef, field, val);

    assertObjs(newFetcher(), obj);
    assertObjs(newFetcher().filter(classRef), obj);
    assertObjs(newFetcher().filter(field, val), obj);
    assertObjs(newFetcher().filter(field, Arrays.asList("asdf", val)), obj);
    assertObjs(newFetcher().filterAbsent(field));
  }

  @Test
  public void test_fetch_class() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, null, null);

    assertObjs(newFetcher().filter(classRef), obj1);
    assertObjs(newFetcher().filter(classRef2), obj2);
    assertObjs(newFetcher().filter(classRef).filter(classRef2), obj1, obj2);
  }

  @Test
  public void test_fetch_values_empty() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    addObj(classRef, null, null);
    addObj(classRef, field, null);
    addObj(classRef, field, "val");
    assertObjs(newFetcher().filter(field, Collections.<String>emptyList()));
  }

  @Test
  public void test_fetch_absent() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    BaseObject obj = addObj(classRef, field, null);
    assertObjs(newFetcher().filterAbsent(field), obj);
    assertObjs(newFetcher().filter(classRef).filterAbsent(field), obj);
    assertObjs(newFetcher().filter(field, "val").filterAbsent(field));
  }

  @Test
  public void test_fetch_present() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    BaseObject obj1 = addObj(classRef, field, "val");
    BaseObject obj2 = addObj(classRef, field, null);
    BaseObject obj3 = addObj(classRef, field, "123");
    assertObjs(newFetcher().filterPresent(field), obj1, obj3);
    assertObjs(newFetcher().filter(classRef).filterPresent(field), obj1, obj3);
    assertObjs(newFetcher().filter(field, "val").filterPresent(field), obj1);
    assertObjs(newFetcher().filter(field, "abc").filterPresent(field));
  }

  @Test
  public void test_fetch_field_and() throws Exception {
    ClassField<String> field1 = FIELD_MY_STRING;
    String val1 = "val";
    ClassField<Integer> field2 = FIELD_MY_INT;
    Integer val2 = 5;

    BaseObject obj1 = addObj(classRef, field1, val1);
    BaseObject obj2 = addObj(classRef, field2, val2);
    BaseObject obj3 = addObj(classRef, field1, val1);
    obj3.setIntValue(field2.getName(), val2);

    assertObjs(newFetcher(), obj1, obj2, obj3);
    assertObjs(newFetcher().filter(field1, val1), obj1, obj3);
    assertObjs(newFetcher().filter(field2, val2), obj2, obj3);
    assertObjs(newFetcher().filter(field1, val1).filter(field2, val2), obj3);
    assertObjs(newFetcher().filterAbsent(field1), obj2);
    assertObjs(newFetcher().filterAbsent(field1).filter(field2, val2), obj2);
    assertObjs(newFetcher().filterAbsent(field2), obj1);
    assertObjs(newFetcher().filterAbsent(field2).filter(field1, val1), obj1);
  }

  @Test
  public void test_fetch_field_or() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val1 = "val1";
    String val2 = "val2";

    addObj(classRef, null, null);
    addObj(classRef, field, "asdf");
    BaseObject obj1 = addObj(classRef, field, val1);
    BaseObject obj2 = addObj(classRef, field, val2);

    assertObjs(newFetcher().filter(field, val1), obj1);
    assertObjs(newFetcher().filter(field, val2), obj2);
    assertObjs(newFetcher().filter(field, Arrays.asList(val1, val2)), obj1, obj2);
  }

  @Test
  public void test_fetch_combined() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val1 = "val1";
    String val2 = "val2";
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, field, val1);
    BaseObject obj3 = addObj(classRef, field, val1);
    BaseObject obj4 = addObj(classRef, FIELD_MY_INT, null);
    BaseObject obj5 = addObj(classRef, field, null);
    BaseObject obj6 = addObj(classRef, field, val2);
    assertObjs(newFetcher(), obj1, obj3, obj4, obj5, obj6, obj2);
    assertObjs(newFetcher().filter(classRef), obj1, obj3, obj4, obj5, obj6);
    assertObjs(newFetcher().filterAbsent(field), obj1, obj4, obj5);
    assertObjs(newFetcher().filter(field, val1), obj3);
    assertObjs(newFetcher().filter(field, val2), obj6);
    assertObjs(newFetcher().filter(field, Arrays.asList(val1, val2)), obj3, obj6);
    assertObjs(newFetcher().filter(field, val1).filter(field, val2));
    assertObjs(newFetcher().filter(field, "other"));
    assertObjs(newFetcher().filter(classRef2), obj2);
    assertObjs(newFetcher().filter(field, val1).filter(classRef2), obj3, obj2);
    assertObjs(newFetcher().filter(classRef).filter(classRef2), obj1, obj3, obj4, obj5, obj6, obj2);
  }

  @Test
  public void test_fetch_list_multiSelect() throws Exception {
    ClassField<List<String>> field = FIELD_MY_LIST_MS;
    String val1 = "val1";
    BaseObject obj1 = addObj(classRef, field, Arrays.asList(val1));
    String val2 = "val2";
    BaseObject obj2 = addObj(classRef, field, Arrays.asList(val2));
    BaseObject obj3 = addObj(classRef, field, Arrays.asList(val1, val2));

    assertObjs(newFetcher().filter(field, Arrays.asList(val1)), obj1);
    assertObjs(newFetcher().filter(field, Arrays.asList(val2)), obj2);
    assertObjs(newFetcher().filter(field, Arrays.asList(val1, val2)), obj3);
    assertObjs(newFetcher().filter(field, Arrays.asList(Arrays.asList(val1), Arrays.asList(val2))),
        obj1, obj2);
    assertObjs(newFetcher().filterAbsent(field));
  }

  @Test
  public void test_fetch_list_singleSelect() throws Exception {
    ClassField<List<String>> field = FIELD_MY_LIST_SS;
    String val1 = "val1";
    BaseObject obj1 = addObj(classRef, field, Arrays.asList(val1));
    String val2 = "val2";
    BaseObject obj2 = addObj(classRef, field, Arrays.asList(val2));

    assertObjs(newFetcher().filter(field, Arrays.asList(val1)), obj1);
    assertObjs(newFetcher().filter(field, Arrays.asList(val2)), obj2);
    assertObjs(newFetcher().filter(field, Arrays.asList(val1, val2)), obj1, obj2);
    assertObjs(newFetcher().filter(field, Arrays.asList(Arrays.asList(val1), Arrays.asList(val2))),
        obj1, obj2);
    assertObjs(newFetcher().filterAbsent(field));
  }

  @Test
  public void test_fetch_single_list() throws Exception {
    ClassField<String> field = FIELD_MY_SINGLE_LIST;
    String val1 = "val1";
    BaseObject obj1 = addObj(classRef, field, val1);
    String val2 = "val2";
    BaseObject obj2 = addObj(classRef, field, val2);

    assertObjs(newFetcher().filter(field, Arrays.asList(val1)), obj1);
    assertObjs(newFetcher().filter(field, Arrays.asList(val2)), obj2);
    assertObjs(newFetcher().filter(field, Arrays.asList(val1, val2)), obj1, obj2);
    assertObjs(newFetcher().filterAbsent(field));
  }

  @Test
  public void test_fetch_first() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    addObj(classRef2, null, null);
    addObj(classRef, null, null);
    Optional<BaseObject> ret = newFetcher().filter(classRef).first();
    assertTrue(ret.isPresent());
    assertEqualObjs(obj1, ret.get());
  }

  @Test
  public void test_fetch_first_none() throws Exception {
    Optional<BaseObject> ret = newFetcher().filter(classRef).first();
    assertFalse(ret.isPresent());
  }

  @Test
  public void test_fetch_firstAssert() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    addObj(classRef2, null, null);
    addObj(classRef, null, null);
    BaseObject ret = newFetcher().filter(classRef).firstAssert();
    assertEqualObjs(obj1, ret);
  }

  @Test
  public void test_fetch_firstAssert_none() throws Exception {
    addObj(classRef2, null, null);
    assertTrue(new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newFetcher().filter(classRef).firstAssert();
      }
    }.evaluate().getMessage().startsWith("empty"));
  }

  @Test
  public void test_fetch_unique() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    addObj(classRef2, null, null);
    BaseObject ret = newFetcher().filter(classRef).unique();
    assertEqualObjs(obj1, ret);
  }

  @Test
  public void test_fetch_unique_none() throws Exception {
    assertTrue(new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newFetcher().filter(classRef).unique();
      }
    }.evaluate().getMessage().startsWith("empty"));
  }

  @Test
  public void test_fetch_unique_multiple() throws Exception {
    addObj(classRef, null, null);
    addObj(classRef, null, null);
    assertTrue(new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        newFetcher().filter(classRef).unique();
      }
    }.evaluate().getMessage().startsWith("non unique"));
  }

  @Test
  public void test_fetch_number() throws Exception {
    assertObjs(newFetcher().filter(-1));
    assertObjs(newFetcher().filter(0));
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef, null, null);
    assertObjs(newFetcher().filter(-1));
    assertObjs(newFetcher().filter(0), obj1);
    assertObjs(newFetcher().filter(1), obj2);
  }

  @Test
  public void test_fetch_number_multiple() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, null, null);
    addObj(classRef, null, null);
    addObj(classRef2, null, null);
    assertObjs(newFetcher().filter(obj1.getNumber()), obj1, obj2);
    assertObjs(newFetcher().filter(obj1.getNumber()).filter(classRef), obj1);
    assertObjs(newFetcher().filter(obj1.getNumber()).filter(classRef2), obj2);
  }

  @Test
  public void test_withTranslation() throws Exception {
    XWikiObjectFetcher fetcher = newFetcher();
    assertFalse(fetcher.getTranslationDoc().isPresent());
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    fetcher.withTranslation(transDoc);
    assertTrue(fetcher.getTranslationDoc().isPresent());
    assertSame(transDoc, fetcher.getTranslationDoc().get());
  }

  @Test
  public void test_withTranslation_illegalTransDocRef() throws Exception {
    XWikiDocument transDoc = new XWikiDocument(classRef2.getDocRef());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        newFetcher().withTranslation(transDoc);
      }
    }.evaluate();
  }

  @Test
  public void test_fetch_withTranslation_obj() throws Exception {
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");
    BaseObject obj1 = addObj(classRef, FIELD_LANG, "en");
    BaseObject obj2 = addObj(classRef, FIELD_LANG, "fr");
    BaseObject obj3 = addObj(classRef, null, null);
    assertObjs(newFetcher().filter(classRef), obj1, obj2, obj3);
    assertObjs(newFetcher().filter(classRef).withTranslation(transDoc), obj2);
  }

  @Test
  public void test_fetchField_withTranslation_obj() throws Exception {
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");
    addObj(classRef, FIELD_MY_STRING, "nah");
    addObj(classRef, FIELD_LANG, "fr").setStringValue(FIELD_MY_STRING.getName(), "yep");
    addObj(classRef, FIELD_LANG, "en").setStringValue(FIELD_MY_STRING.getName(), "nope");
    assertEquals(newFetcher().withTranslation(transDoc).fetchField(FIELD_MY_STRING).list(),
        ImmutableList.of("yep"));
  }

  @Test
  public void test_fetchField_withTranslation_doc() throws Exception {
    XWikiDocument transDoc = new XWikiDocument(doc.getDocumentReference());
    transDoc.setTranslation(1);
    transDoc.setLanguage("fr");
    transDoc.setTitle("val");
    assertEquals(newFetcher().withTranslation(transDoc).fetchField(XWikiDocumentClass.FIELD_TITLE)
        .list(), ImmutableList.of("val"));
  }

  @Test
  public void test_fetchList() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, null, null);
    BaseObject obj3 = addObj(classRef, null, null);
    List<BaseObject> ret = newFetcher().list();
    assertEquals(3, ret.size());
    assertEqualObjs(obj1, ret.get(0));
    assertEqualObjs(obj3, ret.get(1));
    assertEqualObjs(obj2, ret.get(2));
  }

  @Test
  public void test_fetchList_immutability() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    new ExceptionAsserter<UnsupportedOperationException>(UnsupportedOperationException.class) {

      @Override
      protected void execute() throws Exception {
        newFetcher().list().remove(0);
      }
    }.evaluate();
    assertObjs(newFetcher(), obj);
  }

  @Test
  public void test_fetchMap() throws Exception {
    BaseObject obj1 = addObj(classRef, null, null);
    BaseObject obj2 = addObj(classRef2, null, null);
    BaseObject obj3 = addObj(classRef, null, null);
    Map<ClassIdentity, List<BaseObject>> ret = newFetcher().map();
    assertEquals(2, ret.size());
    assertEquals(2, ret.get(classRef).size());
    assertEqualObjs(obj1, ret.get(classRef).get(0));
    assertEqualObjs(obj3, ret.get(classRef).get(1));
    assertEquals(1, ret.get(classRef2).size());
    assertEqualObjs(obj2, ret.get(classRef2).get(0));
  }

  @Test
  public void test_fetchMap_immutability() throws Exception {
    BaseObject obj = addObj(classRef, null, null);
    new ExceptionAsserter<UnsupportedOperationException>(UnsupportedOperationException.class) {

      @Override
      protected void execute() throws Exception {
        newFetcher().map().remove(classRef);
      }
    }.evaluate();
    assertObjs(newFetcher(), obj);
    new ExceptionAsserter<UnsupportedOperationException>(UnsupportedOperationException.class) {

      @Override
      protected void execute() throws Exception {
        newFetcher().map().get(classRef).remove(0);
      }
    }.evaluate();
    assertObjs(newFetcher(), obj);
  }

  @Test
  public void test_fetchField_noObj() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;

    assertEquals(newFetcher().fetchField(field).first().isPresent(), false);
    assertEquals(newFetcher().fetchField(field).list(), Collections.emptyList());
    assertEquals(newFetcher().fetchField(field).set(), Collections.emptySet());
  }

  @Test
  public void test_fetchField_oneObj() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val = "val";
    addObj(classRef, field, val);

    assertEquals(newFetcher().fetchField(field).first().get(), val);
    assertEquals(newFetcher().fetchField(field).list(), Arrays.asList(val));
    assertEquals(newFetcher().fetchField(field).set(), ImmutableSet.of(val));
  }

  @Test
  public void test_fetchField_manyObj() throws Exception {
    ClassField<String> field = FIELD_MY_STRING;
    String val1 = "val1";
    String val2 = "val2";
    addObj(classRef2, field, val1);
    addObj(classRef, null, null);
    addObj(classRef, field, val1);
    addObj(classRef, null, null);
    addObj(classRef, field, val2);
    addObj(classRef2, field, val2);
    addObj(classRef, field, val1);

    assertEquals(newFetcher().fetchField(field).first().get(), val1);
    assertEquals(newFetcher().fetchField(field).list(), Arrays.asList(val1, val2, val1));
    assertEquals(newFetcher().fetchField(field).set(), ImmutableSet.of(val1, val2));
    assertEquals(newFetcher().fetchField(field).iterNullable().copyInto(new ArrayList<>()),
        Arrays.asList(null, val1, null, val2, val1));
  }

  @Test
  public void test_fetchField_docField() throws Exception {
    ClassField<String> field = XWikiDocumentClass.FIELD_CONTENT;
    String val = "val";
    doc.setContent(val);

    assertTrue(newFetcher().fetchField(field).first().isPresent());
    assertEquals(newFetcher().fetchField(field).first().get(), val);
    assertEquals(newFetcher().fetchField(field).list(), Arrays.asList(val));
    assertEquals(newFetcher().fetchField(field).set(), ImmutableSet.of(val));
  }

  @Test
  public void test_EmptyFetcher() throws Exception {
    assertFalse(XWikiObjectFetcher.empty().first().isPresent());
    assertTrue(XWikiObjectFetcher.empty().iter().isEmpty());
    assertTrue(XWikiObjectFetcher.empty().list().isEmpty());
    assertTrue(XWikiObjectFetcher.empty().map().isEmpty());
    assertTrue(XWikiObjectFetcher.empty().count() == 0);
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
      } else if (field.getType() == List.class) {
        obj.setStringListValue(field.getName(), (List<?>) value);
      }
    }
    return obj;
  }

  private static void assertObjs(XWikiObjectFetcher fetcher, BaseObject... expObjs) {
    List<BaseObject> ret = fetcher.list();
    assertEquals("not same size, objs: " + ret, expObjs.length, ret.size());
    for (int i = 0; i < ret.size(); i++) {
      assertEqualObjs(expObjs[i], ret.get(i));
    }
  }

  private static void assertEqualObjs(BaseObject expObj, BaseObject actObj) {
    assertNotSame("object not cloned", expObj, actObj);
    assertEquals(expObj.getDocumentReference(), actObj.getDocumentReference());
    assertEquals(expObj.getXClassReference(), actObj.getXClassReference());
    assertEquals(expObj.getNumber(), actObj.getNumber());
    assertEquals(expObj.getId(), actObj.getId());
    assertEquals("not same amount of fields set", expObj.getPropertyList().size(),
        actObj.getPropertyList().size());
    for (String propName : expObj.getPropertyList()) {
      try {
        BaseProperty expProp = (BaseProperty) expObj.get(propName);
        BaseProperty actProp = (BaseProperty) actObj.get(propName);
        assertNotSame("object property not cloned", expProp, actProp);
        assertEquals(expProp.getName(), actProp.getName());
        assertEquals(expProp.getValue(), actProp.getValue());
      } catch (XWikiException xwe) {
        throw new RuntimeException(xwe);
      }
    }
  }

}
