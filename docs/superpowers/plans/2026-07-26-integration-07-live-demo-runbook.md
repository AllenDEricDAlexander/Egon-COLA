# Integration 07 Live Topology, Compose Demo and Runbook Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用真实 PostgreSQL、两套 Redis、Kafka、双 Engine、MVC/WebFlux/RPC 示例证明完整闭环，并提供一条命令化人工联调路径和中英文 Runbook。

**Architecture:** Testcontainers/Failsafe 是自动化验收事实，Compose Demo 是开发者人工联调入口；两者复用同一组应用、规则 fixture、端口和成功判据。测试 Fixture 统一管理容器、子进程、日志、LKG 与清理。

**Tech Stack:** Maven Failsafe、JUnit 5、Testcontainers、PostgreSQL、Redis、Kafka、Docker Compose、Bash、curl、jq。

## Global Constraints

- 依赖 Integration 01-06。
- 默认 Surefire 不启动外部拓扑；真实进程仅在 `gateway-live` 显式 profile。
- DDC Redis 与 Rate Redis 必须物理分离并有行为断言。
- 每个测试 scope、数据库、进程名、日志和 LKG 目录唯一。
- 外部 Redis 禁止 `FLUSHALL`；Compose 清理只针对唯一 project name。
- 不自动遗留项目、容器或后台进程。

---

### Task 1: 拆分并加固 live test fixture

**Files:**
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveEnvironment.java`
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayAdminTestClient.java`
- Modify: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayProcessSpec.java`
- Modify: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayProcessHarness.java`
- Modify: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayTestInfrastructure.java`
- Test: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayProcessHarnessTest.java`

**Interfaces:**
- Produces one closeable environment owning containers and JVMs.
- Produces unique names `gateway-engine-1`, `gateway-engine-2` and per-instance LKG/log/manifest paths.
- Produces start/stop/kill/restart/wait APIs.

- [ ] **Step 1: Write failing process isolation/restart tests**

```java
assertThat(engineOne.logFile()).isNotEqualTo(engineTwo.logFile());
assertThat(engineOne.manifestFile()).isNotEqualTo(engineTwo.manifestFile());
harness.kill(provider);
ProcessHandle restarted = harness.restart(provider);
assertThat(restarted.pid()).isNotEqualTo(firstPid);
```

Assert `close()` reverses startup order and forcibly stops timed-out processes while preserving diagnostic logs.

- [ ] **Step 2: Run harness tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test -Dtest=GatewayProcessHarnessTest
```

- [ ] **Step 3: Extract Test Fixture + typed Admin Facade**

Move resource ownership from the 1600-line IT without changing production behavior. `GatewayAdminTestClient`
contains actual DTO/JSON methods for application, credential, group, draft, validate, release, rollback, runtime
consistency, provider projection and trace lookup.

- [ ] **Step 4: Run all default Gateway tests**

Expected: PASS; `gateway-live` remains opt-in.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test
git commit -m "test: isolate gateway live topology fixture"
```

### Task 2: 验证 MVC/WebFlux Provider 与租约恢复

**Files:**
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayHttpProvidersLiveIT.java`
- Modify: fixture/application settings required to start both Provider JARs.

**Interfaces:**
- Consumes MVC and WebFlux Provider modules with same service key and distinct instanceId.
- Produces PUBLIC/INTERNAL HTTP assertions and graceful/TTL/new-lease recovery evidence.

- [ ] **Step 1: Write the live scenario before changing fixtures**

Scenario:

```text
start MVC + WebFlux -> catalog and provider projection contain both
publish route       -> repeated calls observe providerType mvc and webflux
PUBLIC internal-only route -> 404; INTERNAL -> 200
graceful stop MVC   -> immediate projection removal, WebFlux serves all calls
restart same MVC instanceId -> new leaseId, both serve
kill WebFlux        -> removed after TTL, MVC serves
restart WebFlux     -> new leaseId, both serve again
```

- [ ] **Step 2: Run only this Failsafe IT**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dit.test=GatewayHttpProvidersLiveIT verify
```

Expected before fixture completion: FAIL with missing WebFlux process/registration.

- [ ] **Step 3: Add both process specs and deterministic response identity**

Pass Testcontainers DDC Redis host/port, unique advertised ports, matching version and unique zone/provider type.
Use bounded polling on Admin projections; no fixed sleeps.

