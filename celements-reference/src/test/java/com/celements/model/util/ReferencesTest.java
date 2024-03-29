package com.celements.model.util;

import static com.celements.model.util.References.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.google.common.base.Optional;

public class ReferencesTest {

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
  public void test_isAbsoluteRef() {
    assertTrue(isAbsoluteRef(wikiRef));
    assertTrue(isAbsoluteRef(spaceRef));
    assertTrue(isAbsoluteRef(docRef));
    assertTrue(isAbsoluteRef(attRef));
    ObjectReference objRef = new ObjectReference("Class.Obj", docRef);
    assertTrue(isAbsoluteRef(objRef));
    // ObjectPropertyReference is buggy, always contains EntityType.OBJECT, see setType
    // assertTrue(isAbsoluteRef(new ObjectPropertyReference("field", objRef)));
    assertFalse(isAbsoluteRef(getRelativeRefResolver().resolve(
        "space.doc", EntityType.DOCUMENT)));
    assertTrue(isAbsoluteRef(getRelativeRefResolver().resolve(
        "wiki:space.doc", EntityType.DOCUMENT)));
    assertFalse(isAbsoluteRef(new EntityReference("wiki", EntityType.WIKI, new EntityReference(
        "superwiki", EntityType.WIKI))));
  }

  @Test
  public void test_cloneRef() {
    SpaceReference ref = docRef.getLastSpaceReference();
    EntityReference clone = cloneRef(ref);
    assertTrue(clone instanceof SpaceReference);
    assertClone(ref, clone);
    assertEquals(ref.getParent(), clone.getParent());
  }

  @Test
  public void test_cloneRef_wikiRef() {
    WikiReference ref = docRef.getWikiReference();
    WikiReference clone = cloneRef(ref, WikiReference.class);
    assertClone(ref, clone);
    assertClone(ref.getParent(), clone.getParent());
  }

  @Test
  public void test_cloneRef_spaceRef() {
    SpaceReference ref = docRef.getLastSpaceReference();
    SpaceReference clone = cloneRef(ref, SpaceReference.class);
    assertClone(ref, clone);
    assertEquals(ref.getParent(), clone.getParent());
  }

  @Test
  public void test_cloneRef_docRef() {
    DocumentReference ref = attRef.getDocumentReference();
    EntityReference clone = cloneRef(ref, DocumentReference.class);
    assertClone(ref, clone);
    assertEquals(ref.getParent(), clone.getParent());
    assertEquals(ref.getParent().getParent(), clone.getParent().getParent());
    assertTrue(clone instanceof DocumentReference);
  }

  @Test
  public void test_cloneRef_entityRef() {
    DocumentReference ref = attRef.getDocumentReference();
    EntityReference clone = cloneRef(ref, EntityReference.class);
    assertTrue(clone instanceof DocumentReference);
    assertClone(ref, clone);
    assertEquals(ref.getParent(), clone.getParent());
    assertEquals(ref.getParent().getParent(), clone.getParent().getParent());
  }

  @Test
  public void test_cloneRef_relative() {
    EntityReference ref = new EntityReference("doc", EntityType.DOCUMENT, new EntityReference(
        "space", EntityType.SPACE));
    assertClone(ref, cloneRef(ref));
    assertClone(ref, cloneRef(ref, EntityReference.class));
    assertEquals(ref.getParent(), cloneRef(ref).getParent());
  }

  @Test
  public void test_cloneRef_wrongAbsoluteType() {
    assertThrows("cannot clone space reference as document reference",
        IllegalArgumentException.class, () -> cloneRef(spaceRef, DocumentReference.class));
  }

  @Test
  public void test_cloneRef_relativeAsAbsolute() {
    final EntityReference ref = getRelativeRefResolver().resolve("space.doc", EntityType.DOCUMENT);
    assertThrows(" cannot clone relative reference as absolute",
        IllegalArgumentException.class, () -> cloneRef(ref, DocumentReference.class));
  }

  @Test
  public void test_asCompleteRef() {
    EntityReference relative = getRelativeRefResolver().resolve("wiki:space.doc",
        EntityType.DOCUMENT);
    assertEquals(docRef, asCompleteRef(relative, DocumentReference.class));
    assertEquals(docRef, asCompleteRef(relative, EntityReference.class));
  }

  @Test
  public void test_asCompleteRef_incomplete() {
    EntityReference relative = getRelativeRefResolver().resolve("space.doc", EntityType.DOCUMENT);
    assertThrows(IllegalArgumentException.class,
        () -> asCompleteRef(relative, DocumentReference.class));
  }

  private EntityReferenceResolver<String> getRelativeRefResolver() {
    return new RelativeStringEntityReferenceResolver();
  }

