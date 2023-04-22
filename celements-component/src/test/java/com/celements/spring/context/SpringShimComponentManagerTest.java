package com.celements.spring.context;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

import com.celements.spring.test.TestComponentRole;
import com.celements.spring.test.TestDefaultComponent;
import com.celements.spring.test.TestSpringPerLookupComponent;
import com.celements.spring.test.TestSpringSingletonComponent;
import com.celements.spring.test.TestXWikiPerLookupComponent;
import com.celements.spring.test.TestXWikiSingletonComponent;
import com.google.common.collect.ImmutableSet;

public class SpringShimComponentManagerTest {

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
    assertTrue(cm.hasComponent(TestComponentRole.class));
    assertTrue(cm.hasComponent(TestComponentRole.class, "default"));
    assertSame(cm.hasComponent(TestComponentRole.class),
        cm.hasComponent(TestComponentRole.class, "default"));
    assertTrue(cm.hasComponent(TestComponentRole.class, TestXWikiSingletonComponent.NAME));
    assertTrue(cm.hasComponent(TestComponentRole.class, TestXWikiPerLookupComponent.NAME));
    assertTrue(cm.hasComponent(TestComponentRole.class, TestSpringSingletonComponent.NAME));
    assertTrue(cm.hasComponent(TestComponentRole.class, TestSpringPerLookupComponent.NAME));
    assertFalse(cm.hasComponent(TestComponentRole.class, "asdf"));
  }

  @Test
  public void test_lookup() throws Exception {
    assertSame(cm.lookup(TestComponentRole.class), ctx.getBean(TestComponentRole.class));
    assertSame(cm.lookup(TestComponentRole.class, "default"), ctx.getBean(TestComponentRole.class));
    assertSame(cm.lookup(TestComponentRole.class, TestXWikiSingletonComponent.NAME),
        ctx.getBean(TestXWikiSingletonComponent.NAME, TestComponentRole.class));
    assertSame(cm.lookup(TestComponentRole.class, TestSpringSingletonComponent.NAME),
        ctx.getBean(TestSpringSingletonComponent.NAME, TestComponentRole.class));
  }

  @Test
  public void test_lookup_singleton() throws ComponentLookupException {
    TestComponentRole s1 = cm.lookup(TestComponentRole.class, TestSpringSingletonComponent.NAME);
    TestComponentRole s2 = cm.lookup(TestComponentRole.class, TestSpringSingletonComponent.NAME);
    assertSame(s1, s2);
    TestComponentRole x1 = cm.lookup(TestComponentRole.class, TestXWikiSingletonComponent.NAME);
    TestComponentRole x2 = cm.lookup(TestComponentRole.class, TestXWikiSingletonComponent.NAME);
    assertSame(x1, x2);
  }

  @Test
  public void test_lookup_perLookup() throws ComponentLookupException {
    TestComponentRole s1 = cm.lookup(TestComponentRole.class, TestSpringPerLookupComponent.NAME);
    TestComponentRole s2 = cm.lookup(TestComponentRole.class, TestSpringPerLookupComponent.NAME);
    assertNotSame(s1, s2);
    TestComponentRole x1 = cm.lookup(TestComponentRole.class, TestXWikiPerLookupComponent.NAME);
    TestComponentRole x2 = cm.lookup(TestComponentRole.class, TestXWikiPerLookupComponent.NAME);
    assertNotSame(x1, x2);
  }

  @Test
  public void test_lookupList() throws Exception {
    List<TestComponentRole> ret = cm.lookupList(TestComponentRole.class);
    assertNotNull(ret);
    assertEquals(5, ret.size());
    assertEquals(ImmutableSet.of(TestDefaultComponent.class,
        TestXWikiSingletonComponent.class, TestXWikiPerLookupComponent.class,
        TestSpringSingletonComponent.class, TestSpringPerLookupComponent.class),
        ret.stream().map(i -> i.getClass()).collect(Collectors.toSet()));
  }

  @Test
  public void test_lookupMap() throws Exception {
    Map<String, TestComponentRole> ret = cm.lookupMap(TestComponentRole.class);
    assertNotNull(ret);
    assertEquals(5, ret.size());
    assertSame(ctx.getBean(TestComponentRole.class), ret.get("default"));
    assertSame(ctx.getBean(TestXWikiSingletonComponent.class),
        ret.get(TestXWikiSingletonComponent.NAME));
    assertSame(ctx.getBean(TestSpringSingletonComponent.class),
        ret.get(TestSpringSingletonComponent.NAME));
    assertSame(TestXWikiPerLookupComponent.class,
        ret.get(TestXWikiPerLookupComponent.NAME).getClass());
    assertSame(TestSpringPerLookupComponent.class,
        ret.get(TestSpringPerLookupComponent.NAME).getClass());
  }

  @Test
  public void test_getComponentDescriptor() throws Exception {
    for (String hint : Arrays.asList("default",
        TestXWikiSingletonComponent.NAME, TestXWikiPerLookupComponent.NAME,
        TestSpringSingletonComponent.NAME, TestSpringPerLookupComponent.NAME)) {
      ComponentDescriptor<TestComponentRole> descr = cm.getComponentDescriptor(
          TestComponentRole.class, hint);
      assertNotNull(descr);
      assertSame(TestComponentRole.class, descr.getRole());
      assertEquals(hint, descr.getRoleHint());
      assertSame(cm.lookup(TestComponentRole.class, hint).getClass(), descr.getImplementation());
    }
  }

  @Test
  public void test_getComponentDescriptorList() throws Exception {
    Map<String, ComponentDescriptor<TestComponentRole>> descriptors = cm
        .getComponentDescriptorList(TestComponentRole.class).stream()
        .collect(toMap(d -> d.getRoleHint(), d -> d));
    assertSame(TestDefaultComponent.class,
        descriptors.remove("default").getImplementation());
    assertSame(TestXWikiSingletonComponent.class,
        descriptors.remove(TestXWikiSingletonComponent.NAME).getImplementation());
    assertSame(TestXWikiPerLookupComponent.class,
        descriptors.remove(TestXWikiPerLookupComponent.NAME).getImplementation());
    assertSame(TestSpringSingletonComponent.class,
        descriptors.remove(TestSpringSingletonComponent.NAME).getImplementation());
    assertSame(TestSpringPerLookupComponent.class,
        descriptors.remove(TestSpringPerLookupComponent.NAME).getImplementation());
    assertTrue(descriptors.toString(), descriptors.isEmpty());
  }

  @Test
  public void test_release() throws Exception {
    for (String hint : Arrays.asList(
        TestXWikiSingletonComponent.NAME,
        TestSpringSingletonComponent.NAME)) {
      TestComponentRole roleOrig = cm.lookup(TestComponentRole.class, hint);
      assertNotNull(roleOrig);
      assertSame("not singleton", roleOrig, cm.lookup(TestComponentRole.class, hint));
      cm.release(roleOrig);
      assertNotNull("descriptor doesn't exist after release",
          cm.getComponentDescriptor(TestComponentRole.class, hint));
      TestComponentRole roleNew = cm.lookup(TestComponentRole.class, hint);
      assertNotNull(roleNew);
      assertNotSame("instance not released", roleOrig, roleNew);
    }
  }
}
