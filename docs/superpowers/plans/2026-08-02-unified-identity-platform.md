# Egon-COLA Unified Identity Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增 `egon-cola-platform-idp`，把人员认证、统一 SSO 和 Token 发行从 RBAC3 迁移到 IdP，并打通 RBAC3 授权、Gateway 基础身份校验、DDC 配置/注册和模拟后端端到端链路。

**Architecture:** IdP 是全局人员身份和 OAuth 2.1 Token 的唯一权威；RBAC3 是租户映射和授权事实的唯一权威。Gateway 只校验 IdP JWT、全局用户状态和 Token Version，并透传原始 Bearer；下游使用 IdP Starter 二次验签，再用 RBAC3 Starter 从授权中心拉取并缓存 `systemCode + tid + sid` 授权快照。

**Tech Stack:** Java 21、Spring Boot 3.5.16、Spring Security OAuth2/Jose、PostgreSQL、Flyway 11.15、Redisson 3.26、Kafka/Transactional Outbox、React 19、TypeScript 6、Vite 8、Vitest 4、Playwright。

## Global Constraints

- 新聚合模块名称固定为 `egon-cola-platform-idp`，包含 contract、core、starter、gateway-adapter、admin、admin-web。
- IdP 不保存租户、租户成员、角色、权限、数据范围和字段策略。
- Access Token 只包含稳定身份声明；不得包含角色、权限或 RBAC3 版本。
- Gateway 严禁调用 RBAC3 或执行权限判断，只做 IdP 身份基础校验。
- 下游必须二次校验 JWT，并在自己的 Redis 中缓存 RBAC3 授权快照。
- Access Token 单租户化，`sub` 是全局身份，`tid` 是当前租户，`sid` 是稳定 SSO/Refresh Family 会话。
- RBAC3 授权上下文键固定为 `(tid, sid)`；角色激活不重签 IdP Token。
- Refresh Token 只通过 HttpOnly Cookie 传输，必须 Rotation，并检测重放。
- 所有既有 Flyway 文件不可修改；IdP 新增一个 V1，RBAC3 只新增一个 V3。
- 测试放在所属模块 `src/test`；不新增独立 IdP 测试模块。
- 不引入生产双栈迁移；当前 RBAC3 人员 Token、Refresh 和 Session 直接失效。
- 按 TDD 执行：每个行为先写测试并确认因缺少行为而失败，再写最小实现。
- 每个 Task 完成定向验证后独立提交；不启动应用，直到最终端到端 Task。

---

## File Structure

### New IdP files

```text
egon-cola-platforms/egon-cola-platform-idp/
├── pom.xml
├── README.md
├── egon-cola-platform-idp-contract/
│   ├── pom.xml
│   └── src/main/java/top/egon/cola/platform/idp/contract/
│       ├── IdpClaimNames.java
│       ├── IdpErrorCode.java
│       ├── IdentityPrincipal.java
│       ├── IdentityUserState.java
│       └── OAuthClientView.java
├── egon-cola-platform-idp-core/
│   ├── pom.xml
│   └── src/main/java/top/egon/cola/platform/idp/core/
│       ├── identity/
│       ├── oauth/
│       ├── token/
│       ├── audit/
│       └── port/
├── egon-cola-platform-idp-starter/
│   ├── pom.xml
│   └── src/main/java/top/egon/cola/platform/idp/starter/
│       ├── autoconfigure/
│       ├── security/
│       └── state/
├── egon-cola-platform-idp-gateway-adapter/
│   ├── pom.xml
│   └── src/main/java/top/egon/cola/platform/idp/gateway/
│       ├── autoconfigure/
│       └── security/
├── egon-cola-platform-idp-admin/
│   ├── pom.xml
│   └── src/
│       ├── main/java/top/egon/cola/platform/idp/admin/
│       ├── main/resources/application.yml
│       ├── main/resources/application-local.yml
│       ├── main/resources/db/migration/V1__create_idp_schema.sql
│       └── main/resources/redis/rotate-refresh-token.lua
└── egon-cola-platform-idp-admin-web/
    ├── package.json
    └── src/
```

### Existing files changed by responsibility

- Reactor/dependency management: `egon-cola-platforms/pom.xml`.
- Gateway runtime adapter switch: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml` and engine security tests/configuration.
- RBAC3 migration/identity/session/snapshot: `egon-cola-platform-rbac3-admin` and `egon-cola-platform-rbac3-contract`.
- Downstream cache and annotations: `egon-cola-platform-rbac3-starter`.
- Existing Admin Backend security: DDC Admin, Gateway Admin, RBAC3 Admin security/configuration files.
- Existing Admin Web SSO: the `auth/` or `features/auth/` directories and API clients in DDC/Gateway/RBAC3 Admin Web.
- Live topology: existing Gateway test providers/suite plus a new IdP-aware mock backend fixture.

---

### Task 1: IdP Reactor and Stable Contracts

**Files:**
- Modify: `egon-cola-platforms/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-idp/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-idp/README.md`
- Create: all six IdP child `pom.xml` files
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/main/java/top/egon/cola/platform/idp/contract/IdpClaimNames.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/main/java/top/egon/cola/platform/idp/contract/IdpErrorCode.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/main/java/top/egon/cola/platform/idp/contract/IdentityPrincipal.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/main/java/top/egon/cola/platform/idp/contract/IdentityUserState.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/main/java/top/egon/cola/platform/idp/contract/OAuthClientView.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract/src/test/java/top/egon/cola/platform/idp/contract/IdentityPrincipalTest.java`

**Interfaces:**
- Produces: `IdentityPrincipal(String subject, String tenantId, String sessionId, String clientId, String tokenId, long tokenVersion, Set<String> audience, Instant issuedAt, Instant expiresAt)`.
- Produces: claim names `sub/tid/sid/client_id/jti/token_version/aud` shared by IdP, Gateway and downstream.

