package com.celements.navigation.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = NavigationController.class)
public final class NavigationExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(NavigationExceptionHandler.class);

  @ExceptionHandler(NavigationApiException.class)
  public ResponseEntity<NavigationErrorResponse> handleNavigationApiException(
      NavigationApiException exception) {
    return response(exception.status(),
        new NavigationErrorResponse(exception.code(), exception.getMessage()));
  }

  @ExceptionHandler({ MethodArgumentTypeMismatchException.class, ConversionFailedException.class })
  public ResponseEntity<NavigationErrorResponse> handleInvalidParameter() {
    return response(HttpStatus.BAD_REQUEST,
        new NavigationErrorResponse("invalid_parameter", "The parameter is invalid."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<NavigationErrorResponse> handleUnexpectedException(Exception exception) {
    LOGGER.error("Navigation REST request failed.", exception);
    return response(HttpStatus.INTERNAL_SERVER_ERROR, new NavigationErrorResponse(
        "navigation_unavailable", "Navigation is currently unavailable."));
  }

  private ResponseEntity<NavigationErrorResponse> response(HttpStatus status,
      NavigationErrorResponse body) {
    return ResponseEntity.status(status)
        .header(HttpHeaders.CACHE_CONTROL, NavigationController.CACHE_CONTROL).body(body);
  }

}
