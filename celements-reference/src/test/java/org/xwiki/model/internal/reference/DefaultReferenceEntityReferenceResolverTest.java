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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.InvalidEntityReferenceException;

/**
 * Unit tests for {@link DefaultReferenceEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class DefaultReferenceEntityReferenceResolverTest {

  private EntityReferenceResolver<EntityReference> resolver;

  private Mockery mockery = new Mockery();

  @Before
  public void setUp() {
    resolver = new DefaultReferenceEntityReferenceResolver();
    final EntityReferenceValueProvider mockValueProvider = mockery
        .mock(EntityReferenceValueProvider.class);
    ReflectionUtils.setFieldValue(resolver, "provider", mockValueProvider);

    mockery.checking(new Expectations() {

      {
        allowing(mockValueProvider).getDefaultValue(EntityType.SPACE);
        will(returnValue("defspace"));
        allowing(mockValueProvider).getDefaultValue(EntityType.WIKI);
        will(returnValue("defwiki"));
        allowing(mockValueProvider).getDefaultValue(EntityType.DOCUMENT);
        will(returnValue("defpage"));
        allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT);
        will(returnValue("defobject"));
        allowing(mockValueProvider).getDefaultValue(EntityType.OBJECT_PROPERTY);
        will(returnValue("defproperty"));
      }
    });
  }

  @Test
  public void testResolveDocumentReferenceWhenMissingParents() {
    EntityReference partialReference = new EntityReference("page", EntityType.DOCUMENT);

    EntityReference reference = resolver.resolve(partialReference, EntityType.DOCUMENT);

    assertNotSame(partialReference, reference);
    assertEquals("defspace", reference.getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getType());
    assertEquals("defwiki", reference.getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
  }

  @Test
  public void testResolveAttachmentReferenceWhenMissingParents() {
    EntityReference reference = resolver
        .resolve(new EntityReference("filename", EntityType.ATTACHMENT), EntityType.ATTACHMENT);

    assertEquals("defpage", reference.getParent().getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
    assertEquals("defspace", reference.getParent().getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
    assertEquals("defwiki", reference.getParent().getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
  }

  @Test
  public void testResolveDocumentReferenceWhenMissingParentBetweenReferences() {
    EntityReference partialReference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("wiki", EntityType.WIKI));

    EntityReference reference = resolver.resolve(partialReference, EntityType.DOCUMENT);

    assertNotSame(partialReference, reference);
    assertEquals("defspace", reference.getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getType());
    assertNotSame(partialReference.getParent().getParent(),
        reference.getParent().getParent());
    assertEquals("wiki", reference.getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
  }

  @Test
  public void testResolveAttachmentReferenceWhenMissingParentBetweenReferences() {
    EntityReference reference = resolver
        .resolve(new EntityReference("filename", EntityType.ATTACHMENT, new EntityReference(
            "wiki", EntityType.WIKI)), EntityType.ATTACHMENT);

    assertEquals("defpage", reference.getParent().getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
    assertEquals("defspace", reference.getParent().getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
    assertEquals("wiki", reference.getParent().getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
  }

  @Test
  public void testResolveDocumentReferenceWhenInvalidReference() {
    try {
      resolver
          .resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("filename",
              EntityType.ATTACHMENT)), EntityType.DOCUMENT);
      fail("Should have thrown an exception here");
    } catch (InvalidEntityReferenceException expected) {
      assertEquals(
          "Invalid reference [name = [page], type = [DOCUMENT], parent = [name = [filename], "
              + "type = [ATTACHMENT], parent = [null]]]",
          expected.getMessage());
    }
  }

  @Test
  public void testResolveDocumentReferenceWhenTypeIsSpace() {
    EntityReference reference = resolver
        .resolve(new EntityReference("space", EntityType.SPACE), EntityType.DOCUMENT);

    assertEquals(EntityType.DOCUMENT, reference.getType());
    assertEquals("defpage", reference.getName());
    assertEquals(EntityType.SPACE, reference.getParent().getType());
    assertEquals("space", reference.getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    assertEquals("defwiki", reference.getParent().getParent().getName());
  }

  @Test
  public void testResolveSpaceReferenceWhenTypeIsDocument() {
    EntityReference reference = resolver
        .resolve(new EntityReference("page", EntityType.DOCUMENT), EntityType.SPACE);

    assertEquals(EntityType.SPACE, reference.getType());
    assertEquals("defspace", reference.getName());
    assertEquals(EntityType.WIKI, reference.getParent().getType());
    assertEquals("defwiki", reference.getParent().getName());
  }

  /**
   * Tests that a relative object reference is resolved correctly and completed with the default
   * document parent.
   */
  @Test
  public void testResolveObjectReferenceWhenMissingParents() {
    EntityReference reference = resolver.resolve(new EntityReference("object", EntityType.OBJECT),
        EntityType.OBJECT);
    assertEquals(EntityType.OBJECT, reference.getType());
    assertEquals("object", reference.getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
    assertEquals("defpage", reference.getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
    assertEquals("defspace", reference.getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    assertEquals("defwiki", reference.getParent().getParent().getParent().getName());
  }

  /**
   * Tests that a relative object property is resolved correctly and completed with the default
   * object parent.
   */
  @Test
  public void testResolveObjectPropertyReferenceWhenMissingParents() {
    EntityReference reference = resolver.resolve(
        new EntityReference("property", EntityType.OBJECT_PROPERTY), EntityType.OBJECT_PROPERTY);
    assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
    assertEquals("property", reference.getName());
    assertEquals(EntityType.OBJECT, reference.getParent().getType());
    assertEquals("defobject", reference.getParent().getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
    assertEquals("defpage", reference.getParent().getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
    assertEquals("defspace", reference.getParent().getParent().getParent().getName());
    assertEquals(EntityType.WIKI,
        reference.getParent().getParent().getParent().getParent().getType());
    assertEquals("defwiki",
        reference.getParent().getParent().getParent().getParent().getName());
  }

  /**
   * Test that a document reference, when resolved as an object reference, is correctly completed
   * with the default
   * values for object name.
   */
  @Test
  public void testResolveObjectReferenceWhenTypeIsDocument() {
    EntityReference reference = resolver
        .resolve(new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space",
            EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))), EntityType.OBJECT);
    assertEquals(EntityType.OBJECT, reference.getType());
    assertEquals("defobject", reference.getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
    assertEquals("page", reference.getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
    assertEquals("space", reference.getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
    assertEquals("wiki", reference.getParent().getParent().getParent().getName());
  }

  /**
   * Test that a document reference, when resolved as a property reference, is correctly completed
   * with the default
   * values for object and property name.
   */
  @Test
  public void testResolveObjectPropertyReferenceWhenTypeIsDocument() {
    EntityReference reference = resolver.resolve(
        new EntityReference("page", EntityType.DOCUMENT, new EntityReference("space",
            EntityType.SPACE, new EntityReference("wiki", EntityType.WIKI))),
        EntityType.OBJECT_PROPERTY);
    assertEquals(EntityType.OBJECT_PROPERTY, reference.getType());
    assertEquals("defproperty", reference.getName());
    assertEquals(EntityType.OBJECT, reference.getParent().getType());
    assertEquals("defobject", reference.getParent().getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getParent().getType());
    assertEquals("page", reference.getParent().getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getParent().getType());
    assertEquals("space", reference.getParent().getParent().getParent().getName());
    assertEquals(EntityType.WIKI,
        reference.getParent().getParent().getParent().getParent().getType());
    assertEquals("wiki",
        reference.getParent().getParent().getParent().getParent().getName());
  }

  @Test
  public void testResolveDocumentReferenceWhenNullReference() {
    EntityReference reference = resolver.resolve(null, EntityType.DOCUMENT);

    assertEquals(EntityType.DOCUMENT, reference.getType());
    assertEquals("defpage", reference.getName());
    assertEquals(EntityType.SPACE, reference.getParent().getType());
    assertEquals("defspace", reference.getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getType());
    assertEquals("defwiki", reference.getParent().getParent().getName());
  }
}
