package com.celements.keycloak;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;

public class KeycloakServiceTest {

  @Test
  public void testHttpBaseUrlWithPathAndTrailingSlashes() {
    KeycloakService service = newService(Map.of(
        KeycloakService.BASE_URL_PROPERTY, "http://idp.alumni.localhost/keycloak///",
        "celements.keycloak.realm", "alumni"));
    assertTrue(service.isConfigValid());
    assertEquals("idp.alumni.localhost", service.getHost());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni", service.getIssuerUri());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni/protocol/openid-connect/",
        service.getOAuth2BaseUrl());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni/protocol/openid-connect/certs",
        service.getJwkSetUri());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni/protocol/openid-connect/revoke",
        service.getRevokeUrl());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni/protocol/openid-connect/logout",
        service.getLogoutUrl());
  }

  @Test
  public void testHttpsBaseUrl() {
    KeycloakService service = newService(Map.of(
        KeycloakService.BASE_URL_PROPERTY, "https://iam.example.org",
        "celements.keycloak.realm", "production"));
    assertEquals("https://iam.example.org/realms/production", service.getIssuerUri());
  }

  @Test
  public void testLegacyHostPreservesHttpsEndpoint() {
    KeycloakService service = newService(Map.of(
        KeycloakService.HOST_PROPERTY, "iam.example.org",
        "celements.keycloak.realm", "production"));
    assertEquals("https://iam.example.org/realms/production", service.getIssuerUri());
  }

  @Test
  public void testBaseUrlOverridesLegacyHost() {
    KeycloakService service = newService(Map.of(
        KeycloakService.BASE_URL_PROPERTY, "http://localhost:8080/auth/",
        KeycloakService.HOST_PROPERTY, "iam.example.org",
        "celements.keycloak.realm", "local"));
    assertEquals("http://localhost:8080/auth/realms/local", service.getIssuerUri());
  }

  @Test
  public void testInvalidBaseUrls() {
    for (String baseUrl : List.of("", "idp.example.org", "ftp://idp.example.org",
        "https:///auth", "https://user@idp.example.org", "https://idp.example.org?x=1",
        "https://idp.example.org#fragment", "https://idp.example.org:-1/base",
        "https://idp.example.org:0/base",
        "https://idp.example.org:65536/base", "https://idp.example.org:99999/base")) {
      KeycloakService service = newService(Map.of(
          KeycloakService.BASE_URL_PROPERTY, baseUrl,
          "celements.keycloak.realm", "alumni"));
      assertFalse(baseUrl, service.isConfigValid());
      try {
        service.getIssuerUri();
        fail("Expected invalid base URL: " + baseUrl);
      } catch (IllegalArgumentException expected) {
        assertTrue(expected.getMessage().contains(KeycloakService.BASE_URL_PROPERTY));
      }
    }
  }

  private KeycloakService newService(Map<String, String> properties) {
    return new KeycloakService(new MapConfigurationSource(properties), null);
  }

  private static class MapConfigurationSource implements ConfigurationSource {

    private final Map<String, String> properties;

    MapConfigurationSource(Map<String, String> properties) {
      this.properties = new HashMap<>(properties);
    }

    @Override
    public <T> T getProperty(String key, T defaultValue) {
      return properties.containsKey(key) ? cast(properties.get(key)) : defaultValue;
    }

    @Override
    public <T> T getProperty(String key, Class<T> valueClass) {
      return valueClass.cast(properties.get(key));
    }

    @Override
    public <T> T getProperty(String key) {
      return cast(properties.get(key));
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(String value) {
      return (T) value;
    }

    @Override
    public List<String> getKeys() {
      return List.copyOf(properties.keySet());
    }

    @Override
    public boolean containsKey(String key) {
      return properties.containsKey(key);
    }

    @Override
    public boolean isEmpty() {
      return properties.isEmpty();
    }

  }

}
