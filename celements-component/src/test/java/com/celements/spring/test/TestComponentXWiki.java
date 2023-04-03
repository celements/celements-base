package com.celements.spring.test;

@org.xwiki.component.annotation.Component("TestXWiki")
public class TestComponentXWiki implements TestRole {

  @Override
  public String getter() {
    return "TestComponentXWiki";
  }

}
