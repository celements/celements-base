package com.celements.rights.access;

import static org.junit.Assert.*;

import org.junit.Test;

public class EAccessLevelTest {

  @Test
  public void testGetAccessLevel() {
    assertEquals(EAccessLevel.VIEW, EAccessLevel.getAccessLevel("view").get());
    assertEquals(EAccessLevel.EDIT, EAccessLevel.getAccessLevel("edit").get());
    assertEquals(EAccessLevel.ADMIN, EAccessLevel.getAccessLevel("admin").get());
    assertFalse(EAccessLevel.getAccessLevel("unknown").isPresent());
    assertFalse(EAccessLevel.getAccessLevel(null).isPresent());
  }

}