- [ ] **Step 1: Write the failing contract test**

```java
@Test
void rejectsBlankStableIdentityClaims() {
    assertThrows(IllegalArgumentException.class, () -> new IdentityPrincipal(
            " ", "tenant-a", "sid-a", "web", "jti-a", 0,
            Set.of("mock-api"), Instant.EPOCH, Instant.EPOCH.plusSeconds(900)));
}

@Test
void accessIdentityContainsNoAuthorizationFacts() {
    Set<String> components = Arrays.stream(IdentityPrincipal.class.getRecordComponents())
            .map(RecordComponent::getName).collect(Collectors.toSet());
    assertFalse(components.contains("roles"));
    assertFalse(components.contains("permissions"));
    assertFalse(components.contains("authVersion"));
}
```

- [ ] **Step 2: Run the test and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-contract -am -DskipITs -Dtest=IdentityPrincipalTest test`

Expected: reactor/module or `IdentityPrincipal` is missing.

- [ ] **Step 3: Add reactor POMs and minimal immutable contracts**

```java
public record IdentityPrincipal(
        String subject, String tenantId, String sessionId, String clientId,
        String tokenId, long tokenVersion, Set<String> audience,
        Instant issuedAt, Instant expiresAt) {
    public IdentityPrincipal {
        subject = requireText(subject, "subject");
        tenantId = requireText(tenantId, "tenantId");
        sessionId = requireText(sessionId, "sessionId");
        clientId = requireText(clientId, "clientId");
        tokenId = requireText(tokenId, "tokenId");
        audience = Set.copyOf(audience);
        if (tokenVersion < 0 || audience.isEmpty() || !expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("invalid identity principal");
        }
    }
}
```

Add IdP artifacts to `dependencyManagement` and add the IdP aggregator after DDC/Gateway/RBAC3 in `egon-cola-platforms/pom.xml`.

- [ ] **Step 4: Run contract tests and reactor validation**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp -am -DskipTests install`

Expected: `BUILD SUCCESS`, all six modules occur exactly once in the reactor.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/pom.xml egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): add platform reactor and contracts"
```

### Task 2: Identity Core, Password Policy, and Token Version

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/identity/IdentityUser.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/identity/IdentityUserStatus.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/identity/PasswordCredential.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/identity/UsernameNormalizer.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/identity/IdentityFacade.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/IdentityUserStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/PasswordHashPort.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/IdentityUserStatePort.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/audit/IdentitySecurityEventPort.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/identity/IdentityFacadeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/identity/UsernameNormalizerTest.java`

**Interfaces:**
- Produces: `IdentityFacade.authenticate(String username, char[] password, String sourceBucket, Instant now)` returning `AuthenticatedIdentity`.
- Produces: `disable`, `changePassword`, `revokeAll` operations that atomically increment `tokenVersion` through `IdentityUserStore`.

- [ ] **Step 1: Write failing tests for normalization, lockout, non-enumerating failure, and revocation**

```java
@Test
void passwordChangeIncrementsTokenVersionAndPublishesRevocation() {
    IdentityUser user = activeUser(4);
    store.save(user);
    facade.changePassword(user.id(), "old-pass".toCharArray(), "new-pass".toCharArray(), NOW);
    assertEquals(5, store.get(user.id()).tokenVersion());
    assertEquals("IDENTITY_TOKEN_REVOKED", events.single().eventType());
}

@Test
void unknownUserAndWrongPasswordHaveSamePublicFailure() {
    assertEquals("INVALID_CREDENTIALS", publicCode(() -> facade.authenticate(
            "missing", chars("x"), "local", NOW)));
    assertEquals("INVALID_CREDENTIALS", publicCode(() -> facade.authenticate(
            "alice", chars("wrong"), "local", NOW)));
}
```

- [ ] **Step 2: Run core tests and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core -am -DskipITs -Dtest=IdentityFacadeTest,UsernameNormalizerTest test`

Expected: core types are missing.

- [ ] **Step 3: Implement the domain and Application Facade**

Use `IdentityFacade` to orchestrate stores and ports. Keep the password hash algorithm behind `PasswordHashPort`; always clear supplied `char[]` in `finally`. Normalize username with Unicode NFKC, trim and lowercase using `Locale.ROOT`. Update failed counters and lock time through optimistic versioned writes.

```java
public interface IdentityUserStore {
    Optional<IdentityUser> findByNormalizedUsername(String normalized);
    Optional<IdentityUser> findById(String identitySub);
    IdentityUser save(IdentityUser user, long expectedVersion);
}

public interface IdentityUserStatePort {
    void publish(IdentityUserState state);
    void revokeFamilies(String identitySub, long tokenVersion, String reason);
}
```

- [ ] **Step 4: Run all core tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core -am -DskipITs test`

Expected: all tests pass with no logged credentials.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core
git commit -m "feat(idp): add identity and credential domain"
```

### Task 3: IdP PostgreSQL Schema, Persistence, and Bootstrap CLI

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/db/migration/V1__create_idp_schema.sql`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/domain/IdentityUserEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/identity/domain/IdentityCredentialEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/IdentityClientEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/IdentityClientRedirectUriEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/IdentityClientAudienceEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/token/domain/IdentitySigningKeyEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/audit/domain/IdentityAuditLogEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/outbox/domain/IdentityOutboxEventEntity.java`
- Create: JPA repositories and adapters under matching `infrastructure/` packages
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/bootstrap/IdpBootstrapRunner.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/migration/IdpMigrationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/bootstrap/IdpBootstrapRunnerTest.java`

**Interfaces:**
- Consumes: core store/port interfaces from Task 2.
- Produces: PostgreSQL adapters and `--idp-bootstrap-admin=alice` CLI shape reading password from `IDP_BOOTSTRAP_PASSWORD` without logging it.

- [ ] **Step 1: Write failing migration and bootstrap tests**

