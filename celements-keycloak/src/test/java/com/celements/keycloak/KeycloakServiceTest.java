package com.celements.keycloak;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.spring.security.oauth2.wiki.TenantOicdActiveRequestMatcher;
import com.celements.wiki.WikiDescriptorService;

public class KeycloakServiceTest extends AbstractComponentTest {

  private static final String REALM_PROPERTY = "celements.keycloak.realm";

  private ConfigurationSource configSource;

  @Before
  public void setUp() throws Exception {
    configSource = registerComponentMock(ConfigurationSource.class,
        CelementsFromWikiConfigurationSource.NAME);
    var context = getBeanFactory().getBean(Execution.class).getContext();
    Execution execution = registerComponentMock(Execution.class);
    expect(execution.getContext()).andStubReturn(context);
    execution.removeContext();
    expectLastCall().anyTimes();
  }

  @Test
  public void testConfiguredEndpointsAndHintedWiring() {
    expectRealm(Optional.of("alumni"));
    expect(configSource.containsKey(KeycloakService.BASE_URL_PROPERTY)).andStubReturn(true);
    expect(configSource.getProperty(KeycloakService.BASE_URL_PROPERTY, String.class))
        .andStubReturn("http://idp.alumni.localhost/keycloak///");
    replayDefault();

    var service = getBeanFactory().getBean(KeycloakService.class);
    assertTrue(service.isConfigValid());
    assertEquals("idp.alumni.localhost", service.getHost());
    assertEquals("http://idp.alumni.localhost/keycloak/realms/alumni", service.getIssuerUri());
    String oauthUrl = "http://idp.alumni.localhost/keycloak/realms/alumni/protocol/openid-connect/";
    assertEquals(oauthUrl, service.getOAuth2BaseUrl());
    assertEquals(oauthUrl + "certs", service.getJwkSetUri());
    assertEquals(oauthUrl + "revoke", service.getRevokeUrl());
    assertEquals(oauthUrl + "logout", service.getLogoutUrl());
    assertEquals(DEFAULT_DB + "-login", service.getRegistrationId());

    verifyDefault();
  }

  @Test
  public void testLegacyHostPreservesHttpsEndpoint() {
    expectRealm(Optional.of("production"));
    expect(configSource.containsKey(KeycloakService.BASE_URL_PROPERTY)).andReturn(false);
    expect(configSource.getProperty(KeycloakService.HOST_PROPERTY, "localhost"))
        .andReturn("iam.example.org");
    replayDefault();

    var service = getBeanFactory().getBean(KeycloakService.class);
    assertEquals("https://iam.example.org/realms/production", service.getIssuerUri());

    verifyDefault();
  }

  @Test
  public void testDefaultHostPreservesHttpsEndpoint() {
    expectRealm(Optional.of("production"));
    expect(configSource.containsKey(KeycloakService.BASE_URL_PROPERTY)).andReturn(false);
    expect(configSource.getProperty(KeycloakService.HOST_PROPERTY, "localhost"))
        .andReturn("localhost");
    replayDefault();

    var service = getBeanFactory().getBean(KeycloakService.class);
    assertEquals("https://localhost/realms/production", service.getIssuerUri());

    verifyDefault();
  }

  @Test
  public void testInvalidBaseUrlKeepsAuthenticatedTenantMatched() throws Exception {
    expectRealm(Optional.of("alumni"));
    expect(configSource.containsKey(KeycloakService.BASE_URL_PROPERTY)).andStubReturn(true);
    expect(configSource.getProperty(KeycloakService.BASE_URL_PROPERTY, String.class))
        .andReturn("https://user:secret@[invalid?token=secret")
        .andReturn("http://external.example.org");
    var wikiManager = registerComponentMock(WikiDescriptorService.class);
    expect(wikiManager.isOicdEnabled(new WikiReference(DEFAULT_DB))).andReturn(true).times(3);
    replayDefault();

    var service = getBeanFactory().getBean(KeycloakService.class);
    var matcher = getBeanFactory().getBean(TenantOicdActiveRequestMatcher.class);
    var request = new MockHttpServletRequest("GET", "/protected");
    assertTrue(matcher.matches(request));
    var malformed = assertThrows(IllegalArgumentException.class, service::getIssuerUri);
    assertTrue(malformed.getMessage().startsWith(KeycloakService.BASE_URL_PROPERTY));
    assertNull(malformed.getCause());
    assertTrue(matcher.matches(request));
    assertThrows(IllegalArgumentException.class, service::getJwkSetUri);
    assertTrue(matcher.matches(request));

    verifyDefault();
  }

  @Test
  public void testMissingRealmPreservesInactiveConfiguration() {
    expectRealm(Optional.empty());
    replayDefault();

    var service = getBeanFactory().getBean(KeycloakService.class);
    assertFalse(service.isConfigValid());

    verifyDefault();
  }

  private void expectRealm(Optional<String> realm) {
    expect(configSource.containsKey(REALM_PROPERTY)).andStubReturn(realm.isPresent());
    expect(configSource.getStringProperty(REALM_PROPERTY)).andStubReturn(realm);
  }

}
