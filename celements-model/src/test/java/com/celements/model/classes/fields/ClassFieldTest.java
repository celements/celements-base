package com.celements.model.classes.fields;

import static com.celements.model.classes.TestClassDefinition.*;
import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.AllowedReason.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.*;
import static org.mutabilitydetector.unittesting.MutabilityMatchers.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.web.Utils;

public class ClassFieldTest extends AbstractComponentTest {

  private TestClassField field;

  DocumentReference classRef;
  String name = "name";
  String prettyName = "prettyName";
  String validationRegExp = "validationRegExp";
  String validationMessage = "validationMessage";

  @Before
  public void prepareTest() throws Exception {
    classRef = new DocumentReference("wiki", "class", "any");
    field = new TestClassField.Builder(NAME, name).size(5).prettyName(prettyName).validationRegExp(
        validationRegExp).validationMessage(validationMessage).build();
  }

  // @Test mutabilitydetector broken in Java11+
  public void test_immutability() {
    assertInstancesOf(AbstractClassField.class, areImmutable(), allowingForSubclassing(),
        // ClassReference is effecitvely immutable
        assumingFields("classRef").areNotModifiedAndDoNotEscape());
    assertImmutable(TestClassField.class);
  }

  @Test
  public void test_constr_null_classDefName() throws Exception {
    try {
      new TestClassField.Builder(null, field.getName()).build();
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_constr_null_name() throws Exception {
    try {
      new TestClassField.Builder(NAME, null).build();
      fail("expecting NullPointerException");
    } catch (NullPointerException npe) {
      // expected
    }
  }

  @Test
  public void test_getters() throws Exception {
    assertSame(Utils.getComponent(ClassDefinition.class, NAME), field.getClassDef());
    assertEquals(name, field.getName());
    assertEquals(TestClassField.class, field.getType());
    assertEquals(prettyName, field.getPrettyName());
    assertEquals(validationRegExp, field.getValidationRegExp());
    assertEquals(validationMessage, field.getValidationMessage());
  }

  @Test
  public void test_getXField() throws Exception {
    assertTrue(field.getXField() instanceof StringClass);
    StringClass xField = (StringClass) field.getXField();
    assertEquals(field.getName(), xField.getName());
    assertEquals(prettyName, xField.getPrettyName());
    assertEquals(validationRegExp, xField.getValidationRegExp());
    assertEquals(validationMessage, xField.getValidationMessage());
  }

  @Test
  public void test_equals() throws Exception {
    assertTrue(field.equals(getBuilder().build()));
    assertTrue(field.equals(getBuilder().build()));
    assertTrue(field.equals(getBuilder().prettyName("asdf").validationRegExp(
        "asdf").validationMessage("asdf").build()));
    assertFalse(field.equals(new TestClassField.Builder(NAME, "other").build()));
    assertFalse(field.equals(new TestClassField.Builder("Class.Other", field.getName()).build()));
    assertFalse(field.equals(null));
  }

  @Test
  public void test_hashCode() throws Exception {
    assertTrue(field.hashCode() == getBuilder().build().hashCode());
    assertTrue(field.hashCode() == getBuilder().build().hashCode());
    assertTrue(field.hashCode() == getBuilder().prettyName("asdf").validationRegExp(
        "asdf").validationMessage("asdf").build().hashCode());
    assertFalse(field.hashCode() == new TestClassField.Builder(NAME, "other").build().hashCode());
    assertFalse(field.hashCode() == new TestClassField.Builder("Class.Other",
        field.getName()).build().hashCode());
  }

  @Test
  public void test_defaults() throws Exception {
    assertEquals("This Is Camel Case", new TestClassField.Builder(NAME,
        "thisIsCamelCase").build().getPrettyName());
    assertEquals("This Is Camel Case (yyyy.MM.dd)", new DateField.Builder(NAME,
        "thisIsCamelCase").dateFormat("yyyy.MM.dd").build().getPrettyName());
    assertEquals("validation_Classes.TestClass_name", getBuilder().validationRegExp(
        "r").build().getValidationMessage());
  }

  private TestClassField.Builder getBuilder() {
    return new TestClassField.Builder(NAME, field.getName());
  }

  @Test
  public void test_toString() throws Exception {
    assertEquals(field.getClassDef() + "." + name, field.toString());
  }

}
