package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.WikiReference;

@ComponentRole
public interface ServerUrlUtilsRole {

  @NotNull
  URL getServerURL() throws MalformedURLException;

  @NotNull
  Optional<URL> getServerURL(@NotNull WikiReference wikiRef) throws MalformedURLException;

}
