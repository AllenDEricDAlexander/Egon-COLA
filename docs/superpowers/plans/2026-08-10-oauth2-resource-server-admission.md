# OAuth 2.0 Resource Server Admission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [x]) syntax for tracking.

**Goal:** Implement approved OAuth 2.0 Resource Server admission, single-resource USER tokens, IdP-owned SERVICE authorization, authenticated DDC registration, and exact Gateway/downstream resource validation for the bizCode + appCode + env boundary.

**Architecture:** IdP is the source of truth for Resource Servers, public-key client credentials, OAuth grants, Admission Tickets, and service scopes. RBAC3 only decides USER entry and fine-grained user permissions. DDC accepts CONFIG_CLIENT, HTTP_PROVIDER, RPC_PROVIDER, and INTERNAL_GATEWAY registrations only when an IdP Admission Ticket exactly matches the registering triple and instance. Gateway and downstream services validate an at+jwt against one resolved Resource URI; downstream USER requests continue into RBAC3, while SERVICE requests use local IdP scope enforcement.

**Tech Stack:** Java 21, Spring Boot, Spring Security OAuth2 Resource Server, Spring Data JPA, PostgreSQL, SQLite, Flyway, Redis/Redisson, JWT/JWK, gRPC/Protobuf, JUnit 5, Mockito, Testcontainers, Maven Wrapper.

## Global Constraints

- The approved source of truth is docs/superpowers/specs/2026-08-10-oauth2-resource-server-admission-design.md.
- Do not start any application process. Verification is source, unit-test, integration-test, migration-test, and Maven reactor based.
- Implement tasks in order. Every task ends in its own commit and must leave its targeted modules compiling.
- Use test-driven development: first add the smallest failing test, run it and confirm the expected failure, then implement, rerun, and commit.
- Never edit an existing Flyway migration. Add exactly one next-version migration per affected database history:
  - IdP PostgreSQL: V2__add_oauth_resource_servers.sql.
  - DDC PostgreSQL: V8__add_resource_admission_audit.sql.
  - DDC SQLite: V8__add_resource_admission_audit.sql. This is the same logical V8 change applied once to each supported dialect history.
  - Gateway PostgreSQL: V11__rename_mcp_oauth_resource.sql.
- Resource identity is always the exact bizCode + appCode + env triple. bizCode is only a grouping dimension; no wildcard or future-app inheritance is allowed.
- One Access Token has exactly one Resource URI in aud. Do not retain the old audience request parameter, multi-audience token, or static clientIds validation path.
- USER authorization calls RBAC3 only for application entry and downstream fine-grained permissions. SERVICE token issuance and request authorization never query RBAC3 permission data.
- Admission Ticket and OAuth Access Token are different JWT types and audiences. Neither may be accepted in place of the other.
- Production admission is fail-closed. Test code may inject an in-memory admission port; ordinary business configuration must not disable admission.
- Private keys are read only from absolute owner-only files. Never persist or log private keys, raw assertions, raw Admission Tickets, or access tokens.
- Every new or changed Java class, record, enum, field, constructor, enum constant, and method receives project-style Chinese/English Javadoc. Every new Java package receives bilingual package-info.java.
- Preserve unrelated worktree changes. Stage and commit only files belonging to the current task.

## Design Pattern Decisions

- Use compact Specification-style domain services for ResourceServerAdmissionPolicy, UserResourceAccessPolicy, and ClientCredentialsAccessPolicy. Each service owns a complete business rule sequence; do not split every predicate into a separate class.
- Use Ports and Adapters at module boundaries:
  - UserResourceAccessAuthorizationPort isolates IdP Core from RBAC3 transport.
  - DdcAdmissionTicketSupplier isolates DDC components from IdP.
  - ResourceServerRuntimePort isolates domain state changes from Redis projection and outbox delivery.
- Use the existing Transactional Outbox component for Resource Server disable delivery. Do not build another polling/retry framework.
- Do not introduce an authentication Strategy hierarchy. private_key_jwt is the only client authentication method in this phase; a direct authenticator is simpler. Introduce strategies only when a second real mechanism such as mTLS is implemented.
- Do not use a factory for USER versus SERVICE authorization. Their issuance policies are separate explicit services, and the token endpoint dispatches directly by grant_type.

## Dependency and Delivery Map

~~~text
IdP Core
  -> IdP Admin persistence and OAuth endpoints
  -> IdP Starter token/admission runtime
  -> IdP Gateway Adapter

RBAC3 Admin
  -> USER Resource Access Decision used by IdP
  -> SERVICE endpoints protected by IdP scopes, not RBAC3 service permissions

DDC Starter API
  <- IdP Starter implementation of DdcAdmissionTicketSupplier
  -> DDC config-client, HTTP, RPC, and Gateway registration producers
  -> RPC DDC protobuf adapter
  -> DDC Admin verification and lease persistence

IdP Outbox
  -> DDC idempotent triple revocation command
  -> existing DDC subscriptions remove revoked instances from Gateway
~~~

---

## Task 1: Add IdP Resource, Grant, Credential, and Principal Core Contracts

**Files:**

- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceServer.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceServerStatus.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientResourceGrant.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceGrantType.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientJwkCredential.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ResourceServerAdmissionPolicy.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/UserResourceAccessPolicy.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicy.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/resource/package-info.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/ResourceServerStore.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientCredentialStore.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/ClientAssertionReplayStore.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/UserResourceAccessAuthorizationPort.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/core/port/ResourceServerRuntimePort.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/IdpPrincipal.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/PrincipalType.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/ServiceIdentityPrincipal.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/IdentityPrincipal.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/IdpClaimNames.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/main/java/top/egon/cola/platform/idp/contract/IdpErrorCode.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/resource/ResourceServerPolicyTest.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/core/resource/ClientCredentialsAccessPolicyTest.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core/src/test/java/top/egon/cola/platform/idp/contract/IdentityPrincipalTest.java

- [x] **Step 1: Write failing domain and contract tests**

Cover exact triple identity, absolute fragment-free Resource URI, active status, USER_DELEGATION invariants, CLIENT_CREDENTIALS tenant/scope invariants, and distinct USER/SERVICE principals.

~~~java
assertThat(resource.matches("permission", "idp", "prod")).isTrue();
assertThat(resource.matches("permission", "rbac3", "prod")).isFalse();

assertThat(policy.authorize(client, target, "tenant-001",
        Set.of("rbac3:policy:read")).scopes())
        .containsExactly("rbac3:policy:read");

assertThatThrownBy(() -> policy.authorize(client, target, "tenant-002",
        Set.of("rbac3:policy:read")))
        .hasMessageContaining("IDP_SERVICE_RESOURCE_GRANT_NOT_FOUND");
~~~

- [x] **Step 2: Run the tests and confirm they fail because the new contracts do not exist**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-core \
  -Dtest=ResourceServerPolicyTest,ClientCredentialsAccessPolicyTest,IdentityPrincipalTest test
~~~

- [x] **Step 3: Implement the minimal domain records, policies, and ports**

Use these stable shapes:

~~~java
public record ResourceServer(
        String resourceServerId,
        URI resourceUri,
        String bizCode,
        String appCode,
        String environment,
        String managementClientId,
        String rbacApplicationCode,
        String entryPermissionCode,
        Duration admissionTicketTtl,
        ResourceServerStatus status,
        long version) {
}

