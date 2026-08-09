# DDC Starter Package Consolidation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reorganize DDC Starter by domain, share HTTP and Redis infrastructure, remove orphan code, and migrate every repository consumer without compatibility shims.

**Architecture:** Configuration, registry, and management remain separate domain facades. Shared HTTP request construction, TLS transport creation, one RedissonClient, and a generic Redis Topic subscription handle live under `transport`; configuration and registry retain their own validation, lifecycle, reconciliation, and error semantics.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Redisson 3.26.0, Maven, JUnit 5, Mockito, AssertJ.

## Global Constraints

- Work in the current clean `main` checkout explicitly authorized by the user.
- Commit every task separately and stage only task-owned paths.
- Breaking package and class renames are allowed; do not add compatibility wrappers.
- Keep DDC Starter as the business-side dependency; do not add Maven modules or dependencies.
- Do not modify database schemas, data, or existing Flyway migrations.
- Do not start applications, browsers, Redis, PostgreSQL, or other runtime services.
- Preserve Spring Boot ConfigData/YAML-only semantics and independent `registry.enabled` behavior.

---

### Task 1: Record the approved design and implementation plan

**Files:**

- Create: `docs/superpowers/specs/2026-08-09-ddc-starter-package-consolidation-design.md`
- Create: `docs/superpowers/plans/2026-08-09-ddc-starter-package-consolidation.md`

**Interfaces:**

- Consumes: the user-approved package tree and breaking-change authorization.
- Produces: the fixed migration scope and verification gates for Tasks 2-6.

- [ ] **Step 1:** Write the approved design with explicit client, Redis, subscription, package, compatibility, and non-goal boundaries.
- [ ] **Step 2:** Write this task-by-task plan with exact paths, test commands, and commit boundaries.
- [ ] **Step 3:** Run `rg -n "T[B]D|T[O]DO|implement[ ]later|fill[ ]in[ ]details" docs/superpowers/specs/2026-08-09-ddc-starter-package-consolidation-design.md docs/superpowers/plans/2026-08-09-ddc-starter-package-consolidation.md` and require no output.
- [ ] **Step 4:** Run `git diff --check -- docs/superpowers/specs/2026-08-09-ddc-starter-package-consolidation-design.md docs/superpowers/plans/2026-08-09-ddc-starter-package-consolidation.md`.
- [ ] **Step 5:** Commit as `docs(ddc): design starter package consolidation`.

### Task 2: Share signed HTTP request infrastructure

**Files:**

- Create: `.../src/main/java/top/egon/cola/component/ddc/transport/http/DdcOpenApiRequestFactory.java`
- Move: `management/client/DdcRestClientFactory.java` to `transport/http/DdcRestClientFactory.java`
- Move: `management/client/DdcClientTransportSecurity.java` to `transport/http/DdcClientTransportSecurity.java`
- Move: `model/security/DdcCanonicalRequest.java` and `DdcRequestSigner.java` to `transport/http/`
- Modify: `client/HttpDdcAdminClient.java`
- Modify: `registry/DdcOpenApiServiceRegistryClient.java`
- Modify: `management/client/HttpDdcManagementClient.java` and request-factory code
- Test: focused Starter HTTP client and request factory tests

**Interfaces:**

- Consumes: `DdcProperties.Admin`, management client properties, `RestClient`, `ObjectMapper`.
- Produces: `DdcOpenApiRequestFactory.create(HttpMethod, String, Map<String,List<String>>, Object)` returning a signed/unsigned immutable request with target URI, headers, body, and `hasBody`.

