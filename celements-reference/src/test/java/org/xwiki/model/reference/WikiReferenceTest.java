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
 * Unit tests for {@link WikiReference}.
 *
 * @version $Id: 12683b8b27b51f7f05b2c38f2fde8dd549a0da40 $
 * @since 2.2M1
 */
public class WikiReferenceTest {

  @Test
  public void testInvalidType() {
    try {
      new WikiReference(new EntityReference("wiki", EntityType.DOCUMENT));
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("Invalid type [DOCUMENT] for a wiki reference", expected.getMessage());
    }
  }
}
