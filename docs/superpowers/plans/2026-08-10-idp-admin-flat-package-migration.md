# IdP Admin Flat Package Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `egon-cola-platform-idp-admin` 迁移为 `identity/oauth/token/audit` 业务域优先、域内 `controller/config/service/service.impl/repo/domain.dto/domain.vo/domain.pojo` 扁平分层，并将跨域能力统一收敛到 `support`。

**Architecture:** 保留 `idp-core` 的领域模型、Facade 和 Port，不复制核心认证逻辑。Admin 只重组 Web、管理用例、持久化适配器和平台集成；Controller 依赖域 Service 接口，现有嵌套请求/响应 Record 提取为独立 DTO/VO，JPA Entity 进入 `domain.pojo`。DDC、RBAC3、Outbox、安全、启动与运行时注册进入 `support`。

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring Security, Spring Data JPA, Redisson, Maven, JUnit 5.

## Global Constraints

- 不修改 HTTP 路径、HTTP Method、状态码、JSON 字段、Gateway Operation 名称或权限编码。
- 不修改数据库表、Redis Key、DDC Key、JWT Claims、Token 生命周期和 OAuth2/PKCE 行为。
- 不修改 `idp-core`、`idp-starter`、`idp-gateway-adapter` 的包结构，不增加兼容壳或旧包转发类型。
- 生产源码移动优先使用 `git mv`；每个新 Java 包补中文在前、英文在后的 `package-info.java`。
- 不新增依赖、设计模式、Flyway Migration 或运行时进程；不启动应用。
- 每个 Task 只提交自身文件，提交前运行 `git diff --cached --check` 和路径检查。

---

### Task 1: Migrate Cross-Domain Support Packages

**Files:**

- Move current `bootstrap/*` to `support/bootstrap/*`.
- Move current `security/*` to `support/security/*`.
- Move current `integration/ddc/*` to `support/ddc/*`.
- Move current `integration/rbac3/*` to `support/rbac3/*`.
- Move current `integration/runtime/*` to `support/runtime/*`.
- Move `integration/outbox/IdentityOutboxPublisher.java`, `outbox/domain/IdentityOutboxEventEntity.java`, and `outbox/infrastructure/IdentityOutboxEventRepository.java` to `support/outbox/service`, `support/outbox/domain/pojo`, and `support/outbox/repo`.
- Move `interfaces/http/IdpHttpExceptionHandler.java` to `support/web` and `IdpAuthBootstrapController.java` to `support/security`.
- Modify all affected production/test imports and package declarations.
- Create `package-info.java` for every new support package.

**Interfaces:**

- Preserve all Spring Bean names, conditional properties, DDC keys, Redis keys, security filter order and runtime readiness behavior.
- Keep `IdentityOutboxPublisher` implementing the existing IdP Core ports and identity state projection contract.

- [x] Move files with `git mv`, update declarations/imports, and add support package documentation.
- [x] Run focused support tests with `-Dtest=IdpBootstrapRunnerTest,IdpBootstrapServiceTest,IdpDevelopmentClientBootstrapTest,IdpDdcPolicyApplierTest,IdpDdcPolicyConfigurationTest,IdentityOutboxPublisherTest,IdpHttpProviderPublicationGateTest,IdpAdminSecurityIT -Dsurefire.failIfNoSpecifiedTests=false`.
- [x] Commit as `refactor(idp): flatten admin support packages`.

### Task 2: Migrate the Identity Domain

**Files:**

- Move controllers to `identity/controller`.
- Move JPA entities to `identity/domain/pojo`.
- Move persistence adapters to `identity/repo` and password hashing to `identity/service/impl`.
- Replace `IdentityUserAdminService` with `IdentityUserService` plus `IdentityUserServiceImpl`.
- Replace `IdentityUserStateReconciler` with `IdentityUserStateService` plus `IdentityUserStateServiceImpl`; extract `IdentityStateProjection`.
- Extract create/update DTOs and user/reset VO records into `identity/domain/dto` and `identity/domain/vo`.
- Add `identity/config/IdentityConfig` for the identity Facade wiring split from the old platform configuration.
- Modify matching tests and package documentation.

**Interfaces:**

- `IdentityUserService` exposes `list`, `create`, `update`, `resetPassword`, and `revokeAll` with extracted DTO/VO types.
- `IdentityUserStateService` exposes `int reconcile()`.
- JSON field names and admin permission checks remain byte-for-byte compatible at the Controller boundary.

