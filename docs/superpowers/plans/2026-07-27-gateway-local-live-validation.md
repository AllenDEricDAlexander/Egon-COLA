# Gateway Local Live Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不使用 Docker 的前提下，用本机进程验证 DDC、Gateway、HTTP Provider、RPC Provider 和 RPC Consumer 的服务注册发现、规则路由、HTTP/RPC 调用与 Gateway 流量 PostgreSQL 落库闭环。

**Architecture:** 保留 `GatewayTestInfrastructure` 作为 live 套件门面，用 Strategy 将既有 Testcontainers 后端和新增 host-local 后端隔离。host-local 后端只管理测试专属的临时 PostgreSQL、两套 Redis 与进程内 KRaft Kafka，业务 JVM 与现有 `GatewayLiveTopologyIT` 完全复用，确保验收的生产代码路径不变。

**Tech Stack:** JDK 21、Maven Failsafe、JUnit 5、PostgreSQL 18 本机二进制、Redis 7 本机二进制、Spring Kafka Test KRaft broker、Spring Boot 多 JVM 测试应用。

## Global Constraints

- 只在 `main` 分支修复；用户已明确授权。
- 不创建 worktree，不创建子代理，不使用 Docker 或 Testcontainers 执行本轮 live 验证。
- 不访问或清空用户现有数据库、Redis keyspace；所有状态使用临时目录、随机端口和两个独立 Redis 进程。
- 只有真实复现的代码缺陷才修改生产代码；每个缺陷先得到失败用例，再做最小修复。
- 用户未跟踪文件保持原样，任何提交都使用精确路径暂存。
- 验证结束必须停止 DDC、Gateway、Provider、Consumer、Kafka、Redis 与 PostgreSQL 临时进程。

---

### Task 1: 建立默认测试基线和无 Docker 失败证据

**Files:**
- Read: `egon-cola-components/egon-cola-component-dynamic-config-center/pom.xml`
- Read: `egon-cola-components/egon-cola-component-gateway/pom.xml`
- Read: `egon-cola-components/egon-cola-component-rpc/pom.xml`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayInfrastructureLiveIT.java`

**Interfaces:**
- Consumes current `main` and the existing opt-in `gateway-live` profile.
- Produces module-test baselines and a reproducible failure showing that `gateway.live.infrastructure=local` currently still reaches Docker.

- [ ] **Step 1: Run the three focused component reactors**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-dynamic-config-center,egon-cola-component-rpc,egon-cola-component-gateway \
  -am test
```

- [ ] **Step 2: Run the existing infrastructure IT in requested local mode**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local \
  -Dit.test=GatewayInfrastructureLiveIT verify
```

Expected before Task 2: FAIL because `GatewayTestInfrastructure` ignores the local mode and initializes Testcontainers without Docker.

- [ ] **Step 3: Preserve diagnostics and confirm no source mutation**

```bash
git status --short
git diff --check
```

- [ ] **Step 4: Commit this execution plan only**

```bash
git add docs/superpowers/plans/2026-07-27-gateway-local-live-validation.md
git commit -m "docs: plan gateway local live validation"
```

### Task 2: 增加 host-local live infrastructure Strategy

**Files:**
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/pom.xml`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayTestInfrastructure.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayInfrastructureBackend.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayTestcontainersInfrastructure.java`
- Create: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayLocalInfrastructure.java`
- Modify: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyIT.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayTestInfrastructureTest.java`

**Interfaces:**
- `GatewayInfrastructureBackend` produces `start()`, `createDatabase(String)`, `jdbcUrl(String)`, PostgreSQL credentials, two distinct Redis endpoints, Kafka bootstrap servers and idempotent `close()`.
- `GatewayTestInfrastructure` selects `testcontainers` by default and `local` only for `-Dgateway.live.infrastructure=local`.
- `GatewayLocalInfrastructure` owns every child process and deletes only its own temporary directory after bounded shutdown.

- [ ] **Step 1: Add a failing mode-selection test**

```java
@Test
void selectsLocalInfrastructureFromSystemProperty() {
    String previous = System.getProperty("gateway.live.infrastructure");
    try {
        System.setProperty("gateway.live.infrastructure", "local");
        assertThat(new GatewayTestInfrastructure().type())
                .isEqualTo("local");
    } finally {
        restore("gateway.live.infrastructure", previous);
    }
}
```

- [ ] **Step 2: Run the test and observe RED**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test -Dtest=GatewayTestInfrastructureTest
```

Expected: FAIL because the current infrastructure has no selectable backend or `type()` contract.

- [ ] **Step 3: Implement the minimal Strategy and local resource lifecycle**

Use `initdb`/`pg_ctl` with trust authentication inside a unique temporary directory, start two `redis-server` processes with distinct random ports and disabled persistence, then start `EmbeddedKafkaKraftBroker`. Readiness must poll actual PostgreSQL, Redis and Kafka endpoints; shutdown must reverse startup order and forcibly terminate only owned processes after timeout.

- [ ] **Step 4: Replace Testcontainer-specific credential access in the topology test**

Replace `infrastructure.postgres().getUsername()` and `.getPassword()` with the backend-neutral `postgresUsername()` and `postgresPassword()` methods. Do not change route, registry, protocol or persistence assertions.

- [ ] **Step 5: Run GREEN unit and infrastructure live tests**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am test -Dtest=GatewayTestInfrastructureTest
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local \
  -Dit.test=GatewayInfrastructureLiveIT verify
```

- [ ] **Step 6: Commit the local infrastructure support**

```bash
git add \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/pom.xml \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/pom.xml \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process \
  egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyIT.java
git commit -m "test: support gateway live topology without Docker"
```

### Task 3: 完成本机 DDC/Gateway HTTP、RPC 与落库闭环

**Files:**
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayHttpProvidersLiveIT.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayRpcDualEngineLiveIT.java`
- Test: `egon-cola-components/egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayInfrastructureLiveIT.java`

**Interfaces:**
- Produces HTTP Provider registration/discovery, Rule Snapshot activation, public/internal route behavior, distributed rate limit and Kafka-to-PostgreSQL trace projection evidence.
- Produces RPC Provider and `INTERNAL_GATEWAY` registration/discovery, RPC Consumer-to-Gateway-to-Provider and HTTP-to-RPC route evidence.

- [ ] **Step 1: Run the HTTP and infrastructure scenarios**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local \
  -Dit.test=GatewayInfrastructureLiveIT,GatewayHttpProvidersLiveIT verify
```

- [ ] **Step 2: Run the RPC dual-Engine scenario**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-gateway/egon-cola-component-gateway-test/egon-cola-component-gateway-test-suite \
  -am -Pgateway-live -Dgateway.live.infrastructure=local \
  -Dit.test=GatewayRpcDualEngineLiveIT verify
```

- [ ] **Step 3: If a business-path failure appears, perform root-cause/TDD repair**

Capture the failing boundary and logs, add the smallest behavior test that fails for the identified cause, implement one minimal fix, run its focused test, then rerun the affected live scenario. Do not combine unrelated repairs.

- [ ] **Step 4: Run final related regression**

```bash
./mvnw -B -ntp -f egon-cola-components/pom.xml \
  -pl egon-cola-component-dynamic-config-center,egon-cola-component-rpc,egon-cola-component-gateway \
  -am test
git diff --check
git status --short --branch
```

- [ ] **Step 5: Confirm cleanup**

Verify that no test-owned Java, PostgreSQL, Redis or Kafka process remains and that both user-owned untracked files are unchanged.
