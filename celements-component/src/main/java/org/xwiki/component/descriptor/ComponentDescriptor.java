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

import static com.google.common.base.Preconditions.*;
import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.*;

import java.util.Collection;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Represent a component.
 *
 * @version $Id$
 * @since 1.7M1
 */
public interface ComponentDescriptor<T> extends ComponentRole<T> {

  Class<? extends T> getImplementation();

  ComponentInstantiationStrategy getInstantiationStrategy();

  Collection<ComponentDependency<?>> getComponentDependencies();

  default BeanDefinition asBeanDefinition() {
    return BeanDefinitionBuilder
        .genericBeanDefinition(checkNotNull(getImplementation()))
        .setLazyInit(true) // xwiki components are always lazy
        .setPrimary(isDefault()) // default components are primary beans
        .setScope(getInstantiationStrategy() == PER_LOOKUP
            ? BeanDefinition.SCOPE_PROTOTYPE
            : BeanDefinition.SCOPE_SINGLETON)
        .getBeanDefinition();
  }

}
