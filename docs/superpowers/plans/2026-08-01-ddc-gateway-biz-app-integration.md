# DDC Gateway Biz-App Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. This task must be executed inline in the current workspace; the user explicitly prohibited subagents.

**Goal:** Restore a cleanly compiling and runnable DDC/Gateway/Provider topology after DDC service identity changed to `bizCode + appCode + env + namespace`.

**Architecture:** Keep DDC as the owner of registry identity and keep Gateway's existing Adapter boundary. Provider scope travels from Gateway Starter reporting through Admin release snapshots into Engine provider keys, while each runtime that registers its own service uses `DdcServiceKeyFactory` so it cannot omit the local DDC biz/app scope. RPC consumers may explicitly target a Gateway biz/app scope and otherwise fall back to their local DDC scope.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, JUnit 5, PostgreSQL, Redis, React 19, Vite.

## Global Constraints

- Execute inline on the current `main` checkout; do not create a subagent or worktree.
- Preserve the committed DDC `biz-ns-env-app` model and the existing Gateway UI work.
- Add no dependencies and perform no unrelated refactoring.
- Use one failing behavior test before each production behavior change.
- Use the existing Adapter and Factory patterns; do not add another abstraction layer.
- Do not modify any existing Flyway migration. No schema migration is required because provider biz/app scope already lives in the operation provider identity JSON.
- Use host-local Redis and PostgreSQL; do not substitute Testcontainers evidence for the requested live topology.
- Leave the requested DDC, Gateway, Web UI, and backend processes running after verification.

---

### Task 1: Restore DDC-scoped runtime registrations

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/config/DdcRegistryAutoConfig.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter/src/main/java/top/egon/cola/component/ddc/registry/DdcServiceKeyFactory.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/HttpProviderLeaseRuntime.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-provider-runtime/src/main/java/top/egon/cola/component/gateway/provider/GatewayHttpProviderAutoConfiguration.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/provider/RpcProviderLeaseManager.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/consumer/RpcConsumerGatewayManager.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcProperties.java`
- Modify: `egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-starter/src/main/java/top/egon/cola/component/rpc/config/EgonRpcAutoConfig.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntime.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Modify: focused tests beside each runtime.

**Interfaces:**
- `DdcRegistryAutoConfig` produces a singleton `DdcServiceKeyFactory` from `DdcProperties`.
- `DdcServiceKeyFactory` produces both local-scope keys and explicitly targeted Gateway keys while applying the same validation.
- HTTP and RPC Providers consume that factory and register their service under the local DDC `bizCode/appCode/env/namespace`.
- Gateway Engine consumes the same factory when it registers its own `INTERNAL_GATEWAY` slot.
- RPC Consumer properties produce optional `gatewayBizCode` and `gatewayAppCode`; blank values fall back to local DDC properties.

- [ ] **Step 1: Add failing registration-scope tests**

  Assert the real `DdcServiceRegistration.serviceKey()` observed by the fake registry has literal scope `retail-biz/orders-app/test/gateway-test`. Add a consumer test where the local app differs from `platform-biz/gateway-app` and assert the subscription uses the explicit Gateway target scope.

- [ ] **Step 2: Run RED tests**

  ```bash
  ./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
    -pl egon-cola-platform-gateway-provider-runtime -am \
    -Dtest=HttpProviderLeaseRuntimeTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml \
    -pl egon-cola-component-rpc-starter -am \
    -Dtest=RpcProviderLifecycleTest,RpcConsumerGatewayManagerTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: scope assertions or compilation fail because current runtimes still call the obsolete seven-field DDC constructors.

- [ ] **Step 3: Implement the minimal factory/target-scope wiring**

  Register `DdcServiceKeyFactory` once and replace local-scope direct constructors with `factory.fromScope(...)`. For RPC Consumer discovery only, construct the key from explicit Gateway biz/app when configured, otherwise from local `DdcProperties`; keep env/namespace and service/group/version behavior unchanged.

- [ ] **Step 4: Run GREEN focused tests and clean compilation**

  Run the commands from Step 2, then run clean compile for the two changed reactors.

- [ ] **Step 5: Commit Task 1**

  ```bash
  git commit -m "fix: preserve DDC scope in provider registrations"
  ```

### Task 2: Carry provider biz/app through Gateway releases and discovery

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/GatewayReportingProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/reporting/GatewayInterfaceDefinitionReport.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/rule/GatewayProviderServiceRef.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/provider/ProviderQuery.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/provider/ProviderServiceKey.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/GatewayHttpOperationMapper.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-starter/src/main/java/top/egon/cola/component/gateway/starter/discovery/RpcGatewayDefinitionContributor.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/release/GatewayReleaseService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/catalog/GatewayCatalogStore.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/infrastructure/persistence/JdbcGatewayCatalogStore.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/catalog/GatewayCatalogService.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/rule/GatewayRuleCompiler.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/EngineGatewayRuleCompiler.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/discovery/DdcProviderServiceRegistryAdapter.java`
- Modify: focused contract, starter, admin, core, and engine tests.

**Interfaces:**
- `GatewayInterfaceDefinitionReport.ProviderService`, `GatewayProviderServiceRef`, `ProviderServiceKey`, and `ProviderQuery` all carry required `bizCode` and `appCode`.
- Starter reporting takes `bizCode` from `egon.cola.component.gateway.reporting.biz-code` and `appCode` from the existing `application-code`.
- Manual operations derive `bizCode` from their containing Gateway business-domain node and `appCode` from the containing application.
- `DdcProviderServiceRegistryAdapter` maps all nine DDC query/key fields without guessing defaults.

