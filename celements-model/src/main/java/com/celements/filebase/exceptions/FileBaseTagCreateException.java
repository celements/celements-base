package com.celements.filebase.exceptions;

public class FileBaseTagCreateException extends Exception {

  private static final long serialVersionUID = 1L;

  public FileBaseTagCreateException(String message) {
    super(message);
  }

  public FileBaseTagCreateException(String message, Throwable cause) {
    super(message, cause);
  }

}
