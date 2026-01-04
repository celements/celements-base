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
 *
 */

package com.xpn.xwiki;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

public class XWikiConfig extends Properties {

  public static final Splitter SPLITTER = Splitter
      .on(CharMatcher.anyOf(",").or(CharMatcher.whitespace()))
      .trimResults().omitEmptyStrings();

  public XWikiConfig() {
    // Default constructor so that properties can be added after constructing the instance
    // by using XWikiConfig.put().
  }

  public XWikiConfig(String path) throws XWikiException {
    try (FileInputStream fis = new FileInputStream(path)) {
      loadConfig(fis, path);
    } catch (IOException e) {
      Object[] args = { path };
      throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
          XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND, "Configuration file {0} not found", e,
          args);
    }
  }

  public XWikiConfig(InputStream is) throws XWikiException {
    if (is != null) {
      loadConfig(is, "");
    }
  }

  public void loadConfig(InputStream is, String path) throws XWikiException {
    try {
      load(is);
    } catch (IOException e) {
      Object[] args = { path };
      throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
          XWikiException.ERROR_XWIKI_CONFIG_FORMATERROR,
          "Error reading configuration file", e, args);
    }
  }

  /**
   * @return array of string splited from property.
   * @param param
   *          - name of property
   */
  public String[] getPropertyAsList(String param) {
    return getPropertyList(param).toArray(new String[0]);
  }

  public List<String> getPropertyList(String param) {
    return SPLITTER.splitToList(getProperty(param, ""));
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method trims the spaces around the value.
   * </p>
   *
   * @see java.util.Properties#getProperty(java.lang.String, java.lang.String)
   */
  @Override
  public String getProperty(String key, String defaultValue) {
    return getPropertyOpt(key).orElse(defaultValue);
  }

  /**
   * {@inheritDoc}
   * <p>
   * This method trims the spaces around the value.
   * </p>
   *
   * @see java.util.Properties#getProperty(java.lang.String)
   */
  @Override
  public String getProperty(String key) {
    return getPropertyOpt(key).orElse(null);
  }

  public Optional<String> getPropertyOpt(String key) {
    return getEnvProperty(key)
        .or(() -> Optional.ofNullable(super.getProperty(key)))
        .map(String::trim);
  }

  private Optional<String> getEnvProperty(String key) {
    var envKey = Optional.ofNullable(key)
        .map(k -> k.replace('.', '_').replace('-', '_'))
        .orElse("");
    return Optional.ofNullable(System.getenv(envKey.toUpperCase()))
        .or(() -> Optional.ofNullable(System.getenv(envKey)));
  }
}
