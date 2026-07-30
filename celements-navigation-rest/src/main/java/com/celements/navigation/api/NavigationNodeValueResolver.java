package com.celements.navigation.api;

import org.xwiki.model.reference.DocumentReference;

interface NavigationNodeValueResolver {

  String serialize(DocumentReference docRef);

  String resolveTitle(DocumentReference docRef, String language);

  String resolveUrl(DocumentReference docRef, String language);

}
