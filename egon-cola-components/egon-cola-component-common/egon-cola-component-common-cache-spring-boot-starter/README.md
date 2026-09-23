# Egon COLA Spring Cache Starter

[English](README.md) | [中文](README.zh-CN.md)

Native Spring Cache annotations + L1 Guava + L2 Redisson `RMapCache`, with the L1 of every node
invalidated through one Redis Pub/Sub channel. Targets Java 21 / Spring Boot 3.5.x. mp-sd-ext owns
persistence only; the caching policy is declared by the business methods of a concrete Repository.
The starter reuses the Spring Cache SPI and the existing two-level cache implementation, and adds no
custom cache annotation, no SpEL resolver and no business aspect.

## 1. Enablement

This is a required component: `enabled` defaults to `true` and no extra switch is needed to wire it.
The starter only consumes the `RedissonClient` bean the host provides and never creates a client:
resolution prefers the bean named `redissonClient` and falls back to a sole candidate when that name
is absent. With no candidate, or several candidates and none under the agreed name, startup fails
(`CACHE_REDISSON_CLIENT_MISSING`) instead of silently degrading to a single-level cache.
`spring-boot-starter-cache` arrives transitively with this starter, while the Redisson client
dependency is declared by the host. Example configuration:

```yaml
egon:
  cola:
    component:
      cache:
        enabled: true
        key-prefix: egon:cola:cache:v2:${spring.application.name}
        ttl:
          l1-expire: PT5M
          l1-jitter: PT2M
          l2-expire: PT1H
          l2-jitter: PT20M
          null-expire: PT60S
        regions:
          UserPO:
            l1-expire: PT10M
            l2-expire: PT30M
          UserSearch:
            l1-expire: PT2M
            l2-expire: PT5M
```

`key-prefix` must isolate application, domain and cache version so that different services never
share one L2/lock namespace. Expiry and jitter for L1 and L2 are four independent fields rather than
one shared TTL plus a ratio, and `null-expire` controls the null sentinel on its own.

Enable Spring's AOP caching explicitly on a host configuration class, and point the switch at this
component's key rather than at the host's own Redis switch:

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = EgonColaCacheProperties.PREFIX, name = "enabled",
        havingValue = "true", matchIfMissing = true)
@EnableCaching(proxyTargetClass = true)
public class CacheConfiguration {
}
```

The auto-configuration registers `egonColaTwoLevelCacheManager` ahead of Boot's
`CacheAutoConfiguration`. When the host already provides any `CacheManager`, the whole
auto-configuration steps aside; with several managers, choose one explicitly through
`@CacheConfig(cacheManager = "egonColaTwoLevelCacheManager")` or the `cacheManager` attribute on each
annotation. When mp-sd-ext is wired at the same time, stepping aside is not a silent downgrade: if
`cache.enabled` is at its default of true and another `CacheManager` exists in the context, the
persistence-side tenant scoping decision fails with `CACHE_MANAGER_INCOMPATIBLE`. That failure does
not trigger when `enabled=false` or when the context has no `CacheManager` at all (slice contexts).
`@EnableCaching` and `enabled=true` are two independent switches; registering the manager alone does
not enable annotation interception.

## 2. mp-sd-ext integration

`EgonColaRepository` no longer offers `getByCache`, `listByCache`, `getCachePortProvider` or
`cacheRegionName`, and no longer invalidates the cache implicitly inside its CRUD write methods.
Former callers move to annotated methods on the concrete Repository; this is a deliberate API change.
A business service calls those public, non-final methods through an injected Repository bean:

```java
@Repository
@CacheConfig(cacheNames = "UserPO")
public class UserRepository extends EgonColaRepository<UserDAO, UserPO> {
    // Constructor-injected mapper, validator, tenant provider and configuration,
    // as in the existing implementation.

    @Cacheable(keyGenerator = "egonColaRepositoryKeyGenerator", sync = true)
    public UserPO findCachedById(@NotNull @Positive Long id) {
        return getById(id);
    }

    @CacheEvict(keyGenerator = "egonColaRepositoryKeyGenerator", condition = "#result")
    public boolean updateCachedById(@NotNull UserPO entity) {
        return updateById(entity);
    }

