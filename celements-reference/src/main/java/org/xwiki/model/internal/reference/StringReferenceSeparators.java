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

package org.xwiki.model.internal.reference;

import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Define string separators for string reference serializer and resolver.
 *
 * @version $Id: d809294846d00a89524b7f20f914239095837198 $
 * @since 3.3M2
 */
interface StringReferenceSeparators {

  /**
   * A backslash string.
   */
  char CESCAPE = '\\';

  /**
   * A colon string. Colon is used to separate wiki name.
   */
  char CWIKISEP = ':';

  /**
   * A dot string. Dot is used to separate space names and document name.
   */
  char CSPACESEP = '.';

  /**
   * An at-sign string. At sign is used to separate attachment name.
   */
  char CATTACHMENTSEP = '@';

  /**
   * An hat sign string. Hat sign is used to separate object name.
   */
  char COBJECTSEP = '^';

  /**
   * An dot is used to separate object property name.
   */
  char CPROPERTYSEP = CSPACESEP;

  /**
   * An hat sign is used to separate class name.
   */
  char CCLASSPROPSEP = COBJECTSEP;

  /**
   * A backslash string.
   */
  String ESCAPE = Character.toString(CESCAPE);

  /**
   * A double backslash string.
   */
  String DBLESCAPE = ESCAPE + ESCAPE;

  /**
   * A colon string. Colon is used to separate wiki name.
   */
  String WIKISEP = Character.toString(CWIKISEP);

  /**
   * A dot string. Dot is used to separate space names and document name.
   */
  String SPACESEP = Character.toString(CSPACESEP);

  /**
   * An at-sign string. At sign is used to separate attachment name.
   */
  String ATTACHMENTSEP = Character.toString(CATTACHMENTSEP);

  /**
   * An hat sign string. Hat sign is used to separate object name.
   */
  String OBJECTSEP = Character.toString(COBJECTSEP);

  /**
   * An dot is used to separate object property name.
   */
  String PROPERTYSEP = Character.toString(CPROPERTYSEP);

  /**
   * An hat sign is used to separate class property name.
   */
  String CLASSPROPSEP = Character.toString(CCLASSPROPSEP);

  /**
   * The list of strings to escape for each type of entity.
   */
  Map<EntityType, List<String>> ESCAPES = ImmutableMap.<EntityType, List<String>>builder()
      .put(EntityType.ATTACHMENT, ImmutableList.of(ATTACHMENTSEP, ESCAPE))
      .put(EntityType.DOCUMENT, ImmutableList.of(SPACESEP, ESCAPE))
      .put(EntityType.SPACE, ImmutableList.of(SPACESEP, WIKISEP, ESCAPE))
      .put(EntityType.OBJECT, ImmutableList.of(OBJECTSEP, ESCAPE))
      .put(EntityType.OBJECT_PROPERTY, ImmutableList.of(PROPERTYSEP, ESCAPE))
      .put(EntityType.CLASS_PROPERTY, ImmutableList.of(CLASSPROPSEP, SPACESEP, ESCAPE))
      .build();

  /**
   * The replacement list corresponding to the list in {@link #ESCAPES} map.
   */
  Map<EntityType, List<String>> REPLACEMENTS = ImmutableMap.<EntityType, List<String>>builder()
      .put(EntityType.ATTACHMENT, ImmutableList.of(ESCAPE + ATTACHMENTSEP, DBLESCAPE))
      .put(EntityType.DOCUMENT, ImmutableList.of(ESCAPE + SPACESEP, DBLESCAPE))
      .put(EntityType.SPACE, ImmutableList.of(ESCAPE + SPACESEP, ESCAPE + WIKISEP, DBLESCAPE))
      .put(EntityType.OBJECT, ImmutableList.of(ESCAPE + OBJECTSEP, DBLESCAPE))
      .put(EntityType.OBJECT_PROPERTY, ImmutableList.of(ESCAPE + PROPERTYSEP, DBLESCAPE))
      .put(EntityType.CLASS_PROPERTY, ImmutableList.of(ESCAPE + CLASSPROPSEP, ESCAPE + SPACESEP,
          DBLESCAPE))
      .build();

  /**
   * Map defining syntax separators for each type of reference.
   */
  Map<EntityType, List<Character>> SEPARATORS = ImmutableMap.<EntityType, List<Character>>builder()
      .put(EntityType.DOCUMENT, ImmutableList.of(CSPACESEP, CWIKISEP))
      .put(EntityType.ATTACHMENT, ImmutableList.of(CATTACHMENTSEP, CSPACESEP, CWIKISEP))
      .put(EntityType.SPACE, ImmutableList.of(CWIKISEP))
      .put(EntityType.OBJECT, ImmutableList.of(COBJECTSEP, CSPACESEP, CWIKISEP))
      .put(EntityType.OBJECT_PROPERTY, ImmutableList.of(CPROPERTYSEP, COBJECTSEP, CSPACESEP,
          CWIKISEP))
      .put(EntityType.CLASS_PROPERTY, ImmutableList.of(CCLASSPROPSEP, CSPACESEP, CWIKISEP))
      .build();
}
