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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2.3
 */
public class RelativeStringEntityReferenceResolverTest {

  private EntityReferenceResolver resolver;

  @Before
  public void setUp() {
    resolver = new RelativeStringEntityReferenceResolver();
  }

  @Test
  public void testResolveDocumentReference() throws Exception {
    EntityReference reference = resolver.resolve("", EntityType.DOCUMENT);
    assertNull(reference);

    reference = resolver.resolve("space.page", EntityType.DOCUMENT);
    assertNull(reference.extractReference(EntityType.WIKI));
    assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
    assertEquals("page", reference.getName());

    reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
    assertEquals("wiki", reference.extractReference(EntityType.WIKI).getName());
    assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
    assertEquals("page", reference.getName());
  }

  @Test
  public void testResolveDocumentReferenceWithBaseReference() throws Exception {
    EntityReference reference = resolver.resolve("", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE));
    assertNull(reference.extractReference(EntityType.WIKI));
    assertEquals("space", reference.extractReference(EntityType.SPACE).getName());
    assertNull(reference.extractReference(EntityType.DOCUMENT));
  }
}
