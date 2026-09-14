package com.celements.keycloak;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import com.google.common.net.InetAddresses;

final class KeycloakUrl {

  private KeycloakUrl() {}

  static String normalizeBaseUrl(String value, String propertyName) {
    if ((value == null) || value.isBlank()) {
      throw invalid(propertyName);
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
        throw invalid(propertyName);
      }
      if ("http".equalsIgnoreCase(scheme) && !isLocalHost(uri.getHost())) {
        throw invalid(propertyName);
      }
      String normalized = uri.toString();
      while (normalized.endsWith("/")) {
        normalized = normalized.substring(0, normalized.length() - 1);
      }
      return normalized;
    } catch (URISyntaxException exc) {
      throw invalid(propertyName);
    }
  }

  static String append(String baseUrl, String path) {
    return baseUrl + (path.startsWith("/") ? path : "/" + path);
  }

  private static boolean isLocalHost(String host) {
    String lowerHost = host.toLowerCase(Locale.ROOT);
    if (lowerHost.equals("localhost") || lowerHost.endsWith(".localhost")) {
      return true;
    }
    if (host.startsWith("[") && host.endsWith("]")) {
      String address = host.substring(1, host.length() - 1);
      return !address.contains("%") && InetAddresses.isInetAddress(address)
          && InetAddresses.toAddrString(InetAddresses.forString(address)).equals("::1");
    }
    return host.startsWith("127.") && InetAddresses.isInetAddress(host);
  }

  private static IllegalArgumentException invalid(String propertyName) {
    return new IllegalArgumentException(propertyName
        + " must be an absolute HTTP(S) URL without user info, query, or fragment;"
        + " HTTP is allowed only for localhost, .localhost subdomains, or literal loopback addresses");
  }

}
