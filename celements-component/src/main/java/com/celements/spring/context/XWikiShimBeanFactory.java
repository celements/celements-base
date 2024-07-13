package com.celements.spring.context;

import static com.celements.common.MoreOptional.*;
import static java.util.stream.Collectors.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Named;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingFunction;
import com.google.common.base.Strings;

import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;

/**
 * Extension of the {@link DefaultListableBeanFactory} ensuring backwards compatibility with XWiki
 * components. Since XWiki components are identified by Role+Hint we need to register, wire and
 * request them with the {@link ComponentRole#getBeanName()}.
 */
@Component
public class XWikiShimBeanFactory extends DefaultListableBeanFactory {

  public XWikiShimBeanFactory() {
    super();
  }

  public XWikiShimBeanFactory(@Nullable BeanFactory parentBeanFactory) {
    super(parentBeanFactory);
  }

  // TODO merge with getComponentRoles?
  public <T> Optional<ComponentRole<T>> resolveXWikiComponentRole(String beanName) {
    var beanNames = Stream.<String>empty();
    if (!Strings.isNullOrEmpty(beanName)) {
      String canonicalName = canonicalName(beanName);
      beanNames = StreamEx.of(canonicalName)
          .append(getAliases(canonicalName));
    }
    return beanNames.map(ComponentRole::<T>fromBeanName)
        .flatMap(Optional::stream)
        .findFirst();
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
      throws BeanDefinitionStoreException {
    try {
      var beanClass = Class.forName(beanDefinition.getBeanClassName());
      var beanFqn = beanClass.getName();
      List<ComponentRole<?>> roles = determineXWikiComponentRoles(beanName, beanClass);
      if (!roles.isEmpty()) {
        // component serving xwiki roles, register beanDef with class FQN
        super.registerBeanDefinition(beanFqn, beanDefinition);
        roles.stream() // and all role bean names as aliases
            .map(ComponentRole::getBeanName)
            .forEach(alias -> registerAlias(beanFqn, alias));
      } else {
        // default component, register beanDef with given beanName
        super.registerBeanDefinition(beanName, beanDefinition);
        registerAlias(beanName, beanFqn); // and class FQN as alias
      }
    } catch (ClassNotFoundException e) {
      super.registerBeanDefinition(beanName, beanDefinition);
    }
  }

  private List<ComponentRole<?>> determineXWikiComponentRoles(String beanName, Class<?> beanClass) {
    if (beanClass.getAnnotation(org.xwiki.component.annotation.Component.class) != null) {
      // XWiki Components are registered once per XWiki Role, thus we expect the specific role this
      // instance is registered for to be contained in the beanName
      // see CelSpringContext#registerXWikiComponent
      return List.of(ComponentRole.fromBeanName(beanName).orElseThrow());
    } else {
      // A Spring Component may serve many XWiki Roles, thus register all of them as aliases
      return Stream.of(ClassUtils.getAllInterfacesForClass(beanClass))
          .distinct()
          .filter(this::isComponentRole)
          .map(role -> new DefaultComponentRole<>(role, beanName))
          .collect(toUnmodifiableList());
    }
  }

  /**
   * This fallback is required for {@link org.xwiki.component.annotation.Component} beans, which are
   * registered with the {@link ComponentRole#getBeanName()} but may be requested by their hint.
   */
  @Override
  protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args,
      boolean typeCheckOnly) throws BeansException {
    try {
      return super.doGetBean(name, requiredType, args, typeCheckOnly);
    } catch (NoSuchBeanDefinitionException exc) {
      return Optional.ofNullable(requiredType)
          .filter(this::isComponentRole)
          .map(t -> new DefaultComponentRole<>(t, name))
          .map(ComponentRole::getBeanName)
          .flatMap(asOpt(n -> super.doGetBean(n, requiredType, args, typeCheckOnly)))
          .orElseThrow(() -> exc);
    }
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons,
      boolean allowEagerInit) throws BeansException {
    var beans = super.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    return EntryStream.of(beans)
        .mapKeys(beanName -> resolveXWikiComponentRole(beanName)
            .map(ComponentRole::getRoleHint)
            .orElse(beanName))
        .toCustomMap(() -> new LinkedHashMap<>(beans.size()));
  }

  /**
   * This fallback is required for {@link org.xwiki.component.annotation.Component} beans, which are
   * registered with the {@link ComponentRole#getBeanName()} but may be autowired by their hint.
   */
  @Override
  public Object doResolveDependency(DependencyDescriptor descriptor, String beanName,
      Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
    try {
      return super.doResolveDependency(descriptor, beanName, autowiredBeanNames, typeConverter);
    } catch (NoSuchBeanDefinitionException exc) {
      return Optional.ofNullable(descriptor)
          .filter(d -> isComponentRole(d.getDependencyType()))
          .map(ComponentRoleDependencyDescriptor::new)
          .flatMap(asOpt(d -> super.doResolveDependency(d, beanName, autowiredBeanNames,
              typeConverter)))
          .orElseThrow(() -> exc);
    }
  }

  private class ComponentRoleDependencyDescriptor extends DependencyDescriptor {

    private static final long serialVersionUID = 189370051788264173L;

    public ComponentRoleDependencyDescriptor(DependencyDescriptor original) {
      super(original);
    }

    @Override
    public Object resolveShortcut(BeanFactory beanFactory) throws BeansException {
      return getAnnotatedBeanName()
          .map(value -> new DefaultComponentRole<>(getDependencyType(), value))
          .map(ComponentRole::getBeanName)
          .flatMap(asOpt(beanFactory::getBean))
          .orElse(null);
    }

    private Optional<String> getAnnotatedBeanName() {
      return findFirstPresent(Stream.of(
          () -> Optional.ofNullable(getAnnotation(Named.class)).map(Named::value),
          () -> Optional.ofNullable(getAnnotation(Qualifier.class)).map(Qualifier::value)));
    }
  }

  private boolean isComponentRole(Class<?> type) {
    return (type != null) && type.isAnnotationPresent(
        org.xwiki.component.annotation.ComponentRole.class);
  }

  private <F, T> Function<F, Optional<T>> asOpt(ThrowingFunction<F, T, BeansException> func) {
    return val -> {
      try {
        return Optional.ofNullable(func.apply(val));
      } catch (BeansException exc) {
        return Optional.empty();
      }
    };
  }

}
