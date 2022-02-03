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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.xwiki.model.EntityType;

/**
 * Unit tests for {@link EntityReference}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class EntityReferenceTest {

  @Test
  public void testExtractReference() {
    EntityReference reference1 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals("wiki", reference1.extractReference(EntityType.WIKI).getName());
    assertEquals("space", reference1.extractReference(EntityType.SPACE).getName());
    assertEquals("page", reference1.extractReference(EntityType.DOCUMENT).getName());
    assertNull(reference1.extractReference(EntityType.ATTACHMENT));
  }

  @Test
  public void testGetRoot() {
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals(new EntityReference("wiki", EntityType.WIKI), reference.getRoot());
  }

  @Test
  public void testGetChild() {
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertNull(reference.getChild());
    assertEquals(reference, reference.getParent().getChild());
    assertEquals(reference, reference.getParent().getParent().getChild().getChild());
  }

  @Test
  public void testEquals() {
    EntityReference reference1 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals(reference1, reference1);

    EntityReference reference2 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals(reference1, reference2);

    EntityReference reference3 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki2", EntityType.WIKI)));
    assertFalse(reference1.equals(reference3));

    EntityReference reference4 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space2", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertFalse(reference1.equals(reference4));

    EntityReference reference5 = new EntityReference("page2", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertFalse(reference1.equals(reference5));

    EntityReference reference6 = new EntityReference("page", EntityType.DOCUMENT, null);
    assertFalse(reference1.equals(reference6));
    assertEquals(reference6, new EntityReference("page", EntityType.DOCUMENT, null));
  }

  @Test
  public void testHashCode() {
    EntityReference reference1 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals(reference1.hashCode(), reference1.hashCode());

    EntityReference reference2 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertEquals(reference1.hashCode(), reference2.hashCode());

    EntityReference reference3 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki2", EntityType.WIKI)));
    assertFalse(reference1.hashCode() == reference3.hashCode());

    EntityReference reference4 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space2", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertFalse(reference1.hashCode() == reference4.hashCode());

    EntityReference reference5 = new EntityReference("page2", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    assertFalse(reference1.hashCode() == reference5.hashCode());

    EntityReference reference6 = new EntityReference("page", EntityType.DOCUMENT, null);
    assertFalse(reference1.hashCode() == reference6.hashCode());
    assertEquals(reference6.hashCode(),
        new EntityReference("page", EntityType.DOCUMENT, null).hashCode());
  }

  @Test
  public void testClone() {
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
    EntityReference clonedReference = reference.clone();

    assertNotSame(reference, clonedReference);
    assertNotSame(reference.getParent(), clonedReference.getParent());
    assertNotSame(reference.getParent().getParent(),
        clonedReference.getParent().getParent());
    assertEquals(reference, clonedReference);
  }

  @Test
  public void testCompareTo() {
    EntityReference reference = new EntityReference("f", EntityType.DOCUMENT,
        new EntityReference("e", EntityType.SPACE,
            new EntityReference("d", EntityType.WIKI)));

    EntityReference reference2 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("b", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)));

    EntityReference reference3 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("a", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)));

    assertEquals(0, reference.compareTo(reference));

    List<EntityReference> list = new ArrayList<>();
    list.add(reference);
    list.add(reference2);
    list.add(reference3);
    Collections.sort(list);

    // Reference3 is first since it serializes as a:a:c which comes before a:b:c and d:e:f
    assertSame(reference, list.get(2));
    assertSame(reference2, list.get(1));
    assertSame(reference3, list.get(0));
  }

  @Test
  public void testNullType() {
    try {
      new EntityReference("name", null);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference type cannot be null", expected.getMessage());
    }
  }

  @Test
  public void testNullName() {
    try {
      new EntityReference(null, EntityType.WIKI);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference name cannot be null or empty",
          expected.getMessage());
    }
  }

  @Test
  public void testEmptyName() {
    try {
      new EntityReference("", EntityType.WIKI);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference name cannot be null or empty",
          expected.getMessage());
    }
  }
}
