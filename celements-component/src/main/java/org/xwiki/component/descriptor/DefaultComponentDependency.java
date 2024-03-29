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

import java.util.Arrays;

/**
 * Default implementation of {@link ComponentDependency}.
 *
 * @version $Id$
 * @since 1.7M1
 */
public class DefaultComponentDependency<T> extends DefaultComponentRole<T>
    implements ComponentDependency<T> {

  private Class<?> mappingType;

  private String name;

  private String[] hints;

  @Override
  public Class<?> getMappingType() {
    return this.mappingType;
  }

  @Override
  public String getName() {
    return this.name;
  }

  public void setMappingType(Class<?> mappingType) {
    this.mappingType = mappingType;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setHints(String[] hints) {
    this.hints = hints;
  }

  @Override
  public String[] getHints() {
    return this.hints;
  }

  @Override
  public String toString() {
    return "ComponentDependency ["
        + "name=" + name
        + ", hints=" + Arrays.toString(hints)
        + ", mappingType=" + mappingType
        + "]";
  }
}
