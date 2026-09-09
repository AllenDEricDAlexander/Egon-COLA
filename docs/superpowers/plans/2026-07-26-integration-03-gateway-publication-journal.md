# Integration 03 Yuheng Publication Journal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Yuheng Rule 发布改为每个 chunk/activation 可恢复、可对账、幂等的持久化阶段状态机。

**Architecture:** Yuheng Admin 在外部 Tianshu 调用前保存 publication operation，Coordinator 固定执行 chunks→activation，Adapter 只发布一个完整 operation。Reconciler 从第一个非成功 phase 继续，activation 成功后才更新 release 和 draft。

**Tech Stack:** Java 21、Spring JDBC、PostgreSQL、Flyway、Tianshu management-client、UUIDv7、JUnit 5。

## Global Constraints

- 依赖 Integration 02 exact GET 与 Tianshu 一致性合同。
- 不修改 Yuheng V1-V3；只新增一份 V4。
- 每个 operation 在网络调用前落库；同一次重试不得生成新 changeId。
- 任一 chunk 非 SUCCESS 时禁止 activation。
- release/draft 只在 activation SUCCESS 后推进。

---

### Task 1: 增加 publication journal schema 与 Store

**Files:**
- Create: `.../yuheng-admin/src/main/resources/db/migration/V4__add_release_publication_journal.sql`
- Create: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleasePublicationStore.java`
- Create: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/infrastructure/persistence/JdbcGatewayReleasePublicationStore.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/infrastructure/persistence/JdbcGatewayReleasePublicationStoreTest.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/migration/GatewayV4MigrationTest.java`

**Interfaces:**
- Produces `PublicationRecord`, `PhaseType`, `PublicationStatus` and store insert/update/query methods.
- Consumes releaseId/attemptNo from existing `GatewayReleaseStore`.

- [ ] **Step 1: Write failing migration/store tests**

```java
store.insert(List.of(new PublicationRecord(
        "release-1", 1, 0, CHUNK, "yuheng.rules.chunk.release-1.0",
        sha256, null, uuidV7, null, PLANNED, null, null, now, now
)));
assertThat(store.findAttempt("release-1", 1)).singleElement()
        .extracting(PublicationRecord::changeId).isEqualTo(uuidV7);
```

Assert duplicate `(release,attempt,phaseOrder)` and duplicate changeId are rejected.

- [ ] **Step 2: Run Yuheng Admin persistence tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-admin -am test \
  -Dtest=GatewayV4MigrationTest,JdbcGatewayReleasePublicationStoreTest
```

- [ ] **Step 3: Add V4 and exact store API**

V4 creates `gateway_release_publication` with primary key
`(release_id, attempt_no, phase_order)`, unique `change_id`, non-null content hash/status and
the composite foreign key `(release_id, attempt_no)` referencing
`gateway_release_attempt(release_id, attempt_no)`. `expected_version` remains nullable only while status is
`PLANNED`; the Store refuses `SUBMITTED` unless it has been resolved.

Store API:

```java
void insertAll(List<PublicationRecord> operations);
List<PublicationRecord> findAttempt(String releaseId, int attemptNo);
Optional<PublicationRecord> nextIncomplete(String releaseId, int attemptNo);
void resolveVersion(String changeId, long expectedVersion, Instant now);
void markSubmitted(String changeId, Instant now);
void markResult(String changeId, Long targetVersion, PublicationStatus status,
                String errorCode, String errorMessage, Instant now);
```

- [ ] **Step 4: Run migration/store tests and Admin module tests**

Run Step 2, then full Yuheng Admin tests. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-yuheng/egon-cola-component-yuheng-admin
git commit -m "feat: persist yuheng release publication phases"
```

### Task 2: 把 Tianshu Publisher 收敛为单 artifact Adapter

**Files:**
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/rule/GatewayDdcRulePublisher.java`
- Create: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/rule/GatewayDdcPublicationCommand.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/rule/GatewayDdcRulePublisherTest.java`

**Interfaces:**
- Consumes fully resolved appCode/env/namespace/configKey/value/expectedVersion/changeId/operator/timeout.
- Produces one `DdcManagementPublishResult`; no loop, no changeId concatenation, no null version.

- [ ] **Step 1: Replace recording-client happy path with strict contract tests**

```java
GatewayDdcPublicationCommand command = new GatewayDdcPublicationCommand(
        "yuheng-biz-gateway-default", "test", "default",
        "yuheng.rules.chunk.release-1.0", chunkValue, 1L,
        uuidV7, "admin", Duration.ofSeconds(30)
);
publisher.publish(command);
assertThat(client.request.expectedVersion()).isEqualTo(1L);
assertThat(UUID.fromString(client.request.changeId()).version()).isEqualTo(7);
```

Constructor validation rejects null version, non-v7 changeId and blank coordinate.

- [ ] **Step 2: Run publisher tests and confirm old aggregate API fails**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-admin -am test \
  -Dtest=GatewayDdcRulePublisherTest
```

- [ ] **Step 3: Implement single-command adapter**

```java
public DdcManagementPublishResult publish(GatewayDdcPublicationCommand c) {
    return client.publish(new DdcManagementPublishRequest(
            c.appCode(), c.env(), c.namespace(), c.configKey(), c.value(),
            c.expectedVersion(), c.changeId(), c.timeout().toMillis(),
            c.operator()
    ));
}
```

Keep `ensureReadyTarget(scope)` as a separate preflight method used once by the Coordinator.

- [ ] **Step 4: Re-run publisher/Admin tests**

Expected: PASS and no test double accepts an invalid request.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-yuheng/egon-cola-component-yuheng-admin
git commit -m "refactor: publish one yuheng tianshu artifact"
```

### Task 3: 创建并执行持久化 publication phases

