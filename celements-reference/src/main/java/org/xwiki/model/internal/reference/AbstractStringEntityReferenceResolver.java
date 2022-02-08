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

import static org.xwiki.model.internal.reference.StringReferenceSeparators.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Generic implementation deferring default values for unspecified reference parts to extending
 * classes. This allows
 * for example both the Current Entity Reference Resolver and the Default Entity Reference Resolver
 * to share the code
 * from this class.
 *
 * @see AbstractEntityReferenceResolver
 * @version $Id: 6a3fc29a3d3bf995ba951dff8b89ed8f3fcd22c7 $
 * @since 2.2M1
 */
public abstract class AbstractStringEntityReferenceResolver extends AbstractEntityReferenceResolver
    implements EntityReferenceResolver<String> {

  /**
   * Map defining ordered entity types of a proper reference chain for a given entity type.
   */
  private static final Map<EntityType, List<EntityType>> ENTITYTYPES = ImmutableMap
      .<EntityType, List<EntityType>>builder()
      .put(EntityType.DOCUMENT, ImmutableList.of(EntityType.DOCUMENT, EntityType.SPACE,
          EntityType.WIKI))
      .put(EntityType.ATTACHMENT, ImmutableList.of(EntityType.ATTACHMENT, EntityType.DOCUMENT,
          EntityType.SPACE, EntityType.WIKI))
      .put(EntityType.SPACE, ImmutableList.of(EntityType.SPACE, EntityType.WIKI))
      .put(EntityType.OBJECT, ImmutableList.of(EntityType.OBJECT, EntityType.DOCUMENT,
          EntityType.SPACE, EntityType.WIKI))
      .put(EntityType.OBJECT_PROPERTY, ImmutableList.of(EntityType.OBJECT_PROPERTY,
          EntityType.OBJECT, EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI))
      .put(EntityType.CLASS_PROPERTY, ImmutableList.of(EntityType.CLASS_PROPERTY,
          EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI))
      .build();

  /**
   * Array of character to unescape in entity names.
   */
  private static final String[] ESCAPEMATCHING = { DBLESCAPE, ESCAPE };

  /**
   * The replacement array corresponding to the array in {@link #ESCAPEMATCHING} array.
   */
  private static final String[] ESCAPEMATCHINGREPLACE = { ESCAPE, "" };

  @Override
  public EntityReference resolve(String entityReferenceRepresentation, EntityType type,
      Object... parameters) {
    // TODO: Once we support nested spaces, handle the possibility of having nested spaces. The
    // format is still
    // to be defined but it could be for example: Wiki:Space1.Space2.Page

    // First, check if there's a definition for the type
    if (!SEPARATORS.containsKey(type)) {
      throw new RuntimeException("No parsing definition found for Entity Type [" + type + "]");
    }

    // Handle the case when the passed representation is null. In this case we consider it similar
    // to passing
    // an empty string.
    StringBuilder representation;
    if (entityReferenceRepresentation == null) {
      representation = new StringBuilder();
    } else {
      representation = new StringBuilder(entityReferenceRepresentation);
    }

    EntityReference reference = null;
    List<Character> separatorsForType = SEPARATORS.get(type);
    List<EntityType> entityTypesForType = ENTITYTYPES.get(type);

    // Iterate over the representation string looking for iterators in the correct order (rightmost
    // separator
    // looked for first).
    for (int i = 0; i < separatorsForType.size(); i++) {
      String name;
      if (representation.length() > 0) {
        name = getSegmentName(representation, separatorsForType.get(i), entityTypesForType.get(i),
            parameters);
      } else {
        // There's no definition for the current segment use default values
        name = resolveDefaultValue(entityTypesForType.get(i), parameters);
      }

      if (name != null) {
        EntityReference newReference = new EntityReference(name, entityTypesForType.get(i));
        if (reference != null) {
          reference = reference.appendParent(newReference);
        } else {
          reference = newReference;
        }
      }
    }

    // Handle last entity reference's name
    String name;
    if (representation.length() > 0) {
      name = StringUtils.replaceEach(representation.toString(), ESCAPEMATCHING,
          ESCAPEMATCHINGREPLACE);
    } else {
      name = resolveDefaultValue(entityTypesForType.get(separatorsForType.size()), parameters);
    }

    if (name != null) {
      EntityReference newReference = new EntityReference(name,
          entityTypesForType.get(separatorsForType.size()));
      if (reference != null) {
        reference = reference.appendParent(newReference);
      } else {
        reference = newReference;
      }
    }

    return reference;
  }

  /**
   * Retrieve a segment name.
   *
   * @param representation
   *          the current string representation of the reference
   * @param separator
   *          the separator for the segment
   * @param entityType
   *          the type of the segment, used to get a default value if the name is empty
   * @param parameters
   *          optional parameters, forwarded to get a default value
   * @return the segment name
   */
  private String getSegmentName(StringBuilder representation, char separator, EntityType entityType,
      Object... parameters) {
    String name = null;

    // Search all characters for a non escaped separator. If found, then consider the part after the
    // character as
    // the reference name and continue parsing the part before the separator.
    boolean found = false;
    int i = representation.length();
    while (--i >= 0) {
      char currentChar = representation.charAt(i);
      int nextIndex = i - 1;
      char nextChar = 0;
      if (nextIndex >= 0) {
        nextChar = representation.charAt(nextIndex);
      }

      if (currentChar == separator) {
        int numberOfBackslashes = getNumberOfCharsBefore(CESCAPE, representation, nextIndex);

        if ((numberOfBackslashes % 2) == 0) {
          // Found a valid separator (not escaped), separate content on its left from content on its
          // right
          if (i == (representation.length() - 1)) {
            name = resolveDefaultValue(entityType, parameters);
          } else {
            name = representation.substring(i + 1, representation.length());
          }
          representation.delete(i, representation.length());
          found = true;
          break;
        } else {
          // Unescape the character
          representation.delete(nextIndex, i);
          --i;
        }
      } else if (nextChar == CESCAPE) {
        // Unescape the character
        representation.delete(nextIndex, i);
        --i;
      }
    }

    // If not found then the full buffer is the current reference segment
    if (!found) {
      name = representation.toString();
      representation.setLength(0);
    }

    return name;
  }

  /**
   * Search how many time the provided character is found consecutively started to the provided
   * index and before.
   *
   * @param c
   *          the character to be searched
   * @param representation
   *          the string being searched
   * @param currentPosition
   *          the current position where the search is started in backward direction
   * @return the number of character in the found group
   */
  private int getNumberOfCharsBefore(char c, StringBuilder representation, int currentPosition) {
    int position = currentPosition;

    while ((position >= 0) && (representation.charAt(position) == c)) {
      --position;
    }

    return currentPosition - position;
  }
}
