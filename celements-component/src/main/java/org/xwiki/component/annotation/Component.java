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
package org.xwiki.component.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Defines a component implementation. A hint can be specified to differentiate this implementation
 * from another one.
 *
 * @version $Id$
 * @since 1.8.1
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
@org.springframework.stereotype.Component // TODO working?
public @interface Component {

  /**
   * The hint value. Can be any text value. It's used to differentiate one component implementation
   * from another one.
   */
  // TODO AliasFor not working :(
  // @AliasFor(annotation = org.springframework.stereotype.Component.class, attribute = "value")
  String value() default "";

  /**
   * Sometimes you want to register the component several times with different hints. In this case
   * the default annotation value is ignored and the passed list of hints is used instead.
   */
  String[] hints() default {};

  /**
   * When specified forces the component to be registered as many times as there are roles
   * specified. Otherwise the superclass/interface hierarchy is scanned for ComponentRole
   * annotations and the component is registered under all roles found.
   */
  Class<?>[] roles() default {};
}