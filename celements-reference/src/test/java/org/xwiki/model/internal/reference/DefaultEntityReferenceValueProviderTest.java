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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider}.
 *
 * @version $Id: 2df8c30e29571f619571a6e6986863b4646101e2 $
 * @since 2.3M1
 */
public class DefaultEntityReferenceValueProviderTest {

  private Mockery mockery = new Mockery();

  private EntityReferenceValueProvider provider;

  @Before
  public void setUp() {
    this.provider = new DefaultEntityReferenceValueProvider();
    final ModelConfiguration mockConfiguration = this.mockery.mock(ModelConfiguration.class);
    ReflectionUtils.setFieldValue(this.provider, "configuration", mockConfiguration);

    this.mockery.checking(new Expectations() {

      {
        allowing(mockConfiguration).getDefaultReferenceValue(EntityType.SPACE);
        will(returnValue("defspace"));
        allowing(mockConfiguration).getDefaultReferenceValue(EntityType.WIKI);
        will(returnValue("defwiki"));
        allowing(mockConfiguration).getDefaultReferenceValue(EntityType.DOCUMENT);
        will(returnValue("defpage"));
        allowing(mockConfiguration).getDefaultReferenceValue(EntityType.ATTACHMENT);
        will(returnValue("deffilename"));
      }
    });
  }

  @Test
  public void testGetDefaultValue() {
    assertEquals("defpage", this.provider.getDefaultValue(EntityType.DOCUMENT));
    assertEquals("defspace", this.provider.getDefaultValue(EntityType.SPACE));
    assertEquals("deffilename", this.provider.getDefaultValue(EntityType.ATTACHMENT));
    assertEquals("defwiki", this.provider.getDefaultValue(EntityType.WIKI));
  }
}
