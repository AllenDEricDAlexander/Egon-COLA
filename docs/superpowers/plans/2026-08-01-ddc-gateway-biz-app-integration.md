# Tianshu Yuheng Biz-App Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. This task must be executed inline in the current workspace; the user explicitly prohibited subagents.

**Goal:** Restore a cleanly compiling and runnable Tianshu/Yuheng/Provider topology after Tianshu service identity changed to `bizCode + appCode + env + namespace`.

**Architecture:** Keep Tianshu as the owner of registry identity and keep Yuheng's existing Adapter boundary. Provider scope travels from Yuheng Starter reporting through Admin release snapshots into Engine provider keys, while each runtime that registers its own service uses `DdcServiceKeyFactory` so it cannot omit the local Tianshu biz/app scope. RPC consumers may explicitly target a Yuheng biz/app scope and otherwise fall back to their local Tianshu scope.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, JUnit 5, PostgreSQL, Redis, React 19, Vite.

## Global Constraints

- Execute inline on the current `main` checkout; do not create a subagent or worktree.
- Preserve the committed Tianshu `biz-ns-env-app` model and the existing Yuheng UI work.
- Add no dependencies and perform no unrelated refactoring.
- Use one failing behavior test before each production behavior change.
- Use the existing Adapter and Factory patterns; do not add another abstraction layer.
- Do not modify any existing Flyway migration. Add exactly one new Yuheng Admin migration for the application `biz_code`; provider biz/app scope continues to live in the operation provider identity JSON.
- Use host-local Redis and PostgreSQL; do not substitute Testcontainers evidence for the requested live topology.
- Leave the requested Tianshu, Yuheng, Web UI, and backend processes running after verification.

---

### Task 1: Restore Tianshu-scoped runtime registrations

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/config/DdcRegistryAutoConfig.java`
- Modify: `egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-starter/src/main/java/top/egon/cola/component/tianshu/registry/DdcServiceKeyFactory.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-provider-runtime/src/main/java/top/egon/cola/component/yuheng/provider/HttpProviderLeaseRuntime.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-provider-runtime/src/main/java/top/egon/cola/component/yuheng/provider/GatewayHttpProviderAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderLeaseManager.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManager.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/rpc/RpcGatewaySlotRuntime.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/GatewayEngineConfiguration.java`
- Modify: focused tests beside each runtime.

**Interfaces:**
- `DdcRegistryAutoConfig` produces a singleton `DdcServiceKeyFactory` from `DdcProperties`.
- `DdcServiceKeyFactory` produces both local-scope keys and explicitly targeted Yuheng keys while applying the same validation.
- HTTP and RPC Providers consume that factory and register their service under the local Tianshu `bizCode/appCode/env/namespace`.
- Yuheng Engine consumes the same factory when it registers its own `INTERNAL_GATEWAY` slot.
- RPC Consumer properties produce optional `gatewayBizCode` and `gatewayAppCode`; blank values fall back to local Tianshu properties.

- [ ] **Step 1: Add failing registration-scope tests**

  Assert the real `DdcServiceRegistration.serviceKey()` observed by the fake registry has literal scope `retail-biz/orders-app/test/yuheng-test`. Add a consumer test where the local app differs from `xingyuan-biz/yuheng-app` and assert the subscription uses the explicit Yuheng target scope.

- [ ] **Step 2: Run RED tests**

  ```bash
  ./mvnw -B -ntp -f egon-cola-xingyuan/egon-cola-yuheng/pom.xml \
    -pl yuheng-provider-runtime -am \
    -Dtest=HttpProviderLeaseRuntimeTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml \
    -pl egon-cola-component-rpc-starter -am \
    -Dtest=RpcProviderLifecycleTest,RpcConsumerGatewayManagerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: scope assertions or compilation fail because current runtimes still call the obsolete seven-field Tianshu constructors.

- [ ] **Step 3: Implement the minimal factory/target-scope wiring**

  Register `DdcServiceKeyFactory` once and replace local-scope direct constructors with `factory.fromScope(...)`. For RPC Consumer discovery only, construct the key from explicit Yuheng biz/app when configured, otherwise from local `DdcProperties`; keep env/namespace and service/group/version behavior unchanged.

- [ ] **Step 4: Run GREEN focused tests and clean compilation**

  Run the commands from Step 2, then run clean compile for the two changed reactors.