```java
@Test
void migrationCreatesIdentityTablesButNoTenantAuthorizationTables() {
    assertThat(tableNames()).contains("identity_user", "identity_user_credential",
            "identity_client", "identity_signing_key", "identity_audit_log",
            "identity_outbox_event");
    assertThat(tableNames()).doesNotContain("tenant", "tenant_membership", "role", "permission");
}
```

- [ ] **Step 2: Run the tests and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am -DskipITs=false -Dtest=IdpMigrationIT,IdpBootstrapRunnerTest test`

Expected: V1 and persistence adapters are missing.

- [ ] **Step 3: Add exactly one IdP V1 migration and persistence adapters**

V1 creates `identity_user`, `identity_user_credential`, `identity_client`, `identity_client_redirect_uri`, `identity_client_audience`, `identity_signing_key`, `identity_audit_log`, and `identity_outbox_event`, with unique normalized username, client/redirect/audience constraints, optimistic versions, UTC timestamps and Outbox indexes. It creates no session or authorization tables.

Use the repository's ID generator starter; do not derive machine ID from hostname. Configure `DelegatingPasswordEncoder` with Argon2id as default and BCrypt verification compatibility.

- [ ] **Step 4: Run migration and persistence tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am -DskipITs=false test`

Expected: V1 applies from empty PostgreSQL and bootstrap creates one ACTIVE user with no default password in source/config.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin
git commit -m "feat(idp): add persistence and bootstrap admin"
```

### Task 4: OAuth Client, PKCE Authorization Code, and Tenant Resolution

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/oauth/AuthorizationRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/oauth/AuthorizationCode.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/oauth/OAuthClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/oauth/AuthorizationFacade.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/AuthorizationCodeStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/OAuthClientStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/TenantMembershipPort.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/infrastructure/RedisAuthorizationCodeStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/rbac3/HttpTenantMembershipAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/OAuthAuthorizationController.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/oauth/AuthorizationFacadeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/OAuthAuthorizationFlowIT.java`

**Interfaces:**
- Produces: `TenantMembershipPort.resolve(identitySub, tenantId, clientId)`.
- Produces: one-time SHA-256-digested authorization code with 60-second TTL.

- [ ] **Step 1: Write failing tests for exact redirect, PKCE S256, one-time code, and active tenant mapping**

```java
@Test
void refusesAuthorizationWhenRbac3DoesNotResolveMembership() {
    membership.reject("alice-sub", "tenant-b", "gateway-admin-web");
    assertThrows(OAuthException.class, () -> facade.authorize(requestFor("tenant-b"), sso("alice-sub")));
}

@Test
void consumesMatchingS256CodeOnlyOnce() {
    String code = facade.authorize(requestWithChallenge(S256_OF_VERIFIER), sso("alice-sub")).code();
    assertEquals("alice-sub", facade.consume(code, VERIFIER, REDIRECT_URI).identitySub());
    assertThrows(OAuthException.class, () -> facade.consume(code, VERIFIER, REDIRECT_URI));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am -DskipITs -Dtest=AuthorizationFacadeTest,OAuthAuthorizationFlowIT test`

Expected: Authorization Facade and Redis store are missing.

- [ ] **Step 3: Implement Client validation and one-time code flow**

```java
public interface TenantMembershipPort {
    TenantMembership resolve(String identitySub, String tenantId, String clientId);
    List<TenantMembership> list(String identitySub, String clientId);
}

public interface AuthorizationCodeStore {
    void put(String codeDigest, AuthorizationCode code, Duration ttl);
    AuthorizationCode consume(String codeDigest);
}
```

Use Redis Lua or `RBucket.getAndDelete()` semantics for one-time consumption. Require `S256`, exact Redirect URI, registered Audience, `state`, and `nonce` for browser Clients.

- [ ] **Step 4: Run OAuth authorization tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp -am -DskipITs=false -Dtest=AuthorizationFacadeTest,OAuthAuthorizationFlowIT test`

Expected: wrong verifier/redirect/tenant all fail; one valid code succeeds exactly once.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): add pkce authorization flow"
```

### Task 5: RS256 Access Token, Refresh Rotation, Replay, and Logout

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/TokenFacade.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/AccessTokenClaims.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/RefreshTokenClaims.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token/RefreshFamily.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/TokenSigner.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/RefreshTokenStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/token/infrastructure/Rs256TokenService.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/token/infrastructure/RedisRefreshTokenStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/redis/rotate-refresh-token.lua`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/OAuthTokenController.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/OAuthMetadataController.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/token/TokenFacadeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/token/RefreshRotationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/token/AccessTokenClaimsIT.java`

**Interfaces:**
- Produces: RS256 Access JWT with exact Claims from the spec.
- Produces: Refresh JWT in `HttpOnly; SameSite=Lax` Cookie; Redis stores only digest state.

- [ ] **Step 1: Write failing Access Claims and Refresh replay tests**

```java
@Test
void accessTokenCarriesIdentityButNoAuthorizationFacts() {
    Jwt jwt = decode(issueFor("alice", "tenant-a", "sid-1"));
    assertEquals("alice", jwt.getSubject());
    assertEquals("tenant-a", jwt.getClaimAsString("tid"));
    assertEquals("sid-1", jwt.getClaimAsString("sid"));
    assertNull(jwt.getClaim("roles"));
    assertNull(jwt.getClaim("permissions"));
    assertNull(jwt.getClaim("authVersion"));
}

@Test
void replayOfConsumedRefreshRevokesWholeFamilyAndBumpsVersion() {
    TokenPair first = issue();
    TokenPair second = facade.refresh(first.refreshToken(), NOW.plusSeconds(1));
    assertThrows(RefreshReplayException.class,
            () -> facade.refresh(first.refreshToken(), NOW.plusSeconds(2)));
    assertFalse(store.family(second.familyId()).active());
    assertEquals(1, users.get("alice").tokenVersion());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp -am -DskipITs -Dtest=TokenFacadeTest,RefreshRotationIT,AccessTokenClaimsIT test`

