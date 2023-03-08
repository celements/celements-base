package com.celements.model.field;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.store.id.IdVersion;
import com.celements.web.classes.oldcore.XWikiDocumentClass;
import com.celements.web.classes.oldcore.XWikiObjectClass;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

public class XObjectFieldAccessorTest extends AbstractComponentTest {

  private XObjectFieldAccessor accessor;
  private ClassDefinition testClassDef;

  @Before
  public void prepareTest() throws Exception {
    accessor = (XObjectFieldAccessor) Utils.getComponent(FieldAccessor.class,
        XObjectFieldAccessor.NAME);
    testClassDef = Utils.getComponent(ClassDefinition.class, TestClassDefinition.NAME);
    expectClass(testClassDef);
  }

  private static BaseClass expectClass(ClassDefinition classDef) throws XWikiException {
    BaseClass bClass = expectNewBaseObject(classDef.getDocRef());
    for (ClassField<?> field : classDef.getFields()) {
      expect(bClass.get(field.getName())).andReturn(field.getXField()).anyTimes();
    }
    return bClass;
  }

  @Test
  public void test_getValue() {
    ClassField<String> field = TestClassDefinition.FIELD_MY_STRING;
    BaseObject obj = new BaseObject();
    obj.setXClassReference(testClassDef.getClassReference());
    assertFalse(accessor.get(obj, field).isPresent());
    String value = "asdf";
    obj.setStringValue(field.getName(), value);
    assertEquals(value, accessor.get(obj, field).get());
  }

  @Test
  public void test_getValue_xObjFields() {
    ClassReference classRef = testClassDef.getClassReference();
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classRef);
    Long id = 9876543210L;
    obj.setId(id, IdVersion.CELEMENTS_3);
    DocumentReference docRef = new DocumentReference("wiki", "space", "doc");
    obj.setDocumentReference(docRef);
    Integer number = 5;
    obj.setNumber(number);
    assertEquals(docRef, accessor.get(obj, XWikiObjectClass.FIELD_DOC_REF).get());
    assertEquals(classRef, accessor.get(obj, XWikiObjectClass.FIELD_CLASS_REF).get());
    assertEquals(number, accessor.get(obj, XWikiObjectClass.FIELD_NUMBER).get());
  }

  @Test
  public void test_setValue() {
    ClassField<String> field = TestClassDefinition.FIELD_MY_STRING;
    BaseObject obj = new BaseObject();
    obj.setXClassReference(testClassDef.getClassReference());
    String value = "asdf";
    replayDefault();
    assertTrue("value should have changed", accessor.set(obj, field, value));
    assertEquals(value, accessor.get(obj, field).get());
    assertFalse("value shouldn't have unchanged", accessor.set(obj, field, value));
    assertTrue("value should have changed", accessor.set(obj, field, null));
    assertFalse(accessor.get(obj, field).isPresent());
    verifyDefault();
  }

  @Test
  public void test_FieldAccessException_invalidXClass() {
    final ClassField<String> field = TestClassDefinition.FIELD_MY_STRING;
    final BaseObject obj = new BaseObject();
    obj.setXClassReference(new ClassReference("space", "class"));
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.get(obj, field);
      }
    }.evaluate();
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.set(obj, field, null);
      }
    }.evaluate();
  }

  @Test
  public void test_FieldAccessException_pseudoClass() {
    final ClassField<String> field = XWikiDocumentClass.FIELD_CONTENT;
    final BaseObject obj = new BaseObject();
    obj.setXClassReference(new ClassReference("space", "class"));
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.get(obj, field);
      }
    }.evaluate();
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.set(obj, field, null);
      }
    }.evaluate();
  }

  @Test
  public void test_FieldAccessException_setValue_xObjFields() {
    final BaseObject obj = new BaseObject();
    final ClassReference classRef = testClassDef.getClassReference();
    obj.setXClassReference(classRef);
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.set(obj, XWikiObjectClass.FIELD_CLASS_REF, classRef);
      }
    }.evaluate();
    final DocumentReference docRef = new DocumentReference("wiki", "space", "doc");
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.set(obj, XWikiObjectClass.FIELD_DOC_REF, docRef);
      }
    }.evaluate();
    final Integer number = 5;
    new ExceptionAsserter<FieldAccessException>(FieldAccessException.class) {

      @Override
      protected void execute() throws FieldAccessException {
        accessor.set(obj, XWikiObjectClass.FIELD_NUMBER, number);
      }
    }.evaluate();
  }

}
