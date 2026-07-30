package com.celements.spring.test;

@org.xwiki.component.annotation.Component(TestXWikiSingletonComponent.NAME)
public class TestXWikiSingletonComponent implements TestComponentRole {

  public static final String NAME = "TestXWikiSingleton";

  @Override
  public String getter() {
    return NAME;
  }

}
