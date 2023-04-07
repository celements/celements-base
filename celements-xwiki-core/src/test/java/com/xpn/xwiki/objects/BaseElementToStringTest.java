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

public class BaseElementToStringTest extends AbstractBridgedComponentTestCase {

  private DocumentReference docRef = new DocumentReference("wiki", "Space", "Doc");
  private DocumentReference classRef = new DocumentReference("wiki", "Celements", "Class");

  @Test
  public void test_toString_BaseObject() throws Exception {
    BaseObject obj = new BaseObject();
    assertEquals("BaseObject ?_?_0", obj.toString());
    obj.setDocumentReference(docRef);
    assertEquals("BaseObject wiki:Space.Doc_?_0", obj.toString());
    obj.setXClassReference(classRef);
    assertEquals("BaseObject wiki:Space.Doc_Celements.Class_0", obj.toString());
    obj.setNumber(5);
    assertEquals("BaseObject wiki:Space.Doc_Celements.Class_5", obj.toString());
    obj.setId(9876543210L, IdVersion.CELEMENTS_3);
    assertEquals("BaseObject 9876543210 wiki:Space.Doc_Celements.Class_5", obj.toString());
    assertEquals("wiki:Space.Doc_Celements.Class_5", obj.toString(false));
  }

  @Test
  public void test_toString_BaseProperty() throws Exception {
    BaseProperty prop = new LongProperty();
    assertEquals("LongProperty ?", prop.toString());
    prop.setName("name");
    assertEquals("LongProperty name", prop.toString());
    prop.setId(9876543210L, IdVersion.CELEMENTS_3);
    assertEquals("LongProperty 9876543210 name", prop.toString());
    assertEquals("name", prop.toString(false));
  }

  @Test
  public void test_toString_BaseProperty_withBaseObject() throws Exception {
    BaseProperty prop = new LongProperty();
    prop.setName("name");
    BaseObject obj = new BaseObject();
    obj.setDocumentReference(docRef);
    obj.setXClassReference(classRef);
    obj.setNumber(5);
    obj.setId(9876543210L, IdVersion.CELEMENTS_3);
    prop.setObject(obj);
    assertEquals("LongProperty 9876543210 name wiki:Space.Doc_Celements.Class_5", prop.toString());
    assertEquals("name wiki:Space.Doc_Celements.Class_5", prop.toString(false));
  }

}