public sealed interface IdpPrincipal
        permits IdentityPrincipal, ServiceIdentityPrincipal {
    PrincipalType principalType();
    String subject();
    String tenantId();
    String clientId();
    String tokenId();
    Instant issuedAt();
    Instant expiresAt();
}

public record ServiceIdentityPrincipal(
        String subject,
        String tenantId,
        String clientId,
        String tokenId,
        URI resourceUri,
        long resourceVersion,
        Set<String> scopes,
        String sourceBizCode,
        String sourceAppCode,
        String sourceEnvironment,
        String credentialId,
        Instant issuedAt,
        Instant expiresAt) implements IdpPrincipal {
}
~~~

IdentityPrincipal continues to represent USER and implements IdpPrincipal without
changing its record components or constructor; it adds only principalType()
returning USER. Add claim constants for principal_type, resource_version, scope,
source_biz, source_app, source_env, credential_id, resource, and token_use.

- [x] **Step 4: Run all IdP Core tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-core test
~~~

- [x] **Step 5: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core
git commit -m "feat(idp): add resource authorization domain"
~~~

---

## Task 2: Add IdP V2 Persistence and Remove the Audience Table Dependency

**Files:**

- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/db/migration/V2__add_oauth_resource_servers.sql
- Delete: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/domain/pojo/IdentityClientAudienceEntity.java
- Delete: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/IdentityClientAudienceRepository.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityResourceServerEntity.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityClientJwkEntity.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/resource/domain/pojo/IdentityClientResourceGrantEntity.java
- Create: repositories under top.egon.cola.platform.idp.admin.resource.repo
- Create: package-info.java for resource, resource.domain, resource.domain.pojo, and resource.repo
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/oauth/repo/JpaOAuthClientStore.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrap.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/support/migration/IdpMigrationIT.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/support/persistence/IdpPersistenceEntityContractTest.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpDevelopmentClientBootstrapTest.java

- [x] **Step 1: Extend migration tests before creating V2**

Assert all three tables, unique constraints, grant-type checks, tenant/scope checks, and absence of identity_client_audience after V2. Run the PostgreSQL integration path as well as script-contract assertions.

- [x] **Step 2: Run focused tests and confirm V2 is missing**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am \
  -Dtest=IdpMigrationIT,IdentityPersistenceEntityContractTest,IdpDevelopmentClientBootstrapTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Add V2 and JPA mappings**

V2 creates identity_resource_server, identity_client_jwk, and
identity_client_resource_grant with every column and constraint defined in
section 11.1 of the approved spec, then drops identity_client_audience.

Use a nullable tenant_id only for USER_DELEGATION and a required tenant_id plus non-empty allowed_scopes for CLIENT_CREDENTIALS. Use partial unique indexes exactly as approved. Store public JWK JSON only.

Update JpaOAuthClientStore to obtain allowed user Resource URIs through USER_DELEGATION joins during the transition to Task 5; it must never query the dropped audience table. Update development bootstrap to create explicit idp and rbac3 Resource rows and explicit application-level grants. Production bootstrap remains explicit and does not invent wildcard grants.

- [x] **Step 4: Run migration, entity, repository, and bootstrap tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am \
  -Dtest=IdpMigrationIT,IdentityPersistenceEntityContractTest,IdpDevelopmentClientBootstrapTest,OAuthClientServiceImplTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 5: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin
git commit -m "feat(idp): persist resource servers and grants"
~~~

---

## Task 3: Implement Resource Server, Key, Grant, Projection, and Batch Admin APIs

**Files:**

- Create controllers, services, service implementations, DTOs, VOs, and package-info.java files under:
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/resource
- Create: resource/config/ResourceServerConfig.java
- Create: resource/service/ResourceServerService.java
- Create: resource/service/impl/ResourceServerServiceImpl.java
- Create: resource/service/ResourceServerProjectionService.java
- Create: resource/controller/ResourceServerController.java
- Create: resource/controller/ClientResourceGrantController.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/support/security/IdpSecurityConfig.java
- Modify: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/java/top/egon/cola/platform/idp/admin/support/bootstrap/IdpBootstrapService.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerServiceImplTest.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/ResourceServerProjectionServiceTest.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerControllerTest.java
- Create: egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/controller/ClientResourceGrantControllerTest.java

- [x] **Step 1: Write service and controller tests for all approved endpoints**

Cover create/list/detail, enable/disable, add/remove JWK, USER_DELEGATION grant, CLIENT_CREDENTIALS grant, and explicit appCodes batch expansion. Assert:

- duplicate Resource URI or triple is rejected;
- managementClientId is bound to the same Resource;
- batch operations write one row per selected app and never a wildcard;
- key deletion cannot remove the last active key from an enabled Resource;
- service grants require one tenant and at least one allowed scope;
- all mutations enforce optimistic version checks.

- [x] **Step 2: Confirm the focused tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am \
  -Dtest=ResourceServerServiceImplTest,ResourceServerControllerTest,ClientResourceGrantControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement the flat business package**

Use the approved structure only: controller, config, service, service.impl, repo, domain, domain.dto, domain.vo, and domain.pojo. Put cross-domain transport/security helpers under support.

Project these Redis indexes transactionally after successful persistence:

~~~text
identity:resource-server:{resourceServerId}
identity:resource-uri:{sha256(resourceUri)}
identity:resource-scope:{sha256(bizCode + ":" + appCode + ":" + env)}
identity:oauth-client:{clientId}
identity:service-resource-grant:{clientId}:{resourceServerId}:{tenantId}
~~~

Each Resource projection includes status, URI, triple, version, managementClientId, RBAC application, and entry permission. Client projection includes type, status, bound source Resource, and version. Projection write/delete failures fail the management mutation; no stale ACTIVE result may be returned.

- [x] **Step 4: Add and test RBAC3 management permission declarations**

Add exactly:

~~~text
idp:resource-server:read
idp:resource-server:create
idp:resource-server:update
idp:resource-server:status
idp:resource-server:key
idp:resource-server:grant
~~~

- [x] **Step 5: Run IdP Admin resource and security tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am \
  -Dtest='*ResourceServer*Test,*ClientResourceGrant*Test,IdpAdminSecurityIT,IdpBootstrapServiceTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin
git commit -m "feat(idp): manage resource server admission"
~~~

---

## Task 4: Add the RBAC3 USER Resource Entry Decision

**Files:**

- Create: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ResourceAccessDecisionRequest.java
- Create: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/ResourceAccessDecisionResponse.java
- Modify: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationController.java
- Modify: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/java/top/egon/cola/platform/rbac3/admin/authorization/application/AuthorizationDecisionService.java
- Modify: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java
- Modify: egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java
- Create or update package-info.java in any new package

- [x] **Step 1: Write a failing decision test**

POST /internal/v1/authorization/resource-access-decisions accepts identitySub, tid, sid, rbacApplicationCode, and entryPermissionCode. It returns only ALLOW/DENY, a stable reason, and authorization versions; it never returns the role or permission set.

Test positive entry permission, missing permission, inactive membership/session, wrong tenant/application, and decision-store failure.

