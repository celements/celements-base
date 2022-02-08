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
package org.xwiki.model.internal.reference;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Unit tests for {@link LocalReferenceEntityReferenceSerializer}.
 *
 * @version $Id: f52fe54e86e2a42c9213f4f119ca5693fa6c32f8 $
 */
public class LocalReferenceEntityReferenceSerializerTest {

  private EntityReferenceSerializer<EntityReference> serializer;

  @Before
  public void setUp() {
    this.serializer = new LocalReferenceEntityReferenceSerializer();
  }

  @Test
  public void testSerializeDocumentReference() throws Exception {
    EntityReference reference = this.serializer
        .serialize(new DocumentReference("wiki", "space", "page"));

    assertEquals(EntityType.DOCUMENT, reference.getType());
    assertEquals("page", reference.getName());
    assertEquals(EntityType.SPACE, reference.getParent().getType());
    assertEquals("space", reference.getParent().getName());
    assertNull(reference.getParent().getParent());
  }

  @Test
  public void testSerializeSpaceReferenceWithChild() {
    EntityReference reference = this.serializer
        .serialize(new SpaceReference("space", new WikiReference("wiki")));

    assertEquals(EntityType.SPACE, reference.getType());
    assertEquals("space", reference.getName());
    assertNull(reference.getParent());
  }
}
