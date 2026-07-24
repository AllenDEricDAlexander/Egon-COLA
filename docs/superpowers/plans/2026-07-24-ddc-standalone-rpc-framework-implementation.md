# DDC Standalone and gRPC + Protobuf RPC Framework Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不引入任何集群协调代码的前提下，先完成单 Admin、单 Redis 的 DDC 运行闭环和 Redis-only 服务注册发现，再交付 Provider/Consumer 轻量 gRPC + Protobuf RPC Component，并仅用测试范围的 Mock Gateway 验证调用链。

**Architecture:** PR1 将 DDC 重构为统一 `instanceId + leaseId` 租约协议、配置客户端生命周期协调器、Redis-only 服务目录和单 Admin 同步全目标确认发布。PR2 只依赖已经合入的 PR1，使用标准 Proto 生成的 gRPC Descriptor 作为唯一协议事实，通过注解绑定 Provider 和 JDK Proxy Consumer；Consumer 永远只连接唯一 Gateway，但本轮 Gateway、Provider Directory、动态 Handler 和转发器全部是 `rpc-test-suite` 内的 Mock，生产 Gateway 延后到 PR1/PR2 完成后单独开发。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Maven、Redisson 3.26.0、PostgreSQL、SQLite、Flyway 11.15.0、grpc-java 1.75.0、Protocol Buffers 4.32.0、`protobuf-maven-plugin` 0.6.1、JUnit 5、Mockito、AssertJ、Awaitility、Maven Surefire/Failsafe。

**Design Spec:** `docs/superpowers/specs/2026-07-24-ddc-standalone-rpc-framework-design.md`

---

## 0. Global execution contract

### 0.1 Delivery order

This plan is one architecture plan implemented as two strictly sequential pull requests:

1. **PR1 — DDC standalone closure and registry**
   - starts from the current `main`;
   - does not add or reference the RPC Component;
   - must compile, test, and package independently;
   - must be merged before PR2 starts.
2. **PR2 — RPC Starter and test topology**
   - starts from `main` after PR1 has been merged;
   - consumes only public APIs exported by the DDC Starter;
   - must compile, test, and package independently.

Do not create PR2 files on the PR1 branch. Do not combine the two pull requests into one commit series.

### 0.2 Hard scope boundaries

- DDC remains a single Admin process and one Redis single-server connection.
- Do not add Raft, JRaft, leader election, Redisson distributed locks, Redis Cluster, Sentinel, multi-Admin coordination, or cross-node Waiter recovery.
- Provider and Gateway registry state remains Redis-only. Do not add a service registry database entity or table.
- Preserve existing DDC management/configuration paths. New registry paths are added under `/api/v1/ddc/openapi/registry/**`.
- Do not edit either existing `V1__create_ddc_schema.sql`.
- Add exactly one logical migration version, `V2__add_lease_and_sync_publish.sql`, with one PostgreSQL script and one SQLite dialect script.
- New publishes use only `SYNC_ALL_ACK`; legacy mode strings remain readable as historical data.
- `.proto` is the only RPC IDL. Do not create a second serializer, envelope IDL, schema registry, or custom protoc plugin.
- RPC V1 supports only unary methods whose request and response implement `com.google.protobuf.Message`.
- Do not add a production Gateway engine, Provider selector, Consumer-to-Provider channel, retry, gray routing, rate limiting, circuit breaking, or business retry.
- Do not add a production RPC Gateway package or the production types `RpcGatewayNodeRegistrar`, `RpcProviderDirectory`, `RpcProviderChannelFactory`, `RpcGatewayHandlerRegistry`, or `RpcUnaryForwarder`.
- All Gateway registration, Provider discovery, selection, dynamic handling, Provider channels, and forwarding used by current tests must remain under `rpc-test-suite/src/test`; they are Mock fixtures, not published APIs.
- Every `ManagedChannelBuilder` in RPC production code must call `disableRetry()`.
- Do not start any application after implementation verification. Tests may start bounded test servers/processes and must close them.

### 0.3 Design-pattern decisions

Use patterns only at the variation points already present in the design:

- **Facade / lifecycle coordinator:** `DdcRuntimeCoordinator`, `RpcProviderLifecycle`, and `RpcConsumerGatewayManager` own ordered startup and shutdown.
- **Observer:** DDC service snapshot/catalog subscriptions publish immutable complete snapshots.
- **Adapter:** `DdcOpenApiServiceRegistryClient` is the production registry adapter; deterministic registry and Gateway adapters exist only in RPC test code.
- **Registry:** publish locks, publish Waiters, and Provider method bindings use narrow registries keyed by their real identities; Mock Gateway registries remain test-private.
- **Proxy:** Consumer bindings use JDK Dynamic Proxy only.
- **Factory:** channel creation is isolated in channel factories, with retry disabled in the only creation path.

Do not retain the current publish Strategy hierarchy. V1 has one completion rule, so direct `SYNC_ALL_ACK` state transitions are clearer than `PublishConsistencyPolicy`.

### 0.4 Test and commit discipline

For every task:

1. write the focused failing test;
2. run it and record the expected failure;
3. implement the smallest production change;
4. rerun the focused test;
5. run the task-level module test;
6. review `git diff --check` and `git status --short`;
7. commit only that task.

Never use `Thread.sleep` in convergence tests. Use Awaitility with explicit timeout and poll interval.

---

# PR1 — DDC standalone closure and Redis-only registry

## Task 1: Introduce the unified lease contract in the DDC Starter

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcLeaseRole.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcLeaseOperationStatus.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcErrorStatus.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcInstanceIdentity.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcLeaseSession.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/vo/DdcLeaseOperationResult.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcLeaseSessionHolder.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcInstanceRegisterRequest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcHeartbeatRequest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcAckRequest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/DdcAdminClient.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcException.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcLeaseSessionHolderTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/client/HttpDdcAdminClientTest.java`

### Public contract

Use immutable records for identity/session/results:

```java
public enum DdcLeaseRole {
    CONFIG_CLIENT,
    RPC_PROVIDER,
    INTERNAL_GATEWAY
}

public record DdcInstanceIdentity(
        String instanceId,
        String appCode,
        String env,
        String namespace,
        String host,
        Integer port,
        String pid,
        String sdkVersion
) {
}

public record DdcLeaseSession(
        String instanceId,
        String leaseId,
        DdcLeaseRole role,
        int leaseSeconds,
        int heartbeatIntervalSeconds,
        Instant registeredAt,
        Instant leaseExpireAt
) {
}

public record DdcLeaseOperationResult(
        DdcLeaseOperationStatus status,
        Instant leaseExpireAt
) {
    public boolean renewed() {
        return status == DdcLeaseOperationStatus.RENEWED;
    }

    public boolean deleted() {
        return status == DdcLeaseOperationStatus.DELETED;
    }
}
```

`DdcLeaseOperationStatus` has exactly:

```text
RENEWED
DELETED
NOT_FOUND
LEASE_MISMATCH
NOT_DELETED
```

`DdcErrorStatus` implements the existing common `ErrorStatus` contract and assigns stable 56xxx codes to invalid request, lease not found/mismatch, instance conflict, publish in progress/no target/change conflict/expired target, signature required/invalid/expired/replay, and internal failure. Change `DdcException` and `DdcAdminException` to carry this common status through `ResultDto`; do not return raw exception messages or HTTP-200 failure envelopes that the client treats as success.

Change the config-client API to:

```java
public interface DdcAdminClient {
    DdcLeaseSession register(DdcInstanceRegisterRequest request);
    DdcLeaseOperationResult heartbeat(DdcHeartbeatRequest request);
    DdcLeaseOperationResult offline(DdcHeartbeatRequest request);
    List<DdcConfigValue> pull();
    void reportDefaults(DdcDefaultReportRequest request);
    void ack(DdcAckRequest request);
}
```

`DdcHeartbeatRequest` gains `leaseId`; `DdcInstanceRegisterRequest` gains `leaseSeconds` and `heartbeatIntervalSeconds` but never accepts `leaseId`; `DdcAckRequest` gains `leaseId` and `contentChecksum`.

`DdcLeaseSessionHolder` stores one `AtomicReference<DdcLeaseSession>` and exposes `current()`, `replace(session)`, and compare-and-clear. Callers must fetch the current value for each heartbeat, deregistration, and ACK.

### TDD steps

- [ ] Add tests proving the holder atomically replaces an old lease, returns the current lease, and cannot clear a newly replaced lease using an old session.
- [ ] Extend `HttpDdcAdminClientTest` to prove Register parses `ResultDto<DdcLeaseSession>` and heartbeat/offline parse `DdcLeaseOperationResult`.
- [ ] Add a client test proving any `ResultDto.success=false` becomes a typed `DdcException` with the returned stable code/status.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
    -am -Dtest=DdcLeaseSessionHolderTest,HttpDdcAdminClientTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: compilation/test failure because the lease types and new return signatures do not exist.

- [ ] Add the enums, immutable records, holder, request fields, and client signatures.
- [ ] Add the direct common-core dependency, stable DDC errors, and parse/check every existing `ResultDto` envelope without changing public OpenAPI paths.
- [ ] Update existing fake `DdcAdminClient` implementations in tests so the Starter compiles; return deterministic lease results.
- [ ] Rerun the focused command. Expected: `BUILD SUCCESS`.
- [ ] Run the complete Starter test suite:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
    -am test
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): define unified lease contract"
  ```

---

