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
 *
 */
package com.xpn.xwiki.objects;

import static org.junit.Assert.*;

import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.store.id.IdVersion;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for the {@link BaseElement} class.
 *
 * @version $Id$
 */
public class BaseObjectTest extends AbstractBridgedComponentTestCase {

  @Test
  public void testSetWikiSetName() throws Exception {
    BaseObject baseObject = new BaseObject();

    baseObject.setWiki("otherwiki");

    assertEquals("otherwiki", baseObject.getWiki());
    assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("Main", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("WebHome", baseObject.getDocumentReference().getName());

    baseObject.setName("space.page");

    assertEquals("otherwiki", baseObject.getWiki());
    assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("page", baseObject.getDocumentReference().getName());
  }

  @Test
  public void testSetNameSetWiki() throws Exception {
    String database = getContext().getDatabase();
    BaseObject baseObject = new BaseObject();

    baseObject.setName("space.page");

    assertEquals(database, baseObject.getWiki());
    assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("page", baseObject.getDocumentReference().getName());

    baseObject.setWiki("otherwiki");

    assertEquals("otherwiki", baseObject.getWiki());
    assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("page", baseObject.getDocumentReference().getName());
  }

  @Test
  public void testSetNameAloneWithChangingContext() throws Exception {
    String database = getContext().getDatabase();
    BaseObject baseObject = new BaseObject();

    baseObject.setName("space.page");

    try {
      getContext().setDatabase("otherwiki");

      assertEquals(database, baseObject.getWiki());
      assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
      assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
      assertEquals("page", baseObject.getDocumentReference().getName());

      baseObject.setName("otherspace.otherpage");
    } finally {
      getContext().setDatabase(database);
    }

    assertEquals(database, baseObject.getWiki());
    assertEquals(database, baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("otherspace", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("otherpage", baseObject.getDocumentReference().getName());

    baseObject = new BaseObject();
    try {
      getContext().setDatabase("otherwiki");
      baseObject.setName("space.page");
    } finally {
      getContext().setDatabase(database);
    }

    assertEquals("otherwiki", baseObject.getWiki());
    assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("space", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("page", baseObject.getDocumentReference().getName());

    baseObject.setName("otherspace.otherpage");

    assertEquals("otherwiki", baseObject.getWiki());
    assertEquals("otherwiki", baseObject.getDocumentReference().getWikiReference().getName());
    assertEquals("otherspace", baseObject.getDocumentReference().getLastSpaceReference().getName());
    assertEquals("otherpage", baseObject.getDocumentReference().getName());
  }

  @Test
  public void test_clone() {
    BaseObject obj = new BaseObject();
    obj.setId(5, IdVersion.XWIKI_2);
    BaseObject clone = (BaseObject) obj.clone();
    assertEquals(obj.getGuid(), clone.getGuid());
    assertTrue(clone.hasValidId());
    assertEquals(obj.getId(), clone.getId());
    assertEquals(obj.getIdVersion(), clone.getIdVersion());
  }

  @Test
  public void test_duplicate() {
    BaseObject obj = new BaseObject();
    obj.setId(5, IdVersion.XWIKI_2);
    BaseObject duplicate = obj.duplicate();
    assertNotNull(duplicate.getGuid());
    assertNotEquals(obj.getGuid(), duplicate.getGuid());
    assertFalse(duplicate.hasValidId());
    assertEquals(0, duplicate.getId());
  }

  @Test
  public void test_classRef_relativity() {
    BaseObject obj = new BaseObject();
    obj.setDocumentReference(new DocumentReference("db", "space", "doc"));
    obj.setXClassReference(new DocumentReference("otherdb", "space", "class"));
    assertEquals("db", obj.getXClassReference().getWikiReference().getName());
  }

}
