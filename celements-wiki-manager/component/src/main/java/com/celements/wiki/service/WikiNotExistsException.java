package com.celements.wiki.service;

public class WikiNotExistsException extends Exception {

  private static final long serialVersionUID = 1L;

  public WikiNotExistsException(String message) {
    super(message);
  }

  public WikiNotExistsException(String message, Throwable cause) {
    super(message, cause);
  }

  public WikiNotExistsException(Throwable cause) {
    super(cause);
  }

}
