package com.celements.globalredirect;

import java.util.List;

import org.xwiki.model.reference.DocumentReference;

public interface IGlobalRedirectService {

  boolean isActivated();

  DocumentReference getGlobalRedirectDocRef();

  List<IGlobalRedirect> getGlobalRedirect();

}
