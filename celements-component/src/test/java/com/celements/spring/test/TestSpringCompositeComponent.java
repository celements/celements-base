package com.celements.spring.test;

import javax.inject.Named;

@org.springframework.stereotype.Component
@Named(TestSpringCompositeComponent.NAME)
public class TestSpringCompositeComponent extends TestAbstractCompositeComponent {

  public static final String NAME = "TestSpringComposite";

}
