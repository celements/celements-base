package com.celements.spring.test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component("TestSpringPerLookup")
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
public class TestComponentSpringPerLookup implements TestRole {

  @Override
  public String getter() {
    return "TestComponentSpringPerLookup" + this.hashCode();
  }

}
