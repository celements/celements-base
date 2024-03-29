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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

/**
 * Unit tests for {@link org.xwiki.model.internal.scripting.ModelScriptService}.
 *
 * @version $Id: 77b03a9643275e5588336bdb9df2750275884d7a $
 * @since 2.3M1
 */
public class ModelScriptServiceTest {

  private ModelScriptService service;

  private ComponentManager mockComponentManager;

  private DocumentReferenceResolver mockResolver;

  private Mockery mockery = new Mockery();

  @Before
  public void setUp() {
    this.service = new ModelScriptService();
    this.mockComponentManager = this.mockery.mock(ComponentManager.class);
    ReflectionUtils.setFieldValue(this.service, "componentManager", this.mockComponentManager);
    this.mockResolver = this.mockery.mock(DocumentReferenceResolver.class);
  }

  @Test
  public void testCreateDocumentReference() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(new DocumentReference("wiki", "space", "page"));
      }
    });

    this.service.createDocumentReference("wiki", "space", "page", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenEmptyParameters() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(null);
      }
    });

    this.service.createDocumentReference("", "", "", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenWikiParameterEmpty() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("space", EntityType.SPACE)));
      }
    });

    this.service.createDocumentReference("", "space", "page", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenSpaceParameterEmpty() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(new EntityReference("page", EntityType.DOCUMENT,
            new EntityReference("wiki", EntityType.WIKI)));
      }
    });

    this.service.createDocumentReference("wiki", "", "page", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenPageParameterEmpty() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(new EntityReference("space", EntityType.SPACE,
            new EntityReference("wiki", EntityType.WIKI)));
      }
    });

    this.service.createDocumentReference("wiki", "space", "", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenWikiAndSpaceParametersEmpty() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "default/reference");
        will(returnValue(mockResolver));

        allowing(mockResolver).resolve(new EntityReference("wiki", EntityType.WIKI));
      }
    });

    this.service.createDocumentReference("wiki", "", "", "default/reference");
  }

  @Test
  public void testCreateDocumentReferenceWhenInvalidHint() throws Exception {
    this.mockery.checking(new Expectations() {

      {
        allowing(mockComponentManager).lookup(DocumentReferenceResolver.class, "invalid");
        will(throwException(new ComponentLookupException("error")));
      }
    });

    assertNull(this.service.createDocumentReference("wiki", "space", "page", "invalid"));
  }
}
