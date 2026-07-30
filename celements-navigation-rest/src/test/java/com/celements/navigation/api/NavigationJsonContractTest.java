package com.celements.navigation.api;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NavigationJsonContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void serializesExplicitNullsAndExactBooleanPropertyNames() throws Exception {
    var node = new NavigationNodeDto("Content.Home", "/Content/Home?language=de", "Startseite",
        true, false, true, List.of());
    var response = new NavigationTreeResponse("Content", null, "de", null, 2,
        List.of(new NavigationSegmentDto(null, List.of(node))));
    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response));
    assertTrue(json.has("currentNode"));
    assertTrue(json.get("currentNode").isNull());
    assertTrue(json.has("partName"));
    assertTrue(json.get("partName").isNull());
    assertTrue(json.at("/segments/0/partName").isNull());
    JsonNode nodeJson = json.at("/segments/0/nodes/0");
    assertTrue(nodeJson.has("isLeaf"));
    assertTrue(nodeJson.has("isActive"));
    assertTrue(nodeJson.has("isOpen"));
    assertFalse(nodeJson.has("leaf"));
    assertFalse(nodeJson.has("active"));
    assertFalse(nodeJson.has("open"));
    assertTrue(nodeJson.get("children").isArray());
  }

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
