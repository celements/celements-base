package com.celements.configuration.composite;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractBaseComponentTest;
import com.celements.configuration.composite.AllPropertiesConfigurationSource;
import com.celements.configuration.properties.CelementsPropertiesConfigurationSource;
import com.celements.configuration.properties.XWikiPropertiesConfigurationSource;

public class AllPropertiesConfigurationSourceTest extends AbstractBaseComponentTest {

  private AllPropertiesConfigurationSource cfgSrc;

  @Before
  public void prepareTest() throws Exception {
    cfgSrc = getSpringContext().getBean(AllPropertiesConfigurationSource.class);
  }

  @Test
  public void test_getSources() throws Exception {
    List<ConfigurationSource> sources = cfgSrc.getSources();
    assertEquals(2, sources.size());
    assertEquals(XWikiPropertiesConfigurationSource.class, sources.get(0).getClass());
    assertEquals(CelementsPropertiesConfigurationSource.class, sources.get(1).getClass());
  }

}