- [x] **Step 2: Confirm the test fails because the endpoint is absent**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-rbac3-admin -am \
  -Dtest=InternalAuthorizationControllerTest,AuthorizationDecisionServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement the minimum USER-only decision**

Reuse the current authorization snapshot/decision path. Do not add service principals, service grants, or service scopes to RBAC3. Preserve version/fence semantics so a revoked USER permission is visible immediately.

- [x] **Step 4: Run RBAC3 focused tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-rbac3-admin -am \
  -Dtest=InternalAuthorizationControllerTest,AuthorizationDecisionServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 5: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-rbac3
git commit -m "feat(rbac3): decide user resource entry"
~~~

---

## Task 5: Replace USER audience Flow with RFC 8707 Resource Policy

**Files:**

- Modify IdP Core:
  - core/oauth/OAuthClient.java
  - core/oauth/AuthorizationRequest.java
  - core/oauth/AuthorizationCode.java
  - core/oauth/AuthorizationFacade.java
  - core/token/AccessTokenClaims.java
  - core/token/TokenFacade.java
  - src/test/java/top/egon/cola/platform/idp/core/oauth/AuthorizationFacadeTest.java
  - src/test/java/top/egon/cola/platform/idp/core/token/TokenFacadeTest.java
- Modify IdP Admin:
  - oauth/controller/OAuthAuthorizationController.java
  - oauth/controller/OAuthTokenController.java
  - oauth/config/OAuthConfig.java
  - token/service/impl/Rs256TokenService.java
  - support/rbac3/HttpTenantMembershipAdapter.java
- Create: support/rbac3/HttpUserResourceAccessAuthorizationAdapter.java
- Create or update bilingual package-info.java and integration tests
- Modify: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthAuthorizationFlowIT.java
- Modify: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenTransportIT.java
- Modify: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/token/service/impl/AccessTokenClaimsIT.java

- [x] **Step 1: Change tests to require resource and reject audience**

Add tests for:

- exactly one resource parameter at authorize and token exchange;
- missing, repeated, relative, fragmented, disabled, or ungranted Resource;
- a user with A entry permission denied for B;
- authorization code bound to one Resource;
- exchange rejects a Resource different from the code;
- refresh rechecks Resource status, client grant, membership, and RBAC entry;
- RBAC DENY maps to access_denied, unavailable maps to temporarily_unavailable;
- USER at+jwt contains one aud plus principal_type=USER and resource_version;
- USER token has no role, permission, data, field, or service scope claim.

- [x] **Step 2: Run the tests and confirm current audience behavior fails them**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-core,:egon-cola-platform-idp-admin -am \
  -Dtest=AuthorizationFacadeTest,TokenFacadeTest,OAuthAuthorizationFlowIT,OAuthTokenTransportIT,AccessTokenClaimsIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement one shared UserResourceAccessPolicy**

Authorization, code exchange, and refresh all call the same policy:

~~~java
UserResourceAccess authorize(
        String clientId,
        URI resource,
        String identitySub,
        String tenantId,
        String sessionId);
~~~

The ordered checks are Client ACTIVE, redirect/PKCE, Resource ACTIVE, USER_DELEGATION ACTIVE, membership ACTIVE, then RBAC3 entry ALLOW. Do not duplicate this sequence in controllers.

- [x] **Step 4: Cut over the HTTP protocol and token claims**

Remove audience request parsing. Require resource at authorize and code exchange. Use typ=at+jwt and exactly one aud. Keep refresh token internal claims bound to resourceServerId and resourceVersion so refresh can revalidate the current record.

- [x] **Step 5: Run USER OAuth tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-core,:egon-cola-platform-idp-admin -am \
  -Dtest=AuthorizationFacadeTest,TokenFacadeTest,OAuthAuthorizationFlowIT,OAuthTokenTransportIT,AccessTokenClaimsIT \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): issue single resource user tokens"
~~~

---

## Task 6: Implement private_key_jwt and IdP-owned Client Credentials

**Files:**

- Create core credential and token classes under idp-core/core/oauth and idp-core/core/token
- Create: idp-admin/oauth/service/impl/PrivateKeyJwtAuthenticator.java
- Create: idp-admin/oauth/repo/RedisClientAssertionReplayStore.java
- Create: idp-admin/token/service/impl/ClientCredentialsTokenService.java
- Create: idp-admin/support/oauth/LocalServiceAccessTokenSupplier.java
- Modify: idp-admin/oauth/controller/OAuthTokenController.java
- Modify: idp-admin/oauth/domain/pojo/IdentityClientEntity.java
- Modify: idp-admin/oauth/service/impl/OAuthClientServiceImpl.java
- Modify: idp-admin/token/service/impl/Rs256TokenService.java
- Modify: idp-admin/oauth/config/OAuthConfig.java
- Delete: idp-admin/support/rbac3/FileServiceAuthorizationSupplier.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/service/impl/PrivateKeyJwtAuthenticatorTest.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/repo/RedisClientAssertionReplayStoreTest.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/token/service/impl/ClientCredentialsTokenServiceTest.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/support/oauth/LocalServiceAccessTokenSupplierTest.java
- Modify: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthTokenTransportIT.java

- [x] **Step 1: Write failing assertion and service-grant tests**

Cover iss=sub=client_id, endpoint-specific aud, kid lookup, RS256 allowlist, iat/exp maximum 60 seconds, validity windows, ACTIVE client/key/resource/grant, Redis replay rejection, exact tenant, requested scope subset, and no refresh token.

~~~java
assertThat(response.tokenType()).isEqualTo("Bearer");
assertThat(response.refreshToken()).isNull();
assertThat(claims.get("principal_type")).isEqualTo("SERVICE");
assertThat(claims.getAudience()).containsExactly(targetResourceUri);
assertThat(claims.getClaimAsStringList("scope"))
        .containsExactly("rbac3:policy:read");
~~~

- [x] **Step 2: Confirm tests fail because client_credentials is unsupported**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-core,:egon-cola-platform-idp-admin -am \
  -Dtest='*PrivateKeyJwt*Test,*ClientCredentials*Test,OAuthTokenTransportIT' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement direct private_key_jwt authentication**

The authenticator accepts only:

~~~text
client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer
~~~

Select the public key by client_id plus kid before signature verification. Reject unknown algorithms before parsing claims. Store the successful clientId+jti replay key with TTL through put-if-absent.

- [x] **Step 4: Implement ClientCredentialsAccessPolicy and SERVICE token signing**

Derive source_biz, source_app, and source_env from the source Client's bound Resource Server. Never accept them from the request. The granted scope is the requested set after proving it is a subset of the IdP Service Grant.

- [x] **Step 5: Replace the static RBAC bearer file**

LocalServiceAccessTokenSupplier obtains a short-lived SERVICE token through the same ClientCredentialsAccessPolicy and signing path, caches it only until a renewal skew, and supplies it to the existing RBAC3 HTTP adapters. Production configuration points to an owner-only private key file and explicit client/kid; remove the static bearer-file property and class.

- [x] **Step 6: Run IdP OAuth and replay tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin -am \
  -Dtest='*PrivateKeyJwt*Test,*ClientCredentials*Test,OAuthTokenTransportIT,*Replay*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 7: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp
git commit -m "feat(idp): authorize service client credentials"
~~~

