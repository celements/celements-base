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
package com.xpn.xwiki.web;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.xpn.xwiki.test.AbstractComponentTest;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.SkinAction} class.
 *
 * @version $Id$
 */
public class SkinActionTest extends AbstractComponentTest {

  private SkinAction action;

  @Before
  public void prepareTest() {
    action = new SkinAction();
  }

  @Test
  public void test_isTextJavascript_javaScriptMimetype() {
    assertTrue(this.action.isJavascriptMimeType("text/javascript"));
  }

  @Test
  public void test_isApplicationJavascript_javaScriptMimetype() {
    assertTrue(this.action.isJavascriptMimeType("application/javascript"));
  }

  @Test
  public void test_isApplicationXJavascript_javaScriptMimetype() {
    assertTrue(this.action.isJavascriptMimeType("application/x-javascript"));
  }

  @Test
  public void test_isTextEcmascript_javaScriptMimetype() {
    assertTrue(this.action.isJavascriptMimeType("text/ecmascript"));
  }

  @Test
  public void test_isApplicationEcmascript_javaScriptMimetype() {
    assertTrue(this.action.isJavascriptMimeType("application/ecmascript"));
  }

  @Test
  public void test_npeJavascriptMimetype() {
    assertFalse(this.action.isJavascriptMimeType(null));
  }

  @Test
  public void test_incorrectSkinFile() {
    try {
      this.action.getSkinFilePath("../../resources/js/xwiki/xwiki.js", "colibri");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
    try {
      this.action.getSkinFilePath("../../../", "colibri");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
    try {
      this.action.getSkinFilePath("resources/js/xwiki/xwiki.js", "..");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
    try {
      this.action.getSkinFilePath("../resources/js/xwiki/xwiki.js", ".");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
  }

  @Test
  public void test_incorrectResourceFile() {
    try {
      this.action.getResourceFilePath("../../skins/js/xwiki/xwiki.js");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
    try {
      this.action.getResourceFilePath("../../../");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
    try {
      this.action.getResourceFilePath("../../redirect");
      assertTrue("should fail", false);
    } catch (IOException e) {
      // good
    }
  }
}
