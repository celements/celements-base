package com.celements.spring.test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.context.ApplicationContext;
import org.xwiki.component.annotation.Requirement;

import com.celements.spring.context.CelSpringContext;
import com.google.common.collect.ImmutableSet;

public abstract class TestAbstractCompositeComponent implements TestCompositeComponentRole {

  @Inject
  private TestComponentRole defaultFromSpring;

  @Requirement
  private TestComponentRole defaultFromXWiki;

  @Inject
  @Named(TestSpringSingletonComponent.NAME)
  private TestComponentRole springFromSpring;

  @Requirement(TestSpringSingletonComponent.NAME)
  private TestComponentRole springFromXWiki;

  @Inject
  @Named(TestXWikiSingletonComponent.NAME)
  private TestComponentRole xwikiFromSpring;

  @Requirement(TestXWikiSingletonComponent.NAME)
  private TestComponentRole xwikiFromXWiki;

  @Inject
  private List<TestComponentRole> listFromSpring;

  @Requirement
  private List<TestComponentRole> listFromXWiki;

  @Inject
  private Map<String, TestComponentRole> mapFromSpring;

  @Requirement
  private Map<String, TestComponentRole> mapFromXWiki;

  @Inject
  private ApplicationContext springCtxFromSpring;

  @Requirement
  private ApplicationContext springCtxFromXWiki;

  @Override
  public void assertComposition() {
    assertNotNull(defaultFromSpring);
    assertSame(TestDefaultComponent.class, defaultFromSpring.getClass());
    assertNotNull(defaultFromXWiki);
    assertSame(TestDefaultComponent.class, defaultFromXWiki.getClass());
    assertNotNull(springFromSpring);
    assertSame(TestSpringSingletonComponent.class, springFromSpring.getClass());
    assertNotNull(springFromXWiki);
    assertSame(TestSpringSingletonComponent.class, springFromXWiki.getClass());
    assertSame(springFromSpring, springFromXWiki);
    assertNotNull(xwikiFromSpring);
    assertSame(TestXWikiSingletonComponent.class, xwikiFromSpring.getClass());
    assertNotNull(xwikiFromXWiki);
    assertSame(TestXWikiSingletonComponent.class, xwikiFromXWiki.getClass());
    assertSame(xwikiFromSpring, xwikiFromXWiki);
    assertList(listFromSpring);
    assertList(listFromXWiki);
    // XXX: @Inject a map will always get you the beanName and not the hint like for @Requirement
    assertMap(mapFromSpring, i -> i.getClass().getName());
    assertMap(mapFromXWiki, i -> i.getter());
    assertNotNull(springCtxFromSpring);
    assertSame(CelSpringContext.class, springCtxFromSpring.getClass());
    assertNotNull(springCtxFromXWiki);
    assertSame(CelSpringContext.class, springCtxFromXWiki.getClass());
  }

  private void assertList(Collection<TestComponentRole> list) {
    assertNotNull(list);
    assertEquals(5, list.size());
    assertTrue(list.remove(defaultFromXWiki));
    assertTrue(list.remove(springFromSpring));
    assertTrue(list.remove(xwikiFromXWiki));
    assertEquals(
        ImmutableSet.of(TestSpringPerLookupComponent.class, TestXWikiPerLookupComponent.class),
        list.stream().map(o -> o.getClass()).collect(Collectors.toSet()));
  }

  private void assertMap(Map<String, TestComponentRole> map,
      Function<TestComponentRole, String> keyGetter) {
    assertNotNull(map);
    assertEquals(5, map.size());
    assertSame(defaultFromSpring, map.get(keyGetter.apply(defaultFromSpring)));
    assertSame(springFromXWiki, map.get(keyGetter.apply(springFromXWiki)));
    assertSame(xwikiFromSpring, map.get(keyGetter.apply(xwikiFromSpring)));
    assertSame(TestSpringPerLookupComponent.class,
        map.get(keyGetter.apply(new TestSpringPerLookupComponent())).getClass());
    assertSame(TestXWikiPerLookupComponent.class,
        map.get(keyGetter.apply(new TestXWikiPerLookupComponent())).getClass());
  }

}
