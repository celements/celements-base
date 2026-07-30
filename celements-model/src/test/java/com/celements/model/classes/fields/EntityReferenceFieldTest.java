package com.celements.model.classes.fields;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.EntityReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.classes.TestClassDefinition;
import com.celements.model.classes.fields.ref.EntityReferenceField;
import com.celements.model.classes.fields.ref.ReferenceField;

public class EntityReferenceFieldTest extends AbstractComponentTest {

  // test static definition
  private static final ClassField<EntityReference> STATIC_DEFINITION = new EntityReferenceField.Builder(
      TestClassDefinition.CLASS_REF, "name").build();

  private ReferenceField<EntityReference> field;

  @Before
  public void prepareTest() throws Exception {
    assertNotNull(STATIC_DEFINITION);
    field = new EntityReferenceField.Builder(TestClassDefinition.CLASS_REF, "name").build();
  }

  @Test
  public void test_immutability() {
    // assertImmutable(EntityReferenceField.class); FIXME An unhandled error occurred.
  }

  @Test
  public void test_resolve() throws Exception {
    assertEquals(field.getClassReference().getDocRef(), field.resolve(getClassDefFN()).get());
  }

  private String getClassDefFN() {
    return getContext().getDatabase() + ":" + field.getClassDef().toString();
  }

}
