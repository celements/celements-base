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
 *
 */
package org.xwiki.component.descriptor;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Splitter;

public interface ComponentRole<T> {

  String DEFAULT_HINT = "default";
  String BEAN_NAME_SEPARATOR = "|||";

  Class<T> getRole();

  String getRoleHint();

  default boolean isDefault() {
    return DEFAULT_HINT.equals(getRoleHint());
  }

  default String getBeanName() {
    return getRole().getName() + BEAN_NAME_SEPARATOR + getRoleHint();
  }

  @SuppressWarnings("unchecked")
  static <T> Optional<ComponentRole<T>> fromBeanName(String beanName) {
    try {
      List<String> parts = Splitter.on(ComponentRole.BEAN_NAME_SEPARATOR).omitEmptyStrings()
          .splitToList(beanName);
      if (parts.size() > 1) {
        Class<T> role = (Class<T>) Class.forName(parts.get(0));
        return Optional.of(new DefaultComponentRole<>(role, parts.get(1)));
      }
      return Optional.empty();
    } catch (ClassNotFoundException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

}
