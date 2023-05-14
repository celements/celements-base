package com.celements.spring.test;

@org.xwiki.component.annotation.Component
public class TestDefaultComponent implements TestComponentRole {

  @Override
  public String getter() {
    return "default";
  }

}
