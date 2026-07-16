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
package org.xwiki.model.internal.scripting;

import static org.junit.Assert.*;
import static org.easymock.EasyMock.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link org.xwiki.model.internal.scripting.ModelScriptService}.
 *
 * @version $Id: 77b03a9643275e5588336bdb9df2750275884d7a $
 * @since 2.3M1
 */
public class ModelScriptServiceTest extends AbstractBaseComponentTest {

  private ModelScriptService service;

  private DocumentReferenceResolver<EntityReference> resolver;

  @Before
  public void prepareTest() throws Exception {
    resolver = registerComponentMock(DocumentReferenceResolver.class, "default/reference");
    service = getBeanFactory().getBean(ModelScriptService.class);
  }

  @Test
  public void test_createDocumentReference() throws Exception {
    expect(resolver.resolve(new DocumentReference("wiki", "space", "page"))).andReturn(null);

    replayDefault();
    service.createDocumentReference("wiki", "space", "page", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenEmptyParameters() throws Exception {
    expect(resolver.resolve((EntityReference) null)).andReturn(null);

    replayDefault();
    service.createDocumentReference("", "", "", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenWikiParameterEmpty() throws Exception {
    expect(resolver.resolve(new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("space", EntityType.SPACE)))).andReturn(null);

    replayDefault();
    service.createDocumentReference("", "space", "page", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenSpaceParameterEmpty() throws Exception {
    expect(resolver.resolve(new EntityReference("page", EntityType.DOCUMENT,
        new EntityReference("wiki", EntityType.WIKI)))).andReturn(null);

    replayDefault();
    service.createDocumentReference("wiki", "", "page", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenPageParameterEmpty() throws Exception {
    expect(resolver.resolve(new EntityReference("space", EntityType.SPACE,
        new EntityReference("wiki", EntityType.WIKI)))).andReturn(null);

    replayDefault();
    service.createDocumentReference("wiki", "space", "", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenWikiAndSpaceParametersEmpty() throws Exception {
    expect(resolver.resolve(new EntityReference("wiki", EntityType.WIKI))).andReturn(null);

    replayDefault();
    service.createDocumentReference("wiki", "", "", "default/reference");
    verifyDefault();
  }

  @Test
  public void test_createDocumentReference_whenInvalidHint() throws Exception {
    replayDefault();
    assertNull(service.createDocumentReference("wiki", "space", "page", "invalid"));
    verifyDefault();
  }
}
