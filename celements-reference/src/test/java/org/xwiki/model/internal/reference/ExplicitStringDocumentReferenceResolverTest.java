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
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Unit tests for
 * {@link org.xwiki.model.internal.reference.ExplicitStringDocumentReferenceResolver}.
 *
 * @version $Id: 1e7fa718ee39db6efd8d5609f99e4e19f72445a9 $
 * @since 2.2.3
 */
public class ExplicitStringDocumentReferenceResolverTest {

  private DocumentReferenceResolver<String> resolver;

  @Before
  public void setUp() throws Exception {
    this.resolver = new ExplicitStringDocumentReferenceResolver();
    ReflectionUtils.setFieldValue(this.resolver, "entityReferenceResolver",
        new ExplicitStringEntityReferenceResolver());
  }

  @Test
  public void testResolveWithExplicitDocumentReference() {
    DocumentReference reference = this.resolver.resolve("",
        new DocumentReference("wiki", "space", "page"));

    assertEquals("page", reference.getName());
    assertEquals("space", reference.getLastSpaceReference().getName());
    assertEquals("wiki", reference.getWikiReference().getName());
  }
}
