package com.celements.configuration;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.XWikiPropertiesConfigurationSource;

import com.celements.common.test.AbstractBaseComponentTest;

public class PropertiesConfigurationSourceTest extends AbstractBaseComponentTest {

  private PropertiesConfigurationSource cfgSrc;

  @Before
  public void prepareTest() throws Exception {
    cfgSrc = getSpringContext().getBean(PropertiesConfigurationSource.class);
  }

  @Test
  public void test_getSources() throws Exception {
    List<ConfigurationSource> sources = cfgSrc.getSources();
    assertEquals(2, sources.size());
    assertEquals(XWikiPropertiesConfigurationSource.class, sources.get(0).getClass());
    assertEquals(CelementsPropertiesConfigurationSource.class, sources.get(1).getClass());
  }

}
