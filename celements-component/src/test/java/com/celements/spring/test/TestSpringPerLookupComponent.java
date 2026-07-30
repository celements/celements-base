package com.celements.spring.test;

import javax.inject.Named;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;

@org.springframework.stereotype.Component
@Named(TestSpringPerLookupComponent.NAME)
@Scope(value = BeanDefinition.SCOPE_PROTOTYPE)
public class TestSpringPerLookupComponent implements TestComponentRole {

  public static final String NAME = "TestSpringPerLookup";

  @Override
  public String getter() {
    return NAME;
  }

}
