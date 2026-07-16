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

import static org.easymock.EasyMock.*;

import java.util.Set;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;

/**
 * Unit tests for {@link ComponentAnnotationLoader}.
 *
 * @version $Id$
 * @since 1.8.1
 */
public class ComponentAnnotationLoaderTest {

  @ComponentRole
  public interface Role {}

  @ComponentRole
  public interface ExtendedRole extends Role {}

  @Component
  public class RoleImpl implements ExtendedRole {}

  /**
   * Note that even though it's not necessary we implement Role to ensure that the SuperRoleImpl
   * component will only be registered once.
   */
  @Component
  public class SuperRoleImpl extends RoleImpl implements Role {}

  @Component("test")
  public class SimpleRole implements Role {}

  @Component("test")
  public class OverrideRole implements Role {}

  @Test
  public void testFindComponentRoleClasses() {
    assertComponentRoleClasses(RoleImpl.class);
  }

  /**
   * Verify that when there are several component implementations for the same role/hint then
   * component implementations defined in META-INF/component-overrides.txt are used in priority.
   */
  @Test
  public void testOverrides() throws Exception {
    ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
    ComponentManager mockManager = createMock(ComponentManager.class);
    Capture<ComponentDescriptor> descriptors = newCapture(CaptureType.ALL);
    mockManager.registerComponent(capture(descriptors));
    expectLastCall().anyTimes();
    replay(mockManager);

    loader.initialize(mockManager, this.getClass().getClassLoader());

    verify(mockManager);
    Assert.assertFalse(descriptors.getValues().stream()
        .anyMatch(descriptor -> descriptor.getImplementation().equals(SimpleRole.class)));
  }

  /**
   * Verify that we get the same result when we use a class that extends another class (i.e.
   * inheritance works).
   */
  @Test
  public void testFindComponentRoleClasseWhenClassExtension() {
    assertComponentRoleClasses(SuperRoleImpl.class);
  }

  private void assertComponentRoleClasses(Class<?> componentClass) {
    ComponentAnnotationLoader loader = new ComponentAnnotationLoader();
    Set<Class<?>> classes = loader.findComponentRoleClasses(componentClass);
    Assert.assertEquals(2, classes.size());
    Assert.assertTrue(classes.contains(Role.class));
    Assert.assertTrue(classes.contains(ExtendedRole.class));
  }
}
