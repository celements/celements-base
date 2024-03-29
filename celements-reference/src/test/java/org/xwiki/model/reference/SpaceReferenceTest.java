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

import org.junit.Test;
import org.xwiki.model.EntityType;

/**
 * Unit tests for {@link org.xwiki.model.reference.SpaceReference}.
 *
 * @version $Id: aa1605fe7716f71b9146033941ac8583584049b3 $
 * @since 2.2M1
 */
public class SpaceReferenceTest {

  @Test
  public void testInvalidType() {
    try {
      new SpaceReference(new EntityReference("space", EntityType.WIKI));
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid type [WIKI] for a space reference", expected.getMessage());
    }
  }

  @Test
  public void testInvalidNullParent() {
    try {
      new SpaceReference("page", (WikiReference) null);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid parent reference [null] in a space reference",
          expected.getMessage());
    }
  }

  @Test
  public void testInvalidParentType() {
    try {
      new SpaceReference(new EntityReference("space", EntityType.SPACE,
          new EntityReference("whatever", EntityType.DOCUMENT, null)));
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals(
          "Invalid parent reference [name = [whatever], type = [DOCUMENT], parent = [null]] "
              + "in a space reference",
          expected.getMessage());
    }
  }
}