---

## Task 7: Add Admission Endpoint and IdP Starter Admission Client

**Files:**

- Create: idp-core/core/resource/AdmissionRequest.java
- Create: idp-core/core/resource/AdmissionTicketClaims.java
- Create: idp-admin/resource/controller/ResourceServerAdmissionController.java
- Create: idp-admin/resource/service/impl/ResourceServerAdmissionServiceImpl.java
- Modify: idp-admin/token/service/impl/Rs256TokenService.java
- Create in DDC Starter:
  - api/extension/DdcAdmissionTicketSupplier.java
  - model/admission/DdcAdmissionRequest.java
  - model/admission/DdcAdmissionTicket.java
  - package-info.java for model.admission
- Modify: idp-starter/pom.xml to depend on DDC Starter without creating a reverse dependency
- Create in IdP Starter:
  - admission/PrivateKeyJwtAssertionFactory.java
  - admission/HttpResourceServerAdmissionClient.java
  - admission/CachingDdcAdmissionTicketSupplier.java
  - admission/OwnerOnlyPrivateKeyLoader.java
  - admission/package-info.java
- Modify: idp-starter/autoconfigure/IdpStarterProperties.java
- Modify: idp-starter/autoconfigure/IdpStarterAutoConfiguration.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/service/impl/ResourceServerAdmissionServiceImplTest.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/controller/ResourceServerAdmissionControllerTest.java
- Create: idp-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/OwnerOnlyPrivateKeyLoaderTest.java
- Create: idp-starter/src/test/java/top/egon/cola/platform/idp/starter/admission/CachingDdcAdmissionTicketSupplierTest.java
- Modify: idp-starter/src/test/java/top/egon/cola/platform/idp/starter/autoconfigure/IdpStarterAutoConfigurationTest.java

- [x] **Step 1: Write failing endpoint and starter tests**

Test assertion binding to admission endpoint, exact triple and instance, separate typ/token_use/aud, ticket TTL, replay, key rotation, owner-only absolute private-key path, caching, renewal skew, and IdP-unavailable fail-closed behavior.

- [x] **Step 2: Confirm tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin,:egon-cola-platform-idp-starter,:egon-cola-platform-dynamic-config-center-starter -am \
  -Dtest='*Admission*Test,IdpStarterAutoConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement Admission Ticket issuance**

Sign:

~~~text
typ=rs-admission+jwt
token_use=resource_server_admission
aud=ddc-registry
sub=resourceServerId
resource, resource_version, biz, app, env, instance_id, credential_id
~~~

The endpoint returns the JWT plus expiresAt for local renewal scheduling. It does not create an instance approval record.

- [x] **Step 4: Implement the dependency-neutral DDC SPI**

DDC Starter owns only the request/ticket interface. IdP Starter implements it. This preserves:

~~~text
idp-starter -> ddc-starter
ddc-starter -X-> idp-starter
~~~

Do not add any IdP type to DDC public models.

- [x] **Step 5: Wire IdP Starter admission properties**

Require resourceServerId, resourceUri, bizCode, appCode, env, instanceId, managementClientId, kid, absolute privateKeyPath, admissionEndpoint, and renewalSkew. Validate the configured URI/triple against the returned Ticket.

- [x] **Step 6: Run focused tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin,:egon-cola-platform-idp-starter,:egon-cola-platform-dynamic-config-center-starter -am \
  -Dtest='*Admission*Test,IdpStarterAutoConfigurationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 7: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter
git commit -m "feat(idp): issue resource admission tickets"
~~~

---

## Task 8: Carry Admission Tickets Through DDC Models and Protobuf

**Files:**

- Modify DDC Starter:
  - model/config/DdcInstanceRegisterRequest.java
  - model/config/DdcHeartbeatRequest.java
  - model/registry/DdcServiceRegistration.java
  - model/registry/DdcServiceLeaseRequest.java
  - api/client/DdcServiceRegistryClient.java
  - model/registry/DdcServiceInstance.java
  - service/lifecycle/DdcInstanceService.java
  - autoconfigure/DdcAutoConfiguration.java
- Modify HTTP Registration Starter:
  - DdcHttpRegistrationRuntime.java
  - DdcHttpRegistrationAutoConfiguration.java
- Modify RPC DDC Adapter producer boundaries:
  - registry/DdcRpcProviderRegistry.java
  - autoconfigure/DdcRpcAutoConfiguration.java
- Modify Gateway Engine producer boundary:
  - rpc/RpcGatewaySlotRuntime.java
  - GatewayEngineConfiguration.java
- Modify RPC DDC Adapter:
  - src/main/proto/ddc_config_runtime.proto
  - src/main/proto/ddc_service_registry.proto
  - src/main/proto/ddc_common.proto
  - mapping/DdcConfigProtoMapper.java
  - mapping/DdcRegistryProtoMapper.java
  - mapping/DdcCommonProtoMapper.java
  - client/config/RpcDdcConfigClient.java
  - client/registry/RpcDdcServiceRegistryClient.java
  - src/test/java/top/egon/cola/component/rpc/ddc/mapping/DdcConfigProtoMapperTest.java
  - src/test/java/top/egon/cola/component/rpc/ddc/mapping/DdcRegistryProtoMapperTest.java
  - src/test/java/top/egon/cola/component/rpc/ddc/contract/DdcRpcContractDescriptorTest.java
  - src/test/java/top/egon/cola/component/rpc/ddc/contract/DdcRpcGeneratedContractTest.java
  - src/test/java/top/egon/cola/component/rpc/ddc/client/RpcDdcConfigClientTest.java
  - src/test/java/top/egon/cola/component/rpc/ddc/client/RpcDdcServiceRegistryClientTest.java

- [x] **Step 1: Write failing model and protobuf mapping tests**

Admission Ticket is required on register and heartbeat, never stored in metadata, never returned in discovery, and never printed by toString. Deregistration remains possible with lease identity only.

- [x] **Step 2: Confirm contract tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-starter,:egon-cola-component-rpc-ddc-adapter -am \
  -Dtest='DdcServiceRegistrationTest,DdcConfigProtoMapperTest,DdcRegistryProtoMapperTest,DdcRpcContractDescriptorTest,DdcRpcGeneratedContractTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Add append-only protobuf fields**

Use new field numbers only:

~~~protobuf
message RegisterConfigClientRequest {
  // existing 1..9
  string admission_ticket = 10;
}
message HeartbeatConfigClientRequest {
  // existing 1..8
  string admission_ticket = 9;
}
message RegisterServiceRequest {
  // existing 1..8
  string admission_ticket = 9;
}
message HeartbeatServiceRequest {
  // existing 1..3
  string admission_ticket = 4;
}
~~~

Add admission audit fields to DdcServiceInstance with new numbers after revision. Do not reuse or renumber an existing protobuf tag.

- [x] **Step 4: Replace the two-string heartbeat API and update every producer**

Use DdcServiceLeaseRequest for heartbeat so service key, instance, lease, and new Ticket travel together:

~~~java
DdcLeaseOperationResult heartbeat(DdcServiceLeaseRequest request);
DdcLeaseOperationResult deregister(String instanceId, String leaseId);
~~~

