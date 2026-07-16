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
package com.xpn.xwiki.internal.cache.rendering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit test for {@link DefaultRenderingCacheConfiguration}.
 *
 * @version $Id$
 * @since 2.4M1
 */
public class DefaultRenderingCacheConfigurationTest extends AbstractComponentTest {

  private RenderingCacheConfiguration configuration;

  private DocumentReference documentReference;

  private MapConfigurationSource wikiConfigurationSource;

  private MapConfigurationSource xwikiPropertiesConfigurationSource;

  private MapConfigurationSource getWikiConfigurationSource() {
    return wikiConfigurationSource;
  }

  private MapConfigurationSource getXWikiPropertiesConfigurationSource() {
    return xwikiPropertiesConfigurationSource;
  }

  @Before
  public void prepareTest() throws Exception {
    wikiConfigurationSource = new MapConfigurationSource();
    xwikiPropertiesConfigurationSource = new MapConfigurationSource();
    registerComponentMock(ConfigurationSource.class, "wiki", wikiConfigurationSource);
    registerComponentMock(ConfigurationSource.class, "xwikiproperties",
        xwikiPropertiesConfigurationSource);
    configuration = getBeanFactory().getBean(RenderingCacheConfiguration.class);
    documentReference = new DocumentReference("wiki", "space", "page");

    getContext().setDatabase("wiki");
  }

  @Test
  public void test_isCached_withNoConfiguration() throws Exception {
    Assert.assertFalse(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_whenDisabled() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", false);

    Assert.assertFalse(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_whenDisabledWithNoConfiguration() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);

    Assert.assertFalse(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withExactReference() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);
    source.setProperty("core.renderingcache.documents",
        Collections.singletonList("wiki:space.page"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withWrongReference() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);
    source.setProperty("core.renderingcache.documents",
        Collections.singletonList("wrongreference"));

    Assert.assertFalse(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withOnePattern() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);
    source.setProperty("core.renderingcache.documents", Collections.singletonList("wiki:space.*"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withSeveralPattern() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);
    source.setProperty("core.renderingcache.documents",
        Arrays.asList("wrongreference", "wiki:space.*"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withSeveralPattern2() throws Exception {
    MapConfigurationSource source = getXWikiPropertiesConfigurationSource();

    source.setProperty("core.renderingcache.enabled", true);
    source.setProperty("core.renderingcache.documents",
        Arrays.asList("wiki:space.*", "wrongreference"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));
  }

  @Test
  public void test_isCached_withWikiConfiguration() throws Exception {
    getXWikiPropertiesConfigurationSource().setProperty("core.renderingcache.enabled", true);

    MapConfigurationSource wikiSource = getWikiConfigurationSource();

    wikiSource.setProperty("core.renderingcache.enabled", true);
    wikiSource.setProperty("core.renderingcache.documents",
        Arrays.asList("wiki:space.*", "wrongreference"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));

    wikiSource.setProperty("core.renderingcache.documents",
        Arrays.asList("space.*", "wrongreference"));

    Assert.assertTrue(this.configuration.isCached(this.documentReference));
  }

  private static class MapConfigurationSource implements ConfigurationSource {

    private final Map<String, Object> properties = new HashMap<>();

    void setProperty(String key, Object value) {
      properties.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, T defaultValue) {
      return (T) properties.getOrDefault(key, defaultValue);
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass) {
      return valueClass.cast(properties.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key) {
      return (T) properties.get(key);
    }

    @Override
    public List<String> getKeys() {
      return new ArrayList<>(properties.keySet());
    }

    @Override
    public boolean containsKey(String key) {
      return properties.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
      return properties.isEmpty();
    }
  }
}
