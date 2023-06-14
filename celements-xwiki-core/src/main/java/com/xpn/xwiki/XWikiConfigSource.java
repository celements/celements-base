package com.xpn.xwiki;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;

@Component(XWikiConfigSource.NAME)
public class XWikiConfigSource implements ConfigurationSource {

  public static final String NAME = "xwikicfg";

  private static final String XWIKI_CFG_PATH = "/WEB-INF/xwiki.cfg";

  private final XWikiConfig cfg;

  @Inject
  public XWikiConfigSource(ServletContext servletContext) throws IOException, XWikiException {
    try (InputStream is = servletContext.getResourceAsStream(XWIKI_CFG_PATH)) {
      cfg = new XWikiConfig(is);
    }
  }

  public XWikiConfig getXWikiConfig() {
    return cfg;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key, T defaultValue) {
    return (T) cfg.getProperty(key, defaultValue != null ? defaultValue.toString() : null);
  }

  @Override
  public <T> T getProperty(String key, Class<T> valueClass) {
    return valueClass.cast(getProperty(key));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(String key) {
    return (T) cfg.getProperty(key);
  }

  @Override
  public List<String> getKeys() {
    return Collections.emptyList();
  }

  @Override
  public boolean containsKey(String key) {
    return getProperty(key) != null;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  public boolean isVirtualMode() {
    return "1".equals(getProperty("xwiki.virtual"));
  }

}
