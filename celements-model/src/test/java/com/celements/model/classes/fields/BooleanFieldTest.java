package com.celements.model.classes.fields;

import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.classes.TestClassDefinition;
import com.xpn.xwiki.objects.classes.BooleanClass;

public class BooleanFieldTest extends AbstractComponentTest {

  // test static definition
  private static final ClassField<Boolean> STATIC_DEFINITION = new BooleanField.Builder(
      TestClassDefinition.CLASS_REF, "name").build();

  private BooleanField field;

  String displayFormType = "select";
  String displayType = "displayType";
  Integer defaultValue = 5;

  @Before
  public void prepareTest() throws Exception {
    assertNotNull(STATIC_DEFINITION);
    field = new BooleanField.Builder(TestClassDefinition.CLASS_REF, "name").displayType(
        displayType).defaultValue(defaultValue).displayFormType(displayFormType).build();
  }

  // @Test mutabilitydetector broken in Java11+
  public void test_immutability() {
    assertImmutable(BooleanField.class);
  }

  @Test
  public void test_getters() throws Exception {
    assertEquals(displayType, field.getDisplayType());
    assertEquals(defaultValue, field.getDefaultValue());
    assertEquals(displayFormType, field.getDisplayFormType());
  }

  @Test
  public void test_getXField() throws Exception {
    assertTrue(field.getXField() instanceof BooleanClass);
    BooleanClass xField = (BooleanClass) field.getXField();
    assertEquals(field.getName(), xField.getName());
    assertEquals(displayType, xField.getDisplayType());
    assertEquals(defaultValue, (Integer) xField.getDefaultValue());
    assertEquals(displayFormType, xField.getDisplayFormType());
  }

  @Test
  public void test_serialize() throws Exception {
    assertFalse(field.serialize(null).isPresent());
    assertEquals(Integer.valueOf(1), field.serialize(true).get());
    assertEquals(Integer.valueOf(0), field.serialize(false).get());
  }

  @Test
  public void test_resolve() throws Exception {
    assertFalse(field.resolve(null).isPresent());
    assertEquals(true, field.resolve(1).get());
    assertEquals(true, field.resolve(5).get());
    assertEquals(false, field.resolve(0).get());
  }
}
