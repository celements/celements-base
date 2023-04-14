package com.celements.spring.context;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Service;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

import one.util.streamex.EntryStream;

/**
 * Shim implementation of the XWiki {@link ComponentManager} that converts all calls to
 * the Spring {@link BeanFactory}. Since XWiki components are identified by Role+Hint they are
 * registered with the {@link ComponentRole#getBeanName()} as
 * {@link ComponentDescriptor#asBeanDefinition()}.
 */
@Service(SpringShimComponentManager.NAME)
public class SpringShimComponentManager implements ComponentManager {

  public static final String NAME = "springify";

  private static final Logger LOGGER = LoggerFactory.getLogger(SpringShimComponentManager.class);

  private final XWikiShimBeanFactory beanFactory;
  private final ComponentDescriptorFactory descriptorFactory;

  @Inject
  public SpringShimComponentManager(XWikiShimBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.descriptorFactory = new ComponentDescriptorFactory();
  }

  @Override
  public <T> boolean hasComponent(Class<T> role) {
    return hasComponent(role, null);
  }

  @Override
  public <T> boolean hasComponent(Class<T> role, String hint) {
    ComponentRole<?> compRole = new DefaultComponentRole<>(role, hint);
    return Stream.of(beanFactory.getBeanNamesForType(role))
        .anyMatch(name -> name.equals(hint) || name.equals(compRole.getBeanName()));
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    return lookup(role, null);
  }

  @Override
  public <T> T lookup(Class<T> role, String hint) throws ComponentLookupException {
    ComponentRole<T> compRole = new DefaultComponentRole<>(role, hint);
    try {
      return getBean(compRole.getBeanName(), role)
          .map(Optional::of)
          .orElseGet(() -> getBean(hint, role))
          .orElseThrow(() -> new ComponentLookupException("lookup - [" + compRole + "] failed"));
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookup - [" + compRole + "] failed", exc);
    }
  }

  private <T> Optional<T> getBean(String name, Class<T> type) {
    if (name != null) {
      try {
        return Optional.of(beanFactory.getBean(name, type));
      } catch (NoSuchBeanDefinitionException exc) {}
    }
    return Optional.empty();
  }

  @Override
  public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException {
    return lookupEntries(role)
        .mapKeys(ComponentRole::getRoleHint)
        .toMap();
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    return lookupEntries(role)
        .values()
        .toList();
  }

  private <T> EntryStream<ComponentRole<T>, T> lookupEntries(Class<T> role)
      throws ComponentLookupException {
    try {
      return EntryStream.of(beanFactory.getBeansOfType(role))
          .mapKeys(beanName -> ComponentRole.<T>fromBeanName(beanName)
              .orElseGet(() -> new DefaultComponentRole<>(role, beanName)))
          .filterKeys(compRole -> compRole.getRole() == role);
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookupEntries - failed for [" + role + "]", exc);
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
    try {
      String beanName = descriptor.getBeanName();
      beanFactory.registerBeanDefinition(beanName, descriptor.asBeanDefinition());
      if (component != null) {
        beanFactory.registerSingleton(beanName, component);
      }
    } catch (BeansException exc) {
      throw new ComponentRepositoryException("registerComponent - failed for [" + descriptor + "]",
          exc);
    }
  }

  @Override
  public void unregisterComponent(Class<?> role, String hint) {
    ComponentRole<?> compRole = new DefaultComponentRole<>(role, hint);
    try {
      beanFactory.removeBeanDefinition(compRole.getBeanName());
    } catch (NoSuchBeanDefinitionException exc) {
      LOGGER.debug("unregisterComponent - component [{}], not registered", compRole);
    }
  }

  @Override
  public <T> void release(T component) throws ComponentLifecycleException {
    if (component != null) {
      try {
        beanFactory.destroyBean(component);
      } catch (BeansException exc) {
        throw new ComponentLifecycleException("release - failed for class ["
            + component.getClass() + "]", exc);
      }
    }
  }

  @Override
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String hint) {
    ComponentRole<T> compRole = new DefaultComponentRole<>(role, hint);
    return createComponentDescriptor(compRole, getBean(hint, role).orElse(null));
  }

  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    Stream<ComponentDescriptor<T>> ret = Stream.empty();
    try {
      ret = lookupEntries(role).mapKeyValue(this::createComponentDescriptor);
    } catch (ComponentLookupException cle) {
      LOGGER.error("getComponentDescriptorList - failed for [{}]", role, cle);
    }
    return ret.filter(Objects::nonNull).collect(toList());
  }

  @SuppressWarnings("unchecked")
  private <T> ComponentDescriptor<T> createComponentDescriptor(ComponentRole<T> compRole,
      Object instance) {
    if (instance != null) {
      Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
      return descriptorFactory.create(instanceClass, compRole.getRole(), compRole.getRoleHint());
    }
    return null;
  }

  @Deprecated
  @Override
  public ComponentEventManager getComponentEventManager() {
    return null;
  }

  @Deprecated
  @Override
  public void setComponentEventManager(ComponentEventManager eventManager) {
    // not supported
  }

  @Override
  public ComponentManager getParent() {
    return null;
  }

  @Override
  public void setParent(ComponentManager parentComponentManager) {
    if (parentComponentManager != null) {
      throw new UnsupportedOperationException();
    }
  }

}