- [ ] **Step 1: Add failing scope-isolation tests**

  Add literal tests proving two otherwise identical Providers from different biz/apps are different keys; a DDC catalog query contains the release's biz/app; and a Starter report retains configured biz/app into the Provider Service identity.

- [ ] **Step 2: Run RED tests**

  ```bash
  ./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml \
    -pl egon-cola-platform-gateway-starter,egon-cola-platform-gateway-admin,egon-cola-platform-gateway-engine \
    -am -Dtest=ProviderModelTest,GatewayDefinitionReportFactoryTest,GatewayCatalogServiceTest,ProviderDirectoryTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  ```

  Expected: new scope expectations fail and the clean Engine compile reports obsolete DDC constructors.

- [ ] **Step 3: Implement end-to-end scope propagation**

  Add required fields at each identity boundary, update canonical ordering and equality through record components, update manual and reported provider identity creation, and map both directions in the existing DDC Adapter. Do not derive appCode from serviceName.

- [ ] **Step 4: Run GREEN tests and Gateway clean reactor test**

  Run Step 2, then:

  ```bash
  ./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml clean test
  ```

- [ ] **Step 5: Commit Task 2**

  ```bash
  git commit -m "fix: propagate DDC application scope through gateway"
  ```

### Task 3: Align runnable configuration and live fixtures

**Files:**
- Modify: Gateway Engine and test application YAML files that enable DDC registration/reporting.
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveTopologyIT.java` only where live child JVM properties require biz/app scope.
- Modify: RPC README examples only if the new explicit Gateway target properties are otherwise undocumented.

**Interfaces:**
- Local test scope is `demo-biz / gateway-test-http-provider / test / gateway-test` for the HTTP backend.
- Gateway Engine local identity is `platform-biz / egon-cola-gateway-engine / local / default`.
- DDC Admin runs on 18080; Gateway Admin runs on 8080; Engine data plane remains 18083; web UIs use 5173/5174.

- [ ] **Step 1: Add scope properties to enabled runtime fixtures**

  Add explicit non-secret biz/app values wherever DDC registry or Gateway reporting is enabled. Keep disabled fixtures unchanged unless their tests validate property binding.

- [ ] **Step 2: Run component and Web UI baselines**

  ```bash
  ./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-dynamic-config-center/pom.xml clean test
  ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-rpc/pom.xml clean test
  ./mvnw -B -ntp -f egon-cola-platforms/egon-cola-platform-gateway/pom.xml clean test
  npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run build
  npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run build
  ```

- [ ] **Step 3: Commit Task 3 if tracked configuration changed**

  ```bash
  git commit -m "test: align gateway live scope configuration"
  ```

### Task 4: Start and verify the host-local topology

**Files:**
- Runtime-only: `target/local-dev-run/` for logs and process state; these files remain ignored and must not expose credentials.

**Interfaces:**
- DDC Admin readiness: `http://127.0.0.1:18080/actuator/health/readiness`.
- Gateway Admin readiness: `http://127.0.0.1:8080/actuator/health/readiness`.
- Gateway Engine readiness: `http://127.0.0.1:18083/actuator/health/readiness`.
- DDC Web: `http://127.0.0.1:5174`; Gateway Web: `http://127.0.0.1:5173`.
- Backend group: two HTTP Provider instances with the same biz/app/env/ns/service key and distinct instance IDs/ports.

- [ ] **Step 1: Validate host infrastructure without printing credentials**

  Confirm PostgreSQL and Redis readiness using the existing authenticated local configuration. Resolve port ownership before starting any project process.

- [ ] **Step 2: Package executable applications**

  Package DDC Admin, Gateway Admin/Engine, and the HTTP Provider test application from clean sources.

- [ ] **Step 3: Start services in dependency order**

  Start DDC Admin, Gateway Admin, Gateway Engine, two HTTP Provider instances, DDC Web, and retain the existing Gateway Web process when its proxy/port are correct. Use readiness polling rather than fixed sleeps.

- [ ] **Step 4: Create/enable the matching DDC scope if absent**

  Through the local-dev Admin API, ensure `demo-biz`, `gateway-test-http-provider`, `gateway-test`, and `test` exist and are enabled before Provider registration. Do not delete or overwrite unrelated local data.

- [ ] **Step 5: Verify registry and routing boundaries**

  Assert DDC returns one app row with two online instances for the target scope, Gateway Admin's Provider projection sees the scoped service, and a published test route forwards through Engine to an available backend. Capture exact HTTP status and non-secret response fields.

- [ ] **Step 6: Final repository and process verification**

  ```bash
  git diff --check
  git status --short --branch
  ```

  Recheck listeners, readiness endpoints, registry instance count, and backend response immediately before completion. Leave project processes running.

## Plan Self-Review

- Scope coverage: DDC key construction, RPC/HTTP registration, Gateway rule/report propagation, Admin persistence, Engine discovery, runtime configuration, and host-local startup are covered.
- Placeholder scan: no implementation placeholder is used; runtime credentials remain intentionally sourced from existing local configuration and are never copied into the plan.
- Type consistency: all provider identity boundaries use the same ordered dimensions `bizCode, appCode, env, namespace` before protocol/service fields.
- Risk boundary: live verification proves only this host-local process topology; it does not prove multi-host, HA, Redis Cluster/Sentinel, Kafka projection, or production TLS behavior.
