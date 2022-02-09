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
package com.xpn.xwiki.internal.model.reference;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Generate an entity reference string that doesn't contain reference parts that are the same as
 * either the current entity in the execution context or as the passed entity reference (if any).
 * Note that the terminal part is always kept (eg the document's page for a document reference or
 * the attachment's filename for an attachment reference).
 *
 * @version $Id: 694e99fbf3283bf1d5e5b7028123702fee532ada $
 * @since 2.2M1
 */
@Component("compact")
@Singleton
public class CompactStringEntityReferenceSerializer extends DefaultStringEntityReferenceSerializer {

  @Requirement("current")
  private EntityReferenceValueProvider provider;

  @Override
  protected void serializeEntityReference(EntityReference currentReference,
      StringBuilder representation, boolean isLastReference, Object... parameters) {
    boolean shouldPrint = false;
    // Only serialize if:
    // - the current entity reference has a different value than the passed reference
    // - the entity type being serialized is not the last type of the chain
    // In addition an entity reference isn't printed only if all parent references are not printed
    // either, otherwise print it. For example "wiki:page" isn't allowed for a Document Reference.
    if (isLastReference || (representation.length() > 0)) {
      shouldPrint = true;
    } else {
      String defaultName = resolveDefaultValue(currentReference.getType(), parameters);
      if ((defaultName == null) || !defaultName.equals(currentReference.getName())) {
        shouldPrint = true;
      }
    }
    if (shouldPrint) {
      super.serializeEntityReference(currentReference, representation, isLastReference);
    }
  }

  protected String resolveDefaultValue(EntityType type, Object... parameters) {
    String resolvedDefaultValue = null;
    if ((parameters.length > 0) && (parameters[0] instanceof EntityReference)) {
      // Try to extract the type from the passed parameter.
      EntityReference referenceParameter = (EntityReference) parameters[0];
      resolvedDefaultValue = referenceParameter.extractRef(type)
          .map(EntityReference::getName)
          .orElse(resolvedDefaultValue);
    }
    if (resolvedDefaultValue == null) {
      resolvedDefaultValue = provider.getDefaultValue(type);
    }
    return resolvedDefaultValue;
  }
}
