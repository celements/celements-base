package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springdoc.core.SpringDocConfigProperties;
import org.springdoc.core.SpringDocConfiguration;
import org.springdoc.webmvc.core.SpringDocWebMvcConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NavigationOpenApiContractTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AnnotationConfigWebApplicationContext context;
  private MockMvc mockMvc;

  @Before
  public void prepare() {
    context = new AnnotationConfigWebApplicationContext();
    context.setServletContext(new MockServletContext());
    context.register(DelegatingWebMvcConfiguration.class, SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class);
    context.addBeanFactoryPostProcessor(beanFactory -> {
      beanFactory.registerSingleton(SpringDocConfigProperties.class.getName(),
          new SpringDocConfigProperties());
      beanFactory.registerSingleton(NavigationController.class.getName(),
          new NavigationController(createNiceMock(NavigationRequestResolver.class),
              createNiceMock(NavigationTreeBuilder.class)));
    });
    context.refresh();
    mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
  }

  @After
  public void cleanup() {
    context.close();
  }

  @Test
  public void apiDocsExposeNavigationContract() throws Exception {
    var result = mockMvc.perform(get("/api/v3/api-docs").servletPath("/api"))
        .andExpect(status().isOk())
        .andReturn();
    JsonNode json = objectMapper.readTree(result.getResponse().getContentAsByteArray());
    JsonNode operation = json.at("/paths/~1v1~1navigation~1{nodeSpace}/get");
    assertFalse(operation.isMissingNode());
    assertEquals("Get the current wiki's navigation tree", operation.get("summary").asText());
    assertEquals(
        Set.of("nodeSpace", "currentNode", "language", "partName", "show_inactive_to_level"),
        parameterNames(operation.get("parameters")));
    assertTrue(findParameter(operation, "nodeSpace").get("required").asBoolean());
    JsonNode inactiveLevelSchema = findParameter(operation, "show_inactive_to_level").get("schema");
    assertEquals("0", inactiveLevelSchema.get("default").asText());
    assertEquals(0, inactiveLevelSchema.get("minimum").asInt());
    assertEquals(100, inactiveLevelSchema.get("maximum").asInt());
    assertEquals(Set.of("200", "400", "404", "500"),
        iterableFieldNames(operation.get("responses").fieldNames()));
    for (String status : Set.of("200", "400", "404", "500")) {
      JsonNode response = operation.get("responses").get(status);
      JsonNode content = response.at("/content/application~1json");
      assertTrue(content.get("schema").isObject());
    }
    JsonNode schemas = json.at("/components/schemas");
    JsonNode treeSchema = findSchema(schemas, "NavigationTreeResponse");
    JsonNode nodeSchema = findSchema(schemas, "NavigationNodeDto");
    JsonNode segmentSchema = findSchema(schemas, "NavigationSegmentDto");
    JsonNode errorSchema = findSchema(schemas, "NavigationErrorResponse");
    assertNotNull(treeSchema);
    assertNotNull(nodeSchema);
    assertNotNull(segmentSchema);
    assertNotNull(errorSchema);
    assertTrue(treeSchema.at("/properties/segments").isObject());
    assertTrue(treeSchema.at("/properties/currentNode/nullable").asBoolean());
    assertTrue(treeSchema.at("/properties/partName/nullable").asBoolean());
    assertTrue(segmentSchema.at("/properties/partName/nullable").asBoolean());
    assertTrue(nodeSchema.at("/properties/isLeaf").isObject());
    assertTrue(nodeSchema.at("/properties/isActive").isObject());
    assertTrue(nodeSchema.at("/properties/isOpen").isObject());
    assertTrue(nodeSchema.at("/properties/children").isObject());
    assertTrue(errorSchema.at("/properties/code").isObject());
    assertTrue(errorSchema.at("/properties/message").isObject());
  }

  private Set<String> parameterNames(JsonNode parameters) {
    var names = new HashSet<String>();
    parameters.forEach(parameter -> names.add(parameter.get("name").asText()));
    return names;
  }

  private Set<String> iterableFieldNames(Iterator<String> fields) {
    var names = new HashSet<String>();
    fields.forEachRemaining(names::add);
    return names;
  }

  private JsonNode findParameter(JsonNode operation, String name) {
    for (JsonNode parameter : operation.get("parameters")) {
      if (name.equals(parameter.get("name").asText())) {
        return parameter;
      }
    }
    fail("Missing OpenAPI parameter " + name);
    return null;
  }

  private JsonNode findSchema(JsonNode schemas, String simpleName) {
    Iterator<String> names = schemas.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      if (name.endsWith(simpleName)) {
        return schemas.get(name);
      }
    }
    return null;
  }

}
