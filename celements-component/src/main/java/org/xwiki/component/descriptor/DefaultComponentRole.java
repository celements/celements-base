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
package org.xwiki.component.descriptor;

import static com.google.common.base.Strings.*;

import java.util.Objects;

public class DefaultComponentRole<T> implements ComponentRole<T> {

  public static final String HINT = ComponentRole.DEFAULT_HINT;

  private Class<T> role;

  private String roleHint = DEFAULT_HINT;

  @Deprecated
  public DefaultComponentRole() {}

  public DefaultComponentRole(Class<T> role, String hint) {
    setRole(role);
    setRoleHint(hint);
  }

  public void setRole(Class<T> role) {
    this.role = role;
  }

  @Override
  public Class<T> getRole() {
    return this.role;
  }

  public void setRoleHint(String roleHint) {
    this.roleHint = roleHint;
  }

  @Override
  public String getRoleHint() {
    return !isNullOrEmpty(roleHint) ? roleHint : ComponentRole.DEFAULT_HINT;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRole(), getRoleHint());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    } else if (!(obj instanceof ComponentRole)) {
      return false;
    } else {
      ComponentRole<?> other = (ComponentRole<?>) obj;
      return Objects.equals(this.getRole(), other.getRole())
          && Objects.equals(this.getRoleHint(), other.getRoleHint());
    }
  }

  @Override
  public String toString() {
    return "role = [" + getRole().getName() + "]"
        + " hint = [" + getRoleHint() + "]";
  }
}
