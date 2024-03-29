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

import org.jmock.Expectations;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for
 * {@link com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer}.
 *
 * @version $Id: 28161a91ce952dbe3defd0070657aa419de9e5f9 $
 * @since 2.2M1
 */
public class CompactWikiStringEntityReferenceSerializerTest
    extends AbstractBridgedComponentTestCase {

  private EntityReferenceSerializer<EntityReference> serializer;

  private ModelContext mockModelContext;

  @Override
  protected void registerComponents() throws Exception {
    super.registerComponents();

    this.mockModelContext = registerMockComponent(ModelContext.class);
    this.serializer = getComponentManager().lookup(EntityReferenceSerializer.class, "compactwiki");
  }

  @org.junit.Test
  public void testSerializeWhenInSameWiki() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");

    getMockery().checking(new Expectations() {

      {
        allowing(mockModelContext).getCurrentEntityReference();
        will(returnValue(new WikiReference("wiki")));
      }
    });

    assertEquals("space.page", this.serializer.serialize(reference));
    assertEquals("space", this.serializer.serialize(reference.getParent()));
  }

  @org.junit.Test
  public void testSerializeWhenNotInSameWiki() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");

    getMockery().checking(new Expectations() {

      {
        allowing(mockModelContext).getCurrentEntityReference();
        will(returnValue(new WikiReference("otherwiki")));
      }
    });

    assertEquals("wiki:space.page", this.serializer.serialize(reference));
    assertEquals("wiki:space", this.serializer.serialize(reference.getParent()));
  }
}
