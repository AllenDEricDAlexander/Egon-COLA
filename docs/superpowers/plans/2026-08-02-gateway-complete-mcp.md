# Egon Gateway Complete MCP Implementation Plan

> 本文保留为初始实施记录，不再代表当前本地 Tool 注册方式。手工本地 Tool
> Draft 和 disabled Route 锚点已由
> [Gateway 注解托管 MCP 设计与破坏性迁移](../specs/2026-08-06-gateway-annotation-managed-mcp-design.md)
> 取代。

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. This plan is inline-only; do not create or dispatch subagents.

**Goal:** Build the complete MCP control plane, runtime, Admin Web, federation, security, release integration, and multi-process validation on top of the existing Egon Gateway.

**Architecture:** Add a dependency-light `gateway-mcp-runtime` between Gateway Core and Engine. MCP configuration is nested in the existing canonical Gateway Rule so HTTP/RPC and MCP compile, persist to LKG, and activate atomically through the current DDC key. Gateway performs base IdP identity validation; the MCP runtime acts as a downstream application and pulls authorization snapshots from RBAC3.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Reactor/Netty, Jackson, PostgreSQL 18/Flyway, Redis Streams, Micrometer/OpenTelemetry, Resilience4j, React 19, TypeScript 6, Ant Design 6, TanStack Query, Vitest, Playwright, official MCP Java SDK 2.0.0 compatibility fixtures, MCP conformance CLI.

## Global Constraints

- Execute inline in `/Users/mario/SelfProject/Egon-COLA`; never create a subagent or worktree.
- Complete the approved unified identity plan before Task 7; do not create a second authentication system.
- Base Gateway validation is limited to IdP JWT/user state/tokenVersion; MCP authorization is a downstream RBAC3 concern.
- Do not add roles or permissions to the IdP access token.
- Never modify Flyway V1-V6. Add exactly one Gateway migration: `V7__add_gateway_mcp_control_plane.sql`.
- Do not call Gateway's public HTTP route from an MCP handler; invoke operations through `GatewayOperationInvoker`.
- The existing `gateway.rules.active` key remains the only active pointer.
- A release either activates HTTP/RPC and MCP together or leaves the previous release active.
- `2025-11-25` is the stable baseline; `2026-07-28` remains explicitly marked RC until the official release source changes.
- Never forward an inbound token to a remote MCP provider. Local business providers receive the original JWT and authorize independently.
- No default password, inline secret, arbitrary external `$ref`, arbitrary file path, provider URL, or command execution.
- Every backend task uses TDD and ends with the exact Maven command stated in that task; every frontend task ends with lint, typecheck, unit tests, and build for the changed surface.
- Maven/static/H2 evidence is not real PostgreSQL/Redis/DDC/multi-process proof; Task 19 supplies that evidence.
- Do not start long-lived project processes until Task 19 has passed all prior gates.

---

