package com.celements.configuration;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractBaseComponentTest;

public class DefaultConfigurationSourceTest extends AbstractBaseComponentTest {

  private DefaultConfigurationSource cfgSrc;

  @Before
  public void prepareTest() throws Exception {
    cfgSrc = getSpringContext().getBean(DefaultConfigurationSource.class);
  }

  @Test
  public void test_getSources() throws Exception {
    List<ConfigurationSource> sources = cfgSrc.getSources();
    assertEquals(2, sources.size());
    assertEquals(FromWikiConfigurationSource.class, sources.get(0).getClass());
    assertEquals(SpacePreferencesConfigurationSource.class, sources.get(1).getClass());
  }

}
