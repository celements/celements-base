# Remove `com.xpn.xwiki.XWiki`

## Goal

Remove the 5,915-line `com.xpn.xwiki.XWiki` class without replacing it with another
god object.

Use `XWiki` temporarily as a compatibility facade:

1. delete unused methods;
2. migrate methods that already have replacements;
3. move remaining responsibilities into cohesive Spring components;
4. leave deprecated delegates only where compatibility requires them;
5. move bootstrap and state ownership out of `XWiki`;
6. remove `XWiki` after its Java and scripting consumers have migrated.

This plan concerns the internal `com.xpn.xwiki.XWiki` class. The
`com.xpn.xwiki.api.XWiki` scripting facade may remain for `$xwiki` compatibility, but it must
eventually use services directly instead of wrapping the internal class.

## Constraints and decisions

- Do not create one service per method. The inventory groups methods by responsibility; each
  group may need no new service, one service, or a small number of focused services.
- Prefer an existing service or component over a new abstraction.
- A replacement is not complete if it injects `XWikiProvider`, calls
  `XWikiContext#getWiki()`, or accepts `XWiki`.
- Migrate one vertical slice at a time: introduce or complete the owner, migrate callers and
  tests, then reduce `XWiki` to a deprecated delegate for that slice.
- Preserve exact legacy behavior at the delegate boundary, including exception translation,
  context restoration, notification order, and scripting behavior.
- Do not add an interface merely because an implementation is a Spring component.
- An interface in `celements-xwiki-core` with its implementation in `celements-model` is a valid
  way to avoid a Maven cycle when the legacy `XWiki` facade must call model-layer logic.

## Module placement

`celements-model` already depends on `celements-xwiki-core`. Therefore
`celements-xwiki-core` must not depend on `celements-model`.

Choose placement in this order:

1. Reuse an existing service visible to `celements-xwiki-core`.
2. If the responsibility is genuinely core-level, put a concrete Spring component in
   `celements-xwiki-core`.
3. If the implementation belongs in `celements-model` but `XWiki` needs to delegate to it:
   - define the narrow service interface in `celements-xwiki-core`;
   - implement it as a Spring component in `celements-model`;
   - make only the deprecated `XWiki` adapter perform a static Spring lookup;
   - inject the interface normally in all new code.

Before using option 3, verify that every runtime which creates `XWiki` contains the
`celements-model` implementation. Focused `celements-xwiki-core` tests must register a mock or test
implementation when they exercise the delegate.

For new, unhinted services, plain Spring type lookup is sufficient. Add XWiki
`@ComponentRole`/hint metadata only if an actual hinted lookup contract requires it.

## Completed inventory

### Inventory basis

The inventory was generated from the current
`celements-xwiki-core/target/classes/com/xpn/xwiki/XWiki.class`. The class file was newer than
`XWiki.java` at inventory time.

- Public methods: **328**
- Public constructors: **2**
- Unique public method names: **241**
- Existing `@Deprecated` annotations in `XWiki.java`: **83**

Methods below are grouped by name. `xN` means that all `N` public overloads are included in that
entry. The categories account for all 328 public methods exactly. Constructors are listed
separately under lifecycle.

The usage scan covered 15,747 Java and relevant resource files in the checked-out Celements,
Progon, Synventis, TNA, Staempfli, Helvetia, Veranstalter, DODIS, and BWX trees. It excluded build
output, dependencies, generated frontend output, and files over 2 MB.

Because privileged scripts can obtain the internal object through `getXWiki()`, absence from this
source scan does not prove runtime absence. Delete candidates require the additional scripting and
production-document audit described below.

### Summary

| Category | Public methods | Action |
|---|---:|---|
| Unused | 16 | Delete after the dynamic scripting audit |
| Existing replacement | 53 | Migrate callers; do not create another service |
| Missing responsibility | 180 | Extract into cohesive owners |
| Compatibility-only API | 36 | Retain temporarily as deprecated delegates |
| Bootstrap/state/lifecycle | 43 | Move during the final phase |
| **Total** | **328** | |

