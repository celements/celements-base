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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Generate a different representation of an Entity Reference (eg as a String).
 *
 * @param <T>
 *          the type of the new representation
 * @version $Id: 67af7e838e4db97a8a8a2a127200674d35be5222 $
 * @since 2.2M1
 */
@ComponentRole
public interface EntityReferenceSerializer<T> {

  /**
   * Serialize an entity reference into a new representation of type <T>.
   *
   * @param reference
   *          the reference to serialize
   * @param parameters
   *          optional parameters. Their meaning depends on the serializer implementation
   * @return the new representation (eg as a String)
   */
  T serialize(EntityReference reference, Object... parameters);
}
