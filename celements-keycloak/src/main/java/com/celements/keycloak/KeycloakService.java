package com.celements.keycloak;

import static com.celements.execution.XWikiExecutionProp.*;
import static com.celements.logging.LogUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.event.Event;

import com.celements.common.observation.listener.AbstractLocalEventListener;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.model.reference.RefBuilder;
import com.celements.observation.save.SaveEventOperation;
import com.celements.observation.save.object.ObjectEvent;
import com.celements.spring.security.oauth2.IdentityService;
import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class KeycloakService implements IdentityService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakService.class);

  private static final String CELEMENTS_KEYCLOAK_REALM = "celements.keycloak.realm";

  private final Map<String, AuthenticationManager> authManagerCache = new ConcurrentHashMap<>();
  private final Map<String, JwtDecoder> jwtDecoderCache = new ConcurrentHashMap<>();

  private final ConfigurationSource configSource;
  private final Execution execution;

  @Inject
  public KeycloakService(
      @Named(CelementsFromWikiConfigurationSource.NAME) ConfigurationSource configSource,
      Execution execution) {
    this.configSource = configSource;
    this.execution = execution;
    LOGGER.info("KeycloakService constructor: {} host '{}', realm '{}'", configSource.getClass(),
        getHost(), getRealm());
  }

  @Override
  public boolean isConfigValid() {
    return configSource.containsKey(CELEMENTS_KEYCLOAK_REALM)
        && getRealmOpt().isPresent();
  }

  @NotEmpty
  private Optional<String> getRealmOpt() {
    return configSource.getStringProperty(CELEMENTS_KEYCLOAK_REALM);
  }

  @Override
  @NotEmpty
  public String getRealm() {
    return getRealmOpt().orElse(XWikiConstant.MAIN_WIKI.getName());
  }

  @Override
  @NotEmpty
  public String getHost() {
    return configSource.getProperty("celements.keycloak.host", "localhost");
  }

  @Override
  @NotEmpty
  public String getLoginClientId() {
    return configSource.getProperty("celements.keycloak.login_client_id", "unkown");
  }

  @Override
  @NotEmpty
  public String getLoginClientSecret() {
    return configSource.getProperty("celements.keycloak.login_client_secret", "<unkown-secret>");
  }

  @Override
  @NotEmpty
  public String getIssuerUri() {
    return "https://" + getHost() + "/realms/" + getRealm();
  }

  @Override
  @NotEmpty
  public String getOAuth2BaseUrl() {
    return getIssuerUri() + "/protocol/openid-connect/";
  }

  @Override
  @NotEmpty
  public String getJwkSetUri() {
    return getOAuth2BaseUrl() + "certs";
  }

  @Override
  @NotEmpty
  public String getLoginUrl() {
    String loginUrl = "/oauth2/authorization/" + getRegistrationId();
    LOGGER.info("get getLoginUrl for wikiName '{}' returns '{}'",
        defer(() -> getWikiName().orElse(null)), loginUrl);
    return loginUrl;
  }

  @Override
  @NotEmpty
  public String getLogoutSucessUrl() {
    // TODO get celements-logout URL respecting XWikiPreferences or
    // xwiki.cfg config
    return "/";
  }

  @Override
  @NotEmpty
  public String getRegistrationId() {
    Optional<String> wikiNameOpt = getWikiName();
    LOGGER.info("get registrationId for wikiName '{}'", defer(() -> wikiNameOpt.orElse(null)));
    return wikiNameOpt.map(wikiName -> wikiName + "-").orElse("") + "login";
  }

  @Override
  @NotNull
  public AuthenticationManager getAuthenticationManagerForWiki(WikiReference wikiRef) {
    return authManagerCache.computeIfAbsent(wikiRef.getName(),
        this::buildAuthenticationManagerForWiki);
  }

  private AuthenticationManager buildAuthenticationManagerForWiki(String wikiName) {
    LOGGER.info("Building AuthenticationManager for wikiName '{}', jwkSetUri '{}'", wikiName,
        defer(this::getJwkSetUri));
    JwtAuthenticationProvider provider = new JwtAuthenticationProvider(
        NimbusJwtDecoder.withJwkSetUri(getJwkSetUri()).build());
    provider.setJwtAuthenticationConverter(jwtAuthConverter());
    return new ProviderManager(provider);
  }

  @Override
  @NotNull
  public JwtDecoder getJwtDecoder() {
    return jwtDecoderCache.computeIfAbsent(getWikiName().orElseThrow(),
        (String currentWikiName) -> buildJwtDecoderForCurrentWiki());
  }

  /**
   * CAUTION: the JwtDecoder gets generated for the current wiki in the execution context.
   */
  private JwtDecoder buildJwtDecoderForCurrentWiki() {
    LOGGER.info("Building JwtDecoder for wikiName '{}', jwkSetUri '{}'",
        defer(() -> getWikiName().orElse(null)), defer(this::getJwkSetUri));
    return NimbusJwtDecoder.withJwkSetUri(getJwkSetUri()).build();
  }

  private Optional<String> getWikiName() {
    return Optional.ofNullable(execution.getContext())
        .flatMap(eContext -> eContext.get(WIKI))
        .map(WikiReference::getName);
  }

  private JwtAuthenticationConverter jwtAuthConverter() {
    JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
    authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
    authoritiesConverter.setAuthorityPrefix("ROLE_");
    return createJwtAuthenticationCoverter(authoritiesConverter);
  }

  private JwtAuthenticationConverter createJwtAuthenticationCoverter(
      JwtGrantedAuthoritiesConverter authoritiesConverter) {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setPrincipalClaimName("preferred_username");
    converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
    return converter;
  }

  @Component(AuthCacheInvalidationListener.NAME)
  public class AuthCacheInvalidationListener
      extends AbstractLocalEventListener<XWikiDocument, Object> {

    public static final String NAME = "keycloakServiceAuthCacheInvalidationListener";

    private final Logger logger = LoggerFactory.getLogger(AuthCacheInvalidationListener.class);

    @Override
    public List<Event> getEvents() {
      logger.info("getEvents: registering for document update events.");
      return List.of(
          new ObjectEvent(SaveEventOperation.CREATED, getXWikiPreferencesClassRef()),
          new ObjectEvent(SaveEventOperation.UPDATED, getXWikiPreferencesClassRef()));
    }

    @Override
    public String getName() {
      return NAME;
    }

    private ClassReference getXWikiPreferencesClassRef() {
      return RefBuilder.create().space(XWikiConstant.XWIKI_SPACE)
          .doc(XWikiConstant.XWIKI_PREF_DOC_NAME).build(ClassReference.class);
    }

    @Override
    protected void onEventInternal(@NotNull Event event, @NotNull XWikiDocument changedDoc,
        @Nullable Object data) {
      if (Objects.equal(changedDoc.getDocumentReference(),
          context.getXWikiPreferencesDocRef())) {
        logger.trace("changes on {} saved. Invalidating authentication manager cache",
            changedDoc.getDocumentReference());
        authManagerCache.remove(changedDoc.getWikiRef().getName());
        jwtDecoderCache.remove(changedDoc.getWikiRef().getName());
      } else {
        logger.trace("changes on {} saved. NOT invalidating authentication manager cache",
            changedDoc.getDocumentReference());
      }
    }

  }

}
