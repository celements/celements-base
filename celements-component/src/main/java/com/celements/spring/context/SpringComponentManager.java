package com.celements.spring.context;

import static com.celements.spring.context.CelementsBeanFactory.*;
import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentManagerInitializer;
import org.xwiki.component.manager.ComponentRepositoryException;

import com.google.common.collect.ImmutableList;

import one.util.streamex.EntryStream;

@Service(SpringComponentManager.NAME)
public class SpringComponentManager implements ComponentManager {

  public static final String NAME = "springify";

  private static final Logger LOGGER = LoggerFactory.getLogger(RequirementBeanPostProcessor.class);

  private final GenericApplicationContext springContext;
  private final ComponentDescriptorFactory descriptorFactory;

  private ComponentEventManager eventManager;

  @Inject
  public SpringComponentManager(GenericApplicationContext context) {
    springContext = context;
    descriptorFactory = new ComponentDescriptorFactory();
  }

  @PostConstruct
  public void init() {
    // Extension point to allow component to manipulate ComponentManager initialized state.
    springContext.getBeansOfType(ComponentManagerInitializer.class)
        .values().stream()
        .forEach(initializer -> initializer.initialize(this));
  }

  @Override
  public <T> boolean hasComponent(Class<T> role) {
    return hasComponent(role, null);
  }

  @Override
  public <T> boolean hasComponent(Class<T> role, String hint) {
    String beanName = uniqueBeanName(role, hint);
    Map<String, T> beans = springContext.getBeansOfType(role);
    return beans.containsKey(beanName) || beans.containsKey(hint);
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    return lookup(role, null);
  }

  @Override
  public <T> T lookup(Class<T> role, String hint) throws ComponentLookupException {
    String beanName = uniqueBeanName(role, hint);
    try {
      return getBean(beanName, role)
          .map(Optional::of)
          .orElseGet(() -> getBean(hint, role))
          .orElseThrow(() -> new ComponentLookupException("lookup - [" + beanName + "] failed"));
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookup - [" + beanName + "] failed", exc);
    }
  }

  private <T> Optional<T> getBean(String name, Class<T> type) {
    try {
      return Optional.of(springContext.getBean(name, type));
    } catch (NoSuchBeanDefinitionException exc) {
      return Optional.empty();
    }
  }

  @Override
  public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException {
    try {
      return EntryStream.of(springContext.getBeansOfType(role))
          .mapKeys(CelementsBeanFactory::getHintFromBeanName)
          .toImmutableMap();
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookupMap - failed for [" + role + "]", exc);
    }
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    try {
      return ImmutableList.copyOf(springContext.getBeansOfType(role).values());
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookupList - failed for [" + role + "]", exc);
    }
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor)
      throws ComponentRepositoryException {
    registerComponent(descriptor, null);
  }

  @Override
  public <T> void registerComponent(ComponentDescriptor<T> descriptor, T component)
      throws ComponentRepositoryException {
    String beanName = descriptor.getBeanName();
    try {
      // TODO test
      if (component != null) {
        // according to method contract, if an instance is provided it should never be created from
        // the descriptor, irrespective of the instantiation strategy. the component must be fully
        // initialized already.
        springContext.getBeanFactory().registerSingleton(beanName, component);
      } else {
        springContext.registerBeanDefinition(beanName, descriptor.asBeanDefinition());
      }
      getEventManager().ifPresent(em -> em.notifyComponentRegistered(descriptor));
    } catch (BeansException exc) {
      throw new ComponentRepositoryException("registerComponent - failed for [" + descriptor + "]",
          exc);
    }
  }

  @Override
  public void unregisterComponent(Class<?> role, String hint) {
    try {
      ComponentDescriptor<?> descriptor = getComponentDescriptor(role, hint);
      springContext.removeBeanDefinition(uniqueBeanName(role, hint));
      getEventManager().ifPresent(em -> em.notifyComponentUnregistered(descriptor));
    } catch (NoSuchBeanDefinitionException exc) {
      LOGGER.debug("unregisterComponent - component [{}], not registered",
          uniqueBeanName(role, hint));
    }
  }

  @Override
  public <T> void release(T component) throws ComponentLifecycleException {
    if (component != null) {
      try {
        // TODO is this sufficient ? (test with&without bean definition)
        springContext.getBeanFactory().destroyBean(component);
      } catch (BeansException exc) {
        throw new ComponentLifecycleException("release - failed for class ["
            + component.getClass() + "]", exc);
      }
    }
  }

  @Override
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String hint) {
    String beanName = uniqueBeanName(role, hint);
    return createComponentDescriptor(role, beanName, springContext.getBean(beanName));
  }

  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    return EntryStream.of(springContext.getBeansOfType(role))
        .mapKeyValue((name, instance) -> createComponentDescriptor(role, name, instance))
        .collect(toList());
  }

  @SuppressWarnings("unchecked")
  public <T> ComponentDescriptor<T> createComponentDescriptor(Class<T> role, String beanName,
      Object instance) {
    if (instance != null) {
      Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
      return descriptorFactory.create(instanceClass, role, getHintFromBeanName(beanName));
    }
    return null;
  }

  @Override
  public ComponentEventManager getComponentEventManager() {
    return eventManager;
  }

  public Optional<ComponentEventManager> getEventManager() {
    return Optional.ofNullable(eventManager);
  }

  @Override
  public void setComponentEventManager(ComponentEventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public ComponentManager getParent() {
    return null;
  }

  @Override
  public void setParent(ComponentManager parentComponentManager) {
    if (parentComponentManager != null) {
      throw new UnsupportedOperationException("parent component manager not supported");
    }
  }

}