### Unused: delete

These methods have no checked-source call outside `XWiki`, and any internal occurrence is either
absent, inert, or only supports another candidate in this list:

- `checkActive` x2
- `copyWiki`
- `generateValidationKey`
- `getDocLanguagePreferenceNew`
- `getHttpTimeout`
- `getHttpUserAgent`
- `getVirtualWikiList`
- `getVirtualWikiCache`
- `hasCaptcha`
- `isExo`
- `isLDAP`
- `isTest`
- `setTest`
- `getThreadMap`
- `setThreadMap`

Additional evidence:

- `getVirtualWikiList` always returns an empty list.
- `getVirtualWikiCache` returns a new empty cache and already points callers to `WikiService`.
- `isExo` and `isLDAP` always return `false`.
- `isTest`/`setTest` document the removed Cactus test mode.
- `getThreadMap`/`setThreadMap` document themselves as removed; `setThreadMap` is also a no-op
  because it assigns the parameter to itself.

Deletion gate:

- search checked-in Java, Velocity, Groovy, XWiki XML documents, and application scripts;
- inspect known database-backed programming scripts for `$xwiki.getXWiki()`,
  `$context.getXWiki()`, and reflective calls;
- remove any scripting facade method that exposes the candidate;
- then delete the method instead of creating a replacement.

### Existing replacement: migrate callers

#### Configuration and current context

- `Param` x2, `ParamAsLong` x2, `getConfig`, `getEncoding`, `isVirtualMode`
  -> `XWikiConfigSource`
- `getDatabase`, `getDefaultLanguage`
  -> `ModelContext` / the typed execution-context wiki property

#### Wiki and request references

- `getVirtualWikisDatabaseNames`
  -> `WikiService#streamAllWikis`
- `getServerURL`
  -> `WikiService#streamUrisForWiki`
- `getServerWikiPage`
  -> `WikiDescriptorService#getDescriptorDocRef`
- `stripSegmentFromPath`
  -> keep as a private helper in the request filter which needs it, or use the URL/path utility
  already owning that parsing

#### Model and query access

- `exists` x2
  -> `IModelAccessFacade#exists`
- `getClass`
  -> typed `getXClass`/model access
- `getSpaces`, `getSpaceDocsName`
  -> `ModelUtils`
- `search` x4
  -> `QueryExecutor` or `QueryExecutionService`
- `renamePage` x3
  -> `XWikiDocument#rename`

#### URLs

- `getURL` x5, `getExternalURL` x2, `getAttachmentURL` x2,
  `getExternalAttachmentURL`
  -> `UrlService`
- `getURLFactoryService`
  -> inject or look up the existing URL component directly

Before declaring URL migration complete, remove the current back-edge in `XWikiUrlService`, which
still reaches `context.getXWikiContext().getWiki()`.

#### Users, authentication, rights, and mail

- `checkAuth`
  -> `XWikiAuthService`
- `checkAccess`
  -> the rights facade / `XWikiRightService`
- `createUser` x2, `createEmptyUser`, `getUser` x2, `sendConfirmationEmail`,
  `sendValidationEmail` x2
  -> `UserService`
- `sendMessage` x2
  -> the mail-sender component
- `generateRandomString`
  -> `RandomStringUtils` or the generator already used by `UserService`

#### Cache and syntax configuration

- `getCacheFactory`, `getLocalCacheFactory`
  -> `CacheManager`
- `getConfiguredSyntaxes`, `getDefaultDocumentSyntax`
  -> `CoreConfiguration` and the syntax-1.0-only policy

### Compatibility-only API: retain as deprecated delegates

Do not design new general-purpose APIs around these signatures. Migrate known callers to their
real owners, keep the old method only for a compatibility window, and mark non-deprecated methods
in this section deprecated when a delegate exists.

#### Legacy text and UI helpers

