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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.ObjectReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be
 * considered a helper
 * component to resolve {@link ObjectReference} objects from their string representation. This
 * implementation uses fixed
 * default values when parts of the Reference are missing in the string representation. Default
 * values are retrieved
 * from the {@link org.xwiki.model.ModelConfiguration} class.
 *
 * @version $Id: ec836349fbd6f78d5a5211c2cdc41218ea65b106 $
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultStringObjectReferenceResolver implements ObjectReferenceResolver<String> {

  /**
   * The default entity resolver, used to delegate actual resolving of string representations.
   */
  @Requirement
  private EntityReferenceResolver<String> entityReferenceResolver;

  @Override
  public ObjectReference resolve(String objectReferenceRepresentation, Object... parameters) {
    return new ObjectReference(this.entityReferenceResolver.resolve(objectReferenceRepresentation,
        EntityType.OBJECT, parameters));
  }

  @Override
  public ObjectReference resolve(String objectReferenceRepresentation) {
    return new ObjectReference(this.entityReferenceResolver.resolve(objectReferenceRepresentation,
        EntityType.OBJECT));
  }
}