Update all in-repo implementations and test fixtures in the same commit.
At each production constructor, inject DdcAdmissionTicketSupplier from Task 7,
acquire a Ticket from the exact outgoing triple/instance, and attach it to the
registration or heartbeat. This avoids a temporary unauthenticated production
constructor between Tasks 8 and 10.

- [x] **Step 5: Run DDC Starter and RPC adapter tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-starter,:egon-cola-component-rpc-ddc-adapter -am test
~~~

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter
git commit -m "feat(ddc): carry resource admission tickets"
~~~

---

## Task 9: Verify Admission and Cap DDC Leases

**Files:**

- Create under DDC Admin security/admission:
  - DdcAdmissionVerifier.java
  - IdpJwtDdcAdmissionVerifier.java
  - DdcAdmissionClaims.java
  - DdcAdmissionException.java
  - package-info.java
- Modify:
  - config/DdcAdminProperties.java
  - service/lease/DdcLeaseValidator.java
  - service/lease/DdcConfigLeaseService.java
  - service/lease/DdcInstanceAdminService.java
  - service/registry/DdcServiceRegistryService.java
  - repository/DdcConfigLeaseRedisRepository.java
  - repository/DdcServiceRegistryRedisRepository.java
  - model/entity/DdcInstanceEntity.java
- Create:
  - src/main/resources/db/postgresql/V8__add_resource_admission_audit.sql
  - src/main/resources/db/sqlite/V8__add_resource_admission_audit.sql
  - repository/DdcV8MigrationTest.java
- Modify relevant service, Redis, security, and RPC provider tests

- [x] **Step 1: Write the fail-closed security matrix**

Test missing Ticket, wrong signature, wrong typ, wrong token_use, wrong aud, expired Ticket, wrong biz/app/env/instance, disabled/stale Resource projection, and lease request longer than Ticket lifetime. Test both CONFIG_CLIENT and provider registration/heartbeat.

- [x] **Step 2: Confirm the tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest='*Admission*Test,DdcInstanceAdminServiceTest,DdcConfigLeaseServiceTest,DdcServiceRegistryServiceTest,DdcV8MigrationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Implement one verifier before both lease paths**

~~~java
DdcAdmissionClaims verify(
        String ticket,
        String bizCode,
        String appCode,
        String env,
        String instanceId);
~~~

Validate IdP signature/JWK, typ, token_use, issuer, ddc-registry audience, time, exact binding, and ACTIVE/current Resource projection. Use Spring Security JWT/JWK support already present in DDC Admin; do not add another JWT library.

- [x] **Step 4: Cap and persist leases**

~~~java
Instant leaseExpireAt = min(
        now.plusSeconds(requestedLeaseSeconds),
        admission.expiresAt());
~~~

Persist resourceServerId, resourceVersion, credentialId, and admissionExpiresAt:

- in ddc_instance columns for CONFIG_CLIENT;
- in the Redis service-instance document for RPC_PROVIDER, HTTP_PROVIDER, and INTERNAL_GATEWAY;
- in config lease Redis state so every heartbeat is compared with the original binding.

Never persist the raw Ticket or assertion.

- [x] **Step 5: Add one V8 per supported DDC dialect**

Both files are the next version of separate Flyway histories and represent the same logical schema change. Do not edit V1 through V7.

- [x] **Step 6: Run DDC Admin tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-admin -am \
  -Dtest='*Admission*Test,DdcInstanceAdminServiceTest,DdcConfigLeaseServiceTest,DdcServiceRegistryServiceTest,DdcV8MigrationTest,DdcRegistryRpcProviderTest,DdcConfigRpcProviderTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 7: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin
git commit -m "feat(ddc): verify resource admission leases"
~~~

---

## Task 10: Enforce Admission-aware Readiness and Renewal Failure Recovery

**Files:**

- Modify DDC Starter:
  - service/lifecycle/DdcRuntimeCoordinator.java
  - src/test/java/top/egon/cola/component/ddc/service/lifecycle/DdcRuntimeCoordinatorTest.java
- Modify HTTP Registration Starter:
  - DdcHttpRegistrationRuntime.java
  - src/test/java/top/egon/cola/component/ddc/http/registration/DdcHttpRegistrationRuntimeTest.java
- Modify RPC DDC Adapter:
  - registry/DdcRpcProviderRegistry.java
  - src/test/java/top/egon/cola/component/rpc/ddc/registry/DdcRpcProviderRegistryTest.java
- Modify Gateway Engine:
  - rpc/RpcGatewaySlotRuntime.java
  - GatewayEngineConfiguration.java
  - src/test/java/top/egon/cola/component/gateway/engine/rpc/RpcGatewaySlotRuntimeTest.java

- [x] **Step 1: Write producer-side renewal tests**

For each role, assert:

- Ticket is acquired before initial registration;
- current ticket is attached to every heartbeat;
- renewal occurs before expiresAt minus skew;
- IdP failure before initial registration prevents Ready;
- renewal failure moves the subsystem out of Ready and does not extend the lease;
- deregistration still runs best-effort during shutdown without requiring a fresh Ticket;
- no ordinary admission.enabled=false property exists.

- [x] **Step 2: Confirm tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-starter,:egon-cola-platform-dynamic-config-center-http-registration-starter,:egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-gateway-engine -am \
  -Dtest='DdcRuntimeCoordinatorTest,DdcHttpRegistrationRuntimeTest,DdcRpcProviderRegistryTest,RpcGatewaySlotRuntimeTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Enforce renewal timing at actual registration boundaries**

Use the DdcAdmissionTicketSupplier injection added in Task 8. Acquire using the
same DdcAdmissionRequest triple and instance used in the outgoing DDC request.
Renew before expiresAt minus skew. Do not obtain a Ticket in generic application
bootstrap code and pass it through metadata.

- [x] **Step 4: Make readiness follow authenticated lease state**

CONFIG_CLIENT, HTTP_PROVIDER, RPC_PROVIDER, and INTERNAL_GATEWAY are Ready only while their DDC lease was established or renewed with a non-expired Ticket. Preserve existing recovery state machines and add the admission failure as the cause; do not create parallel schedulers where an existing heartbeat scheduler is available.

- [x] **Step 5: Run producer tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-dynamic-config-center-starter,:egon-cola-platform-dynamic-config-center-http-registration-starter,:egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-gateway-engine -am test
~~~

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-dynamic-config-center \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine
git commit -m "feat(ddc): authenticate runtime registrations"
~~~

---

## Task 11: Revoke DDC Leases from Resource Disable Outbox Events

**Files:**

- Modify: idp-admin/pom.xml to use egon-cola-component-transactional-outbox-starter
- Create under idp-admin/resource/support/outbox:
  - TransactionalOutboxResourceServerEventAdapter.java
  - DdcResourceServerLifecycleDeliveryHandler.java
  - package-info.java
- Modify resource service/configuration from Task 3
- Modify RPC DDC Adapter:
  - src/main/proto/ddc_management.proto
  - mapping/DdcManagementProtoMapper.java
  - client management contract and tests
- Create in DDC Admin:
  - service/lease/DdcResourceAdmissionRevocationService.java
  - RPC provider method for idempotent triple revocation