- [ ] **Step 5: Hold the atomic compatibility checkpoint**

  Do not commit yet: Yuheng Engine cannot cleanly compile until Task 2 updates
  the Tianshu Provider Adapter in the same source state. Commit Tasks 1 and 2
  together after the full cross-module compatibility path is green.

### Task 2: Carry provider biz/app through Yuheng releases and discovery

**Files:**
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/yuheng/starter/GatewayReportingProperties.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/yuheng/contract/reporting/GatewayInterfaceDefinitionReport.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-contract/src/main/java/top/egon/cola/component/yuheng/contract/rule/GatewayProviderServiceRef.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-core/src/main/java/top/egon/cola/component/yuheng/core/provider/ProviderQuery.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-core/src/main/java/top/egon/cola/component/yuheng/core/provider/ProviderServiceKey.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/yuheng/starter/discovery/GatewayHttpOperationMapper.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-starter/src/main/java/top/egon/cola/component/yuheng/starter/discovery/RpcGatewayDefinitionContributor.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/release/GatewayReleaseService.java`
- Modify: Yuheng Admin application entity/service/controller and add `V5__add_gateway_application_biz_scope.sql`.
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/catalog/GatewayCatalogStore.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/infrastructure/persistence/JdbcGatewayCatalogStore.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/application/catalog/GatewayCatalogService.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin/src/main/java/top/egon/cola/component/yuheng/admin/rule/GatewayRuleCompiler.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/rule/EngineGatewayRuleCompiler.java`
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-biz-gateway/src/main/java/top/egon/cola/component/yuheng/engine/discovery/DdcProviderServiceRegistryAdapter.java`
- Modify: focused contract, starter, admin, core, and engine tests.

**Interfaces:**
- `GatewayInterfaceDefinitionReport.ProviderService`, `GatewayProviderServiceRef`, `ProviderServiceKey`, and `ProviderQuery` all carry required `bizCode` and `appCode`.
- Starter reporting takes `bizCode` from `egon.cola.component.yuheng.reporting.biz-code` and `appCode` from the existing `application-code`.
- Yuheng Admin persists each application's owning `bizCode`; existing application codes remain globally unique, matching Tianshu's managed application constraint.
- Manual operations derive `bizCode` from their containing Yuheng business-domain node and `appCode` from the containing application.
- `DdcProviderServiceRegistryAdapter` maps all nine Tianshu query/key fields without guessing defaults.

- [ ] **Step 1: Add failing scope-isolation tests**

  Add literal tests proving two otherwise identical Providers from different biz/apps are different keys; a Tianshu catalog query contains the release's biz/app; and a Starter report retains configured biz/app into the Provider Service identity.

- [ ] **Step 2: Run RED tests**

  ```bash
  ./mvnw -B -ntp -f egon-cola-xingyuan/egon-cola-yuheng/pom.xml \
    -pl yuheng-starter,yuheng-admin,yuheng-biz-gateway \
    -am -Dtest=ProviderModelTest,GatewayDefinitionReportFactoryTest,GatewayCatalogServiceTest,ProviderDirectoryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: new scope expectations fail and the clean Engine compile reports obsolete Tianshu constructors.

- [ ] **Step 3: Implement end-to-end scope propagation**

  Add required fields at each identity boundary, update canonical ordering and equality through record components, update manual and reported provider identity creation, and map both directions in the existing Tianshu Adapter. Do not derive appCode from serviceName.

- [ ] **Step 4: Run GREEN tests and Yuheng clean reactor test**

  Run Step 2, then:

  ```bash
  ./mvnw -B -ntp -f egon-cola-xingyuan/egon-cola-yuheng/pom.xml clean test
  ```

- [ ] **Step 5: Commit the atomic Tasks 1-2 compatibility change**

  ```bash
  git commit -m "fix: propagate Tianshu application scope through yuheng"
  ```

### Task 3: Align runnable configuration and live fixtures

**Files:**
- Modify: Yuheng Engine and test application YAML files that enable Tianshu registration/reporting.
- Modify: `egon-cola-xingyuan/egon-cola-yuheng/yuheng-test/yuheng-test-suite/src/test/java/top/egon/cola/component/yuheng/test/live/GatewayLiveTopologyIT.java` only where live child JVM properties require biz/app scope.
- Modify: RPC README examples only if the new explicit Yuheng target properties are otherwise undocumented.

