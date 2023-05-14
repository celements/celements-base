package com.celements.configuration;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractBaseComponentTest;

public class FromWikiConfigurationSourceTest extends AbstractBaseComponentTest {

  private FromWikiConfigurationSource cfgSrc;

  @Before
  public void prepareTest() throws Exception {
    cfgSrc = getSpringContext().getBean(FromWikiConfigurationSource.class);
  }

  @Test
  public void test_getSources() throws Exception {
    List<ConfigurationSource> sources = cfgSrc.getSources();
    assertEquals(2, sources.size());
    assertEquals(WikiPreferencesConfigurationSource.class, sources.get(0).getClass());
    assertEquals(PropertiesConfigurationSource.class, sources.get(1).getClass());
  }

}
