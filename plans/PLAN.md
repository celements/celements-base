# Remote Observation Refactor Plan

## Summary
Refactor cluster observation so remote messages describe the cluster-side work to perform, not the local domain event that caused it.

Use three remote event families only:
- `Invalidate*CacheEvent` for cache/config/runtime refresh.
- `LuceneQueueIndexEvent` / `LuceneQueueDeleteEvent` for search index work.
- `WikiLifecycleRemoteEvent` only where wiki lifecycle work must run on each node.

Business events such as `DocumentDeletedEvent`, `ObjectEvent`, `OrgEvent`, `EventDeletedEvent`, `ArticleUpdatedEvent`, `AffiliationCreatedEvent`, and `SubscribedEventUpdatedEvent` become local-only.

## Key Changes
- Add explicit remote cache events:
  - `InvalidateDocumentCacheEvent(DocumentReference ref, boolean exists)`
  - `FlushDocumentCacheEvent(WikiReference wiki)` for wiki deletion or full wiki cache invalidation.
  - `InvalidateGroupMembershipCacheEvent(WikiReference wiki)`
  - `RefreshWikiConfigEvent`
  - `RefreshGlobalRedirectConfigEvent`
  - `RefreshCelTagsEvent`
  - `RefreshMacroMappingsEvent`
  - `InvalidateNavigationCacheEvent(WikiReference wiki)`
  - `InvalidateTreeNodeCacheEvent(WikiReference wiki)`
  - `InvalidateXObjectPageTypeCacheEvent(WikiReference wiki)`
  - `InvalidateBlogCacheEvent(WikiReference wiki)`

- Keep Lucene remote events, but make them the only remote index mechanism:
  - Keep `LuceneQueueIndexEvent` and `LuceneQueueDeleteEvent`.
  - Keep document/wiki/attachment-to-Lucene converters local-only.
  - Add operation-level deduping/coalescing where possible so one logical change queues at most the necessary final index/delete tasks.

- Replace broad remote document/wiki converters:
  - Stop using `DocumentEventConverter` for cluster communication.
  - Stop using generic `WikiEventConverter` except during migration compatibility.
  - Emit explicit cache/index/lifecycle events from local document/wiki listeners instead.

- Make domain events local-only:
  - Remove `@Remote` from broad business/domain events.
  - Convert listeners that mutate business state to `AbstractLocalEventListener` or add an explicit `!RemoteObservationManagerContext.isRemoteState()` guard.
  - Treat `OrgEvent`, `ObjectEvent`, blog/calendar/article/event/subscription/affiliation events as local workflow events unless a specific remote cache/index event is emitted from them.

- Replace accidental remote consumers:
  - `DocumentCacheStoreListener` listens to `InvalidateDocumentCacheEvent` and `FlushDocumentCacheEvent`.
  - document cache, group cache, wiki config, global redirect, tags, macros, navigation, tree node, page type, and blog listeners listen to their matching explicit invalidation/refresh events.
  - TNA/payment/user/business mutation listeners must not process remote `Document*` replays.

## Implementation Steps
1. Introduce a small remote event package for cluster maintenance events, annotated with `@Remote`, carrying only references/flags, never full `XWikiDocument` or `XWikiContext`.

2. Add local publisher listeners that translate local facts into explicit remote events:
   - local document create/update/delete publishes document cache invalidation and Lucene queue events.
   - local wiki delete publishes document-cache flush and Lucene wiki delete.
   - local config/class/tag/navigation/page-type/blog changes publish the relevant cache refresh event.
   - attachment changes publish Lucene attachment queue events.

3. Update remote maintenance listeners to consume explicit events:
   - cache listeners perform only invalidation/refresh.
   - Lucene queue listener remains the remote index executor.
   - lifecycle listeners run only from explicit lifecycle remote events, if still required.

4. Fence off business logic from remote replay:
   - make all domain mutation listeners local-only.
   - remove remote annotations from domain event classes.
   - add tests proving remote-state replays do not create users, update affiliations, copy collection documents, recompute derived fields, or promote subscriptions.

5. Disable old broad remote distribution behind a compatibility switch:
   - default new behavior: explicit remote events only.
   - temporary fallback property: allow old `DocumentEventConverter`/`WikiEventConverter` during rollout.
   - log warnings when the fallback path sends broad events.

6. After rollout validation, remove the fallback converters and stale `@Remote` annotations from domain events completely.

## Test Plan
- Unit tests for each explicit remote event:
  - serializes/deserializes through `AnnotationEventConverter`.
  - carries only reference/flag data.
  - does not require `XWikiDocument` or `XWikiContext`.

- Listener tests:
  - remote `InvalidateDocumentCacheEvent` invalidates document cache.
  - remote `FlushDocumentCacheEvent` flushes wiki/document cache.
  - remote group/wiki/tag/global redirect/navigation/tree/page-type/blog events refresh only their target caches.
  - remote Lucene queue events queue index/delete tasks exactly once.

- Regression tests for accidental business execution:
  - remote-state document/domain event replay does not trigger user creation, affiliation counterpart writes, subscription promotion, collection propagation, payment/order updates, or derived field writes.
  - local events still trigger existing business workflows.

- Cascade test for the progon collection removal case:
  - local operation may emit local domain events internally.
  - cluster traffic contains only final cache invalidation and Lucene queue events.
  - no remote `OrgEvent`, `ObjectEvent`, `EventDeletedEvent`, or `DocumentDeletedEvent` is sent.

## Assumptions
- Preferred rollout is incremental and compatibility-safe: introduce explicit events first, then disable broad converters after tests pass.
- Remote events should be idempotent and safe to replay.
- Cache invalidation can be coarse where needed, for example wiki-level invalidation, to avoid overfitting the first refactor.
- Lucene queue events are already the right abstraction and should be refined, not replaced.
