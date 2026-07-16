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
package org.xwiki.xml;

import static org.junit.Assert.*;

import org.junit.Test;

import com.celements.xml.test.AbstractComponentTest;

/**
 * Unit tests for {@link org.xwiki.xml.XMLUtils}.
 *
 * @version $Id$
 * @since 1.6M1
 */
public class XMLUtilsTest extends AbstractComponentTest {

  @Test
  public void test_escapeXMLComment() {
    assertEquals("-\\- ", XMLUtils.escapeXMLComment("-- "));
    assertEquals("-\\", XMLUtils.escapeXMLComment("-"));
    assertEquals("-\\-\\-\\", XMLUtils.escapeXMLComment("---"));
    assertEquals("- ", XMLUtils.escapeXMLComment("- "));
  }

  @Test
  public void test_unescapeXMLComment() {
    assertEquals("", XMLUtils.unescapeXMLComment("\\"));
    assertEquals("\\", XMLUtils.unescapeXMLComment("\\\\"));
    assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-"));
    assertEquals("--", XMLUtils.unescapeXMLComment("\\-\\-\\"));
  }
}