    @CacheEvict(allEntries = true, condition = "#result")
    public boolean updateCachedBatch(Collection<UserPO> entities) {
        return updateBatchById(entities);
    }
}
```

Cache annotations belong on concrete business entry points; the final CRUD methods on the base class
keep their existing data validation, tenant and transaction guards. Never annotate a final or
private method and expect CGLIB to intercept it. Batch writes remain wrapped by the caller's
transaction. Every insert, update, delete and custom-SQL path that touches cached data must declare
the matching eviction, because calling plain CRUD no longer evicts anything.
Single-key reads and writes all use `egonColaRepositoryKeyGenerator`: it accepts one `Long` id or one
`EgonModel` argument and produces a trusted `tenant:id`, so the read, update and delete entry points
share the same region key by construction. The id must be positive, and when the tenant carried by
the entity disagrees with the current context the generator rejects it with
`CACHE_KEY_TENANT_MISMATCH`. Multi-parameter and collection/array batch signatures are not supported
by this strategy. For batch reads, never store a `List<PO>` under one id key of a single-entity
region; if the whole batch should be cached, give it its own region, normalize id order and
duplicates, and evict that region on write.

## 3. Annotations, SpEL and combined operations

Spring performs the standard parsing of `@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching` and
`@CacheConfig`. Prefer `#p0` / `#a0` to avoid depending on parameter names; `#id`,
`#root.methodName`, `#root.target`, static methods and `@beanName` are supported as well. For
single-key repository entry points, prefer `keyGenerator` (see §2 and §4) over spelling out tenant
SpEL at every call site.

```java
@CacheConfig(cacheNames = "UserByCode", cacheManager = "egonColaTwoLevelCacheManager")
public class UserLookupRepository {
    // keyFactory is a bean registered by the host; it returns a stable string
    // prefixed with the current tenant.
    @Cacheable(key = "@keyFactory.byCode(#p0)",
               condition = "#p0 != null && !#p0.isBlank()", unless = "#result == null")
    public UserPO findByCode(String code) { /* query the database and return the PO */ }

    @Caching(
        put = @CachePut(cacheNames = "UserPO",
                       key = "T(org.slf4j.MDC).get('tenantId') + ':' + #result.id",
                       unless = "#result == null"),
        evict = {
            @CacheEvict(cacheNames = "UserByCode", allEntries = true),
            @CacheEvict(cacheNames = "UserSearch", allEntries = true)
        })
    public UserPO updateAndReload(UserPO entity) { /* re-read and return the full PO after a successful update */ }
}
```

The snippet shows where the annotations go and omits the business implementation. `@CachePut` caches
the **method return value**, so it cannot be placed directly on `updateById`, which returns `boolean`
— that would write a boolean into a PO cache. After updating some fields, do not cache the
incomplete argument as though it were a full PO; prefer eviction, or re-read and return.

- `condition` decides whether a call takes part in caching; `@Cacheable` evaluates it before the
  invocation and therefore cannot see `#result`.
- `unless` vetoes the cache write after execution and can see `#result`; `@CacheEvict` has no
  `unless`, so evicting after a successful write uses `condition = "#result"`.
- `@CacheEvict` runs after a successful return by default; `beforeInvocation=true` evicts
  immediately, even when the business call fails or the transaction rolls back, and cannot use
  `#result`.
- `@Caching` groups several put or evict operations. By this project's convention it never contains a
  `@Cacheable`, so that "skip execution on a hit" is never mixed with a write operation.
- `sync=true` accepts exactly one cache, forbids `unless`, and cannot be combined with other cache
  operations. Do not use `@Caching` to work around those limits.

References: [Spring cache annotations](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html),
[`Cacheable` API](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/cache/annotation/Cacheable.html).

## 4. Keys and tenant boundaries

Every key must be a `tenantId:businessKey` string, for example `41:7`, `41:code:alice`,
`41:query:page:1`. The tenant prefix is numeric, the business segment is non-empty, and neither may
contain `*` or control characters; region names match `[A-Za-z0-9_.-]{1,100}`. Spring's default
`SimpleKey` and `Long` keys do not satisfy this convention. Single-key repository entry points must
use `keyGenerator = "egonColaRepositoryKeyGenerator"`, while other shapes (composite query keys) are
carried by a host-registered named `KeyGenerator` or by SpEL that returns the string form above.
Composite arguments need an unambiguous encoding covering every dimension that changes the result,
such as ordering, paging and permission scope; never concatenate uncontrolled separators.

