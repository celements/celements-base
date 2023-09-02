package com.celements.spring.context;

import static com.celements.common.MoreObjectsCel.*;
import static com.google.common.base.Predicates.*;
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
import org.springframework.context.ConfigurableApplicationContext;
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

import com.celements.common.MoreOptional;
import com.google.common.base.Strings;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

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

  private final ConfigurableApplicationContext springContext;
  private final XWikiShimBeanFactory beanFactory;
  private final ComponentDescriptorFactory descriptorFactory;

  @Inject
  public SpringShimComponentManager(
      ConfigurableApplicationContext springContext,
      XWikiShimBeanFactory beanFactory) {
    this.springContext = springContext;
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
        .anyMatch(name -> name.equals(hint) || name.equals(compRole.getBeanName()))
        || role.isInstance(springContext); // context isn't in beanFactory
  }

  @Override
  public <T> T lookup(Class<T> role) throws ComponentLookupException {
    return lookup(role, null);
  }

  @Override
  public <T> T lookup(Class<T> role, String hint) throws ComponentLookupException {
    ComponentRole<T> compRole = new DefaultComponentRole<>(role, hint);
    try {
      return getBean(compRole)
          .or(() -> tryCast(springContext, role)) // context isn't in beanFactory
          .orElseThrow(() -> new NoSuchBeanDefinitionException(compRole.getBeanName()));
    } catch (BeansException exc) {
      throw new ComponentLookupException("lookup - [" + compRole + "] failed", exc);
    }
  }

  private <T> Optional<T> getBean(ComponentRole<T> compRole) {
    return Stream.of(compRole.getBeanName(), Optional.ofNullable(compRole.getRoleHint())
        .filter(not(ComponentRole.DEFAULT_HINT::equals)).orElse(""))
        .map(name -> getBean(name, compRole.getRole()))
        .flatMap(MoreOptional::stream)
        .findFirst();
  }

  private <T> Optional<T> getBean(String name, Class<T> type) {
    try {
      return (Strings.isNullOrEmpty(name))
          ? Optional.of(beanFactory.getBean(type))
          : Optional.of(beanFactory.getBean(name, type));
    } catch (NoSuchBeanDefinitionException exc) {
      return Optional.empty();
    }
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
          .filterKeys(compRole -> compRole.getRole() == role)
          .ifEmpty(StreamEx.of(tryCast(springContext, role)) // context isn't in beanFactory
              .mapToEntry(ctx -> new DefaultComponentRole<>(role), ctx -> ctx));
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
        Stream.of(beanFactory.getBeanNamesForType(component.getClass()))
            .forEach(beanFactory::destroySingleton);
      } catch (BeansException exc) {
        throw new ComponentLifecycleException("release - failed for class ["
            + component.getClass() + "]", exc);
      }
    }
  }

  @Override
  public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String hint) {
    ComponentRole<T> compRole = new DefaultComponentRole<>(role, hint);
    return Optional.ofNullable(createComponentDescriptor(role, compRole.getBeanName()))
        .orElseGet(() -> createComponentDescriptor(role, hint));
  }

  @Override
  public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role) {
    return Stream.of(beanFactory.getBeanDefinitionNames())
        .map(beanName -> createComponentDescriptor(role, beanName))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  private <T> ComponentDescriptor<T> createComponentDescriptor(Class<T> role, String beanName) {
    try {
      String beanClassName = beanFactory.getBeanDefinition(beanName).getBeanClassName();
      Class<? extends T> instanceClass = classForName(role, beanClassName);
      if (instanceClass != null) {
        String hint = ComponentRole.fromBeanName(beanName)
            .map(ComponentRole::getRoleHint)
            .orElse(beanName);
        return descriptorFactory.create(instanceClass, role, hint);
      }
    } catch (NoSuchBeanDefinitionException | ClassNotFoundException exc) {
      LOGGER.error("createComponentDescriptor - failed for [{}]", beanName, exc);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private <T> Class<? extends T> classForName(Class<T> parentClass, String className)
      throws ClassNotFoundException {
    if (className != null) {
      Class<?> ret = Class.forName(className);
      if ((parentClass == null) || parentClass.isAssignableFrom(ret)) {
        return (Class<? extends T>) ret;
      }
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
