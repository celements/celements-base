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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Specialized version of {@link org.xwiki.model.reference.EntityReferenceResolver} which can be
 * considered a helper
 * component to resolve {@link DocumentReference} objects from their string representation. The
 * behavior is the one
 * defined in {@link org.xwiki.model.internal.reference.ExplicitStringEntityReferenceResolver}.
 *
 * @version $Id: 04e0dee9ef496b4a8a64d98f6a4452dd3d96d154 $
 * @since 2.2.3
 */
@Component("explicit/reference")
@Singleton
public class ExplicitReferenceDocumentReferenceResolver
    implements DocumentReferenceResolver<EntityReference> {

  /**
   * Default entity reference resolver used for resolution.
   */
  @Requirement("explicit/reference")
  private EntityReferenceResolver<EntityReference> entityReferenceResolver;

  @Override
  public DocumentReference resolve(EntityReference documentReferenceRepresentation,
      Object... parameters) {
    return new DocumentReference(this.entityReferenceResolver.resolve(
        documentReferenceRepresentation, EntityType.DOCUMENT, parameters));
  }
}
