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
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * The behavior is the same as for {@link CurrentEntityReferenceValueProvider} but with the
 * following differences:
 * <ul>
 * <li>if the passed reference doesn't have a page name specified (or if it's empty) the value used
 * is the default page name (instead of the page name of the current document's reference).</li>
 * </ul>
 *
 * @version $Id: 42c2cf3fdf8b6fdee819c145a4306cc989b452ff $
 * @since 2.3M1
 */
@Component("currentmixed")
@Singleton
public class CurrentMixedEntityReferenceValueProvider extends CurrentEntityReferenceValueProvider {

  @Requirement
  private EntityReferenceValueProvider defaultProvider;

  @Override
  public String getDefaultValue(EntityType type) {
    String result;
    if (type == EntityType.DOCUMENT) {
      result = this.defaultProvider.getDefaultValue(EntityType.DOCUMENT);
    } else {
      result = super.getDefaultValue(type);
    }
    return result;
  }
}