- [x] Add/adjust compile-time tests for Service interfaces and extracted DTO/VO contracts.
- [x] Move and refactor the identity production code.
- [x] Run `IdentityUserAdminServiceTest`, `IdentityUserStateReconcilerTest`, `IdentityEntityMappingTest`, `IdentityPersistenceAdapterTest`, `SpringPasswordHashAdapterTest`, and `IdentityProfileControllerIT` under their new names/packages.
- [x] Commit as `refactor(idp): flatten admin identity packages`.

### Task 3: Migrate the OAuth Domain

**Files:**

- Move OAuth protocol and client controllers to `oauth/controller`; rename `IdpSsoLoginController` to `OAuthLoginController`.
- Split `/oauth2/userinfo` from `IdentityProfileController` into `OAuthUserInfoController` without changing the route or response.
- Move `IdpOAuthConfiguration` to `oauth/config/OAuthConfig`.
- Replace `OAuthClientAdminService` with `OAuthClientService` plus `OAuthClientServiceImpl`.
- Move OAuth repositories and Redis stores to `oauth/repo`.
- Move client entities to `oauth/domain/pojo` and extract the SSO session record.
- Extract login/client request DTOs and login/token/error/client/userinfo response VOs.
- Modify matching tests and package documentation.

**Interfaces:**

- Preserve Authorization Code + PKCE S256 validation, redirect/audience exact matching, refresh-cookie-only transport and OAuth error bodies.
- `OAuthClientService` exposes list/create/update/redirect/audience operations with extracted DTO/VO types.

- [x] Add/adjust contract assertions for the extracted OAuth DTO/VO JSON names.
- [x] Move and refactor OAuth production code.
- [x] Run `IdpSsoSessionStoreTest`, `OAuthAuthorizationFlowIT`, `OAuthClientAdminServiceTest`, `IdpSsoLoginControllerIT`, `OAuthTokenTransportIT`, and `IdentityProfileControllerIT` under their new names/packages.
- [x] Commit as `refactor(idp): flatten admin oauth packages`.

### Task 4: Migrate Token and Audit Domains

**Files:**

- Move signing-key Controller to `token/controller`, configuration to `token/config`, persistence to `token/repo`, entity to `token/domain/pojo`, and signer/key-loader implementations to `token/service/impl`.
- Replace `SigningKeyAdminService` with `SigningKeyService` plus `SigningKeyServiceImpl`; extract publish DTO and signing-key VO.
- Move audit Controller, Repository and Entity to `audit/controller`, `audit/repo`, and `audit/domain/pojo`.
- Add `IdentityAuditService` and `IdentityAuditServiceImpl` so the Controller no longer queries the Repository directly.
- Extract audit query DTO and audit/page VOs.
- Add package documentation and update tests/imports.

**Interfaces:**

- Preserve RS256/JWKS/PEM file checks, refresh rotation/replay semantics, signing-key lifecycle and audit pagination/sort order.
- `SigningKeyService` exposes list/publish/activate/retire; `IdentityAuditService` exposes paged query.

- [ ] Add/adjust DTO/VO and Service contract tests.
- [ ] Move and refactor token and audit production code.
- [ ] Run signing-key, key-loader, token-claims, refresh-rotation, audit and persistence entity tests.
- [ ] Commit as `refactor(idp): flatten admin token and audit packages`.

### Task 5: Final Residual and Reactor Verification

**Files:**

- Modify any remaining `idp-admin` tests, reflection strings, documentation or Spring configuration references returned by the residual scans.
- Remove empty legacy source/test package directories after all files have moved.

**Interfaces:**

- Produces one source tree containing only the approved business-domain packages and `support` boundaries.

- [ ] Scan for forbidden production packages: `.interfaces.http`, `.application`, `.configuration`, `.infrastructure`, `.integration`, root `.security`, root `.bootstrap`, and root `.outbox`.
- [ ] Run `./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am test` and capture exit code 0.
- [ ] Run `./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am package -DskipTests` and capture exit code 0.
- [ ] Run `git diff --check`, inspect the final tree, and confirm no application was started.
- [ ] Commit any final test/document-only cleanup as `test(idp): verify flat admin package structure`; omit this commit when no cleanup is required.
