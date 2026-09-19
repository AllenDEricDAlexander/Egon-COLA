# Egon COLA Two-Level Cache Starter

`egon-cola-component-common-cache-spring-boot-starter` is a Spring Boot starter implementing a two-level
cache (L1 Guava in-JVM + L2 Redisson `RMapCache` shared) on top of the Spring Cache abstraction, with a
single generic Redis pub/sub change-event channel and a repository-facing cache port.

Cluster coherence uses one envelope and one subscription per node — never a per-cache listener:

```text
writer node ── afterCommit: local dual-level evict + publish(EVICT/PUT/PREFIX_EVICT) ──► Redis topic
                                                                                             │
peer node ◄── EgonColaCacheChangedListener (single SmartLifecycle subscription) ────────────┘
                └── generic EventBus dispatch by operation enum → Manager local primitives
```

> Current document target: Egon COLA `5.3.3`, Java 21+, Spring Boot 3.5.x, Redisson 3.26.x.

---

## 1. Quick start

Prerequisites: the host application already provides exactly one `RedissonClient` bean (typically by the
host's own redisson-spring-boot-starter). This component never creates a Redisson client.

```yaml
egon:
  cola:
    component:
      cache:
        enabled: true
```

With `enabled=true` the autoconfiguration registers three beans:
`egonColaTwoLevelCacheManager` (Spring `CacheManager`), `egonColaCacheChangedListener` (event subscriber),
and `egonColaCachePort` (`top.egon.cola.component.common.core.cache.EgonColaCachePort`, used by the
MyBatis-Plus repository base class for transparent eviction and declarative cached reads).

## 2. Configuration keys

| Key | Default | Meaning |
| --- | --- | --- |
| `egon.cola.component.cache.enabled` | `false` | Master switch; the component is completely inert when absent or `false`. |
| `egon.cola.component.cache.node-id` | `""` (random per JVM) | Origin node id used for self-echo suppression of change events. |
| `egon.cola.component.cache.key-prefix` | `egon:cola:cache` | Redis key namespace for L2 maps, locks and prefix eviction. |
| `egon.cola.component.cache.redis.topic` | `egon:cola:cache:event` | Single pub/sub topic carrying the generic change-event envelope. |
| `egon.cola.component.cache.tenant-mdc-key` | `tenantId` | MDC key holding the current tenant id; read at guard time, fail-closed. |
| `egon.cola.component.cache.second-evict-delay` | `PT5S` | Delay of the second local re-eviction after commit (race shield). |
| `egon.cola.component.cache.l1.max-size` | `10000` | Per-region Guava L1 entry cap. |
| `egon.cola.component.cache.ttl.expire` | `PT30M` | Base TTL for non-null L2 entries (jittered per entry). |
| `egon.cola.component.cache.ttl.null-expire` | `PT60S` | Base TTL for cached null sentinels (penetration shield). |
| `egon.cola.component.cache.ttl.jitter-ratio` | `0.1` | TTL jitter factor in `[0, 0.5]`; one sample is shared by L1 and L2. |
| `egon.cola.component.cache.batch.max-keys` | `1000` | Hard cap for `getAll` batches and prefix-eviction delete chunks. |
| `egon.cola.component.cache.lock.wait-time` | `PT0.5S` | Max wait for the per-key origin lock on cache miss. |
| `egon.cola.component.cache.lock.lease-time` | `PT10S` | Lease of the per-key origin lock (watchdog-free, explicit release). |

## 3. Key and tenancy rules

- Cache keys are always `tenantId:id` (exact: `^\d+:\d+$`); glob patterns are only `tenantId:*`
  (`^\d+:\*$`). Region names match `^[A-Za-z0-9_.-]{1,100}$`.
- Every guard runs at registration time against the MDC tenant and fails closed (missing or non-matching
  tenant context is rejected before any cache or Redis interaction).
- All rejections and degradations use the frozen `CACHE_*` error codes (section 5); they are exception
  message prefixes, never dynamically generated strings.

## 4. Cluster requirements

- All nodes share one Redis and the same `key-prefix`; every enabled node subscribes exactly once to the
  change-event topic via `EgonColaCacheChangedListener` (single `RTopic.addListener` call in production code).
- Envelope: `schemaVersion=1`, `eventId`, `originNodeId`, `occurredAt` (UTC), `cacheName`, `operation`
  (`EVICT` / `PREFIX_EVICT` / `PUT`), `keys`. Deserialization is bound to the envelope type only; the L2
  value codec uses a restricted polymorphic type validator (`top.egon.cola.`, `java.util.`, `java.time.`,
  `java.lang.`).
- `PUT` events drop only the peer's L1 (L2 keeps the authoritative write); `EVICT` drops both levels;
  `PREFIX_EVICT` clears the tenant prefix on both levels. Self-echo events are skipped; redelivered or
  out-of-order events are idempotent.
- A node that misses events still converges within the configured TTL (second-level staleness is accepted;
  hard consistency is not promised).

## 5. Degradation and error codes

Redis outages never propagate to callers on cache paths: operations degrade to authoritative loads or no-ops
with a single log line.

| Code | Where | Semantics |
| --- | --- | --- |
| `CACHE_NAME_INVALID` | guards | Region name violates the shape rule. |
| `CACHE_KEY_TENANT_MISMATCH` | guards | Malformed key, cross-tenant key/glob, or missing/non-numeric MDC tenant (fail-closed). |
| `CACHE_GLOB_PATTERN_FORBIDDEN` | guards | Glob outside `tenantId:*`, or a glob under `EVICT`/`PUT`. |
| `CACHE_KEY_SIZE_EXCEEDED` | `getAll` | Batch larger than `batch.max-keys`. |
| `CACHE_LOADER_REQUIRED` | `get`/`getAll` | Null loader passed. |
| `CACHE_EVENT_UNSUPPORTED_SCHEMA` | envelope ctor | Unknown `schemaVersion` (surfaces inside the `CACHE_EVENT_DESERIALIZE_FAILED` log). |
| `CACHE_EVENT_DESERIALIZE_FAILED` | listener (ERROR) | Payload cannot be parsed or applied; dropped, listener never throws. |
| `CACHE_L2_OPERATION_FAILED` | manager/cache/port (WARN) | Any L2/scheduler/publish operation degraded to no-op. |
| `CACHE_REDISSON_CLIENT_MISSING` | autoconfiguration startup | No host `RedissonClient` resolvable — fail fast, no client is ever created. |

After-commit eviction failures are swallowed in the committing thread (the transaction is already durable);
the delayed second shot re-applies local eviction without republishing, and a shutdown scheduler logs
`CACHE_L2_OPERATION_FAILED` without failing the commit path.

## 6. Zero-impact guarantee

- Without the starter on the classpath, or with `enabled` absent/`false`, no bean is registered and the
  MyBatis-Plus repository base class behaves byte-for-byte like the baseline (no cache port, no subscription,
  no Redis traffic).
- A host-defined `CacheManager` bean makes the whole trio yield (class-level `@ConditionalOnMissingBean`).
- Removing the starter or the `enabled: true` key reverts to baseline without code changes.
