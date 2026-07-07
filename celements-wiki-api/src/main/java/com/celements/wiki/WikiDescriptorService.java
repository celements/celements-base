package com.celements.wiki;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.wiki.exception.WikiDescriptorException;
import com.celements.wiki.exception.WikiMissingException;

public interface WikiDescriptorService {

  @NotNull
  DocumentReference getDescriptorDocRef(@NotNull WikiReference wikiRef);

  @NotNull
  List<WikiDescriptor> getDescriptors(@NotNull WikiReference wikiRef) throws WikiMissingException;

  void createDescriptor(@NotNull WikiReference wikiRef, @NotEmpty String host)
      throws WikiDescriptorException;

  void deleteDescriptors(@NotNull WikiReference wikiRef) throws WikiDescriptorException;

  boolean isOicdEnabled(@NotNull WikiReference wikiRef);

}
