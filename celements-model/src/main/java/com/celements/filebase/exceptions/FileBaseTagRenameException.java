package com.celements.filebase.exceptions;

public class FileBaseTagRenameException extends Exception {

  private static final long serialVersionUID = 1L;

  public FileBaseTagRenameException(String message) {
    super(message);
  }

  public FileBaseTagRenameException(String message, Throwable cause) {
    super(message, cause);
  }

}