### Task 1: MCP Reactor Module and Wire-Neutral Contracts

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/pom.xml`
- Modify: `egon-cola-platforms/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/protocol/McpProtocolDialect.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/protocol/McpJsonRpcRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/protocol/McpJsonRpcResponse.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/protocol/McpJsonRpcError.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/protocol/McpErrorCode.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuleContent.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeServer.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeTool.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeResource.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeResourceTemplate.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimePrompt.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeTaskPolicy.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeApp.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeRemoteProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/mcp/rule/McpRuntimeRemoteMount.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/mcp/McpContractTest.java`

**Interfaces:**
- Consumes: existing `GatewayRuntimeOperation.operationId()` and stable Jackson-compatible contract conventions.
- Produces: immutable sorted `McpRuleContent.empty()` and all runtime records used by Tasks 2-19.

- [ ] **Step 1: Write the failing contract test**

```java
@Test
void ruleContentIsDeterministicAndRejectsDuplicateCapabilityNames() {
    McpRuleContent content = fixturesWithTools("invoice.get", "invoice.get");
    assertThrows(IllegalArgumentException.class, content::validate);
    assertEquals(List.of(), McpRuleContent.empty().servers());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpContractTest test`

Expected: test compilation fails because MCP contracts do not exist.

- [ ] **Step 3: Add the module and minimal records**

```java
public enum McpProtocolDialect {
    STABLE_2025_11_25,
    RC_2026_07_28,
    LEGACY_2024_SSE
}

public record McpRuleContent(
        List<McpRuntimeServer> servers,
        List<McpRuntimeTool> tools,
        List<McpRuntimeResource> resources,
        List<McpRuntimeResourceTemplate> resourceTemplates,
        List<McpRuntimePrompt> prompts,
        List<McpRuntimeTaskPolicy> taskPolicies,
        List<McpRuntimeApp> apps,
        List<McpRuntimeRemoteProvider> remoteProviders,
        List<McpRuntimeRemoteMount> remoteMounts
) {
    public static McpRuleContent empty() {
        return new McpRuleContent(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(), List.of()
        );
    }

    public void validate() {
        McpRuleValidator.validateServers(servers);
        McpRuleValidator.validateCapabilityNames(
                tools, resources, resourceTemplates, prompts, apps
        );
        McpRuleValidator.validateRemoteNamespaces(
                remoteProviders, remoteMounts
        );
    }
}
```

Keep records free of Spring, SDK, JPA, or Reactor types. Normalize ordering in compact constructors.

- [ ] **Step 4: Run GREEN and module install**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime -am -DskipITs test`

Expected: contract tests pass and the empty runtime jar builds.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/pom.xml egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add MCP runtime contracts"
```

### Task 2: JSON-RPC Codec, Dialect Adapters, and Dispatcher

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/protocol/McpJsonRpcCodec.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/protocol/McpDialectAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/protocol/StableMcpDialectAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/protocol/RcMcpDialectAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/protocol/LegacySseMcpAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/McpRequestContext.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/McpMethodHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/McpMethodDispatcher.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/handler/McpDiscoverHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/handler/McpInitializeHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/handler/McpInitializedHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/server/handler/McpPingHandler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/protocol/McpDialectCompatibilityTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/server/McpMethodDispatcherTest.java`

**Interfaces:**
- Consumes: `McpJsonRpcRequest`, `McpProtocolDialect`, `McpRuntimeServer`.
- Produces: `McpDialectAdapter.decode(HttpMcpRequest)` and `McpMethodDispatcher.dispatch(request, context)`.

- [ ] **Step 1: Write failing dialect tests**

```java
@Test
void rcRequiresPerRequestMetadataAndHeaderBodyAgreement() {
    HttpMcpRequest request = rcRequest("tools/call", "tools/list", null);
    McpProtocolException error = assertThrows(
            McpProtocolException.class,
            () -> rc.decode(request)
    );
    assertEquals(McpErrorCode.MCP_HEADER_MISMATCH, error.code());
}

@Test
void stableInitializeAndRcDiscoverNormalizeToTheSameServerDescription() {
    assertEquals(
            stableDescription(stable.handle(initializeRequest())),
            rcDescription(rc.handle(discoverRequest()))
    );
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpDialectCompatibilityTest,McpMethodDispatcherTest test`

Expected: adapter and dispatcher symbols are missing.

- [ ] **Step 3: Implement protocol validation and handler registry**

```java
public interface McpMethodHandler {
    String method();
    Publisher<McpJsonRpcResponse> handle(
            McpJsonRpcRequest request,
            McpRequestContext context
    );
}

public final class McpMethodDispatcher {
    private final Map<String, McpMethodHandler> handlers;
    public Publisher<McpJsonRpcResponse> dispatch(
            McpJsonRpcRequest request,
            McpRequestContext context
    ) {
        McpMethodHandler handler = handlers.get(request.method());
        if (handler == null) {
            return Mono.just(McpJsonRpcResponse.methodNotFound(request.id()));
        }
        return Flux.from(handler.handle(request, context))
                .onErrorResume(McpProtocolException.class,
                        error -> Mono.just(error.toResponse(request.id())));
    }
}
```

Reject batch input, JSON depth above 64, missing JSON-RPC version, invalid id, unsupported version, RC requests without per-request metadata, and `Mcp-Method`/`Mcp-Name` mismatches.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpDialectCompatibilityTest,McpMethodDispatcherTest test`

Expected: dialect and dispatcher tests pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime
git commit -m "feat(gateway): add MCP protocol adapters"
```

### Task 3: Nest MCP in the Existing Rule, Compiler, DDC Activation, and LKG

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/rule/GatewayRuleContent.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/rule/GatewayRuleCanonicalizer.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/rule/GatewayRuleCompiler.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/EngineGatewayRuleCompiler.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/CompiledGatewayRules.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/GatewayRuleJsonCodec.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/GatewayRuleActivationApplier.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/rule/CompiledMcpRules.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/rule/McpRuleCompiler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/rule/GatewayMcpRuleCompilerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rule/GatewayMcpActivationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/rule/GatewayLegacySnapshotCompatibilityTest.java`

**Interfaces:**
- Consumes: `McpRuleContent.empty()` from Task 1.
- Produces: `CompiledGatewayRules.mcpRules()` and one atomic activation for both rule families.

- [ ] **Step 1: Write failing atomicity and compatibility tests**

```java
@Test
void invalidMcpContentLeavesPreviousHttpAndMcpReleaseActive() {
    applier.apply(ACTIVE_CONFIG_KEY, validRelease, 1L);
    CompiledGatewayRules before = applier.active();
    assertThrows(RuntimeException.class,
            () -> applier.apply(ACTIVE_CONFIG_KEY, invalidMcpRelease, 2L));
    assertSame(before, applier.active());
}

@Test
void snapshotWithoutMcpFieldLoadsAsEmptyMcpRules() {
    assertTrue(codec.readSnapshot(legacyJson).content().mcp().servers().isEmpty());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GatewayMcpRuleCompilerTest,GatewayMcpActivationTest,GatewayLegacySnapshotCompatibilityTest test`

Expected: `mcp()` and compiled MCP rules do not exist.

- [ ] **Step 3: Extend the rule with a compatibility constructor**

```java
public record GatewayRuleContent(
        String gatewayGroupId,
        String gatewayGroupCode,
        String env,
        String namespace,
        List<GatewayRuntimeOperation> operations,
        List<GatewayRuntimeRoute> routes,
        List<GatewayRuntimePolicy> providerPolicies,
        List<GatewayRuntimePolicy> trafficPolicies,
        List<GatewayRuntimePolicy> securityPolicies,
        List<GatewayRuntimePolicy> corsPolicies,
        List<GatewayRpcDescriptor> rpcDescriptors,
        McpRuleContent mcp
) {
    public GatewayRuleContent {
        mcp = mcp == null ? McpRuleContent.empty() : mcp;
    }
    public GatewayRuleContent(
            String gatewayGroupId,
            String gatewayGroupCode,
            String env,
            String namespace,
            List<GatewayRuntimeOperation> operations,
            List<GatewayRuntimeRoute> routes,
            List<GatewayRuntimePolicy> providerPolicies,
            List<GatewayRuntimePolicy> trafficPolicies,
            List<GatewayRuntimePolicy> securityPolicies,
            List<GatewayRuntimePolicy> corsPolicies,
            List<GatewayRpcDescriptor> rpcDescriptors
    ) {
        this(
                gatewayGroupId, gatewayGroupCode, env, namespace,
                operations, routes, providerPolicies, trafficPolicies,
                securityPolicies, corsPolicies, rpcDescriptors,
                McpRuleContent.empty()
        );
    }
}
```

Compile MCP before resource preparation. Only after HTTP/RPC and MCP compile, artifacts validate, LKG persists, and providers prepare may the existing `AtomicReference<CompiledGatewayRules>` change.

- [ ] **Step 4: Run GREEN plus existing release tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs test`

Expected: all existing Gateway release tests and new MCP tests pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): activate MCP in unified releases"
```

### Task 4: V7 PostgreSQL Control Plane, Task, Approval, and Artifact Metadata

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/resources/db/migration/V7__add_gateway_mcp_control_plane.sql`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/McpServerEntity.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/McpServerRepository.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpCapabilityDraftStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpRemoteProviderStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpArtifactMetadataStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpApprovalStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpTaskStore.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/persistence/GatewayMcpFlywayPostgresqlIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/persistence/JdbcMcpControlPlaneStoreTest.java`

**Interfaces:**
- Consumes: `LongIdGenerator`, Jackson and existing Admin actor/revision conventions.
- Produces: stores for Task 5 and shared task/approval records for Tasks 7/11.

- [ ] **Step 1: Write failing migration tests**

```java
@Test
void v7CreatesAllMcpTablesAndCanUpgradeV1ThroughV6() {
    migrateToSix();
    migrateToLatest();
    assertThat(tableNames()).contains(
            "gateway_mcp_server",
            "gateway_mcp_tool_draft",
            "gateway_mcp_task_instance",
            "gateway_mcp_approval"
    );
}
```

- [ ] **Step 2: Run RED on an isolated PostgreSQL instance**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GatewayMcpFlywayPostgresqlIT test`

Expected: V7 and MCP tables are absent.

- [ ] **Step 3: Add exactly one V7 migration and stores**

Create all 13 tables listed in the spec, with JSONB, revision, soft-delete, owner/expiry/status indexes, enum checks, unique capability names and one-time approval digest. Do not edit V1-V6.

```java
public interface McpCapabilityDraftStore {
    McpCapabilityDraft load(String gatewayGroupId);
    McpDraftMutation saveTool(
            McpToolDraft tool,
            long expectedRevision,
            GatewayAdminActor actor
    );
}
```

- [ ] **Step 4: Run GREEN on empty and upgrade databases**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=GatewayMcpFlywayPostgresqlIT test`, then `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs test`.

Expected: empty migration, V1-V6 upgrade and store concurrency tests pass.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git commit -m "feat(gateway): add MCP control plane schema"
```

### Task 5: MCP Admin Domain, Validation, CRUD, Preview, and Unified Release

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/application/McpControlPlaneService.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/application/McpValidationService.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/application/McpReleaseContentFactory.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpServerController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpCapabilityController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpTaskAdminController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpAppAdminController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpRemoteProviderController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpProtocolInspectorController.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/application/release/GatewayReleaseService.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpAdminApiIT.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/application/McpUnifiedReleaseTest.java`

**Interfaces:**
- Consumes: Task 4 stores and Task 3 unified rule compiler.
- Produces: `/api/v1/gateway/admin/mcp/**` and MCP content in normal Gateway preview/publish/rollback.

- [ ] **Step 1: Write failing API and release tests**

```java
@Test
void publishingGatewayReleaseIncludesMcpDraftAndRejectsUnknownOperation() {
    createServer();
    bindTool("missing-operation");
    mockMvc.perform(post(releasePreviewUrl))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code")
                    .value("GATEWAY_MCP_OPERATION_NOT_FOUND"));
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpAdminApiIT,McpUnifiedReleaseTest test`

Expected: controllers/services are missing.

- [ ] **Step 3: Implement CRUD and validation facade**

Validation must resolve Operation IDs from Catalog, compile schemas, verify immutable Artifact SHA, validate Remote fingerprints, URI templates, namespace conflicts, task policies and permission names. All writes require Idempotency-Key and expectedRevision and emit existing Gateway audit records.

```java
public McpRuleContent compileForRelease(
        String gatewayGroupId,
        long expectedDraftRevision
) {
    McpCapabilityDraft draft = draftStore.load(gatewayGroupId);
    draft.requireRevision(expectedDraftRevision);
    validationService.validate(draft);
    return canonicalizer.canonicalize(contentFactory.create(draft));
}
```

- [ ] **Step 4: Run GREEN and existing Admin tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin
git commit -m "feat(gateway): add MCP management APIs"
```

### Task 6: Direct Operation Invoker and Local Tools

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/invocation/GatewayOperationInvoker.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/invocation/GatewayOperationInvocation.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/invocation/GatewayInvocationResult.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/invocation/EngineGatewayOperationInvoker.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/tool/McpToolCatalog.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/tool/McpArgumentBinder.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/tool/McpResultBinder.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/tool/McpToolsListHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/tool/McpToolsCallHandler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/invocation/EngineGatewayOperationInvokerTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/tool/McpLocalToolFlowTest.java`

**Interfaces:**
- Consumes: active `GatewayRuntimeOperation`, provider directory and governance services.
- Produces: local HTTP/RPC Tool calls with no loopback request.

- [ ] **Step 1: Write failing no-loopback Tool tests**

```java
@Test
void toolCallUsesOperationInvokerAndNeverAcceptsProviderCoordinates() {
    handler.call(requestWithArguments(Map.of("providerUrl", "http://evil")));
    assertEquals("operation-42", invoker.singleInvocation().operationId());
    assertFalse(invoker.singleInvocation().arguments()
            .containsKey("providerUrl"));
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpLocalToolFlowTest,EngineGatewayOperationInvokerTest test`

- [ ] **Step 3: Implement the invoker adapter and Tool handlers**

Reuse security context, provider selection, rate/concurrency, timeout, retry/circuit/bulkhead, upstream adapters, response size and telemetry. Forward only configured argument mappings and the original local bearer credential.

- [ ] **Step 4: Run GREEN plus HTTP/RPC regression suites**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpLocalToolFlowTest,EngineGatewayOperationInvokerTest test`, then `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): invoke local operations as MCP tools"
```

### Task 7: IdP Identity, RBAC3 Snapshot Authorization, and One-Time Approval

**Prerequisite:** Unified identity plan Tasks 4-11 are complete and the downstream authorization starter exists.

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/security/McpAuthorizationPort.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/security/McpAuthorizationRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/security/McpApprovalPort.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/security/Rbac3McpAuthorizationAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/security/JdbcMcpApprovalAdapter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/security/McpSecurityGate.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/interfaces/McpApprovalController.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/security/McpSecurityGateTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/security/McpRbac3IntegrationTest.java`

**Interfaces:**
- Consumes: IdP principal `(iss, sub, tid, sid, clientId, tokenVersion)` and RBAC3 downstream snapshot cache.
- Produces: per-primitive authorization and one-time HIGH/CRITICAL approval consumption.

- [ ] **Step 1: Write failing boundary tests**

```java
@Test
void highRiskToolRequiresApprovalBoundToExactRequestDigest() {
    assertDenied(callWithoutApproval(), "MCP_APPROVAL_REQUIRED");
    String token = approvals.issue(subject, tool, digestA);
    assertDenied(callWith(token, digestB), "MCP_APPROVAL_MISMATCH");
    assertAllowed(callWith(token, digestA));
    assertDenied(callWith(token, digestA), "MCP_APPROVAL_CONSUMED");
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpSecurityGateTest,McpRbac3IntegrationTest test`

- [ ] **Step 3: Implement downstream authorization**

Map operations to exact permission keys such as `mcp:{serverCode}:tool:{toolName}:call`; fetch/refresh RBAC3 snapshots with the approved starter; compare snapshot/fencing versions; hash approval tokens at rest; bind approval to subject, tenant, client, server, tool and canonical argument digest.

- [ ] **Step 4: Run GREEN and prove Gateway base auth makes no RBAC call**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-idp/egon-cola-platform-idp-gateway-adapter -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpSecurityGateTest,McpRbac3IntegrationTest,IdpGatewaySecurityProviderTest,GatewayIdentityOnlySecurityTest,GatewayOriginalBearerForwardingTest test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): authorize MCP through RBAC3"
```

### Task 8: Reactor Netty MCP Ingress, Stable Session, RC Stateless Requests, and Legacy SSE

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/transport/McpHttpRequest.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/transport/McpHttpResponse.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/transport/McpSessionStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/transport/McpSubscriptionEventStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/RedisMcpSessionStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/McpEngineHttpHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/GatewayCompositeHttpDataPlaneHandler.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpTransportIntegrationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpCrossNodeSessionTest.java`

**Interfaces:**
- Consumes: Task 2 adapters/dispatcher and existing Gateway listener request/response types.
- Produces: all public MCP endpoints and Redis-backed cross-node Stable/Legacy streams.

- [ ] **Step 1: Write failing transport tests**

```java
@Test
void stablePostOnNodeBDeliversEventToGetStreamOnNodeA() {
    String session = nodeA.initializeStable();
    nodeB.post(session, pingRequest());
    assertEquals("pong", nodeA.nextEvent(session).resultText());
}

@Test
void rcCallNeedsNoSessionAndUsesPerRequestMeta() {
    assertEquals(200, nodeB.postRc(discoverRequest()).status());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTransportIntegrationTest,McpCrossNodeSessionTest test`

- [ ] **Step 3: Implement path routing and Redis stream state**

Route fixed `/mcp/`, `/legacy/mcp/`, protected-resource metadata and internal App/health paths before normal HTTP route matching. Keep request drain/cancellation behavior. Redis keys use configured prefix, expiry, bounded stream length and no token/body values.

- [ ] **Step 4: Run GREEN and existing listener/drain tests**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTransportIntegrationTest,McpCrossNodeSessionTest test`, then `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): expose MCP transport endpoints"
```

### Task 9: Resources, Templates, and Cross-Node Subscriptions

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourceCatalog.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourceUriValidator.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/StaticTextResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/StaticBlobResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/OperationResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/ObjectStorageResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/DatabaseSchemaResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourcesListHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourcesReadHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/resource/McpResourceTemplatesListHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/subscription/McpSubscriptionService.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/subscription/McpResourceSubscribeHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/subscription/McpSubscriptionsListenHandler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/resource/McpResourceFlowTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpSubscriptionHaTest.java`

**Interfaces:**
- Consumes: `GatewayOperationInvoker`, artifact/content ports, Redis event store.
- Produces: all local resource methods and Stable/RC subscription translation.

- [ ] **Step 1: Write failing URI/security/subscription tests**

```java
@Test
void resourceUriCannotSelectNetworkOrEscapeStorageRoot() {
    assertRejected("https://169.254.169.254/latest/meta-data");
    assertRejected("egon://finance/../../secret");
}

@Test
void stableUpdateIsVisibleThroughRcListenOnAnotherNode() {
    stable.subscribe(uri);
    events.publish(uri);
    assertEquals(uri, rc.listen().next().uri());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpResourceFlowTest,McpSubscriptionHaTest test`

- [ ] **Step 3: Implement driver registry and bounded shared events**

Each driver validates permissions, URI, MIME and size before reading. Object storage resolves against a configured real root and rejects symlink escape. Database schema reads allowlisted schemas only. Translate list_changed/updated to each dialect.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpResourceFlowTest,McpSubscriptionHaTest test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add MCP resources and subscriptions"
```

### Task 10: Prompts and Completion

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/McpPromptDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/StrictPromptTemplate.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/StaticPromptDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/OperationPromptDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/McpPromptsListHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/prompt/McpPromptsGetHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/completion/McpCompletionProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/completion/DictionaryCompletionProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/completion/OperationCompletionProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/completion/McpCompletionHandler.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/prompt/McpPromptFlowTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/completion/McpCompletionTest.java`

**Interfaces:**
- Consumes: resource runtime, operation invoker, active prompt descriptors.
- Produces: prompt list/get, embedded resource checks, completion results limited to 100.

- [ ] **Step 1: Write failing injection and completion tests**

```java
@Test
void strictTemplateDoesNotExecuteSpelOrResolveUndeclaredVariables() {
    assertThrows(McpProtocolException.class,
            () -> template.render("${T(java.lang.Runtime).getRuntime()}", Map.of()));
}

@Test
void completionIsPermissionFilteredStableAndLimited() {
    assertEquals(100, completion.complete(request).values().size());
    assertEquals(sorted, completion.complete(request).values());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpPromptFlowTest,McpCompletionTest test`.

- [ ] **Step 3: Implement strict interpolation and providers**

Only `${declaredArgument}` interpolation is accepted. Embedded resources re-enter resource authorization. Completion providers never return secrets or unauthorized values and enforce time/rate bounds.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpPromptFlowTest,McpCompletionTest test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add MCP prompts and completion"
```

### Task 11: Durable Tasks, Worker Lease, Input, Cancellation, and Recovery

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTask.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTaskStateMachine.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTaskStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTaskExecutor.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTaskService.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTasksGetHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTasksUpdateHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/task/McpTasksCancelHandler.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/JdbcMcpRuntimeTaskStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/McpTaskWorker.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/task/McpTaskStateMachineTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpTaskRecoveryPostgresqlIT.java`

**Interfaces:**
- Consumes: Task 4 table and Tool/Remote executors.
- Produces: Tasks extension with cross-engine durable ownership.

- [ ] **Step 1: Write failing state and recovery tests**

```java
@Test
void onlyDeclaredTransitionsAreAccepted() {
    assertEquals(INPUT_REQUIRED, state.transition(WORKING, REQUEST_INPUT));
    assertThrows(IllegalStateException.class,
            () -> state.transition(COMPLETED, CANCEL));
}

@Test
void taskCreatedOnNodeAIsLeasedAndCompletedByNodeBAfterCrash() {
    String taskId = nodeA.createTask();
    nodeA.crashBeforeCompletion();
    nodeB.runWorkerOnce();
    assertEquals(COMPLETED, nodeB.get(taskId).status());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTaskStateMachineTest,McpTaskRecoveryPostgresqlIT test` against the task's isolated PostgreSQL fixture.

- [ ] **Step 3: Implement durable service**

Persist before response; generate 256-bit random task IDs; enforce subject/tenant/client ownership on every method; use `FOR UPDATE SKIP LOCKED` worker leasing and lease expiry; persist input request/response keys; make cancel cooperative; clean expired terminal results.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs=false -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTaskStateMachineTest,McpTaskRecoveryPostgresqlIT test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add durable MCP tasks"
```

### Task 12: MCP App Artifact Registry and UI Resources

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/app/McpAppArtifactStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/app/McpAppRuntime.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/app/McpAppSecurityValidator.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/app/AppUiResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/mcp/artifact/FileSystemMcpAppArtifactStore.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/FileSystemMcpAppArtifactReader.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/app/McpAppSecurityTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/mcp/artifact/McpArtifactUploadIT.java`

**Interfaces:**
- Consumes: immutable artifact metadata and Resource runtime.
- Produces: upload/validate/read of `text/html;profile=mcp-app` with verified SHA/CSP/permissions.

- [ ] **Step 1: Write failing artifact attacks**

```java
@Test
void artifactVersionCannotBeOverwrittenAndSymlinkEscapeIsRejected() {
    upload("dashboard", "1.0.0", safeHtml);
    assertThrows(ConflictException.class,
            () -> upload("dashboard", "1.0.0", changedHtml));
    assertRejected(symlinkOutsideArtifactRoot());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpAppSecurityTest,McpArtifactUploadIT test`.

- [ ] **Step 3: Implement atomic artifact writes and response metadata**

Write to a temporary file in the configured root, fsync, verify SHA, atomically move, then persist metadata. Reject missing CSP/permissions, oversized files, forbidden origins, script navigation and non-profile MIME. Serve with sandbox/CSP/no-cookie/nosniff headers.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpAppSecurityTest,McpArtifactUploadIT test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add MCP app artifacts"
```

### Task 13: Remote MCP Federation and Dialect Translation

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/remote/RemoteMcpClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-core/src/main/java/top/egon/cola/component/gateway/core/mcp/remote/RemoteAuthProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/McpRemoteClientPool.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/McpCapabilitySynchronizer.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/McpNamespaceRouter.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/McpDialectTranslator.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/RemoteMcpToolDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/RemoteMcpResourceDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/RemoteMcpPromptDriver.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/remote/RemoteMcpCompletionProvider.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/remote/ReactorNettyRemoteMcpClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/remote/ReferenceRemoteAuthProvider.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/test/java/top/egon/cola/component/gateway/mcp/remote/McpFederationTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/remote/RemoteTokenIsolationIT.java`

**Interfaces:**
- Consumes: remote descriptors fixed in Active Release and outbound secret references.
- Produces: mounted remote primitives, health, circuit breaker and Stable/RC translation.

- [ ] **Step 1: Write failing mount/translation/token tests**

```java
@Test
void stableClientCanCallMountedRcToolWithoutInboundTokenLeak() {
    callAsStable("github.create_issue", inboundBearer);
    assertEquals("create_issue", remote.receivedTool());
    assertNotEquals(inboundBearer, remote.authorization());
    assertEquals(remoteCredential, remote.authorization());
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpFederationTest,RemoteTokenIsolationIT test`.

- [ ] **Step 3: Implement sync, namespace and resilient clients**

Discover and persist capability fingerprints; compile only reviewed descriptors into release; translate lifecycle, subscriptions, tasks, apps, trace, pagination and errors; use timeout/bulkhead/circuit; resolve OAuth client credentials, token exchange, secret references and mTLS without storing secrets in rules.

- [ ] **Step 4: Run GREEN with Stable and RC remote fixtures**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpFederationTest,RemoteTokenIsolationIT test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): add MCP remote federation"
```

### Task 14: MCP Telemetry, Audit, Configuration, Health, and Failure Recovery

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-runtime/src/main/java/top/egon/cola/component/gateway/mcp/telemetry/McpTelemetry.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/MicrometerMcpTelemetry.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/McpRuntimeProperties.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/McpRuntimeHealthIndicator.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp/McpAuditPublisher.java`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/resources/application.yml`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/GatewayEngineConfiguration.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpTelemetrySecurityTest.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/mcp/McpLkgRecoveryIT.java`

**Interfaces:**
- Consumes: all runtime events and existing Gateway trace/audit infrastructure.
- Produces: bounded metrics, trace tree, sanitized audit and readiness/degraded status.

- [ ] **Step 1: Write failing sanitization and recovery tests**

```java
@Test
void metricsAndAuditNeverUseSecretsOrHighCardinalityIds() {
    execute(requestWithBearerAndTaskId());
    assertFalse(registry.allTagValues().contains(taskId));
    assertFalse(audit.single().json().contains(bearer));
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTelemetrySecurityTest,McpLkgRecoveryIT test`.

- [ ] **Step 3: Implement telemetry and configuration validation**

Record method/primitive/status/server/remote provider only. Trace child spans for operation/remote/artifact/task. Health exposes active release, protocols, Redis/task/artifact/remote summary without secrets. Restore MCP content from the same LKG and remain on prior Active after failed DDC apply.

- [ ] **Step 4: Run GREEN**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpTelemetrySecurityTest,McpLkgRecoveryIT test`, then `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am -DskipITs test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "feat(gateway): observe and recover MCP runtime"
```

### Task 15: Admin Web Foundation, SSO, MCP Servers, Tools, and Protocol Inspector

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/app/App.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/layouts/AdminLayout.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.ts`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpServersPage.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpServerWorkbenchPage.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpToolsPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpProtocolInspector.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/mcpValidation.ts`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpServersPage.test.tsx`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpToolsPanel.test.tsx`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpProtocolInspector.test.tsx`

**Interfaces:**
- Consumes: Task 5 APIs and unified SSO from the identity plan.
- Produces: usable Server/Tool/Inspector control-plane UI.

- [ ] **Step 1: Write failing UI tests**

```tsx
it('selects an Operation from catalog and never exposes a provider URL field', async () => {
  renderWorkbench()
  await user.click(await screen.findByRole('button', { name: '新增 Tool' }))
  expect(screen.getByLabelText('Operation')).toBeInTheDocument()
  expect(screen.queryByLabelText('Provider URL')).not.toBeInTheDocument()
})
```

- [ ] **Step 2: Run RED**

Run: `npm test -- --run src/features/mcp/McpServersPage.test.tsx src/features/mcp/McpToolsPanel.test.tsx src/features/mcp/McpProtocolInspector.test.tsx` in `gateway-admin-web`.

- [ ] **Step 3: Implement navigation, routes, typed API and forms**

Use Ant Design and existing Query/Scope patterns. Workbench tabs keep per-entity async state isolated; every save sends idempotency and expected revision; Protocol Inspector redacts authorization and supports Stable/RC request templates. Use RBAC capability guards for read/write/test/release/approve.

- [ ] **Step 4: Run GREEN and frontend gates**

Run: `npm run lint && npm run typecheck && npm test -- --run && npm run build`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
git commit -m "feat(gateway-web): manage MCP servers and tools"
```

### Task 16: Admin Web Resources, Prompts, Tasks, Apps, Remote Federation, and Release

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpResourcesPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpPromptsPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpTasksPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpAppsPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRemoteProvidersPage.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRemoteMountsPanel.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCapabilityPreview.tsx`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRuntimeStatus.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/releases/ReleaseDetailPage.tsx`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/app/App.tsx`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpCompleteWorkbench.test.tsx`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/mcp-control-plane.spec.ts`

**Interfaces:**
- Consumes: remaining Task 5 APIs and existing release API.
- Produces: all MCP control plane flows, one unified release preview/publish and sandboxed App preview.

- [ ] **Step 1: Write failing complete-workbench and release tests**

```tsx
it('shows remote capability conflicts and blocks release until resolved', async () => {
  renderRemoteMountWithConflict()
  expect(await screen.findByText('CAPABILITY_NAME_CONFLICT')).toBeVisible()
  expect(screen.getByRole('button', { name: '发布' })).toBeDisabled()
})
```

- [ ] **Step 2: Run RED**

Run from `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`: `npm test -- --run src/features/mcp/McpCompleteWorkbench.test.tsx`.

- [ ] **Step 3: Implement all panels and release integration**

Include URI/template/schema validation, prompt render test, task state views, immutable App upload/security report/sandbox preview, remote discover diff/health/mount conflicts, complete capability preview and runtime consistency. Existing Release Detail renders MCP diff and still publishes one release.

- [ ] **Step 4: Run frontend gates and Playwright against test API**

Run: `npm run lint && npm run typecheck && npm test -- --run && npm run build && npm run e2e -- mcp-control-plane.spec.ts`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
git commit -m "feat(gateway-web): complete MCP control plane"
```

### Task 17: Local HTTP/RPC/Job Providers, Remote MCP Fixtures, Apps Host, and Clients

**Files:**
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-provider/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-provider/src/main/java/top/egon/cola/component/gateway/test/mcp/provider/McpTestProviderApplication.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-provider/src/main/java/top/egon/cola/component/gateway/test/mcp/provider/McpJobController.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/pom.xml`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/src/main/java/top/egon/cola/component/gateway/test/mcp/remote/StableRemoteMcpApplication.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/src/main/java/top/egon/cola/component/gateway/test/mcp/remote/RcRemoteMcpApplication.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-mcp-remote/src/main/resources/apps/test-dashboard.html`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/StableMcpTestClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/RcMcpTestClient.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/LegacyMcpTestClient.java`
- Test: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpFixtureContractTest.java`

**Interfaces:**
- Consumes: existing provider runtime/DDC registration and protocol contracts.
- Produces: deterministic fixtures for every primitive and dialect, with no external model/API dependency.

- [ ] **Step 1: Write failing fixture contract tests**

```java
@Test
void fixturesExposeHttpRpcJobStableRcAndAppCapabilities() {
    assertEquals("HTTP", fixtures.httpOperation().protocol());
    assertEquals("RPC", fixtures.rpcOperation().protocol());
    assertTrue(fixtures.stableRemote().tools().contains("remote_echo"));
    assertTrue(fixtures.rcRemote().apps().contains("remote_dashboard"));
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -DskipITs -Dsurefire.failIfNoSpecifiedTests=false -Dtest=McpFixtureContractTest test`.

- [ ] **Step 3: Implement deterministic providers and clients**

Provide echo/query/write/high-risk, resource text/blob/template/update, prompt/completion, input-required job, cancellation, App UI and remote failure endpoints. Never require an LLM or third-party API.

- [ ] **Step 4: Run GREEN and install fixture executables**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test -am -DskipITs test`.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test
git commit -m "test(gateway): add complete MCP fixtures"
```

### Task 18: Conformance, Security, Failure, HA, and Frontend End-to-End Gates

**Files:**
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpStableConformanceIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpRcConformanceIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpSecurityIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpHaRecoveryIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpCompleteReleaseIT.java`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/resources/mcp/rc-scenarios.json`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/resources/mcp/security-corpus.json`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/resources/mcp/complete-release.json`
- Modify: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/e2e/mcp-control-plane.spec.ts`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/scripts/run-mcp-conformance.sh`
- Create: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/scripts/run-mcp-security.sh`

**Interfaces:**
- Consumes: complete runtime/Admin/Web and Task 17 fixtures.
- Produces: one executable complete MCP Release Gate.

- [ ] **Step 1: Add failing release-gate tests**

The complete IT must assert every spec primitive, local HTTP/RPC, remote Stable/RC, task recovery, App SHA/CSP, RBAC/approval, DDC interruption, LKG restore, failed release rollback and cross-node calls.

```java
@Test
void completeReleaseGate() {
    assertAll(
            this::stableConformance,
            this::rcScenarios,
            this::allLocalPrimitives,
            this::allRemotePrimitives,
            this::securityCorpus,
            this::crossNodeAndRecovery
    );
}
```

- [ ] **Step 2: Run RED**

Run: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -DskipITs=false -Dtest='Mcp*IT' test`.

Expected: incomplete capabilities fail with exact assertions.

- [ ] **Step 3: Close every observed gap without weakening assertions**

Use the official conformance CLI for Stable and the pinned official RC schema/scenarios. Add hostile cases for SSRF, traversal, external refs, deep/large JSON, prompt injection, token leak, task/approval replay and App CSP. Do not mark failures skipped except when a test explicitly proves an optional protocol feature is disabled by configuration.

- [ ] **Step 4: Run the full release gate**

Run:

```bash
./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway -am -DskipITs=false clean verify
cd egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web
npm run lint
npm run typecheck
npm test -- --run
npm run build
npm run e2e
```

Expected: every command exits 0.

- [ ] **Step 5: Commit**

```bash
git add egon-cola-platforms/egon-cola-platform-gateway
git commit -m "test(gateway): verify complete MCP release"
```

### Task 19: Four-System Host-Local E2E, Operator Scripts, and Final Running Topology

**Files:**
- Create: `scripts/unified-platform/start-local-stack.sh`
- Create: `scripts/unified-platform/stop-local-stack.sh`
- Create: `scripts/unified-platform/status-local-stack.sh`
- Create: `scripts/unified-platform/verify-local-stack.sh`
- Create: `scripts/unified-platform/lib/common.sh`
- Create: `scripts/unified-platform/fixtures/unified-platform-release.json`
- Create: `scripts/unified-platform/fixtures/rbac3-bootstrap.json`
- Create: `docs/operations/unified-identity-mcp-local-runbook.md`
- Modify: `README.md`

**Interfaces:**
- Consumes: completed unified identity plan, Tasks 1-18, host PostgreSQL/Redis/Kafka and executable jars/web builds.
- Produces: verified IdP/RBAC3/DDC/Gateway/MCP/mock-provider topology left running for user QA.

- [ ] **Step 1: Write the failing verification script first**

`verify-local-stack.sh` must fail unless all processes, health endpoints, SSO, RBAC snapshots, DDC registration/release, Stable/RC/Legacy MCP calls, local/remote primitives, task recovery and Admin Web static endpoints are healthy. It reads passwords/tokens from restrictive files and never prints them.

```bash
verify_http "idp" "${IDP_BASE_URL}/actuator/health/readiness"
verify_http "rbac3" "${RBAC3_BASE_URL}/actuator/health/readiness"
verify_http "ddc" "${DDC_BASE_URL}/actuator/health/readiness"
verify_http "gateway" "${GATEWAY_BASE_URL}/actuator/health/readiness"
verify_mcp_stable
verify_mcp_rc
verify_mcp_legacy
verify_task_cross_engine
```

- [ ] **Step 2: Run RED before starting anything**

Run: `scripts/unified-platform/verify-local-stack.sh`.

Expected: exits non-zero with a precise missing-process report.

- [ ] **Step 3: Implement safe start/stop/status and fixture bootstrap**

Use explicit ports, PID files, log files and secret files under a validated `target/local-unified-platform/` directory. Start host-local PostgreSQL/Redis/Kafka only if owned by the script; otherwise verify and reuse user-local services without deleting data. Bootstrap IdP only from `IDP_BOOTSTRAP_PASSWORD_FILE`; publish RBAC3 mappings/permissions; create Gateway MCP control-plane data; publish one unified release; never log credentials.

- [ ] **Step 4: Perform deep host-local validation**

Run full Maven and frontend gates from Task 18, then:

```bash
scripts/unified-platform/start-local-stack.sh
scripts/unified-platform/verify-local-stack.sh
scripts/unified-platform/status-local-stack.sh
```

Manually induce and automatically verify: DDC interruption/LKG continuity, invalid release rollback, Remote MCP outage/circuit recovery, Engine A task creation/Engine B read, RBAC permission revocation and IdP tokenVersion revocation.

Expected: verifier exits 0 after recovery and records only sanitized evidence.

- [ ] **Step 5: Commit scripts and runbook**

```bash
git add scripts/unified-platform docs/operations/unified-identity-mcp-local-runbook.md README.md
git commit -m "test(platform): verify unified identity and MCP stack"
```

- [ ] **Step 6: Leave the verified topology running for user QA**

Do not run the stop script. Report exact Admin Web URLs, API/health URLs, MCP endpoints, PID/status command, sanitized log locations and how the user can retrieve the generated QA credential from its restrictive local file.

## Plan Self-Review

### Spec coverage

- Tasks 1-3 cover contracts, protocols and atomic DDC/LKG release.
- Tasks 4-5 cover the full control plane, one V7 migration and APIs.
- Tasks 6-8 cover local invocation, IdP/RBAC3/approval and all transports.
- Tasks 9-13 cover Resources, Prompts, Completion, Tasks, Apps and Remote Federation.
- Task 14 covers config, telemetry, audit, health and recovery.
- Tasks 15-16 cover the complete Admin Web and SSO surface.
- Tasks 17-19 cover fixtures, conformance, security, HA, four-system E2E and final running processes.

### Type consistency

- `McpRuleContent` is defined in Task 1, nested in `GatewayRuleContent` in Task 3 and compiled by all later tasks.
- `GatewayOperationInvoker` is defined in Task 6 and consumed by Tools, Resources, Prompts and Completion.
- `McpAuthorizationPort` and `McpApprovalPort` are defined in Task 7 and used by all primitive handlers.
- `McpSessionStore` and `McpSubscriptionEventStore` are defined in Task 8 and reused by Task 9.
- `McpTaskStore` is defined in Task 11 and implemented by the PostgreSQL adapter against Task 4's table.
- `McpAppArtifactStore` is defined in Task 12 and used by Admin upload and Engine read.
- `RemoteMcpClient` and `RemoteAuthProvider` are defined in Task 13 and never accept an inbound bearer credential as outbound auth.

### Scope and execution

The plan is large because the approved design has one complete Release Gate, but every task has a distinct reviewer boundary and an exact test command. No capability is deferred to another release. Execution remains inline and follows the already selected `superpowers:executing-plans` path.
