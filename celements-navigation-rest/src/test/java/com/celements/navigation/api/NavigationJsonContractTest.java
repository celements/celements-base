package com.celements.navigation.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class NavigationJsonContractTest {

  @Test
  public void defensivelyCopiesAllDtoLists() {
    var mutableChildren = new ArrayList<NavigationNodeDto>();
    var node = new NavigationNodeDto("Content.Home", "/Content/Home", "Home", true, false, false,
        mutableChildren);
    mutableChildren.add(node);
    assertTrue(node.children().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> node.children().add(node));
  }

}