Every Cache SPI access, cache hits included, validates the tenant configured in MDC; cross-tenant
reads or writes and a missing context are not allowed. `@CacheEvict(allEntries=true)`,
`Cache.clear()` and `Cache.invalidate()` **clear only the current tenant's view of that region** and
leave other tenants untouched. That semantics is the constraint this component adds for multi-tenant
regions and differs from clearing the physical region globally. Cross-tenant work must set the
context per tenant. Exact keys and `tenantId:*` eviction events continue to share one channel.
Complete the upgrade of every node before enabling business string keys, because older nodes still
reject a non-numeric business segment.

## 5. Nulls, avalanche and breakdown

- **Penetration**: null is cached by default as an internal sentinel under the shorter `null-expire`,
  and may be combined with `sync=true`. `unless = "#result == null"` only means "do not cache empty
  results"; repeated queries for missing data still reach the source, so it is not penetration
  protection on its own. Where empty results must not be cached, add a bloom filter at the business
  entry point. The business owns initialization, incremental updates and the database query after a
  false positive; the starter never infers the full data set.
- **Avalanche**: each write samples `[l1-expire, l1-expire + l1-jitter]` and
  `[l2-expire, l2-expire + l2-jitter]` separately, and the actual lifetime of the entry written to L1
  is truncated by the sampled L2 result, so no local copy outlives the shared value.
  `regions.<cacheName>` overrides the global TTL field by field across those five keys; a field left
  out inherits the global configuration. Each level's base TTL must be positive, jitter must not be
  negative, and `base + jitter` must still be expressible in milliseconds, otherwise binding is
  rejected. The null sentinel uses `null-expire` and gets no jitter.
- **Breakdown**: `@Cacheable(sync=true)` calls `Cache.get(key, Callable)`. Overlapping loads for the
  same key are merged locally, and while Redis is healthy a distributed lock plus a second query are
  added on top. On a lock-wait timeout or a Redis failure the call falls back to the source without
  populating the cache, and the locally merged requests still share that one result. The distributed
  lock has a bounded lease, so past the lease or the wait time, or during a Redis failure, single
  execution across the cluster is not guaranteed. Size `lock.wait-time` / `lock.lease-time` to the
  worst-case database duration, and do not treat `sync=true` as a business mutex.

## 6. Transactions, cluster consistency and manual access

Whenever a real Spring transaction and transaction synchronizer are present, `put`, `evict` and
`clear` execute after commit and are discarded on rollback; without a transaction they execute
immediately. Reads inside a transaction bypass the cache and loaded results are published only after
commit, so other transactions never observe an uncommitted value. Synchronous loads inside a
transaction are not merged across transactions. When a cache write must live in the same transaction,
the outer service opens that transaction, or the transaction advisor is ordered ahead of the cache
advisor. `beforeInvocation=true` uses the immediate eviction path and keeps Spring's native behavior.

The writing node modifies L2 and publishes the event; the other nodes only invalidate L1, so a
duplicate or late event cannot delete the newer L2 value. When an L2 hit backfills L1, the expiry is
`min(read start + sampled L1 lifetime, read start + remaining L2 lifetime)`, which never extends the
shared value's lifetime; if the L2 remainder is unknown or non-positive nothing is backfilled and the
read goes to the source as a miss. Reading itself does not refresh L2. Lost Pub/Sub messages,
concurrent source loads and write races can still serve a value that is stale within its TTL, so this
cache does not promise strong consistency. Redis read, write and lock acquisition or release failures
degrade through the existing `CACHE_L2_OPERATION_FAILED` log. On shutdown the component disposes of
its internal scheduler and leaves the host's RedissonClient open.

A `this.findCachedById()` call within the same class does not pass through the Spring proxy. Prefer
having an external bean call the annotated entry point; for a more complex flow, operate manually
through the same manager:

```java
Cache cache = cacheManager.getCache("UserPO");
UserPO value = cache.get(tenantId + ":" + id, () -> repository.getById(id));
cache.evict(tenantId + ":" + id);
```

