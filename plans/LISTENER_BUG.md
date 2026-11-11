I found one very plausible loop family, plus two structural risks that can turn normal fan-out into a cluster storm.

**Most Likely Loop**
`LuceneQueueIndexEvent` can escape the remote replay guard because indexing happens asynchronously.

The important chain is:

1. A node receives remote `LuceneQueueIndexEvent`.
2. [QueueEventListener](/mnt/mlsdata/workspace/celements-features/celements-search/src/main/java/com/celements/search/lucene/observation/QueueEventListener.java) handles remote queue events and queues Lucene work.
3. [IndexUpdater](/mnt/mlsdata/workspace/celements-features/celements-search/src/main/java/com/xpn/xwiki/plugin/lucene/IndexUpdater.java) later performs the index update on a scheduler thread.
4. Because that later task is no longer inside `remoteState`, it emits local Lucene completion events like `LuceneDocumentIndexedEvent`.
5. [PerformanceIndexEventConverter](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/realisation/observation/index/PerformanceIndexEventConverter.java) converts those into `PerformanceIndexedEvent`.
6. [AllPerformanceIndexedListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/realisation/observation/index/AllPerformanceIndexedListener.java) and [SubscribedPerformanceIndexedListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/subscription/observation/SubscribedPerformanceIndexedListener.java) react by queueing more Lucene work.
7. Those new `LuceneQueueIndexEvent`s are remote again.

The suspicious detail: both derived queue paths use `.queue()` with notifications still enabled.

In particular:

- [AllPerformanceIndexedListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/realisation/observation/index/AllPerformanceIndexedListener.java) queues collection copies after a `PerformanceIndexedEvent`.
- [SubscriptionService.promoteIndexedEventToSubscriber](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/subscription/SubscriptionService.java) queues subscriber documents after a `PerformanceIndexedEvent`.

That contrasts with [UpdateAllCollectionIndexFieldListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/realisation/observation/index/UpdateAllCollectionIndexFieldListener.java), which already calls `.withoutNotifications()`. So the codebase already has the right idea in one place, but not in the likely fan-out paths.

This is the first thing I would fix or instrument.

**Second Candidate**
The subscription copy path can also fan out through document saves.

Relevant chain:

1. [SubscriptionEventConverter](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/subscription/observation/SubscriptionEventConverter.java) listens to local `OrgEvent`s and emits `SubscribedEventCreatedEvent`, `SubscribedEventUpdatedEvent`, or `SubscribedEventDeletedEvent`.
2. Those events are `@Remote`.
3. [SubscribedEventListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/subscription/observation/SubscribedEventListener.java) promotes events to subscriber wikis.
4. [EventCopyService](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/collection/event/EventCopyService.java) saves or deletes copied documents.
5. Those saves create new `Document*`, `Object*`, `OrgEvent`, and Lucene queue events.

The listener is local-only, so direct remote replay is skipped, but the local saves it performs are ordinary local changes. In cyclic subscription setups, or when copied documents are repeatedly considered changed, this can bounce or repeatedly amplify.

Also, `SubscribedEvent*` being `@Remote` looks questionable. If the only real remote goals are cache invalidation and Lucene indexing, these domain events should probably not cross the cluster at all.

**Structural Risks**
There are two framework-level issues that make storms harder to reason about.

First, there are two components named `observation.remote`:

- [celements LocalEventListener](/mnt/mlsdata/workspace/celements-core/src/main/java/com/celements/common/observation/listener/LocalEventListener.java)
- [upstream LocalEventListener](/mnt/mlsdata/workspace/celements-base/celements-observation/src/main/java/org/xwiki/observation/remote/internal/LocalEventListener.java)

The celements one skips `@Local` events before sending remote. The upstream one does not. We should verify which implementation is actually active at runtime. If the upstream one wins, supposedly-local events are less protected.

Second, [SerializableEventConverter](/mnt/mlsdata/workspace/celements-base/celements-observation/src/main/java/org/xwiki/observation/remote/internal/converter/SerializableEventConverter.java) can send any serializable event/source/data combination as a fallback. That weakens the design rule “only `@Remote` events are remote”.

**Why Remote-State Does Not Fully Save Us**
[DefaultRemoteObservationManager](/mnt/mlsdata/workspace/celements-base/celements-observation/src/main/java/org/xwiki/observation/remote/internal/DefaultRemoteObservationManager.java) does prevent immediate rebroadcast while replaying a remote event. That part is good.

The likely hole is async work: remote event arrives, listener queues Lucene work, remote replay finishes, then async Lucene indexing emits fresh local events. Those fresh local events can be broadcast again.

**Recommended Next Steps**
1. Add storm logging around remote send/receive with event class, source ref, data type, origin node, and maybe a correlation id. Start with [KafkaNetworkAdapter](/mnt/mlsdata/workspace/celements-base/celements-observation/src/main/java/com/celements/observation/remote/kafka/KafkaNetworkAdapter.java), [QueueEventListener](/mnt/mlsdata/workspace/celements-features/celements-search/src/main/java/com/celements/search/lucene/observation/QueueEventListener.java), and [IndexUpdater](/mnt/mlsdata/workspace/celements-features/celements-search/src/main/java/com/xpn/xwiki/plugin/lucene/IndexUpdater.java).

2. Suppress notifications for derived index queueing from `PerformanceIndexedEvent`, especially in:
   - [AllPerformanceIndexedListener](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/realisation/observation/index/AllPerformanceIndexedListener.java)
   - [SubscriptionService](/mnt/mlsdata/workspace/progon-core/src/main/java/ch/programmonline/subscription/SubscriptionService.java)

3. Treat `PerformanceIndexedEvent` as a local completion signal, not as a trigger for more cluster-visible Lucene queue events unless explicitly intended.

4. Remove or stop using `@Remote` on subscription/domain propagation events unless there is a concrete remote-side consumer that must run.

5. Verify the active `observation.remote` component. The runtime must use the celements listener that skips `@Local`, or equivalent filtering should be added.

6. Consider disabling or heavily constraining `SerializableEventConverter`. Remote distribution should be explicit, not an accidental fallback.

My strongest bet is the Lucene queue/index-completion/progon indexed-listener chain. It has the exact shape of a delayed remote loop: the direct remote guard works, but async index completion re-enters the event system as local work and can broadcast again.
