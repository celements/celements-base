package com.celements.spring.test;

@org.springframework.stereotype.Component("TestSpring")
public class TestComponentSpring implements TestRole {

  @Override
  public String getter() {
    return "TestComponentSpring";
  }

}
