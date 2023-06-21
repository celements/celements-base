package org.xwiki.component.spring;

import java.io.IOException;
import java.util.List;

import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.xwiki.component.annotation.ComponentAnnotationLoader;
import org.xwiki.component.descriptor.ComponentDescriptor;

@Configuration
@ComponentScan(basePackages = {
    "org.xwiki",
    "com.xpn.xwiki" })
public class XWikiSpringConfig {

  public static final List<ComponentDescriptor<?>> loadXWikiDescriptors(
      ResourceLoader resourceLoader) throws ApplicationContextException {
    try {
      return new ComponentAnnotationLoader().loadDeclaredDescriptors(
          resourceLoader.getClassLoader());
    } catch (ClassNotFoundException | IOException exc) {
      throw new ApplicationContextException("failed to scan XWiki components", exc);
    }
  }
}
