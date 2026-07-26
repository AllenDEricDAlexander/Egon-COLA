# Integration 02 DDC Publish Consistency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 DDC 具备 exact config 查询、draft/published 分离、Cluster-safe 原子 Redis 发布、可恢复 dispatcher、可靠 ACK 和确定性应用顺序。

**Architecture:** DB 保存 immutable config version 和 durable publish task，`published_version` 是 pull 唯一运行态指针。Redis v2 key 使用同 scope hash tag，Lua 原子写 value/version/event；失败通过同一 task 前向重放，不引入 DB/Redis 2PC。

**Tech Stack:** Spring Data JPA、Flyway、PostgreSQL、SQLite、Redisson Lua、Spring Lifecycle、JUnit 5、Testcontainers Redis。

## Global Constraints

- 依赖 Integration 01 已完成。
- 不修改 DDC V1-V3；PostgreSQL、SQLite 各新增一份 V4。
- `current_version` 仍表示最新 draft；pull 只读 `published_version` 对应的 immutable version。
- Redis 合同是幂等至少一次和前向收敛，不宣称分布式事务。
- Redis v2 与 legacy 双写/双 topic 一个 minor release。

---

### Task 1: 增加 management exact config GET

**Files:**
- Create: `.../management-client/src/main/java/top/egon/cola/component/ddc/management/model/DdcManagementConfigQuery.java`
- Create: `.../management-client/src/main/java/top/egon/cola/component/ddc/management/client/DdcManagementErrorCode.java`
- Modify: `.../management-client/src/main/java/top/egon/cola/component/ddc/management/DdcManagementClient.java`
- Modify: `.../management-client/src/main/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClient.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiController.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcManagementFacade.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcConfigService.java`
- Test: `.../management-client/src/test/java/top/egon/cola/component/ddc/management/client/HttpDdcManagementClientTest.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiControllerTest.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcManagementFacadeTest.java`

**Interfaces:**
- Produces `Optional<DdcManagementConfig> findConfig(DdcManagementConfigQuery query)`.
- Produces GET
  `/api/v1/ddc/openapi/management/configs/{appCode}/{env}/{namespace}/{configKey}`.
- Produces stable codes `CONFIG_NOT_FOUND` and `PUBLISH_TASK_NOT_FOUND`.

- [ ] **Step 1: Write failing client and controller tests**

```java
assertThat(client.findConfig(new DdcManagementConfigQuery(
        "gateway-engine-default", "test", "default", "gateway.rules.active"
))).contains(new DdcManagementConfig(
        "gateway-engine-default", "test", "default",
        "gateway.rules.active", "{}", "JSON", 3L,
        true, false, Instant.parse("2026-07-26T00:00:00Z")
));
```

Add a 404/empty response case that returns `Optional.empty()` and a validation case for blank coordinate.

- [ ] **Step 2: Run management client/admin tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-management-client,\
egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=HttpDdcManagementClientTest,DdcManagementOpenApiControllerTest,DdcManagementFacadeTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: missing method/endpoint failure.

- [ ] **Step 3: Implement exact query without list filtering**

```java
public record DdcManagementConfigQuery(
        String appCode, String env, String namespace, String configKey
) {
    public DdcManagementConfigQuery {
        appCode = required(appCode, "appCode");
        env = required(env, "env");
        namespace = required(namespace, "namespace");
        configKey = required(configKey, "configKey");
    }
}
```

`DdcConfigService` adds a repository-level exact lookup that preserves disabled/deleted management state; it
must not reuse fuzzy list filtering or the runtime `value` method. The HTTP client catches only
`DdcManagementClientException` with code `CONFIG_NOT_FOUND` and returns empty;
authentication, `PUBLISH_TASK_NOT_FOUND`, signature and network failures propagate unchanged. The Admin facade
uses the stable not-found codes instead of message matching.

- [ ] **Step 4: Re-run focused tests**

Run Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "feat: query exact ddc management config"
```

### Task 2: 增加 published_version 与运行态读取

**Files:**
- Create: `.../admin/src/main/resources/db/postgresql/V4__add_published_config_pointer.sql`
- Create: `.../admin/src/main/resources/db/sqlite/V4__add_published_config_pointer.sql`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcConfigItemEntity.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcConfigService.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigVersionRepository.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/migration/DdcV4MigrationTest.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcConfigServiceTest.java`

