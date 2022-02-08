package com.celements.model.reference;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ImmutableDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.web.Utils;

public class RefBuilderTest extends AbstractComponentTest {

  ModelUtils modelUtils;
  WikiReference wikiRef;
  SpaceReference spaceRef;
  DocumentReference docRef;
  AttachmentReference attRef;

  @Before
  public void prepareTest() throws Exception {
    modelUtils = Utils.getComponent(ModelUtils.class);
    wikiRef = new WikiReference("wiki");
    spaceRef = new SpaceReference("space", wikiRef);
    docRef = new ImmutableDocumentReference("doc", spaceRef);
    attRef = new AttachmentReference("att.jpg", docRef);
  }

  @Test
  public void test_wikiRef() {
    assertRefStrict(wikiRef, new RefBuilder().with(wikiRef).build());
    assertRefStrict(wikiRef, new RefBuilder().wiki(wikiRef.getName()).build());
    assertRefStrict(wikiRef, new RefBuilder().with(EntityType.WIKI, wikiRef.getName()).build());
  }

  @Test
  public void test_spaceRef() {
    assertRefStrict(spaceRef, new RefBuilder().with(spaceRef).build());
    assertRefStrict(spaceRef, new RefBuilder().with(wikiRef).space(spaceRef.getName()).build());
    assertRefStrict(spaceRef, new RefBuilder().with(wikiRef).with(EntityType.SPACE,
        spaceRef.getName()).build());
  }

  @Test
  public void test_docRef() {
    assertRefStrict(docRef, new RefBuilder().with(docRef).build());
    assertRefStrict(docRef, new RefBuilder().with(spaceRef).doc(docRef.getName()).build());
    assertRefStrict(docRef, new RefBuilder().with(spaceRef).with(EntityType.DOCUMENT,
        docRef.getName()).build());
  }

  @Test
  public void test_attRef() {
    assertRefStrict(attRef, new RefBuilder().with(attRef).build());
    assertRefStrict(attRef, new RefBuilder().with(docRef).att(attRef.getName()).build());
    assertRefStrict(attRef, new RefBuilder().with(docRef).with(EntityType.ATTACHMENT,
        attRef.getName()).build());
  }

  @Test
  public void test_uniqueness() {
    RefBuilder builder = RefBuilder.from(spaceRef);
    assertNotSame(builder.build(), builder.build());
  }

  @Test
  public void test_build_token() {
    assertRefStrict(wikiRef, new RefBuilder().with(wikiRef).build(WikiReference.class));
    assertRefStrict(spaceRef, new RefBuilder().with(spaceRef).build(SpaceReference.class));
    assertRefStrict(docRef, new RefBuilder().with(docRef).build(DocumentReference.class));
    assertRefStrict(attRef, new RefBuilder().with(attRef).build(AttachmentReference.class));
  }

  @Test
  public void test_build_EntityType() {
    assertRefStrict(wikiRef, new RefBuilder().with(wikiRef).build(EntityType.WIKI));
    assertRefStrict(spaceRef, new RefBuilder().with(spaceRef).build(EntityType.SPACE));
    assertRefStrict(docRef, new RefBuilder().with(docRef).build(EntityType.DOCUMENT));
    assertRefStrict(attRef, new RefBuilder().with(attRef).build(EntityType.ATTACHMENT));
  }

  private void assertRefStrict(EntityReference expected, EntityReference actual) {
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
  public void test_incomplete() {
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        new RefBuilder().build();
      }
    }.evaluate();
    new ExceptionAsserter<IllegalArgumentException>(IllegalArgumentException.class) {

      @Override
      protected void execute() throws Exception {
        new RefBuilder().with(spaceRef).build(DocumentReference.class);
      }
    }.evaluate();
  }

  @Test
  public void test_incomplete_nullable() {
    assertNull(new RefBuilder().nullable().build());
    assertNull(new RefBuilder().nullable().buildRelative());
    assertNull(new RefBuilder().nullable().with(spaceRef).build(DocumentReference.class));
  }

}
