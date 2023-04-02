package com.celements.spring;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;

public class CelementsAnnotationConfigApplicationContext
    extends AnnotationConfigApplicationContext {

  public CelementsAnnotationConfigApplicationContext(String... basePackages) {
    super();
    setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
    scan(basePackages); // spring components
    scanXWiki(); // xwiki components
    refresh();
  }

  private void scanXWiki() {
    try {
      ComponentAnnotationLoader descriptorLoader = new ComponentAnnotationLoader();
      ClassLoader classLoader = this.getClass().getClassLoader();
      for (ComponentDescriptor<?> descriptor : descriptorLoader
          .loadDeclaredDescriptors(classLoader)) {
        // TODO duplicate to SpringComponentManager#registerComponent
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(descriptor.getImplementation());
        // TODO required?
        // builder.addPropertyValue("property1", "propertyValue");
        // builder.setInitMethodName("initialize"); // perhaps for initializables ?
        builder.setScope(descriptor.getBeanScope());
        String name = descriptor.getBeanName();
        System.err.println("REGISTER: " + name);
        registerBeanDefinition(name, builder.getBeanDefinition());
      }
    } catch (ClassNotFoundException | IOException | BeansException exc) {
      throw new RuntimeException("failed to initialise component manager", exc);
    }
  }
}
