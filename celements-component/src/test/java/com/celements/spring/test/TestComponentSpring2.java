package com.celements.spring.test;

import java.util.List;
import java.util.Map;

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

  @Inject
  private List<TestRole> listFromSpring;

  @Requirement
  private List<TestRole> listFromXWiki;

  @Inject
  private Map<String, TestRole> mapFromSpring;

  @Requirement
  private Map<String, TestRole> mapFromXWiki;

  @Override
  public String getter() {
    return "in TestComponentSpring2 got: "
        + springFromSpring.getter() + " from @Inject, "
        + xwikiFromSpring.getter() + " from @Inject, "
        + listFromSpring.size() + " from @Inject, "
        + mapFromSpring + " from @Inject, "
        + springFromXWiki.getter() + " from @Requirement, "
        + xwikiFromXWiki.getter() + " from @Requirement, "
        + listFromXWiki.size() + " from @Requirement, "
        + mapFromXWiki + " from @Requirement";
  }

}
