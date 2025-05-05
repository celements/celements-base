package com.celements.globalredirect;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

public interface IGlobalRedirectService {

  boolean isActivated();

  @NotNull
  DocumentReference getGlobalRedirectDocRef();

  @NotNull
  List<? extends IGlobalRedirect> getGlobalRedirect();

}