  @Test
  public void test_extractRef() {
    assertEquals(wikiRef, extractRef(docRef, WikiReference.class).get());
    assertEquals(spaceRef, extractRef(docRef, SpaceReference.class).get());
    assertEquals(docRef, extractRef(docRef, DocumentReference.class).get());
    assertFalse(extractRef(docRef, AttachmentReference.class).isPresent());
    assertEquals(attRef, extractRef(attRef, AttachmentReference.class).get());
    assertSame(docRef, extractRef(docRef, DocumentReference.class).get());
  }

  @Test
  public void test_adjustRef_higherLevel_1() {
    EntityReference toRef = new WikiReference("oWiki");
    DocumentReference ret = adjustRef(docRef, DocumentReference.class, toRef);
    assertEquals(toRef, ret.getWikiReference());
    assertEquals(docRef.getName(), ret.getName());
    assertEquals(spaceRef.getName(), ret.getLastSpaceReference().getName());
    assertEquals("wiki should have changed", toRef, ret.getWikiReference());
  }

  @Test
  public void test_adjustRef_higherLevel_2() {
    EntityReference toRef = new SpaceReference("oSpace", new WikiReference("oWiki"));
    DocumentReference ret = adjustRef(docRef, DocumentReference.class, toRef);
    assertEquals(toRef, ret.getLastSpaceReference());
    assertEquals(docRef.getName(), ret.getName());
    assertEquals("space should have changed", toRef, ret.getLastSpaceReference());
  }

  @Test
  public void test_adjustRef_sameLevel() {
    EntityReference toRef = new DocumentReference("oWiki", "oSpace", "oDoc");
    DocumentReference ret = adjustRef(docRef, DocumentReference.class, toRef);
    assertEquals("expecting toRef if same level entity", toRef, ret);
    assertNotSame(toRef, ret);
    assertEquals("doc should have changed", toRef, ret);
  }

  @Test
  public void test_adjustRef_lowerLevel() {
    EntityReference toRef = new AttachmentReference("oAtt", new DocumentReference("oWiki", "oSpace",
        "oDoc"));
    DocumentReference ret = adjustRef(docRef, DocumentReference.class, toRef);
    assertEquals("expecting docRef of lower level entity", toRef.getParent(), ret);
    assertNotSame(docRef, ret);
  }

  @Test
  public void test_create_wiki() {
    String name = "wiki";
    WikiReference wikiRef = create(WikiReference.class, name);
    assertNotNull(wikiRef);
    assertEquals(name, wikiRef.getName());
  }

  @Test
  public void test_create_space() {
    String name = "space";
    SpaceReference spaceRef = create(SpaceReference.class, name, wikiRef);
    assertNotNull(spaceRef);
    assertEquals(name, spaceRef.getName());
    assertEquals(wikiRef, spaceRef.getParent());
  }

  @Test
  public void test_create_doc() {
    String name = "doc";
    DocumentReference docRef = create(DocumentReference.class, name, spaceRef);
    assertNotNull(docRef);
    assertEquals(name, docRef.getName());
    assertEquals(spaceRef, docRef.getParent());
  }

  @Test
  public void test_create_parent_immutable() {
    String name = "file";
    AttachmentReference attRef = create(AttachmentReference.class, name,
        new DocumentReference(docRef));
    assertNotNull(attRef);
    assertEquals(name, attRef.getName());
    assertEquals(docRef, attRef.getParent());
    assertEquals(docRef, attRef.getDocumentReference());
  }

  @Test
  public void test_create_entity() {
    String name = "doc";
    EntityReference docRef = create(EntityType.DOCUMENT, name, spaceRef);
    assertNotNull(docRef);
    assertEquals(name, docRef.getName());
    assertEquals(spaceRef, docRef.getParent());
  }

  @Test
  public void test_create_childparent() {
    WikiReference wikiRef = new WikiReference("wiki");
    SpaceReference spaceRef = create(SpaceReference.class, "space", wikiRef);
    assertNotSame(wikiRef, spaceRef.getParent());
    assertEquals(wikiRef, spaceRef.getParent());
  }

  @Test
  public void test_completeRef_identity() {
    Optional<DocumentReference> ret = completeRef(DocumentReference.class, docRef);
    assertTrue(ret.isPresent());
    assertEquals(docRef, ret.get());
    assertNotSame(docRef, ret.get());
    assertTrue(ret.get() instanceof DocumentReference);
  }

  @Test
  public void test_completeRef_incomplete() {
    assertFalse(completeRef(WikiReference.class, create(EntityType.SPACE, "space")).isPresent());
    assertFalse(completeRef(DocumentReference.class, wikiRef, spaceRef).isPresent());
    // handle varargs pitfalls
    assertFalse(completeRef(WikiReference.class).isPresent());
    assertFalse(completeRef(WikiReference.class, (EntityReference[]) null).isPresent());
    assertFalse(completeRef(WikiReference.class, null, null).isPresent());
  }