- Modify:
  - DdcConfigLeaseRedisRepository.java
  - DdcServiceRegistryRedisRepository.java
  - DdcInstanceRepository.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/support/outbox/TransactionalOutboxResourceServerEventAdapterTest.java
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/resource/support/outbox/DdcResourceServerLifecycleDeliveryHandlerTest.java
- Create: ddc-admin/src/test/java/top/egon/cola/component/ddc/admin/service/lease/DdcResourceAdmissionRevocationServiceTest.java
- Modify: ddc-admin/src/test/java/top/egon/cola/component/ddc/admin/rpc/provider/DdcManagementRpcProviderTest.java
- Modify: rpc-ddc-adapter/src/test/java/top/egon/cola/component/rpc/ddc/mapping/DdcManagementProtoMapperTest.java

- [x] **Step 1: Write failing disable and idempotency tests**

Disable permission/idp/prod and assert:

- IDENTITY_RESOURCE_SERVER_DISABLED is enqueued in the same transaction as status/version change;
- payload contains resourceServerId, bizCode, appCode, env, and resourceVersion, but no key material;
- DDC revokes matching CONFIG_CLIENT and provider leases;
- permission/rbac3/prod remains online;
- replaying the same event/version is a successful no-op;
- transient DDC failure is retryable and the outbox record remains pending.

- [x] **Step 2: Confirm tests fail**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-component-rpc-ddc-adapter -am \
  -Dtest='*ResourceServerEvent*Test,*AdmissionRevocation*Test,DdcManagementProtoMapperTest,DdcManagementRpcProviderTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Enqueue through the repository-native Transactional Outbox**

Use channel identity-resource-runtime and destination identity.resource-server.disabled.v1. Keep the existing IdentityOutboxPublisher unchanged for its current user-state/audit responsibility; it is not a delivery engine. The new resource event uses the supported component rather than adding custom polling.

- [x] **Step 4: Deliver an idempotent DDC management command**

The delivery handler calls the DDC management client. DDC verifies the event version, revokes exact-triple config and provider leases, marks persisted config instances offline, and publishes the existing registry change notifications. Gateway removal then occurs through its existing DDC subscription path.

- [x] **Step 5: Run outbox and revocation tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-admin,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-component-rpc-ddc-adapter -am \
  -Dtest='*ResourceServerEvent*Test,*AdmissionRevocation*Test,DdcManagementProtoMapperTest,DdcManagementRpcProviderTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin \
  egon-cola-components/egon-cola-component-rpc/egon-cola-component-rpc-ddc-adapter
git commit -m "feat(idp): revoke disabled resource leases"
~~~

---

## Task 12: Make IdP Starter Validate Exact USER and SERVICE Resource Tokens

**Files:**

- Modify:
  - idp-starter/autoconfigure/IdpStarterProperties.java
  - idp-starter/autoconfigure/IdpStarterAutoConfiguration.java
  - idp-starter/security/IdpJwtVerifier.java
  - idp-starter/security/IdpAuthenticationToken.java
  - idp-starter/security/IdpBearerAuthenticationFilter.java
- Create:
  - idp-starter/state/IdentityResourceServerState.java
  - idp-starter/state/IdentityResourceServerStateReader.java
  - idp-starter/state/RedisIdentityResourceServerStateReader.java
  - idp-starter/state/IdentityOAuthClientStateReader.java
  - idp-starter/state/RedisIdentityOAuthClientStateReader.java
  - idp-starter/security/RequiresServiceScope.java
  - idp-starter/security/ServiceScopeAuthorization.java
- Modify package-info.java and all Starter tests

- [x] **Step 1: Replace existing verifier tests with the complete matrix**

Assert:

- typ must be at+jwt;
- aud contains exactly the configured Resource URI;
- Resource projection is ACTIVE and its version equals resource_version;
- USER requires current user state/tokenVersion and produces IdentityPrincipal;
- SERVICE requires active CONFIDENTIAL source client state and produces ServiceIdentityPrincipal;
- Admission Ticket is rejected as an access token;
- projection missing/malformed/stale or Redis unavailable is invalid_token;
- direct backend access gets the same result as Gateway-routed access;
- internal paths are no longer globally skipped;
- SERVICE scope mismatch returns 403, while token/resource failures return 401.

- [x] **Step 2: Confirm tests fail against the user-only verifier**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-starter -am \
  -Dtest=IdpJwtVerifierTest,IdpStarterAutoConfigurationTest,IdpBearerAuthenticationFilterTest,ServiceScopeAuthorizationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Replace multi-value properties**

Remove audiences and clientIds. Configure one resourceServerId and one resourceUri for the current application. Keep issuer and JWK Set URI. Add Redis prefixes for Resource and Client projections with secure fixed defaults.

- [x] **Step 4: Branch verification by principal_type**

USER and SERVICE share signature, issuer, time, exact Resource, and Resource projection checks. Only USER reads user state. Only SERVICE reads source Client state and scopes. Unknown principal_type fails closed.

- [x] **Step 5: Add local service-scope enforcement**

~~~java
@RequiresServiceScope("service:authorization:snapshot")
~~~

The guard accepts only an authenticated ServiceIdentityPrincipal and checks its signed scope set. It does not call RBAC3 or a remote service.

- [x] **Step 6: Run all Starter tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-starter -am test
~~~

- [x] **Step 7: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-starter \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-core
git commit -m "feat(idp): verify user and service resource tokens"
~~~

---

## Task 13: Remove RBAC3 Service Permissions and Protect Internal APIs with IdP Scopes

**Files:**

- Modify:
  - rbac3-admin/interfaces/http/InternalIdentityController.java
  - rbac3-admin/interfaces/http/InternalAuthorizationController.java
  - rbac3-admin/interfaces/http/ParticipationController.java
  - rbac3-admin/authorization/application/AuthorizationDecisionService.java
  - rbac3-admin/participation/application/ParticipationFacade.java
  - rbac3-admin/tenant/TenantContextResolver.java
  - rbac3-admin/security/Rbac3AdminSecurityConfiguration.java
  - rbac3-admin/security/Rbac3JwtAuthenticationConverter.java
  - rbac3-admin/security/Rbac3MethodAuthorization.java
- Delete:
  - rbac3-admin/security/CurrentRbac3ServicePrincipal.java
  - rbac3-admin/security/RequiresRbac3ServicePermission.java
- Modify: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalIdentityControllerTest.java
- Modify: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/interfaces/http/InternalAuthorizationControllerTest.java
- Modify: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/participation/ParticipationConcurrencyIT.java
- Modify: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/tenant/TenantContextFilterTest.java
- Modify: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/authorization/AuthorizationDecisionServiceTest.java

- [x] **Step 1: Write tests proving RBAC3 no longer owns machine permissions**

Use IdP ServiceIdentityPrincipal fixtures. Assert internal endpoints accept a valid target Resource Token with the required scope and reject:

- USER tokens on SERVICE-only endpoints;
- SERVICE tokens for another Resource;
- missing scope;
- wrong tenant;
- wrong source application binding.

Also assert no repository or snapshot query is made merely to authorize a SERVICE scope.

- [x] **Step 2: Confirm old service-principal tests fail the new expectation**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-rbac3-admin -am \
  -Dtest='InternalIdentityControllerTest,InternalAuthorizationControllerTest,ParticipationConcurrencyIT,TenantContextFilterTest,AuthorizationDecisionServiceTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Change endpoint annotations from RBAC permission to IdP scope**

