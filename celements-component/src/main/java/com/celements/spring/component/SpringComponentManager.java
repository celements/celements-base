package com.celements.spring.component;

import static java.util.stream.Collectors.*;
import static org.xwiki.component.descriptor.ComponentDescriptor.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentManagerInitializer;
import org.xwiki.component.manager.ComponentRepositoryException;

import com.google.common.collect.ImmutableList;

import one.util.streamex.EntryStream;

/**
 * TODO compare class with {@link EmbeddableComponentManager} and analyse missing stuff
 */
@Service(SpringComponentManager.NAME)
public class SpringComponentManager implements ComponentManager {

  public static final String NAME = "springify";

  private final GenericApplicationContext springContext;

  // TODO init ?
  // TODO notifies aren't always running anymore. what is it needed for?
  private ComponentEventManager eventManager;

  @Inject
  public SpringComponentManager(GenericApplicationContext context) {
    springContext = context;
  }

  /**
   * Load all component annotations and register them as components.
   */
  // @PostConstruct TODO
  public void init() {
    try {
      // Extension point to allow component to manipulate ComponentManager initialized state.
      lookupList(ComponentManagerInitializer.class).stream()
          .forEach(initializer -> initializer.initialize(this));
    } catch (ComponentLookupException exc) {
      throw new RuntimeException("failed to initialise component manager", exc);
    }
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
    return getBean(uniqueBeanName(role, hint), role)
        .map(Optional::of)
        .orElseGet(() -> getBean(hint, role))
        .orElseThrow(() -> new ComponentLookupException("lookup - failed for role [" + role
            + "] and hint [" + hint + "]"));
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
    return EntryStream.of(springContext.getBeansOfType(role))
        .mapKeys(ComponentDescriptor::getHintFromBeanName)
        .toImmutableMap();
  }

  @Override
  public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException {
    return ImmutableList.copyOf(springContext.getBeansOfType(role).values());
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
      if (component != null) {
        // according to method contract, if an instance is provided it should never be created from
        // the descriptor, irrespective of the instantiation strategy. the component must be fully
        // initialized already.
        if (descriptor.getInstantiationStrategy() == ComponentInstantiationStrategy.PER_LOOKUP) {
          // TODO let's log this, it shouldn't happen outside of tests!
        }
        springContext.getBeanFactory().registerSingleton(beanName, component);
      } else {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder
            .genericBeanDefinition(descriptor.getImplementation());
        // TODO required?
        // builder.addPropertyValue("property1", "propertyValue");
        // builder.setInitMethodName("initialize"); // perhaps for initializables ?
        builder.setScope(descriptor.getBeanScope());
        springContext.registerBeanDefinition(beanName, builder.getBeanDefinition());
      }
      getEventManager().ifPresent(em -> em.notifyComponentRegistered(descriptor));
    } catch (BeansException exc) {
      throw new ComponentRepositoryException("registerComponent - failed for descriptor ["
          + descriptor + "]", exc);
    }
  }

  @Override
  public void unregisterComponent(Class<?> role, String hint) {
    String beanName = uniqueBeanName(role, hint);
    ComponentDescriptor<?> descriptor = getComponentDescriptor(role, hint);
    if (descriptor != null) {
      try {
        springContext.removeBeanDefinition(beanName);
        // springContext.refresh(); // TODO needed?
      } catch (BeansException exc) {
        // TODO log
      }
      getEventManager().ifPresent(em -> em.notifyComponentUnregistered(descriptor));
    }
  }

  @Override
  public <T> void release(T component) throws ComponentLifecycleException {
    if (component != null) {
      try {
        // TODO is this sufficient ? (test with&without bean definition)
        springContext.getBeanFactory().destroyBean(component);
        // TODO flawed this can only release components with xwiki annoations
        // GenericApplicationContext ctx = SpringCtx.get();
        // loader.streamComponentsDescriptors(component.getClass())
        // .filter(descriptor -> hasComponent(descriptor.getRole(), descriptor.getRoleHint()))
        // .map(this::uniqueBeanName)
        // .filter(beanName -> ctx.getBean(beanName) == component)
        // .forEach(beanName -> ctx.getDefaultListableBeanFactory().destroySingleton(beanName));
        // springContext.refresh(); // TODO not allowed?
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

  private final ComponentDescriptorFactory cdFactory = new ComponentDescriptorFactory();

  @SuppressWarnings("unchecked")
  public <T> ComponentDescriptor<T> createComponentDescriptor(Class<T> role, String beanName,
      Object instance) {
    if (instance != null) {
      Class<? extends T> instanceClass = (Class<? extends T>) instance.getClass();
      return cdFactory.create(instanceClass, role,
          getHintFromBeanName(beanName));
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
