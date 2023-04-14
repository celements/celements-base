package com.celements.spring;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import com.celements.spring.context.CelSpringContext;
import com.celements.spring.context.SpringShimComponentManager;
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
  private ComponentManager cm;

  @Before
  public void prepare() {
    ctx = new CelSpringContext();
    cm = ctx.getBean(SpringShimComponentManager.NAME, ComponentManager.class);
  }

  @After
  public void destroy() {
    cm = null;
    ctx.close();
    ctx = null;
  }

  @Test
  public void test_hasComponent() throws Exception {
    assertTrue(cm.hasComponent(ComponentManager.class));
    assertTrue(cm.hasComponent(ComponentManager.class, "default"));
    assertTrue(cm.hasComponent(ComponentManager.class, SpringShimComponentManager.NAME));
  }

  @Test
  public void test_lookup() throws Exception {
    assertSame(cm.lookup(ComponentManager.class),
        ctx.getBean(ComponentManager.class));
    assertSame(cm.lookup(ComponentManager.class),
        ctx.getBean(new DefaultComponentRole<>(ComponentManager.class, null).getBeanName()));
    assertSame(cm.lookup(ComponentManager.class, "default"),
        ctx.getBean(new DefaultComponentRole<>(ComponentManager.class, null).getBeanName()));
    assertSame(cm.lookup(ComponentManager.class, "default"),
        ctx.getBean(new DefaultComponentRole<>(ComponentManager.class, "default").getBeanName()));
    assertSame(cm.lookup(ComponentManager.class, SpringShimComponentManager.NAME),
        ctx.getBean(SpringShimComponentManager.NAME, ComponentManager.class));
  }

  @Test
  public void test_lookupList() throws Exception {
    List<ComponentManager> ret = cm.lookupList(ComponentManager.class);
    assertNotNull(ret);
    assertEquals(2, ret.size());
    assertTrue(ret.contains(cm));
    assertTrue(ret.contains(ctx.getBean(ComponentManager.class)));
  }

  @Test
  public void test_lookupMap() throws Exception {
    Map<String, ComponentManager> ret = cm.lookupMap(ComponentManager.class);
    assertNotNull(ret);
    assertEquals(2, ret.size());
    assertSame(ctx.getBean(ComponentManager.class), ret.get("default"));
    assertSame(cm, ret.get(SpringShimComponentManager.NAME));
  }

  @Test
  public void test_ctx_singleton() {
    TestComponentRole s1 = ctx.getBean(TestSpringSingletonComponent.NAME, TestComponentRole.class);
    TestComponentRole s2 = ctx.getBean(TestSpringSingletonComponent.NAME, TestComponentRole.class);
    assertSame(s1, s2);
    TestComponentRole x1 = ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class);
    TestComponentRole x2 = ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class);
    assertSame(x1, x2);
  }

  @Test
  public void test_ctx_perLookup() {
    TestComponentRole s1 = ctx.getBean(TestSpringPerLookupComponent.NAME, TestComponentRole.class);
    TestComponentRole s2 = ctx.getBean(TestSpringPerLookupComponent.NAME, TestComponentRole.class);
    assertNotSame(s1, s2);
    TestComponentRole x1 = ctx.getBean(TestXWikiPerLookupComponent.NAME, TestComponentRole.class);
    TestComponentRole x2 = ctx.getBean(TestXWikiPerLookupComponent.NAME, TestComponentRole.class);
    assertNotSame(x1, x2);
  }

  @Test
  public void test_cm_singleton() throws ComponentLookupException {
    TestComponentRole s1 = cm.lookup(TestComponentRole.class, TestSpringSingletonComponent.NAME);
    TestComponentRole s2 = cm.lookup(TestComponentRole.class, TestSpringSingletonComponent.NAME);
    assertSame(s1, s2);
    TestComponentRole x1 = cm.lookup(TestComponentRole.class, TestXWikiSingletonComponent.NAME);
    TestComponentRole x2 = cm.lookup(TestComponentRole.class, TestXWikiSingletonComponent.NAME);
    assertSame(x1, x2);
  }

  @Test
  public void test_cm_perLookup() throws ComponentLookupException {
    TestComponentRole s1 = cm.lookup(TestComponentRole.class, TestSpringPerLookupComponent.NAME);
    TestComponentRole s2 = cm.lookup(TestComponentRole.class, TestSpringPerLookupComponent.NAME);
    assertNotSame(s1, s2);
    TestComponentRole x1 = cm.lookup(TestComponentRole.class, TestXWikiPerLookupComponent.NAME);
    TestComponentRole x2 = cm.lookup(TestComponentRole.class, TestXWikiPerLookupComponent.NAME);
    assertNotSame(x1, x2);
  }

  @Test
  public void test_composition() {
    ctx.getBean(TestSpringCompositeComponent.NAME, TestCompositeComponentRole.class)
        .assertComposition();
    ctx.getBean(TestXWikiCompositeComponent.NAME, TestCompositeComponentRole.class)
        .assertComposition();
  }

}