Preserve the current strings as OAuth scope identifiers to minimize API churn:

~~~text
service:identity:resolve
service:identity:bind
service:authorization:snapshot
service:authorization:decide
service:authorization:fence
service:participation:write
service:participation:read
~~~

These codes are now granted only by IdP identity_client_resource_grant.allowed_scopes. RBAC3 does not store or evaluate them as permissions.

- [x] **Step 4: Replace CurrentRbac3ServicePrincipal**

Controllers and application services receive ServiceIdentityPrincipal. TenantContextResolver derives tenant from its tid, and source-application constraints use source_app. Rbac3MethodAuthorization remains USER-only after the service branch is removed.

- [x] **Step 5: Run RBAC3 tests and residual scan**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-rbac3-admin -am test
rg -n 'CurrentRbac3ServicePrincipal|RequiresRbac3ServicePermission|hasPermission\\(permission\\)' \
  egon-cola-platforms/egon-cola-platform-rbac3
~~~

Expected residual scan result: no active source or test references.

- [x] **Step 6: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-rbac3
git commit -m "refactor(rbac3): delegate service access to idp"
~~~

---

## Task 14: Resolve Exact Route Resources in Gateway and Map Both Principal Types

**Files:**

- Modify Gateway Core/Engine:
  - engine/http/RuleBackedHttpGatewaySecurityProcessor.java
  - engine/rpc/RuleBackedRpcGatewaySecurityProcessor.java
  - engine/mcp/McpGatewayIdentityAuthenticator.java
  - Create engine/http/RuleBackedHttpGatewaySecurityProcessorTest.java
  - Create engine/rpc/RuleBackedRpcGatewaySecurityProcessorTest.java
  - Create engine/mcp/McpGatewayIdentityAuthenticatorTest.java
- Modify IdP Gateway Adapter:
  - autoconfigure/IdpGatewayAdapterProperties.java
  - autoconfigure/IdpGatewayAdapterAutoConfiguration.java
  - security/IdpGatewayJwtVerifier.java
  - security/IdpIdentityAuthenticationProvider.java
  - security/IdpTrustedIdentityMapper.java
  - add GatewayResourceServerResolver.java and tests
- Rename MCP runtime/control-plane resource field where active:
  - gateway-contract McpRuntimeServer.java
  - gateway-admin MCP controller/service/entity
  - gateway-mcp-core descriptions
  - gateway-admin-web TypeScript field/label only as a contract-alignment change, without adding UI features
- Create: gateway-admin/src/main/resources/db/migration/V11__rename_mcp_oauth_resource.sql
- Update Gateway migration, contract, admin, engine, and adapter tests

- [x] **Step 1: Write route-binding and principal mapping tests**

Cover:

- HTTP and RPC route triple resolves the ACTIVE Resource projection;
- A token on B route returns IDP_RESOURCE_AUDIENCE_MISMATCH;
- same biz but different app still fails;
- stale Resource version fails;
- USER maps to Gateway principal type USER and user headers;
- SERVICE maps to type SERVICE and source/scopes headers;
- client-supplied X-Egon user/service headers are removed before trusted headers are written;
- Gateway performs no RBAC3 call.

- [x] **Step 2: Confirm tests fail with static audiences and USER-only mapping**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-gateway-adapter,:egon-cola-platform-gateway-engine,:egon-cola-platform-gateway-admin -am \
  -Dtest='IdpGatewaySecurityProviderTest,IdpGatewayAdapterAutoConfigurationTest,RuleBackedHttpGatewaySecurityProcessorTest,RuleBackedRpcGatewaySecurityProcessorTest,McpGatewayIdentityAuthenticatorTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Pass trusted route identity into authentication**

HTTP and RPC security processors add only server-derived attributes:

~~~text
idp.biz-code
idp.app-code
idp.env
~~~

GatewayResourceServerResolver uses the Redis scope index to resolve the Resource URI/version. It never trusts request headers or a caller-supplied audience. IdpGatewayJwtVerifier delegates the final token checks to the shared IdpJwtVerifier with the resolved Resource expectation.

- [x] **Step 4: Align MCP with a Resource URI**

Rename the active MCP OAuth field from a free audience string to resourceUri and validate it as an absolute fragment-free URI. If the database column is renamed, add only Gateway V11 and leave V1 through V10 untouched. The frontend change is limited to the existing field name/type/label and is not a new management feature.

- [x] **Step 5: Map and sanitize trusted identity**

USER headers contain subject, tenant, session, client, token, and Resource. SERVICE headers contain subject/client, tenant, source triple, scopes, credential, token, and Resource. Clear both header families before writing the one selected by principal_type.

- [x] **Step 6: Run Gateway and adapter tests**

~~~bash
./mvnw -B -ntp -pl :egon-cola-platform-idp-gateway-adapter,:egon-cola-platform-gateway-engine,:egon-cola-platform-gateway-admin -am test
~~~

- [x] **Step 7: Commit**

~~~bash
git add egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter \
  egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): bind tokens to route resources"
~~~

---

## Task 15: Remove Legacy Configuration, Complete Security Matrices, and Update Documentation

**Files:**

- Modify:
  - egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml
  - egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml
  - egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml
  - egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application.yml
  - egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml
  - egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/application.yml
  - egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend/src/main/resources/application.yml
- Modify existing admin-web OAuth request call sites so they send resource:
  - egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/auth/AuthContext.tsx
  - egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/types.ts
  - egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx
  - egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/auth/oauthClient.ts
  - egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/auth/oauthClient.test.ts
  - egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.tsx
  - egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/AuthContext.tsx
- Modify:
  - egon-cola-platforms/egon-cola-platform-idp/README.md
  - egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md
  - egon-cola-platforms/egon-cola-platform-gateway/README.md
  - egon-cola-platforms/egon-cola-platform-rbac3/README.md
- Modify the approved spec status and add implementation/operation notes without changing approved business decisions
- Create: idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java
- Create: ddc-test/src/test/java/top/egon/cola/component/ddc/test/DdcResourceAdmissionLifecycleTest.java
- Create: idp-gateway-adapter/src/test/java/top/egon/cola/platform/idp/gateway/security/IdpGatewayResourceBindingTest.java
- Create: rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3UserResourceAuthorizationIT.java
- Do not add a new standalone test module

- [x] **Step 1: Add the final acceptance tests before cleanup**

Create focused integration tests for:

1. registered permission/idp/prod instance admitted; forged permission/rbac3/prod instance rejected;
2. two instances of the same triple admitted without approval records;
3. USER with A entry permission gets A token and cannot get/use B token;
4. USER A token plus missing interface permission returns 403 from RBAC3 downstream;
5. SERVICE grant issues a single-resource, single-tenant token without RBAC3 calls;
6. SERVICE missing target/tenant/scope grant is denied at IdP;
7. SERVICE missing operation scope returns 403 downstream;
8. disable idp revokes only idp DDC leases and stops new tokens/tickets;
9. IdP unavailable permits no new admission and no lease extension beyond current Ticket expiry;
10. direct backend access enforces the same USER/SERVICE split as Gateway.

- [x] **Step 2: Run the acceptance tests and record expected initial failures**

