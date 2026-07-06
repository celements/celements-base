package com.celements.wiki;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

public interface WikiDescriptorService {

  @NotNull
  DocumentReference getDescriptorDocRef(@NotNull WikiReference wikiRef);

  boolean isOicdEnabled(@NotNull WikiReference wikiRef);

}