  @Test
  public void test_completeRef_fromRelative() {
    Optional<DocumentReference> ret = completeRef(DocumentReference.class, create(
        EntityType.DOCUMENT, "doc"), create(EntityType.WIKI, "wiki"), spaceRef);
    assertTrue(ret.isPresent());
    assertEquals(docRef, ret.get());
    assertNotSame(docRef, ret.get());
    assertTrue(ret.get() instanceof DocumentReference);
  }

  @Test
  public void test_completeRef_EntityReference() {
    try {
      completeRef(EntityReference.class, docRef);
      fail("expecting IAE because EntityReference cannot be absolute");
    } catch (IllegalArgumentException exc) {
      // expected
    }
  }

  @Test
  public void test_completeRef_higher_1() {
    EntityReference toRef = new WikiReference("oWiki");
    assertEquals(docRef, completeRef(DocumentReference.class, docRef, toRef).get());
    DocumentReference ret = completeRef(DocumentReference.class, toRef, docRef).get();
    assertEquals(toRef, ret.getWikiReference());
    assertEquals(docRef.getName(), ret.getName());
    assertEquals(spaceRef.getName(), ret.getLastSpaceReference().getName());
    assertEquals("wiki should have changed", toRef, ret.getWikiReference());
  }

  @Test
  public void test_completeRef_higher_2() {
    EntityReference toRef = new SpaceReference("oSpace", new WikiReference("oWiki"));
    assertEquals(docRef, completeRef(DocumentReference.class, docRef, toRef).get());
    DocumentReference ret = completeRef(DocumentReference.class, toRef, docRef).get();
    assertEquals(toRef, ret.getLastSpaceReference());
    assertEquals(docRef.getName(), ret.getName());
    assertEquals("space should have changed", toRef, ret.getLastSpaceReference());
  }

  @Test
  public void test_completeRef_same() {
    EntityReference toRef = new DocumentReference("oWiki", "oSpace", "oDoc");
    assertEquals(docRef, completeRef(DocumentReference.class, docRef, toRef).get());
    DocumentReference ret = completeRef(DocumentReference.class, toRef, docRef).get();
    assertEquals("expecting toRef if same level entity", toRef, ret);
    assertNotSame(toRef, ret);
    assertEquals("doc should have changed", toRef, ret);
  }

  @Test
  public void test_completeRef_lower() {
    EntityReference toRef = new AttachmentReference("oAtt", new DocumentReference("oWiki", "oSpace",
        "oDoc"));
    assertEquals(docRef, completeRef(DocumentReference.class, docRef, toRef).get());
    DocumentReference ret = completeRef(DocumentReference.class, toRef, docRef).get();
    assertEquals("expecting docRef of lower level entity", toRef.getParent(), ret);
    assertNotSame(docRef, ret);
  }

  @Test
  public void test_combineRef() {
    SpaceReference spaceRef2 = new SpaceReference("space2", new WikiReference("wiki2"));
    WikiReference wikiRef2 = new WikiReference("wiki3");
    assertEquals(docRef, combineRef(docRef, spaceRef2, wikiRef2).get());
    assertEquals(docRef, combineRef(docRef, wikiRef2, spaceRef2).get());
    assertEquals(new DocumentReference("wiki2", "space2", "doc"), combineRef(spaceRef2, docRef,
        wikiRef).get());
    assertEquals(new DocumentReference("wiki2", "space2", "doc"), combineRef(spaceRef2, wikiRef2,
        docRef).get());
    assertEquals(new DocumentReference("wiki3", "space", "doc"), combineRef(wikiRef2, docRef,
        spaceRef2).get());
    assertEquals(new DocumentReference("wiki3", "space2", "doc"), combineRef(wikiRef2, spaceRef2,
        docRef).get());
  }

  @Test
  public void test_combineRef_type() {
    WikiReference wikiRef2 = new WikiReference("wiki2");
    assertEquals(wikiRef2, combineRef(EntityType.WIKI, wikiRef2, docRef).get());
    assertEquals(new SpaceReference("space", wikiRef2), combineRef(EntityType.SPACE, wikiRef2,
        docRef).get());
    assertEquals(new DocumentReference("wiki2", "space", "doc"), combineRef(EntityType.DOCUMENT,
        wikiRef2, docRef).get());
    assertFalse(combineRef(EntityType.ATTACHMENT, wikiRef2, docRef).isPresent());
  }

  @Test
  public void test_combineRef_absent() {
    assertFalse(combineRef(EntityType.SPACE, create(EntityType.DOCUMENT, "space")).isPresent());
    // handle varargs pitfalls
    assertFalse(combineRef().isPresent());
    assertFalse(combineRef((EntityReference[]) null).isPresent());
    assertFalse(combineRef((EntityReference) null, null).isPresent());
  }

  private void assertClone(Object expected, Object actual) {
    if (expected == null) {
      assertNull(actual);
    } else {
      assertNotSame(expected, actual);
      assertEquals(expected, actual);
    }
  }

}
