package com.celements.spring;

import java.io.IOException;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.xwiki.component.annotation.ComponentAnnotationLoader;

import com.celements.spring.component.CelementsBeanFactory;

public class CelementsAnnotationConfigApplicationContext
    extends AnnotationConfigApplicationContext {

  // TODO CelementsServletContextListener

  public CelementsAnnotationConfigApplicationContext(String... basePackages) {
    super(new CelementsBeanFactory());
    setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
    scan(basePackages); // spring components
    scanXWiki(); // xwiki components
    refresh();
  }

  private void scanXWiki() {
    try {
      ClassLoader classLoader = this.getClass().getClassLoader();
      new ComponentAnnotationLoader().loadDeclaredDescriptors(classLoader)
          .forEach(descriptor -> registerBeanDefinition(
              descriptor.getBeanName(),
              descriptor.asBeanDefinition()));
    } catch (ClassNotFoundException | IOException | BeansException exc) {
      throw new IllegalStateException("failed to scan XWiki components", exc);
    }
  }
}
