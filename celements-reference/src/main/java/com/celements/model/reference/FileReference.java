package com.celements.model.reference;

import java.io.Serializable;
import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

public interface FileReference extends Serializable {

  @NotEmpty
  public String getFileName();

  @NotNull
  public Optional<DocumentReference> getBaseDocumentReference();

  @NotNull
  public Optional<String> getQueryString();

  @NotEmpty
  public String getFullPath();

}
