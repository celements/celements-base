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
package org.xwiki.container.servlet.filters;

import static org.easymock.EasyMock.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.container.servlet.filters.SavedRequestManager.SavedRequest;

/**
 * Test for {@link SavedRequestManager}.
 *
 * @version $Id$
 * @since 2.5M1
 */
public class SavedRequestManagerTest {

  /** Fake test URL. */
  private static final String TEST_URL = "http://localhost/xwiki/bin/view/Test/Page";

  /** Mocked request. */
  private HttpServletRequest request;

  private HttpSession session;

  @Before
  public void prepareTest() {
    session = createMock(HttpSession.class);
    request = createMock(HttpServletRequest.class);
    final Map<String, String[]> params = new HashMap<>();
    params.put("aaa", new String[] { "bbb" });
    params.put("srid", new String[] { "r4Nd0m" });
    expect(request.getSession()).andReturn(session).anyTimes();
    expect(request.getParameterMap()).andReturn(params).anyTimes();
    expect(request.getRequestURL()).andReturn(new StringBuffer(TEST_URL)).anyTimes();
    expect(request.getParameter("srid")).andReturn("r4Nd0m").anyTimes();
    replay(request);
    final Map<String, SavedRequest> saveMap = new HashMap<>();
    saveMap.put("r4Nd0m", new SavedRequest(request));
    expect(session.getAttribute(anyString())).andReturn(saveMap).anyTimes();
    replay(session);
  }

  @Test
  public void testGetters() {
    Assert.assertEquals("srid", SavedRequestManager.getSavedRequestIdentifier());
    Assert.assertEquals(SavedRequest.class.getCanonicalName() + "_SavedRequests",
        SavedRequestManager.getSavedRequestKey());
  }

  @Test
  public void testSave() {
    String srid = SavedRequestManager.saveRequest(this.request);
    Assert.assertNotNull(srid);
    Assert.assertFalse("".equals(srid));
  }

  @Test
  public void testSavedUrl() {
    Assert.assertEquals(TEST_URL + "?srid=r4Nd0m",
        SavedRequestManager.getOriginalUrl(this.request));
  }
}
