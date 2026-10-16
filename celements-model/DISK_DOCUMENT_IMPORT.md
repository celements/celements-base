# Disk document overwrite configuration

Disk XML documents normally create missing documents and translations while leaving existing
ones alone. Operators can make selected documents authoritative from disk on each import by
shipping `/docs/<wikiname>.json` in the servlet application resources. For example, for wiki
`example`, `/docs/example.json` contains:

```json
["XWiki.XWikiPreferences", "Configuration.Defaults"]
```

Use literal, canonical, wiki-relative document FullNames, including the space. Names are case
sensitive. The existing model parser supports one space and a document name, with escaped
delimiters. JSON adds its own escaping (for example `"Space.Page\\.WithDot"`).
`"Outer\\.Inner.Page"` names the literal space `Outer.Inner`, not a nested space hierarchy.
Unescaped multi-dot names such as `Outer.Inner.Page` are noncanonical and rejected; this
configuration does not expand the baseline parser's grammar. Duplicate entries have no additional
effect. Blank names, surrounding whitespace, cross-wiki references, wildcard and regex selectors
are rejected. Names are never trimmed. An entry selects all disk translations of that document,
but only XML documents actually present in the import are installed. Unused entries create nothing.

The configuration belongs to the actual target wiki, including documents supplied by `/docs/all/`.
It never selects a document in another wiki. JSON files are excluded from package entries and do
not discover new wikis. Resources are accessed through `ServletContext`; an exploded deployment
or a filesystem real path is unnecessary.

| Global force | Document/language exists | Listed | Result |
| --- | --- | --- | --- |
| true | either | either | Overwrite/install |
| false | no | either | Install missing document/language |
| false | yes | yes | Overwrite |
| false | yes | no | Skip |

A missing configuration or `[]` grants no extra overwrites. Each wiki invocation reads and fully
validates its configuration once before any disk document installation, then uses an immutable
snapshot across all paths. The next invocation reloads it. Changing the resource during an
invocation does not change that invocation's decisions. The importer injects the concrete
`JsonConfigOverwriteFromDisk` component and prepares one JDK `Predicate<DocumentReference>` for
the target wiki before enumerating installation paths. The predicate closes over an immutable set;
the component keeps no mutable per-wiki policy cache.

A present malformed, unreadable or invalid resource fails that wiki invocation before installation,
including when global force is true or there are no installation paths. The exception identifies
the resource and a safe validation reason without echoing configuration contents. Existing outer
initialization error handling remains in place: check import errors and resulting stored values;
initialization completion alone does not prove installation succeeded.

## Replacement and runtime visibility

Selection uses the existing Package `ACTION_OVERWRITE`, with `preserveVersion=true`,
`withVersions=false`, `backupPack=false`, and the existing package-definition/edit-rights checks
disabled. It supplies a complete XML document rather than applying a property merge. Include the
intended class definition, objects, properties, content and metadata in the XML. Actual store
verification replaced content, title and class definition: the old class field disappeared and the
disk field was present. The old self-object property and an unrelated object's custom property
remained in the tested configured store. This is neither a field-merge policy nor a guarantee that
every stored structure is erased or preserved across stores.

A repeated selected import restored disk content after an operator edit, kept the document identity
and retained earlier revisions while history grew. Author, content author and creator were observed
as the import operator; creation date was retained. These are observed Package/store rules, not a
promise to preserve disk authors/dates or avoid new revisions.

Review and back up affected data before shipping authoritative XML. Keep ownership of that
configuration explicit: an operator edit to a listed document may be replaced on the next applicable
import. Removing an entry restores normal skip behavior for an existing document, but does not
undo prior imports. The mandatory-document initializer continues to invoke the importer with
global force false.

Stored document changes and already-created runtime components can have different visibility.
The owning application/configuration subsystem must use its supported invalidation or restart
procedure where required. This importer adds no automatic restart, lifecycle replay, SQL repair,
HTTP endpoint or consumer-specific policy.

## Verification

Run focused tests with:

```sh
mvn -o -f celements-model/pom.xml test -Dtest=DiskDocumentImporterTest,JsonConfigOverwriteFromDiskTest
```

`DiskDocumentImporterStoreIT` additionally exercises actual Package installation, model reads and
Celements Hibernate persistence through the configured `modelAccess -> docCache -> celHibernate`
chain and real Hibernate versioning. Actual verification passed persisted replacement, preservation,
controls, metadata, history and repeat-import assertions on MariaDB 10.1.48 in one default-language test wiki with
class-table reads/writes disabled. This scope does not establish behavior for every store or backend
image; the raw-primary-store fallback remains separately unproved. Stored values do not by themselves
prove runtime configuration/cache visibility; use the owning subsystem's supported invalidation or
restart procedure where required.

The revised opt-in store test creates tables only in a fresh owned disposable fixture and refuses
existing tables. See [STORE_TEST.md](STORE_TEST.md) for provisioning, guards and evidence retention.
The current MariaDB 10.1 fixture and create-only guard path passed native verification; the earlier
MariaDB 10.11 verification remains a separate historical result. Separate safety review and explicit
run authorization are required; this documentation authorizes no runtime or consumer lifecycle action.
Without its opt-in properties the test is skipped, which is not store acceptance.
