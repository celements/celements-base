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
package org.xwiki.url.internal.standard;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.InvalidURLException;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.XWikiURLType;
import org.xwiki.url.standard.HostResolver;
import org.xwiki.url.standard.StandardURLConfiguration;

import com.celements.reference.test.AbstractComponentTest;

/**
 * Unit tests for {@link org.xwiki.url.internal.standard.StandardXWikiURLFactory}.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class StandardXWikiURLFactoryTest extends AbstractComponentTest {

  private XWikiURLFactory factory;

  private HostResolver hostResolver;

  private StandardURLConfiguration configuration;

  @Before
  public void prepareTest() throws Exception {
    configuration = registerComponentMock(StandardURLConfiguration.class);
    hostResolver = registerComponentMock(HostResolver.class);
    factory = getBeanFactory().getBean("standard", XWikiURLFactory.class);
    expect(hostResolver.resolve(anyString())).andReturn(new WikiReference("Wiki")).anyTimes();
    expect(configuration.isPathBasedMultiWiki()).andReturn(false).anyTimes();
    expect(configuration.getWikiPathPrefix()).andReturn("wiki").anyTimes();
  }

  @After
  public void verifyTest() {
    verifyDefault();
  }

  @Test
  public void test_createPathBasedXWikiURL() throws Exception {
    reset(configuration);
    expect(configuration.isPathBasedMultiWiki()).andReturn(true).anyTimes();
    expect(configuration.getWikiPathPrefix()).andReturn("wiki").anyTimes();
    replayDefault();
    // Verify Main wiki URL.
    XWikiURL xwikiURL = createURL("http://localhost:8080/xwiki/bin/view/Space/Page", false,
        "localhost");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

    // Verify Sub Wiki URL.
    xwikiURL = createURL("http://host/xwiki/wiki/subwiki/view/Space/Page", false, "subwiki");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));
  }

  @Test
  public void test_createDomainBasedXWikiURL() throws Exception {
    replayDefault();
    // Verify Main wiki URL.
    XWikiURL xwikiURL = createURL("http://localhost:8080/xwiki/bin/view/Space/Page", true,
        "localhost");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

    // Verify Sub Wiki URL.
    xwikiURL = createURL("http://subwiki.domain.ext/xwiki/bin/view/Space/Page", true,
        "subwiki.domain.ext");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));
  }

  @Test
  public void test_createXWikiURL_whenTrailingSlash() throws Exception {
    replayDefault();
    XWikiURL xwikiURL = createURL("http://host/xwiki/bin/", true, "host");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Main", "WebHome"));
  }

  @Test
  public void test_createXWikiURL_whenInvalidURL() throws Exception {
    replayDefault();
    try {
      // Invalid URL since the space in the page name isn't encoded.
      createURL("http://host/xwiki/bin/view/Space/Page Name", true, "host");
      Assert.fail("Should have thrown an exception here");
    } catch (InvalidURLException expected) {
      Assert.assertEquals("Invalid URL [http://host/xwiki/bin/view/Space/Page Name]",
          expected.getMessage());
    }
  }

  @Test
  public void test_createXWikiURL_whenURLHasParameters() throws Exception {
    replayDefault();
    XWikiURL xwikiURL = createURL(
        "http://host/xwiki/bin/view/Space/Page?param1=value1&param2=value2", true, "host");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

    // Note: the parameters order are the same as the order specified in the URL.
    Map<String, List<String>> expectedMap = new LinkedHashMap<>();
    expectedMap.put("param1", Arrays.asList("value1"));
    expectedMap.put("param2", Arrays.asList("value2"));
    Assert.assertEquals(expectedMap, xwikiURL.getParameters());

    // Verify it works when there's a param with no value.
    xwikiURL = createURL("http://host/xwiki/bin/view/Space/Page?param", true, "host");
    expectedMap = new LinkedHashMap<>();
    expectedMap.put("param", Collections.<String>emptyList());
    Assert.assertEquals(expectedMap, xwikiURL.getParameters());
  }

  @Test
  public void test_createXWikiURL_withEncodedChars() throws Exception {
    replayDefault();
    XWikiURL xwikiURL = createURL("http://host/xwiki/bin/view/Space/Page%20Name?param=%2D", true,
        "host");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page Name"));
    Map<String, List<String>> expectedMap = new LinkedHashMap<>();
    expectedMap.put("param", Arrays.asList("-"));
    Assert.assertEquals(expectedMap, xwikiURL.getParameters());
  }

  /**
   * Verify we ignore path parameters (see section 3.3 of the RFC 2396:
   * http://www.faqs.org/rfcs/rfc2396.html).
   */
  @Test
  public void test_createXWikiURL_whenURLHasPathParameters() throws Exception {
    replayDefault();
    XWikiURL xwikiURL = createURL(
        "http://host/xwiki/bin/view/Space;param1=value1/Page;param2=value2", true, "host");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "Page"));

    // Ensure we don't remove ";" when they are encoded in order to allow the ";" character to be in
    // document names
    // for example.
    xwikiURL = createURL("http://host/xwiki/bin/view/Space/My%3BPage", true, "host");
    assertXWikiURL(xwikiURL, "view", new DocumentReference("Wiki", "Space", "My;Page"));
  }

  private XWikiURL createURL(String url, final boolean isDomainBasedWikiFormat,
      final String expectedHost)
      throws Exception {
    return this.factory.createURL(new URL(url),
        Collections.<String, Object>singletonMap("ignorePrefix", "/xwiki"));
  }

  private void assertXWikiURL(XWikiURL xwikiURL, String expectedAction,
      EntityReference expectedReference)
      throws Exception {
    Assert.assertEquals(XWikiURLType.ENTITY, xwikiURL.getType());
    XWikiEntityURL entityURL = (XWikiEntityURL) xwikiURL;
    Assert.assertEquals(expectedAction, entityURL.getAction());
    Assert.assertEquals(expectedReference, entityURL.getEntityReference());
  }
}
