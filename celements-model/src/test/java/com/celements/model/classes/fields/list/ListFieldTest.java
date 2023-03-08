package com.celements.model.classes.fields.list;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.AllowedReason.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.*;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.*;

import java.util.Arrays;
import java.util.Collections;
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
import com.celements.model.util.ClassFieldValue;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

public class ListFieldTest extends AbstractComponentTest {

  // test static definition
  private static final ClassField<List<String>> STATIC_DEFINITION = new StaticListField.Builder(
      TestClassDefinition.CLASS_REF, "name").build();

  private StaticListField.Builder fieldBuilder;

  private boolean multiSelect = true;
  private Integer size = 5;
  private DisplayType displayType = DisplayType.checkbox;
  private Boolean picker = true;
  private String separator = ",";
  private List<String> values = Arrays.asList("A", "B", "C");

  @Before
  public void prepareTest() throws Exception {
    assertNotNull(STATIC_DEFINITION);
    fieldBuilder = new StaticListField.Builder(TestClassDefinition.CLASS_REF, "name");
    fieldBuilder.multiSelect(multiSelect).size(size).displayType(displayType).picker(
        picker).separator(separator).values(values);
  }

  @Test
  public void test_defaultSize_isSet() {
    ListField.getBuilderSizeDefaultSet(fieldBuilder);
    assertEquals((Integer) 5, fieldBuilder.size);
  }

  @Test
  public void test_defaultSize_singleSelect() {
    fieldBuilder.multiSelect(false);
    fieldBuilder.size(null);
    ListField.getBuilderSizeDefaultSet(fieldBuilder);
    assertNull(fieldBuilder.size);
    assertEquals((Integer) 1, fieldBuilder.build().getSize());
  }

  @Test
  public void test_defaultSize_multiselect() {
    fieldBuilder.multiSelect(true);
    fieldBuilder.size(null);
    ListField.getBuilderSizeDefaultSet(fieldBuilder);
    assertEquals((Integer) 2, fieldBuilder.size);
  }

  // @Test mutabilitydetector broken in Java11+
  public void test_immutability() {
    assertInstancesOf(AbstractListField.class, areImmutable(), allowingForSubclassing(),
        AllowedReason.provided(Marshaller.class).isAlsoImmutable());
    assertInstancesOf(ListField.class, areImmutable(), allowingForSubclassing());
    assertInstancesOf(CustomListField.class, areImmutable(), allowingForSubclassing(),
        assumingFields("values").areSafelyCopiedUnmodifiableCollectionsWithImmutableElements());
    assertInstancesOf(StaticListField.class, areImmutable(), allowingForSubclassing());
    assertImmutable(StaticListField.class);
    assertImmutable(DBListField.class);
  }

  @Test
  public void test_getters() throws Exception {
    StaticListField field = fieldBuilder.build();
    assertEquals(multiSelect, field.getMultiSelect());
    assertEquals(size, field.getSize());
    assertEquals(displayType.name(), field.getDisplayType());
    assertEquals(displayType, field.getDisplayTypeEnum());
    assertEquals(picker, field.getPicker());
    assertEquals(separator, field.getSeparator());
    assertEquals(values, field.getValues());
  }

  @Test
  public void test_defaults() throws Exception {
    StaticListField field = new StaticListField.Builder(TestClassDefinition.CLASS_REF, "name")
        .build();
    assertNull(field.getMultiSelect());
    assertEquals(new Integer(1), field.getSize());
    assertNull(field.getDisplayType());
    assertNull(field.getDisplayTypeEnum());
    assertNull(field.getPicker());
    assertEquals(AbstractListField.DEFAULT_SEPARATOR, field.getSeparator());
    assertEquals(Collections.emptyList(), field.getValues());
  }

  @Test
  public void test_getXField() throws Exception {
    StaticListField field = fieldBuilder.build();
    assertTrue(field.getXField() instanceof ListClass);
    StaticListClass xField = (StaticListClass) field.getXField();
    assertEquals(multiSelect, xField.isMultiSelect());
    assertEquals(size, (Integer) xField.getSize());
    assertEquals(displayType.name(), xField.getDisplayType());
    assertEquals(picker, xField.isPicker());
    assertEquals(separator, xField.getSeparators());
    assertEquals(" ", xField.getSeparator()); // this is the view separator
    assertEquals("separator has to be | for XField values", Joiner.on(
        AbstractListField.DEFAULT_SEPARATOR).join(values), xField.getValues());
    assertEquals(values, xField.getList(getContext()));
  }