**Interfaces:**
- Produces `DdcConfigItemEntity.publishedVersion`.
- Produces pull/value using `DdcConfigVersionEntity.newValue` at publishedVersion.

- [ ] **Step 1: Write migration and service failure tests**

```java
@Test
void pullReturnsPublishedVersionInsteadOfNewerDraft() {
    item.setCurrentVersion(2L);
    item.setPublishedVersion(1L);
    when(versionRepository.findByConfigIdAndVersion("config-1", 1L))
            .thenReturn(Optional.of(version("v1", 1L)));

    assertThat(service.pull("orders", "test", "default"))
            .singleElement()
            .satisfies(value -> {
                assertThat(value.getConfigValue()).isEqualTo("v1");
                assertThat(value.getVersion()).isEqualTo(1L);
            });
}
```

Migration test upgrades V1-V3 data and asserts `published_version=current_version` for both dialects.

- [ ] **Step 2: Run tests and observe missing column/old pull behavior**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcV4MigrationTest,DdcConfigServiceTest
```

- [ ] **Step 3: Add both V4 files and published snapshot mapping**

PostgreSQL keeps the pointer nullable so a newly created draft is not visible before its first successful
publish:

```sql
alter table ddc_config_item add column published_version bigint;
update ddc_config_item set published_version = current_version;
```

SQLite:

```sql
alter table ddc_config_item add column published_version integer;
update ddc_config_item set published_version = current_version;
```

New configs initialize `current_version=1` and `published_version=NULL`. Pull skips a null published pointer,
resolves non-null pointers through the immutable version row, and never reads a newer `config_value` as runtime
state.

- [ ] **Step 4: Run admin tests for both database profiles**

Run Step 2 and the existing SQLite/PostgreSQL migration test profiles. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: separate ddc draft and published versions"
```

### Task 3: 建立 Redis v2 hash-tag key 合同

**Files:**
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/common/DdcKeys.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/repository/DdcRedisConfigRepository.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DdcRedisChangeSubscription.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepository.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepository.java`
- Modify: `.../admin/src/main/resources/redis/ddc_config_lease_register.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_config_lease_heartbeat.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_config_lease_deregister.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_config_lease_expire.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_service_register.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_service_heartbeat.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_service_deregister.lua`
- Modify: `.../admin/src/main/resources/redis/ddc_service_expire.lua`
- Modify: `.../dynamic-config-center-test/pom.xml`
- Create: `.../dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRedisSentinelIT.java`
- Create: `.../dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRedisClusterIT.java`
- Test: `.../starter/src/test/java/top/egon/cola/component/ddc/common/DdcKeysTest.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcRedisClusterSlotContractTest.java`

**Interfaces:**
- Produces `DdcKeys.v2Config*`, `v2Registry*`, legacy fallback and topic names.
- Produces same Redis cluster slot for every multi-key Lua invocation.

- [ ] **Step 1: Add failing slot tests**

```java
assertThat(slot(DdcKeys.v2Config(scope, "a")))
        .isEqualTo(slot(DdcKeys.v2Version(scope, "a")))
        .isEqualTo(slot(DdcKeys.v2Topic(scope)));

assertThat(registryScriptKeys(serviceKey, "instance-1"))
        .extracting(this::slot)
        .containsOnly(slot(registryScriptKeys(serviceKey, "instance-1").getFirst()));
```

Use Redisson/Redis CRC16 implementation or a deterministic test helper that honors `{...}`.

- [ ] **Step 2: Run key/repository tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-starter,\
egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcKeysTest,DdcRedisClusterSlotContractTest
```

Expected: current legacy keys occupy different slots.

- [ ] **Step 3: Implement scoped tags and mixed-version compatibility**

```java
private static String configTag(String appCode, String env, String namespace) {
    return "{" + digest(appCode + "\n" + env + "\n" + namespace) + "}";
}

private static String registryTag(String env, String namespace, DdcServiceKind kind) {
    return "{" + digest(env + "\n" + namespace + "\n" + kind.name()) + "}";
}
```

Write legacy and v2 config value/version; publish both topics. New readers use v2 first and legacy only on
absence. Registry scripts receive only v2 same-slot keys; legacy topic remains emitted for old subscribers.

- [ ] **Step 4: Run unit tests, Sentinel failover and real three-node Redis Cluster IT**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-test -am \
  -Pddc-redis-sentinel,ddc-redis-cluster verify