## Task 2: Implement atomic config-client leases in the single Redis Admin

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcConfigLeaseRedisRepository.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcLeaseValidator.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcConfigLeaseService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_config_lease_register.lua`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_config_lease_heartbeat.lua`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_config_lease_deregister.lua`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcKeys.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfig.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/common/DdcAdminException.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcInstanceAdminService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcOpenApiController.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcConfigLeaseServiceTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcOpenApiControllerTest.java`

### Redis contract

Keep the current config-client index as a secondary index and replace the instance bucket with:

```text
ddc:lease:instance:{env}:{namespace}:CONFIG_CLIENT:{instanceId}
```

The bucket value is the complete JSON lease record and has a real Redis TTL. The existing scope index contains `instanceId` members so Admin can list live config clients without `KEYS` or `SCAN`.

The three Lua scripts atomically maintain the bucket and index:

- Register receives an Admin-generated UUIDv7 `leaseId`, replaces any prior bucket for that `instanceId`, refreshes TTL, and ensures index membership.
- Heartbeat checks stored `instanceId` and `leaseId` before updating `lastHeartbeatAt`, `leaseExpireAt`, and TTL.
- Deregister checks both fields before deleting the bucket and index member.

Lua return codes map to `RENEWED`, `DELETED`, `NOT_FOUND`, `LEASE_MISMATCH`, or `NOT_DELETED`. No script may create a missing lease during heartbeat.

`DdcConfigLeaseService` owns role/TTL validation. Allowed TTL is 5–300 seconds and heartbeat interval must be positive and less than TTL.

### TDD steps

- [ ] Add service tests proving:
  - every Register generates a different `leaseId`;
  - the second Register invalidates the first lease;
  - correct heartbeat renews the expiry;
  - old heartbeat returns `LEASE_MISMATCH`;
  - old deregister returns `NOT_DELETED` and leaves the new lease intact;
  - missing lease heartbeat returns `NOT_FOUND`;
  - invalid TTL/heartbeat input fails before Redis access.
- [ ] Add controller tests for lease-bearing Register, heartbeat, and offline responses.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=DdcConfigLeaseServiceTest,DdcOpenApiControllerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because the lease repository/service and response contracts are absent.

- [ ] Implement script loading through Redisson `RScript`; do not use `RLock`.
- [ ] Refactor `DdcInstanceAdminService` to delegate current-lease facts to `DdcConfigLeaseService`; keep writing the legacy CONFIG_CLIENT management fields until Task 3 adds lease columns.
- [ ] Make `/instances/register` return `ResultDto<DdcLeaseSession>` and heartbeat/offline return `ResultDto<DdcLeaseOperationResult>`.
- [ ] Rerun the focused tests and then:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am test
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): enforce atomic config leases"
  ```

---

## Task 3: Add the single V2 migration and lease-aware persistence

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/postgresql/V2__add_lease_and_sync_publish.sql`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/sqlite/V2__add_lease_and_sync_publish.sql`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcInstanceEntity.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcPublishTaskEntity.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/entity/DdcPublishAckEntity.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/enums/PublishMode.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/enums/PublishStatus.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcPublishAckRepository.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcPublishTaskRepository.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcInstanceAdminService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcLeaseExpiryScanner.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcSchemaScriptTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/repository/DdcV2MigrationTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcLeaseExpiryScannerTest.java`

### Migration content

Both dialect scripts represent the same logical V2:

```text
ddc_instance:
  lease_id varchar(64)
  lease_expire_at timestamp/datetime

ddc_publish_task:
  content_checksum varchar(64)
  attempt_count integer not null default 0
  dispatched_at timestamp/datetime
  completed_at timestamp/datetime
  failure_stage varchar(64)

ddc_publish_ack:
  lease_id varchar(64)
  content_checksum varchar(64)
```

Drop `uk_ddc_publish_ack_instance` and replace it with:

```text
uk_ddc_publish_ack_target(change_id, instance_id, lease_id)
```

Legacy rows remain readable with nullable lease/checksum fields. Do not synthesize lease identities for historical rows.

`PublishMode` adds `SYNC_ALL_ACK` but retains legacy constants for deserialization/history. `PublishStatus` adds `UNKNOWN`; `SUCCESS`, `FAILED`, `TIMEOUT`, and `UNKNOWN` are terminal for new flows.

Repository signatures become lease-aware:

```java
Optional<DdcPublishAckEntity> findByChangeIdAndInstanceIdAndLeaseId(
        String changeId, String instanceId, String leaseId);

List<DdcPublishTaskEntity> findByStatusIn(Collection<String> statuses);
```

Add conditional update queries for transitions from `PENDING/PUBLISHING` to one terminal state rather than read-modify-write races.

After the columns exist, persist the current config-client `leaseId` and `leaseExpireAt` in `DdcInstanceAdminService`. Add `DdcLeaseExpiryScanner` with a default five-second interval. It removes stale Redis index members and marks the database projection `OFFLINE` only when the row still contains the expired lease ID.

### TDD steps

- [ ] Extend `DdcSchemaScriptTest` to require both V2 resources and assert the new columns/index while asserting V1 content remains untouched.
- [ ] Add `DdcV2MigrationTest` that creates an old SQLite schema using V1, runs V2, inserts a lease-aware task/target, and verifies the new unique target identity.
- [ ] Add scanner tests proving stale index members are removed and the database projection is marked offline only when its current `leaseId` still matches.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=DdcSchemaScriptTest,DdcV2MigrationTest,DdcLeaseExpiryScannerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because V2 and the entity fields are absent.

- [ ] Add the two dialect scripts without modifying either V1 file.
- [ ] Update entities, enums, and repositories.
- [ ] Persist lease projection fields and implement the single-Admin expiry scanner.
- [ ] Rerun the focused tests. Expected: `BUILD SUCCESS`.
- [ ] Verify the V1 files are byte-for-byte unchanged:

  ```bash
  git diff --exit-code -- \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/postgresql/V1__create_ddc_schema.sql \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/sqlite/V1__create_ddc_schema.sql
  ```

- [ ] Run the Admin tests:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am test
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): migrate lease-aware publish state"
  ```

---

## Task 4: Close the config-client startup, refresh, recovery, and shutdown lifecycle

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinator.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRuntimeState.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceIdentityFactory.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/listener/DdcRedisChangeSubscription.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/resources/META-INF/egon-cola-ddc.properties`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcInstanceService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcRefreshService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/service/DdcFieldBindingService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/repository/DdcLocalConfigRepository.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/listener/DdcRedisChangeListener.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcPublishTarget.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcPublishMessage.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcRuntimeCoordinatorTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcRefreshServiceTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/service/DdcFieldBindingServiceTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcSampleRefreshFlowTest.java`

### Lifecycle implementation

`DdcRuntimeCoordinator` implements `SmartLifecycle` and starts after all singleton beans and the Redis listener subscription exist. Its start sequence is:

1. validate `appCode`, `env`, and `namespace`;
2. build one immutable process `DdcInstanceIdentity`;
3. verify the listener subscription is active;
4. Register and atomically install the returned lease session;
5. report all collected defaults;
6. pull the complete snapshot;
7. call `applySnapshot` for values newer than local versions;
8. transition to `READY`;
9. start one named daemon scheduler for heartbeat/recovery.

Its stop sequence is:

1. transition to `STOPPING`;
2. stop the scheduler;
3. fetch the current lease and best-effort deregister;
4. remove the Redis topic listener by listener ID;
5. clear the session;
6. allow the container to invoke the DDC Redisson bean's `shutdown` destroy method.

`fail-fast=true` throws from `SmartLifecycle.start()` on Register/default-report/initial-pull failure. `fail-fast=false` keeps defaults and schedules recovery; recovery Register always replaces the current lease and then performs the initial snapshot.

Replace the hard-coded SDK version with a Maven-filtered property in `META-INF/egon-cola-ddc.properties`.

### Refresh implementation

- Add `DdcRefreshService.applySnapshot(DdcConfigValue)`, which never ACKs.
- Keep `refresh(DdcPublishMessage)` for topic messages and require current `instanceId + leaseId`.
- `DdcPublishMessage` carries `contentChecksum` plus the fixed `List<DdcPublishTarget>` where each target is `instanceId + leaseId`.
- A listener ignores messages whose scope, envelope checksum, or target identity does not match.
- If local version and content checksum already match the message, send `SUCCESS`, not `IGNORED`.
- If a truly older version arrives, send `IGNORED`.
- ACK transport failure is recorded separately and does not turn a successful field update into a conversion failure.

Make field application rollback-safe:

1. convert every refreshable binding before any write;
2. capture previous field values;
3. apply every converted value;
4. if a write fails, restore prior values best-effort and do not update local version/checksum;
5. update version/checksum only after all writes succeed.

Serialize snapshot/topic updates per `configKey` with a local keyed lock so an older snapshot cannot overwrite a newer topic update.

### TDD steps

- [ ] Add coordinator tests for exact startup order, fail-fast behavior, non-fail-fast recovery, heartbeat `NOT_FOUND` re-register, heartbeat `LEASE_MISMATCH` re-register, and reverse-order shutdown.
- [ ] Add refresh tests for:
  - snapshot does not ACK;
  - target message sends lease-aware success ACK;
  - non-target message does nothing;
  - older snapshot cannot overwrite a newer topic value;
  - already-applied same version/checksum returns `SUCCESS`;
  - conversion failure performs no field write/version advance;
  - second field write failure restores the first field;
  - ACK transport failure leaves the applied value/version intact.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
    -am -Dtest=DdcRuntimeCoordinatorTest,DdcRefreshServiceTest,DdcFieldBindingServiceTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because the coordinator/snapshot path and transactional field plan do not exist.

- [ ] Implement the coordinator, listener handle, version resource, lease recovery, and two refresh paths.
- [ ] Remove `ackEnabled`; V1 topic refresh ACK is mandatory.
- [ ] Rename `heartbeatTimeoutSeconds` to `leaseSeconds` while accepting the old property name as a deprecated binding alias for one release.
- [ ] Update the sample tests to provide a valid lease and target identity.
- [ ] Rerun focused tests and then the Starter + DDC test modules:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
    -am test
  ```

