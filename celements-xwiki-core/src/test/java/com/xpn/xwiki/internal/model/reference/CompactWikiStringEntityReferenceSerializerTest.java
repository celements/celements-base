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
import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit tests for
 * {@link com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer}.
 *
 * @version $Id: 28161a91ce952dbe3defd0070657aa419de9e5f9 $
 * @since 2.2M1
 */
public class CompactWikiStringEntityReferenceSerializerTest
    extends AbstractComponentTest {

  private EntityReferenceSerializer<EntityReference> serializer;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(ModelContext.class);
    serializer = getBeanFactory().getBean("compactwiki", EntityReferenceSerializer.class);
  }

  @Test
  public void test_serialize_whenInSameWiki() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    expect(getMock(ModelContext.class).getCurrentEntityReference())
        .andReturn(new WikiReference("wiki")).anyTimes();
    replayDefault();

    assertEquals("space.page", this.serializer.serialize(reference));
    assertEquals("space", this.serializer.serialize(reference.getParent()));
    verifyDefault();
  }

  @Test
  public void test_serialize_whenNotInSameWiki() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    expect(getMock(ModelContext.class).getCurrentEntityReference())
        .andReturn(new WikiReference("otherwiki")).anyTimes();
    replayDefault();

    assertEquals("wiki:space.page", this.serializer.serialize(reference));
    assertEquals("wiki:space", this.serializer.serialize(reference.getParent()));
    verifyDefault();
  }
}
