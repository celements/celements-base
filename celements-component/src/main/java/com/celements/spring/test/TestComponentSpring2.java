package com.celements.spring.test;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Requirement;

@org.springframework.stereotype.Component("TestSpring2")
public class TestComponentSpring2 implements TestRole {

  @Inject
  @Named("TestSpring")
  private TestRole springFromSpring;

  @Requirement("TestSpring")
  private TestRole springFromXWiki;

  @Inject
  @Named("TestXWiki")
  private TestRole xwikiFromSpring;

  @Requirement("TestXWiki")
  private TestRole xwikiFromXWiki;

  @Override
  public String getter() {
    return "in TestComponentSpring2 got: "
        + springFromSpring.getter() + " from @Inject, "
        + xwikiFromSpring.getter() + " from @Inject, "
        + springFromXWiki.getter() + " from @Requirement, "
        + xwikiFromXWiki.getter() + " from @Requirement";
  }

}
