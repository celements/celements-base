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
package org.xwiki.configuration.internal;

import static com.celements.common.test.CelementsSpringTestUtil.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractBaseComponentTest;

/**
 * Unit tests for {@link org.xwiki.configuration.internal.SpacePreferencesConfigurationSource}.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class SpacePreferencesConfigurationSourceTest extends AbstractBaseComponentTest {

  private ConfigurationSource source;
  private DocumentReference currentDocRef;

  @Before
  public void prepare() throws Exception {
    currentDocRef = new DocumentReference("wiki", "space", "doc");
    registerComponentMocks(DocumentAccessBridge.class);
    source = getComponentManager().lookup(ConfigurationSource.class, "space");
  }

  @Test
  public void test_getProperty() throws Exception {
    String key = "key", value = "value";
    DocumentReference docRef = new DocumentReference("wiki", "space", "WebPreferences");
    DocumentReference classDocRef = new DocumentReference("wiki", "XWiki", "XWikiPreferences");
    expect(getMock(DocumentAccessBridge.class).getCurrentDocumentReference())
        .andReturn(currentDocRef).atLeastOnce();
    expect(getMock(DocumentAccessBridge.class).getProperty(docRef, classDocRef, key))
        .andReturn(value).atLeastOnce();

    replayDefault();
    assertEquals(value, source.getProperty(key));
    verifyDefault();
  }

  @Test
  public void test_getProperty_noCurrentDocRef() throws Exception {
    expect(getMock(DocumentAccessBridge.class).getCurrentDocumentReference())
        .andReturn(null).atLeastOnce();

    replayDefault();
    assertNull(source.getProperty("key"));
    verifyDefault();
  }
}
