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
package com.celements.web.sajson;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.junit.Before;
import org.junit.Test;

import com.celements.web.contextmenu.ERequestLiteral;

public class JSONparserTest {

  private Parser jsonParser;
  private ILexicalParser<ERequestLiteral> jsonEventHandlerMock;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    jsonEventHandlerMock = createMock(ILexicalParser.class);
    jsonParser = new Parser(null, jsonEventHandlerMock);
  }

  @Test
  public void testParse_arrayWithString() throws Exception {
    jsonEventHandlerMock.initEvent();
    jsonEventHandlerMock.openArrayEvent();
    jsonEventHandlerMock.stringEvent("cel_cm_agenda");
    jsonEventHandlerMock.stringEvent("cel_cm_navigation_menuitem");
    jsonEventHandlerMock.closeArrayEvent();
    jsonEventHandlerMock.finishEvent();
    replay(jsonEventHandlerMock);
    jsonParser.parse("[\"cel_cm_agenda\",\"cel_cm_navigation_menuitem\"]");
    verify(jsonEventHandlerMock);
  }
  
  @Test
  public void testParse_dictionary() throws Exception {
    jsonEventHandlerMock.initEvent();
    jsonEventHandlerMock.openDictionaryEvent();
    jsonEventHandlerMock.openPropertyEvent("elemId");
    jsonEventHandlerMock.stringEvent("Content.Agenda");
    jsonEventHandlerMock.closePropertyEvent();
    jsonEventHandlerMock.openPropertyEvent("cmClassNames");
    jsonEventHandlerMock.openArrayEvent();
    jsonEventHandlerMock.stringEvent("cel_cm_agenda");
    jsonEventHandlerMock.stringEvent("cel_cm_navigation_menuitem");
    jsonEventHandlerMock.closeArrayEvent();
    jsonEventHandlerMock.closePropertyEvent();
    jsonEventHandlerMock.closeDictionaryEvent();
    jsonEventHandlerMock.finishEvent();
    replay(jsonEventHandlerMock);
    jsonParser.parse("{\"elemId\" : \"Content.Agenda\","
        + " \"cmClassNames\" : [ \"cel_cm_agenda\","
        + "\"cel_cm_navigation_menuitem\"]}");
    verify(jsonEventHandlerMock);
  }
  
}
