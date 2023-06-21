package com.celements.spring.context;

import static com.celements.common.MoreOptional.*;
import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static java.util.stream.Collectors.*;

import java.util.List;
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
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingFunction;

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

  /**
   * Register beans implementing a {@link ComponentRole} with the
   * {@link ComponentRole#getBeanName()}.
   */
  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
      throws BeanDefinitionStoreException {
    super.registerBeanDefinition(determineBeanName(beanDefinition, beanName), beanDefinition);
  }

  private String determineBeanName(BeanDefinition beanDefinition, String beanName) {
    if (beanName.contains(ComponentRole.BEAN_NAME_SEPARATOR)) {
      return beanName;
    }
    List<Class<?>> roles = getComponentRoles(beanDefinition);
    if (roles.size() > 1) {
      throw new IllegalStateException("multiple roles found for class ["
          + beanDefinition.getBeanClassName() + "]: " + roles);
    }
    return roles.stream()
        .map(role -> new DefaultComponentRole<>(role, beanName))
        .map(ComponentRole::getBeanName)
        .findAny()
        .orElse(beanName);
  }

  private List<Class<?>> getComponentRoles(BeanDefinition beanDefinition) {
    try {
      return Optional.ofNullable(beanDefinition.getBeanClassName())
          .map(rethrowFunction(Class::forName))
          .map(Class::getInterfaces)
          .map(Stream::of).orElse(Stream.empty())
          .filter(this::isComponentRole)
          .collect(toList());
    } catch (ClassNotFoundException exc) {
      throw new BeanDefinitionStoreException("failed loading class for: " + beanDefinition, exc);
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