That route keeps tenant validation, two-level synchronization and post-commit handling. The host may
use `RedisTemplate` for its own caches in a **separate namespace**, but must not modify this
component's `RMapCache` Redis keys directly: internal structure, serialization, TTL metadata and the
L1 event protocol have to be maintained together. A direct `RedisTemplate.delete` neither clears the
L1 of the other nodes nor inherits this component's transaction semantics.

For compatibility with existing manual callers the `egonColaCachePort` bean and common-core's
`EgonColaCachePort` are retained, although mp-sd-ext and the scaffolding no longer inject it.
`second-evict-delay` applies only to the delayed post-commit eviction of that legacy port; the
annotation path uses the Cache SPI semantics described above.

## 7. Configuration

All keys live under `egon.cola.component.cache`:

| Key                                                | Default                 | Purpose                                    |
|--------------------------------------------------|-------------------------|--------------------------------------------|
| `enabled`                                        | `true`                  | Required-component switch; wired by default  |
| `node-id`                                        | `""`                    | Ignores the event echo of this node          |
| `key-prefix`                                     | `egon:cola:cache`       | L2 and lock namespace, templated per application/version |
| `redis.topic`                                    | `egon:cola:cache:event` | Single event channel                         |
| `tenant-mdc-key`                                 | `tenantId`              | Current tenant context                       |
| `l1.max-size`                                    | `10000`                 | Local capacity per region                    |
| `ttl.l1-expire` / `ttl.l1-jitter`                | `PT5M` / `PT2M`         | L1 base TTL and jitter bound                 |
| `ttl.l2-expire` / `ttl.l2-jitter`                | `PT1H` / `PT20M`        | L2 base TTL and jitter bound                 |
| `ttl.null-expire`                                | `PT60S`                 | Null-sentinel base TTL (no jitter)           |
| `regions.<name>.l1-expire/l1-jitter/l2-expire/l2-jitter/null-expire` | inherit global | Region-level overrides            |
| `batch.max-keys`                                 | `1000`                  | Legacy port batch-read limit and batch delete size |
| `lock.wait-time`                                 | `PT0.5S`                | Maximum distributed-lock wait                |
| `lock.lease-time`                                | `PT10S`                 | Distributed-lock lease                       |
| `second-evict-delay`                             | `PT5S`                  | Legacy port delayed second eviction          |

When `node-id` is left empty, the manager generates `node-<UUID>` the first time it is needed and
reuses it within the process; an explicitly configured value is used verbatim.

Unknown-field tolerance is disabled for all property binding: the legacy shared `ttl.expire` /
`ttl.jitter-ratio` keys, including their region-level overrides, fail at startup with
`The elements [...] were left unbound.` instead of being silently ignored, so a rolling release can
never leave old and new nodes on different TTL semantics.

The L2 codec keeps the restricted type allow-list: `top.egon.cola.`, `java.util.`, `java.time.`,
`java.lang.`. A host object outside the allow-list cannot be used as an L2 value directly, and an L1
hit is no evidence that Redis serialization would succeed.

## 8. Verification

`EgonColaCacheAnnotationTest` uses a real Spring cache proxy and a mocked Redis, covering annotations,
SpEL, conditions, combined operations, tenancy, TTL, concurrency and transactions.
`EgonColaCachePropertiesTest`, `EgonColaCacheAutoConfigurationTest`, `EgonColaCacheCodecsTest` and
`contract/RequiredTwoLevelCacheContractTest` cover binding and validation, conditional wiring and
fail-fast, the restricted type allow-list, and the required-component contract;
`EgonColaTwoLevelCacheManagerTest`, `EgonColaCacheChangedEventTest` and
`EgonColaCacheChangedListenerTest` cover region handles and name validation, node-id resolution,
event payloads and listener lifecycle. mp-sd-ext's `EgonColaRepositoryCacheEnhancementTest` verifies
the proxy integration between a concrete Repository and the base CRUD methods.
`core/EgonColaTwoLevelCacheTest`, `port/EgonColaTwoLevelCachePortEvictionIntegrationTest` and
`port/EgonColaCacheClusterConvergenceTest` extend `support/CacheRedisTestSupport` and are
Testcontainers Redis cases: they are still enabled explicitly with `-Degon.cola.cache.redis.it=true`,
require the caller to prepare and authorize the Docker environment first, and skip per case through an
assumption when Docker is unavailable.