- [ ] Assert no hard-coded SDK version remains:

  ```bash
  ! rg -n 'SDK_VERSION\\s*=|5\\.2\\.1' \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): close config client lifecycle"
  ```

---

## Task 5: Replace the partial client signature with canonical OpenAPI HMAC verification

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/security/DdcCanonicalRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/security/DdcRequestSigner.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcCachedBodyHttpServletRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcNonceCache.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcOpenApiHmacFilter.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/client/HttpDdcAdminClient.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcGlobalExceptionHandler.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/application.yml`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/security/DdcRequestSignerTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcOpenApiHmacFilterTest.java`

### Canonical request

Both client and Admin build this exact UTF-8 string:

```text
HTTP_METHOD\n
PATH\n
CANONICAL_QUERY\n
TIMESTAMP\n
NONCE\n
SHA256(BODY)
```

Rules:

- method is uppercase;
- path is the raw request path without scheme, host, or fragment;
- query parameters are percent-encoded, sorted by encoded key then encoded value, rendered as `key=value`, and joined with `&`;
- an empty query is an empty line;
- the client serializes POST DTOs to one byte array, hashes and signs that byte array, and sends those exact bytes as `application/json`;
- empty GET body hashes empty bytes;
- Signature is lowercase HMAC-SHA256 hex;
- Admin compares signature and content hash using a constant-time byte comparison.

Headers are exactly:

```text
X-DDC-Access-Key
X-DDC-Timestamp
X-DDC-Nonce
X-DDC-Content-SHA256
X-DDC-Signature
```

Admin configuration remains one configured credential in V1:

```yaml
openapi:
  signature-enabled: false
  access-key:
  secret-key:
  allowed-clock-skew-seconds: 300
  nonce-cache-max-size: 10000
```

`DdcNonceCache` is bounded and expires entries after the allowed time window. The replay key is `accessKey + ":" + nonce`. Signature-disabled mode bypasses the filter for compatibility.

### TDD steps

- [ ] Add deterministic signer vectors for GET query sorting, JSON bytes, empty body, and one-byte body tampering.
- [ ] Add filter tests for valid request, wrong signature, wrong content hash, expired timestamp, future timestamp, missing headers, unknown access key, duplicate nonce, and disabled verification.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=DdcRequestSignerTest,DdcOpenApiHmacFilterTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because canonical signing and Admin verification are absent.

- [ ] Implement one canonicalization algorithm in the Starter and consume it from the Admin module through its existing Starter dependency.
- [ ] Ensure the repeatable-body wrapper exposes identical bytes to Jackson after the filter verifies them.
- [ ] Return stable DDC error codes from the filter without entering a controller.
- [ ] Rerun focused tests and both module suites.
- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): verify canonical OpenAPI signatures"
  ```

---

## Task 6: Add Redis-only service registration, catalog, and immutable subscriptions

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/enums/DdcServiceKind.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceKey.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceRegistration.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceInstance.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceQuery.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcServiceCatalogSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/registry/DdcRegistryEvent.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcServiceLeaseRequest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcRegistrySubscription.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcServiceRegistryClient.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcOpenApiServiceRegistryClient.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcRegistrySubscriptionManager.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfig.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryOpenApiController.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcServiceRegistryRedisRepository.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcServiceRegistryService.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_service_register.lua`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_service_heartbeat.lua`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_service_deregister.lua`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/redis/ddc_service_expire.lua`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcKeys.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcAutoConfig.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcLeaseExpiryScanner.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/registry/DdcRegistrySubscriptionManagerTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/test/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfigTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcServiceRegistryServiceTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryOpenApiControllerTest.java`

### Public Starter interface

```java
public interface DdcServiceRegistryClient {
    DdcLeaseSession register(DdcServiceRegistration registration);
    DdcLeaseOperationResult heartbeat(String instanceId, String leaseId);
    DdcLeaseOperationResult deregister(String instanceId, String leaseId);
    DdcServiceSnapshot getInstances(DdcServiceKey serviceKey);
    DdcRegistrySubscription subscribe(
            DdcServiceKey serviceKey,
            Consumer<DdcServiceSnapshot> listener);
    DdcServiceCatalogSnapshot getServiceKeys(DdcServiceQuery query);
    DdcRegistrySubscription subscribeServices(
            DdcServiceQuery query,
            Consumer<DdcServiceCatalogSnapshot> listener);
}
```

`DdcRegistrySubscription` extends `AutoCloseable` and `close()` is idempotent.

`DdcOpenApiServiceRegistryClient` keeps a local immutable registration identity for every active `instanceId + leaseId`. It builds `DdcServiceLeaseRequest` with env, namespace, kind, Service Key, instanceId, and leaseId for heartbeat/deregister; an unknown or stale local identity fails before HTTP rather than guessing the Redis key.

All registry model records defensively copy maps/lists and expose stable ordering:

- Service keys sort by env, namespace, kind, protocol, serviceName, group, version.
- Instances sort by `instanceId`, then `leaseId`.
- Metadata is unmodifiable and enforces the size/length/reserved-prefix rules from the Spec.

### Admin behavior

Implement exactly these endpoints:

```text
POST /api/v1/ddc/openapi/registry/instances/register
POST /api/v1/ddc/openapi/registry/instances/heartbeat
POST /api/v1/ddc/openapi/registry/instances/deregister
GET  /api/v1/ddc/openapi/registry/instances
GET  /api/v1/ddc/openapi/registry/services
```

The Lua scripts atomically maintain the bucket, service ZSET, service revision, catalog SET, catalog revision, and invalidation topic described in the Spec. Use SHA-256 of the canonical Service Key for Redis key suffixes. Do not use `KEYS`, `SCAN`, a database repository, or a distributed lock.

Register always creates a new lease. Heartbeat only extends a matching lease and never changes Service Key/address/metadata. Deregister with an old lease returns `NOT_DELETED`. List and catalog reads clean stale members idempotently before returning a complete snapshot.

Because the canonical Redis bucket is keyed by kind + instanceId, Register must reject reuse of one instanceId for a different Service Key with `DDC_INSTANCE_ID_CONFLICT`; it must not silently overwrite the other service. RPC Provider avoids this by deriving one registry instance ID per service.

### Subscription behavior

`DdcRegistrySubscriptionManager`:

1. subscribes to the kind/protocol invalidation topic;
2. then performs a full HTTP pull;
3. debounces relevant events and performs another full pull;
4. reconciles on `registry.reconcile-interval-seconds`, default `10`;
5. publishes only when revision or snapshot content changed;
6. expires locally cached instances at `leaseExpireAt` even while Admin is unavailable;
7. catches listener exceptions without terminating Redis listener/scheduler threads.

Split auto-configuration so `egon.cola.component.ddc.enabled=false` can disable the CONFIG_CLIENT lifecycle while `egon.cola.component.ddc.registry.enabled=true` still creates the public registry client for RPC applications.

`registry.enabled` defaults to `false` and uses `matchIfMissing=false`. RPC Provider applications and the test-only Mock Gateway enable it explicitly; the future production Gateway will opt in during its own project. This prevents the Admin application's existing `ddc.enabled=false` setting from accidentally auto-configuring a registry client back to itself.

### TDD steps

- [ ] Add Admin tests for new lease on Register, strict heartbeat/deregister matching, metadata validation, catalog add/remove revisions, stable snapshots, and expired member cleanup.
- [ ] Add subscription tests for subscribe-before-pull ordering, event-driven full refresh, reconciliation after a dropped event, immutable ordering, listener isolation, and local expiry while Admin is unavailable.
- [ ] Add auto-config tests proving config-client and registry toggles are independent and no in-memory test adapter is auto-configured.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=DdcRegistrySubscriptionManagerTest,DdcRegistryAutoConfigTest,DdcServiceRegistryServiceTest,DdcRegistryOpenApiControllerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because registry models, endpoints, scripts, and subscriptions do not exist.

- [ ] Implement public models/client, isolated auto-config, Admin service/repository/scripts, and expiry integration.
- [ ] Rerun focused tests and complete Starter/Admin tests.
- [ ] Assert no registry table/entity was added:

  ```bash
  ! rg -n 'ddc_service_instance|DdcServiceInstanceEntity|JpaRepository<.*ServiceInstance' \
    egon-cola-components/egon-cola-component-dynamic-config-center
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): add Redis service registry"
  ```

---

## Task 7: Rebuild publish preparation around changeId, resource locks, and fixed targets

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/vo/DdcConfigResourceKey.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/PublishResourceLockRegistry.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/PublishCompletionWaiterRegistry.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishStateTransitionService.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/AsyncPublishConsistencyPolicy.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/StrongAllAckPublishConsistencyPolicy.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/StrongQuorumAckPublishConsistencyPolicy.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/PublishConsistencyPolicy.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/PublishConsistencyPolicyFactory.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/policy/PublishDecision.java`
- Delete: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/policy/PublishConsistencyPolicyTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/dto/DdcPublishRequest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/model/vo/DdcPublishResultVO.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/repository/DdcRedisRepository.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/common/DdcChecksum.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/model/dto/DdcPublishMessage.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/PublishResourceLockRegistryTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcPublishPreparationTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcPublishServiceFailureTest.java`