- [ ] **Step 1:** Add a failing `DdcOpenApiRequestFactoryTest` proving canonical query ordering, optional unsigned mode, HMAC headers, Trace headers, defensive body copies, and serialization failure mapping.
- [ ] **Step 2:** Run `./mvnw -B -ntp -pl egon-cola-platforms/egon-cola-platform-dynamic-config-center/egon-cola-platform-dynamic-config-center-starter -am -Dtest=DdcOpenApiRequestFactoryTest -Dsurefire.failIfNoSpecifiedTests=false test` and confirm compilation/test failure because the new type is absent.
- [ ] **Step 3:** Implement the minimal shared request factory and move the shared transport/security types.
- [ ] **Step 4:** Refactor all three HTTP clients to consume the factory while retaining their endpoint and domain-error mapping.
- [ ] **Step 5:** Run focused request/client tests, then all Starter tests.
- [ ] **Step 6:** Run a residual scan for old `management.client.DdcRestClientFactory`, `management.client.DdcClientTransportSecurity`, and `model.security` imports.
- [ ] **Step 7:** Commit as `refactor(ddc): share openapi transport infrastructure`.

### Task 3: Share the Redis client and Topic subscription handle

**Files:**

- Create: `transport/redis/DdcRedisTopicSubscription.java`
- Move: `config/DdcRedisTopology.java` to `transport/redis/DdcRedisTopology.java`
- Move: `common/DdcKeys.java` to `transport/redis/DdcRedisKeys.java`
- Modify: `config/DdcAutoConfig.java` and `DdcRegistryAutoConfig.java`
- Modify: configuration and registry subscription code
- Delete: `repository/DdcRedisConfigRepository.java` and its obsolete test
- Test: Redis Topic subscription and both auto-configuration test suites

**Interfaces:**

- Consumes: `List<RTopic>`, message class, Redisson `MessageListener<T>`.
- Produces: `DdcRedisTopicSubscription<T>` with constructor registration, rollback, `isActive()`, and idempotent `close()`.

- [ ] **Step 1:** Replace the old configuration subscription test with a failing generic subscription test covering successful registration, partial rollback, active state, and idempotent close.
- [ ] **Step 2:** Add a failing auto-configuration test proving configuration and registry consumers receive the same `ddcRedissonClient` instance and registry-only mode still creates it.
- [ ] **Step 3:** Run the focused tests and confirm expected failures against the existing two-client implementation.
- [ ] **Step 4:** Implement the generic handle, one shared Redisson bean, and independent activation conditions.
- [ ] **Step 5:** Migrate DDC Redis keys/topology, remove the unused Redis configuration repository, and update Admin callers of the key utility.
- [ ] **Step 6:** Run focused Starter/Admin Redis tests and all Starter tests.
- [ ] **Step 7:** Scan for `ddcRegistryRedissonClient`, `DdcRedisChangeSubscription`, `DdcRedisConfigRepository`, and `common.DdcKeys`; require no production references.
- [ ] **Step 8:** Commit as `refactor(ddc): share redis transport infrastructure`.

### Task 4: Split registry implementation internals

**Files:**

- Move: `registry/DdcOpenApiServiceRegistryClient.java` to `registry/client/HttpDdcServiceRegistryClient.java`
- Move: `registry/DdcActiveRegistrationIndex.java` to `registry/state/`
- Create: `registry/subscription/DdcRegistrySnapshotLoader.java`
- Create: `registry/subscription/DdcRegistrySubscriptionCoordinator.java`
- Create: `registry/subscription/DdcManagedRegistrySubscription.java`
- Create: `registry/subscription/DdcInstanceSubscription.java`
- Create: `registry/subscription/DdcCatalogSubscription.java`
- Delete: `registry/DdcRegistrySubscriptionManager.java`
- Modify: registry auto-configuration and focused registry tests

**Interfaces:**

- Consumes: the unchanged public `DdcServiceRegistryClient`, `DdcRegistrySubscription`, registry models, shared HTTP request factory, and shared Redis Topic handle.
- Produces: a public facade whose HTTP query adapter implements `DdcRegistrySnapshotLoader`; subscriptions depend on that read-only port rather than on the facade containing themselves.

