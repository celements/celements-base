package com.celements.spring;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import com.celements.spring.context.CelSpringContext;
import com.celements.spring.context.SpringShimComponentManager;
import com.celements.spring.context.XWikiShimBeanFactory;
import com.celements.spring.test.TestRole;

public class SpringTest {

  static ApplicationContext ctx;
  ComponentManager cm;

  @BeforeClass
  public static void prepareClass() {
    ctx = new CelSpringContext();
  }

  @Before
  public void prepare() {
    // ctx = SpringContext.get();
    cm = ctx.getBean(SpringShimComponentManager.NAME, ComponentManager.class);
  }

  @Test
  public void test_init() throws Exception {
    assertTrue(cm.hasComponent(ScriptService.class, "model"));
    assertSame(cm.lookup(ScriptService.class, "model"),
        ctx.getBean(XWikiShimBeanFactory.uniqueBeanName(
            ScriptService.class, "model")));
  }

  @Test
  public void test_hasComponent() throws Exception {
    assertTrue(cm.hasComponent(ComponentManager.class));
    assertTrue(cm.hasComponent(ComponentManager.class, "default"));
    assertTrue(cm.hasComponent(ComponentManager.class, SpringShimComponentManager.NAME));
  }

  @Test
  public void test() {
    System.out.println(ctx.getBean("TestXWiki2", TestRole.class).getter());
    System.out.println(ctx.getBean("TestSpring2", TestRole.class).getter());
    for (String beanName : ctx.getBeanDefinitionNames()) {
      System.out.println(beanName);
    }
  }

  @Test
  public void test_ctx_singleton() {
    TestRole s1 = ctx.getBean("TestSpring", TestRole.class);
    TestRole s2 = ctx.getBean("TestSpring", TestRole.class);
    assertSame(s1, s2);
    TestRole x1 = ctx.getBean("TestXWiki", TestRole.class);
    TestRole x2 = ctx.getBean("TestXWiki", TestRole.class);
    assertSame(x1, x2);
  }

  @Test
  public void test_ctx_perLookup() {
    TestRole s1 = ctx.getBean("TestSpringPerLookup", TestRole.class);
    TestRole s2 = ctx.getBean("TestSpringPerLookup", TestRole.class);
    assertNotSame(s1, s2);
    TestRole x1 = ctx.getBean("TestXWikiPerLookup", TestRole.class);
    TestRole x2 = ctx.getBean("TestXWikiPerLookup", TestRole.class);
    assertNotSame(x1, x2);
  }

  @Test
  public void test_cm_singleton() throws ComponentLookupException {
    TestRole s1 = cm.lookup(TestRole.class, "TestSpring");
    TestRole s2 = cm.lookup(TestRole.class, "TestSpring");
    assertSame(s1, s2);
    TestRole x1 = cm.lookup(TestRole.class, "TestXWiki");
    TestRole x2 = cm.lookup(TestRole.class, "TestXWiki");
    assertSame(x1, x2);
  }

  @Test
  public void test_cm_perLookup() throws ComponentLookupException {
    TestRole s1 = cm.lookup(TestRole.class, "TestSpringPerLookup");
    TestRole s2 = cm.lookup(TestRole.class, "TestSpringPerLookup");
    assertNotSame(s1, s2);
    TestRole x1 = cm.lookup(TestRole.class, "TestXWikiPerLookup");
    TestRole x2 = cm.lookup(TestRole.class, "TestXWikiPerLookup");
    assertNotSame(x1, x2);
  }

}