### Request and locking contract

`DdcPublishRequest` has exactly:

```text
changeId             required UUIDv7 supplied by caller
appCode
env
namespace
configKey
configValue
expectedVersion
timeoutMs
```

Remove `publishMode` from new management requests. Persist `SYNC_ALL_ACK`.

`PublishResourceLockRegistry` is a `ConcurrentHashMap<DdcConfigResourceKey, String>`:

```java
boolean tryAcquire(DdcConfigResourceKey key, String changeId);
void release(DdcConfigResourceKey key, String ownerChangeId);
Optional<String> owner(DdcConfigResourceKey key);
```

`tryAcquire` returns true only when `putIfAbsent` installed this claim; `release` uses conditional `remove(key, owner)`. It never blocks and never uses `ReentrantLock`.

`PublishCompletionWaiterRegistry` is keyed only by `changeId`; its signal is advisory. Every wake-up must reload persistent task/target rows before deciding completion.

### Preparation transaction

Before claiming a resource, load by changeId. A matching persisted `SUCCESS/PENDING/PUBLISHING` task follows the idempotent rules in Task 8 without preparing again. A conflicting persisted task fails immediately. If no task exists, claim the resource; a race that finds the same changeId as current owner but no committed task returns `DDC_PUBLISH_IN_PROGRESS` and the caller can repeat the same idempotent request after preparation commits.

Under newly acquired resource ownership, one transaction:

1. validates the UUIDv7 `changeId`, resource, expected version, and bounded timeout;
2. rejects a mismatched existing change with `DDC_CHANGE_ID_CONFLICT`;
3. persists the new config value/version;
4. computes `contentChecksum = SHA256(configValue UTF-8)`;
5. asks `DdcConfigLeaseService` for the active CONFIG_CLIENT snapshot from Redis;
6. fails with `DDC_NO_LIVE_INSTANCE` when empty;
7. creates one `PENDING` task with `attemptCount=0`;
8. pre-creates one target row per exact `instanceId + leaseId`, with version/checksum;
9. records the operation log.

After commit:

1. register/reuse the Waiter;
2. write Redis config/value;
3. publish one message containing the fixed target identities;
4. conditionally transition `PENDING -> PUBLISHING` and set `dispatchedAt`;
5. wait outside the database transaction and outside the resource registry.

Different resource keys may publish in parallel. A second change on the same key fails fast with `DDC_PUBLISH_IN_PROGRESS` and the owning changeId.

### TDD steps

- [ ] Add resource registry concurrency tests for one winner per resource, different-resource parallelism, and owner-only release.
- [ ] Add preparation tests for caller changeId, fixed Redis lease targets, no-live-instance failure, content checksum, target precreation, same-resource rejection, different-resource parallel preparation, Redis dispatch failure, and no database `ONLINE` target lookup.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=PublishResourceLockRegistryTest,DdcPublishPreparationTest,DdcPublishServiceFailureTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because the current service creates its own changeId, uses database ONLINE rows, and has no resource/Waiter registries.

- [ ] Implement the new preparation/dispatch path and direct all status writes through `DdcPublishStateTransitionService`.
- [ ] Delete the obsolete policy hierarchy and update old tests to history-only enum coverage.
- [ ] Rerun focused and complete Admin tests.
- [ ] Assert production code has no publish-time blocking lock:

  ```bash
  ! rg -n 'ReentrantLock|RLock|STRONG_QUORUM_ACK.*decide|AsyncPublishConsistencyPolicy' \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): serialize sync publish resources"
  ```

---

## Task 8: Complete strict ACK, timeout, restart UNKNOWN, and idempotent retry

**Files:**

- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/PublishTimeoutScanner.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/PublishStartupRecovery.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishStateTransitionService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/service/DdcPublishService.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcPublishTaskController.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/application.yml`
- Replace: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcAckServiceTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcPublishTimeoutScannerTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/PublishStartupRecoveryTest.java`
- Test: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/service/DdcPublishRetryTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/controller/DdcConfigControllerTest.java`

### ACK state machine

ACK lookup is only:

```text
changeId + instanceId + leaseId
```

Then require exact equality of `targetVersion` and `contentChecksum`. Never create a target from an ACK request.

Rules:

- duplicate ACK for the same target/status is idempotent;
- a target can transition from null/PENDING to `SUCCESS`, `FAILED`, or `IGNORED` once;
- same version/checksum already applied is reported as `SUCCESS`;
- any target `FAILED` conditionally terminates the task as `FAILED`;
- all targets `SUCCESS` conditionally terminate it as `SUCCESS`;
- terminal task ACKs are read-only and cannot rewrite the result;
- invalid identity/version/checksum returns stable DDC errors and does not update counters;
- after commit, signal the changeId Waiter and conditionally release the resource when the task is terminal.

### Timeout and recovery

`PublishTimeoutScanner`:

- marks stale `PENDING` as `FAILED` using `dispatch-timeout-ms`;
- marks incomplete `PUBLISHING` target rows as `TIMEOUT` and the task as `TIMEOUT`;
- calls the same conditional state-transition service used by ACK and request threads;
- signals the Waiter and owner-conditionally releases the resource.

`PublishStartupRecovery` runs before Admin reports ready:

- in one recovery transaction, changes every legacy `PENDING/PUBLISHING` task to `UNKNOWN`;
- sets `completedAt` and the restart reason;
- does not infer success from partial ACKs;
- clears only the corresponding in-process resource owner if present;
- does not auto-dispatch.

### Idempotency and retry endpoint

Add:

```text
POST /api/v1/ddc/publish-tasks/{changeId}/retry
```

The original publish endpoint behavior is:

- matching `SUCCESS` returns the persisted result;
- matching `PENDING/PUBLISHING` returns/re-waits on the existing task without creating rows;
- mismatched resource/version/checksum/timeout returns `DDC_CHANGE_ID_CONFLICT`;
- `FAILED/TIMEOUT/UNKNOWN` does not re-dispatch through the normal endpoint.

Retry:

1. only accepts `FAILED`, `TIMEOUT`, or `UNKNOWN`;
2. re-acquires the original resource key;
3. verifies every fixed target's exact lease is still active;
4. if any target expired, transitions to `FAILED/DDC_TARGET_LEASE_EXPIRED`;
5. resets only the original target ACK states;
6. increments `attemptCount`;
7. republishes the original version/checksum/target set;
8. uses the same changeId Waiter.

### TDD steps

- [ ] Replace the permissive ACK test with strict identity/version/checksum/terminal/idempotency cases.
- [ ] Add scanner race tests showing ACK-vs-timeout has exactly one terminal winner.
- [ ] Add startup recovery tests for `PENDING/PUBLISHING -> UNKNOWN` and no inference from partial ACKs.
- [ ] Add retry tests for all original statuses, conflict detection, fixed-target reuse, expired target rejection, attempt increment, and no resnapshot.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin \
    -am -Dtest=DdcAckServiceTest,DdcPublishTimeoutScannerTest,PublishStartupRecoveryTest,DdcPublishRetryTest,DdcConfigControllerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because current ACKs are dynamically inserted/permissive and retry/recovery/scanner do not exist.

- [ ] Implement strict ACK, shared transitions, scanner, startup recovery, and retry endpoint.
- [ ] Add Admin properties:

  ```text
  publish.dispatch-timeout-ms=5000
  publish.default-timeout-ms=30000
  publish.max-timeout-ms=60000
  publish.scan-interval-ms=1000
  ```

- [ ] Rerun focused and complete Admin tests.
- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "feat(ddc): complete synchronous publish state machine"
  ```

---

## Task 9: Add DDC integration coverage, documentation, and the PR1 release gate

**Files:**

- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/README.md`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/README.zh-CN.md`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcLeaseLifecycleTest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcRegistryLifecycleTest.java`
- Create: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcSyncPublishFlowTest.java`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test/src/main/resources/application.yml`

### Integration coverage

Use test-scoped deterministic Redis doubles for the normal Maven test suite; do not introduce Testcontainers or require Docker.

The DDC test module must prove:

- config-client startup Register → defaults → snapshot → READY ordering;
- new lease replaces old lease and stale heartbeat/offline cannot mutate it;
- Provider and Mock-Gateway-style registry entry Register → subscribe → heartbeat → deregister;
- registry state is absent from all JPA tables;
- one publish snapshots exact config-client leases and returns only after every success ACK;
- same-resource concurrent publish rejects one request without blocking ACK handling;
- timeout, Redis dispatch failure, and Admin restart UNKNOWN are externally visible;
- retry retains original targets.

The English and Chinese READMEs must stay structurally aligned and document:

- standalone-only deployment topology;
- single Admin/single Redis limitation;
- PostgreSQL production and SQLite test profiles;
- HMAC canonical request/headers;
- lease defaults for CONFIG_CLIENT, RPC_PROVIDER, INTERNAL_GATEWAY;
- registry API and Redis-only durability semantics;
- `SYNC_ALL_ACK`, `UNKNOWN`, and changeId retry behavior;
- explicit non-support for multi-Admin/Raft/Redis Cluster.

### TDD and verification steps

- [ ] Add the integration tests and run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
    -am test
  ```

  Expected before final fixture wiring: focused failures identifying missing integration seams.

