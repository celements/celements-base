package com.celements.spring;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentRole;

import com.celements.spring.context.CelSpringContext;
import com.celements.spring.test.TestComponentRole;
import com.celements.spring.test.TestCompositeComponentRole;
import com.celements.spring.test.TestSpringCompositeComponent;
import com.celements.spring.test.TestSpringPerLookupComponent;
import com.celements.spring.test.TestSpringSingletonComponent;
import com.celements.spring.test.TestXWikiCompositeComponent;
import com.celements.spring.test.TestXWikiPerLookupComponent;
import com.celements.spring.test.TestXWikiSingletonComponent;

public class CelSpringContextTest {

  private CelSpringContext ctx;

  @Before
  public void prepare() {
    ctx = new CelSpringContext();
  }

  @After
  public void destroy() {
    ctx.close();
    ctx = null;
  }

  @Test
  public void test_singleton() {
    TestComponentRole s1 = ctx.getBean(TestSpringSingletonComponent.NAME, TestComponentRole.class);
    TestComponentRole s2 = ctx.getBean(TestSpringSingletonComponent.NAME, TestComponentRole.class);
    assertSame(s1, s2);
    TestComponentRole x1 = ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class);
    TestComponentRole x2 = ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class);
    assertSame(x1, x2);
  }

  @Test
  public void test_perLookup() {
    TestComponentRole s1 = ctx.getBean(TestSpringPerLookupComponent.NAME, TestComponentRole.class);
    TestComponentRole s2 = ctx.getBean(TestSpringPerLookupComponent.NAME, TestComponentRole.class);
    assertNotSame(s1, s2);
    TestComponentRole x1 = ctx.getBean(TestXWikiPerLookupComponent.NAME, TestComponentRole.class);
    TestComponentRole x2 = ctx.getBean(TestXWikiPerLookupComponent.NAME, TestComponentRole.class);
    assertNotSame(x1, x2);
  }

  @Test
  public void test_beanName() throws Exception {
    TestComponentRole defaultBean = ctx.getBean(TestComponentRole.class);
    assertNotNull(defaultBean);
    assertSame(defaultBean, ctx.getBean(TestComponentRole.class));
    assertSame(defaultBean, ctx.getBean(new DefaultComponentRole<>(
        TestComponentRole.class, null).getBeanName()));
    assertSame(defaultBean, ctx.getBean(new DefaultComponentRole<>(
        TestComponentRole.class, "default").getBeanName()));
    assertSame(ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class),
        ctx.getBean(new DefaultComponentRole<>(TestComponentRole.class,
            TestXWikiSingletonComponent.NAME).getBeanName()));
  }

  @Test
  public void test_composition() {
    ctx.getBean(TestSpringCompositeComponent.NAME, TestCompositeComponentRole.class)
        .assertComposition();
    ctx.getBean(TestXWikiCompositeComponent.NAME, TestCompositeComponentRole.class)
        .assertComposition();
  }
}