Expected: Token/Refresh implementations are missing.

- [ ] **Step 3: Implement signing, JWK, atomic rotation, revoke, and logout**

The Lua operation receives family ID, current digest, successor digest, expected subject/version and expirations. It returns `ROTATED`, `REPLAY`, `REVOKED`, or `MISSING`. `REPLAY` calls the core revocation use case in the same application flow: persist incremented Token Version, revoke the family/index, update user state and append Outbox/audit.

Expose `/.well-known/oauth-authorization-server`, `/oauth2/jwks`, `/oauth2/token`, `/oauth2/revoke`, and `/oauth2/logout`. Refresh Cookie is host-only, HttpOnly, SameSite=Lax, path `/oauth2`; local profile alone may set Secure=false.

- [ ] **Step 4: Run Token tests including Redis integration**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp -am -DskipITs=false -Dtest=TokenFacadeTest,RefreshRotationIT,AccessTokenClaimsIT test`

Expected: rotation is single-winner, replay revokes family, JWK verifies Access JWT, no authorization Claims exist.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): issue and revoke oauth tokens"
```

### Task 6: IdP Admin APIs, Audit/Outbox, DDC, and Provider Lifecycle

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/IdentityUserController.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/OAuthClientController.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/SigningKeyController.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/IdentityAuditController.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/outbox/IdentityOutboxPublisher.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/ddc/IdpRuntimePolicy.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/ddc/AtomicIdpRuntimePolicy.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/ddc/IdpDdcPolicyApplier.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/integration/runtime/IdpHttpProviderPublicationGate.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/IdpAdminApplication.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application-local.yml`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/interfaces/http/IdpAdminSecurityIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/integration/ddc/IdpDdcPolicyApplierTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/integration/runtime/IdpHttpProviderPublicationGateTest.java`

**Interfaces:**
- Produces: `/api/v1/identity/users|clients|signing-keys|audits|me`.
- Produces: DDC CONFIG_CLIENT readiness and HTTP_PROVIDER publication only after OAuth runtime is ready.

- [ ] **Step 1: Write failing security, dynamic policy, and publication-gate tests**

```java
@Test
void providerIsNotPublishedBeforeDdcAndOauthAreReady() {
    assertFalse(gate.mayPublish(status(false, true, true)));
    assertFalse(gate.mayPublish(status(true, false, true)));
    assertTrue(gate.mayPublish(status(true, true, true)));
}

@Test
void invalidAccessTtlKeepsLastKnownGoodSnapshot() {
    PolicySnapshot before = policy.snapshot();
    assertEquals(FAILED, applier.apply("idp.token.access-ttl", "1s", 2).status());
    assertEquals(before, policy.snapshot());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am -DskipITs -Dtest=IdpAdminSecurityIT,IdpDdcPolicyApplierTest,IdpHttpProviderPublicationGateTest test`

Expected: Admin endpoints and runtime adapters are missing.

- [ ] **Step 3: Implement admin Facades/Controllers and platform adapters**

Reuse the repository's Gateway annotations, DDC `@DdcValue` declaration plus typed custom Applier, CONFIG_CLIENT coordinator and HTTP provider runtime. Do not place secrets in DDC declarations. Every mutating Admin API checks RBAC3 permission through the downstream Starter once Task 11 is available; until then, wire an explicit interface port so the dependency direction stays stable.

- [ ] **Step 4: Run IdP Admin tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin -am -DskipITs=false test`

Expected: Admin unauthorized calls return 401/403, invalid DDC updates preserve LKG, provider gate requires all readiness facts.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): add admin and platform lifecycle"
```

### Task 7: Reusable IdP Starter

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterProperties.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfiguration.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifier.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpBearerAuthenticationFilter.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/security/IdpAuthenticationToken.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/state/IdentityUserStateReader.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/java/top/egon/cola/platform/idp/starter/state/RedisIdentityUserStateReader.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/IdpJwtVerifierTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/security/IdpBearerAuthenticationFilterTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter/src/test/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfigurationTest.java`

**Interfaces:**
- Produces: authenticated Spring Security principal backed only by `IdentityPrincipal`.
- Produces: exact Issuer/Audience/Client/Token Version checks and one retry for unknown JWK Kid.

- [ ] **Step 1: Write failing claim/status/filter tests**

```java
@Test
void rejectsTokenWhenRedisVersionDiffers() {
    state.put(activeState("alice", 7));
    assertThrows(BadCredentialsException.class,
            () -> verifier.verify(jwt("alice", 6, "tenant-a", "sid-a")));
}