- `getFormEncoded`, `getXMLEncoded`, `getTextArea`, `getHTMLArea`
- `getFlash`
- `addTooltip` x2, `addTooltipJS`, `addMandatory`

Prefer existing escaping/rendering utilities. If exact legacy output must remain available to
scripts, put only that behavior in a narrowly named compatibility component.

#### Legacy date helpers

- `getCurrentDate`, `getDate`, `getTimeDelta`

Java callers should use `Date`/`java.time` directly. Keep delegates only while the scripting facade
still exposes these methods.

#### Old overloads and naming

- `ProtectUserPage`
  -> `protectUserPage`
- `getDocLanguagePreference`
  -> `getLanguagePreference`
- `getDocumentName`, `getDocumentNameFromPath`
  -> typed document-reference methods
- `getDocName`
  -> typed reference parsing
- `getWebAppPath`
  -> servlet/application context; Celements currently always returns `/`

#### Methods copied from the old compatibility aspect

- `getURLEncoded`
  -> `Util#encodeURI`
- `isVirtual`
  -> `XWikiConfigSource#isVirtualMode`
- `getCacheService`
  -> `CacheManager`
- `getWebCopyright`
  -> `getSpaceCopyright`
- `getWebPreference` x3, `getWebPreferenceAsInt` x2, `getWebPreferenceAsLong` x2
  -> the corresponding space-preference methods
- `copyWikiWeb` x2
  -> `copySpaceBetweenWikis`
- `getDefaultWeb`
  -> `getDefaultSpace`
- `useDefaultWeb`
  -> `skipDefaultSpaceInURLs`
- `getMessage`, `parseMessage` x2
  -> `XWikiMessageTool`

### Missing responsibility: extract

These 180 methods are not 180 proposed services. They are grouped below into candidate
responsibilities. Confirm each boundary while migrating its real callers.

#### Resource and filesystem access: 13 methods

- `ParamAsRealPath` x2, `ParamAsRealPathVerified`
- `getRealPath`
- `getResource`, `getResourceAsStream`, `getResourceContent`,
  `getResourceContentAsBytes`, `getResourceLastModificationDate`, `resourceExists`
- `getTempDirectory`, `getWorkDirectory`, `getWorkSubdirectory`

Candidate owner: a core resource/environment component. Keep configuration lookup in
`XWikiConfigSource`; do not create a second configuration service.

#### Document access, history, classes, and validation: 25 methods

- `getDocument` x5, `getDocumentFromPath`
- `saveDocument` x3
- `deleteDocument` x2, `deleteAllDocuments` x2
- `getDeletedDocuments`, `getDeletedDocument`,
  `getDeletedAttachments` x2, `getDeletedAttachment`
- `rollback`
- `refreshLinks`
- `getClassList`, `getXClass`, `getPropertyClassFromName`
- `getObjectFromRequest`
- `validateDocument`

Candidate owners:

- extend or reuse model access for ordinary load/save/delete;
- a document-history/recycle-bin component for deleted data and rollback;
- a class-access component for XClass operations;
- keep request decoding outside document persistence.

This is the main candidate for a narrow interface in `celements-xwiki-core` with an implementation
in `celements-model`. The interface must expose the modern contract, not simply copy all legacy
`XWikiContext` signatures. The `XWiki` adapter converts legacy arguments and exceptions.

Mixed-family notes:

- `getDocument` includes two already-deprecated string overloads. They remain compatibility
  delegates; the typed implementation moves behind the service.
- `deleteDocument` and `saveDocument` must preserve legacy notification behavior until all callers
  use model access directly.

#### Document and space copying: 17 methods

- `copyDocument` x15
- `copySpaceBetweenWikis` x2

Candidate owner: a document-copy component, probably implemented in `celements-model` behind a
core-defined interface.

Eight `copyDocument` string overloads are already deprecated. Keep those as delegates to the typed
reference methods; do not reproduce all overloads on the new interface.

