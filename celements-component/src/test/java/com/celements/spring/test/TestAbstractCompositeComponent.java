package com.celements.spring.test;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Requirement;

import com.google.common.collect.ImmutableSet;

public abstract class TestAbstractCompositeComponent implements TestCompositeComponentRole {

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

  @Override
  public void assertComposition() {
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
    assertMap(mapFromSpring, TestComponentRole.class.getName() + "|||");
    assertMap(mapFromXWiki, "");
  }

  private void assertList(Collection<TestComponentRole> list) {
    assertNotNull(list);
    assertEquals(4, list.size());
    assertTrue(list.remove(springFromXWiki));
    assertTrue(list.remove(xwikiFromSpring));
    assertEquals(
        ImmutableSet.of(TestSpringPerLookupComponent.class, TestXWikiPerLookupComponent.class),
        list.stream().map(o -> o.getClass()).collect(Collectors.toSet()));
  }

  private void assertMap(Map<String, TestComponentRole> map, String prefix) {
    assertNotNull(map);
    assertEquals(4, map.size());
    assertSame(springFromXWiki, map.get(TestSpringSingletonComponent.NAME));
    assertSame(xwikiFromSpring, map.get(prefix + TestXWikiSingletonComponent.NAME));
    assertSame(TestSpringPerLookupComponent.class,
        map.get(TestSpringPerLookupComponent.NAME).getClass());
    assertSame(TestXWikiPerLookupComponent.class,
        map.get(prefix + TestXWikiPerLookupComponent.NAME).getClass());
  }

}