~~~bash
./mvnw -B -ntp \
  -pl :egon-cola-platform-idp-admin,:egon-cola-platform-idp-starter,:egon-cola-platform-idp-gateway-adapter,:egon-cola-platform-rbac3-admin,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-platform-dynamic-config-center-http-registration-starter,:egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-gateway-engine \
  -am -Dtest='*ResourceAdmission*Test,*ResourceAccess*Test,*ClientCredentials*Test,*SecurityMatrix*Test' \
  -Dsurefire.failIfNoSpecifiedTests=false test
~~~

- [x] **Step 3: Remove active legacy paths**

Delete or rename active source/config references to:

~~~text
audience request parameter
IdpStarterProperties.audiences
IdpStarterProperties.clientIds
IdpGatewayAdapterProperties.audiences
IdpGatewayAdapterProperties.clientIds
identity_client_audience entity/repository
FileServiceAuthorizationSupplier
RBAC3 service permission principal/annotation
unauthenticated DDC register or heartbeat
~~~

Historical migration/spec text may retain terms when describing history. Active Java, YAML, API examples, and generated contracts may not.

- [x] **Step 4: Run residual scans**

~~~bash
rg -n 'RequestParam\\(\"audience\"\\)|getAudiences\\(|setAudiences\\(|clientIds|identity_client_audience|FileServiceAuthorizationSupplier|CurrentRbac3ServicePrincipal|RequiresRbac3ServicePermission' \
  egon-cola-platforms egon-cola-components \
  --glob '!**/target/**' \
  --glob '!**/db/migration/V1__*' \
  --glob '!docs/superpowers/specs/**'

rg -n 'new DdcServiceRegistration\\(' \
  egon-cola-platforms egon-cola-components \
  --glob '!**/target/**'
~~~

The first scan must have no active result. Review every constructor result from the second scan and prove that it supplies an Admission Ticket or receives one through the registration boundary.

- [x] **Step 5: Run targeted reactor verification**

~~~bash
./mvnw -B -ntp \
  -pl :egon-cola-platform-idp-core,:egon-cola-platform-idp-admin,:egon-cola-platform-idp-starter,:egon-cola-platform-idp-gateway-adapter,:egon-cola-platform-rbac3-admin,:egon-cola-platform-dynamic-config-center-starter,:egon-cola-platform-dynamic-config-center-http-registration-starter,:egon-cola-platform-dynamic-config-center-admin,:egon-cola-component-rpc-ddc-adapter,:egon-cola-platform-gateway-engine,:egon-cola-platform-gateway-admin \
  -am test
~~~

Verification result (2026-08-11): the focused cross-module acceptance suite passed,
Gateway Admin passed 169 tests, IdP Admin passed 94 tests, and the changed DDC/RBAC3
fixtures passed their focused suites. The combined default reactor remains red only in
three pre-existing RBAC3 Gateway discovery tests because
`Map<String, Object>` cannot be converted to a complete Gateway schema. This unrelated
failure is recorded in the spec and is not included in the OAuth2 implementation scope.

- [x] **Step 6: Typecheck and test the mechanically affected admin clients**

~~~bash
npm --prefix egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web test
npm --prefix egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web test
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web test
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web run typecheck
npm --prefix egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web test
~~~

- [x] **Step 7: Verify migration immutability**

~~~bash
git diff --name-only HEAD~15..HEAD -- \
  '*/src/main/resources/db/**/V*.sql'
~~~

Review the output manually. Only IdP V2, DDC V8 for PostgreSQL/SQLite, and Gateway V11 may be new. No pre-existing migration may be modified, renamed, or deleted.

- [x] **Step 8: Update documentation and spec status**

Document:

- the Resource URI convention and exact triple;
- administrator provisioning order;
- owner-only key generation/rotation;
- USER versus SERVICE token claim examples with secret values omitted;
- IdP Service Grant scope ownership;
- DDC admission readiness/failure behavior;
- Resource disable/recovery behavior;
- migration order and rollback limitations.

Mark the spec implemented only after all verification passes. Do not start services.

- [x] **Step 9: Commit**

~~~bash
git status --short
git add \
  docs/superpowers/specs/2026-08-10-oauth2-resource-server-admission-design.md \
  docs/superpowers/plans/2026-08-10-oauth2-resource-server-admission.md \
  egon-cola-platforms/egon-cola-platform-idp/README.md \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/main/resources/application.yml \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin/src/test/java/top/egon/cola/platform/idp/admin/oauth/controller/OAuthResourceSecurityMatrixIT.java \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/auth/AuthContext.tsx \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/api/types.ts \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-admin-web/src/features/clients/ClientListPage.tsx \
  egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter/src/test/java/top/egon/cola/platform/idp/gateway/security/IdpGatewayResourceBindingTest.java \
  egon-cola-platforms/egon-cola-platform-rbac3/README.md \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application.yml \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/main/resources/application-local.yml \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin/src/test/java/top/egon/cola/platform/rbac3/admin/integration/Rbac3UserResourceAuthorizationIT.java \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/auth/oauthClient.ts \
  egon-cola-platforms/egon-cola-platform-rbac3/egon-cola-platform-rbac3-admin-web/src/features/auth/oauthClient.test.ts \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/README.md \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin/src/main/resources/application.yml \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-admin-web/src/auth/AuthContext.tsx \
  egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-test/src/test/java/top/egon/cola/component/ddc/test/DdcResourceAdmissionLifecycleTest.java \
  egon-cola-platforms/egon-cola-platform-gateway/README.md \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/application.yml \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/auth/AuthContext.tsx \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-idp-backend/src/main/resources/application.yml
git diff --cached --name-only
git commit -m "test(idp): verify resource admission flows"
~~~

Before committing, compare git diff --cached --name-only with the Task 15 file
list. Do not stage egon-cola-components in this task because Task 15 has no
component-owned file.

---

## Completion Checklist

- [x] Resource Server uniqueness and trust boundary is bizCode + appCode + env, with one Resource URI per triple.
- [x] Batch management expands explicit appCodes and stores no wildcard.
- [x] IdP stores public keys only and authenticates private_key_jwt with replay protection.
- [x] USER authorization uses resource, one aud, and RBAC3 entry decision at authorize/exchange/refresh.
- [x] USER JWT contains identity/resource state only, not roles or permissions.
- [x] Client Credentials uses IdP Service Grants for exact target, tenant, and scope and returns no refresh token.
- [x] SERVICE token issuance and request authorization make no RBAC3 permission query.
- [x] Admission Ticket is a distinct JWT and all DDC register/heartbeat paths require it.
- [x] DDC lease expiry never exceeds Ticket expiry and audit state contains no raw credential.
- [x] Resource disable reliably revokes only the matching triple.
- [x] Gateway resolves Resource from trusted route identity and validates USER/SERVICE tokens without business authorization.
- [x] Downstream Starter validates the exact Resource; USER continues into RBAC3 and SERVICE uses local scope checks.
- [x] Legacy audience/static-client/static-service-permission paths are absent from active code/config.
- [x] New Java code and packages have complete Chinese/English documentation.
- [x] Targeted acceptance and affected-module reactor tests pass; known unrelated suite failures are documented.
- [x] No existing Flyway migration changed.
- [x] No application process was started.
