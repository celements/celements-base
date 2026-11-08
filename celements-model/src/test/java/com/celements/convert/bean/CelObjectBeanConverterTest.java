package com.celements.convert.bean;

import static com.celements.model.classes.TestClassDefinition.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.object.ObjectBean;
import com.xpn.xwiki.doc.CelObject;
import com.xpn.xwiki.objects.BaseObject;

public class CelObjectBeanConverterTest extends AbstractComponentTest {

  private ClassDefinition classDef;

  @Before
  public void prepareTest() throws Exception {
    classDef = getBeanFactory().getBean(NAME, ClassDefinition.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_celObjectToBean() {
    DocumentReference docRef = new DocumentReference("wiki", "space", "doc");
    BaseObject object = new BaseObject();
    object.setDocumentReference(docRef);
    object.setXClassReference(CLASS_REF);
    object.setNumber(3);
    object.setStringValue(FIELD_MY_STRING.getName(), "value");

    CelObjectBeanConverter<TestBean> converter = getBeanFactory().getBean(
        CelObjectBeanConverter.class);
    converter.initialize(classDef);
    converter.initialize(TestBean.class);
    TestBean bean = converter.apply(CelObject.from(object));

    assertEquals(docRef, bean.getDocumentReference());
    assertEquals(CLASS_REF, bean.getClassReference());
    assertEquals(Integer.valueOf(3), bean.getNumber());
    assertEquals("value", bean.getMyString());
  }

  public static class TestBean extends ObjectBean {

    private String myString;

    public String getMyString() {
      return myString;
    }

    public void setMyString(String myString) {
      this.myString = myString;
    }

  }

}
