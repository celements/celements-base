package com.celements.navigation.api;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.spring.security.oauth2.IdentityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class NavigationControllerTest extends AbstractComponentTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private NavigationRequestResolver requestResolver;
  private NavigationTreeBuilder treeBuilder;
  private MockMvc mockMvc;

  @Override
  protected void beforeSpringContextRefresh(ConfigurableApplicationContext context) {
    super.beforeSpringContextRefresh(context);
    context.addBeanFactoryPostProcessor(beanFactory -> beanFactory.registerSingleton(
        "testIdentityService", createNiceMock(IdentityService.class)));
  }

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(NavigationRequestResolver.class, NavigationTreeBuilder.class);
    requestResolver = getMock(NavigationRequestResolver.class);
    treeBuilder = getMock(NavigationTreeBuilder.class);
    var beanFactory = (DefaultListableBeanFactory) getBeanFactory();
    beanFactory.destroySingleton(NavigationController.class.getName());
    beanFactory.registerResolvableDependency(NavigationRequestResolver.class, requestResolver);
    beanFactory.registerResolvableDependency(NavigationTreeBuilder.class, treeBuilder);
    var controllerProxy = getBeanFactory().getBean(NavigationController.class.getName(),
        NavigationController.class);
    var controller = AopTestUtils.<NavigationController>getTargetObject(controllerProxy);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  public void test_getNavigation_isPublicThroughMethodSecurity() throws Exception {
    var method = NavigationController.class.getMethod("getNavigation", String.class,
        String.class, String.class, String.class, int.class);
    assertEquals("permitAll()", method.getAnnotation(PreAuthorize.class).value());
  }

  @Test
  public void test_getNavigation_returnsExactJsonAndPrivateNoStoreHeader() throws Exception {
    var request = request();
    expect(requestResolver.resolve("Content", null, "de", null, 0)).andReturn(request);
    expect(treeBuilder.build(request)).andReturn(response());

    replayDefault();
    var result = mockMvc
        .perform(get("/v1/navigation/Content").param("language", "de")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andReturn();
    verifyDefault();

    JsonNode json = readJson(result.getResponse().getContentAsByteArray());
    assertEquals("Content", json.get("nodeSpace").asText());
    assertTrue(json.get("currentNode").isNull());
    assertTrue(json.get("partName").isNull());
    assertTrue(json.at("/segments/0/partName").isNull());
    assertTrue(json.at("/segments/0/nodes/0/isLeaf").asBoolean());
    assertFalse(json.at("/segments/0/nodes/0/isActive").asBoolean());
    assertFalse(json.at("/segments/0/nodes/0/isOpen").asBoolean());
    assertTrue(json.at("/segments/0/nodes/0/children").isEmpty());
  }

  @Test
  public void test_getNavigation_rejectsNonnumericInactiveLevel() throws Exception {
    replayDefault();
    var result = mockMvc
        .perform(get("/v1/navigation/Content").param("show_inactive_to_level", "not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andReturn();
    verifyDefault();

    assertError(result.getResponse().getContentAsByteArray(), "invalid_parameter",
        "The parameter is invalid.");
  }

  @Test
  public void test_getNavigation_returnsStableSafeApiError() throws Exception {
    expect(requestResolver.resolve("Content", null, null, null, 101))
        .andThrow(new NavigationApiException(HttpStatus.BAD_REQUEST, "invalid_parameter",
            "The parameter is invalid."));

    replayDefault();
    var result = mockMvc
        .perform(get("/v1/navigation/Content").param("show_inactive_to_level", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andReturn();
    verifyDefault();

    assertError(result.getResponse().getContentAsByteArray(), "invalid_parameter",
        "The parameter is invalid.");
  }

  @Test
  public void test_getNavigation_returnsPrivateNoStoreHeaderForNotFound() throws Exception {
    var request = request();
    expect(requestResolver.resolve("Content", "Content.Missing", null, null, 0)).andReturn(request);
    expect(treeBuilder.build(request))
        .andThrow(new NavigationApiException(HttpStatus.NOT_FOUND, "navigation_node_not_found",
            "The navigation node was not found."));

    replayDefault();
    var result = mockMvc
        .perform(get("/v1/navigation/Content").param("currentNode", "Content.Missing"))
        .andExpect(status().isNotFound())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andReturn();
    verifyDefault();

    assertError(result.getResponse().getContentAsByteArray(), "navigation_node_not_found",
        "The navigation node was not found.");
  }

  @Test
  public void test_getNavigation_mapsUnexpectedFailureWithoutLeakingDetails() throws Exception {
    var request = request();
    expect(requestResolver.resolve("Content", null, null, null, 0)).andReturn(request);
    expect(treeBuilder.build(request))
        .andThrow(new IllegalStateException("secret backend detail"));

    replayDefault();
    var result = mockMvc.perform(get("/v1/navigation/Content"))
        .andExpect(status().isInternalServerError())
        .andExpect(header().string("Cache-Control", "private, no-store"))
        .andReturn();
    verifyDefault();

    assertFalse(result.getResponse().getContentAsString().contains("secret backend detail"));
    assertError(result.getResponse().getContentAsByteArray(), "navigation_unavailable",
        "Navigation is currently unavailable.");
  }

  private NavigationRequest request() {
    return new NavigationRequest(new SpaceReference("Content", new WikiReference("xwiki")),
        "Content", Optional.empty(), Optional.empty(), "de", Optional.empty(), 0);
  }

  private NavigationTreeResponse response() {
    var node = new NavigationNodeDto("Content.Home", "/Content/Home?language=de", "Startseite",
        true, false, false, List.of());
    return new NavigationTreeResponse("Content", null, "de", null, 0,
        List.of(new NavigationSegmentDto(null, List.of(node))));
  }

  private JsonNode readJson(byte[] content) throws Exception {
    return objectMapper.readTree(content);
  }

  private void assertError(byte[] content, String code, String message) throws Exception {
    JsonNode json = readJson(content);
    assertEquals(code, json.get("code").asText());
    assertEquals(message, json.get("message").asText());
  }

}
