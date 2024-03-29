/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.model.reference;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.xwiki.model.EntityType;

/**
 * Unit tests for {@link org.xwiki.model.reference.DocumentReference}.
 *
 * @version $Id: 45198820c9570f5ff26bf4be23b8f91cbfe50538 $
 * @since 2.2M1
 */
public class DocumentReferenceTest {

  @Test
  public void testConstructors() {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    assertEquals(reference, new DocumentReference(
        new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE,
                new EntityReference("wiki", EntityType.WIKI, null)))));
    assertEquals(reference, new DocumentReference("wiki", Arrays.asList("space"), "page"));
    assertEquals(reference, new DocumentReference("page",
        new SpaceReference("space", new WikiReference("wiki"))));
  }

  @Test
  public void testInvalidType() {
    try {
      new DocumentReference(new EntityReference("page", EntityType.SPACE));
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid type [SPACE] for a document reference", expected.getMessage());
    }
  }

  @Test
  public void testInvalidNullParent() {
    try {
      new DocumentReference("page", null);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid parent reference [null] in a document reference",
          expected.getMessage());
    }
  }

  @Test
  public void testInvalidParentType() {
    try {
      new DocumentReference(
          new EntityReference("page", EntityType.DOCUMENT, new WikiReference("wiki")));
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "Invalid parent reference [name = [wiki], type = [WIKI], parent = [null]] in a "
              + "document reference",
          expected.getMessage());
    }
  }

  @Test
  public void testConstructorIsolation() {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    EntityReference spaceReference = reference.getParent();

    // Verify that create a new reference based on an existing one doesn't modify the existing one
    // in any manner.
    new DocumentReference(
        reference.getWikiReference().getName(), reference.getLastSpaceReference().getName(),
        reference.getName());
  }

  @Test
  public void testGetWikiReference() {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    assertEquals(new WikiReference("wiki"), reference.getWikiReference());
  }

  @Test
  public void testGetLastSpaceReferenceWhenOneSpace() {
    DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
    SpaceReference spaceReference = documentReference.getLastSpaceReference();
    assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceReference);
  }

  @Test
  public void testGetLastSpaceReferenceWhenMultipleSpaces() {
    DocumentReference reference = new DocumentReference("wiki", Arrays.asList("space1", "space2"),
        "page");
    assertEquals(
        new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
        reference.getLastSpaceReference());
  }

  @Test
  public void testSpaceReferences() {
    DocumentReference reference1 = new DocumentReference("wiki", "space", "page");
    List<SpaceReference> spaceRefs = reference1.getSpaceReferences();
    assertEquals(1, spaceRefs.size());
    assertEquals(new SpaceReference("space", new WikiReference("wiki")), spaceRefs.get(0));

    DocumentReference reference2 = new DocumentReference("wiki", Arrays.asList("space1", "space2"),
        "page");
    List<SpaceReference> spaceRefs2 = reference2.getSpaceReferences();
    assertEquals(2, spaceRefs2.size());
    assertEquals(new SpaceReference("space1", new WikiReference("wiki")), spaceRefs2.get(0));
    assertEquals(
        new SpaceReference("space2", new SpaceReference("space1", new WikiReference("wiki"))),
        spaceRefs2.get(1));
  }
}
