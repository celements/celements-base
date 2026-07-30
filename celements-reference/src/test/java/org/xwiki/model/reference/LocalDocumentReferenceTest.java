package org.xwiki.model.reference;

import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.model.EntityType;

public class LocalDocumentReferenceTest {

  @Test
  public void testFromDocumentReference() {
    DocumentReference documentReference = new DocumentReference("wiki", "space", "document");

    LocalDocumentReference reference = new LocalDocumentReference(documentReference);

    assertEquals("space", reference.getSpaceName());
    assertEquals("document", reference.getDocumentName());
    assertNull(reference.extractReference(EntityType.WIKI));
    assertEquals(documentReference,
        reference.getDocRef(documentReference.getWikiReference()));
  }

  @Test
  public void testRejectsInvalidReference() {
    assertThrows(IllegalArgumentException.class,
        () -> new LocalDocumentReference(new WikiReference("wiki")));
  }
}
