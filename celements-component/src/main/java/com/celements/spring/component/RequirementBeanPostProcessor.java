package com.celements.spring.component;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;
import org.xwiki.component.annotation.ComponentDescriptorFactory;
import org.xwiki.component.descriptor.ComponentDependency;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;

@Component
public class RequirementBeanPostProcessor implements BeanPostProcessor {

  private final ComponentManager cm;
  private final ComponentDescriptorFactory compDescrFactory;

  @Inject
  public RequirementBeanPostProcessor(
      @Named(SpringComponentManager.NAME) ComponentManager cm) {
    this.cm = cm;
    compDescrFactory = new ComponentDescriptorFactory();
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    compDescrFactory.createComponentDependencies(bean.getClass()).forEach(dependency -> {
      try {
        ReflectionUtils.setFieldValue(bean, dependency.getName(), getInstance(dependency));
      } catch (ComponentLookupException cle) {
        // TODO can't always init an instance, how did this work before?
        /*
         * e.g.:
         * ERROR instantiating ComponentDependency [name=currentDocumentNameFactory, hints=null]
         * on bean: org.xwiki.bridge.internal.DefaultAttachmentNameFactory@505fc5a4
         * ERROR instantiating ComponentDependency [name=documentAccessBridge, hints=null]
         * on bean: org.xwiki.bridge.internal.DefaultAttachmentNameFactory@505fc5a4
         *
         */
        System.err.println("ERROR instantiating " + dependency
            + " on bean: " + bean
            + " - " + cle.getMessage());
        // throw new IllegalStateException("failed instantiating " + dependency + " on bean: "
        // + bean, cle);
      }
    });
    return bean;
  }

  // TODO only working for singleton, add PER_LOOKUP
  // TODO check additional stuff being done in:
  // org.xwiki.component.embed.EmbeddableComponentManager.createInstance(ComponentDescriptor<T>)
  @NotNull
  private Object getInstance(ComponentDependency<?> dependency) throws ComponentLookupException {
    if (dependency.getMappingType() != null) {
      if (List.class.isAssignableFrom(dependency.getMappingType())) {
        return cm.lookupList(dependency.getRole());
      } else if (Map.class.isAssignableFrom(dependency.getMappingType())) {
        return cm.lookupMap(dependency.getRole());
      }
    }
    return cm.lookup(dependency.getRole(), dependency.getRoleHint());
  }

}
