package com.celements.spring.test;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

@org.xwiki.component.annotation.Component(TestXWikiPerLookupComponent.NAME)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class TestXWikiPerLookupComponent implements TestComponentRole {

  public static final String NAME = "TestXWikiPerLookup";

  @Override
  public String getter() {
    return NAME;
  }

}