  @Test
  public void test_getXField_defaults() throws Exception {
    StaticListField field = new StaticListField.Builder(TestClassDefinition.CLASS_REF, "name")
        .build();
    StaticListClass xField = (StaticListClass) field.getXField();
    assertEquals(false, xField.isMultiSelect());
    assertEquals(1, xField.getSize());
    assertEquals(DisplayType.select.name(), xField.getDisplayType());
    assertEquals(false, xField.isPicker());
    assertEquals("|", xField.getSeparators());
    assertEquals(" ", xField.getSeparator()); // this is the view separator
    assertEquals("", xField.getValues());
    assertEquals(Collections.emptyList(), xField.getList(getContext()));
  }

  @Test
  public void test_resolve_serialize_multiselect() throws Exception {
    StaticListField field = fieldBuilder.values(Arrays.asList("A", "B", "C", "D")).build();
    DocumentReference classRef = field.getClassDef().getClassRef();
    IModelAccessFacade modelAccess = Utils.getComponent(IModelAccessFacade.class);
    XWikiDocument doc = new XWikiDocument(classRef);
    List<String> value = Arrays.asList("B", "D");

    BaseClass bClass = expectNewBaseObject(classRef);
    expectPropertyClass(bClass, field.getName(), (PropertyClass) field.getXField());

    replayDefault();
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, value));
    List<String> ret = modelAccess.getFieldValue(doc, field).or(ImmutableList.of());
    verifyDefault();

    assertEquals(value, ret);
    assertEquals(value, modelAccess.getXObject(doc, classRef).getListValue(field.getName()));
  }

  @Test
  public void test_resolve_serialize_singleSelect() throws Exception {
    StaticListField field = fieldBuilder.multiSelect(false).values(Arrays.asList("A", "B", "C",
        "D")).build();
    DocumentReference classRef = field.getClassDef().getClassRef();
    IModelAccessFacade modelAccess = Utils.getComponent(IModelAccessFacade.class);
    XWikiDocument doc = new XWikiDocument(classRef);
    List<String> value = Arrays.asList("B");

    BaseClass bClass = expectNewBaseObject(classRef);
    expectPropertyClass(bClass, field.getName(), (PropertyClass) field.getXField());

    replayDefault();
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, value));
    List<String> ret = modelAccess.getFieldValue(doc, field).or(ImmutableList.of());
    verifyDefault();

    assertEquals(value, ret);
    assertEquals(value.get(0), modelAccess.getXObject(doc, classRef).getStringValue(
        field.getName()));
  }

  @Test
  public void test_resolve_serialize_null() throws Exception {
    StaticListField field = fieldBuilder.values(Arrays.asList("A", "B", "C", "D")).build();
    DocumentReference classRef = field.getClassDef().getClassRef();
    IModelAccessFacade modelAccess = Utils.getComponent(IModelAccessFacade.class);
    XWikiDocument doc = new XWikiDocument(classRef);

    BaseClass bClass = expectNewBaseObject(classRef);
    expectPropertyClass(bClass, field.getName(), (PropertyClass) field.getXField());

    replayDefault();
    List<String> ret1 = modelAccess.getFieldValue(doc, field).or(ImmutableList.of());
    modelAccess.setProperty(doc, new ClassFieldValue<>(field, null));
    List<String> ret2 = modelAccess.getFieldValue(doc, field).or(ImmutableList.of());
    verifyDefault();

    assertNotNull(ret1);
    assertTrue(ret1.isEmpty());
    assertNotNull(ret2);
    assertTrue(ret2.isEmpty());
    assertTrue(modelAccess.getXObject(doc, classRef).getListValue(field.getName()).isEmpty());
  }

  @Test
  public void test_resolve_serialize_multiSeparator() throws Exception {
    String separator = "-|,";
    List<String> values = Arrays.asList("A", "B", "C", "D");
    StaticListField field = fieldBuilder.separator(separator).build();
    assertEquals("A-B-C-D", field.serialize(values).get());
    assertEquals(values, field.resolve("A,B-C|D").get());
  }

}
