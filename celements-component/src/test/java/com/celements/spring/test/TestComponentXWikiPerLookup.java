package com.celements.spring.test;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

@org.xwiki.component.annotation.Component("TestXWikiPerLookup")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class TestComponentXWikiPerLookup implements TestRole {

  @Override
  public String getter() {
    return "TestComponentXWikiPerLookup " + this.hashCode();
  }

}
