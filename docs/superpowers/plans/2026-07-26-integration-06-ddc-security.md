# Integration 06 DDC Security Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 DDC 服务 OpenAPI 与人员管理 API 分离认证，补齐 HMAC scope、共享 Redis nonce 和 JWT capability，默认拒绝未认证管理请求。

**Architecture:** Spring Security 负责人员管理面 JWT；现有 HMAC Filter 继续保护 `/openapi/**`，但从配置化 credential 推导可信 scope。NonceStore 使用专用 `ddcAdminRedissonClient` 的 `SET NX PX`，HA 节点共享防重放状态。

**Tech Stack:** Spring Security Resource Server、Nimbus JWT、Servlet Filter、Redisson、MockMvc、JUnit 5。

## Global Constraints

- manifest、Actuator health/info 可匿名；其他 DDC API 默认拒绝。
- `/api/v1/ddc/openapi/**` 使用 HMAC，不要求人员 JWT。
- 人员 JWT claim/capability 语义与 Gateway Admin 一致，但 DDC 不依赖 Gateway 模块。
- operator 请求字段只作审计备注，可信主体来自 credential/JWT。
- 写请求 nonce store 不可用时 fail closed。

---

### Task 1: 定义 DDC 管理认证属性和 capability

**Files:**
- Modify: `.../dynamic-config-center-admin/pom.xml`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminProperties.java`
- Modify: `.../admin/src/main/resources/application.yml`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminCapability.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/config/DdcAdminSecurityPropertiesTest.java`

**Interfaces:**
- Produces JWT issuer/audience/local-dev and HMAC credential scope properties.
- Produces authorities `CAP_DDC_READ`, `CAP_DDC_WRITE`, `CAP_DDC_PUBLISH`, `CAP_DDC_CACHE`, `CAP_*`.

- [ ] **Step 1: Write fail-closed property tests**

Assert production mode without JWT issuer/audience or with HMAC enabled but missing secret fails startup. Assert
local-dev is explicit and default false.

- [ ] **Step 2: Run the property test**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcAdminSecurityPropertiesTest
```

- [ ] **Step 3: Add standard Boot security dependencies and properties**

Add `spring-boot-starter-security` and `spring-boot-starter-oauth2-resource-server`. Reuse versions from parent
dependency management. Model one or more HMAC credentials with accessKey, secret, clientType, appCode/env/
namespace patterns and allowedOperations.

- [ ] **Step 4: Re-run configuration tests**

Expected: PASS with secure defaults and explicit local test configuration.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: define ddc admin security contract"
```

### Task 2: 用 Redis 实现跨节点 NonceStore

**Files:**
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcNonceStore.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/RedisDdcNonceStore.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/InMemoryDdcNonceStore.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/config/DdcAdminRedisConfig.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcOpenApiHmacFilter.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/security/RedisDdcNonceStoreTest.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcOpenApiHmacFilterTest.java`

**Interfaces:**
- Produces `boolean markIfAbsent(String credentialId, String nonce, Duration ttl)`.
- Production consumes exact `ddcAdminRedissonClient`; local-dev may use bounded in-memory store.

- [ ] **Step 1: Write shared replay and outage tests**

```java
assertThat(nodeOne.markIfAbsent("sdk-a", "nonce-1", ttl)).isTrue();
assertThat(nodeTwo.markIfAbsent("sdk-a", "nonce-1", ttl)).isFalse();
```

Both stores point to the same fake/real Redis. Simulate Redis outage and assert signed write returns 503/closed,
not accepted.

- [ ] **Step 2: Run nonce/filter tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=RedisDdcNonceStoreTest,DdcOpenApiHmacFilterTest
```

- [ ] **Step 3: Implement SET NX PX with scoped digest key**

```java
String key = "ddc:security:nonce:{" + digest(credentialId) + "}:" + digest(nonce);
return redisson.getBucket(key).trySet("1", ttl);
```

Never store raw access key or nonce. Condition beans by exact name and keep in-memory store restricted to
explicit local-dev.

- [ ] **Step 4: Run security tests with two application contexts**

Expected: second context rejects the replay.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: share ddc hmac nonce state"
```

### Task 3: 为 HMAC Credential 强制 scope 和可信主体

**Files:**
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcHmacCredential.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcHmacCredentialRegistry.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcServicePrincipal.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcOpenApiHmacFilter.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcManagementOpenApiController.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcOpenApiController.java`
- Modify: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/controller/DdcRegistryOpenApiController.java`
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcHmacScopeTest.java`

**Interfaces:**
- Produces request attribute/security context containing credentialId/clientType/scopes/allowedOperations.
- Controllers use server principal as operator; request operator is retained only in audit detail.

- [ ] **Step 1: Write allowed/denied scope tests**

Cover SDK pull/ACK, Registry register/heartbeat, Management exact GET/upsert/publish and cross-app/namespace
attempts. Wrong clientType or operation returns 403 after signature verification.

- [ ] **Step 2: Run HMAC scope tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcHmacScopeTest,DdcOpenApiHmacFilterTest
```

- [ ] **Step 3: Resolve credential before signature and authorize canonical scope**

Select by accessKey, verify signature with that credential secret, parse app/env/namespace from query/body after
cached-body validation, authorize operation, then attach `DdcServicePrincipal`. Do not trust request `operator`.

- [ ] **Step 4: Run all OpenAPI controller/security tests**

Expected: valid clients remain compatible; cross-scope requests fail 403.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center/egon-cola-component-dynamic-config-center-admin
git commit -m "feat: enforce ddc hmac credential scopes"
```

### Task 4: 保护人员管理 API 的 JWT capability

**Files:**
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityConfiguration.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminJwtAuthenticationConverter.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminAuthenticationEntryPoint.java`
- Create: `.../admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcSecurityFilterRegistration.java`
- Modify: DDC Admin controllers to declare/check exact capabilities where route-level rules are insufficient.
- Test: `.../admin/src/test/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityIntegrationTest.java`

**Interfaces:**
- Produces SecurityFilterChain ordered around HMAC filter.
- Converts `sub`, `roles`, `capabilities` to trusted principal and `ROLE_`/`CAP_` authorities.

- [ ] **Step 1: Write MockMvc authentication matrix**

```text
GET manifest / actuator health            -> anonymous 200
POST /openapi/... valid HMAC              -> 2xx without JWT
GET /configs no token                     -> 401
GET /configs DDC_READ                     -> 200
POST /configs only DDC_READ               -> 403
POST /configs DDC_WRITE or *              -> 2xx
POST /publish-tasks without DDC_PUBLISH   -> 403
```

- [ ] **Step 2: Run the integration test**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center/\
egon-cola-component-dynamic-config-center-admin -am test \
  -Dtest=DdcAdminSecurityIntegrationTest
```

- [ ] **Step 3: Implement explicit request matchers and JWT conversion**

Reuse Gateway Admin claim semantics in copied DDC-owned code, not via module dependency. Disable the servlet
container's automatic registration of `DdcOpenApiHmacFilter`, add it once inside the Security filter chain,
permit HMAC routes through authorization after the HMAC filter has accepted them, and make all unmatched routes
authenticated/denied.

- [ ] **Step 4: Run full DDC Admin and reactor tests**

```bash
./mvnw -B -ntp \
  -pl egon-cola-components/egon-cola-component-dynamic-config-center \
  -am clean verify
```

Expected: PASS with explicit test JWT/HMAC configuration.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-components/egon-cola-component-dynamic-config-center
git commit -m "feat: secure ddc management api"
```
