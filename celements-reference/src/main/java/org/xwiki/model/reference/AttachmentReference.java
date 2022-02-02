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
package org.xwiki.model.reference;

import org.xwiki.model.EntityType;

/**
 * Represents a reference to an Attachment (document reference and file name). Note that an
 * attachment is always
 * attached to a document.
 *
 * @version $Id$
 * @since 2.2M1
 */
public class AttachmentReference extends EntityReference {

  /**
   * Special constructor that transforms a generic entity reference into an
   * {@link AttachmentReference}. It checks the
   * validity of the passed reference (ie correct type and correct parent).
   *
   * @exception IllegalArgumentException
   *              if the passed reference is not a valid attachment reference
   */
  public AttachmentReference(EntityReference reference) {
    super(reference.getName(), reference.getType(), reference.getParent());
  }

  public AttachmentReference(String fileName, DocumentReference parent) {
    super(fileName, EntityType.ATTACHMENT, parent);
  }

  /**
   * {@inheritDoc}
   *
   * Overridden in order to verify the validity of the passed parent
   *
   * @see org.xwiki.model.reference.EntityReference#setParent(EntityReference)
   * @exception IllegalArgumentException
   *              if the passed parent is not a valid attachment reference parent (ie an
   *              attachment reference)
   */
  @Override
  public void setParent(EntityReference parent) {
    if ((parent == null) || (parent.getType() != EntityType.DOCUMENT)) {
      throw new IllegalArgumentException(
          "Invalid parent reference [" + parent + "] for an attachment reference");
    }

    super.setParent(new DocumentReference(parent));
  }

  /**
   * {@inheritDoc}
   *
   * Overridden in order to verify the validity of the passed type
   *
   * @see org.xwiki.model.reference.EntityReference#setType(org.xwiki.model.EntityType)
   * @exception IllegalArgumentException
   *              if the passed type is not an attachment type
   */
  @Override
  public void setType(EntityType type) {
    if (type != EntityType.ATTACHMENT) {
      throw new IllegalArgumentException("Invalid type [" + type + "] for an attachment reference");
    }

    super.setType(EntityType.ATTACHMENT);
  }

  public DocumentReference getDocumentReference() {
    return (DocumentReference) extractReference(EntityType.DOCUMENT);
  }
}
