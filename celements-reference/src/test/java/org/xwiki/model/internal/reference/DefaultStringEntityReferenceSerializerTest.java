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
import static org.easymock.EasyMock.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link DefaultStringEntityReferenceSerializer}.
 *
 * @version $Id: c15cbe154090a34024c74a452df06553e3ebd6c3 $
 * @since 2.2M1
 */
public class DefaultStringEntityReferenceSerializerTest extends AbstractBaseComponentTest {

  private static final String DEFAULT_WIKI = "defwiki";

  private static final String DEFAULT_SPACE = "defspace";

  private static final String DEFAULT_PAGE = "defpage";

  private static final String DEFAULT_ATTACHMENT = "deffilename";

  private static final String DEFAULT_OBJECT = "defobject";

  private static final String DEFAULT_OBJECT_PROPERTY = "defproperty";

  private static final String DEFAULT_CLASS_PROPERTY = "defclassproperty";

  private EntityReferenceSerializer<String> serializer;

  private EntityReferenceResolver<String> resolver;

  @Before
  public void prepareTest() throws Exception {
    EntityReferenceValueProvider valueProvider = registerComponentMock(
        EntityReferenceValueProvider.class);
    serializer = getBeanFactory().getBean(DefaultStringEntityReferenceSerializer.class);
    resolver = getBeanFactory().getBean(DefaultStringEntityReferenceResolver.class);

    expect(valueProvider.getDefaultValue(EntityType.WIKI)).andReturn(DEFAULT_WIKI).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.SPACE)).andReturn(DEFAULT_SPACE).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.DOCUMENT)).andReturn(DEFAULT_PAGE).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.ATTACHMENT))
        .andReturn(DEFAULT_ATTACHMENT).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.OBJECT)).andReturn(DEFAULT_OBJECT).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.OBJECT_PROPERTY))
        .andReturn(DEFAULT_OBJECT_PROPERTY).anyTimes();
    expect(valueProvider.getDefaultValue(EntityType.CLASS_PROPERTY))
        .andReturn(DEFAULT_CLASS_PROPERTY).anyTimes();
  }

  @Test
  public void test_serializeDocumentReference() throws Exception {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space.page", EntityType.DOCUMENT);
    assertEquals("wiki:space.page", serializer.serialize(reference));

    reference = resolver.resolve("wiki:space.", EntityType.DOCUMENT);
    assertEquals("wiki:space.defpage", serializer.serialize(reference));

    reference = resolver.resolve("space.", EntityType.DOCUMENT);
    assertEquals("defwiki:space.defpage", serializer.serialize(reference));

    reference = resolver.resolve("page", EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.page", serializer.serialize(reference));

    reference = resolver.resolve(".", EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.defpage", serializer.serialize(reference));

    reference = resolver.resolve(null, EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.defpage", serializer.serialize(reference));

    reference = resolver.resolve("", EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.defpage", serializer.serialize(reference));

    reference = resolver.resolve("wiki1.wiki2:wiki3:some.space.page", EntityType.DOCUMENT);
    assertEquals("wiki1.wiki2:wiki3:some\\.space.page", serializer.serialize(reference));

    reference = resolver.resolve("some.space.page", EntityType.DOCUMENT);
    assertEquals("defwiki:some\\.space.page", serializer.serialize(reference));

    reference = resolver.resolve("wiki:page", EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.wiki:page", serializer.serialize(reference));

    // Verify that passing null doesn't throw a NPE
    assertNull(serializer.serialize(null));

    // Test escapes

    reference = resolver.resolve("\\.:@\\.", EntityType.DOCUMENT);
    assertEquals("defwiki:defspace.\\.:@\\.", serializer.serialize(reference));

    reference = resolver.resolve("\\\\:\\\\.\\\\", EntityType.DOCUMENT);
    assertEquals("\\\\:\\\\.\\\\", serializer.serialize(reference));

    // The escaping here is not necessary but we want to test that it works
    reference = resolver.resolve("\\wiki:\\space.\\page", EntityType.DOCUMENT);
    assertEquals("wiki:space.page", serializer.serialize(reference));
    verifyDefault();
  }

  @Test
  public void test_serializeSpaceReference() throws Exception {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space1.space2", EntityType.SPACE);
    assertEquals("wiki:space1\\.space2", serializer.serialize(reference));
    verifyDefault();
  }

  @Test
  public void test_serializeAttachmentReference() throws Exception {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space.page@filename", EntityType.ATTACHMENT);
    assertEquals("wiki:space.page@filename", serializer.serialize(reference));

    reference = resolver.resolve("", EntityType.ATTACHMENT);
    assertEquals("defwiki:defspace.defpage@deffilename", serializer.serialize(reference));

    reference = resolver.resolve("wiki:space.page@my.png", EntityType.ATTACHMENT);
    assertEquals("wiki:space.page@my.png", serializer.serialize(reference));

    reference = resolver.resolve("some:file.name", EntityType.ATTACHMENT);
    assertEquals("defwiki:defspace.defpage@some:file.name", serializer.serialize(reference));

    // Test escapes

    reference = resolver.resolve(":.\\@", EntityType.ATTACHMENT);
    assertEquals("defwiki:defspace.defpage@:.\\@", serializer.serialize(reference));
    verifyDefault();
  }

  @Test
  public void test_serializeReferenceWithChild() {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:Space.Page", EntityType.DOCUMENT);
    assertEquals("wiki:Space", serializer.serialize(reference.getParent()));

    assertEquals("wiki", serializer.serialize(reference.getParent().getParent()));
    verifyDefault();
  }

  /**
   * Tests resolving and re-serializing an object reference.
   */
  @Test
  public void test_serializeObjectReference() {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space.page^Object", EntityType.OBJECT);
    assertEquals("wiki:space.page^Object", serializer.serialize(reference));

    // default values
    reference = resolver.resolve("", EntityType.OBJECT);
    assertEquals("defwiki:defspace.defpage^defobject", serializer.serialize(reference));

    // property reference with no object
    reference = resolver.resolve("wiki:space.page.property", EntityType.OBJECT);
    assertEquals("defwiki:defspace.defpage^wiki:space.page.property",
        serializer.serialize(reference));

    // test escaping character
    reference = resolver.resolve("wiki:space.page^Obje\\^ct", EntityType.OBJECT);
    assertEquals("wiki:space.page^Obje\\^ct", serializer.serialize(reference));

    reference = resolver.resolve("wiki:spa^ce.page^Obje\\^ct", EntityType.OBJECT);
    assertEquals("wiki:spa^ce.page^Obje\\^ct", serializer.serialize(reference));

    reference = resolver.resolve(":.\\^@", EntityType.OBJECT);
    assertEquals("defwiki:defspace.defpage^:.\\^@", serializer.serialize(reference));
    verifyDefault();
  }

  /**
   * Tests resolving and re-serializing an object reference.
   */
  @Test
  public void test_serializeObjectPropertyReference() {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space.page^xwiki.class[0].prop",
        EntityType.OBJECT_PROPERTY);
    assertEquals("wiki:space.page^xwiki.class[0].prop", serializer.serialize(reference));

    // default values
    reference = resolver.resolve("", EntityType.OBJECT_PROPERTY);
    assertEquals("defwiki:defspace.defpage^defobject.defproperty",
        serializer.serialize(reference));

    // using separators
    reference = resolver.resolve("space^page@attachment", EntityType.OBJECT_PROPERTY);
    Assert
        .assertEquals("defwiki:defspace.defpage^defobject.space^page@attachment",
            serializer.serialize(reference));

    reference = resolver.resolve("wiki:space^object", EntityType.OBJECT_PROPERTY);
    assertEquals("defwiki:defspace.defpage^defobject.wiki:space^object",
        serializer.serialize(reference));

    // test escaping character
    reference = resolver.resolve("wiki:space.page^xwiki.class[0].prop\\.erty",
        EntityType.OBJECT_PROPERTY);
    assertEquals("wiki:space.page^xwiki.class[0].prop\\.erty",
        serializer.serialize(reference));

    reference = resolver.resolve(":\\.^@", EntityType.OBJECT_PROPERTY);
    assertEquals("defwiki:defspace.defpage^defobject.:\\.^@",
        serializer.serialize(reference));
    verifyDefault();
  }

  /**
   * Tests resolving and re-serializing an object reference.
   */
  @Test
  public void test_serializeClassPropertyReference() {
    replayDefault();
    EntityReference reference = resolver.resolve("wiki:space.page^ClassProperty",
        EntityType.CLASS_PROPERTY);
    assertEquals("wiki:space.page^ClassProperty", serializer.serialize(reference));

    // default values
    reference = resolver.resolve("", EntityType.CLASS_PROPERTY);
    assertEquals("defwiki:defspace.defpage^defclassproperty",
        serializer.serialize(reference));

    // property reference with no object
    reference = resolver.resolve("wiki:space.page.property", EntityType.CLASS_PROPERTY);
    assertEquals("defwiki:defspace.defpage^wiki:space\\.page\\.property",
        serializer.serialize(reference));

    // test escaping character
    reference = resolver.resolve("wiki:space.page^Obje\\^ct", EntityType.CLASS_PROPERTY);
    assertEquals("wiki:space.page^Obje\\^ct", serializer.serialize(reference));

    reference = resolver.resolve("wiki:spa^ce.page^Obje\\^ct", EntityType.CLASS_PROPERTY);
    assertEquals("wiki:spa^ce.page^Obje\\^ct", serializer.serialize(reference));

    reference = resolver.resolve(":.\\^@", EntityType.CLASS_PROPERTY);
    assertEquals("defwiki:defspace.defpage^:\\.\\^@", serializer.serialize(reference));
    verifyDefault();
  }

  @Test
  public void test_serializeRelativeReference() {
    replayDefault();
    EntityReference reference = new EntityReference("page", EntityType.DOCUMENT);
    assertEquals("page", serializer.serialize(reference));

    reference = new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE));
    assertEquals("space.page", serializer.serialize(reference));
    verifyDefault();
  }
}