#### Store policy and maintenance: 17 methods

- `updateDatabase` x3
- `flushCache` x2
- `getFullNameSQL` x2, `isMySQL`
- `hasBacklinks`, `hasTags`, `hasCustomMappings`, `hasDynamicCustomMappings`
- `hasVersioning` x2, `hasAttachmentVersioning`
- `hasRecycleBin`, `hasAttachmentRecycleBin`

Candidate owners:

- store/schema initialization for `updateDatabase`;
- a cache-maintenance component for `flushCache`;
- store capabilities/configuration for the `has*` methods;
- remove database-vendor SQL helpers if their callers can use the owning store/query component.

Do not make a single generic `XWikiStoreService` containing all of these.

#### Request routing and document naming: 10 methods

- `getDocumentReference`, `getDocumentReferenceFromPath`
- `prepareDocuments`
- `setPhonyDocument` x2
- `getServletPath`
- `clearName` x2
- `getUniquePageName` x2

Candidate owners:

- request/document resolution in the web/action layer;
- a small document-name utility only if more than one real caller remains.

The string `setPhonyDocument` overload is already deprecated and should remain only as a delegate
to the typed overload.

#### Rendering, templates, skin, and Groovy: 27 methods

- `parseContent`
- `evaluateTemplate`, `parseTemplate` x2, `renderTemplate` x2
- `include` x2
- `getSkin`, `getSkinFile` x4, `getIconURL`
- `getBaseSkin` x3, `getDefaultBaseSkin`
- `getIncludedMacros`, `getMacroList`
- `prepareResources`
- `parseGroovyFromString` x2, `parseGroovyFromPage` x2
- `getCurrentContentSyntaxId` x2

Candidate owners:

- template/rendering component;
- skin-resource component;
- Groovy execution component;
- content-syntax component.

Reuse the current rendering engine, Velocity renderer, URL service, and `CoreConfiguration` before
adding a new component. Preserve script-right checks and request-bound Velocity context behavior.

One `parseTemplate` overload and one `include` argument-order overload are already deprecated;
those remain compatibility delegates only.

#### Preferences, language, and feature policy: 45 methods

- `getXWikiPreference` x3, `getXWikiPreferenceAsInt` x3,
  `getXWikiPreferenceAsLong` x3
- `getSpacePreference` x3, `getSpacePreferenceAsInt` x2,
  `getSpacePreferenceAsLong` x2, `getSpaceCopyright`
- `getUserPreference` x2, `getUserPreferenceAsInt`, `getUserPreferenceAsLong`,
  `getUserPreferenceFromCookie`
- `getLanguagePreference`, `getInterfaceLanguagePreference`, `isMultiLingual`
- `getSkinPreference` x2
- `getDefaultSpace`, `skipDefaultSpaceInURLs`, `getDefaultPage`,
  `showViewAction`, `useDefaultAction`
- `getEditorPreference`, `getWysiwygToolbars`
- `hasEditComment`, `hasMinorEdit`, `isEditCommentFieldHidden`,
  `isEditCommentSuggested`, `isEditCommentMandatory`
- `hasSectionEdit`, `getSectionEditingDepth`
- `getMaxRecursiveSpaceChecks`
- `getConvertingUserNameType`, `hasCentralizedAuthentication`
- `isTitleInCompatibilityMode`

Candidate owners:

- one preference service which owns wiki -> space -> user -> cookie fallback;
- typed configuration consumers for feature-specific flags.

The preference service is another strong candidate for an interface in
`celements-xwiki-core` with its document-backed implementation in `celements-model`.
Reuse the existing wiki, space, and user `ConfigurationSource` components, but keep the legacy
precedence rules in one place.

Do not turn every boolean preference into a method on a replacement god service. Move each
feature-specific flag to its consumer when there is only one consumer.

#### Mandatory XClasses: 12 methods

