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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;
import org.xwiki.model.EntityType;

/**
 * Unit tests for {@link EntityReference}.
 *
 * @version $Id: 53bc47861f8ca4eeee59cc4166a3cd430bc3f25f $
 * @since 2.2M1
 */
public class EntityReferenceTest {

  private Map<String, Serializable> getParamMap(int nb) {
    Map<String, Serializable> map = new HashMap<>(nb);
    for (int i = 0; i < nb; i++) {
      map.put(UUID.randomUUID().toString(), UUID.randomUUID());
    }
    return map;
  }

  private boolean checkParamMap(EntityReference ref, Map<String, Serializable> map) {
    for (Map.Entry<String, Serializable> entry : map.entrySet()) {
      if (entry.getValue() != ref.getParameter(entry.getKey())) {
        return false;
      }
    }
    return true;
  }

  @Test
  public void testExtractReference() {
    EntityReference wiki = new EntityReference("wiki", EntityType.WIKI, null);
    EntityReference space = new EntityReference("space", EntityType.SPACE, wiki);
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT, space);

    assertSame(wiki, reference.extractReference(EntityType.WIKI));
    assertSame(space, reference.extractReference(EntityType.SPACE));
    assertSame(reference, reference.extractReference(EntityType.DOCUMENT));
    assertNull(reference.extractReference(EntityType.ATTACHMENT));
  }

  @Test
  public void testGetRoot() {
    EntityReference wiki = new EntityReference("wiki", EntityType.WIKI, null);
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE, wiki));
    assertSame(wiki, reference.getRoot());
  }

  @Test
  public void testGetReversedReferenceChain() {
    EntityReference wiki = new EntityReference("wiki", EntityType.WIKI, null);
    EntityReference space = new EntityReference("space", EntityType.SPACE, wiki);
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT, space);

    List<EntityReference> list = reference.getReversedReferenceChain();

    assertEquals(3, list.size());
    assertSame(wiki, list.get(0));
    assertSame(space, list.get(1));
    assertSame(reference, list.get(2));
  }

  @Test
  public void testCopyConstructor() {
    Map<String, Serializable> map1 = getParamMap(3);
    Map<String, Serializable> map2 = getParamMap(1);

    EntityReference parent = new EntityReference("space", EntityType.SPACE,
        new EntityReference("wiki", EntityType.WIKI, null, map2));

    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT, parent, map1);

    assertEquals("page", reference.getName());
    assertSame(EntityType.DOCUMENT, reference.getType());
    assertSame(parent, reference.getParent());
    assertTrue(checkParamMap(reference, map1));
    assertTrue(checkParamMap(reference.getParent().getParent(), map2));
  }

  @Test
  public void testEquals() {
    EntityReference reference1 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference2 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference3 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki2", EntityType.WIKI)));

    EntityReference reference4 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space2", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference5 = new EntityReference("page2", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference6 = new EntityReference("page", EntityType.DOCUMENT, null);

    Map<String, Serializable> map = getParamMap(3);
    EntityReference reference7 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)),
        map);

    EntityReference reference8 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)),
        map);

    EntityReference reference9 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI), map));

    EntityReference reference10 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI, null, map)));

    assertTrue(reference1.equals(reference1));
    assertTrue(reference1.equals(reference2));
    assertFalse(reference1.equals(reference3));
    assertFalse(reference1.equals(reference4));
    assertFalse(reference1.equals(reference5));
    assertFalse(reference1.equals(reference6));
    assertEquals(reference6, new EntityReference("page", EntityType.DOCUMENT, null));
    assertFalse(reference1.equals(reference7));
    assertTrue(reference7.equals(reference8));
    assertFalse(reference1.equals(reference9));
    assertFalse(reference7.equals(reference9));
    assertFalse(reference1.equals(reference10));
    assertFalse(reference7.equals(reference10));
  }

  @Test
  public void testHashCode() {
    EntityReference reference1 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference2 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference3 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki2", EntityType.WIKI)));

    EntityReference reference4 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space2", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference5 = new EntityReference("page2", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));

    EntityReference reference6 = new EntityReference("page", EntityType.DOCUMENT, null);

    Map<String, Serializable> map = getParamMap(3);
    EntityReference reference7 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)),
        map);

    EntityReference reference8 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)),
        map);

    EntityReference reference9 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI), map));

    EntityReference reference10 = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI, null, map)));

    assertEquals(reference1.hashCode(), reference1.hashCode());
    assertEquals(reference1.hashCode(), reference2.hashCode());
    assertFalse(reference1.hashCode() == reference3.hashCode());
    assertFalse(reference1.hashCode() == reference4.hashCode());
    assertFalse(reference1.hashCode() == reference5.hashCode());
    assertFalse(reference1.hashCode() == reference6.hashCode());
    assertEquals(reference6.hashCode(),
        new EntityReference("page", EntityType.DOCUMENT, null).hashCode());
    assertFalse(reference1.hashCode() == reference7.hashCode());
    assertEquals(reference7.hashCode(), reference8.hashCode());
    assertFalse(reference1.hashCode() == reference9.hashCode());
    assertFalse(reference7.hashCode() == reference9.hashCode());
    assertFalse(reference1.hashCode() == reference10.hashCode());
    assertFalse(reference7.hashCode() == reference10.hashCode());
  }

  @Test
  public void testCompareTo() {
    EntityReference reference = new EntityReference("f", EntityType.DOCUMENT,
        new EntityReference("e", EntityType.SPACE,
            new EntityReference("d", EntityType.WIKI)));

    EntityReference reference2 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("b", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)));

    EntityReference reference3 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("a", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)));

    Map map1 = new HashMap(3);
    map1.put("param1", "a");
    map1.put("param2", "b");
    map1.put("param3", "c");
    Map map2 = new HashMap(2);
    map2.put("param1", "a");
    map2.put("param3", "c");
    Map map3 = new HashMap(2);
    map3.put("param1", "b");
    map3.put("param3", "c");

    EntityReference reference4 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("a", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)),
        map1);

    EntityReference reference5 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("a", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)),
        map2);

    EntityReference reference6 = new EntityReference("c", EntityType.DOCUMENT,
        new EntityReference("a", EntityType.SPACE,
            new EntityReference("a", EntityType.WIKI)),
        map3);

    assertEquals(0, reference.compareTo(reference));

    List<EntityReference> list = new ArrayList<>();
    list.add(reference);
    list.add(reference2);
    list.add(reference3);
    list.add(reference4);
    list.add(reference5);
    list.add(reference6);
    Collections.sort(list);

    // Reference3 is first since it serializes as a:a:c which comes before a:b:c and d:e:f
    assertSame(reference, list.get(5));
    assertSame(reference2, list.get(4));
    assertSame(reference3, list.get(0));
    assertSame(reference4, list.get(1));
    assertSame(reference5, list.get(2));
    assertSame(reference6, list.get(3));
  }

  @Test
  public void testNullType() {
    try {
      new EntityReference("name", null);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference type cannot be null", expected.getMessage());
    }
  }

  @Test
  public void testNullName() {
    try {
      new EntityReference(null, EntityType.WIKI);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference name cannot be null or empty",
          expected.getMessage());
    }
  }

  @Test
  public void testEmptyName() {
    try {
      new EntityReference("", EntityType.WIKI);
      fail("Should have thrown an exception here");
    } catch (IllegalArgumentException expected) {
      assertEquals("An Entity Reference name cannot be null or empty",
          expected.getMessage());
    }
  }

  @Test
  public void testReplaceParent() {
    Map<String, Serializable> map1 = getParamMap(3);
    Map<String, Serializable> map2 = getParamMap(2);
    Map<String, Serializable> map3 = getParamMap(1);

    EntityReference wiki = new EntityReference("wiki", EntityType.WIKI, null, map3);
    EntityReference wiki2 = new EntityReference("wiki2", EntityType.WIKI, null);

    EntityReference space = new EntityReference("space", EntityType.SPACE, wiki, map2);
    EntityReference space2 = new EntityReference("space2", EntityType.SPACE, wiki);

    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT, space, map1);
    EntityReference referenceSpace2 = reference.replaceParent(space, space2);
    EntityReference referenceWiki2 = reference.replaceParent(wiki, wiki2);

    assertNotSame(reference, referenceSpace2);
    assertTrue(checkParamMap(referenceSpace2, map1));
    assertSame(space2, referenceSpace2.getParent());

    assertNotSame(reference, referenceWiki2);
    assertTrue(checkParamMap(referenceWiki2, map1));
    assertNotSame(space, referenceWiki2.getParent());
    assertTrue(checkParamMap(referenceWiki2.getParent(), map2));
    assertSame(wiki2, referenceWiki2.getParent().getParent());
  }

  @Test
  public void testEntityReferenceSerialization() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);

    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI, null, getParamMap(1)),
            getParamMap(2)),
        getParamMap(3));

    oos.writeObject(reference);

    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
    ObjectInputStream ois = new ObjectInputStream(bais);

    EntityReference outRefs = (EntityReference) ois.readObject();

    assertEquals(reference, outRefs);
  }
}
