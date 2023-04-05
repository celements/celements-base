package com.celements.spring.context;

import java.io.IOException;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.BeansException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.xwiki.component.annotation.ComponentAnnotationLoader;

import com.celements.spring.CelSpringConfig;
import com.google.common.collect.ImmutableList;

public class CelSpringContext extends AnnotationConfigApplicationContext {

  public CelSpringContext() {
    this(ImmutableList.of());
  }

  public CelSpringContext(@NotNull List<Class<?>> additionalConfigs) {
    super(new XWikiShimBeanFactory());
    setBeanNameGenerator(new FullyQualifiedAnnotationBeanNameGenerator());
    register(ImmutableList.<Class<?>>builder()
        .add(CelSpringConfig.class)
        .addAll(additionalConfigs)
        .build()
        .toArray(new Class[0]));
    registerXWiki();
    refresh();
  }

  protected final void registerXWiki() {
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
