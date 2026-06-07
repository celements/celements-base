package com.celements.filebase.exceptions;

public class FileBaseTagDeleteException extends Exception {

  private static final long serialVersionUID = 1L;

  public FileBaseTagDeleteException(String message) {
    super(message);
  }

  public FileBaseTagDeleteException(String message, Throwable cause) {
    super(message, cause);
  }

}
