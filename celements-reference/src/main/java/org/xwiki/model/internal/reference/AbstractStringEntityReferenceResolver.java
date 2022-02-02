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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Generic implementation deferring default values for unspecified reference parts to extending
 * classes. This allows
 * for example both the Current Entity Reference Resolver and the Default Entity Reference Resolver
 * to share the code
 * from this class.
 *
 * @version $Id$
 * @since 2.2M1
 */
public abstract class AbstractStringEntityReferenceResolver
    implements EntityReferenceResolver<String> {

  private Map<EntityType, List<Character>> separators = new HashMap<EntityType, List<Character>>() {

    {
      put(EntityType.DOCUMENT, Arrays.asList('.', ':'));
      put(EntityType.ATTACHMENT, Arrays.asList('@', '.', ':'));
      put(EntityType.SPACE, Arrays.asList(':'));
      put(EntityType.OBJECT, Arrays.asList('^', '.', ':'));
      put(EntityType.OBJECT_PROPERTY, Arrays.asList('.', '^', '.', ':'));
    }
  };

  private Map<EntityType, List<EntityType>> entityTypes = new HashMap<EntityType, List<EntityType>>() {

    {
      put(EntityType.DOCUMENT,
          Arrays.asList(EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI));
      put(EntityType.ATTACHMENT,
          Arrays.asList(EntityType.ATTACHMENT, EntityType.DOCUMENT, EntityType.SPACE,
              EntityType.WIKI));
      put(EntityType.SPACE, Arrays.asList(EntityType.SPACE, EntityType.WIKI));
      put(EntityType.OBJECT, Arrays.asList(EntityType.OBJECT, EntityType.DOCUMENT, EntityType.SPACE,
          EntityType.WIKI));
      put(EntityType.OBJECT_PROPERTY, Arrays.asList(EntityType.OBJECT_PROPERTY, EntityType.OBJECT,
          EntityType.DOCUMENT, EntityType.SPACE, EntityType.WIKI));
    }
  };

  /**
   * @param type
   *          the entity type for which to return the default value to use (since the use has not
   *          specified it)
   * @param parameters
   *          optional parameters. Their meaning depends on the resolver implementation
   * @return the default value to use
   */
  protected abstract String getDefaultValue(EntityType type, Object... parameters);

  /**
   * {@inheritDoc}
   *
   * @see org.xwiki.model.reference.EntityReferenceResolver#resolve
   */
  @Override
  public EntityReference resolve(String entityReferenceRepresentation, EntityType type,
      Object... parameters) {
    // TODO: Once we support nested spaces, handle the possibility of having nested spaces. The
    // format is still
    // to be defined but it could be for example: Wiki:Space1.Space2.Page

    // First, check if there's a definition for the type
    if (!this.separators.containsKey(type)) {
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
    EntityReference lastReference = null;
    List<Character> separatorsForType = this.separators.get(type);
    List<EntityType> entityTypesForType = this.entityTypes.get(type);

    // Iterate over the representation string looking for iterators in the correct order (rightmost
    // separator
    // looked for first).
    for (int i = 0; i < separatorsForType.size(); i++) {
      String name;
      if (representation.length() > 0) {
        char separator = separatorsForType.get(i);
        name = lastIndexOf(representation, separator, entityTypesForType.get(i), parameters);
      } else {
        // There's no definition for the current segment use default values
        name = resolveDefaultValue(entityTypesForType.get(i), parameters);
      }

      if (name != null) {
        EntityReference newReference = new EntityReference(name, entityTypesForType.get(i));
        if (lastReference != null) {
          lastReference.setParent(newReference);
        }
        lastReference = newReference;
        if (reference == null) {
          reference = lastReference;
        }
      }
    }

    // Handle last entity reference's name
    String name;
    if (representation.length() > 0) {
      name = representation.toString();
    } else {
      name = resolveDefaultValue(entityTypesForType.get(separatorsForType.size()), parameters);
    }

    if (name != null) {
      EntityReference newReference = new EntityReference(name,
          entityTypesForType.get(separatorsForType.size()));
      if (lastReference != null) {
        lastReference.setParent(newReference);
      }
      if (reference == null) {
        reference = newReference;
      }
    }

    return reference;
  }

  private String lastIndexOf(StringBuilder representation, char separator, EntityType entityType,
      Object... parameters) {
    String name = null;

    // Search all characters for a non escaped separator. If found, then consider the part after the
    // character as the reference name and continue parsing the part before the separator.
    boolean found = false;
    for (int j = representation.length() - 1; j > -1; j--) {
      char currentChar = representation.charAt(j);
      char nextChar = 0;
      if (j > 0) {
        nextChar = representation.charAt(j - 1);
      }
      if ((nextChar != '\\') && (currentChar == separator)) {
        // Found a valid separator (not escaped), separate content on its left from content on its
        // right
        if (j == (representation.length() - 1)) {
          name = resolveDefaultValue(entityType, parameters);
        } else {
          name = representation.substring(j + 1, representation.length());
        }
        representation.delete(j, representation.length());
        found = true;
        break;
      } else if (nextChar == '\\') {
        // Unescape the character
        representation.delete(j - 1, j);
        j--;
      }
    }

    // If not found then the full buffer is the current reference segment
    if (!found) {
      name = representation.toString();
      representation.setLength(0);
    }

    return name;
  }

  private String resolveDefaultValue(EntityType type, Object... parameters) {
    String resolvedDefaultValue = null;
    if ((parameters.length > 0) && (parameters[0] instanceof EntityReference)) {
      // Try to extract the type from the passed parameter.
      EntityReference referenceParameter = (EntityReference) parameters[0];
      EntityReference extractedReference = referenceParameter.extractReference(type);
      if (extractedReference != null) {
        resolvedDefaultValue = extractedReference.getName();
      }
    }

    if (resolvedDefaultValue == null) {
      resolvedDefaultValue = getDefaultValue(type, parameters);
    }

    return resolvedDefaultValue;
  }
}
