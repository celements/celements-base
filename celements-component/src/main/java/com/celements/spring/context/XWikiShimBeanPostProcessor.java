package com.celements.spring.context;

import static com.celements.common.MoreObjectsCel.*;
import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;

@Component
public class XWikiShimBeanPostProcessor implements BeanPostProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(XWikiShimBeanPostProcessor.class);

  private final ComponentManager componentManager;
  private final ComponentDescriptorFactory descriptorFactory;

  @Inject
  public XWikiShimBeanPostProcessor(ComponentManager componentManager) {
    this.componentManager = componentManager;
    this.descriptorFactory = new ComponentDescriptorFactory();
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    descriptorFactory.createComponentDependencies(bean.getClass())
        .forEach(dependency -> injectXWikiDependency(dependency, bean));
    return bean;
  }

  private void injectXWikiDependency(ComponentDependency<Object> dependency, Object bean) {
    try {
      ReflectionUtils.setFieldValue(bean, dependency.getName(), getInstance(dependency));
      LOGGER.debug("injectXWikiDependency - [{}] into [{}]", dependency, bean);
    } catch (ComponentLookupException cle) {
      LOGGER.error("injectXWikiDependency - failed [{}] into [{}]" + bean, dependency, bean, cle);
    }
  }

  @NotNull
  private Object getInstance(ComponentDependency<?> dependency) throws ComponentLookupException {
    if (dependency.getMappingType() != null) {
      if (List.class.isAssignableFrom(dependency.getMappingType())) {
        return componentManager.lookupList(dependency.getRole());
      } else if (Map.class.isAssignableFrom(dependency.getMappingType())) {
        return componentManager.lookupMap(dependency.getRole());
      }
    }
    return componentManager.lookup(dependency.getRole(), dependency.getRoleHint());
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    callXWikiInitializers(bean, beanName);
    return bean;
  }

  @SuppressWarnings("deprecation")
  private void callXWikiInitializers(Object bean, String beanName) {
    try {
      tryCast(bean, Initializable.class).ifPresent(rethrow(Initializable::initialize));
    } catch (InitializationException exc) {
      throw new BeanInitializationException("failed XWiki initialization on: " + beanName, exc);
    }
  }

}
