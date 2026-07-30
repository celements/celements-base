package com.celements.navigation.api;

import javax.inject.Inject;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/v1/navigation")
public class NavigationController {

  static final String CACHE_CONTROL = "private, no-store";

  private final NavigationRequestResolver requestResolver;
  private final NavigationTreeBuilder treeBuilder;

  @Inject
  NavigationController(NavigationRequestResolver requestResolver,
      NavigationTreeBuilder treeBuilder) {
    this.requestResolver = requestResolver;
    this.treeBuilder = treeBuilder;
  }

  @GetMapping(path = "/{nodeSpace}", produces = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("permitAll()")
  @Operation(summary = "Get the current wiki's navigation tree", description = """
      Public endpoint returning a caller-relative, rights-filtered navigation tree.
      Input and output references are canonical and local to the wiki handling the request.
      Responses are private and not cacheable by shared or browser caches.
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "200",
          description = "The rights-filtered segmented navigation tree",
          content = @Content(schema = @Schema(implementation = NavigationTreeResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "nodeSpace": "Content",
                    "currentNode": "Content.MyPage",
                    "language": "de",
                    "partName": null,
                    "showInactiveToLevel": 2,
                    "segments": []
                  }
                  """))),
      @ApiResponse(responseCode = "400",
          description = "Invalid reference or parameter, or unsupported language",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class),
              examples = @ExampleObject(value = """
                  {"code":"invalid_reference","message":"The reference is invalid."}
                  """))),
      @ApiResponse(responseCode = "404",
          description = "The active node is absent, inaccessible, out of root, or part-excluded",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "code": "navigation_node_not_found",
                    "message": "The navigation node was not found."
                  }
                  """))),
      @ApiResponse(responseCode = "500", description = "Navigation infrastructure is unavailable",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "code": "navigation_unavailable",
                    "message": "Navigation is currently unavailable."
                  }
                  """))) })
  public ResponseEntity<NavigationTreeResponse> getNavigation(
      @Parameter(description = "Canonical local space reference identifying the navigation root",
          example = "Content", required = true) @PathVariable String nodeSpace,
      @Parameter(description = "Canonical local document reference identifying the active node",
          example = "Content.MyPage") @RequestParam(required = false) String currentNode,
      @Parameter(description = "Allowed wiki language; defaults to the current request language",
          example = "de") @RequestParam(required = false) String language,
      @Parameter(
          description = "Case-sensitive root part filter; missing or blank returns all parts",
          example = "main") @RequestParam(required = false) String partName,
      @Parameter(description = "Inactive expansion threshold from 0 through 100", example = "2",
          schema = @Schema(defaultValue = "0", minimum = "0", maximum = "100")) @RequestParam(
              name = "show_inactive_to_level", defaultValue = "0") int showInactiveToLevel) {
    var request = requestResolver.resolve(nodeSpace, currentNode, language, partName,
        showInactiveToLevel);
    var response = treeBuilder.build(request);
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL).body(response);
  }

}
