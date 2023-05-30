package org.xwiki.component.spring;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;

@Configuration
@ComponentScan(basePackages = {
    "org.xwiki",
    "com.xpn.xwiki" })
public class XWikiSpringConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiSpringConfig.class);

  public final void registerXWikiComponents(
      ResourceLoader resourceLoader,
      BeanDefinitionRegistry beanFactory)
      throws ApplicationContextException {
    try {
      new ComponentAnnotationLoader()
          .loadDeclaredDescriptors(resourceLoader.getClassLoader())
          .stream()
          .forEach(descriptor -> register(beanFactory, descriptor));
    } catch (ClassNotFoundException | IOException exc) {
      throw new ApplicationContextException("failed to scan XWiki components", exc);
    }
  }

  private void register(BeanDefinitionRegistry beanFactory, ComponentDescriptor<?> descriptor) {
    BeanDefinition beanDef = descriptor.asBeanDefinition();
    LOGGER.debug("registerXWikiComponents: xwiki component {} as bean {}", descriptor, beanDef);
    beanFactory.registerBeanDefinition(descriptor.getBeanName(), beanDef);
  }

}
