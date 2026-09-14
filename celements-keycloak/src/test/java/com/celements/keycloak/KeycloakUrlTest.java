package com.celements.keycloak;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class KeycloakUrlTest {

  private static final String PROPERTY = "celements.keycloak.base_url";

  @Test
  public void testLocalHttpHosts() {
    for (String host : List.of("localhost", "LOCALHOST", "idp.alumni.localhost",
        "IDP.Alumni.LOCALHOST", "127.0.0.1", "127.1.2.3", "127.255.255.255",
        "[::1]", "[0:0:0:0:0:0:0:1]", "[0000:0000:0000:0000:0000:0000:0000:0001]")) {
      String url = "http://" + host + ":8080/auth";
      assertEquals(url, normalize(url));
    }
    assertEquals("HTTP://LOCALHOST", normalize("HTTP://LOCALHOST/"));
  }

  @Test
  public void testExternalAndSpoofedHttpHostsAreRejected() {
    for (String host : List.of("example.org", "keycloak", "192.168.1.1", "10.0.0.1",
        "128.0.0.1", "126.255.255.255", "localhost.example.org", "notlocalhost",
        "evil-localhost", "localhost.", ".localhost", "a..localhost", "-a.localhost",
        "a-.localhost", "127.0.0.1.example.org", "127.1", "2130706433", "0x7f000001",
        "0177.0.0.1", "127.00.0.1", "127.0.0.256", "[::]", "[::2]", "[fe80::1]",
        "[::ffff:127.0.0.1]", "[::127.0.0.1]", "[::1%25lo]", "%6cocalhost")) {
      assertInvalid("http://" + host);
    }
    assertInvalid("http://localhost@evil.example.org");
    assertInvalid("http://localhost%2f.evil.example.org");
    assertInvalid("http://localhost\\@evil.example.org");
  }

  @Test
  public void testHttpsAndPathNormalization() {
    assertEquals("https://iam.example.org", normalize(" https://iam.example.org/// "));
    assertEquals("https://iam.example.org:65535/auth", normalize(
        "https://iam.example.org:65535/auth///"));
    assertEquals("http://localhost:1/auth", normalize("http://localhost:1/a/../auth/"));
    assertEquals("https://iam.example.org/auth%20path", normalize(
        "https://iam.example.org/auth%20path/"));
    assertEquals("HTTPS://iam.example.org", normalize("HTTPS://iam.example.org"));
    assertEquals("https://[2001:db8::1]/auth", normalize("https://[2001:db8::1]/auth"));
  }

  @Test
  public void testInvalidBaseUrls() {
    assertInvalid(null);
    for (String url : List.of("", " ", "idp.example.org", "ftp://idp.example.org",
        "https:///auth", "https://user:secret@idp.example.org",
        "https://idp.example.org?token=secret", "https://idp.example.org#secret",
        "https://idp.example.org:-1/base", "https://idp.example.org:0/base",
        "https://idp.example.org:65536/base", "https://idp.example.org:99999/base",
        "https://user:secret@[invalid?token=secret", "https://idp.example.org/%ZZ")) {
      assertInvalid(url);
    }
  }

  @Test
  public void testAppend() {
    assertEquals("http://localhost/auth/realms/local",
        KeycloakUrl.append(normalize("http://localhost/auth///"), "/realms/local"));
    assertEquals("https://iam.example.org/realms/production",
        KeycloakUrl.append(normalize("https://iam.example.org/"), "realms/production"));
  }

  private String normalize(String value) {
    return KeycloakUrl.normalizeBaseUrl(value, PROPERTY);
  }

  private void assertInvalid(String value) {
    var exception = assertThrows(value, IllegalArgumentException.class, () -> normalize(value));
    assertEquals(PROPERTY
        + " must be an absolute HTTP(S) URL without user info, query, or fragment;"
        + " HTTP is allowed only for localhost, .localhost subdomains, or literal loopback addresses",
        exception.getMessage());
    assertNull(exception.getCause());
  }

}
