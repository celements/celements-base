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
package com.xpn.xwiki.internal.model.reference;

import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link CurrentMixedReferenceEntityReferenceResolver}.
 *
 * @version $Id: ed8099512c314069c3279b9768632128ebdafd90 $
 */
public class CurrentMixedReferenceEntityReferenceResolverTest
    extends AbstractBridgedComponentTestCase {

  private static final String CURRENT_WIKI = "currentwiki";

  private static final String CURRENT_SPACE = "currentspace";

  private static final String CURRENT_PAGE = "currentpage";

  private EntityReferenceResolver<EntityReference> resolver;

  @Override
  public void setUp() throws Exception {
    super.setUp();

    this.resolver = getComponentManager().lookup(EntityReferenceResolver.class,
        "currentmixed/reference");
  }

  @Test
  public void testResolveAttachmentReferenceWhenMissingParentsAndContextDocument() {
    getContext().setDatabase(CURRENT_WIKI);
    getContext().setDoc(
        new XWikiDocument(new DocumentReference(CURRENT_WIKI, CURRENT_SPACE, CURRENT_PAGE)));

    EntityReference reference = resolver
        .resolve(new EntityReference("filename", EntityType.ATTACHMENT), EntityType.ATTACHMENT);

    assertEquals("WebHome", reference.getParent().getName());
    assertEquals(EntityType.DOCUMENT, reference.getParent().getType());
    assertEquals(CURRENT_SPACE, reference.getParent().getParent().getName());
    assertEquals(EntityType.SPACE, reference.getParent().getParent().getType());
    assertEquals(CURRENT_WIKI, reference.getParent().getParent().getParent().getName());
    assertEquals(EntityType.WIKI, reference.getParent().getParent().getParent().getType());
  }
}