**Files:**
- Create: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleasePublicationCoordinator.java`
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleaseService.java`
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/config/GatewayAdminConfiguration.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleasePublicationCoordinatorTest.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleaseServiceTest.java`

**Interfaces:**
- Produces `PublicationOutcome execute(releaseId, attemptNo, compiled, actorId)`.
- Consumes exact config GET/upsert and `UuidV7.simpleString()`.

- [ ] **Step 1: Write phase barrier and lost-response tests**

Cover:

```text
missing chunk config -> upsert expected 0 -> exact GET -> journal expectedVersion
chunk 0 SUCCESS, chunk 1 FAILED -> activation never called
publish response lost -> getPublishTask(same changeId) -> mark SUCCESS
process restart -> existing journal reused, no new UUID/version
activation SUCCESS -> only then Release SUCCESS and Draft baseOn
```

- [ ] **Step 2: Run coordinator/service tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-admin -am test \
  -Dtest=GatewayReleasePublicationCoordinatorTest,GatewayReleaseServiceTest
```

- [ ] **Step 3: Implement deterministic operation construction and execution**

Build ordered phases from `CompiledGatewayRelease`:

```java
List<Artifact> artifacts = Stream.concat(
        sortedChunks.stream().map(chunk -> Artifact.chunk(chunk, value)),
        Stream.of(Artifact.activation(ACTIVE_CONFIG_KEY, activationJson))
).toList();
```

Persist every phase as `PLANNED` with configKey/content hash/content/UUIDv7 before the first Tianshu request. For each
phase, exact GET/create then stores expectedVersion and changes status to `RESOLVED` before publish. Execute or
reconcile only `RESOLVED` rows. A crash cannot lose operation identities, and no publish occurs with a nullable
version.

- [ ] **Step 4: Run focused tests and Yuheng Admin test suite**

Expected: PASS, including inline activation with zero chunks.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-yuheng/egon-cola-component-yuheng-admin
git commit -m "feat: coordinate recoverable yuheng publication"
```

### Task 4: 按 phase 恢复 Release

**Files:**
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/interfaces/scheduled/GatewayReleaseReconciler.java`
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleaseStore.java`
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/infrastructure/persistence/JdbcGatewayReleaseStore.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/interfaces/scheduled/GatewayReleaseReconcilerTest.java`

**Interfaces:**
- Consumes journal `nextIncomplete` rather than release-level `changeId`.
- Produces terminal release only after all operations are terminal and activation is SUCCESS.

- [ ] **Step 1: Write crash-at-every-phase tests**

For a three-chunk release, parameterize crash phase 0..3. On reconcile, assert completed phases are queried but
not republished, the interrupted phase reuses changeId, later phases execute in order, and Draft is updated once.

- [ ] **Step 2: Run reconciler tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-admin -am test \
  -Dtest=GatewayReleaseReconcilerTest
```

- [ ] **Step 3: Delegate reconciliation to the Coordinator**

The scheduler loads attempts having a journal row outside SUCCESS and calls
`coordinator.resume(releaseId, attemptNo)`. `GatewayReleaseStore.recoverable()` no longer requires the legacy
release-level `change_id`. It maps Tianshu PARTIAL_SUCCESS/TIMEOUT/UNKNOWN without upgrading the release and removes
the old single-changeId task query path.

- [ ] **Step 4: Run full Yuheng Admin tests**

Expected: PASS and no old non-UUID changeId generation remains:

```bash
rg -n 'yuheng-release-|changeId \+ "-chunk"' \
  egon-cola-components/egon-cola-component-yuheng/egon-cola-component-yuheng-admin/src
```

Expected search output: none in production code.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-yuheng/egon-cola-component-yuheng-admin
git commit -m "fix: resume yuheng release by publication phase"
```

### Task 5: 关闭 chunk 生命周期

**Files:**
- Create: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/interfaces/scheduled/GatewayRuleChunkGarbageCollector.java`
- Modify: `.../yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/config/GatewayAdminProperties.java`
- Modify: `.../yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/rule/GatewayRuleChunkStore.java`
- Modify: `.../yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/rule/GatewayRuleActivationApplier.java`
- Test: `.../yuheng-admin/src/test/java/top/egon/cola/component/yuheng/admin/interfaces/scheduled/GatewayRuleChunkGarbageCollectorTest.java`
- Test: `.../yuheng-biz-gateway/src/test/java/top/egon/cola/component/yuheng/engine/rule/GatewayRuleActivationApplierTest.java`

**Interfaces:**
- Produces 24h default old-release retention and active release protection.
- Produces local `GatewayRuleChunkStore.removeRelease(releaseId)` after successful activation/LKG save.

- [ ] **Step 1: Write active-protection and local-cleanup tests**

Assert current active chunks are never deleted, predecessor is retained until 24h after successor success, and
only journal-known config/version pairs are CAS-deleted. Assert Engine clears assembled chunks only after LKG
write and active swap both succeed.

- [ ] **Step 2: Run GC/activation tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-admin,\
egon-cola-components/egon-cola-component-yuheng/\
egon-cola-component-yuheng-biz-gateway -am test \
  -Dtest=GatewayRuleChunkGarbageCollectorTest,GatewayRuleActivationApplierTest
```

- [ ] **Step 3: Implement bounded cleanup**

Use journal data as the only deletion source. Collector skips current active and previous retention window,
deletes through `DdcManagementClient.delete` with expectedVersion, records metrics, and retries on later runs.
No Redis scan or wildcard delete is allowed.

- [ ] **Step 4: Run Yuheng Admin + Engine tests**

Expected: PASS and bounded local chunk count after repeated releases.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-yuheng
git commit -m "feat: bound yuheng rule chunk lifecycle"
```
