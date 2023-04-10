package com.celements.spring.context;

import static com.celements.common.MoreOptional.*;
import static com.google.common.base.Strings.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Named;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.stereotype.Component;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingFunction;
import com.google.common.base.Splitter;

@Component
public class XWikiShimBeanFactory extends DefaultListableBeanFactory {

  public static final String SEPARATOR = "|||";

  public static String uniqueBeanName(Class<?> role, String hint) {
    if (isNullOrEmpty(hint)) {
      hint = ComponentRole.DEFAULT_HINT;
    }
    return role.getName() + SEPARATOR + hint;
  }

  @SuppressWarnings("unchecked")
  public static <T> Optional<ComponentRole<T>> getRoleFromBeanName(String beanName) {
    try {
      List<String> parts = Splitter.on(SEPARATOR).omitEmptyStrings().splitToList(beanName);
      if (parts.size() > 1) {
        Class<T> role = (Class<T>) Class.forName(parts.get(0));
        return Optional.of(new DefaultComponentRole<>(role, parts.get(1)));
      }
      return Optional.empty();
    } catch (ClassNotFoundException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  /**
   * This fallback is required for {@link org.xwiki.component.annotation.Component} beans, which are
   * registered with the {@link #uniqueBeanName(Class, String)} but may be requested by their hint.
   */
  @Override
  protected <T> T doGetBean(String name, Class<T> requiredType, Object[] args,
      boolean typeCheckOnly) throws BeansException {
    try {
      return super.doGetBean(name, requiredType, args, typeCheckOnly);
    } catch (NoSuchBeanDefinitionException exc) {
      return Optional.ofNullable(requiredType)
          .filter(this::isComponentRole)
          .map(t -> uniqueBeanName(t, name))
          .flatMap(asOpt(n -> super.doGetBean(n, requiredType, args, typeCheckOnly)))
          .orElseThrow(() -> exc);
    }
  }

  /**
   * This fallback is required for {@link org.xwiki.component.annotation.Component} beans, which are
   * registered with the {@link #uniqueBeanName(Class, String)} but may be autowired by their hint.
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
          .map(value -> uniqueBeanName(getDependencyType(), value))
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