- [ ] Add only the test fixture/configuration needed to exercise public DDC APIs; do not reach into Admin private methods.
- [ ] Rerun the DDC reactor:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test \
    -am clean test
  ```

  Expected: `BUILD SUCCESS` for Starter, Admin, and Test.

- [ ] Package the independent PR1 deliverables without starting them:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-starter \
    -am package -DskipTests
  ```

  Expected: Admin executable JAR and Starter JAR are produced.

- [ ] Run structural guards:

  ```bash
  ! rg -n 'JRaft|Raft|leader election|RedisCluster|RLock|ddc_service_instance' \
    egon-cola-components/egon-cola-component-dynamic-config-center

  test "$(find \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/postgresql \
    -name 'V2__*.sql' | wc -l | tr -d ' ')" = "1"

  test "$(find \
    egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin/src/main/resources/db/sqlite \
    -name 'V2__*.sql' | wc -l | tr -d ' ')" = "1"

  git diff --check
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-dynamic-config-center
  git commit -m "test(ddc): verify standalone runtime closure"
  ```

- [ ] Open PR1 only after all commands above pass. PR1 title:

  ```text
  feat(ddc): complete standalone leases registry and sync publish
  ```

- [ ] Merge PR1 and rerun the DDC reactor from updated `main` before creating the PR2 branch.

---

# PR2 — gRPC + Protobuf RPC Starter and test topology

## Task 10: Create the RPC two-aggregator module shape and standard Protobuf toolchain

**Precondition:** PR1 is merged, `main` contains the public DDC registry contract, and the DDC reactor passes from `main`.

**Files:**

- Modify: `egon-cola-components/pom.xml`
- Modify: `egon-cola-components/egon-cola-components-bom/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/main/proto/echo_service.proto`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/pom.xml`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/test/java/top/egon/cola/component/rpc/test/contract/EchoGeneratedContractTest.java`

### Module graph

The production component root has exactly two direct modules:

```text
egon-cola-component-rpc
├── egon-cola-component-rpc-starter
└── egon-cola-component-rpc-test
    ├── egon-cola-component-rpc-test-contract
    ├── egon-cola-component-rpc-test-provider
    ├── egon-cola-component-rpc-test-consumer
    └── egon-cola-component-rpc-test-suite
```

Only `egon-cola-component-rpc-starter` is exported from the BOM. Mark the test aggregator and every child with:

```xml
<maven.deploy.skip>true</maven.deploy.skip>
<egon.release.shape>false</egon.release.shape>
```

Parent-managed versions:

```xml
<grpc.version>1.75.0</grpc.version>
<protobuf.version>4.32.0</protobuf.version>
<protoc.version>4.32.0</protoc.version>
<protoc-gen-grpc-java.version>1.75.0</protoc-gen-grpc-java.version>
<protobuf.maven.plugin.version>0.6.1</protobuf.maven.plugin.version>
<os-maven-plugin.version>1.7.1</os-maven-plugin.version>
<maven.failsafe.plugin.version>3.2.5</maven.failsafe.plugin.version>
<javax.annotation.api.version>1.3.2</javax.annotation.api.version>
```

Add the external library/plugin properties and dependency management to `egon-cola-components/pom.xml`, so business modules that own Proto files can reuse the same compiler/runtime versions. The RPC component POM manages only its internal Starter/test artifact versions and module graph. Manage `grpc-netty-shaded`, `grpc-protobuf`, `grpc-stub`, `protobuf-java`, `javax.annotation-api`, and the two build plugins without importing a third-party gRPC Spring Boot starter.

`rpc-test-contract` configures:

```text
protobuf:compile
protobuf:compile-custom
```

with:

```xml
<protocArtifact>
  com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}
</protocArtifact>
<pluginId>grpc-java</pluginId>
<pluginArtifact>
  io.grpc:protoc-gen-grpc-java:${protoc-gen-grpc-java.version}:exe:${os.detected.classifier}
</pluginArtifact>
```

### Test Proto

`echo_service.proto` is the only Echo IDL:

```proto
syntax = "proto3";

package egon.rpc.test.v1;

option java_multiple_files = true;
option java_package = "top.egon.cola.component.rpc.test.contract.proto";

service EchoService {
  rpc Echo(EchoRequest) returns (EchoResponse);
}

message EchoRequest {
  string message = 1;
}

message EchoResponse {
  string provider_id = 1;
  string message = 2;
}
```

### TDD steps

- [ ] Add `EchoGeneratedContractTest` importing `EchoRequest`, `EchoResponse`, and `EchoServiceGrpc` and asserting the generated service/method full names.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
    -am test
  ```

  Expected before POM/module creation is complete: module or generated-class failure.

- [ ] Add the exact module graph, managed dependencies/plugins, Proto, and generation configuration.
- [ ] Rerun the focused command. Expected: generated Java and grpc-java classes compile and the test passes.
- [ ] Verify shape:

  ```bash
  test "$(sed -n '/<modules>/,/<\\/modules>/p' \
    egon-cola-components/egon-cola-component-rpc/pom.xml | rg -c '<module>')" = "2"

  rg -n 'egon-cola-component-rpc-starter' \
    egon-cola-components/egon-cola-components-bom/pom.xml

  ! rg -n 'egon-cola-component-rpc-test' \
    egon-cola-components/egon-cola-components-bom/pom.xml
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/pom.xml \
    egon-cola-components/egon-cola-components-bom/pom.xml \
    egon-cola-components/egon-cola-component-rpc
  git commit -m "build(rpc): add protobuf grpc component modules"
  ```

---

## Task 11: Bind annotation contracts strictly to generated gRPC Descriptors

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcService.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcMethod.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcProvider.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/annotation/EgonRpcReference.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/RpcContractDescriptor.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/RpcMethodDescriptor.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/GeneratedGrpcDescriptorResolver.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/contract/RpcContractValidator.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/EgonRpcErrorCode.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/EgonRpcException.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/EgonRpcRejectedException.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/support/TestGrpcDescriptorFixtures.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/pom.xml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract/src/main/java/top/egon/cola/component/rpc/test/contract/EchoRpc.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/contract/RpcContractValidatorTest.java`

