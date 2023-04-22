package com.celements.spring.test;

import javax.inject.Named;

@org.springframework.stereotype.Component
@Named(TestSpringSingletonComponent.NAME)
public class TestSpringSingletonComponent implements TestComponentRole {

  public static final String NAME = "TestSpringSingleton";

  @Override
  public String getter() {
    return NAME;
  }

}