@Test
void rejectsMissingTenantAndSessionClaims() {
    assertThrows(BadCredentialsException.class,
            () -> verifier.verify(jwtWithout("tid", "sid")));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter -am -DskipITs -Dtest=IdpJwtVerifierTest,IdpBearerAuthenticationFilterTest,IdpStarterAutoConfigurationTest test`

Expected: Starter classes are missing.

- [ ] **Step 3: Implement fail-closed Starter**

Configure `NimbusJwtDecoder` by JWK Set URI, add Issuer/Audience validators, then validate required Claims and Redis user state. Register one filter at order `-102`, before RBAC3 authorization. The Starter never loads roles or permissions and never trusts forwarded headers as authentication.

- [ ] **Step 4: Run Starter tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter -am -DskipITs test`

Expected: signature, issuer, audience, required Claims, status and Token Version matrices pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter
git commit -m "feat(idp): add resource server starter"
```

### Task 8: Gateway Identity Adapter and Engine Cutover

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpBearerCredentialExtractor.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpGatewayJwtVerifier.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpIdentityAuthenticationProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpTrustedIdentityMapper.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/security/IdpReservedHeaderSanitizer.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/main/java/top/egon/cola/platform/idp/gateway/autoconfigure/IdpGatewayAdapterAutoConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml`
- Modify: engine `application.yml` security provider IDs
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/test/java/top/egon/cola/platform/idp/gateway/security/IdpGatewaySecurityProviderTest.java`
- Modify/Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/security/GatewayOriginalBearerForwardingTest.java`
- Create/Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/security/GatewayIdentityOnlySecurityTest.java`

**Interfaces:**
- Consumes: existing Gateway Credential Extractor, Authentication Provider and Trusted Identity Mapper SPIs.
- Produces: provider IDs `idp-bearer`, `idp-jwt`, `idp-identity`; no Authorization Provider.

- [ ] **Step 1: Write failing Gateway tests**

```java
@Test
void authenticatesIdentityWithoutCallingAuthorizationProvider() {
    GatewaySecurityResult result = chain.secure(exchange(BEARER), context(), identityOnlyPolicy());
    assertTrue(result.allowed());
    assertEquals(0, authorizationCalls.get());
    assertEquals(BEARER, result.forwardHeaders().get("Authorization"));
}

@Test
void removesSpoofedTrustedHeadersBeforeMappingVerifiedClaims() {
    exchange.headers().set("X-Egon-Identity-Sub", "mallory");
    assertEquals("alice", secure(exchange).forwardHeaders().get("X-Egon-Identity-Sub"));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dtest=IdpGatewaySecurityProviderTest,GatewayIdentityOnlySecurityTest,GatewayOriginalBearerForwardingTest test`

Expected: IdP Gateway Adapter is missing and engine still depends on RBAC3 Adapter.

- [ ] **Step 3: Implement Adapter and replace runtime dependency**

Implement identity-only Gateway SPI composition. Change engine runtime dependency from `egon-cola-platform-rbac3-gateway-adapter` to `egon-cola-platform-idp-gateway-adapter`. Security policies use `ORIGINAL_BEARER`; omit authorization provider ID. Preserve existing generic Chain behavior for other policies.

- [ ] **Step 4: Prove Gateway has no RBAC3 runtime dependency**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am dependency:tree -Dincludes=top.egon:egon-cola-platform-rbac3-gateway-adapter`

Expected: no dependency line for the engine.

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs test`

Expected: Gateway Adapter and engine tests pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine
git commit -m "feat(gateway): validate idp identity only"
```

### Task 9: RBAC3 Global Identity Mapping and Direct Cutover Migration

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V3__adopt_idp_identity.sql`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/domain/ExternalIdentityEntity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/domain/UserEntity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/infrastructure/IdentityRepositories.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacade.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/AuthController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/auth/application/AuthenticationFacade.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/migration/Rbac3IdpMigrationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/identity/application/IdentityMappingFacadeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3LegacyAuthenticationRemovedIT.java`

**Interfaces:**
- Produces: unique `(tenantId, identitySub) -> rbac3UserId` ACTIVE mapping.
- Produces: internal tenant list/resolve endpoints.
- Removes: personnel login/refresh/token endpoints from RBAC3.

- [ ] **Step 1: Write failing migration, mapping, and removed-endpoint tests**

```java
@Test
void sameGlobalIdentityMayMapToTwoTenantsButOnlyOncePerTenant() {
    facade.bind("tenant-a", "alice-sub", "user-a");
    facade.bind("tenant-b", "alice-sub", "user-b");
    assertThrows(DuplicateIdentityMappingException.class,
            () -> facade.bind("tenant-a", "alice-sub", "other-user"));
}

@Test
void legacyPersonnelTokenEndpointsAreGone() throws Exception {
    mvc.perform(post("/api/v1/auth/login")).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isNotFound());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin -am -DskipITs=false -Dtest=Rbac3IdpMigrationIT,IdentityMappingFacadeTest,Rbac3LegacyAuthenticationRemovedIT test`

Expected: V3 and new mapping API are missing; legacy endpoints still exist.

- [ ] **Step 3: Add exactly one V3 and remove personnel token issuance**

V3 adds/normalizes `identity_sub`, unique/index constraints and authorization-context fields needed by `(tenant_id, session_id)`. It marks/deletes all existing session and refresh records according to FK safety, and does not edit V1/V2. Remove Controller routes and production bean wiring for `AuthenticationFacade`, `JwtTokenService` and `RefreshTokenService`; retain source only when still needed by migration tests, otherwise delete it with its obsolete tests.

- [ ] **Step 4: Verify migration history and RBAC3 tests**

Run: `git diff 0c5966ad -- egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V1__create_rbac3_schema.sql egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V2__add_session_strong_authentication_time.sql`

Expected: no output.

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3 -am -DskipITs=false test`

Expected: RBAC3 tests pass with IdP identity mapping and no personnel token endpoints.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-rbac3
git commit -m "feat(rbac3): adopt global idp identities"
```

### Task 10: RBAC3 Authorization Context and System Snapshot APIs

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/domain/SessionEntity.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/session/application/SessionFacade.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SessionSnapshotProjector.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/RoleActivationController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/SessionAuthorizationSnapshot.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract/src/main/java/top/egon/cola/platform/rbac3/contract/authorization/SystemAuthorizationSnapshot.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotService.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/session/application/AuthorizationContextFacadeTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/snapshot/application/SystemAuthorizationSnapshotServiceTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/IdpRoleActivationSecurityIT.java`

**Interfaces:**
- Produces: `snapshot(tid, sid, systemCode, identitySub)` with active roles, permissions, data/field policy and three versions.
- Produces: role activation bound to JWT `sub/tid/sid` without Token reissue.

- [ ] **Step 1: Write failing context/snapshot tests**

```java
@Test
void roleActivationChangesContextVersionWithoutChangingIdentitySession() {
    AuthorizationContext before = contexts.open("tenant-a", "sid-1", "alice-sub");
    AuthorizationContext after = facade.activate(before.key(), Set.of("role-admin"), actor("alice-sub"));
    assertEquals(before.sessionId(), after.sessionId());
    assertEquals(before.contextVersion() + 1, after.contextVersion());
}

@Test
void snapshotContainsOnlyTargetSystemPermissions() {
    Rbac3SystemAuthorizationSnapshot snapshot = service.snapshot(
            "tenant-a", "sid-1", "gateway-admin", "alice-sub");
    assertThat(snapshot.permissions()).contains("gateway:release:read");
    assertThat(snapshot.permissions()).noneMatch(p -> p.startsWith("ddc:"));
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3 -am -DskipITs -Dtest=AuthorizationContextFacadeTest,SystemAuthorizationSnapshotServiceTest,IdpRoleActivationSecurityIT test`

Expected: system-scoped snapshot or IdP-bound context behavior is missing.

- [ ] **Step 3: Implement Authorization Context semantics and internal APIs**

Reuse the existing Session table and role activation rules, but require caller identity fields to match target context. First snapshot request creates default context only for an ACTIVE mapping. Project `systemCode`-scoped permissions and advance `authVersion/contextVersion/policyVersion` monotonically. Emit the four RBAC invalidation event types from the spec through the existing Transactional Outbox adapter.

- [ ] **Step 4: Run RBAC3 context and snapshot tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3 -am -DskipITs=false test`

Expected: existing RBAC behavior remains green and new IdP-bound snapshot/activation tests pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-rbac3
git commit -m "feat(rbac3): expose session authorization contexts"
```

### Task 11: Downstream RBAC3 Starter Cache, Events, and Fence

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/pom.xml`
- Replace: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/security/Rbac3JwtVerifier.java` responsibility with IdP Starter dependency
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/security/Rbac3BearerAuthenticationFilter.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/client/Rbac3AuthorizationClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/cache/AuthorizationSnapshotCache.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/cache/RedisAuthorizationSnapshotCache.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/cache/SingleFlightSnapshotLoader.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/event/Rbac3AuthorizationInvalidationConsumer.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/autoconfigure/Rbac3StarterProperties.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/main/java/top/egon/cola/platform/rbac3/starter/autoconfigure/Rbac3StarterAutoConfiguration.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/cache/AuthorizationSnapshotCacheTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/cache/SingleFlightSnapshotLoaderTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/event/Rbac3AuthorizationInvalidationConsumerTest.java`
- Modify/Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter/src/test/java/top/egon/cola/platform/rbac3/starter/security/StarterFailClosedSecurityMatrixTest.java`

**Interfaces:**
- Consumes: authenticated `IdentityPrincipal` from IdP Starter.
- Produces: downstream-owned cache key `rbac3:authorization:{systemCode}:{tenantId}:{sid}`.
- Produces: `AuthorizationService` backed by current system snapshot and optional synchronous Fence.

- [ ] **Step 1: Write failing cache/failure/invalidation tests**

```java
@Test
void concurrentMissesPerformOneRbac3Fetch() throws Exception {
    runConcurrently(20, () -> loader.load(KEY));
    assertEquals(1, client.fetchCount());
}

@Test
void expiredCacheAndUnavailableRbac3FailsClosedWithoutExtendingTtl() {
    cache.put(KEY, snapshot(), Duration.ofSeconds(1));
    clock.advance(Duration.ofSeconds(2));
    client.fail();
    assertThrows(AuthorizationUnavailableException.class, () -> loader.load(KEY));
    assertFalse(cache.get(KEY).isPresent());
}

@Test
void contextEventDeletesExactSidWithoutRedisScan() {
    consumer.accept(contextChanged("tenant-a", "sid-1", 4));
    assertFalse(cache.get(key("app", "tenant-a", "sid-1")).isPresent());
    assertEquals(0, redis.scanCalls());
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter -am -DskipITs -Dtest=AuthorizationSnapshotCacheTest,SingleFlightSnapshotLoaderTest,Rbac3AuthorizationInvalidationConsumerTest,StarterFailClosedSecurityMatrixTest test`

Expected: downstream cache/client/event components are missing.

- [ ] **Step 3: Implement Cache-Aside + SingleFlight + idempotent invalidation**

Use a per-Key `CompletableFuture` map for JVM SingleFlight, Redis JSON snapshot with TTL+jitter, and user/tenant Set indexes written atomically. Events compare event/policy versions and delete exact keys or index members; never use `SCAN`. If an unexpired value exists, return it; if missing/expired and RBAC3 fetch fails, throw and map to 503/403 according to current security boundary without extending TTL.

- [ ] **Step 4: Run Starter tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter -am -DskipITs=false test`

Expected: cache concurrency, event order, exact invalidation and fail-closed matrices pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-starter egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-contract
git commit -m "feat(rbac3): add downstream authorization cache"
```

### Task 12: Protect All Four Admin Backends with IdP and RBAC3 Starters

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/security/IdpAdminSecurityConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/pom.xml`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3AdminSecurityConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/security/Rbac3JwtAuthenticationConverter.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/pom.xml`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/infrastructure/security/GatewayAdminSecurityConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/infrastructure/security/GatewayAdminJwtAuthenticationConverter.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/pom.xml`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminSecurityConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/security/DdcAdminJwtAuthenticationConverter.java`
- Create: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/interfaces/http/IdpAuthBootstrapController.java`
- Create: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/Rbac3AuthBootstrapController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayAuthBootstrapController.java`
- Create: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/java/top/egon/cola/component/ddc/admin/interfaces/http/DdcAuthBootstrapController.java`
- Test: `egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/security/AdminBootstrapAuthorizationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/security/AdminBootstrapAuthorizationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/infrastructure/security/AdminBootstrapAuthorizationIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/test/java/top/egon/cola/component/ddc/admin/security/AdminBootstrapAuthorizationIT.java`

**Interfaces:**
- Consumes: IdP `IdentityPrincipal` and RBAC3 system-scoped authorization.
- Produces: bootstrap response with identity, tenant, permissions/menu and snapshot versions.

- [ ] **Step 1: Change integration tests first**

```java
@Test
void tokenRolesDoNotGrantAdminPermission() throws Exception {
    mvc.perform(get("/api/v1/auth/bootstrap")
            .with(bearer(idpTokenWithExtraClaim("roles", List.of("ADMIN")))))
            .andExpect(status().isForbidden());
}

@Test
void rbac3SnapshotGrantsRegisteredSystemPermission() throws Exception {
    authorizeInRbac3("alice", SYSTEM_CODE, "admin:bootstrap:read");
    mvc.perform(get("/api/v1/auth/bootstrap").with(bearer(idpToken("alice"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.permissions[0]").exists());
}
```

- [ ] **Step 2: Run affected backend tests and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am -DskipITs -Dtest='*SecurityIntegrationTest,*AdminBootstrapAuthorizationIT' test`

Expected: existing converters still grant Claims or bootstrap endpoints are missing.

- [ ] **Step 3: Replace role/capability Claim conversion with Starter authorization**

All four Admin Backends use IdP Starter for authentication and RBAC3 Starter for permissions. Remove converters that interpret `roles` or `capabilities`. Configure unique `systemCode` and Audience. Keep service-to-service Internal endpoints in a separate security chain with explicit service credentials.

- [ ] **Step 4: Run backend security tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin,egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin,egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin -am -DskipITs=false test`

Expected: all Admin security suites pass; Token role/capability Claims have no effect.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp egon-cola-platforms/egon-cola-platform-rbac3 egon-cola-platforms/egon-cola-platform-gateway egon-cola-platforms/egon-cola-platform-dynamic-config-center
git commit -m "feat(security): protect admin backends with idp sso"
```

### Task 13: Unified Browser SSO for IdP, RBAC3, Gateway, and DDC

**Files:**
- Create: IdP Admin Web `package.json`, Vite/TS config, `src/auth/oauthClient.ts`, `src/auth/AuthContext.tsx`, login/consent/tenant/admin pages
- Modify: Gateway Admin Web `src/auth/AuthContext.tsx`, `LoginPage.tsx`, `tokenStore.ts`, API client and tests
- Modify: DDC Admin Web `src/auth/AuthContext.tsx`, `LoginPage.tsx`, `tokenStore.ts`, API client and tests
- Modify: RBAC3 Admin Web `src/features/auth/AuthenticationShell.tsx`, `LoginPage.tsx`, `auth.api.ts`, API client and tests
- Create: `src/auth/pkce.ts` and `src/auth/oauthClient.test.ts` in each Web project, using the same contract but no new shared build package
- Test: existing Web auth/API/integration tests

**Interfaces:**
- Produces: Authorization Code + PKCE S256 flow with in-memory Access Token and IdP Refresh Cookie.
- Removes: manual Token input, LocalStorage/SessionStorage Token persistence and RBAC3 password login.

- [ ] **Step 1: Write failing browser-auth tests in each Web project**

```ts
it('keeps access token in memory and redirects with S256 PKCE', async () => {
  const auth = createOAuthClient(config, cryptoFixture)
  await auth.authorize('tenant-a')
  expect(location.assign).toHaveBeenCalledWith(expect.stringContaining('code_challenge_method=S256'))
  expect(localStorage.length).toBe(0)
  expect(sessionStorage.length).toBe(0)
})

it('deduplicates concurrent refresh and returns to authorize after failure', async () => {
  await Promise.all([auth.refresh(), auth.refresh(), auth.refresh()])
  expect(fetch).toHaveBeenCalledTimes(1)
})
```

- [ ] **Step 2: Run all four Web tests and verify RED**

Run in each Admin Web directory: `npm test -- --run`

Expected: IdP Web is missing; existing Web stores Token persistently or lacks OAuth callback flow.

- [ ] **Step 3: Implement the SSO Auth Client and replace old Login pages**

Use Web Crypto SHA-256 for PKCE, `sessionStorage` only for short-lived `state/nonce/verifier` transaction data with immediate deletion at Callback, never for Token. Keep Access Token in React context memory. Call `/oauth2/token` with `credentials: 'include'`; deduplicate Refresh with one module-level Promise. Tenant switch starts a new Authorization request using the existing IdP Cookie.

- [ ] **Step 4: Run Web verification**

Run for each of the four Web directories:

```bash
npm test -- --run
npm run typecheck
npm run build
```

Expected: tests/typecheck/build succeed and no manual Token field is rendered.

Run: `rg -n "localStorage|sessionStorage.*token|accessToken.*sessionStorage|refreshToken.*storage" egon-cola-platforms/*/*-admin-web/src egon-cola-platforms/egon-cola-platform-idp/*-admin-web/src`

Expected: no Token persistence match; OAuth transaction state matches are allowed only in `pkce.ts/oauthClient.ts`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web
git commit -m "feat(web): adopt unified idp sso"
```

### Task 14: Mock Backend, DDC Registration, Gateway Release, and E2E Harness

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend/pom.xml`
- Create: fixture application/controller/security under that module `src/main/java`
- Modify: Gateway test aggregator POM and dependency management
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/idp/UnifiedIdentityTopologyIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/idp/UnifiedIdentityRevocationIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/idp/UnifiedIdentityTenantSwitchIT.java`
- Create: `scripts/unified-identity-local.sh`
- Create: `docs/runbooks/unified-identity-local.md`
- Modify: local YAML for IdP, RBAC3, Gateway, DDC and fixture ports/identities

**Interfaces:**
- Produces: mock backend `/api/mock/read` and `/api/mock/admin` protected by IdP + RBAC3 Starter.
- Produces: deterministic local commands `prepare`, `verify`, `start`, `status`, `stop` without containers.

- [ ] **Step 1: Write failing end-to-end tests**

```java
@Test
void gatewayAcceptsIdentityWhileBackendEnforcesRbacPermission() {
    String token = idp.loginAndAuthorize("alice", "tenant-a", "mock-backend");
    assertEquals(403, gateway.get("/mock/admin", token).statusCode());
    rbac3.grantAndActivate("alice", "tenant-a", tokenSid(token), "mock:admin");
    awaitAuthorizationEvent();
    assertEquals(200, gateway.get("/mock/admin", token).statusCode());
}

@Test
void disablingUserRejectsExistingAccessTokenAtGateway() {
    String token = idp.loginAndAuthorize("alice", "tenant-a", "mock-backend");
    idp.disable("alice");
    awaitIdentityEvent();
    assertEquals(401, gateway.get("/mock/read", token).statusCode());
}
```

- [ ] **Step 2: Run the live topology test and verify RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -DskipITs=false -Dtest=UnifiedIdentityTopologyIT,UnifiedIdentityRevocationIT,UnifiedIdentityTenantSwitchIT test`

Expected: fixture module/topology is missing.

- [ ] **Step 3: Implement host-local fixture and lifecycle script**

The script uses explicit local ports and PID files under a task-specific `mktemp -d`/`.runtime/unified-identity` directory. It checks PostgreSQL and Redis without deleting existing databases, creates only named development schemas/databases after confirmation already encoded by the local profile, starts DDC Admin, Gateway Admin/Engine, IdP, RBAC3 and mock backend, bootstraps clients/users/mappings/roles/releases, then runs HTTP assertions. It must not use Docker or start services during ordinary unit-test Tasks.

- [ ] **Step 4: Run end-to-end verification**

Run:

```bash
./scripts/unified-identity-local.sh prepare
./scripts/unified-identity-local.sh verify
```

Expected evidence:

- DDC CONFIG_CLIENT and HTTP_PROVIDER entries for IdP, RBAC3 and mock backend.
- Gateway Release resolves all three providers through DDC.
- Valid IdP Token reaches mock backend through Gateway.
- Missing RBAC permission is 403 downstream, not Gateway.
- Role activation changes behavior without Token replacement.
- Tenant switch produces different `tid` with same SSO Family behavior.
- User disable and Refresh replay reject old Access Token.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway egon-cola-platforms/egon-cola-platform-idp egon-cola-platforms/egon-cola-platform-rbac3 egon-cola-platforms/egon-cola-platform-dynamic-config-center scripts/unified-identity-local.sh docs/runbooks/unified-identity-local.md
git commit -m "test(identity): verify unified platform topology"
```

### Task 15: Full Verification, Completion Audit, and Quality-Control Startup

**Files:**
- Modify only files required by a failing verification, always after adding/re-running a regression test.
- Update: `docs/runbooks/unified-identity-local.md` with final verified ports, URLs, account bootstrap procedure and stop command.

**Interfaces:**
- Produces: fresh build/test/runtime evidence for every acceptance item in the spec.
- Produces: running IdP, RBAC3, Gateway, DDC and mock backend processes for user quality control.

- [ ] **Step 1: Verify migration immutability and no forbidden coupling**

Run:

```bash
git diff 0c5966ad -- egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V1__create_rbac3_schema.sql egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/db/migration/V2__add_session_strong_authentication_time.sql
./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am dependency:tree -Dincludes=top.egon:egon-cola-platform-rbac3-gateway-adapter
rg -n 'roles|permissions|capabilities|authVersion|policyVersion' egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/token
```

Expected: first two commands have no forbidden diff/dependency; Token source match is absent except explicit rejection tests or comments.

- [ ] **Step 2: Run the full Java reactor**

Run: `./mvnw -pl egon-cola-platforms -am clean verify`

Expected: `BUILD SUCCESS`, zero test failures.

- [ ] **Step 3: Run all four Web suites**

For each Admin Web directory run:

```bash
npm test -- --run
npm run lint
npm run typecheck
npm run build
```

Expected: all commands exit 0.

- [ ] **Step 4: Run fresh host-local end-to-end verification**

Run:

```bash
./scripts/unified-identity-local.sh stop
./scripts/unified-identity-local.sh prepare
./scripts/unified-identity-local.sh verify
```

Expected: all scenarios in Spec 23.3 and Acceptance 1-18 produce explicit PASS lines.

- [ ] **Step 5: Audit every spec acceptance item against evidence**

Create a local checklist from Spec Section 24. For each item record the exact test class, command output, HTTP response or DDC/Gateway runtime endpoint that proves it. Any missing evidence returns to the owning Task; do not mark completion from a narrower test.

- [ ] **Step 6: Commit verification-driven fixes and final runbook**

Commit each regression fix in its owning module immediately after its targeted test returns green. Then commit the final runbook separately:

```bash
git add docs/runbooks/unified-identity-local.md
git commit -m "docs(identity): finalize local quality-control runbook"
```

If the runbook is already accurate, do not create an empty commit.

- [ ] **Step 7: Start all systems for user quality control**

Run:

```bash
./scripts/unified-identity-local.sh start
./scripts/unified-identity-local.sh status
```

Expected: status reports running DDC Admin, Gateway Admin, Gateway Engine, IdP Admin, RBAC3 Admin and mock backend, with their exact PIDs/ports and health URLs. Leave these processes running; report the stop command without executing it.

---

## Execution Notes

- User explicitly selected Inline Execution and explicitly authorized implementation on the current `main` checkout; no worktree or subagent is created.
- At the start of execution, load this plan and use `superpowers:executing-plans` task by task.
- For every new behavior, use `superpowers:test-driven-development`; record the RED command before production edits.
- Before each commit and before any completion claim, use `superpowers:verification-before-completion` and read the full command result.
- If a test exposes a design contradiction, update the spec and plan in a dedicated documentation commit before continuing.
- The final process startup is explicitly requested by the user and occurs only after full verification.
