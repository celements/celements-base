package com.celements.spring;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import com.celements.spring.component.SpringComponentManager;
import com.celements.spring.test.TestRole;

public class SpringTest /* extends AbstractComponentTest */ {

  SpringComponentManager cm;

  @Before
  public void setup() {
    cm = SpringContextManager.get().getBean(SpringComponentManager.NAME,
        SpringComponentManager.class);
  }

  @Test
  public void test_init() throws Exception {
    assertTrue(cm.hasComponent(ScriptService.class, "model"));
    assertSame(
        SpringContextManager.get().getBean(ComponentDescriptor.uniqueBeanName(
            ScriptService.class, "model")),
        cm.lookup(ScriptService.class, "model"));
  }

  @Test
  public void test_hasComponent() throws Exception {
    assertTrue(cm.hasComponent(ComponentManager.class));
    assertTrue(cm.hasComponent(ComponentManager.class, "default"));
    assertTrue(cm.hasComponent(ComponentManager.class, SpringComponentManager.NAME));
  }

  @Test
  public void test() {
    GenericApplicationContext ctx = SpringContextManager.get();
    System.out.println(ctx.getBean("TestXWiki2", TestRole.class).getter());
    System.out.println(ctx.getBean("TestSpring2", TestRole.class).getter());
    for (String beanName : ctx.getBeanDefinitionNames()) {
      System.out.println(beanName);
    }
  }

}
