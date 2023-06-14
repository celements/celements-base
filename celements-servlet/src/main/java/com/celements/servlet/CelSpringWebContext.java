package com.celements.servlet;

import static org.xwiki.component.spring.XWikiSpringConfig.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.xwiki.component.spring.XWikiSpringConfig;

import com.celements.spring.CelSpringConfig;
import com.celements.spring.context.XWikiShimBeanFactory;
import com.google.common.collect.ImmutableList;

public class CelSpringWebContext extends AnnotationConfigWebApplicationContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelSpringWebContext.class);

  final AtomicReference<Exception> firstClosingStackTrace = new AtomicReference<>();

  public CelSpringWebContext() {
    this(ImmutableList.of(
        XWikiSpringConfig.class,
        CelSpringConfig.class));
  }

  public CelSpringWebContext(@NotNull List<Class<?>> configs) {
    this(new FullyQualifiedAnnotationBeanNameGenerator(), configs);
  }

  public CelSpringWebContext(
      @NotNull BeanNameGenerator beanNameGenerator,
      @NotNull List<Class<?>> configs) {
    super();
    setBeanNameGenerator(beanNameGenerator);
    register(configs.toArray(new Class[configs.size()]));
    LOGGER.info("initializing configs: {}", configs);
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    return new XWikiShimBeanFactory();
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws BeansException {
    super.loadBeanDefinitions(beanFactory);
    loadXWikiDescriptors(this).forEach(descriptor -> {
      BeanDefinition beanDef = descriptor.asBeanDefinition();
      LOGGER.debug("loadBeanDefinitions: xwiki {} as {}", descriptor, beanDef);
      beanFactory.registerBeanDefinition(descriptor.getBeanName(), beanDef);
    });
  }

  @Override
  public void close() {
    // let's remember the first closing stacktrace to debug early closing
    firstClosingStackTrace.compareAndSet(null, new Exception());
    super.close();
  }

}