- `initializeMandatoryClasses`
- `getTagClass`, `getSheetClass`, `getUserClass`, `getRedirectClass`, `getPrefsClass`,
  `getGroupClass`, `getRightsClass` x2, `getGlobalRightsClass`, `getCommentsClass`,
  `getSkinClass`

Candidate owner: the existing mandatory-XClass initializer mechanism. Complete the existing
`MainXClassInitializer`/class-compositor direction instead of adding a new umbrella class service.
Access to an existing XClass should use model/class access; initialization should be startup or
wiki-lifecycle work.

#### User compatibility behavior: 11 methods

- `validateUser`
- `protectUserPage`
- `convertUsername`
- `getUserName` x3, `getLocalUserName` x3
- `getUserTimeZone`
- `formatDate`

Candidate owners:

- extend `UserService` where the behavior is genuinely user lifecycle or lookup;
- a user-display/date-formatting component only if multiple non-legacy callers remain;
- move rights-object creation behind the user creation workflow.

Do not make the model implementation call back through `XWikiProvider` to reach auth, rights,
groups, preferences, or document access.

#### Legacy plugin access: 2 methods

- `getPlugin`
- `getPluginApi`

Keep behind the plugin manager during migration. New code must inject the real component rather
than resolve a plugin through `XWiki`.

#### Wiki-owner lookup: 1 method

- `getWikiOwner`

Candidate owner: `WikiDescriptorService` or another cohesive wiki-descriptor component. Do not add
a standalone one-method service if the descriptor service can own it.

### Bootstrap, state, and lifecycle: final phase

These 43 methods expose or mutate the global runtime aggregate and are handled only after normal
callers use components directly.

#### Bootstrap and event listener

- `loadPlugins`
- `resetRenderingEngine`
- `onEvent`, `getEvents`, `getName`

#### Global metadata and flags

- `getVersion`, `setVersion`
- `isReadOnly`, `setReadOnly`
- `getEngineContext`
- `getUrlPatternMatcher`, `setUrlPatternMatcher`
- `getMetaclass`, `setMetaclass`

#### Store state

- `getStore`, `setStore`
- `getNotCacheStore`, `getHibernateStore`
- `getAttachmentStore`, `setAttachmentStore`
- `getAttachmentVersioningStore`, `setAttachmentVersioningStore`
- `getVersioningStore`, `setVersioningStore`
- `getRecycleBinStore`, `setRecycleBinStore`
- `getAttachmentRecycleBinStore`, `setAttachmentRecycleBinStore`

#### Service state

- `getRenderingEngine`, `setRenderingEngine`
- `getPluginManager`, `setPluginManager`
- `getNotificationManager`, `setNotificationManager`
- `getAuthService`, `setAuthService`
- `getRightService`, `setRightService`
- `getGroupService`, `setGroupService`
- `getCriteriaService`, `setCriteriaService`
- `setConfig`

#### Constructors

- `XWiki()`
- `XWiki(boolean init)`

Target:

- Spring owns stores, rendering, auth, rights, groups, plugins, and configuration directly.
- `CelementsBootstrap` coordinates initialization without constructing `new XWiki(true)`.
- The document observation behavior becomes its own listener bean.
- Request execution context contains typed request/wiki/document/user state, not the `XWiki`
  aggregate.

Do not replace this state with a new `XWikiEngineService` aggregate.

## Implementation plan

### 1. Inventory and freeze growth

Status: **completed in this plan**.

- Keep this inventory synchronized while methods move or disappear.
- Reject new production calls to `XWiki`, `XWikiProvider`, and `XWikiContext#getWiki()`.
- Reject new public methods on `XWiki`.
- Prefer source checks or existing static-analysis infrastructure; do not add a new dependency only
  for this rule.

### 2. Delete and migrate before extracting

1. Complete the scripting/production-document audit for the 16 unused candidates.
2. Delete confirmed-unused methods.
3. Migrate the 53 methods with existing replacements, one owner at a time.
4. Mark compatibility methods deprecated as soon as their replacement is usable.
5. Do not wait for the whole class before removing obsolete call sites and tests.

