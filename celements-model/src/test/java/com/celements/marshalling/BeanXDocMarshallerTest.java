package com.celements.marshalling;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.classes.TestClassDefinition.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.convert.bean.TestBean;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

public class BeanXDocMarshallerTest extends AbstractComponentTest {

  private ClassDefinition classDef;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(IModelAccessFacade.class);
    classDef = Utils.getComponent(ClassDefinition.class, NAME);
    expectClass(classDef, new WikiReference("wiki"));
  }

  private BaseClass expectClass(ClassDefinition classDef, WikiReference wikiRef)
      throws XWikiException {
    BaseClass bClass = expectNewBaseObject(classDef.getDocRef(wikiRef));
    for (ClassField<?> field : classDef.getFields()) {
      expect(bClass.get(field.getName())).andReturn(field.getXField()).anyTimes();
    }
    return bClass;
  }

  @Test
  public void test() throws Exception {
    BeanXDocMarshaller<TestBean> marshaller = new BeanXDocMarshaller.Builder<>(TestBean.class,
        classDef).build();
    XWikiDocument doc = expectDoc(new DocumentReference("wiki", "space", "doc"));
    addObj(doc, classDef);
    String fullName = getModelUtils().serializeRef(doc.getDocumentReference());

    replayDefault();
    TestBean bean = marshaller.resolve(fullName).get();
    verifyDefault();

    assertNotNull(bean);
    assertEquals(doc.getDocumentReference(), bean.getDocumentReference());
    assertEquals(fullName, marshaller.serialize(bean));
  }

  @Test
  public void test_noObj() throws Exception {
    BeanXDocMarshaller<TestBean> marshaller = new BeanXDocMarshaller.Builder<>(TestBean.class,
        classDef).build();
    XWikiDocument doc = expectDoc(new DocumentReference("wiki", "space", "doc"));
    String fullName = getModelUtils().serializeRef(doc.getDocumentReference());

    replayDefault();
    assertFalse(marshaller.resolve(fullName).isPresent());
    verifyDefault();
  }

  @Test
  public void test_localRef() throws Exception {
    ReferenceSerializationMode serializationMode = ReferenceSerializationMode.LOCAL;
    BeanXDocMarshaller<TestBean> marshaller = new BeanXDocMarshaller.Builder<>(TestBean.class,
        classDef).serializationMode(serializationMode).baseRef(new EntityReference("space",
            EntityType.SPACE)).build();
    XWikiDocument doc = expectDoc(new DocumentReference("xwikidb", "space", "doc"));
    addObj(doc, classDef);
    String fullName = doc.getDocumentReference().getName();

    replayDefault();
    TestBean bean = marshaller.resolve(fullName).get();
    verifyDefault();

    assertNotNull(bean);
    assertEquals(doc.getDocumentReference(), bean.getDocumentReference());
    assertEquals(fullName, marshaller.serialize(bean));
  }

  private XWikiDocument expectDoc(DocumentReference docRef) {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setNew(false);
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(doc.getDocumentReference()))
        .andReturn(doc).anyTimes();
    return doc;
  }

  private <T> BaseObject addObj(XWikiDocument doc, ClassIdentity classId) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classId.getDocRef(doc.getDocumentReference().getWikiReference()));
    doc.addXObject(obj);
    return obj;
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
