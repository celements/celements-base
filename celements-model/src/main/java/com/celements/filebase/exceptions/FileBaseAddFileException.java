package com.celements.filebase.exceptions;

public class FileBaseAddFileException extends Exception {

  private static final long serialVersionUID = 1;

  public FileBaseAddFileException(String message) {
    super(message);
  }

  public FileBaseAddFileException(String message, Throwable cause) {
    super(message, cause);
  }

  public FileBaseAddFileException(Throwable cause) {
    super(cause);
  }

}
