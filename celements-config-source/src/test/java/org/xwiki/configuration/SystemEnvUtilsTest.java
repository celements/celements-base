package org.xwiki.configuration;

import static org.junit.Assert.*;
import static org.xwiki.configuration.SystemEnvUtils.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class SystemEnvUtilsTest {

  @Test
  public void test_parseJsonEnvList() {
    assertEquals(Arrays.asList("value1", "value2"),
        tryParseJsonArray("[\"value1\",\"value2\"]").orElse(null));
  }

  @Test
  public void test_parseEmptyJsonEnvList() {
    assertEquals(Optional.of(List.of()), tryParseJsonArray("[]"));
  }

  @Test
  public void test_parsePlainEnvList() {
    assertEquals(Optional.empty(), tryParseJsonArray("value"));
  }

  @Test
  public void test_parseMalformedJsonEnvList() {
    assertEquals(Optional.empty(), tryParseJsonArray("[\"value\""));
  }
}