**Interfaces:**
- Local test scope is `demo-biz / yuheng-test-http-provider / test / yuheng-test` for the HTTP backend.
- Yuheng Engine local identity is `xingyuan-biz / egon-cola-yuheng-biz-gateway / local / default`.
- Tianshu Admin runs on 18080; Yuheng Admin runs on 8080; Engine data plane remains 18083; web UIs use 5173/5174.

- [ ] **Step 1: Add scope properties to enabled runtime fixtures**

  Add explicit non-secret biz/app values wherever Tianshu registry or Yuheng reporting is enabled. Keep disabled fixtures unchanged unless their tests validate property binding.

- [ ] **Step 2: Run component and Web UI baselines**

  ```bash
  ./mvnw -B -ntp -f egon-cola-xingyuan/egon-cola-tianshu/pom.xml clean test
  ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml clean test
  ./mvnw -B -ntp -f egon-cola-xingyuan/egon-cola-yuheng/pom.xml clean test
  npm --prefix egon-cola-xingyuan/egon-cola-tianshu/egon-cola-tianshu-admin-web run build
  npm --prefix egon-cola-xingyuan/egon-cola-yuheng/yuheng-admin-web run build
  ```

- [ ] **Step 3: Commit Task 3 if tracked configuration changed**

  ```bash
  git commit -m "test: align yuheng live scope configuration"
  ```

### Task 4: Start and verify the host-local topology

**Files:**
- Runtime-only: `target/local-dev-run/` for logs and process state; these files remain ignored and must not expose credentials.

**Interfaces:**
- Tianshu Admin readiness: `http://127.0.0.1:18080/actuator/health/readiness`.
- Yuheng Admin readiness: `http://127.0.0.1:8080/actuator/health/readiness`.
- Yuheng Engine data plane: `http://127.0.0.1:18081`; management health: `http://127.0.0.1:18083/actuator/health`.
- Tianshu Web: `http://127.0.0.1:5174`; Yuheng Web: `http://127.0.0.1:5173`.
- Backend group: two HTTP Provider instances with the same biz/app/env/ns/service key and distinct instance IDs/ports.

- [ ] **Step 1: Validate host infrastructure without printing credentials**

  Confirm PostgreSQL and Redis readiness using the existing authenticated local configuration. Resolve port ownership before starting any project process.

- [ ] **Step 2: Package executable applications**

  Package Tianshu Admin, Yuheng Admin/Engine, and the HTTP Provider test application from clean sources.

- [ ] **Step 3: Start services in dependency order**

  Start Tianshu Admin, Yuheng Admin, Yuheng Engine, two HTTP Provider instances, Tianshu Web, and retain the existing Yuheng Web process when its proxy/port are correct. Use readiness polling rather than fixed sleeps.

- [ ] **Step 4: Create/enable the matching Tianshu scope if absent**

  Through the local-dev Admin API, ensure `demo-biz`, `yuheng-test-http-provider`, `yuheng-test`, and `test` exist and are enabled before Provider registration. Do not delete or overwrite unrelated local data.

- [ ] **Step 5: Verify registry and routing boundaries**

  Assert Tianshu returns one app row with two online instances for the target scope, Yuheng Admin's Provider projection sees the scoped service, and a published test route forwards through Engine to an available backend. Capture exact HTTP status and non-secret response fields.

- [ ] **Step 6: Final repository and process verification**

  ```bash
  git diff --check
  git status --short --branch
  ```

  Recheck listeners, readiness endpoints, registry instance count, and backend response immediately before completion. Leave project processes running.

## Plan Self-Review

- Scope coverage: Tianshu key construction, RPC/HTTP registration, Yuheng rule/report propagation, Admin persistence, Engine discovery, runtime configuration, and host-local startup are covered.
- Placeholder scan: no implementation placeholder is used; runtime credentials remain intentionally sourced from existing local configuration and are never copied into the plan.
- Type consistency: all provider identity boundaries use the same ordered dimensions `bizCode, appCode, env, namespace` before protocol/service fields.
- Risk boundary: live verification proves only this host-local process topology; it does not prove multi-host, HA, Redis Cluster/Sentinel, Kafka projection, or production TLS behavior.
