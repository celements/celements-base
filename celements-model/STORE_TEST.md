# Disposable Package/store verification

`DiskDocumentImporterStoreIT` is opt-in and uses the real Package/model/cache/Hibernate/history
path. It requires Linux, `/usr/bin/docker`, a user-owned rootless Docker socket at
`/run/user/<uid>/docker.sock`, Docker Compose, the cached build dependencies and the
official `mariadb:10.1` image. Provisioning writes a new volume; the test creates mapped tables and
saves documents/history in `celdev1355_test`. Use only a fresh disposable fixture, never an
application database. Provisioning and running require separate explicit operator authorization.

After review/authorization, choose a fresh UUID as `STORE_IT_TOKEN` and your rootless daemon's
numeric UID as `STORE_IT_UID`. Export both for Compose. A unique project name such as
`disk-import-<UUID>` keeps fixture instances separate. From the repository root:

```sh
docker --host unix:///run/user/<uid>/docker.sock compose --project-name disk-import-<UUID> --file celements-model/src/test/resources/disk-import-store.compose.yaml up -d --pull never
```

Record the full ID of the newly created `celements-disk-import-<UUID>` container and retain the
Compose configuration, container/volume identities and logs. Confirm MariaDB finished initialization
before the one test invocation. Use a new, absent report directory outside historical reports:

```sh
mvn -o -f celements-model/pom.xml test -Dtest=DiskDocumentImporterStoreIT -Dcelements.storeIT.token=<UUID> -Dcelements.storeIT.containerId=<full-container-ID> -Dceldev1355.storeAcceptance=FIX-STORE-01 -Dsurefire.reportsDirectory=<fresh-absolute-report-directory>
```

The 10.1 image uses the legacy `MYSQL_ALLOW_EMPTY_PASSWORD`, `MYSQL_ROOT_HOST` and
`MYSQL_DATABASE` initialization variables in the supplied Compose file. No newer image healthcheck
is assumed. The test requires a `10.1.x-MariaDB` server version; `10.11` is rejected. This is the
verified fixture target with the existing Hibernate/driver versions: the scoped native test passed
on MariaDB 10.1.48. Bind the acquired immutable image ID/digest during safety review;
the mutable `10.1` tag alone does not pin bytes.

The test accepts no JDBC URL or port override. Before JDBC it checks the current user's socket,
full container ID, UUID/name/ownership labels, pinned image/hostname, sole loopback port, bridge
attachment, restart policy and exclusive matching labelled local volume. The port is discovered
from that container, not an arbitrary endpoint. Before any schema write, the same physical JDBC
connection verifies schema/hostname/server version and that there are no tables. It then creates
the mappings. An existing table, failed identity check or SQL error stops the test; there is no
DROP, reset, automatic retry or cleanup. Reusing the fixture after a completed or partial run is
deliberately rejected. Missing opt-in token skips the test; a skip is not acceptance.

The module explicitly binds `surefire.reportsDirectory` to the plugin's `reportsDirectory`
parameter, defaulting to `target/surefire-reports`. Surefire 3.5.4 has no native user-property
expression for this parameter; the small project binding makes the isolated override above work.

Keep the container, volume and fresh XML/text/output reports for inspection on success or failure.
Cleanup belongs to the owner after evidence has been retained and deletion explicitly authorized:
recheck the exact UUID/full ID/labels and exclusive volume, then remove only that fixture's
container and volume. Do not use broad prune, an unrelated Compose project or a reused token.
No cleanup command is part of the test. A new run needs a fresh token/fixture and separate authority.

The current MariaDB 10.1.48 run passed the fresh-fixture/create-only guards and actual persisted
Package/store assertions. Content, title and class definitions were replaced; old self-object
properties and unrelated custom data remained. Controls, missing documents, identity, import-operator
metadata, retained creation date, real history and repeated import after an operator edit passed.
This covers one default-language synthetic wiki with class tables disabled; it does not prove every
backend, raw-primary-store fallback or consumer/runtime configuration visibility. See
`DISK_DOCUMENT_IMPORT.md` for operator ownership and supported invalidation/restart requirements.
The earlier MariaDB 10.11 pass remains a separate historical result. A completed or partial fixture
is consumed; every future run still needs a fresh fixture, review and explicit authorization.
