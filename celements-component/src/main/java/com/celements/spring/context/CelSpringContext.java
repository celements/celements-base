package com.celements.spring.context;

import static com.celements.common.MoreObjectsCel.*;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.xwiki.component.spring.XWikiSpringConfig;

import com.celements.spring.CelSpringConfig;
import com.google.common.collect.ImmutableList;

/**
 * Extension of the {@link AnnotationConfigApplicationContext} ensuring backwards compatibility with
 * XWiki components by registering not only {@link org.springframework.stereotype.Component} but
 * also {@link org.xwiki.component.annotation.Component}, see {@link #registerXWiki()}.
 */
public class CelSpringContext extends AnnotationConfigApplicationContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelSpringContext.class);

  public CelSpringContext() {
    this(ImmutableList.of(
        XWikiSpringConfig.class,
        CelSpringConfig.class));
  }

  public CelSpringContext(@NotNull List<Class<?>> configs) {
    this(new FullyQualifiedAnnotationBeanNameGenerator(), configs);
  }

  public CelSpringContext(
      @NotNull BeanNameGenerator beanNameGenerator,
      @NotNull List<Class<?>> configs) {
    super(new XWikiShimBeanFactory());
    setBeanNameGenerator(beanNameGenerator);
    if (!configs.isEmpty()) {
      register(configs.toArray(new Class[configs.size()]));
    }
    configs.stream().flatMap(tryCast(XWikiSpringConfig.class)).findFirst()
        .ifPresent(cfg -> cfg.registerXWikiComponents(this, this));
    LOGGER.info("initializing configs: {}", configs);
  }
}
