package com.celements.configuration.composite;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.configuration.composite.FromWikiConfigurationSource;
import com.celements.configuration.composite.AllPropertiesConfigurationSource;
import com.celements.configuration.document.WikiPreferencesConfigurationSource;

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
    assertEquals(AllPropertiesConfigurationSource.class, sources.get(1).getClass());
  }

}
