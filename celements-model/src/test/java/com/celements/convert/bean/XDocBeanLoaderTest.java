package com.celements.convert.bean;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.classes.TestClassDefinition.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.convert.bean.XDocBeanLoader.BeanLoadException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

public class XDocBeanLoaderTest extends AbstractComponentTest {

  private XDocBeanLoader<TestBean> loader;
  private ClassDefinition classDef;
  private XWikiDocument doc;

  @Before
  @SuppressWarnings("unchecked")
  public void prepareTest() throws Exception {
    classDef = Utils.getComponent(ClassDefinition.class, NAME);
    loader = Utils.getComponent(XDocBeanLoader.class);
    loader.initialize(TestBean.class, classDef);
    expectClass(classDef, new WikiReference("wiki"));
    doc = new XWikiDocument(new DocumentReference("wiki", "space", "doc"));
  }

  private BaseClass expectClass(ClassDefinition classDef, WikiReference wikiRef)
      throws XWikiException {
    return expectPropertyClasses(classDef.getDocRef(wikiRef), classDef.getFields().stream()
        .collect(Collectors.toMap(ClassField::getName, ClassField::getXField)));
  }

  @Test
  public void test() throws Exception {
    BaseObject obj = addObj(classDef);
    obj.setStringValue(FIELD_MY_STRING.getName(), "asdf");
    obj.setIntValue(FIELD_MY_INT.getName(), 5);
    obj.setIntValue(FIELD_MY_BOOL.getName(), 1);
    obj.setStringValue(FIELD_MY_DOCREF.getName(), "test.asdf");
    obj.setStringListValue(FIELD_MY_LIST_MS.getName(), Arrays.asList("asdf1", "asdf2"));
    obj.setStringValue(FIELD_MY_SINGLE_LIST.getName(), "asdf");

    replayDefault();
    TestBean bean = loader.load(doc);
    verifyDefault();

    assertNotNull(bean);
    assertEquals(doc.getDocumentReference(), bean.getDocumentReference());
    assertEquals("asdf", bean.getMyString());
    assertEquals(new Integer(5), bean.getMyInt());
    assertEquals(true, bean.getMyBool());
    assertEquals(new DocumentReference("xwikidb", "test", "asdf"), bean.getMyDocRef());
    assertEquals(Arrays.asList("asdf1", "asdf2"), bean.getMyListMS());
    assertEquals("asdf", bean.getMySingleList());
  }

  @Test
  public void test_noFields() throws Exception {
    addObj(classDef);

    replayDefault();
    TestBean bean = loader.load(doc);
    verifyDefault();

    assertNotNull(bean);
    assertEquals(doc.getDocumentReference(), bean.getDocumentReference());
    assertNull(bean.getMyString());
    assertNull(bean.getMyInt());
    assertNull(bean.getMyBool());
    assertNull(bean.getMyDocRef());
    assertEquals(Collections.emptyList(), bean.getMyListMS());
    assertNull(bean.getMySingleList());
  }

  @Test
  public void test_noObj() throws Exception {
    replayDefault();
    new ExceptionAsserter<>(BeanLoadException.class) {

      @Override
      protected void execute() throws BeanLoadException {
        loader.load(doc);
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void test_notInitialized() throws Exception {
    loader = Utils.getComponent(XDocBeanLoader.class);
    replayDefault();
    new ExceptionAsserter<>(IllegalStateException.class) {

      @Override
      protected void execute() throws IllegalStateException, BeanLoadException {
        loader.load(doc);
      }
    }.evaluate();
    verifyDefault();
  }

  private <T> BaseObject addObj(ClassIdentity classId) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classId.getDocRef(doc.getDocumentReference().getWikiReference()));
    doc.addXObject(obj);
    return obj;
  }

}
