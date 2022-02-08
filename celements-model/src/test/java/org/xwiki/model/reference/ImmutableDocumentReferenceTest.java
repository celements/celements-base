package org.xwiki.model.reference;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.web.Utils;

public class ImmutableDocumentReferenceTest extends AbstractComponentTest {

  private DocumentReference docRef;

  @Before
  public void prepareTest() {
    docRef = new ImmutableDocumentReference("wiki", "space", "doc");
  }

  @Test
  public void test_equals() {
    assertEquals(docRef, new DocumentReference(docRef));
    assertEquals(docRef, new ImmutableDocumentReference(docRef));
    assertEquals(docRef, new ImmutableDocumentReference(docRef.getWikiReference().getName(),
        docRef.getLastSpaceReference().getName(), docRef.getName()));
    assertEquals(docRef, new ImmutableDocumentReference(docRef.getName(),
        docRef.getLastSpaceReference()));
  }

  @Test
  public void test_extractReference() {
    ImmutableDocumentReference clone = new ImmutableDocumentReference(docRef);
    assertSame(docRef, docRef.extractReference(EntityType.DOCUMENT));
    clone.extractReference(EntityType.SPACE).setName("asdf");
    clone.extractReference(EntityType.WIKI).setName("asdf");
    assertEquals(docRef, clone);
  }

  @Test
  public void test_getWikiReference() {
    ImmutableDocumentReference clone = new ImmutableDocumentReference(docRef);
    clone.getWikiReference().setName("asdf");
    assertEquals(docRef, clone);
  }

  @Test
  public void test_getLastSpaceReference() {
    ImmutableDocumentReference clone = new ImmutableDocumentReference(docRef);
    clone.getLastSpaceReference().setName("asdf");
    assertEquals(docRef, clone);
  }

  @Test
  public void test_getSpaceReferences() {
    ImmutableDocumentReference clone = new ImmutableDocumentReference(docRef);
    clone.getSpaceReferences().get(0).setName("asdf");
    assertEquals(docRef, clone);
  }

  @Test
  public void serialize() {
    assertEquals("wiki:space.doc", Utils.getComponent(EntityReferenceSerializer.class).serialize(
        docRef));
  }

}
