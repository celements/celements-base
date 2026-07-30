package com.celements.navigation.api;

import org.springframework.http.HttpStatus;

final class NavigationApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;
  private final String code;

  NavigationApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  HttpStatus status() {
    return status;
  }

  String code() {
    return code;
  }

}