### Annotation API

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcService {
    Class<?> grpcClass();
    String group() default "default";
    String version() default "1.0.0";
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcMethod {
    String name();
}

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcProvider {
}

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EgonRpcReference {
    long timeoutMs() default -1;
}
```

`EchoRpc` binds only to generated artifacts:

```java
@EgonRpcService(
        grpcClass = EchoServiceGrpc.class,
        group = "default",
        version = "1.0.0")
public interface EchoRpc {

    @EgonRpcMethod(name = "Echo")
    EchoResponse echo(EchoRequest request);
}
```

### Validation algorithm

`GeneratedGrpcDescriptorResolver` invokes the generated class's public static `getServiceDescriptor()`. It uses each gRPC `MethodDescriptor`'s `ProtoMethodDescriptorSupplier` to obtain the Protobuf `Descriptors.MethodDescriptor`.

For every Java RPC method, validate:

- declaring interface has `@EgonRpcService`;
- exactly one request parameter;
- request and response implement `Message`;
- no overloads;
- nonblank group/version and method name;
- matching Proto method exists;
- Proto method is unary;
- Java request `getDefaultInstance().getDescriptorForType()` equals Proto input descriptor;
- Java response descriptor equals Proto output descriptor;
- full method name comes from the generated gRPC `MethodDescriptor`, never from annotation concatenation.

Return immutable `RpcContractDescriptor` / `RpcMethodDescriptor` values. Cache only successfully validated interface descriptors.

Starter unit tests must not depend on `rpc-test-contract`, because that module depends on Starter for annotations. `TestGrpcDescriptorFixtures` builds test-only gRPC `ServiceDescriptor` objects from Protobuf well-known `StringValue`/`Int32Value` descriptors and exposes generated-class-shaped static `getServiceDescriptor()` methods. This avoids a Maven dependency cycle and does not add a second `.proto`.

At this task, add the one-way `rpc-test-contract -> rpc-starter` dependency so `EchoRpc` can compile. Never add `rpc-starter -> rpc-test-contract`.

### TDD steps

- [ ] Add validator tests for the valid Echo contract and every rejection: missing service annotation, missing method annotation, missing generated descriptor, unknown Proto method, streaming method, zero/two args, non-Message types, request mismatch, response mismatch, overload, blank group/version.
- [ ] Assert valid full method name is exactly `/egon.rpc.test.v1.EchoService/Echo`.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
    -am -Dtest=RpcContractValidatorTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because contract annotations/resolver/validator do not exist.

- [ ] Implement only Descriptor-backed resolution; do not build replacement Protobuf marshallers or service definitions from annotation strings.
- [ ] Rerun the focused tests, then compile the one-way annotated test-contract → Starter dependency:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-contract \
    -am test
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "feat(rpc): validate protobuf descriptor contracts"
  ```

---

## Task 12: Implement Provider exposure, lease registration, trace scope, and shutdown

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderBeanScanner.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderMethodRegistry.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderAvailabilityRegistry.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcServerServiceDefinitionFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderServerFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderLifecycle.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderLeaseManager.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcMetadataKeys.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcProcessIdentity.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcProcessIdentityFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcInvocationMetadata.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcProviderServerInterceptor.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/resources/META-INF/egon-cola-rpc.properties`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/RpcProviderBeanScannerTest.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/provider/RpcProviderLifecycleTest.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/context/RpcProviderServerInterceptorTest.java`

### Provider binding

`RpcProviderBeanScanner` discovers Spring beans annotated `@EgonRpcProvider`, finds all implemented `@EgonRpcService` interfaces, validates them, and rejects duplicate `serviceName + group + version + methodName`.

`RpcServerServiceDefinitionFactory` uses the generated `io.grpc.MethodDescriptor<Request, Response>` from each validated method and `ServerCalls.asyncUnaryCall`. It invokes the bound Java bean method, rejects null input/output, maps declared RPC rejection exceptions, and converts all other exceptions to sanitized `Status.INTERNAL`.

Every handler consults `RpcProviderAvailabilityRegistry` before invoking the bean. A service is available only after its current registry lease is installed; heartbeat loss and graceful draining flip it unavailable immediately even though other services continue sharing the Server.

One grpc-netty-shaded Server exposes every Provider service. Do not use `InProcessServerBuilder`.

`RpcProcessIdentityFactory` creates one immutable process identity independently of whether the DDC CONFIG_CLIENT lifecycle is enabled. It uses `spring.application.name`, DDC env/namespace, resolved host, PID, and one UUIDv7 suffix. Provider registry IDs and Consumer source Metadata derive from this same process identity.

### Provider lifecycle

`RpcProviderLifecycle` is a late-phase `SmartLifecycle`:

1. validate Provider properties and scan contracts;
2. build service definitions/interceptors;
3. bind and start the real Server;
4. resolve actual bound port;
5. validate advertised host/port (`0.0.0.0` is forbidden);
6. register one `DdcServiceRegistration` per service;
7. store one current lease per service identity;
8. transition to `READY`;
9. start one named heartbeat scheduler.

Provider registry instance ID:

```text
{processInstanceId}:{serviceName}:{group}:{version}
```

Metadata includes exactly the framework-owned:

```text
egon.rpc.transport=grpc
egon.rpc.serialization=protobuf
egon.rpc.runtime-version={filtered project version}
```

User metadata cannot override `ddc.*`, `egon.internal.*`, or `egon.rpc.*`.

Heartbeat `NOT_FOUND` or `LEASE_MISMATCH` marks that service unavailable, re-registers it for a new lease, and only then restores `READY`.

Shutdown:

1. `DRAINING`;
2. deregister current leases;
3. call Server shutdown to reject new work;
4. await configured drain timeout;
5. call `shutdownNow` only if still running;
6. stop scheduler/executor and clear bindings.

### Trace and Metadata

The server interceptor accepts only the Spec whitelist, validates lengths/characters, creates a TraceContext scope, and clears it in `finally`. It never exposes DDC credentials or arbitrary metadata to application code.

### TDD steps

- [ ] Add scanner tests for valid multi-service bean, duplicate method binding, invalid contract, and non-provider bean.
- [ ] Add lifecycle tests for bind-before-register, random port registration, invalid advertised address, registration fail-fast, per-service lease storage/recovery, and deregister-before-server-stop.
- [ ] Add interceptor tests for whitelist, invalid trace replacement, internal target-header removal, context cleanup, and sanitized internal error.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
    -am -Dtest=RpcProviderBeanScannerTest,RpcProviderLifecycleTest,RpcProviderServerInterceptorTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because Provider production classes do not exist.

- [ ] Implement Provider auto-config conditional on both `rpc.enabled` and `rpc.provider.enabled`.
- [ ] Configure defaults exactly as the Spec: port `19090`, provider lease `30s`, heartbeat `10s`, shutdown `10000ms`, registration fail-fast true.
- [ ] Rerun focused and complete Starter tests.
- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "feat(rpc): expose and register protobuf providers"
  ```

---

## Task 13: Implement unique-Gateway discovery and JDK Proxy Consumer

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcGatewayState.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcGatewayEndpoint.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerChannelFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManager.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerInvocationHandler.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerProxyFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/EgonRpcReferenceBeanPostProcessor.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/context/RpcConsumerClientInterceptor.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapper.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManagerTest.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/consumer/RpcConsumerInvocationHandlerTest.java`
- Test: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/test/java/top/egon/cola/component/rpc/exception/RpcStatusExceptionMapperTest.java`

### Gateway-only discovery

The Consumer constructs one fixed `DdcServiceKey`:

```text
kind=INTERNAL_GATEWAY
serviceName=egon-internal-rpc-gateway
group={configured}
version={configured}
protocol=grpc
```

It calls only `DdcServiceRegistryClient.subscribe` with the fixed Gateway key and snapshot listener. It never calls `getServiceKeys`, never queries `RPC_PROVIDER`, and accepts no Provider host/port property.

`RpcConsumerGatewayManager` states:

```text
STARTING
READY
UNAVAILABLE
AMBIGUOUS
STOPPED
```

Startup waits up to `gateway-discovery-timeout-ms`:

- exactly one unexpired instance → create channel and `READY`;
- zero → `RPC_GATEWAY_UNAVAILABLE`;
- more than one → `RPC_GATEWAY_AMBIGUOUS`.

Runtime:

- zero or multiple active Gateway leases closes any no-longer-unique channel and new calls fail immediately;
- a new single endpoint creates a new channel first, atomically swaps after connectivity reaches READY, and drains the old channel;
- a failed replacement may keep the old channel only while its original lease remains valid;
- expired old lease must close even during Admin/Redis outage.

### Channel and Proxy

The only channel creation path is:

```java
NettyChannelBuilder.forAddress(host, port)
        .usePlaintext() // or transport security from endpoint.secure
        .disableRetry()
        .build();
```

`RpcConsumerProxyFactory` requires an interface, validates its Descriptor contract, and creates one JDK Proxy. `Object` methods are handled locally.

For each RPC call:

1. require Gateway state `READY`;
2. fetch the native generated unary `MethodDescriptor`;
3. reject null request;
4. select the shorter of current gRPC Context deadline, `@EgonRpcReference.timeoutMs`, and global default;
5. add framework service/group/version/invocation/source/trace Metadata;
6. invoke only the Gateway channel with `ClientCalls.blockingUnaryCall`;
7. map final gRPC Status to `EgonRpcException`.

gRPC Context cancellation or thread interruption must cancel the underlying call. No catch block retries it.

### Status mapping

Implement the exact public codes from the Spec. In particular:

- `DEADLINE_EXCEEDED -> RPC_DEADLINE_EXCEEDED`;
- `CANCELLED -> RPC_CANCELLED`;
- Consumer-side `UNAVAILABLE -> RPC_GATEWAY_UNAVAILABLE`;
- `INVALID_ARGUMENT -> RPC_INVALID_REQUEST`;
- `NOT_FOUND` is classified as service/method using the framework status trailer;
- all unclassified statuses become `RPC_INTERNAL`.

Do not expose `StatusRuntimeException` as the public proxy contract.

### TDD steps

- [ ] Add Gateway manager tests for zero/one/multiple at startup, runtime transitions, atomic replacement, expired old endpoint, drain close, and retry-disabled channel factory invocation.
- [ ] Add invocation tests for generated Descriptor use, Gateway-only channel, deadline precedence, cancellation, null rejection, Metadata, status mapping, and absence of retries.
- [ ] Add a reflection/bytecode boundary assertion that `consumer` production classes contain no `RPC_PROVIDER` query and no Provider channel type.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
    -am -Dtest=RpcConsumerGatewayManagerTest,RpcConsumerInvocationHandlerTest,RpcStatusExceptionMapperTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because Consumer discovery/proxy/channel classes do not exist.

- [ ] Implement Consumer auto-config conditional on `rpc.enabled && rpc.consumer.enabled`.
- [ ] Configure defaults: global timeout `3000ms`, discovery `5000ms`, Gateway group `default`, version `1.0.0`, channel drain `5000ms`.
- [ ] Rerun focused and complete Starter tests.
- [ ] Assert production Consumer has no Provider discovery:

  ```bash
  ! rg -n 'RPC_PROVIDER|providerHost|providerPort|round.?robin|pickProvider' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "feat(rpc): proxy consumers through one gateway"
  ```

---

## Task 14: Build the test-only Mock Gateway fixture

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockGatewayProperties.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderEndpoint.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderClusterSnapshot.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderDirectory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderChannelFactory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockByteArrayMarshaller.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockGatewayInvocation.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockDynamicHandlerRegistry.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockUnaryForwarder.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockRoundRobinSelector.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockRpcGateway.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderDirectoryTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockDynamicHandlerRegistryTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockUnaryForwarderTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockGatewayBoundaryTest.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/pom.xml`

### Mock-only boundary

Every class in this task is under `rpc-test-suite/src/test`. Nothing is added to
`rpc-starter/src/main`, no Gateway auto-configuration is created, and no Mock type is
exported through the BOM.

`MockRpcGateway` uses public DDC and grpc-java APIs directly:

1. starts one grpc-netty-shaded Server on loopback port `0`;
2. registers itself through `DdcServiceRegistryClient.register` as
   `INTERNAL_GATEWAY`;
3. stores the returned current lease and heartbeats/deregisters with exact
   `instanceId + leaseId`;
4. uses `MockProviderDirectory` to subscribe to `RPC_PROVIDER/grpc`;
5. delegates deterministic selection to `MockRoundRobinSelector`;
6. delegates byte-level unary forwarding to `MockUnaryForwarder`;
7. records invocation ID, selected Provider, and forwarding count;
8. closes lease, subscriptions, Provider channels, Server, and executors in reverse
   order.

`MockProviderDirectory` consumes `DdcServiceRegistryClient.getServiceKeys`,
`subscribeServices`, and `subscribe` directly. It publishes immutable test snapshots
and never becomes an RPC Starter dependency.

`MockProviderChannelFactory` caches by exact
`instanceId + leaseId + host + port + secure`, calls `disableRetry()` on every
creation path, and evicts removed/replaced leases.

`MockDynamicHandlerRegistry` is a test implementation of grpc-java
`HandlerRegistry` using `MethodDescriptor<byte[], byte[]>`; it retains the original
full method name and does not parse business Proto.

`MockUnaryForwarder` receives the endpoint already chosen by
`MockRoundRobinSelector`, forwards one payload, applies the remaining Deadline, binds
Consumer cancellation to the Provider `ClientCall`, copies only the Metadata
whitelist, and returns the Provider Status. These semantics validate the target
network boundary but make no production Gateway API commitment.

### TDD steps

- [ ] Add Directory tests for catalog discovery, snapshot replacement, deregistration,
  lease expiry, and stable ordering using a mocked public `DdcServiceRegistryClient`.
- [ ] Add Handler tests for original method name, unary byte marshalling, invalid method
  rejection, and concurrent lookup.
- [ ] Add Forwarder tests for preselected endpoint, retry-disabled channel, remaining
  Deadline, cancellation, Status, and Metadata whitelist.
- [ ] Add a boundary test proving:
  - every Mock Gateway type is under `rpc-test-suite/src/test`;
  - RPC Starter has no `top.egon.cola.component.rpc.gateway` package;
  - RPC Starter has none of the deferred Gateway production type names.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am -Dtest=MockProviderDirectoryTest,MockDynamicHandlerRegistryTest,MockUnaryForwarderTest,MockGatewayBoundaryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because the Mock Gateway fixtures do not exist.

- [ ] Implement only the test fixtures and keep their lifecycle manually controlled by
  the test suite.
- [ ] Rerun the focused command. Expected: `BUILD SUCCESS`.
- [ ] Assert no production Gateway implementation exists:

  ```bash
  test ! -d \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/gateway

  ! rg -n 'RpcGatewayNodeRegistrar|RpcProviderDirectory|RpcProviderChannelFactory|RpcGatewayHandlerRegistry|RpcUnaryForwarder' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "test(rpc): add mock gateway fixture"
  ```

---

## Task 15: Build separate Provider and Consumer test applications

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider/src/main/java/top/egon/cola/component/rpc/test/provider/RpcTestProviderApplication.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider/src/main/java/top/egon/cola/component/rpc/test/provider/EchoRpcProvider.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider/src/main/resources/application.yml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider/src/test/java/top/egon/cola/component/rpc/test/provider/EchoRpcProviderTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/src/main/java/top/egon/cola/component/rpc/test/consumer/RpcTestConsumerApplication.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/src/main/java/top/egon/cola/component/rpc/test/consumer/EchoRpcClient.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/src/main/resources/application.yml`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/src/test/java/top/egon/cola/component/rpc/test/consumer/EchoRpcClientContextTest.java`

### Provider test application

`EchoRpcProvider` is a real Spring bean:

```java
@EgonRpcProvider
public class EchoRpcProvider implements EchoRpc {
    private final String providerId;

    @Override
    public EchoResponse echo(EchoRequest request) {
        return EchoResponse.newBuilder()
                .setProviderId(providerId)
                .setMessage(request.getMessage())
                .build();
    }
}
```

The application reads provider ID, bind port, and advertised address from properties. It does not expose a direct Java test hook to the Consumer.

### Consumer test application

`EchoRpcClient` contains only:

```java
@EgonRpcReference(timeoutMs = 3000)
private EchoRpc echoRpc;
```

and a method that constructs `EchoRequest` and calls the injected JDK Proxy. It has no Provider dependency, host/port property, or direct Provider bean.

Both applications depend on the same `rpc-test-contract` artifact. Neither copies the `.proto` or generated source.

### TDD steps

- [ ] Add Provider test proving its implementation returns a Protobuf response and is discovered as an RPC Provider.
- [ ] Add Consumer context test proving `EchoRpcClient` receives a JDK Proxy when a
  one-Mock-Gateway lease fixture is supplied, and context startup fails for
  zero/multiple Gateways.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-provider,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer \
    -am test
  ```

  Expected: failure because the test applications and beans do not exist.

- [ ] Implement the two isolated applications and their default-disabled external registry configuration for unit tests.
- [ ] Rerun the focused modules. Expected: `BUILD SUCCESS`.
- [ ] Assert no copied Proto:

  ```bash
  test "$(find \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test \
    -path '*/src/main/proto/*.proto' | wc -l | tr -d ' ')" = "1"
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "test(rpc): add provider and consumer applications"
  ```

---

## Task 16: Prove Consumer → Mock Gateway → Provider over real TCP in ordinary mvn test

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/support/InMemoryDdcRegistryBackend.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/support/InMemoryDdcServiceRegistryClient.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockRpcGateway.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderDirectory.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/RpcTcpCallTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/RpcTcpCancellationTest.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/RpcTestBoundaryTest.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/pom.xml`

### Deterministic test registry

The in-memory backend exists only under `rpc-test-suite/src/test`. It implements the public DDC lease/registry semantics:

- every Register creates a new lease;
- heartbeat/deregister compare exact `instanceId + leaseId`;
- full immutable snapshot/catalog callbacks;
- explicit clock/expiry advancement for deterministic tests.

Each Spring context receives a distinct `InMemoryDdcServiceRegistryClient` facade over
the shared test backend. Provider, Mock Gateway, and Consumer do not share Spring
beans, application contexts, channels, or direct service object references.

### Mock Gateway integration

Wire the Task 14 `MockRpcGateway` to a distinct
`InMemoryDdcServiceRegistryClient` facade. It registers through the DDC public
interface, discovers Provider snapshots through `MockProviderDirectory`, and uses its
test-private Handler/Channel/Forwarder. Do not add an RPC Starter Gateway adapter to
make this wiring easier.

### TCP smoke test

`RpcTcpCallTest` starts three isolated Spring contexts in one JVM:

- Provider context with a real Netty Server on loopback random TCP port;
- Mock Gateway context/server on a different loopback random TCP port;
- Consumer context with only the Gateway snapshot/channel.

Call:

```text
EchoRpcClient.echo("hello")
```

Assert:

- response message is `hello`;
- response provider ID is the Provider's ID;
- Mock Gateway recorded the invocation;
- Consumer channel target equals Mock Gateway address;
- Mock Gateway Provider channel target equals Provider address;
- Consumer Provider-channel creation count is zero;
- Server/channel classes are not gRPC In-Process;
- full method name remains `/egon.rpc.test.v1.EchoService/Echo`;
- retry is disabled on the Consumer Channel and Mock Gateway Provider Channels;
- all contexts, subscriptions, schedulers, channels, servers, and executors terminate in teardown.

`RpcTcpCancellationTest` uses a blocking Provider method, cancels the Consumer context/call, and asserts the Mock Gateway's Provider `ClientCall` is cancelled and the Consumer receives `RPC_CANCELLED`.

### TDD steps

- [ ] Add boundary test that proves the Mock Gateway is test-only and the Starter has no
  Gateway production package or deferred Gateway type.
- [ ] Add the TCP/cancellation tests first.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am -Dtest=RpcTcpCallTest,RpcTcpCancellationTest,RpcTestBoundaryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: failure because the test registry integration/topology does not exist.

- [ ] Implement the deterministic registry facade and wire the existing Mock Gateway,
  only in test scope.
- [ ] Use Awaitility for every directory/channel readiness wait.
- [ ] Rerun focused tests and then ordinary RPC tests:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am test
  ```

- [ ] Guard against accidental in-process transport and sleeps:

  ```bash
  ! rg -n 'InProcessServerBuilder|InProcessChannelBuilder|Thread\\.sleep' \
    egon-cola-components/egon-cola-component-rpc
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "test(rpc): verify mock gateway TCP call path"
  ```

---

## Task 17: Verify test-only multi-Provider Directory convergence and eviction

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/RpcMultiProviderDirectoryTest.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockRpcGateway.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockProviderDirectory.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/mockgateway/MockRoundRobinSelector.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/support/InMemoryDdcRegistryBackend.java`

### Test flow

Use one Consumer, one Mock Gateway, and two Provider contexts exposing the same Echo Service Key but different instance IDs/ports.

1. Start both Providers and wait until `MockProviderDirectory` has two exact lease identities.
2. Make four Consumer calls.
3. Assert `MockRpcGateway` selected both Provider IDs and Consumer stayed on the one Gateway channel.
4. Deregister/stop Provider A.
5. Wait until the Mock Directory contains only Provider B and Provider A's cached
   test channel is evicted.
6. Make two more calls and assert both use Provider B.
7. Re-register Provider A with the same instance ID but a new lease.
8. Assert the Mock Directory replaces the old identity rather than duplicating it.
9. Advance the deterministic clock past A's lease without heartbeat.
10. Assert expiry removes A and Mock Gateway never forwards to the expired lease.

This test validates DDC discovery and removal through test fixtures only. Do not add
`RpcProviderDirectory`, a selector, or Provider Channel code to production Starter.

### TDD steps

- [ ] Add the test with explicit assertions on revisions, lease IDs, channel eviction, and Consumer Provider-channel count zero.
- [ ] Run:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am -Dtest=RpcMultiProviderDirectoryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected initially: a focused convergence/fixture failure.

- [ ] Extend only the test backend and test selector needed for deterministic replacement/expiry.
- [ ] Rerun focused and ordinary RPC tests.
- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "test(rpc): verify provider discovery and eviction"
  ```

---

## Task 18: Add opt-in independent-JVM DDC/Redis process verification

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/process/RpcMockGatewayApplication.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/process/RpcProcessHarness.java`
- Create: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/src/test/java/top/egon/cola/component/rpc/test/process/RpcProcessIT.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-consumer/src/main/java/top/egon/cola/component/rpc/test/consumer/RpcTestConsumerApplication.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite/pom.xml`

### Maven profile

Add `ddc-live-test` only in the test-suite POM:

- Maven Failsafe 3.2.5 binds `integration-test` and `verify`;
- includes only `**/RpcProcessIT.java`;
- normal Surefire excludes `*IT`;
- the profile is not active by default;
- no Testcontainers/Docker dependency is added.

### Process harness

Use `ProcessBuilder` and the Surefire/Failsafe test classpath to launch exactly four separate JVMs:

1. `DynamicConfigCenterAdminApplication`;
2. `RpcTestProviderApplication`;
3. `RpcMockGatewayApplication`;
4. `RpcTestConsumerApplication`.

The harness:

- uses the current Java executable;
- allocates distinct loopback ports;
- supplies Admin SQLite JDBC URL and `db/sqlite` Flyway location through command-line properties;
- supplies the single Redis host/port from `DDC_TEST_REDIS_HOST` and `DDC_TEST_REDIS_PORT`;
- enables registry/HMAC settings consistently;
- captures each process stdout/stderr in separate files under `target/rpc-process-it`;
- polls bounded health/readiness conditions instead of sleeping;
- terminates children in reverse order in `finally`;
- forcibly terminates only after a graceful timeout;
- includes captured logs in assertion failures.

The Consumer process performs one Echo call, prints one machine-readable success line containing invocation ID/provider ID/message, then exits `0`.

### Process assertions

`RpcProcessIT` proves:

- Admin applied V1 + V2 against SQLite;
- Provider is visible in DDC as `RPC_PROVIDER` with a lease;
- Mock Gateway is visible as the only `INTERNAL_GATEWAY` with a different lease/TTL;
- Consumer discovers only Mock Gateway and completes Consumer → Mock Gateway → Provider over TCP;
- stopping Provider actively deregisters it and the Mock Provider Directory removes
  its exact `instanceId + leaseId`;
- child exit codes and logs contain no leaked credential;
- every remaining child is stopped after the test.

### TDD steps

- [ ] Add the Failsafe test and profile wiring.
- [ ] Verify ordinary tests do not execute it:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am test
  ```

  Expected: ordinary TCP tests pass and `RpcProcessIT` is not run.

- [ ] With one external Redis single server available, run:

  ```bash
  DDC_TEST_REDIS_HOST=127.0.0.1 \
  DDC_TEST_REDIS_PORT=6379 \
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am -Pddc-live-test -Dit.test=RpcProcessIT verify
  ```

  Expected before harness completion: focused process readiness/call failure with captured child logs.

- [ ] Implement bounded readiness, result parsing, deregistration checks, and reverse cleanup.
- [ ] Rerun the live command. Expected: `BUILD SUCCESS`.
- [ ] Confirm no child remains:

  ```bash
  ! jps -l | rg 'DynamicConfigCenterAdminApplication|RpcTestProviderApplication|RpcMockGatewayApplication|RpcTestConsumerApplication'
  ```

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "test(rpc): verify independent process call path"
  ```

---

## Task 19: Document RPC usage and run the complete PR2 release gate

**Files:**

- Create: `egon-cola-components/egon-cola-component-rpc/README.md`
- Create: `egon-cola-components/egon-cola-component-rpc/README.zh-CN.md`
- Modify only if validation exposes drift: `egon-cola-components/egon-cola-component-rpc/**`

### Documentation

The English and Chinese READMEs must stay structurally aligned and include:

- module graph and BOM usage;
- standard `protobuf-maven-plugin` setup;
- one complete Proto, annotated Java Contract, Provider, and Consumer example;
- Provider/Consumer property reference;
- unique-Gateway startup/runtime behavior;
- Mock Gateway test topology and its non-production boundary;
- deadline, cancellation, status, trace, and Metadata rules;
- real TCP test and opt-in process-test commands;
- V1 non-goals: direct Provider, client LB, gray, retry, rate limiting, circuit
  breaking, production Gateway, Provider Directory, dynamic Gateway Handler, Provider
  Channel Factory, and Unary Forwarder.

Do not document Mock Gateway classes as reusable public APIs or production-ready.

### Final verification

- [ ] Run complete RPC unit and ordinary TCP suite:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am clean test
  ```

  Expected: all RPC Starter, Contract, Provider, Consumer, and Suite tests pass over real TCP where applicable.

- [ ] Run DDC + RPC combined regression:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-test,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am test
  ```

- [ ] Rerun opt-in process verification against one external Redis:

  ```bash
  DDC_TEST_REDIS_HOST=127.0.0.1 \
  DDC_TEST_REDIS_PORT=6379 \
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test/egon-cola-component-rpc-test-suite \
    -am -Pddc-live-test -Dit.test=RpcProcessIT verify
  ```

- [ ] Package production artifacts without starting them:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin,egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
    -am package -DskipTests
  ```

- [ ] Run architecture guards:

  ```bash
  ! rg -n 'InProcessServerBuilder|InProcessChannelBuilder|Thread\\.sleep|enableRetry\\(' \
    egon-cola-components/egon-cola-component-rpc

  ! rg -n 'RoundRobin|RandomSelector|LoadBalancer|CircuitBreaker|RateLimiter' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main

  test ! -d \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/gateway

  ! rg -n 'RpcGatewayNodeRegistrar|RpcProviderDirectory|RpcProviderChannelFactory|RpcGatewayHandlerRegistry|RpcUnaryForwarder' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main

  ! rg -n 'RPC_PROVIDER' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer

  rg -n 'disableRetry\\(' \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java

  test "$(find \
    egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-test \
    -path '*/src/main/proto/*.proto' | wc -l | tr -d ' ')" = "1"

  git diff --check
  ```

- [ ] Review the final dependency tree:

  ```bash
  ./mvnw -B -ntp \
    -pl egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter \
    -am dependency:tree \
    -Dincludes=io.grpc:*,com.google.protobuf:*,top.egon:egon-cola-component-dynamic-config-center-*
  ```

  Expected: Starter depends on DDC Starter and gRPC/Protobuf runtime only; it does not depend on DDC Admin, DDC Test, any RPC test module, or a third-party gRPC Spring Boot Starter.

- [ ] Commit:

  ```bash
  git add egon-cola-components/egon-cola-component-rpc
  git commit -m "docs(rpc): document mock gateway test boundary"
  ```

- [ ] Open PR2 only after every available command passes. PR2 title:

  ```text
  feat(rpc): add protobuf grpc starter and mock gateway tests
  ```

---

## 20. Final completion checklist

### PR1

- [ ] DDC contains no multi-Admin, Raft, Redis Cluster, Sentinel, or distributed-lock code.
- [ ] Every Register creates a new Admin-issued lease.
- [ ] Heartbeat/deregister atomically compare exact `instanceId + leaseId`.
- [ ] Config client, Provider, and Gateway-role registrations use one lease contract
  with separate defaults.
- [ ] Provider/Gateway service registration facts exist only in Redis.
- [ ] Config-client lifecycle starts, recovers, ACKs, and shuts down in the specified order.
- [ ] New publish requests accept caller changeId and only `SYNC_ALL_ACK`.
- [ ] Resource serialization is keyed by config resource; Waiters are keyed by changeId.
- [ ] Publish targets are fixed `instanceId + leaseId` rows.
- [ ] ACK matches changeId, version, content checksum, and exact target identity.
- [ ] Restart converts unfinished tasks to `UNKNOWN`; retry keeps original targets.
- [ ] Canonical HMAC verifies method/path/query/timestamp/nonce/body.
- [ ] Exactly one V2 logical migration exists in both supported dialect directories.
- [ ] DDC Starter/Admin/Test pass independently and Admin packages without starting.

### PR2

- [ ] RPC root has only Starter and Test aggregators; only Starter is in the BOM.
- [ ] Test Contract uses one real `.proto` and standard code generation.
- [ ] Java Contract is strictly validated against generated Proto/gRPC Descriptors.
- [ ] V1 supports only unary Protobuf Message contracts.
- [ ] Provider registers leases and gracefully deregisters/drains.
- [ ] Consumer discovers exactly one Gateway and never Provider.
- [ ] Zero/multiple Gateway states fail fast at startup and runtime.
- [ ] Every production channel disables gRPC Retry.
- [ ] Deadline, cancellation, status, trace, and Metadata tests pass.
- [ ] RPC Starter contains no production Gateway package, Directory, dynamic Handler,
  Provider Channel Factory, or Unary Forwarder.
- [ ] Mock Provider Directory discovers/removes instances only in test code.
- [ ] Mock Gateway selection/forwarding exists only in `rpc-test-suite/src/test`.
- [ ] Ordinary `mvn test` proves Consumer → Mock Gateway → Provider over real loopback
  TCP.
- [ ] Multi-Provider test proves Mock Directory convergence/eviction.
- [ ] Opt-in `mvn verify -Pddc-live-test` proves the four-JVM path with one Admin/Redis.
- [ ] RPC and combined DDC/RPC regression suites pass.

### Validation reporting

For each PR, record:

- exact commands run;
- passing test counts;
- any intentionally skipped external-Redis process test and the reason;
- final `git status --short`;
- commit list;
- any remaining risk.

Do not claim the live process test passed unless it was actually run against a reachable Redis single server. Do not start the application for manual testing after the automated gates; hand runtime testing back to the user.