Recommended first slices:

1. configuration and syntax;
2. wiki and URL access;
3. query and ordinary model access;
4. users/auth/rights/mail;
5. compatibility-aspect aliases.

### 3. Extract missing responsibilities

For each responsibility:

1. Trace all Java and scripting callers.
2. Confirm the target owner and module.
3. Add or extend the smallest service contract needed by modern callers.
4. Move the implementation without changing behavior.
5. Migrate production callers and tests to injected services.
6. Make the old `XWiki` method a deprecated delegate.
7. Verify that the new implementation has no dependency on `XWiki`, `XWikiProvider`, or
   `XWikiContext#getWiki()`.
8. Remove the delegate after the compatibility window.

Suggested extraction order:

1. resource/filesystem access;
2. document access, history, copying, and XClass access;
3. preferences and language;
4. request/document resolution;
5. skin, templates, rendering, and Groovy;
6. user compatibility behavior;
7. plugins.

### 4. Decouple the scripting facade

Refactor `com.xpn.xwiki.api.XWiki` so it:

- receives or looks up the relevant services;
- no longer stores `private com.xpn.xwiki.XWiki xwiki`;
- no longer exposes the internal object through `getXWiki()`;
- preserves supported `$xwiki` methods by delegating to services;
- removes old compatibility methods after checked-in and database-backed scripts migrate.

Also update the Velocity, Groovy, and script-context initializers which currently construct the
scripting facade with the internal object.

### 5. Remove global `XWiki` state

Remove, in this order:

1. service/store getters from ordinary callers;
2. `XWikiContext#getWiki()` and `setWiki(...)`;
3. `XWikiProvider`;
4. `XWikiExecutionProp.XWIKI`;
5. the servlet-context `xwiki.instance` future;
6. `CelementsBootstrap#bootstrapXWiki()` construction of `new XWiki(true)`;
7. the remaining lifecycle methods and constructors;
8. `com.xpn.xwiki.XWiki`.

`CelementsBootstrap` should coordinate concrete initialization components and publish the existing
started events. It should not own a new aggregate replacement.

### 6. Deprecation and removal policy

- Deprecate individual methods as soon as an actionable replacement exists.
- Do not mark the whole class `forRemoval = true` while bootstrap, context, or the scripting facade
  legitimately requires it.
- Mark the class deprecated once all normal Java callers have migrated and the documented path is
  services plus the compatibility scripting facade.
- Mark it `forRemoval = true` only after downstream repositories and database-backed scripts have
  completed the migration.
- Remove it in a release which explicitly permits the binary break.

## Verification

For every vertical slice:

- focused component tests for the new owner;
- characterization tests for non-trivial legacy behavior;
- adapter tests only where argument/exception conversion is non-trivial;
- no test per trivial one-line delegate;
- search for remaining production calls to the migrated methods;
- search the new implementation for `XWikiProvider`, `getWiki()`, and `XWiki` parameters/fields;
- run the owning module tests and affected downstream tests.

Before class removal:

- no production Java import of `com.xpn.xwiki.XWiki`;
- no production `XWikiContext#getWiki()` call;
- no Spring component depends on `XWikiProvider`;
- `com.xpn.xwiki.api.XWiki` has no internal `XWiki` field;
- no execution-context or servlet-context property stores `XWiki`;
- no checked-in or known database-backed script calls `getXWiki()`;
- no production subclass of `XWiki`;
- complete `celements-xwiki-core`, `celements-model`, webapp, and representative downstream test
  suites pass.

## Definition of done

- `com.xpn.xwiki.XWiki` is deleted.
- Its responsibilities are either deleted, handled by an existing owner, or moved into focused
  components.
- No replacement component acts as a renamed global aggregate.
- The supported `$xwiki` scripting surface remains functional without the internal class.
- Bootstrap and request execution use typed Spring components and typed execution-context
  properties directly.
