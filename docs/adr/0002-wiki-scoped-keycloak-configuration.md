# ADR 0002: Wiki-scoped Keycloak configuration

## Status

Accepted on 2026-09-13.

## Context

The reusable Celements Keycloak adapter needs one configuration for every Space in a Wiki. Existing
Keycloak settings are stored in each environment's `XWikiPreferences`, and Wiki administrators are
trusted to configure Keycloak destinations within their own Wiki. In the customer deployment that
prompted [ALU-190](https://github.com/progonline/progon-customizing/pull/199), each customer has its
own Keycloak sidecar in its server-app installation. Those deployment details do not define a
universal Celements trust policy.

The adapter already injects `CelementsFromWikiConfigurationSource`. Its effective precedence is Wiki
preferences, `celements.properties`, then `xwiki.properties`; it does not include Space preferences.
`CelementsDefaultConfigurationSource` includes Space preferences. Despite its Javadoc,
`CelementsAllPropertiesConfigurationSource` is file-only in the implementation: it reads
`celements.properties` before `xwiki.properties` and no Wiki preferences.

## Decision drivers

- One Wiki must use the same Keycloak configuration across all its Spaces.
- Existing Wiki-level settings must remain effective without moving them to files.
- The adapter must not embed customer-specific realm, sidecar address, or credential values.
- The security boundary must be explicit because the configured destination receives authentication
  and administrative token requests.

## Considered options

### `CelementsFromWikiConfigurationSource`

Preserves Wiki-level configuration and file fallbacks while excluding Space overrides. It gives a
trusted Wiki administrator control of the destination for that Wiki.

### `CelementsAllPropertiesConfigurationSource`

Limits configuration to deployment-controlled files, but requires moving existing Wiki preferences
into files and removes Wiki administrator control. Its current implementation is file-only despite
the misleading Javadoc.

### `CelementsDefaultConfigurationSource`

Keeps Wiki preferences and file fallbacks, but also permits Space preferences to override the
destination, violating the one-configuration-per-Wiki requirement.

## Decision

The reusable Celements Keycloak adapter uses `CelementsFromWikiConfigurationSource` for Keycloak
settings. Wiki preferences take precedence over `celements.properties`, then `xwiki.properties`;
Space preferences must not override them. This records the adapter's existing source choice; it does
not assert a new code change.

This decision assumes that Wiki administrators are trusted to configure Keycloak destinations for
their own Wiki. Deployments **MUST assess that trust assumption** before adopting this source choice.
The adapter's configuration-source policy is separate from deployment-specific realm names, sidecar
addresses, and credentials; those values belong in each deployment's configuration.

## Consequences

### Positive

- Existing `XWikiPreferences` values remain effective, with file-level fallbacks.
- All Spaces in a Wiki resolve the same Wiki-level Keycloak configuration.
- The reusable adapter remains independent of customer-specific topology and values.

### Negative and security risk

- A trusted Wiki administrator can redirect authentication and administrative token requests to a
  destination they control, potentially exposing client secrets or tokens. This is an intentional
  trust boundary, not a claim that all Celements deployments trust Wiki administrators.
- Deployments without that trust boundary cannot safely rely on Wiki-editable destinations. Their
  rollback/alternative is to move the required values to deployment-controlled properties and
  switch the adapter to the file-only `CelementsAllPropertiesConfigurationSource`; that change must
  be reviewed and tested for the deployment's credential and fallback behavior.

## Source references

- [`KeycloakService`](../../celements-keycloak/src/main/java/com/celements/keycloak/KeycloakService.java)
- [`CelementsFromWikiConfigurationSource`](../../celements-config-source/src/main/java/com/celements/configuration/CelementsFromWikiConfigurationSource.java)
- [`CelementsAllPropertiesConfigurationSource`](../../celements-config-source/src/main/java/com/celements/configuration/CelementsAllPropertiesConfigurationSource.java)
- [`CelementsDefaultConfigurationSource`](../../celements-config-source/src/main/java/com/celements/configuration/CelementsDefaultConfigurationSource.java)
