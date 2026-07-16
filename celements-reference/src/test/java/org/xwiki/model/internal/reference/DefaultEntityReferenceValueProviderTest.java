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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReferenceValueProvider;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider}.
 *
 * @version $Id: 2df8c30e29571f619571a6e6986863b4646101e2 $
 * @since 2.3M1
 */
public class DefaultEntityReferenceValueProviderTest extends AbstractBaseComponentTest {

  private EntityReferenceValueProvider provider;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(ModelConfiguration.class);
    provider = getBeanFactory().getBean(DefaultEntityReferenceValueProvider.class);
  }

  @Test
  public void test_getDefaultValue() {
    ModelConfiguration configuration = getMock(ModelConfiguration.class);
    expect(configuration.getDefaultReferenceValue(EntityType.SPACE)).andReturn("defspace");
    expect(configuration.getDefaultReferenceValue(EntityType.WIKI)).andReturn("defwiki");
    expect(configuration.getDefaultReferenceValue(EntityType.DOCUMENT)).andReturn("defpage");
    expect(configuration.getDefaultReferenceValue(EntityType.ATTACHMENT))
        .andReturn("deffilename");

    replayDefault();
    assertEquals("defpage", this.provider.getDefaultValue(EntityType.DOCUMENT));
    assertEquals("defspace", this.provider.getDefaultValue(EntityType.SPACE));
    assertEquals("deffilename", this.provider.getDefaultValue(EntityType.ATTACHMENT));
    assertEquals("defwiki", this.provider.getDefaultValue(EntityType.WIKI));
    verifyDefault();
  }
}
