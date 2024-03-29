package com.celements.configuration;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.web.Utils;

public class CelementsDefaultConfigurationSourceTest extends AbstractComponentTest {

  private CelementsDefaultConfigurationSource cfgSrc;

  @Before
  public void prepareTest() throws Exception {
    Utils.getComponentManager().unregisterComponent(ConfigurationSource.class, "default");
    CelementsDefaultConfigurationSource instance = new CelementsDefaultConfigurationSource();
    instance.spacePreferencesSource = createDefaultMock(ConfigurationSource.class);
    instance.wikiPreferencesSource = createDefaultMock(ConfigurationSource.class);
    instance.celementsPropertiesSource = createDefaultMock(ConfigurationSource.class);
    instance.xwikiPropertiesSource = createDefaultMock(ConfigurationSource.class);
    instance.initialize();
    DefaultComponentDescriptor<ConfigurationSource> descriptor = new DefaultComponentDescriptor<>();
    descriptor.setRole(ConfigurationSource.class);
    descriptor.setRoleHint("default");
    descriptor.setImplementation(instance.getClass());
    Utils.getComponentManager().registerComponent(descriptor, instance);
    cfgSrc = (CelementsDefaultConfigurationSource) Utils.getComponent(ConfigurationSource.class);
  }

  @Test
  public void test_getProperty_none() throws Exception {
    String key = "key";

    expect(cfgSrc.spacePreferencesSource.containsKey(eq(key))).andReturn(false).once();
    expect(cfgSrc.wikiPreferencesSource.containsKey(eq(key))).andReturn(false).once();
    expect(cfgSrc.celementsPropertiesSource.containsKey(eq(key))).andReturn(false).once();
    expect(cfgSrc.xwikiPropertiesSource.containsKey(eq(key))).andReturn(false).once();

    replayDefault();
    assertNull(cfgSrc.getProperty(key));
    verifyDefault();
  }

  @Test
  public void test_getProperty_fromCel() throws Exception {
    String key = "key";
    String val = "val";

    expect(cfgSrc.spacePreferencesSource.containsKey(eq(key))).andReturn(false).once();
    expect(cfgSrc.wikiPreferencesSource.containsKey(eq(key))).andReturn(false).once();
    expect(cfgSrc.celementsPropertiesSource.containsKey(eq(key))).andReturn(true).once();
    expect(cfgSrc.celementsPropertiesSource.getProperty(eq(key))).andReturn(val).once();

    replayDefault();
    assertEquals(val, cfgSrc.getProperty(key));
    verifyDefault();
  }

}
