package com.celements.model.reference;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

public class RefBuilderTest {

  WikiReference wikiRef;
  SpaceReference spaceRef;
  DocumentReference docRef;
  AttachmentReference attRef;

  @Before
  public void prepareTest() throws Exception {
    wikiRef = new WikiReference("wiki");
    spaceRef = new SpaceReference("space", wikiRef);
    docRef = new DocumentReference("doc", spaceRef);
    attRef = new AttachmentReference("att.jpg", docRef);
  }

  @Test
  public void test_wikiRef() {
    assertSpecificRef(wikiRef, new RefBuilder().with(wikiRef).build());
    assertSpecificRef(wikiRef, new RefBuilder().with(wikiRef).build());
    assertSpecificRef(wikiRef, new RefBuilder().wiki(wikiRef.getName()).build());
    assertSpecificRef(wikiRef, new RefBuilder().with(EntityType.WIKI, wikiRef.getName()).build());
  }

  @Test
  public void test_spaceRef() {
    assertSpecificRef(spaceRef, new RefBuilder().with(spaceRef).build());
    assertSpecificRef(spaceRef, new RefBuilder().with(wikiRef).space(spaceRef.getName()).build());
    assertSpecificRef(spaceRef, new RefBuilder().with(wikiRef).with(EntityType.SPACE,
        spaceRef.getName()).build());
  }

  @Test
  public void test_docRef() {
    assertSpecificRef(docRef, new RefBuilder().with(docRef).build());
    assertSpecificRef(docRef, new RefBuilder().with(spaceRef).doc(docRef.getName()).build());
    assertSpecificRef(docRef, new RefBuilder().with(spaceRef).with(EntityType.DOCUMENT,
        docRef.getName()).build());
  }

  @Test
  public void test_attRef() {
    assertSpecificRef(attRef, new RefBuilder().with(attRef).build());
    assertSpecificRef(attRef, new RefBuilder().with(docRef).att(attRef.getName()).build());
    assertSpecificRef(attRef, new RefBuilder().with(docRef).with(EntityType.ATTACHMENT,
        attRef.getName()).build());
  }

  @Test
  public void test_uniqueness() {
    RefBuilder builder = RefBuilder.from(spaceRef);
    assertNotSame(builder.build(), builder.build());
  }

  @Test
  public void test_build_token() {
    assertSpecificRef(wikiRef, new RefBuilder().with(wikiRef).build(WikiReference.class));
    assertSpecificRef(spaceRef, new RefBuilder().with(spaceRef).build(SpaceReference.class));
    assertSpecificRef(docRef, new RefBuilder().with(docRef).build(DocumentReference.class));
    assertSpecificRef(attRef, new RefBuilder().with(attRef).build(AttachmentReference.class));
    assertSpecificRef(docRef, new RefBuilder().with(attRef).build(DocumentReference.class));
    assertSpecificRef(spaceRef, new RefBuilder().with(attRef).build(SpaceReference.class));
    assertSpecificRef(wikiRef, new RefBuilder().with(attRef).build(WikiReference.class));
  }

  @Test
  public void test_build_EntityType() {
    assertSpecificRef(wikiRef, new RefBuilder().with(wikiRef).build(EntityType.WIKI));
    assertSpecificRef(spaceRef, new RefBuilder().with(spaceRef).build(EntityType.SPACE));
    assertSpecificRef(docRef, new RefBuilder().with(docRef).build(EntityType.DOCUMENT));
    assertSpecificRef(attRef, new RefBuilder().with(attRef).build(EntityType.ATTACHMENT));
    assertSpecificRef(docRef, new RefBuilder().with(attRef).build(EntityType.DOCUMENT));
    assertSpecificRef(spaceRef, new RefBuilder().with(attRef).build(EntityType.SPACE));
    assertSpecificRef(wikiRef, new RefBuilder().with(attRef).build(EntityType.WIKI));
  }

  @Test
  public void test_build_withnull() {
    RefBuilder builder = new RefBuilder().with(docRef).doc(null);
    assertThrows(IllegalArgumentException.class, () -> builder.build());
    assertThrows(IllegalArgumentException.class, () -> builder.build(DocumentReference.class));
    assertSpecificRef(spaceRef, builder.build(SpaceReference.class));
    assertNull(builder.clone().nullable().build());
  }

  private void assertSpecificRef(EntityReference expected, EntityReference actual) {
    assertEquals(expected, actual);
    assertEquals("illegal class: " + actual.getClass(), expected.getClass(), actual.getClass());
  }

  @Test
  public void test_buildRelative() {
    assertEntityRef(wikiRef, new RefBuilder().with(wikiRef).buildRelative());
    assertEntityRef(spaceRef, new RefBuilder().with(spaceRef).buildRelative());
    assertEntityRef(docRef, new RefBuilder().with(docRef).buildRelative());
    assertEntityRef(attRef, new RefBuilder().with(attRef).buildRelative());
  }

  @Test
  public void test_buildRelative_EntityType() {
    assertEntityRef(wikiRef, new RefBuilder().with(wikiRef).buildRelative(EntityType.WIKI));
    assertEntityRef(wikiRef, new RefBuilder().with(spaceRef).buildRelative(EntityType.WIKI));
    assertEntityRef(spaceRef, new RefBuilder().with(spaceRef).buildRelative(EntityType.SPACE));
    assertEntityRef(spaceRef, new RefBuilder().with(docRef).buildRelative(EntityType.SPACE));
    assertEntityRef(docRef, new RefBuilder().with(docRef).buildRelative(EntityType.DOCUMENT));
    assertEntityRef(docRef, new RefBuilder().with(attRef).buildRelative(EntityType.DOCUMENT));
    assertEntityRef(attRef, new RefBuilder().with(attRef).buildRelative(EntityType.ATTACHMENT));
  }

  private void assertEntityRef(EntityReference expected, EntityReference actual) {
    assertEquals(expected, actual);
    assertEquals("illegal class: " + actual.getClass(), EntityReference.class, actual.getClass());
  }

  @Test
  public void test_clone() {
    RefBuilder orig = RefBuilder.from(docRef);
    assertEquals(orig.build(), orig.clone().build());
    assertNotSame(orig, orig.clone());
  }

  @Test
  public void test_incomplete_empty() {
    RefBuilder builder = new RefBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.build());
  }

  @Test
  public void test_incomplete_missing() {
    RefBuilder builder = new RefBuilder().with(spaceRef);
    assertThrows(IllegalArgumentException.class, () -> builder.build(DocumentReference.class));
  }

  @Test
  public void test_incomplete_nullable() {
    assertNull(new RefBuilder().nullable().build());
    assertNull(new RefBuilder().nullable().buildRelative());
    assertNull(new RefBuilder().nullable().with(spaceRef).build(DocumentReference.class));
  }

}
