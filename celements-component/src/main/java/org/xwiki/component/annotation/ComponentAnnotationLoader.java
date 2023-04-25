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

import static java.util.stream.Collectors.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.internal.RoleHint;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * Dynamically loads all components defined using Annotations and declared in
 * META-INF/components.txt files.
 *
 * @version $Id$
 * @since 1.8.1
 */
public class ComponentAnnotationLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ComponentAnnotationLoader.class);

  /**
   * Location in the classloader of the file defining the list of component implementation class to
   * parser for annotations.
   */
  public static final String COMPONENT_LIST = "META-INF/components.txt";

  /**
   * Location in the classloader of the file specifying which component implementation to use when
   * several with the same role/hint are found.
   */
  public static final String COMPONENT_OVERRIDE_LIST = "META-INF/component-overrides.txt";

  /**
   * The encoding used to parse component list files.
   */
  private static final String COMPONENT_LIST_ENCODING = StandardCharsets.UTF_8.name();

  /**
   * Factory to create a Component Descriptor from an annotated class.
   */
  private final ComponentDescriptorFactory factory;

  /**
   * Default constructor.
   */
  public ComponentAnnotationLoader() {
    // make sure to not fail even if no one provided a logger
    factory = new ComponentDescriptorFactory();
  }

  /**
   * @deprecated since 5.2, no replacement
   */
  @Deprecated
  public void initialize(ComponentManager manager, ClassLoader classLoader) {
    try {
      for (ComponentDescriptor<?> descriptor : loadDeclaredDescriptors(classLoader)) {
        manager.registerComponent(descriptor);
      }
    } catch (IOException | ClassNotFoundException | ComponentRepositoryException exc) {
      throw new RuntimeException("Failed to dynamically load components with annotations", exc);
    }
  }

  /**
   * @deprecated since 5.2, no replacement
   */
  @Deprecated
  public void initialize(ComponentManager manager, ClassLoader classLoader,
      List<String> componentClassNames, List<String> componentOverrideClassNames) {
    try {
      for (ComponentDescriptor<?> descriptor : loadDescriptors(classLoader,
          componentClassNames, componentOverrideClassNames)) {
        manager.registerComponent(descriptor);
      }
    } catch (ClassNotFoundException | ComponentRepositoryException exc) {
      throw new RuntimeException("Failed to dynamically load components with annotations", exc);
    }
  }

  /**
   * Loads all declared components defined using annotations.
   *
   * @param classLoader
   *          the classloader to use to look for the Component list declaration file (
   *          {@code META-INF/components.txt})
   */
  public List<ComponentDescriptor<?>> loadDeclaredDescriptors(ClassLoader classLoader)
      throws IOException, ClassNotFoundException {
    List<String> componentClassNames = getDeclaredComponents(classLoader, COMPONENT_LIST);
    List<String> componentOverrideClassNames = getDeclaredComponents(classLoader,
        COMPONENT_OVERRIDE_LIST);
    return loadDescriptors(classLoader, componentClassNames, componentOverrideClassNames);
  }

  public List<ComponentDescriptor<?>> loadDescriptors(ClassLoader classLoader,
      List<String> componentClassNames, List<String> componentOverrideClassNames)
      throws ClassNotFoundException {
    // 2) For each component class name found, load its class and use introspection to find the
    // necessary annotations required to create a Component Descriptor.
    Map<RoleHint<?>, ComponentDescriptor<?>> descriptorMap = new LinkedHashMap<>();
    for (String componentClassName : componentClassNames) {
      Class<?> componentClass = classLoader.loadClass(componentClassName);
      // Look for ComponentRole annotations and register one component per ComponentRole found
      findDescriptors(componentClass).forEach(descriptor -> {
        // If there's already a existing role/hint in the list of descriptors then decide which
        // one to keep by looking at the override list. Use those in the override list in
        // priority. Otherwise use the last registered component.
        RoleHint<?> roleHint = new RoleHint<>(descriptor.getRole(), descriptor.getRoleHint());
        if (descriptorMap.containsKey(roleHint)) {
          // Is the component in the override list?
          ComponentDescriptor<?> existingDescriptor = descriptorMap.get(roleHint);
          if (!componentOverrideClassNames.contains(existingDescriptor.getImplementation()
              .getName())) {
            descriptorMap.put(new RoleHint<>(descriptor.getRole(), descriptor.getRoleHint()),
                descriptor);
            if (!componentOverrideClassNames.contains(descriptor.getImplementation().getName())) {
              LOG.warn("Component [" + existingDescriptor.getImplementation().getName()
                  + "] is being overwritten by component ["
                  + descriptor.getImplementation().getName() + "] for Role/Hint ["
                  + roleHint + "]. It will not be possible to look it up.");
            }
          }
        } else {
          descriptorMap.put(new RoleHint<>(descriptor.getRole(), descriptor.getRoleHint()),
              descriptor);
        }
      });
    }
    return new ArrayList<>(descriptorMap.values());
  }

  /**
   * @deprecated instead use {@link #streamComponentsDescriptors(Class)}
   */
  @Deprecated
  public List<ComponentDescriptor> getComponentsDescriptors(Class<?> componentClass) {
    return findDescriptors(componentClass).collect(toList());
  }

  @SuppressWarnings("unchecked")
  public Stream<ComponentDescriptor<?>> findDescriptors(Class<?> componentClass) {
    return findComponentRoleClasses(componentClass).stream()
        .flatMap(componentRoleClass -> factory.streamDescriptors(
            (Class<?>) componentClass, (Class<Object>) componentRoleClass));
  }

  /**
   * Finds the interfaces that implement component roles by looking recursively in all interfaces of
   * the passed component implementation class. If the roles annotation value is specified then use
   * the specified list instead of doing auto-discovery.
   *
   * @param componentClass
   *          the component implementation class for which to find the component roles it implements
   * @return the list of component role classes implemented
   */
  public Set<Class<?>> findComponentRoleClasses(Class<?> componentClass) {
    // Note: We use a Set to ensure that we don't register duplicate roles.
    Set<Class<?>> classes = new LinkedHashSet<>();

    Component component = componentClass.getAnnotation(Component.class);
    if ((component != null) && (component.roles().length > 0)) {
      classes.addAll(Arrays.asList(component.roles()));
    } else {
      // Look in both superclass and interfaces for @ComponentRole.
      for (Class<?> interfaceClass : componentClass.getInterfaces()) {
        classes.addAll(findComponentRoleClasses(interfaceClass));
        for (Annotation annotation : interfaceClass.getDeclaredAnnotations()) {
          if (annotation.annotationType().getName().equals(ComponentRole.class.getName())) {
            classes.add(interfaceClass);
          }
        }
      }

      // Note that we need to look into the superclass since the super class can itself implements
      // an interface that has the @ComponentRole annotation.
      Class<?> superClass = componentClass.getSuperclass();
      if ((superClass != null) && !superClass.getName().equals(Object.class.getName())) {
        classes.addAll(findComponentRoleClasses(superClass));
      }

    }

    return classes;
  }

  /**
   * Get all components listed in the passed resource file.
   *
   * @param classLoader
   *          the classloader to use to find the resources
   * @param location
   *          the name of the resources to look for
   * @return the list of component implementation class names
   * @throws IOException
   *           in case of an error loading the component list resource
   */
  private List<String> getDeclaredComponents(ClassLoader classLoader, String location)
      throws IOException {
    List<String> annotatedClassNames = new ArrayList<>();
    Enumeration<URL> urls = classLoader.getResources(location);
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();

      InputStream componentListStream = url.openStream();

      try {
        annotatedClassNames.addAll(getDeclaredComponents(componentListStream));
      } finally {
        componentListStream.close();
      }
    }

    return annotatedClassNames;
  }

  /**
   * Get all components listed in the passed resource stream.
   *
   * @param componentListStream
   *          the stream to parse
   * @return the list of component implementation class names
   * @throws IOException
   *           in case of an error loading the component list resource
   * @since 2.5M2
   */
  public List<String> getDeclaredComponents(InputStream componentListStream) throws IOException {
    List<String> annotatedClassNames = new ArrayList<>();

    // Read all components definition from the URL
    // Always force UTF-8 as the encoding, since these files are read from the official jars, and
    // those are generated on an 8-bit system.
    BufferedReader in = new BufferedReader(
        new InputStreamReader(componentListStream, COMPONENT_LIST_ENCODING));
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      // Make sure we don't add empty lines
      if (inputLine.trim().length() > 0) {
        annotatedClassNames.add(inputLine);
      }
    }

    return annotatedClassNames;
  }
}