- [ ] **Step 1:** Move/refactor tests first so they target the proposed coordinator, read-only loader, instance subscription, and catalog subscription; run them and confirm compile failure.
- [ ] **Step 2:** Extract the loader port and managed subscription lifecycle without changing event filtering, refresh coalescing, periodic reconciliation, listener isolation, or local-expiry behavior.
- [ ] **Step 3:** Rename the HTTP implementation and compose registration state plus subscription coordinator.
- [ ] **Step 4:** Run all registry and downstream RPC/Gateway focused tests.
- [ ] **Step 5:** Scan for `DdcOpenApiServiceRegistryClient` and `DdcRegistrySubscriptionManager`; require no references.
- [ ] **Step 6:** Commit as `refactor(ddc): separate registry subscription roles`.

### Task 5: Repackage the configuration domain and shared models

**Files:**

- Move production and test sources from old `bootstrap`, `client`, `environment`, `format`, `listener`, `refresh`, `repository`, `service`, `model`, `common`, `config`, and `trace` packages into the approved target packages.
- Rename: `DdcAdminClient` to `DdcConfigClient`, `HttpDdcAdminClient` to `HttpDdcConfigClient`, `DdcBootstrapClient` to `DdcConfigDataFetcher`, listener/subscription names to configuration-domain names, and `DdcLocalConfigRepository` to `DdcLocalConfigState`.
- Move shared lease values into `lease`, errors into `error`, and tracing into `observability`.
- Modify: `META-INF/spring.factories`, AutoConfiguration imports, all Starter tests, and all repository Java consumers.

**Interfaces:**

- Consumes: behaviorally verified Tasks 2-4.
- Produces: only the approved top-level packages and renamed configuration contracts, with no deprecated forwarding types.

- [ ] **Step 1:** Move the relevant test packages/imports first and run Starter test compilation to confirm failures against old production packages.
- [ ] **Step 2:** Move production sources by domain and update package declarations, imports, Javadocs, Spring resource registrations, component scanning, and bean signatures.
- [ ] **Step 3:** Update Gateway, RPC, IdP, RBAC3, DDC Admin/Test and all test fixtures to the new DDC Starter API.
- [ ] **Step 4:** Run Starter tests, DDC Admin/Test tests, and focused affected downstream tests.
- [ ] **Step 5:** Run repository-wide scans for every removed top-level package and old class name; historical design documents are exempt, active source/resources/README are not.
- [ ] **Step 6:** Commit as `refactor(ddc): organize starter by domain`.

### Task 6: Documentation, architecture checks, and repository verification

**Files:**

- Modify: active DDC/Gateway/RPC/IdP/RBAC3 README or examples containing removed package/class names.
- Modify: `DdcPlatformBoundaryTest` and `DdcManagementContractBoundaryTest` to enforce the new domain and module boundaries behaviorally.

**Interfaces:**

- Consumes: final target package layout.
- Produces: active documentation and regression gates aligned with the breaking API.

- [ ] **Step 1:** Add failing boundary assertions that resolve the new public contracts and reject Admin persistence/Redisson dependencies from management contracts.
- [ ] **Step 2:** Update active documentation and examples; do not rewrite historical specs/plans.
- [ ] **Step 3:** Run targeted DDC Starter/Admin/Test, Gateway, RPC, IdP and RBAC3 Maven verification without starting processes.
- [ ] **Step 4:** Run `git diff --check`, `git status --short`, active-reference residual scans, and inspect every commit/path against the original request.
- [ ] **Step 5:** Commit as `docs(ddc): document domain package layout`.

## Completion Gate

- [ ] Configuration, registry, and management remain separate domain facades.
- [ ] HTTP signing/TLS/request creation is shared by all DDC HTTP clients.
- [ ] One RedissonClient serves configuration and registry while feature switches remain independent.
- [ ] Configuration and registry subscriptions share a generic Topic handle but not business logic.
- [ ] Registry subscription responsibilities are split and no longer depend on a self-containing facade.
- [ ] `DdcRedisConfigRepository`, old packages, old names, and compatibility shells are absent.
- [ ] All repository consumers compile against the new DDC Starter API.
- [ ] Required Maven tests/builds and residual scans pass with no project process started.
