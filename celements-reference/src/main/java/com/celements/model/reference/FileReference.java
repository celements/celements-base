package com.celements.model.reference;

import java.util.Optional;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

public interface FileReference {

  @NotEmpty
  public String getFileName();

  @NotNull
  public Optional<DocumentReference> getDocumentReference();

  @NotNull
  public Optional<String> getQueryString();

  @NotEmpty
  public String getFullPath();

}
