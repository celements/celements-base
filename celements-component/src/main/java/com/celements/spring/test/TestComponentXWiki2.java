package com.celements.spring.test;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Requirement;

@org.xwiki.component.annotation.Component("TestXWiki2")
public class TestComponentXWiki2 implements TestRole {

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
    return "in TestComponentXWiki2 got: "
        + springFromSpring.getter() + " from @Inject, "
        + xwikiFromSpring.getter() + " from @Inject, "
        + springFromXWiki.getter() + " from @Requirement, "
        + xwikiFromXWiki.getter() + " from @Requirement";
  }

}
