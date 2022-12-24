package com.celements.model.classes.fields;

import static org.junit.Assert.*;
import static org.mutabilitydetector.unittesting.MutabilityAssert.*;

import org.apache.commons.codec.digest.MessageDigestAlgorithms;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.PasswordField.StorageType;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;

public class PasswordFieldTest extends AbstractComponentTest {

  // test static definition
  private static final ClassField<String> STATIC_DEFINITION = new PasswordField.Builder(
      TestClassDefinition.NAME, "name").build();

  private PasswordField field;

  StorageType storageType = StorageType.Clear;
  String hashAlgorithm = MessageDigestAlgorithms.MD5;

  @Before
  public void prepareTest() throws Exception {
    assertNotNull(STATIC_DEFINITION);
    field = new PasswordField.Builder(TestClassDefinition.NAME, "name").storageType(
        storageType).hashAlgorithm(hashAlgorithm).build();
  }

  // @Test mutabilitydetector broken in Java11+
  public void test_immutability() {
    assertImmutable(PasswordField.class);
  }

  @Test
  public void test_getters() throws Exception {
    assertEquals(storageType, field.getStorageType());
    assertEquals(hashAlgorithm, field.getHashAlgorithm());
  }

  @Test
  public void test_getXField() throws Exception {
    assertTrue(field.getXField() instanceof PasswordClass);
    PasswordClass xField = (PasswordClass) field.getXField();
    assertEquals(storageType.name(), xField.getStringValue(
        PasswordField.CLASS_FIELD_NAME_STORAGE_TYPE));
    assertEquals(hashAlgorithm, xField.getStringValue(PasswordMetaClass.ALGORITHM_KEY));
  }

  @Test
  public void test_defaults() throws Exception {
    field = new PasswordField.Builder(TestClassDefinition.NAME, "name").build();
    assertEquals(StorageType.Hash, field.getStorageType());
    assertEquals(MessageDigestAlgorithms.SHA_512, field.getHashAlgorithm());
  }

  @Test
  public void test_illegal_hashAlgorithm() throws Exception {
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        new PasswordField.Builder(TestClassDefinition.NAME, "name").hashAlgorithm("asdf").build();
      }
    }.evaluate();
  }

}
