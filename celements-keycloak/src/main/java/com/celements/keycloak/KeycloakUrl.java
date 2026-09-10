package com.celements.keycloak;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

final class KeycloakUrl {

  private KeycloakUrl() {}

  static String normalizeBaseUrl(String value, String propertyName) {
    if ((value == null) || value.isBlank()) {
      throw invalid(propertyName, value, null);
    }
    try {
      URI uri = new URI(value.trim()).normalize();
      String scheme = uri.getScheme();
      if ((scheme == null)
          || !(scheme.toLowerCase(Locale.ROOT).equals("http")
              || scheme.toLowerCase(Locale.ROOT).equals("https"))
          || (uri.getHost() == null)
          || (uri.getPort() == 0)
          || (uri.getPort() > 65535)
          || (uri.getRawUserInfo() != null)
          || (uri.getRawQuery() != null)
          || (uri.getRawFragment() != null)) {
        throw invalid(propertyName, value, null);
      }
      String normalized = uri.toString();
      while (normalized.endsWith("/")) {
        normalized = normalized.substring(0, normalized.length() - 1);
      }
      return normalized;
    } catch (URISyntaxException exc) {
      throw invalid(propertyName, value, exc);
    }
  }

  static String append(String baseUrl, String path) {
    return baseUrl + (path.startsWith("/") ? path : "/" + path);
  }

  private static IllegalArgumentException invalid(String propertyName, String value,
      Exception cause) {
    return new IllegalArgumentException(propertyName
        + " must be an absolute HTTP(S) URL without user info, query, or fragment: " + value,
        cause);
  }

}
