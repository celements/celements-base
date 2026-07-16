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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit tests for
 * {@link com.xpn.xwiki.internal.model.reference.CompactStringEntityReferenceSerializer}.
 *
 * @version $Id: be1f78c994b2a81714f6d841db6ee0566a358161 $
 */
public class CompactStringEntityReferenceSerializerTest extends AbstractComponentTest {

  private EntityReferenceSerializer<EntityReference> serializer;

  @Before
  public void prepareTest() throws Exception {
    serializer = getBeanFactory().getBean("compact", EntityReferenceSerializer.class);
  }

  @Test
  public void test_serialize_whenNoContext() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    assertEquals("wiki:space.page", this.serializer.serialize(reference));
  }

  @Test
  public void test_serialize_whenNoContextDocument() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");
    assertEquals("wiki:space.page", this.serializer.serialize(reference));
  }

  @Test
  public void test_serializeDocumentReference_whenContextDocument() throws Exception {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
    assertEquals("page", this.serializer.serialize(reference));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
    assertEquals("page", this.serializer.serialize(reference));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace",
        "otherpage")));
    assertEquals("space.page", this.serializer.serialize(reference));

    getContext().setDatabase("otherwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "otherspace",
        "otherpage")));
    assertEquals("wiki:space.page", this.serializer.serialize(reference));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "otherspace", "page")));
    assertEquals("space.page", this.serializer.serialize(reference));

    getContext().setDatabase("otherwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "otherspace",
        "page")));
    assertEquals("wiki:space.page", this.serializer.serialize(reference));

    getContext().setDatabase("otherwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
    assertEquals("wiki:space.page", this.serializer.serialize(reference));

    getContext().setDatabase("otherwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space",
        "otherpage")));
    assertEquals("wiki:space.page", this.serializer.serialize(reference));
  }

  @Test
  public void test_serializeSpaceReference_whenHasChildren() throws Exception {
    AttachmentReference reference = new AttachmentReference("filename",
        new DocumentReference("wiki", "space", "page"));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
    assertEquals("page", this.serializer.serialize(reference.getParent()));
    assertEquals("space", this.serializer.serialize(reference.getParent().getParent()));

    getContext().setDatabase("xwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("xwiki", "xspace", "xpage")));
    assertEquals("wiki:space.page", this.serializer.serialize(reference.getParent()));
    assertEquals("wiki:space", this.serializer.serialize(reference.getParent().getParent()));

  }

  @Test
  public void test_serializeAttachmentReference_whenContextDocument() throws Exception {
    AttachmentReference reference = new AttachmentReference("filename",
        new DocumentReference("wiki", "space", "page"));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
    assertEquals("filename", this.serializer.serialize(reference));

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "otherpage")));
    assertEquals("page@filename", this.serializer.serialize(reference));

    getContext().setDatabase("otherwiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("otherwiki", "space", "page")));
    assertEquals("wiki:space.page@filename", this.serializer.serialize(reference));
  }

  @Test
  public void test_serializeEntityReference_withExplicit() {
    DocumentReference reference = new DocumentReference("wiki", "space", "page");

    getContext().setDatabase("wiki");
    getContext().setDoc(new XWikiDocument(new DocumentReference("wiki", "space", "page")));
    assertEquals("space.page",
        this.serializer.serialize(reference, new EntityReference("otherspace", EntityType.SPACE)));
  }
}
