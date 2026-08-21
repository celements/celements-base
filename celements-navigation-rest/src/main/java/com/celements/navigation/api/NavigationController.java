package com.celements.navigation.api;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/v1/navigation")
public class NavigationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationController.class);
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
  @Operation(summary = "Get the current wiki's navigation tree")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Navigation tree"),
      @ApiResponse(responseCode = "400",
          description = "Invalid reference or parameter, or unsupported language",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class))),
      @ApiResponse(responseCode = "404",
          description = "The active node is absent, inaccessible, out of root, or part-excluded",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class))),
      @ApiResponse(responseCode = "500", description = "Navigation infrastructure is unavailable",
          content = @Content(schema = @Schema(implementation = NavigationErrorResponse.class))) })
  public ResponseEntity<NavigationTreeResponse> getNavigation(
      @Parameter(description = "Canonical local space reference") @PathVariable String nodeSpace,
      @Parameter(description = "Canonical local document reference") @RequestParam(
          required = false) String currentNode,
      @Parameter(
          description = "Allowed wiki language; defaults to the request language") @RequestParam(
              required = false) String language,
      @Parameter(
          description = "Case-sensitive root part filter; missing or blank returns all parts")
      @RequestParam(required = false) String partName,
      @Parameter(description = "Inactive expansion threshold from 0 through 100",
          schema = @Schema(defaultValue = "0", minimum = "0", maximum = "100")) @RequestParam(
              name = "show_inactive_to_level", defaultValue = "0") int showInactiveToLevel) {
    var request = requestResolver.resolve(nodeSpace, currentNode, language, partName,
        showInactiveToLevel);
    var response = treeBuilder.build(request);
    return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL).body(response);
  }

  @ExceptionHandler(NavigationApiException.class)
  public ResponseEntity<NavigationErrorResponse> handleNavigationApiException(
      NavigationApiException exception) {
    return errorResponse(exception.status(),
        new NavigationErrorResponse(exception.code(), exception.getMessage()));
  }

  @ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConversionFailedException.class })
  public ResponseEntity<NavigationErrorResponse> handleInvalidParameter() {
    return errorResponse(HttpStatus.BAD_REQUEST,
        new NavigationErrorResponse("invalid_parameter", "The parameter is invalid."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<NavigationErrorResponse> handleUnexpectedException(Exception exception) {
    LOGGER.error("Navigation REST request failed.", exception);
    return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, new NavigationErrorResponse(
        "navigation_unavailable", "Navigation is currently unavailable."));
  }

  private ResponseEntity<NavigationErrorResponse> errorResponse(HttpStatus status,
      NavigationErrorResponse body) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
        .body(body);
  }

}