- [ ] **Step 4: Re-run the live IT**

Expected: PASS with old/new lease IDs captured in assertion output.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test
git commit -m "test: verify mvc and webflux gateway providers"
```

### Task 3: 验证双 Engine RPC 与规则/基础设施生命周期

**Files:**
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayRpcDualEngineLiveIT.java`
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayRuleLifecycleLiveIT.java`
- Create: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayInfrastructureLiveIT.java`
- Reduce/replace: existing `GatewayLiveTopologyIT.java` after all assertions move.

**Interfaces:**
- Produces HTTP→RPC and RPC Consumer→Gateway→Provider evidence for both Engine IDs.
- Produces v1/v2/rollback/LKG, distributed rate and Kafka Trace evidence.

- [ ] **Step 1: Add failing RPC dual-Engine tests**

Invoke an idempotent Echo repeatedly and assert Kafka/Admin Trace contains both engineInstanceIds. Stop one
Engine and assert Consumer fails over; restart it with new slot lease and assert it re-enters rotation. Invoke
non-idempotent CreateOrder with injected Gateway failure and assert no second Provider call.

- [ ] **Step 2: Add failing rule/infrastructure tests**

```text
release v1 -> both Engines consistent
release v2 -> both Engines active v2
rollback v1 content -> new rollback release, both Engines consistent
distributed token bucket -> Engine1 200, Engine2 429
Kafka trace -> exact protocol/provider/engine/trace fields
restart Engine with same LKG -> rule restored, readiness waits for topology
corrupt checksum -> fail closed
restart Admin/DDC -> PostgreSQL facts retained
```

- [ ] **Step 3: Run the three focused live ITs**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live \
  -Dit.test=GatewayRpcDualEngineLiveIT,GatewayRuleLifecycleLiveIT,GatewayInfrastructureLiveIT \
  verify
```

Expected: FAIL until all process/config/rule fixtures are complete.

- [ ] **Step 4: Complete fixture wiring and rerun**

Use DISTRIBUTED policy, Rate Redis address, unique Engine RPC advertised host/port and unique LKG directories.
All waits inspect readiness/release/projection rather than sleep. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test
git commit -m "test: verify gateway rpc and rule lifecycle"
```

### Task 4: 增加 Compose Demo 与可重复脚本

**Files:**
- Create: `.../gateway/deployment/compose.demo.yml`
- Create: `.../gateway/deployment/Dockerfile.test-app`
- Create: `.../gateway/deployment/demo/applications.json`
- Create: `.../gateway/deployment/demo/routes.json`
- Create: `.../gateway/deployment/demo/policies.json`
- Create: `.../gateway/deployment/scripts/demo.sh`
- Create: `.../gateway/deployment/scripts/demo-token.sh`
- Create: `.../gateway/deployment/scripts/wait-ready.sh`
- Modify: `.../gateway/deployment/.env.example`
- Test: `.../gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/deployment/GatewayDemoScriptContractTest.java`

**Interfaces:**
- Produces commands `doctor|build|up-control|init|up-providers|publish|up-consumer|verify|logs|down|purge`.
- Produces deterministic ports 18070-19190 and unique Compose project name.

- [ ] **Step 1: Write shell/Compose behavior tests**

Run every subcommand with fake `docker`, `docker compose` and `curl` executables placed first on `PATH`.
Assert observable argv, exit status and filesystem effects: secrets are not printed, `down` never passes `-v`,
`purge` refuses an unmarked/non-local project and only a marked local project reaches `down -v`. Parse the
rendered Compose model to assert advertised hosts resolve through service DNS names. Do not grep shell source
text for implementation details such as `set -euo pipefail`.

- [ ] **Step 2: Run static test and Compose config**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test -Dtest=GatewayDemoScriptContractTest

docker compose --env-file \
  egon-cola-components/egon-cola-component-gateway/deployment/.env.example \
  -f egon-cola-components/egon-cola-component-gateway/deployment/compose.yml \
  -f egon-cola-components/egon-cola-component-gateway/deployment/compose.demo.yml \
  config --quiet
