package com.celements.model.object;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.reflect.ReflectiveInstanceSupplier;
import com.celements.common.test.AbstractComponentTest;
import com.celements.convert.ConversionException;
import com.celements.convert.bean.BeanClassDefConverter;
import com.celements.convert.bean.XObjectBeanConverter;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.TestClassDefinition;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class ObjectBeanTest extends AbstractComponentTest {

  private ObjectBean objectBean;

  @Before
  public void setUp_ObjectBeanTest() throws Exception {
    objectBean = new ObjectBean();
  }

  @Test
  public void test_setNumber() {
    Integer num = 123;
    objectBean.setNumber(num);
    assertEquals("Number field is needed for bean", num, objectBean.getNumber());
  }

  @Test
  public void test_setClassReference() {
    ClassReference classRef = new ClassReference("space", "classname");
    objectBean.setClassReference(classRef);
    assertEquals("ClassReference field is needed for bean", classRef,
        objectBean.getClassReference());
  }

  @Test
  public void test_setDocumentReference() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "classname");
    objectBean.setDocumentReference(docRef);
    assertEquals("ClassReference field is needed for bean", docRef,
        objectBean.getDocumentReference());
  }

  @Test
  public void test_equals_equals() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    ObjectBean objectBean2 = new ObjectBean();
    objectBean2.setDocumentReference(docRef);
    objectBean2.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean2.setNumber(objNum);
    assertEquals(objectBean, objectBean2);
    assertEquals(objectBean.hashCode(), objectBean2.hashCode());
  }

  @Test
  public void test_equals_notEquals_docRef() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    ObjectBean objectBean2 = new ObjectBean();
    DocumentReference docRef2 = new DocumentReference("wikiName", "space", "document2");
    objectBean2.setDocumentReference(docRef2);
    objectBean2.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean2.setNumber(objNum);
    assertNotEquals(objectBean, objectBean2);
    assertNotEquals(objectBean.hashCode(), objectBean2.hashCode());
  }

  @Test
  public void test_equals_notEquals_classRef() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    ObjectBean objectBean2 = new ObjectBean();
    objectBean2.setDocumentReference(docRef);
    objectBean2.setClassReference(new ClassReference("spaceTest", "ClassTest2"));
    objectBean2.setNumber(objNum);
    assertNotEquals(objectBean, objectBean2);
    assertNotEquals(objectBean.hashCode(), objectBean2.hashCode());
  }

  @Test
  public void test_equals_notEquals_objNum() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    ObjectBean objectBean2 = new ObjectBean();
    objectBean2.setDocumentReference(docRef);
    objectBean2.setClassReference(TestClassDefinition.CLASS_REF);
    Integer objNum2 = 4;
    objectBean2.setNumber(objNum2);
    assertNotEquals(objectBean, objectBean2);
    assertNotEquals(objectBean.hashCode(), objectBean2.hashCode());
  }

  @Test
  public void test_toString() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    assertEquals("docRef [" + docRef + "], classReference [" + TestClassDefinition.CLASS_REF
        + "] from objNum [" + objNum + "]", objectBean.toString());
  }

  @Test
  public void test_ObjectBean_bean() {
    DocumentReference docRef = new DocumentReference("wikiName", "space", "document");
    Integer objNum = 2;
    objectBean.setDocumentReference(docRef);
    objectBean.setClassReference(TestClassDefinition.CLASS_REF);
    objectBean.setNumber(objNum);
    BaseObject objectBeanObj = new BaseObject();
    objectBeanObj.setXClassReference(TestClassDefinition.CLASS_REF);
    objectBeanObj.setDocumentReference(docRef);
    objectBeanObj.setNumber(objNum);
    try {
      ObjectBean objectBean2 = createTestClassConverter().apply(objectBeanObj);
      assertEquals(objectBean, objectBean2);
      assertEquals(objectBean.getDocumentReference(), objectBean2.getDocumentReference());
      assertEquals(objectBean.getClassReference(), objectBean2.getClassReference());
      assertEquals(objectBean.getNumber(), objectBean2.getNumber());
    } catch (ConversionException exp) {
      fail();
    }
  }

  private BeanClassDefConverter<BaseObject, ObjectBean> createTestClassConverter() {
    @SuppressWarnings("unchecked")
    BeanClassDefConverter<BaseObject, ObjectBean> converter = Utils.getComponent(
        BeanClassDefConverter.class, XObjectBeanConverter.NAME);
    converter.initialize(Utils.getComponent(ClassDefinition.class, TestClassDefinition.NAME));
    converter.initialize(new ReflectiveInstanceSupplier<>(ObjectBean.class));
    return converter;
  }

}
