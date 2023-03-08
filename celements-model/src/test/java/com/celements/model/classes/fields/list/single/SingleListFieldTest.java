package com.celements.model.classes.fields.list.single;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.AllowedReason.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.*;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mutabilitydetector.unittesting.AllowedReason;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.marshalling.Marshaller;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.list.AbstractListField;
import com.celements.model.classes.fields.list.DisplayType;
import com.celements.model.util.ClassFieldValue;
import com.google.common.base.Joiner;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

public class SingleListFieldTest extends AbstractComponentTest {

  // test static definition
  private static final ClassField<String> STATIC_DEFINITION = new StringSingleListField.Builder(
      TestClassDefinition.NAME, "name").build();

  private StringSingleListField.Builder fieldBuilder;

  private Integer size = 5;
  private DisplayType displayType = DisplayType.checkbox;
  private Boolean picker = true;
  private List<String> values = Arrays.asList("A", "B", "C");

  @Before
  public void prepareTest() throws Exception {
    assertNotNull(STATIC_DEFINITION);
    fieldBuilder = new StringSingleListField.Builder(TestClassDefinition.NAME, "name");
    fieldBuilder.size(size).displayType(displayType).picker(picker).values(values);
  }

  // @Test mutabilitydetector broken in Java11+
  public void test_immutability() {
    assertInstancesOf(AbstractListField.class, areImmutable(), allowingForSubclassing(),
        AllowedReason.provided(Marshaller.class).isAlsoImmutable());
    assertInstancesOf(SingleListField.class, areImmutable(), allowingForSubclassing());
    assertInstancesOf(CustomSingleListField.class, areImmutable(), allowingForSubclassing(),
        assumingFields("values").areSafelyCopiedUnmodifiableCollectionsWithImmutableElements());
    assertImmutable(StringSingleListField.class);
    assertImmutable(EnumSingleListField.class);
  }

  @Test
  public void test_getters() throws Exception {
    StringSingleListField field = fieldBuilder.build();
    assertEquals(size, field.getSize());
    assertEquals(displayType, field.getDisplayTypeEnum());
    assertEquals(displayType.name(), field.getDisplayType());
    assertEquals(picker, field.getPicker());
    assertEquals(values, field.getValues());
  }

  @Test
  public void test_getXField() throws Exception {
    StringSingleListField field = fieldBuilder.build();
    assertTrue(field.getXField() instanceof ListClass);
    StaticListClass xField = (StaticListClass) field.getXField();
    assertFalse(xField.isMultiSelect());
    assertEquals(size, (Integer) xField.getSize());
    assertEquals(displayType.name(), xField.getDisplayType());
    assertEquals(picker, xField.isPicker());
    assertEquals("|", xField.getSeparators());
    assertEquals(" ", xField.getSeparator()); // this is the view separator
    assertEquals("separator has to be | for XField values", Joiner.on("|").join(values),
        xField.getValues());
    assertEquals(values, xField.getList(getContext()));
  }

  @Test
  public void test_resolve_serialize() throws Exception {
    StringSingleListField field = fieldBuilder.values(Arrays.asList("A", "B", "C", "D")).build();
    DocumentReference classRef = field.getClassDef().getClassRef();
    IModelAccessFacade modelAccess = Utils.getComponent(IModelAccessFacade.class);
    XWikiDocument doc = new XWikiDocument(classRef);
    String value = "B";

    BaseClass bClass = expectNewBaseObject(classRef);
    expectPropertyClass(bClass, field.getName(), (PropertyClass) field.getXField());

    replayDefault();
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, value));
    String ret = modelAccess.getFieldValue(doc, field).orNull();
    verifyDefault();

    assertEquals(value, ret);
    assertEquals(value, modelAccess.getXObject(doc, classRef).getStringValue(field.getName()));
  }

  @Test
  public void test_resolve_serialize_null() throws Exception {
    StringSingleListField field = fieldBuilder.values(Arrays.asList("A", "B", "C", "D")).build();
    DocumentReference classRef = field.getClassDef().getClassRef();
    IModelAccessFacade modelAccess = Utils.getComponent(IModelAccessFacade.class);
    XWikiDocument doc = new XWikiDocument(classRef);

    BaseClass bClass = expectNewBaseObject(classRef);
    expectPropertyClass(bClass, field.getName(), (PropertyClass) field.getXField());

    replayDefault();
    String ret1 = modelAccess.getFieldValue(doc, field).orNull();
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, null));
    String ret2 = modelAccess.getFieldValue(doc, field).orNull();
    verifyDefault();

    assertNull(ret1);
    assertNull(ret2);
    assertTrue(modelAccess.getXObject(doc, classRef).getStringValue(field.getName()).isEmpty());
  }

}