```

- [ ] **Step 3: Implement the command facade and fixtures**

`demo.sh init` generates a local JWT, creates applications/credentials/groups; `publish` uploads fixture
routes/policies, validates and waits for release SUCCESS; `verify` calls MVC, WebFlux, HTTP→RPC and RPC Consumer,
then checks runtime consistency and Trace. Runtime-generated secrets live in gitignored mode-0600 files.

- [ ] **Step 4: Run a real clean Demo lifecycle**

```bash
cd egon-cola-components/egon-cola-component-gateway/deployment
./scripts/demo.sh doctor
./scripts/demo.sh build
./scripts/demo.sh up-control
./scripts/demo.sh init
./scripts/demo.sh up-providers
./scripts/demo.sh publish
./scripts/demo.sh up-consumer
./scripts/demo.sh verify
./scripts/demo.sh down
```

Expected: every step exits 0 and `docker compose ps` has no running project containers after down. Preserve
logs as verification artifacts before cleanup.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway/deployment \
        egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test
git commit -m "feat: add gateway integration demo"
```

### Task 5: 编写并验证中英文联调 Runbook

**Files:**
- Create: `.../gateway/docs/developer-integration.zh-CN.md`
- Create: `.../gateway/docs/developer-integration.md`
- Modify: Gateway/DDC/RPC root `README.md` and `README.zh-CN.md` files.
- Modify: `.../gateway/deployment/README.md`
- Modify: `.../gateway/deployment/README.zh-CN.md`
- Modify: Admin Web README files to remove nonexistent `VITE_GATEWAY_ADMIN_ACTOR_ID`.

**Interfaces:**
- Consumes only commands and ports implemented by Task 4.
- Produces environment, startup, verification, failure, logs, metrics, stop/cleanup and troubleshooting guidance.

- [ ] **Step 1: Inventory executed evidence and documentation targets**

Use the Task 4 command transcripts, rendered Compose model and actual `demo.sh --help` output as the source of
truth. Resolve every relative README link and inventory obsolete variables before editing. Do not add a unit
test that parses prose or shell source.

- [ ] **Step 2: Run current module tests before documentation edits**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-gateway/\
egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite -am test
```

- [ ] **Step 3: Write both Runbooks from executed evidence**

Include prerequisites, port table, exact startup order, JWT generation, init/publish/call commands, readiness
transitions, release/Provider/Trace success evidence, MVC/WebFlux/RPC examples, fault drills, log/metric paths,
data-preserving down, destructive purge warning and symptom-first troubleshooting. Mark unrun TLS/Cluster/HA
scenarios explicitly unverified.

- [ ] **Step 4: Re-run documentation/static/real verification**

Run Step 2, `demo.sh --help`, `docker compose config --quiet`, the documented Demo lifecycle including
`demo.sh verify`, a relative-link resolver, `rg 'VITE_GATEWAY_ADMIN_ACTOR_ID'` expecting no matches, and
`git diff --check`. Expected: PASS. Record the exact evidence in the final acceptance report rather than
encoding prose structure into a change-detector test.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-gateway \
        egon-cola-components/egon-cola-component-dynamic-config-center \
        egon-cola-components/egon-cola-component-rpc
git commit -m "docs: add gateway ddc rpc integration runbook"
```

### Task 6: 最终完成审计

**Files:**
- Create: `docs/superpowers/specs/2026-07-26-gateway-ddc-rpc-integration-acceptance.md`

**Interfaces:**
- Consumes all seven implementation plans.
- Produces requirement-by-requirement evidence for the approved design completion definition.

- [ ] **Step 1: Run three component reactors**

Run the commands in the plan index and preserve Maven summaries.

- [ ] **Step 2: Run `gateway-live` from a clean build**

Expected: all real topology ITs PASS; container/process teardown completes.

- [ ] **Step 3: Run Compose static and Demo black-box verification**

Expected: config and Demo verify PASS, then `down` leaves no project processes.

- [ ] **Step 4: Audit every P0/P1/P2 against authoritative evidence**

For each design section 4 item, link source/test/runtime evidence and mark PASS or an explicit blocker. A missing
live Cluster/Sentinel/TLS/HA environment remains an unverified boundary, not a false pass.

- [ ] **Step 5: Write and commit the final evidence report**

```bash
git add docs/superpowers/specs/2026-07-26-gateway-ddc-rpc-integration-acceptance.md
git commit -m "docs: record integration closure evidence"
```