```

Expected: Sentinel master failover reconnects; Cluster registration, heartbeat, deregistration, lease expiry and
config publish complete without `CROSSSLOT`. If Docker is unavailable, retain unit evidence and report both live
gaps separately.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "feat: add cluster-safe ddc redis keys"
```

### Task 4: 原子 Redis dispatch 与 published pointer 推进

**Files:**
- Create: `.../admin/src/main/resources/redis/ddc_config_publish.lua`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPendingPublishDispatcher.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcRedisRepository.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishService.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishStateTransitionService.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/service/PublishStartupRecovery.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigItemRepository.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcPublishDispatchConsistencyTest.java`

**Interfaces:**
- Produces `DdcRedisRepository.dispatch(DdcAtomicPublishCommand)`.
- Produces durable dispatcher that replays PENDING/UNKNOWN tasks with the same targetVersion/changeId.

- [ ] **Step 1: Write failure-window tests**

Test these transitions explicitly:

```text
Redis Lua fails               -> task FAILED/UNKNOWN, published_version unchanged
Redis succeeds, DB update fails -> task UNKNOWN, same command replayed
same command replayed         -> no extra version, same event identity
different content/changeId reuse -> CHANGE_ID_CONFLICT
```

Assert pull still returns the prior version after Redis failure.

- [ ] **Step 2: Run the consistency test and confirm current leakage**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcPublishDispatchConsistencyTest,DdcPublishServiceFailureTest
```

- [ ] **Step 3: Implement prepare/dispatch/advance as separate short transactions**

Prepare stores immutable version/task/targets without advancing publishedVersion. Lua receives:

```text
KEYS = v2 value, v2 version, v2 idempotency, v2 topic
ARGV = expectedVersion, targetVersion, changeId, content, serializedMessage, checksum
```

After Lua success, a short DB transaction executes:

```java
int changed = configItemRepository.advancePublishedVersion(
        configId, expectedPublishedVersion, targetVersion, now
);
if (changed != 1) {
    stateTransitions.unknown(changeId, "published pointer update is uncertain");
}
```

Startup recovery claims stale PENDING/PUBLISHING/UNKNOWN tasks and replays the same command; it never creates
a new config version.

- [ ] **Step 4: Run admin suite and Redis integration tests**

Run Step 2 plus existing Admin integration tests. Expected: PASS, including lost-response replay.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: make ddc redis publish recoverable"
```

### Task 5: ACK 重试和确定性 Applier 顺序

**Files:**
- Create: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DdcAckDelivery.java`
- Create: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DdcAckDeliveryProperties.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DdcRefreshService.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DdcConfigApplier.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/service/DefaultDdcConfigApplierRegistry.java`
- Modify: `.../starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Test: `.../starter/src/test/java/top/egon/cola/component/ddc/service/DdcAckDeliveryTest.java`
- Test: `.../starter/src/test/java/top/egon/cola/component/ddc/service/DdcRefreshServiceTest.java`

**Interfaces:**
- Produces `DdcConfigApplier.priority()` default `0`.
- Produces `DdcAckDelivery.submit(DdcAckRequest)` with bounded retry and deduplication.

- [ ] **Step 1: Write failing delivery/order tests**

```java
when(admin.ack(request))
        .thenThrow(new ResourceAccessException("reset"))
        .thenReturn(success);
delivery.submit(request);
await().untilAsserted(() -> verify(admin, times(2)).ack(request));
```

Register activation priority `100` and chunk priority `0`; assert pull applies chunk first even when input list
contains activation first. Add non-retryable 4xx and queue saturation cases.

- [ ] **Step 2: Run starter tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-starter -am test \
  -Dtest=DdcAckDeliveryTest,DdcRefreshServiceTest
```

- [ ] **Step 3: Implement bounded lifecycle-owned delivery**

Use a `DelayQueue`/single bounded scheduled executor, key requests by
`changeId|instanceId|leaseId`, retry only transport/5xx, use exponential backoff with jitter, and expose counters.
`DdcRefreshService` applies sorted snapshots and submits ACK rather than performing a single synchronous call.

- [ ] **Step 4: Run full DDC reactor**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center \
  -am clean verify
```

Expected: PASS with no lingering ACK executor thread.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "feat: retry ddc acknowledgements safely"
```
