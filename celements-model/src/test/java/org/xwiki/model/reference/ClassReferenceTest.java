package org.xwiki.model.reference;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.classes.PseudoClassDefinition;
import com.xpn.xwiki.web.Utils;

public class ClassReferenceTest extends AbstractComponentTest {

  private ClassReference classRef;

  @Before
  public void prepareTest() {
    classRef = new ClassReference("space", "class");
  }

  @Test
  public void test_equals() {
    assertEquals(classRef, new ClassReference(classRef));
    assertEquals(classRef, new ClassReference(classRef.getParent().getName(), classRef.getName()));
  }

  @Test
  public void test_clone() {
    assertSame(classRef, classRef.clone());
  }

  @Test
  public void test_getName() {
    assertEquals("class", classRef.getName());
  }

  @Test
  public void test_getParent() {
    EntityReference parent = classRef.getParent();
    assertNotNull(parent);
    assertEquals("space", parent.getName());
    assertEquals(EntityType.SPACE, parent.getType());
    assertNull(parent.getParent());
    parent.setName("x");
  }

  @Test
  public void test_getParent_modify() {
    EntityReference clone = classRef.clone();
    clone.getParent().setName("x");
    clone.getParent().setParent(null);
    clone.getParent().setType(EntityType.WIKI);
    assertEquals(classRef, clone);
  }

  @Test
  public void test_setParent() {
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws IllegalArgumentException {
        classRef.setParent(classRef);
      }

    }.evaluate();
  }

  @Test
  public void test_getType() {
    assertEquals(EntityType.DOCUMENT, classRef.getType());
  }

  @Test
  public void test_setType() {
    for (EntityType type : EntityType.values()) {
      if (type == EntityType.DOCUMENT) {
        classRef.setType(type);
        assertSame(type, classRef.getType());
      } else {
        assertThrows(IllegalArgumentException.class, () -> classRef.setType(type));
      }
    }
  }

  @Test
  public void test_extractReference() {
    assertSame(classRef, classRef.extractReference(EntityType.DOCUMENT));
    assertEquals(classRef.getParent(), classRef.extractReference(EntityType.SPACE));
    assertSame(classRef.getParent(), classRef.extractReference(EntityType.SPACE));
    assertNull(classRef.extractReference(EntityType.WIKI));
  }

  @Test
  public void test_getDocumentReference() {
    assertEquals(new DocumentReference(getContext().getDatabase(), classRef.getParent().getName(),
        classRef.getName()), classRef.getDocRef());
    WikiReference wikiRef = new WikiReference("wiki");
    assertEquals(new DocumentReference(wikiRef.getName(), classRef.getParent().getName(),
        classRef.getName()), classRef.getDocRef(wikiRef));
  }

  @Test
  public void test_isValidObjectClass() {
    assertTrue(classRef.isValidObjectClass());
    assertFalse(new ClassReference(PseudoClassDefinition.CLASS_SPACE, "asdf").isValidObjectClass());
  }

  @Test
  public void serialize() {
    assertEquals("space.class", Utils.getComponent(EntityReferenceSerializer.class).serialize(
        classRef));
  }

}
