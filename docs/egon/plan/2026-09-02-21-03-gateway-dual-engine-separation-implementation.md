# Gateway Dual-Engine Separation Implementation Plan

| Field | Value |
| --- | --- |
| Document | `2026-09-02-21-03-gateway-dual-engine-separation-implementation.md` |
| Template Version | `4` |
| Status | `Ready` |
| Created | `2026-09-02 21:03 CST` |
| Updated | `2026-09-05 07:01 CST` |
| Owner | `Egon-COLA maintainers` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-platform-gateway runtime, Admin projection/Admin Web, gateway test suite, deployment and local operations` |
| Source Requirement | `Confirmed decision: one Admin Server and two Gateway Engine roles; continue with an implementation-ready Plan` |
| Baseline Revision | `main@ce10db63a24eecf6b5a3280cd4e9afa7c1849c81; concurrent dirty-worktree snapshot recorded in §6.1` |
| Implements Spec | [Gateway Dual-Engine Separation Specification](../spec/2026-09-02-19-52-gateway-dual-engine-separation.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-09-05 07:01 CST; baseline main@085c20048e35; explicitly confirmed by the user on 2026-09-02; execution conflict resolution delegated on 2026-09-05` |
| Effective Specs | [Gateway Dual-Engine Separation Specification](../spec/2026-09-02-19-52-gateway-dual-engine-separation.md), [Gateway Engine MCP Package Refactor Specification](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md), [Gateway Complete MCP Design](../../superpowers/specs/2026-08-02-gateway-complete-mcp-design.md), [Gateway Annotation-Managed MCP Design](../../superpowers/specs/2026-08-06-gateway-annotation-managed-mcp-design.md), [Gateway Admin Backend Design](../../superpowers/specs/2026-07-25-gateway-admin-backend-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | [Gateway Engine MCP Package Refactor Implementation Plan](2026-08-19-14-28-gateway-engine-mcp-package-refactor.md) |

## 1. Summary

This Plan implements the confirmed topology of one logical Admin Server plus two independently deployable Gateway Engine
roles: the existing `egon-cola-platform-gateway-engine` becomes the API/RPC Engine, while a new
`egon-cola-platform-gateway-mcp-engine` owns MCP ingress and runtime state. Shared provider selection, traffic,
transport, observability, operation invocation, and rule-activation capabilities move to a non-executable
`egon-cola-platform-gateway-runtime-core`; executable modules depend on that library but never on each other.

Implementation is split into eleven sequential, path-limited Steps. Each Step starts with a focused RED contract,
reaches GREEN, runs its module gate, and produces one semantic commit. Completion requires Maven reactor proof, Admin
Web type/test/build proof, package/dependency/config/profile static gates, unified release and role-consistency tests,
and separately identified user-controlled live topology validation. No database schema change, public wire-contract
change, service startup, browser use, Docker startup, deployment, or publication is part of Plan authoring or static
execution.

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Gateway Dual-Engine Separation Specification](../spec/2026-09-02-19-52-gateway-dual-engine-separation.md)
- Status: `Accepted`
- Revision: `Updated 2026-09-05 06:41 CST`; authored against `main@085c20048e35`; repository drift reconciled through `main@ce10db63a24eecf6b5a3280cd4e9afa7c1849c81`.
- Approval evidence: the user first fixed the architecture as “一个 admin server 两个 gateway engine”, then explicitly confirmed the Spec and invoked `egon-coding-writing-plan`.

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [Dual-engine separation](../spec/2026-09-02-19-52-gateway-dual-engine-separation.md) | `Review`; 2026-09-05 06:41 CST; user-confirmed | All sections | Governs topology, ownership, contracts, consistency, rollout, tests, and exclusions. |
| Amended predecessor | [MCP package refactor](../spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md) | `Accepted`; repository implementation present | Feature-first MCP package boundaries and compatibility rules, except single-executable placement and configuration conclusions amended by the primary Spec | Preserves the already-accepted package refactor while changing deployable ownership. Its reused `REQ-001` through `REQ-006` identifiers are governed by the primary Spec statements in this Plan. |
| Protocol dependency | [Complete MCP design](../../superpowers/specs/2026-08-02-gateway-complete-mcp-design.md) | Existing normative design | MCP protocol, security, capability, session/task, direct operation invocation, error, metrics, and unified snapshot semantics; same-process deployment portions are amended | Prevents the split from weakening MCP behavior or external contracts. |
| Managed-tool dependency | [Annotation-managed MCP design](../../superpowers/specs/2026-08-06-gateway-annotation-managed-mcp-design.md) | Current-design successor named by the MCP design | Annotation source of truth, managed tool projection, whole-argument RPC mapping, and removal of local draft CRUD | Fixes the MCP compiler's authoritative source and compatibility behavior. |
| Admin dependency | [Admin backend design](../../superpowers/specs/2026-07-25-gateway-admin-backend-design.md) | Existing normative design | One Admin, release persistence/state machine, DDC management boundary, projections, recovery, and existing APIs | Role completeness extends, but does not replace, Admin release semantics. |

### 2.3 Superseded or excluded content

- The predecessor's conclusion that all MCP runtime code remains inside `egon-cola-platform-gateway-engine` is superseded by the primary Spec §7–§10.
- The complete MCP design's same-JVM local state assumption is superseded by one role-local atomic/LKG/ACK state per Engine role; the Admin active key and release artifact remain single and unified.
- OpenAPI and direct-RPC Specs remain contextual consumer contracts. They are not effective design sources because this change preserves those contracts verbatim.
- Generated Archetype work, portal-login changes after the Spec baseline, and unrelated Admin Web build state are outside this Plan.

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | [Primary Spec](../spec/2026-09-02-19-52-gateway-dual-engine-separation.md) §4 | One logical Admin manages exactly the `API_RPC` and `MCP` Engine roles. | Admin projection declares both roles and reports missing/unknown roles deterministically. | Contract enum, Admin strategy/projection, UI, deployment metadata, tests. |
| `REQ-002` | Primary Spec §4, §7 | Existing Engine keeps API, HTTP, WebSocket, and RPC ingress and contains no MCP runtime/JDBC ownership. | Package/dependency tests show no `mcp` package, MCP core dependency, JDBC, or PostgreSQL in the API Engine. | Engine POM/config/runtime/resource cleanup and boundary tests. |
| `REQ-003` | Primary Spec §4, §7 | New executable MCP Engine owns MCP ingress, sessions, tasks, approvals, capabilities, subscriptions, and MCP health. | New Spring Boot jar starts from its own main class and its context contains MCP Beans only. | New module, application/config/runtime/server/resources/Dockerfile and moved MCP slice. |
| `REQ-004` | Primary Spec §4, §6 | Both executables depend on runtime-core and never on each other. | Maven dependency tree and package-boundary tests enforce the DAG. | Parent/POM changes, runtime-core extraction, ArchUnit/static tests. |
| `REQ-005` | Primary Spec §4, §8 | MCP invokes Providers directly through shared operation/provider capabilities, never through API/RPC Engine loopback. | MCP context lacks API ingress Beans; operation tests reach provider adapters without engine HTTP/RPC ingress. | Shared operation/HTTP-RPC bridge, MCP adapter, no self-call configuration. |
| `REQ-006` | Primary Spec §4, §9 | One release snapshot/artifact/active key feeds both roles; each role activates atomically with independent LKG and ACK. | Compiler/activation tests prove same identity and role-local restore/ACK paths. | Generic compiled DTO/Strategy, role compilers, LKG directories, release tests. |
| `REQ-007` | Primary Spec §4, §9 | Admin consistency requires both roles and all online nodes on the same release/version/checksum with READY ACK. | Projection tests cover missing role, unknown role, skew, not-ready, and consistent replicas. | Admin role strategy and projection reason codes. |
| `REQ-008` | Primary Spec §4, §11 | External MCP/API/RPC/Admin wire contracts remain unchanged. | Golden controller, rule-wire, MCP conformance, RPC, and frontend mapping tests remain compatible. | No endpoint/DTO/schema change beyond existing node metadata visibility. |
| `REQ-009` | Primary Spec §4, §14–§15 | Roles have independent identity, credentials, TLS, ports, health, logs, LKG, scale, rollout, and rollback. | Compose/config tests find distinct role identities, ports 18084/18085 for MCP defaults, volumes, probes, and rollback targets. | Resources, Docker/Compose/proxy, scripts, runbooks. |
| `REQ-010` | Primary Spec §4, §10 | Redis/PostgreSQL/artifact/remote MCP state belongs to MCP Engine; API Engine has no MCP database configuration. | Config/dependency searches and context tests enforce ownership. | Move MCP adapters/configuration and remove API datasource/MCP keys. |
| `REQ-011` | Primary Spec §4, §13 | Preserve `egon.cola.component.gateway.engine.mcp` and `GATEWAY_MCP_*`; add parity-controlled MCP bootstrap/operations profiles. | Key-parity tests and compatibility binding tests pass for old MCP environment names. | New bootstrap properties/resources and profile/static tests. |
| `REQ-012` | Primary Spec §4, §16 | Validate modules, Beans, protocols, release roles, frontend, deployment, and distinguish static from live proof. | Ordered gate matrix completes; live commands remain explicitly user-controlled. | All Steps and final audits. |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Publish the role contract first, then create the non-executable runtime-core and move only dependency-closed
capabilities into it. Generalize release compilation/activation before creating the MCP compiler, so the new executable
consumes stable shared contracts instead of copying the mixed Engine. The actual MCP ownership transfer is one atomic
Step: moved types, new bootstrap, API cleanup, and both context boundaries must compile together. Admin consistency and
UI follow the role contract; cross-module tests follow both executables; deployment and operator tooling follow stable
module/jar/config names.

There is no generated-code or database dependency. Test fixtures precede production changes inside every Step. Maven
module order is contract → core/mcp-core → runtime-core → API/MCP executables → Admin → test suite. Frontend work
follows the finalized Admin projection shape. Configuration and deployment are deliberately last so they cannot encode
transient module boundaries.

### 4.2 Test-first strategy

| Step | RED contract | Expected RED reason | Minimum GREEN implementation |
| --- | --- | --- | --- |
| 1 | `GatewayEngineRoleEnumTest` | Enum/metadata parser absent | Add exactly `API_RPC` and `MCP`. |
| 2 | Runtime package/dependency boundary test | runtime-core module and shared package absent | Create library and move common capabilities/tests. |
| 3 | Generic activation/compiler tests | activation is tied to `CompiledGatewayRules` | Introduce compiled DTO and Strategy and bridge current compiler. |
| 4 | Shared transport/operation tests with runtime imports | shared types still owned by executable | Move dependency-closed HTTP/RPC outbound/operation slice after its DTO exists. |
| 5 | MCP compiler strategy tests | MCP role compiler/DTO absent | Add compiler-only MCP Engine slice and module wiring. |
| 6 | API/MCP context and ownership tests | one mixed executable still owns both planes | Move MCP runtime, add MCP bootstrap, delete mixed API wiring. |
| 7 | Admin role-consistency tests | projection ignores role completeness | Add named Strategy and integrate projection. |
| 8 | Admin Web component tests | role states are not rendered | Extend type and render both role states. |
| 9 | Cross-module release/topology tests | harness assumes one executable kind | Teach suite both artifacts and role-local ACK/LKG. |
| 10 | Compose/config/proxy contract test | no separate MCP services or stable path router | Add independent services, routing, probes, and overlays. |
| 11 | Local script/CI static contract tests | scripts/workflow know only one Engine jar | Add role-specific lifecycle and CI selectors; update runbooks. |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | Gateway contract package | Establishes the only role vocabulary. |
| Step 2 | Step 1 | None | Gateway parent and common packages | Creates the shared dependency target. |
| Step 3 | Step 2 | None | Rule activation/compiler types | Stabilizes shared release contracts. |
| Step 4 | Step 3 | None | HTTP/RPC outbound/operation packages | Moves transport after the compiled accessor contract exists. |
| Step 5 | Step 4 | None | New MCP module/compiler | Needs shared Strategy and DTO. |
| Step 6 | Step 5 | None | Both executable modules and MCP packages | Atomic ownership transfer prevents duplicate Beans/classes. |
| Step 7 | Step 1, Step 6 | Step 8 only after backend contract is locally fixed | Admin projection/configuration | Consistency consumes role metadata. |
| Step 8 | Step 7 | None | Admin Web gateway types/pages | UI must follow the tested backend projection. |
| Step 9 | Step 6, Step 7 | None | Gateway test suite | Cross-module harness needs both jars and Admin semantics. |
| Step 10 | Step 6, Step 9 | None | Gateway deployment tree | Deployment names/probes follow final runtime contracts. |
| Step 11 | Step 10 | None | Root scripts, workflow, operator docs | Lifecycle automation follows final Compose/jar names. |

### 4.4 Commit boundaries

Every Step produces exactly one semantic, path-limited commit after its focused and module gates pass. Mechanical
renames and their import/package updates remain in the same Step as the owning behavior because splitting them would
leave a non-compiling reactor. No broad staging is permitted; each `git add` uses only the paths listed on that Step.
The pre-existing dirty/untracked paths in §6.1 remain unstaged.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| New runtime-core library | Necessary; primary §6–§7 | `gateway-engine` currently owns common provider/traffic/security/transport/observability and both roles need them | Move the dependency-closed capabilities; do not introduce a second core abstraction | One module, package/import moves, boundary tests | Implement. |
| Two executable engines | Necessary; primary §5–§7 | One `GatewayEngineApplication` and mixed `GatewayEngineConfiguration` own API/RPC/MCP | Retain existing artifact for API/RPC and add only MCP artifact | New bootstrap/resource/deployment lifecycle | Implement. |
| Compiler Strategy | Necessary Complex variation; primary §10, §13 | `EngineGatewayRuleCompiler` compiles every protocol into one concrete record | Generic Strategy selected by explicit qualified Bean wiring; no role-switching Factory/registry hierarchy | Two implementations and shared activation contract | Implement Strategy. |
| Admin role strategy | Necessary Complex consistency rule; primary §9, §12 | `GatewayProjectionService` checks release/ACK but not role set | One named Strategy consumed directly by existing projection service | One class and focused tests | Implement Strategy; reject a new service layer. |
| Dedicated MCP server adapter | Necessary; primary §8 | MCP handler is currently composed into `GatewayCompositeHttpDataPlaneHandler` | Reuse shared `GatewayHttpListener`/handler SPI, create the Spec-defined MCP server, and adapt the MCP handler; no reverse proxy/self-call | One thin adapter plus role server | Implement Adapter. |
| Data-plane proxy config | Necessary operational selection; primary §11, §15 | Existing `deployment/haproxy.cfg` serves control-plane HA only | Add a dedicated data-plane HAProxy file/service; do not overload Admin control-plane proxy | Path routing and health checks | Implement as deployment-only routing. |
| New persistence schema/cache/API | Unnecessary and excluded; primary §17 | Existing release repository, MCP stores, endpoints, and active key are sufficient | Reuse existing repositories/stores/contracts | None | Do not implement. |
| Fetch-then-forward or caller-derived API | Unnecessary | MCP already receives trusted snapshot/context and calls `GatewayOperationInvoker` | Direct provider invocation from compiled snapshot | No new public API | Do not implement. |
| Duplicate DTO/converter layer | Unnecessary | Existing snapshot/projection records and Jackson mapping suffice | Shared interface plus role-specific immutable records | Two records only | Do not add mappers or duplicate wire DTOs. |

The audit found no material overdesign or Spec defect. Strategy addresses real role variation; Adapter isolates MCP
ingress; direct implementation remains preferable everywhere else. State, endpoints, active keys, and persistence are
reused.

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Engine role contract | `REQ-001`, `REQ-007` | Enum test | contract module | `GatewayEngineRoleEnum` | Admin, engines, tests, deployment | Step 1 |
| Shared common runtime | `REQ-002`, `REQ-004` | boundary tests | role contract | runtime-core base | both executables | Step 2 |
| Shared outbound transport/operation | `REQ-004`, `REQ-005` | transport/operation tests | runtime-core base | direct-provider capability | MCP compiler/runtime | Step 3 |
| Generic release activation | `REQ-006` | compiler/activation tests | shared runtime | DTO and Strategy contracts | both compilers | Step 4 |
| MCP compiler slice | `REQ-003`, `REQ-006`, `REQ-011` | strategy/config tests | generic activation | MCP compiled rules | MCP executable | Step 5 |
| Executable ownership split | `REQ-002`, `REQ-003`, `REQ-005`, `REQ-010`, `REQ-011` | context/package tests | Steps 1–5 | two runnable jars | Admin/tests/deployment | Step 6 |
| Admin consistency | `REQ-001`, `REQ-007`, `REQ-008` | projection/contract tests | role enum | role-complete projection | Admin Web/test suite | Step 7 |
| Role UI | `REQ-007`, `REQ-008` | component tests | Admin projection | two role states | operator experience | Step 8 |
| Cross-module proof | `REQ-006`, `REQ-008`, `REQ-012` | suite tests | both engines/Admin | harness and release evidence | deployment gates | Step 9 |
| Deployment topology | `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012` | Compose/config tests | stable jars/harness | independent services and proxy | local operations | Step 10 |
| Lifecycle/CI/docs | `REQ-009`, `REQ-011`, `REQ-012` | shell/workflow static tests | deployment topology | repeatable commands/docs | final audit | Step 11 |

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | Gateway already uses platform modules with `contract/core/mcp-core/engine/admin/starter/test`; it is not generated Archetype application code | Primary §6 selects focused layer-local platform modules | Preserve platform feature-first packages; add runtime-core and MCP executable only; dependency DAG is enforced | Steps 1–11; `MC-ARCH-001` |
| Reuse/capability | Existing `GatewayOperationInvoker`, `GatewayHttpListener`, provider/traffic/security services, DDC client, Redis/JDBC MCP stores | Primary §6–§10 requires extraction/reuse | Move and import existing types; no substitute client, cache, or persistence abstraction | Steps 2–7; `MC-REUSE-001`, `MC-DEP-001` |
| Naming/model/validation/conversion | Existing suffixes include `Properties`, `Configuration`, `Runtime`, `Adapter`, `Service`; contract uses records/enums; `ValidationUtils` exists in common-core | Primary §10, §13 defines role-specific DTO/Strategy names | Use exact semantic suffixes, records for compiled values/properties, Jakarta constraints at binding boundary; no converter is needed because no layer model mapping is added | Steps 1, 4–7; `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-CONVERT-001` |
| Bean/logging/util/JSON/time/config | Gateway `lombok.config` copies `@Qualifier`/`@Value`; Spring configuration and Jackson codec already exist; `Clock` appears in Admin tests | Primary §6.2, §13 | Explicit Bean names and qualified final constructor fields, `@Slf4j`, `@RequiredArgsConstructor`; reuse Jackson/java.time; parity-test base and operations YAML | Steps 2–11; `MC-BEAN-001`, `MC-LOG-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-CONFIG-001` |
| Business variation/pattern | Rule compilation and Admin consistency branch by Engine role | Primary §10 and §12 explicitly select Strategy; MCP handler adaptation is a transport boundary | Two compiler Strategies plus one Admin consistency Strategy; thin Adapter for handler/server; reject factories, state machine duplication, and self-call Facade | Steps 4–7; `MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| Provider invocation | Gateway core, engine operation, HTTP/RPC adapters | `GatewayOperationInvoker`, `EngineGatewayOperationInvoker`, `HttpRpcUpstreamAdapter` | Fits after package extraction and generic compiled accessor | Reuse | No external dependency; package/interface adjustment only | Steps 3–6; `MC-REUSE-001` |
| HTTP listener/drain | Reactor Netty listener/transport types | `GatewayHttpListener`, handler SPI, transport timeout/commit/drain services; API `GatewayHttpServer` is a lifecycle pattern only | Shared listener/transport fits both roles; MCP still needs its own server lifecycle class | Reuse listener and pattern | One MCP server class, no new dependency | Steps 3, 6; `MC-REUSE-001` |
| Release activation | Engine rule compiler/applier and Admin release service | `GatewayRuleActivationApplier`, `GatewayRuleLkgRepository`, active release projection | Fits after generic DTO/Strategy seam | Reuse | Two small role records/Strategies | Steps 4–7; `MC-PATTERN-001` |
| MCP state/security | mcp-core plus existing engine MCP adapters | `RedisMcpSessionStore`, `JdbcMcpRuntimeTaskStore`, RBAC3/identity adapters | Behavior fits; ownership is wrong | Move unchanged behavior | Existing Redis/JDBC/security dependencies move to MCP POM | Step 6; `MC-DEP-001` |
| Validation | Jakarta Validation and Egon `ValidationUtils` | Admin POM already uses validation; common-core supplies `ValidationUtils` | Fits configuration binding and explicit invariant checks | Reuse | Add existing managed validation starter only to MCP executable if absent transitively | Steps 5–6; `MC-VALID-001`, `MC-DEP-001` |
| JSON/time | Jackson and `java.time.Clock` | `GatewayRuleJsonCodec`, Admin projection tests | Fits without custom serializers/time libraries | Reuse | None | Steps 4, 7; `MC-JSON-001`, `MC-TIME-001` |
| Data-plane routing | Existing HAProxy deployment convention | `deployment/haproxy.cfg`, HA compose overlays | Control-plane file must remain isolated; same image/convention fits | Reuse image and health-check convention with a new dedicated config | One config file/service, no application dependency | Step 10; `MC-REUSE-001` |

### 4.8 User-mandated Java Rule Implementation Matrix

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Primary Spec §6.2, §10 | Affected types use domain suffixes but mixed `Engine` naming | Contract enum → shared DTO/Strategy → API/MCP role classes → Admin Strategy | Exact suffixes `Enum`, `DTO`, `Strategy`, `Properties`, `Configuration`, `Runtime`, `Adapter`, `Service` | Type inventory plus compile/search | Steps 1–7 | PASS |
| Rule 2 | Primary Spec §6.2, §13 | Spring configuration binding and manual property validation exist | Properties tests → immutable properties → configuration consumers | Jakarta constraints at binding; `ValidationUtils` for explicit internal checks; no invented validation group | Positive/negative binding and context tests | Steps 5–6 | PASS |
| Rule 3 | Primary Spec §6.2, §10 | Compiled rules are immutable records; runtime/config services are mutable handwritten classes | Tests → role records → consumers; Lombok classes only where Spring/service state needs it | Records for value objects; complete `@RequiredArgsConstructor` baseline; no MapStruct because no model conversion | Constructor/mapping compile and tests | Steps 1, 4–7 | PASS |
| Rule 4 | Primary Spec §6.2, §13 | `lombok.config` copies qualifier/value; several current Beans are implicit | Tests → business Beans → configurations → context tests | `@Slf4j`, explicit stereotype/Bean names, `@RequiredArgsConstructor`, qualified final fields | Wiring/context/log review | Steps 2–7 | PASS |
| Rule 5 | Primary Spec §6.2, §17 | Reactor, Jackson, Micrometer, Spring, DDC, Redis/JDBC already exist in reactor | Tests → POM moves → imports | Closed dependency allowlist; no Hutool/Guava/Apache helper addition | dependency tree and forbidden-import search | Steps 2–11 | PASS |
| Rule 6 | Primary Spec §4, §11 | Rule snapshot and Admin/MCP JSON already use Jackson | Wire tests → role DTOs → codecs/controllers | Preserve property names/defaults; add no external field except existing metadata map consumption | Serialization/golden compatibility tests | Steps 4, 7–9 | PASS |
| Rule 7 | Primary Spec §13, §15 | Engine has base and operations YAML; environment uses overlays/env | Config tests → base MCP YAML → operations YAML → Compose overlays | Identical key structure; retain old MCP prefix/env; new bootstrap keys only under approved prefix | Key parity/config/Compose tests | Steps 5–6, 10–11 | PASS |
| Rule 9 | Primary Spec §10, §12 | Two role-dependent complex decisions exist | Generic contract → two compiler Strategies → Admin consistency Strategy → wiring/tests | Strategy selection by role/Bean, Adapter at MCP handler boundary; direct code elsewhere | Pattern behavior and context tests | Steps 4–7 | PASS |
| Rule 10 | Primary Spec §9, §12 | Release timestamps use `java.time`; projection tests already inject `Clock` | Clock Bean → projection injection → time assertions/search | `Clock` injection, `Instant`/`Duration`; preserve precision and JSON semantics | time tests and forbidden legacy-date search | Step 7 | PASS |
| Rule 11 | Primary Spec §6–§10 | Current platform Gateway has a consistent multi-module feature-first structure | Every listed target in Steps 1–11 | Preserve platform profile; no Archetype hybrid or third architecture | package boundary, reactor, static tree audit | Every Step | PASS |

## 5. Change File Tree

The inventory uses exact directory roots for mechanical package moves; each basename listed under a root is part of that
single operation and must not appear in another operation.

```text
egon-cola-platforms/egon-cola-platform-gateway/
├── pom.xml                                                     MODIFY
├── egon-cola-platform-gateway-contract/                       CREATE role enum/test
├── egon-cola-platform-gateway-runtime-core/                   CREATE library, common/shared transport/rule tests
├── egon-cola-platform-gateway-engine/                         MODIFY API_RPC-only POM/bootstrap/resources/tests
├── egon-cola-platform-gateway-mcp-engine/                     CREATE executable; RENAME MCP runtime/tests into it
├── egon-cola-platform-gateway-admin/                          MODIFY role consistency/projection/config/tests
├── egon-cola-platform-gateway-test/...-test-suite/            MODIFY dual-role harness/live/release/deployment tests
└── deployment/                                                 MODIFY Compose/docs/scripts; CREATE data-plane HAProxy config
egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/
└── src/                                                        MODIFY gateway type/features; CREATE component tests
scripts/                                                         MODIFY unified identity lifecycle and contract tests
.github/workflows/rbac3.yml                                      MODIFY MCP module selector
docs/operations/unified-identity-mcp-local-runbook.md             MODIFY
docs/runbooks/unified-identity-local.md                           MODIFY
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-platforms/egon-cola-platform-gateway/pom.xml` | Modules stop at one Engine | Adds runtime-core and MCP Engine in dependency order | Reactor/module management | Steps 2, 5 | `REQ-003`, `REQ-004` | Maven reactor |
| CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract` | No role enum; existing JUnit/JDK dependencies suffice | `GatewayEngineRoleEnum` and test | Shared role vocabulary | Step 1 | `REQ-001`, `REQ-007`, `REQ-009` | Contract test |
| CREATE/RENAME | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core` | Directory absent; common capabilities live in Engine | POM plus common observability/provider/security/traffic/transport, shared HTTP/RPC outbound/operation/rule contracts and their pure tests | Non-executable shared runtime | Steps 2–4 | `REQ-002`, `REQ-004`, `REQ-005`, `REQ-006` | Boundary and focused tests |
| MODIFY/DELETE/RENAME | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine` | Mixed API/RPC/MCP executable | API_RPC-only POM/config/runtime/resources/compiler; no MCP/JDBC; updated API tests | API/RPC executable | Steps 2–6 | `REQ-002`, `REQ-004`, `REQ-005`, `REQ-010`, `REQ-011` | Engine context/boundary tests |
| CREATE/RENAME | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine` | Absent; MCP slice lives in Engine | POM, Dockerfile, main/config/runtime/properties/server/adapter/compiler/resources and moved MCP adapters/services/domain/tests | MCP executable | Steps 5–6 | `REQ-003`, `REQ-005`, `REQ-006`, `REQ-010`, `REQ-011` | MCP context/boundary/integration tests |
| MODIFY/CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin` | Projection lacks role completeness | Named role Strategy, qualified Clock/wiring, role-complete projection and tests | One Admin consistency authority | Step 7 | `REQ-001`, `REQ-007`, `REQ-008` | Admin tests |
| MODIFY/CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src` | Metadata not typed/rendered as role state | Typed metadata and API_RPC/MCP cards/states with tests | Operator visibility | Step 8 | `REQ-007`, `REQ-008` | Vitest/typecheck/build |
| MODIFY/CREATE | `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite` | Harness/live tests assume one Engine kind | Both artifact resolvers, role-aware live topology, release/LKG/ACK/wire/deployment tests | Cross-module proof | Step 9 | `REQ-006`, `REQ-008`, `REQ-012` | Test-suite Maven gate |
| MODIFY/CREATE | `egon-cola-platforms/egon-cola-platform-gateway/deployment` | Compose starts only mixed Engine replicas | Independent API/MCP replicas, credentials/ports/volumes/probes/overlays, stable path proxy, updated scripts/docs | Deployment topology | Step 10 | `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012` | Compose/static tests; live user gate |
| MODIFY | `scripts` | Unified identity scripts manage one Engine jar | Prepare/start/status/verify/stop both role jars and preserve current portal/release reconciliation | Local lifecycle | Step 11 | `REQ-009`, `REQ-011`, `REQ-012` | Shell contract tests |
| MODIFY | `.github/workflows/rbac3.yml` | Explicit selector covers old Engine only | Includes MCP Engine without changing unrelated jobs | CI module coverage | Step 11 | `REQ-012` | Workflow/static audit |
| MODIFY | `docs/operations/unified-identity-mcp-local-runbook.md`, `docs/runbooks/unified-identity-local.md` | Mixed Engine commands/ports | Two-role startup, health, LKG, skew, rollback and user-controlled live commands | Operations contract | Step 11 | `REQ-009`, `REQ-012` | Link/command audit |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Applicable instructions: user-supplied root `AGENTS.md` rules and the `egon-coding-writing-plan` skill. Execution must later use the approved Plan gate, one Step and one path-limited commit at a time.
- Baseline: branch `main`, commit `ce10db63a24eecf6b5a3280cd4e9afa7c1849c81` at Plan finalization. Drift since the Spec baseline consists of gateway local-stack/portal-origin/IdP CORS fixes and Archetype source-generation work; no dual-engine topology exists yet.
- Existing concurrent user-owned state to preserve and never stage: all modified/staged/deleted/untracked paths under `egon-cola-archetypes/**`, `egon-cola-platforms/egon-cola-platform-admin-web-shared/tsconfig.app.tsbuildinfo`, `docs/egon/plan/2026-09-01-11-23-archetype-two-stage-source-generation-implementation.md`, and `docs/egon/spec/2026-08-27-20-31-archetype-two-stage-source-generation.md`.
- The primary dual-engine Spec and this Plan are documentation deliverables for the current task. Generated Archetype paths and any later concurrent changes remain outside implementation commits unless an exact Step owns them.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Java/Maven | `./mvnw -version`; root POM uses Java 21 and Spring Boot 3.5.16 | JDK 21 and wrapper available | Static/module/reactor only unless a live profile is explicitly authorized |
| Gateway focused tests | `./mvnw -pl exact-gateway-module-path -am -Dtest=ExactFocusedTest -Dsurefire.failIfNoSpecifiedTests=false test` | Step-owned tests compile and pass | Focused module proof |
| Dependency proof | `./mvnw -pl ...gateway-engine,...gateway-mcp-engine -am dependency:tree` | No executable-to-executable edge; API has no MCP/JDBC | Static dependency graph |
| Admin Web | Existing package scripts in `egon-cola-platform-admin-web-shared/package.json` | Installed lockfile-consistent dependencies | Vitest/typecheck/build; no browser |
| Compose | `docker compose ... config` and repository configuration tests | Docker CLI parser only if available; no containers started | Static rendered configuration |
| Live topology | Commands in §8/§9 run only with explicit user authorization | External services, ports, credentials, Docker/runtime available | Separate live evidence, never inferred from compile/tests |

### 6.3 Immutable constraints and approved decisions

- Exactly one logical Admin Server and exactly two Engine roles, `API_RPC` and `MCP`; replicas are `1..N` per role.
- Each executable contributes its role as the code-fixed DDC metadata key `gateway.engine.role`; deployment configuration, host, port, app name, and instance ID must not select or infer the role.
- The current Engine artifact/name remains API/RPC; MCP gets a new executable artifact. Executables never depend on or call each other.
- One Admin release record, snapshot artifact, active key, version, and checksum remain authoritative. Each role has a separate local atomic reference, LKG directory, ACK, identity, credentials, TLS, health, logs, scale, rollout, and rollback.
- MCP public defaults are data port `18084` and management port `18085`. API/RPC public wire contracts, MCP routes, Admin endpoints, RPC descriptors, JSON names, and error semantics remain unchanged.
- Preserve `egon.cola.component.gateway.engine.mcp` and `GATEWAY_MCP_*` for existing MCP capability/state settings. New bootstrap keys are namespaced under `egon.cola.component.gateway.mcp-engine` with base/operations parity.
- No Flyway migration, schema edit, new active key, duplicate release, fetch-then-forward API, self-call, two-phase commit, new public endpoint, new framework, or generated code.
- Do not start services, browsers, Docker, databases, external infrastructure, deployment, or publication during implementation without explicit authorization.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Rebase implementation on `main@ce10db63` and preserve later portal/release-reconciliation/IdP CORS behavior while excluding concurrent Archetype work. | `git log` after the Spec baseline shows focused fixes but no Gateway topology change; current dirty state is confined outside the owned Gateway paths except protected shared build state. | Only reconciles current source; no Spec contract changes. | Overwriting newer behavior or staging Archetype state would be a regression. |
| `PLAN-CLAR-002` | `GatewayCompiledRulesDTO` exposes release identity, `snapshot()`, provider services/policies, and traffic policies; security/CORS remain API-specific until a shared consumer exists. | Existing `EngineGatewayOperationInvoker` and `HttpRpcUpstreamAdapter` read operations/RPC descriptors from `snapshot`; shared services read provider/traffic fields. | Gives moved code only the minimum data it already consumes and preserves one snapshot identity. | Omitting `snapshot()` forces a duplicate model; over-sharing API fields couples MCP unnecessarily. |
| `PLAN-CLAR-003` | Move the Spec-listed HTTP listener/handler/outbound domain-service slice and minimal WebSocket SPI/model; add only `HttpUpstreamAdapter`, `ProtobufDescriptorRegistry`, and `RawByteMarshaller` when required by the five moved operation files. Keep `GatewayHttpServer` and all API proxy/default/security/body/CORS handlers in API Engine; create a separate MCP server. | Current operation imports those three compile-closure types; Spec §8 lists the listener SPI and operation slice but keeps API ingress behavior. | This closes imports without moving the API server or changing ingress ownership. | Omitting closure types would not compile; moving the API server would contradict the selected ownership. |
| `PLAN-CLAR-004` | `GatewayRuntimeConfiguration` owns only property-independent common Bean assembly; each executable owns validated role properties and listener/resource construction. | Current `GatewayEngineConfiguration` mixes common Beans with API and MCP property binding. | Prevents a new shared super-properties model and preserves independent operations. | Shared role settings would recouple configuration and credentials. |
| `PLAN-CLAR-005` | New immutable `McpGatewayEngineProperties` owns only bootstrap identity/data-directory/listener/shared-runtime knobs; moved `McpRuntimeProperties` retains its old prefix for capabilities/state. | Primary Spec separates new bootstrap keys from retained `GATEWAY_MCP_*`; current mutable MCP properties cover runtime behavior. | Preserves environment compatibility while giving the executable its own startup contract. | Moving old keys would break deployments. |
| `PLAN-CLAR-006` | Add an unconditional named `gatewayProjectionClock` Bean for qualified projection injection. | Existing `gatewayOpenApiClock` is conditional and cannot guarantee a stable projection qualifier. | Only makes existing `Clock` dependency explicit/testable. | Reusing the conditional Bean could make Admin context order-dependent. |
| `PLAN-CLAR-007` | Add a dedicated data-plane HAProxy config/service rather than modifying the existing control-plane HAProxy routes. | Existing `deployment/haproxy.cfg` fronts Admin/control-plane HA. | Implements the Spec's stable Host/Path routing while isolating existing control-plane behavior. | Combining files could regress Admin HA routing. |
| `PLAN-CLAR-008` | The MCP module is introduced first with compiler/config contracts, then made executable during the atomic ownership-transfer Step. | Generic activation must exist before moved runtime wiring can compile cleanly. | This is an implementation order only; no intermediate release is deployable or published. | Attempting all extraction in one untested edit increases conflict and rollback risk. |

### 6.5 Accepted execution corrections (2026-09-05)

The user approved [the preflight correction](../review/2026-09-05-06-34-gateway-execution-preflight.md). The effective order is Step 1 role contract, Step 2 runtime foundation, Step 3 generic rule contract/activation (formerly Step 4), Step 4 outbound/operation extraction (formerly Step 3), then unchanged Steps 5–11. Where older table references still name the former Step 3 or 4, this mapping and section 7 govern.

DDC numeric version remains exclusively in `GatewayRuleRuntimeStatus.activeDdcVersion`: `apply(key,value,version)` supplies it; LKG restore uses 0/degraded until DDC applies. The compiled DTO has no numeric version. Its checksum is `snapshot.artifactSha256()` and providerServices is `Set<ProviderServiceKey>`. External wire/LKG schemas and `compile(snapshot)` remain unchanged.

Step 2 explicitly owns import/FQCN-only edits in its current direct production/test consumers, including `McpHaRecoveryIT`; no later-Step business behavior is advanced. The user additionally authorized full local platforms startup, browser testing, and repairs after the split, specifically RBAC3 role-permission assignment and Gateway OpenAPI 3 browsing. Runtime authorization is already satisfied.

### 6.6 Delegated execution decision — legacy construction (2026-09-05)

After the concrete Step 2 conflict was reported, the user directed: “继续，自行决定，不要问我”. Within that delegated authority, `DEC-EXEC-002` limits retroactive standards changes: package/import-only moves retain established type names, object representations, validation, factory construction, defensive copies, defaults and lifecycle contracts. For example, DirectoryProviderSelector's EnumMap copy/null checks and ProviderDirectory's registry/clock construction must not be replaced by a generated assignment-only constructor. Legacy annotation/constructor differences are an explicit migration-specific exception, not a claim that those unchanged implementations satisfy the original Rule 1–4 wording. The final audit must list this exception.

New and materially changed business services, Bean assembly and boundary models remain subject to the full standards. Necessary local normalization, wiring and regression fixes are now allowed within the owning Step; document exact changed paths and behavior evidence before committing. No general skill changes, unrelated cleanup, public wire changes or database migrations are authorized by this decision. Architecture, sequential commits and runtime acceptance remain unchanged.

Step 2 validation refinement: the original reactor-wide `*Provider*Test` selector also executes the unrelated `RpcProviderAccessGuardComponentTest`, which currently fails because its test classpath lacks a Jakarta Validation provider before Gateway is reached. Use the package-qualified Gateway selector below, plus all moved runtime tests and Engine boundary tests. Record the original failure separately for the later full-platform verification; it is not a passed regression. This isolates the intended Step gate without disabling any Gateway test or weakening fail-closed security behavior.

Step 3 compile-closure refinement: update the two existing suite consumers `test/mcp/McpHaRecoveryIT.java` and `test/live/GatewayRuleWireCompatibilityTest.java` in the same Step (imports/generic type arguments only). The runtime-core and Engine POMs may declare the already-managed provided Lombok dependency required by affected business classes; compiler annotation processing is already configured in the platform parent. These changes close the current source/annotation dependencies without a new library version or public protocol change.

Step 4 compile-closure refinement: the listener/upstream dependencies also require `GatewayHttpFlushMode`, `GatewayHeaderFilter`, generic `GatewayDataBufferOwnership`/`GatewayDataBufferPipeline`, and the WebSocket prepared-session/context/frame/observer/peer value-port slice including `ReactorNettyWebSocketPeer`. Move that minimal closure unchanged; keep GatewayHttpServer, GatewayWebSocketProxy, route/security/default handlers and body-logging orchestration in the API Engine. Operation services consume the already-approved GatewayCompiledRulesDTO view, not the executable's compiled record. Existing cross-module test consumers receive import-only updates; independent operation tests use a test-only minimal immutable compiled DTO rather than depending on the API compiler. The same Gateway-package restriction used in Step 2 applies to the broad `*RpcProvider*Test` selector; do not count the upstream AccessGuard failure as passed.

The three existing response lifecycle methods `GatewayOutboundHttpResponse.withBody`, `onAbandon`, and `abandon` become public Java library methods so the unchanged API handlers and future MCP server can call them across packages. Bodies and ownership semantics do not change; this is internal Java visibility, not a new HTTP/RPC contract. Preserve their idempotence/discard tests. Move the buffer/operation package metadata with their implementations, and do not rely on empty source directories in boundary tests.

## 7. Ordered File-by-file Implementation Steps

### Step 1 — Publish the two-role Engine contract

- Requirements: `REQ-001`, `REQ-007`, `REQ-009`
- Dependencies: None
- Baseline state: the contract module has gateway/release contracts but no canonical Engine role type; role strings are ad hoc DDC metadata.
- Observable outcome: all later modules can refer to exactly `API_RPC` and `MCP` through a dependency-neutral contract enum.
- End state: the enum and parser/serialization behavior are available; no runtime yet consumes it.
- Test-first gate: Required — the focused test initially fails to compile because `GatewayEngineRoleEnum` does not exist.
- Manual Checks: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 3, Rule 6, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/runtime/GatewayEngineRoleEnumTest.java`

- Purpose: Define the complete, stable role vocabulary and strict metadata parsing contract before production code.
- Symbols: `GatewayEngineRoleEnumTest`, `containsExactlyApiRpcAndMcp`, `parsesCanonicalMetadata`, `rejectsUnknownRole`.
- Repository evidence: contract tests use JUnit 5 and AssertJ under the same module; DDC node metadata is string-valued.
- Dependencies and consumers: production enum under test; later Admin, Engine, MCP Engine, and test-suite code.
- Why now: It is the RED contract for every role-aware Step.
- Contract/signature changes: Assert enum order/names and `fromWire(String)` returning `Optional` without aliases.
- Input/output and state mapping: canonical string → matching enum; null/blank/unknown → empty; no state mutation.
- Error and edge behavior: case drift and extra roles are rejected rather than silently mapped.
- Standards impact: `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001`, `MC-TEST-001`; exact enum naming and external metadata strings are locked.
- Literal rule enforcement: Rule 1 requires the `Enum` suffix; Rule 3 uses an enum value model; Rule 6 preserves canonical wire strings; Rule 11 keeps the contract in the existing contract profile.
- Implementation pseudocode:

```java
assertThat(GatewayEngineRoleEnum.values()).extracting(Enum::name)
        .containsExactly("API_RPC", "MCP");
assertThat(GatewayEngineRoleEnum.fromWire(" API_RPC ")).contains(GatewayEngineRoleEnum.API_RPC);
assertThat(GatewayEngineRoleEnum.fromWire("api_rpc")).isEmpty();
```

- Verification contribution: RED/GREEN selector proves vocabulary, order, and safe parsing.
- After this file: the test fails only because the enum is absent.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/runtime/GatewayEngineRoleEnum.java`

- Purpose: Supply the canonical cross-module Engine role contract.
- Symbols: `GatewayEngineRoleEnum`, constants `API_RPC`, `MCP`, method `fromWire(String)`.
- Repository evidence: gateway contracts already host dependency-neutral enums/records; no Spring type is needed.
- Dependencies and consumers: only JDK types; consumed later by Admin/runtime tests/deployment metadata assertions.
- Why now: It is the smallest implementation that satisfies File 1 and prevents duplicated strings.
- Contract/signature changes: Adds a new internal contract type without changing any public endpoint or JSON body.
- Input/output and state mapping: exact `name()` values round-trip; invalid input maps to `Optional.empty()`.
- Error and edge behavior: null and blank return empty; no exception leaks into node projection.
- Standards impact: `MC-ARCH-001`, `MC-NAME-001`, `MC-MODEL-001`, `MC-JSON-001`; no utility/dependency addition.
- Literal rule enforcement: Rule 1 fixes semantic suffix/value names; Rule 3 uses an immutable enum; Rule 6 fixes metadata spelling; Rule 11 places it only in contract.
- Implementation pseudocode:

```java
public enum GatewayEngineRoleEnum {
    API_RPC, MCP;
    public static Optional<GatewayEngineRoleEnum> fromWire(String value) {
        String normalized = value == null ? "" : value.trim();
        return Arrays.stream(values()).filter(role -> role.name().equals(normalized)).findFirst();
    }
}
```

- Verification contribution: makes the focused enum test GREEN and supplies role constants to later Steps.
- After this file: the role contract compiles with no runtime dependency.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract -am -Dtest=GatewayEngineRoleEnumTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; the three focused role tests pass.
- Failure returns to: File 1 if an assertion over-specifies wire behavior; File 2 if enum parsing/naming fails.
- Completion criteria: exact two-role vocabulary exists in the contract module and no other production file changes.
- Rollback: revert the Step commit; no consumer exists yet.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/test/java/top/egon/cola/component/gateway/contract/runtime/GatewayEngineRoleEnumTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-contract/src/main/java/top/egon/cola/component/gateway/contract/runtime/GatewayEngineRoleEnum.java`
- Commit: `feat(gateway-contract): define engine roles`

### Step 2 — Extract the common runtime foundation

- Requirements: `REQ-002`, `REQ-004`
- Dependencies: Step 1
- Baseline state: provider, traffic, security, observability, and transport-common packages live inside the executable Engine; no runtime-core module exists.
- Observable outcome: both future executables can depend on a non-executable shared runtime without inheriting ingress or MCP state ownership.
- End state: runtime-core owns dependency-closed common packages/tests; the API Engine compiles through the library and retains protocol-specific code.
- Test-first gate: Required — the new boundary test initially fails because the module/package does not exist and common packages remain under Engine.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java/top/egon/cola/component/gateway/runtime/GatewayRuntimePackageBoundaryTest.java`

- Purpose: Define the reusable library's allowed packages and forbidden executable/Spring Boot entry points.
- Symbols: `GatewayRuntimePackageBoundaryTest`, `containsOnlySharedCapabilities`, `doesNotDependOnExecutableModules`.
- Repository evidence: `GatewayEnginePackageBoundaryTest` already uses package/dependency assertions for the Engine.
- Dependencies and consumers: runtime-core classes and Maven dependency graph; later both executable boundary tests.
- Why now: It is the RED architecture contract before any move.
- Contract/signature changes: Test-only allowlist for `common`, shared `http`, shared `rpc`, `operation`, and `rule`; at this Step only `common` is expected populated.
- Input/output and state mapping: classpath/package scan → violation list; empty violations is GREEN.
- Error and edge behavior: rejects application main classes, MCP packages, API ingress handlers, and dependency edges to either executable.
- Standards impact: `MC-ARCH-001`, `MC-DEP-001`, `MC-TEST-001`; makes architectural drift executable.
- Literal rule enforcement: Rule 5 closes dependency/import scope; Rule 11 prevents a hybrid or third executable structure.
- Implementation pseudocode:

```java
Set<String> packages = scanProductionPackages("top.egon.cola.component.gateway.runtime");
assertThat(packages).allMatch(ALLOWED_SHARED_PACKAGE_PREDICATE);
assertThat(resolveModuleDependencies()).doesNotContain("gateway-engine", "gateway-mcp-engine");
```

- Verification contribution: expected RED proves the extraction is not already present; GREEN becomes the reusable boundary gate.
- After this file: the test cannot pass until the module and moved packages exist.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/pom.xml`

- Purpose: Declare the non-executable shared runtime and only the dependencies needed by moved common capabilities.
- Symbols: Maven artifact `egon-cola-platform-gateway-runtime-core`; no repackage execution or main class.
- Repository evidence: gateway child modules inherit the same parent/dependency management; current Engine POM identifies the exact shared dependency closure.
- Dependencies and consumers: contract, core, DDC/RPC/security/runtime libraries already managed by the reactor; API and MCP executable POMs consume it.
- Why now: The target module must exist before Java packages can move and compile.
- Contract/signature changes: Adds an internal library artifact only.
- Input/output and state mapping: Maven dependency closure → library compile/test classpath; no runtime state.
- Error and edge behavior: Enforcer/dependency tests reject executable cycles and accidental Boot repackage.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`; dependencies are moved/reused, not newly invented.
- Literal rule enforcement: Rule 5 permits only already-managed libraries required by imports; Rule 11 establishes the selected platform module profile.
- Implementation pseudocode:

```xml
<artifactId>egon-cola-platform-gateway-runtime-core</artifactId>
<dependencies><!-- contract/core plus exact existing provider, Reactor, Micrometer, Jackson dependencies --></dependencies>
<!-- intentionally omit spring-boot-maven-plugin repackage and every executable artifact -->
```

- Verification contribution: enables the focused boundary/common tests and later dependency-tree inspection.
- After this file: Maven recognizes the library but common sources still need moving.

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/common`

- Purpose: Move common observability, provider, security, traffic, and transport capabilities plus pure tests to runtime-core; update all direct Engine production imports and McpHaRecoveryIT imports in this same commit. Consumer behavior changes remain in their later owning Steps.
- Symbols: all current `common/observability`, `common/provider`, `common/security`, `common/traffic`, `common/transport` production types; matching pure tests; `GatewayEngineRuntimeProperties` explicitly remains in API Engine.
- Repository evidence: these packages contain no API/RPC/MCP ingress ownership; cross-feature component tests are identifiable by imports and remain in Engine.
- Dependencies and consumers: API Engine configuration/handlers first, MCP Engine later; current contract/core/DDC/Redis/Micrometer dependencies.
- Why now: runtime-core must establish the common base before transport and rule seams move.
- Contract/signature changes: Java packages change from `.gateway.engine.common.{provider,security,traffic,transport,observability}` to `.gateway.runtime.{provider,security,traffic,transport,observability}`; behavior and method signatures stay unchanged.
- Input/output and state mapping: existing provider selections, policies, observations, traffic decisions, cancellation/timeouts → identical results after import change.
- Error and edge behavior: active/passive health, retry/commit, rate-limit failure mode, timeout, and cancellation tests must retain all branches; API-specific integration tests remain under Engine.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-SCOPE-001`; preserve named Beans and Lombok/constructor style while moving.
- Literal rule enforcement: Rule 1 preserves existing semantic names; Rule 4 preserves explicit Bean/qualifier wiring; Rule 5 adds no helper library; Rule 11 moves only the shared closure.
- Implementation pseudocode:

```text
move dependency-closed common production types and pure unit tests to gateway-runtime-core
rewrite package declarations and all API Engine imports without changing method bodies or policies
leave GatewayEngineRuntimeProperties and API-cross-feature component tests in gateway-engine
```

- Verification contribution: common unit tests plus API Engine compile prove behavioral reuse and complete imports.
- After this file: common capabilities have one owner and the mixed Engine still runs through runtime-core.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/pom.xml`

- Purpose: Register runtime-core in reactor/dependency management and make API Engine consume it.
- Symbols: Maven modules list, dependency management, API Engine dependency edge and package-boundary expectations.
- Repository evidence: parent currently lists contract/core/mcp-core/engine/admin/starters/test in dependency order.
- Dependencies and consumers: runtime-core POM and API Engine POM; later MCP Engine.
- Why now: Completes the module DAG after the move.
- Contract/signature changes: build graph only; no runtime/wire contract change.
- Input/output and state mapping: reactor order → compiled runtime-core before Engine.
- Error and edge behavior: dependency-tree gate fails on cycles, duplicate classes, or missing transitive dependencies.
- Standards impact: `MC-ARCH-001`, `MC-DEP-001`, `MC-SCOPE-001`; preserve existing Maven conventions.
- Literal rule enforcement: Rule 5 keeps the dependency allowlist exact; Rule 11 enforces contract/core/runtime/executable layering.
- Implementation pseudocode:

```xml
&lt;module&gt;egon-cola-platform-gateway-runtime-core&lt;/module&gt;
<dependencyManagement><!-- add runtime-core at the same project version --></dependencyManagement>
<!-- gateway-engine depends on runtime-core; runtime-core has no edge back to an executable -->
```

- Verification contribution: reactor compile and dependency tree prove correct direction.
- After this file: Step 2 has a compilable shared base and no Engine behavior change.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -q -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am '-Dtest=%regex[top/egon/cola/component/gateway/.*(Provider|Traffic|Security).*Test.class],GatewayRuntimePackageBoundaryTest,GatewayEnginePackageBoundaryTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; runtime boundary and moved common tests pass; API Engine compiles with no duplicate class.
- Failure returns to: File 3 for incomplete compile closure/imports; File 2/4 for dependency resolution or cycles.
- Completion criteria: runtime-core is non-executable, owns only shared common capabilities, and API Engine depends on it.
- Rollback: revert the Step commit as one rename/module unit.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java/top/egon/cola/component/gateway/runtime/GatewayRuntimePackageBoundaryTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/common`, `egon-cola-platforms/egon-cola-platform-gateway/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpHaRecoveryIT.java`
- Commit: `refactor(gateway): extract common runtime core`

### Step 3 — Generalize role-specific rule compilation and atomic activation

- Requirements: `REQ-006`
- Dependencies: Step 2
- Baseline state: `CompiledGatewayRules` and `EngineGatewayRuleCompiler` are concrete mixed-protocol types; activation services are coupled to them.
- Observable outcome: activation depends on a minimal compiled-rules DTO and compiler Strategy contract while the existing API compiler remains a compatible implementation.
- End state: runtime-core owns generic rule codec/repository/activation contracts; the current mixed compiler/record bridge those contracts without losing MCP behavior until the atomic split.
- Test-first gate: Required — generic activation tests initially fail because no `GatewayCompiledRulesDTO`/ `GatewayRuleCompilerStrategy` contract exists.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-BEAN-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 6, Rule 9, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java/top/egon/cola/component/gateway/runtime/rule/service/GatewayRuleCompilerStrategyTest.java`

- Purpose: Lock generic release identity, minimum accessors, validation, safe activation order, LKG restore, and ACK behavior.
- Symbols: `GatewayRuleCompilerStrategyTest`, test fixture `TestCompiledRulesDTO`, success/failure/LKG identity cases.
- Repository evidence: existing `GatewayRuleActivationApplierTest`, `GatewayRuleLkgRecoveryTest`, and compiler tests already cover atomic swap semantics against the concrete record.
- Dependencies and consumers: new DTO/Strategy, existing snapshot/repositories/applier; later both role compilers.
- Why now: It is the RED contract that makes the split safe without duplicating release logic.
- Contract/signature changes: Test-visible generic type parameter and identity invariants matching snapshot releaseId/artifact checksum plus DDC-owned version in activation status.
- Input/output and state mapping: snapshot/artifact → compiled DTO → validate/apply → local atomic reference/LKG/ACK; failed compile/apply preserves prior state.
- Error and edge behavior: mismatched identity, invalid snapshot, apply failure, corrupt LKG, and stale release never partially activate.
- Standards impact: `MC-VALID-001`, `MC-MODEL-001`, `MC-JSON-001`, `MC-PATTERN-001`, `MC-TEST-001`; tests every invariant rather than adding default interface logic.
- Literal rule enforcement: Rule 2 validates boundary and invariant failures; Rule 3 uses immutable record fixture; Rule 6 preserves snapshot JSON; Rule 9 proves Strategy variation and atomic template flow; Rule 11 keeps shared behavior in runtime-core.
- Implementation pseudocode:

```java
TestCompiledRulesDTO compiled = strategy.compile(snapshotWithIdentity("release-1", 7L, "sha256"));
assertThat(compiled.snapshot()).isSameAs(snapshot);
assertThatCode(() -> activationApplier.activate(compiled)).doesNotThrowAnyException();
assertThat(active.get().releaseId()).isEqualTo("release-1");
```

- Verification contribution: focused RED/GREEN proves generic atomic activation and role-independent identity.
- After this file: test fails for missing contracts/generic activation.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java/top/egon/cola/component/gateway/runtime/rule/domain/GatewayCompiledRulesDTO.java`

- Purpose: Define the minimal immutable view consumed by shared activation and operation/provider capabilities.
- Symbols: `GatewayCompiledRulesDTO`, accessors `releaseId`, `ruleChecksum`, `snapshot`, `providerServices`, `providerPolicies`, `trafficPolicies`.
- Repository evidence: current shared consumers use snapshot plus provider/traffic fields; security/CORS/MCP maps are protocol-owned.
- Dependencies and consumers: gateway rule snapshot/core types, runtime provider/traffic services, both role-specific records.
- Why now: Shared services need a stable contract before concrete compiler extraction.
- Contract/signature changes: Adds internal interface; no default methods or business behavior.
- Input/output and state mapping: immutable compiled record supplies identity and read-only maps to activation/operation consumers.
- Error and edge behavior: concrete record compact constructors must reject null/mismatched identity; interface itself does not hide invalid data.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-JSON-001`; exact DTO suffix and minimal immutable accessors.
- Literal rule enforcement: Rule 1 requires `DTO`; Rule 2 leaves validation at construction; Rule 3 uses immutable role records; Rule 6 exposes existing snapshot rather than a parallel JSON model; Rule 11 is layer-local.
- Implementation pseudocode:

```java
public interface GatewayCompiledRulesDTO {
    String releaseId(); String ruleChecksum(); GatewayRuleSnapshot snapshot();
    Set<ProviderServiceKey> providerServices();
    Map<String, RuntimeProviderPolicy> providerPolicies();
    Map<String, RuntimeTrafficPolicy> trafficPolicies();
}
```

- Verification contribution: lets activation/operation tests compile against only the fields they require.
- After this file: shared consumers can be generically typed without protocol leakage.

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java/top/egon/cola/component/gateway/runtime/rule/service/GatewayRuleCompilerStrategy.java`

- Purpose: Provide the required role variation point for compiling one unified snapshot into role-local compiled state.
- Symbols: `GatewayRuleCompilerStrategy<T extends GatewayCompiledRulesDTO>`, `compile(GatewayRuleSnapshot)`.
- Repository evidence: current compiler is one concrete service with protocol-specific branches; two stable roles are now known.
- Dependencies and consumers: role enum, shared snapshot/DTO, API and MCP compiler implementations, role configurations.
- Why now: Activation and wiring need role selection without conditionals or duplicate orchestration.
- Contract/signature changes: Adds internal Strategy interface; no public API.
- Input/output and state mapping: unified snapshot → validated immutable compiled DTO selected through an explicit qualified Strategy Bean.
- Error and edge behavior: compiler exceptions abort activation and retain LKG; duplicate Bean/role selection is caught by context tests.
- Standards impact: `MC-NAME-001`, `MC-BEAN-001`, `MC-PATTERN-001`; exact Strategy suffix and explicit role selector.
- Literal rule enforcement: Rule 1 fixes Strategy naming; Rule 4 role implementations use named Beans; Rule 9 introduces Strategy only for real compiler variation; Rule 11 keeps contract in runtime service layer.
- Implementation pseudocode:

```java
public interface GatewayRuleCompilerStrategy<T extends GatewayCompiledRulesDTO> {
    T compile(GatewayRuleSnapshot snapshot);
    // qualified implementations validate their projection and return one immutable DTO
    // the interface contains no mode switch, registry lookup, default business logic, or executable dependency
}
```

- Verification contribution: pattern behavior and context tests can assert the exact named/qualified implementation without factory branches.
- After this file: role compilers have a stable shared contract.

#### File 4 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/adapter/json/GatewayRuleJsonCodec.java`

- Purpose: Move generic rule JSON codec, apply-stage/status, chunk/LKG repositories, activation/policy services to runtime-core; bridge the still-mixed compiled record/compiler to the new generic contracts.
- Symbols: shared `GatewayRuleJsonCodec`, `GatewayRuleActivationApplier`, `GatewayRuleApplierRegistrar`, `GatewayPolicyKeyCompiler`, repositories/status; transitional `CompiledGatewayRules implements GatewayCompiledRulesDTO` and `EngineGatewayRuleCompiler implements GatewayRuleCompilerStrategy<CompiledGatewayRules>`.
- Repository evidence: current rule tree separates codec/repository/apply services from `CompiledGatewayRules` and `EngineGatewayRuleCompiler` protocol projections.
- Dependencies and consumers: runtime provider/traffic/operation services; API configuration/runtime; MCP compiler next Step.
- Why now: Completes the generic seam while keeping current executable GREEN.
- Contract/signature changes: generic services accept `GatewayCompiledRulesDTO`; the existing record implements it while retaining HTTP/RPC/security/CORS/MCP fields; the existing compiler becomes a named Strategy bridge. API-only renaming/projection waits for Step 6.
- Input/output and state mapping: same snapshot → API compiled DTO → validate/apply/publish local state; shared fields feed existing services.
- Error and edge behavior: all existing invalid rule, chunk, LKG, partial apply, checksum, compiler and MCP branches remain covered; no intermediate Step drops MCP behavior.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-JSON-001`, `MC-PATTERN-001`; use a named transitional compiler Bean and qualified injection, then finalize API naming in Step 6.
- Literal rule enforcement: Rule 1 preserves transitional names until their responsibility actually changes; Rule 3 uses immutable record; Rule 4 named Bean/final qualified injection; Rule 6 keeps codec/wire snapshot; Rule 9 introduces Strategy without breaking the mixed bridge; Rule 11 separates only shared packages now.
- Implementation pseudocode:

```text
move codec, repositories, apply-stage/status, activation, registrar, policy-key and traffic-policy services to runtime-core
make CompiledGatewayRules implement the minimal DTO and EngineGatewayRuleCompiler implement the generic Strategy while retaining every current plane field/branch
enforce snapshot identity in the record constructor and update current mixed wiring/tests to the shared interfaces without renaming the role types yet
```

- Verification contribution: existing compiler/activation/LKG tests plus new generic test prove behavior and API compatibility.
- After this file: one executable still works, but compilation and activation are role-pluggable.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am '-Dtest=GatewayRuleCompilerStrategyTest,*GatewayRule*Test,*Lkg*Test' -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; generic and API compiler/activation/LKG tests pass; the existing mixed compiled type remains as the generic compatibility bridge until Step 6.
- Failure returns to: File 1 for missing behavior coverage; File 2/3 for interface breadth/identity; File 4 for migration/import/wiring failures.
- Completion criteria: runtime-core activation is role-neutral and the still-mixed executable remains GREEN through a temporary generic Strategy bridge.
- Rollback: revert the Step commit; no MCP compiler consumer exists before Step 5.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java/top/egon/cola/component/gateway/runtime/rule/service/GatewayRuleCompilerStrategyTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java/top/egon/cola/component/gateway/runtime/rule/domain/GatewayCompiledRulesDTO.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java/top/egon/cola/component/gateway/runtime/rule/service/GatewayRuleCompilerStrategy.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rule/adapter/json/GatewayRuleJsonCodec.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java/top/egon/cola/component/gateway/runtime/rule`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java`
- Commit: `refactor(gateway): generalize role rule activation`

### Step 4 — Extract reusable outbound transport and operation invocation

- Requirements: `REQ-004`, `REQ-005`
- Dependencies: Step 3
- Baseline state: runtime-core owns common capabilities, but HTTP listener/server, RPC outbound descriptors, and operation invocation remain tied to API Engine packages.
- Observable outcome: MCP can later reuse direct Provider invocation and safe listener/drain transport without depending on API Engine.
- End state: shared HTTP/RPC outbound/operation closure and pure tests live in runtime-core; API-specific handlers/listeners remain executable-owned.
- Test-first gate: Required — moved/import-adjusted tests initially fail because shared packages and generic collaborators do not exist in runtime-core.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 4, Rule 5, Rule 9, Rule 11
- Ordered files:

#### File 1 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/domain/GatewayInboundHttpRequest.java`

- Purpose: Move the Spec-listed HTTP listener/handler/outbound contract, operation-required upstream adapter, and minimum WebSocket peer/value compile closure to runtime-core with focused tests.
- Symbols: `GatewayInboundHttpRequest`, `GatewayHttpEngineProperties`, `HttpUpstreamRequest`, `HttpUpstreamAdapter`, `GatewayHttpDataPlaneHandler`, `GatewayHttpListener`, `GatewayOutboundHttpResponse`, `ReactorNettyHttpUpstreamAdapter`, and only listener-required WebSocket SPI/model types.
- Repository evidence: current HTTP packages separate common/domain/service/adapter from proxy/default/security/CORS/body handlers; server/listener compile imports define the exact closure.
- Dependencies and consumers: runtime common transport/security, Reactor Netty; API `GatewayHttpServer`/handlers and later MCP server/handler Adapter.
- Why now: Both public listeners need identical resource ownership, in-flight tracking, drain, and timeout safety.
- Contract/signature changes: package prefix becomes `.gateway.runtime.http`; listener, handler SPI, upstream request/response and WebSocket shared contracts remain behaviorally identical.
- Input/output and state mapping: inbound Reactor request/body → injected handler → committed response; cancellation/drain/ownership state remains unchanged.
- Error and edge behavior: body release, no-retry-after-commit, listener drain, stream timeout, and WebSocket close behavior remain covered; API route/security decisions do not move.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-SCOPE-001`; API server remains role-owned and MCP gets its Spec-defined server.
- Literal rule enforcement: Rule 1 preserves transport suffixes; Rule 4 preserves qualified handler wiring; Rule 5 uses existing Reactor utilities; Rule 11 moves only compile closure.
- Implementation pseudocode:

```text
move the listed inbound/property/upstream domain types, upstream adapter, handler SPI, listener, response, and minimum WebSocket SPI/model closure
rewrite API server/handler imports while retaining GatewayHttpServer, proxy, default, security, CORS, body logging, and route selection in gateway-engine
run listener, streaming, timeout, response, operation-upstream, and shared WebSocket model tests against the runtime package
```

- Verification contribution: focused transport tests prove byte ownership and server safety survived extraction.
- After this file: API Engine still owns `GatewayHttpServer` and API behavior while constructing the moved shared listener.

#### File 2 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/adapter`

- Purpose: Move only RPC descriptor/channel/provider-health outbound infrastructure required by direct operation invocation.
- Symbols: Spec-listed `RpcProviderActiveHealthProbe`, `RpcProviderChannelCache` plus operation compile-closure `ProtobufDescriptorRegistry` and `RawByteMarshaller`; `RpcProviderChannelKey` moves only if required by the cache's public signature.
- Repository evidence: RPC listener, forwarder, method index, slot, and API ingress types are separate and remain under Engine.
- Dependencies and consumers: shared provider health/directory and gRPC/protobuf; operation bridge; API RPC runtime.
- Why now: MCP operation invocation must resolve/call Providers without importing API RPC ingress.
- Contract/signature changes: shared types move to `.gateway.runtime.rpc`; no descriptor or RPC wire change.
- Input/output and state mapping: provider endpoint/descriptor → cached channel/marshaller → identical outbound call resources.
- Error and edge behavior: channel eviction, unhealthy providers, descriptor absence, cancellation, and close behavior remain in focused tests.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-UTIL-001`; no alternate RPC client is introduced.
- Literal rule enforcement: Rule 1 keeps Adapter/Registry/Cache/Key suffixes; Rule 5 reuses gRPC/protobuf; Rule 11 leaves ingress in API Engine.
- Implementation pseudocode:

```text
move descriptor registry, provider health probe, provider channel cache, raw marshaller, and channel key to runtime-core
update API RPC and operation imports while leaving listener, forwarding, method-index, slot, and data-plane ownership in gateway-engine
assert dependency and package scans contain no API listener type in runtime-core
```

- Verification contribution: RPC channel/descriptor tests and Engine compile prove the outbound-only boundary.
- After this file: runtime-core can perform RPC outbound preparation without exposing RPC ingress.

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/operation`

- Purpose: Move the existing operation transport, HTTP/RPC dynamic bridge, upstream adapter, and invoker service into runtime-core.
- Symbols: `DefaultGatewayOperationTransport`, `HttpRpcDynamicMessageBridge`, `HttpRpcUpstreamAdapter`, `EngineGatewayOperationInvoker`, package metadata and pure tests.
- Repository evidence: these types already invoke providers from compiled snapshot operations and are consumed by MCP; their Engine package placement is the coupling.
- Dependencies and consumers: runtime HTTP/RPC/provider capabilities, gateway core operation contract; API and MCP role configurations.
- Why now: It realizes direct Provider invocation before MCP runtime moves.
- Contract/signature changes: package prefix changes to `.gateway.runtime.operation`; public `GatewayOperationInvoker` contract remains unchanged.
- Input/output and state mapping: operation name/arguments plus snapshot descriptors → selected provider request → result/error, with no Engine loopback URL.
- Error and edge behavior: unknown operation, invalid arguments, descriptor mismatch, provider failure, timeout/cancellation, and error mapping remain tested.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-BEAN-001`, `MC-PATTERN-001`; direct invocation is simpler than Facade/self-call.
- Literal rule enforcement: Rule 1 preserves Adapter/Invoker naming; Rule 4 uses explicit named invoker wiring later; Rule 5 adds no client library; Rule 9 selects direct flow, not an unnecessary pattern; Rule 11 keeps shared application capability in runtime-core.
- Implementation pseudocode:

```text
move operation transport, dynamic-message bridge, upstream adapter, invoker, package metadata, and pure tests to runtime-core
replace concrete Engine package imports with runtime provider/http/rpc contracts while preserving GatewayOperationInvoker method behavior
assert tests call fake provider adapters directly and never configure an API Engine HTTP or RPC loopback endpoint
```

- Verification contribution: operation unit tests prove direct provider semantics and absence of self-call.
- After this file: both roles can later inject one shared direct-operation invoker.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -q -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine -am '-Dtest=GatewayDataBuffer*Test,GatewayHttp*Test,GatewayOutboundHttpResponseTest,GatewayWebSocketFrameTest,%regex[top/egon/cola/component/gateway/.*RpcProvider.*Test.class],HttpRpcDynamicMessageBridgeTest,HttpRpcUpstreamAdapterTest,EngineGatewayOperationInvokerTest,GatewayRuntimePackageBoundaryTest,GatewayEnginePackageBoundaryTest' -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; shared transport/operation tests and API Engine compile pass; runtime-core scan has no ingress package.
- Failure returns to: File 1/2 for incomplete compile closure; File 3 for direct-invocation or snapshot coupling.
- Completion criteria: runtime-core owns reusable transport/outbound operation only; API Engine remains the sole API/RPC ingress owner.
- Rollback: revert the Step commit as one rename/import unit.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/domain/GatewayInboundHttpRequest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/rpc/adapter`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/operation`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/main/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/src/test/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/pom.xml`
- Commit: `refactor(gateway): share outbound runtime transport`

### Step 5 — Establish the MCP role compiler and bootstrap contracts

- Requirements: `REQ-003`, `REQ-006`, `REQ-011`
- Dependencies: Step 4
- Baseline state: shared Strategy/activation exists, but there is no MCP artifact, role DTO, role compiler, or executable bootstrap properties.
- Observable outcome: the reactor contains an MCP module with tested immutable compiled/config contracts, without yet duplicating or moving runtime Beans.
- End state: MCP compiler projects only MCP/provider/traffic/operation data from the unified snapshot; bootstrap properties bind new keys while legacy MCP properties remain unmoved until Step 6.
- Test-first gate: Required — compiler and binding tests fail because the module, DTO, Strategy, and properties do not exist.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 7, Rule 9, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/rule/service/McpGatewayRuleCompilerStrategyTest.java`

- Purpose: Define MCP-only snapshot projection, identity invariants, role selection, and exclusion of API HTTP/RPC ingress structures.
- Symbols: `McpGatewayRuleCompilerStrategyTest`, compile/identity/invalid-rule/exclusion cases.
- Repository evidence: current `EngineGatewayRuleCompilerTest` contains MCP rule fixtures; annotation-managed design fixes tools/operations as snapshot source.
- Dependencies and consumers: shared snapshot/Strategy/DTO and MCP core rule types; future MCP runtime.
- Why now: It is the RED contract for the second Strategy before runtime ownership moves.
- Contract/signature changes: Test-visible `McpGatewayCompiledRulesDTO` accessors and named MCP compiler Strategy.
- Input/output and state mapping: one unified snapshot → MCP routes/tools/provider/traffic/operation view with identical release identity.
- Error and edge behavior: missing managed operation, invalid tool mapping, identity mismatch, and API-only routes do not create partial MCP state.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-PATTERN-001`, `MC-TEST-001`; enforces immutable DTO and Strategy.
- Literal rule enforcement: Rule 1 locks DTO/Strategy names; Rule 2 tests invalid snapshot boundary; Rule 3 immutable construction; Rule 9 proves MCP Strategy; Rule 11 keeps MCP projection in MCP module.
- Implementation pseudocode:

```java
McpGatewayCompiledRulesDTO compiled = compiler.compile(snapshotWithManagedMcpTools());
assertThat(compiled.releaseId()).isEqualTo(compiled.snapshot().releaseId());
assertThat(compiled.mcpRules()).containsOnlyKeys("app-a");
assertThat(context.getBean("mcpGatewayRuleCompilerStrategy")).isSameAs(compiler);
```

- Verification contribution: focused RED/GREEN proves one unified snapshot and MCP-only compiled ownership.
- After this file: test fails for missing MCP module types.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/config/McpGatewayEnginePropertiesTest.java`

- Purpose: Lock immutable bootstrap binding/defaults, new prefix, validation, and separation from retained legacy MCP settings.
- Symbols: `McpGatewayEnginePropertiesTest`, valid binding, default ports, missing identity, invalid port/data-directory, unknown legacy-remap cases.
- Repository evidence: existing Engine property tests use Spring Binder/context runners; current MCP properties retain `egon.cola.component.gateway.engine.mcp`.
- Dependencies and consumers: `McpGatewayEngineProperties`, Spring Boot configuration properties/validation; Step 6 configuration/runtime.
- Why now: Configuration compatibility is a prerequisite for moving runtime Beans.
- Contract/signature changes: Defines new internal prefix `egon.cola.component.gateway.mcp-engine` and defaults `18084`/ `18085`; no existing key is renamed.
- Input/output and state mapping: YAML/env bootstrap fields → immutable nested records; retained `GATEWAY_MCP_*` remains outside this type.
- Error and edge behavior: blank group/env/node, invalid ports, unsafe/blank data directory, and invalid durations fail binding before listener startup.
- Standards impact: `MC-VALID-001`, `MC-MODEL-001`, `MC-CONFIG-001`; Jakarta validation and compact-constructor invariants are explicit.
- Literal rule enforcement: Rule 2 applies boundary validation; Rule 3 uses records; Rule 7 fixes base/operations key structure; Rule 11 keeps role config in executable.
- Implementation pseudocode:

```java
McpGatewayEngineProperties properties = bind("egon.cola.component.gateway.mcp-engine", validValues());
assertThat(properties.listener().port()).isEqualTo(18084);
assertThat(properties.managementPort()).isEqualTo(18085);
assertThatThrownBy(() -> bind(prefix, valuesWithoutNodeId())).isInstanceOf(BindException.class);
```

- Verification contribution: binding tests distinguish compatibility keys from new bootstrap keys.
- After this file: properties contract is fixed before resources and Beans are moved.

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/pom.xml`

- Purpose: Introduce the MCP artifact with compiler-test dependencies first, without Boot repackage until Step 6 supplies a main class.
- Symbols: artifact `egon-cola-platform-gateway-mcp-engine`; dependencies on contract/core/mcp-core/runtime-core.
- Repository evidence: gateway parent manages sibling artifact versions and test plugins; no executable-to-executable edge is needed.
- Dependencies and consumers: shared runtime and MCP core; later Spring Boot/JDBC/Redis/RBAC3/IdP dependencies move from API Engine.
- Why now: Compiler/property tests need an isolated ownership module.
- Contract/signature changes: new internal Maven artifact; no published/runtime endpoint yet.
- Input/output and state mapping: reactor dependencies → compile/test classpath.
- Error and edge behavior: dependency tree must reject API Engine dependency and duplicate classes.
- Standards impact: `MC-ARCH-001`, `MC-DEP-001`, `MC-SCOPE-001`; no new library other than existing managed dependencies.
- Literal rule enforcement: Rule 5 reuses existing coordinates only; Rule 11 creates the approved second executable boundary, initially non-repackaged.
- Implementation pseudocode:

```xml
<artifactId>egon-cola-platform-gateway-mcp-engine</artifactId>
<dependencies><!-- contract, core, mcp-core, runtime-core and focused test dependencies --></dependencies>
<!-- Step 5 deliberately has no repackage execution; Step 6 adds the main class atomically -->
```

- Verification contribution: module-level tests and dependency tree establish correct DAG before runtime move.
- After this file: MCP module compiles as a library slice only.

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/rule/domain/McpGatewayCompiledRulesDTO.java`

- Purpose: Implement the immutable MCP role projection and compiler Strategy.
- Symbols: record `McpGatewayCompiledRulesDTO`; named Bean `mcpGatewayRuleCompilerStrategy`; class `McpGatewayRuleCompilerStrategy`.
- Repository evidence: current mixed compiled record already has `mcpRules`; shared DTO defines identity/provider/traffic/snapshot accessors.
- Dependencies and consumers: `GatewayCompiledRulesDTO`, managed MCP rule contracts, operation/provider services; Step 6 activation/runtime.
- Why now: It makes the second role's compilation behavior independently testable.
- Contract/signature changes: internal MCP record/Strategy only; identity accessors must equal snapshot values.
- Input/output and state mapping: snapshot managed tool/capability/routing data → immutable MCP maps plus shared provider/traffic maps.
- Error and edge behavior: compact constructor rejects null/mismatched identity/maps; compiler rejects unmanaged/manual local draft semantics.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-BEAN-001`, `MC-PATTERN-001`; explicit Bean name and Strategy role.
- Literal rule enforcement: Rule 1 exact DTO/Strategy suffixes; Rule 2 validates constructor/compiler boundary; Rule 3 record; Rule 4 named Strategy Bean with Lombok only if collaborators require it; Rule 9 Strategy variation; Rule 11 MCP-owned package.
- Implementation pseudocode:

```java
@Service("mcpGatewayRuleCompilerStrategy")
final class McpGatewayRuleCompilerStrategy implements GatewayRuleCompilerStrategy<McpGatewayCompiledRulesDTO> {
    public McpGatewayCompiledRulesDTO compile(GatewayRuleSnapshot snapshot) { return projectManagedMcpState(snapshot); }
    // fixed MCP role metadata is contributed by MCP configuration, not selected through compiler input
}
```

- Verification contribution: makes Files 1–2 GREEN and supplies Step 6's role-local activation type.
- After this file: compiler/config contracts exist with no duplicate runtime Bean.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/pom.xml`

- Purpose: Register the MCP module after runtime-core and before Admin/test modules.
- Symbols: gateway parent modules/dependency management.
- Repository evidence: reactor ordering is explicit in the parent POM.
- Dependencies and consumers: MCP POM, later test-suite dependency.
- Why now: Completes Step 5 compilation and future artifact resolution.
- Contract/signature changes: Maven graph only.
- Input/output and state mapping: parent reactor → compiler module built after prerequisites.
- Error and edge behavior: reactor validation detects duplicate module or incorrect order.
- Standards impact: `MC-ARCH-001`, `MC-DEP-001`, `MC-SCOPE-001`.
- Literal rule enforcement: Rule 5 version/dependency management is reused; Rule 11 preserves the approved module hierarchy.
- Implementation pseudocode:

```xml
&lt;module&gt;egon-cola-platform-gateway-runtime-core&lt;/module&gt;
&lt;module&gt;egon-cola-platform-gateway-engine&lt;/module&gt;
&lt;module&gt;egon-cola-platform-gateway-mcp-engine&lt;/module&gt;
```

- Verification contribution: reactor build proves correct module order and no cycle.
- After this file: Step 5 is compile/test complete and intentionally not deployable.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine -am -Dtest=McpGatewayRuleCompilerStrategyTest,McpGatewayEnginePropertiesTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; compiler and binding tests pass; dependency tree contains runtime-core but not API Engine.
- Failure returns to: File 1/4 for compiler semantics; File 2/4 for config binding; File 3/5 for reactor/dependency issues.
- Completion criteria: MCP role has isolated compiled/config contracts and one Strategy; no production MCP runtime moved or duplicated yet.
- Rollback: revert the Step commit; API Engine remains fully functional.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/rule/service/McpGatewayRuleCompilerStrategyTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/config/McpGatewayEnginePropertiesTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/rule/domain/McpGatewayCompiledRulesDTO.java`, `egon-cola-platforms/egon-cola-platform-gateway/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/rule/service/McpGatewayRuleCompilerStrategy.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/config/McpGatewayEngineProperties.java`
- Commit: `feat(gateway): establish mcp engine compiler`

### Step 6 — Split the mixed executable into API_RPC and MCP runtimes

- Requirements: `REQ-002`, `REQ-003`, `REQ-004`, `REQ-005`, `REQ-006`, `REQ-010`, `REQ-011`
- Dependencies: Step 5
- Baseline state: MCP compiler/config contracts exist, but the existing Engine still owns MCP handlers/stores/security/tasks/configuration and its composite HTTP handler.
- Observable outcome: two independently runnable Spring Boot jars own disjoint ingress/runtime state and consume the same release artifact through runtime-core.
- End state: API Engine contains API/HTTP/WS/RPC only; MCP Engine contains all moved MCP runtime, state/security adapters, direct operation invocation, role-local activation/LKG/ACK, resources, health and Dockerfile.
- Test-first gate: Required — context/ownership tests initially fail because API still exposes MCP Beans/dependencies and MCP has no main/config/runtime/server.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 4, Rule 5, Rule 6, Rule 7, Rule 9, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/bootstrap/McpGatewayEngineContextTest.java`

- Purpose: Define positive MCP Bean ownership, negative API/RPC ingress ownership, stable Bean names/qualifiers, and role metadata.
- Symbols: `McpGatewayEngineContextTest`, `startsMcpOnlyContext`, `exposesDirectOperationInvoker`, `rejectsApiRpcIngressBeans`, `publishesMcpRoleMetadata`.
- Repository evidence: current `GatewayEngineConfigurationTest`, `GatewayEngineApplicationConfigurationTest`, and RBAC3 configuration tests use Spring context/Bean assertions.
- Dependencies and consumers: new MCP application/configuration/runtime, moved MCP services/adapters, DDC metadata.
- Why now: It is the RED executable boundary contract before runtime move.
- Contract/signature changes: fixes internal Bean names including `mcpGatewayEngineRuntime`, `mcpGatewayHttpServer`, `mcpGatewayRuleCompilerStrategy` and direct operation invoker qualifier.
- Input/output and state mapping: validated properties/snapshot/stores → MCP Beans and role-local state; no API handler/RPC listener.
- Error and edge behavior: missing credentials/store/listener settings fail context; optional remote/approval features obey existing conditionals; API Beans remain absent.
- Standards impact: `MC-BEAN-001`, `MC-CONFIG-001`, `MC-ARCH-001`, `MC-TEST-001`; tests explicit Bean identity and ownership.
- Literal rule enforcement: Rule 4 locks names/qualifiers/constructor wiring; Rule 7 loads both profiles; Rule 11 proves the MCP executable boundary.
- Implementation pseudocode:

```java
contextRunner.withPropertyValues(validMcpProperties()).run(context -> {
    assertThat(context).hasSingleBean(McpGatewayEngineRuntime.class).hasSingleBean(McpGatewayHttpServer.class);
    assertThat(context).doesNotHaveBean(DefaultGatewayHttpDataPlaneHandler.class).doesNotHaveBean(RpcGatewayServer.class);
    assertThat(roleMetadata(context)).containsEntry("gateway.engine.role", "MCP");
});
```

- Verification contribution: positive/negative context proof for the new executable.
- After this file: test fails because the MCP runtime does not exist.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEngineConfigurationTest.java`

- Purpose: Convert the existing context test into an API_RPC-only ownership and dependency contract.
- Symbols: API context tests for HTTP/RPC/WS presence, MCP/JDBC absence, explicit role metadata and named compiler/runtime Beans.
- Repository evidence: the test currently validates mixed Engine Beans and is the correct regression owner.
- Dependencies and consumers: API application/configuration/runtime after cleanup.
- Why now: It pairs the MCP positive test with the API negative contract before the atomic move.
- Contract/signature changes: test expectations remove all MCP Beans and datasource; role becomes `API_RPC`.
- Input/output and state mapping: API properties/snapshot → API listeners/compiler/runtime only.
- Error and edge behavior: startup fails for API listener/RPC configuration errors but cannot demand MCP store/security properties.
- Standards impact: `MC-ARCH-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-TEST-001`.
- Literal rule enforcement: Rule 4 asserts named/qualified Beans; Rule 7 asserts API profile contains no MCP keys; Rule 11 enforces API executable ownership.
- Implementation pseudocode:

```java
contextRunner.withPropertyValues(validApiRpcProperties()).run(context -> {
    assertThat(context).hasSingleBean(GatewayHttpListener.class).hasSingleBean(RpcGatewayServer.class);
    assertThat(context).doesNotHaveBean(McpEngineHttpHandler.class).doesNotHaveBean(DataSource.class);
    assertThat(roleMetadata(context)).containsEntry("gateway.engine.role", "API_RPC");
});
```

- Verification contribution: expected RED exposes every mixed Bean/config dependency that must be removed.
- After this file: API test fails until Files 3–6 complete the split.

#### File 3 — `RENAME egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp`

- Purpose: Move the complete MCP adapter/domain/service slice and its MCP-focused tests to the MCP executable with package ownership changes only.
- Symbols: file/artifact, task-token/task-store, telemetry, Redis session, remote client/auth, approval/RBAC3 adapters, `McpRuntimeProperties`, audit/HTTP handler/identity/health/task worker/services and all current MCP tests.
- Repository evidence: the current feature-first `engine/mcp` package has exactly the runtime state/security/transport slice named by the Spec.
- Dependencies and consumers: mcp-core, runtime-core operation/provider, Redis/JDBC/PostgreSQL, RBAC3/IdP, Micrometer/Kafka; MCP configuration.
- Why now: Runtime ownership must move as one unit to avoid duplicate Beans, tables, sessions, or task workers.
- Contract/signature changes: package prefix becomes `.gateway.mcp.engine.mcp`; public MCP routes, JSON, property prefix, errors, metrics, task/session semantics stay unchanged.
- Input/output and state mapping: MCP requests/session/task/approval/capability → same Redis/PostgreSQL/artifact/remote state and direct provider operation results.
- Error and edge behavior: cross-node sessions, task recovery, token isolation, approval/RBAC3, subscription HA, telemetry security, LKG, and conformance tests all move and retain behavior.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-UTIL-001`, `MC-JSON-001`; existing libraries/behavior move without redesign.
- Literal rule enforcement: Rule 1 preserves suffixes; Rule 4 business Beans become explicitly named/qualified with `@Slf4j`/`@RequiredArgsConstructor`; Rule 5 dependencies move from API POM; Rule 6 wire contracts unchanged; Rule 9 direct invocation via Adapter/Strategy; Rule 11 single MCP owner.
- Implementation pseudocode:

```text
move every production type and MCP-focused test under engine/mcp to the matching mcp/engine/mcp package
rewrite imports to runtime-core and preserve the legacy MCP configuration prefix, route constants, JSON contracts, state keys, SQL, metrics, and errors
rename only role-specific configuration test names where needed; never retain a duplicate MCP production class in gateway-engine
```

- Verification contribution: moved unit/integration tests prove behavior while boundary tests prove singular ownership.
- After this file: MCP code has one source owner but still needs executable wiring/resources.

#### File 4 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/McpGatewayEngineApplication.java`

- Purpose: Add the MCP Boot entry point, named configuration/runtime, public server/handler adapter, role-local activation, health, and DDC role metadata.
- Symbols: `McpGatewayEngineApplication`, `McpGatewayEngineConfiguration`, `GatewayRuntimeConfiguration`, `McpGatewayEngineRuntime`, `McpGatewayHttpServer`, `McpGatewayHttpDataPlaneHandlerAdapter`.
- Repository evidence: existing application/configuration/runtime and API-owned `GatewayHttpServer` provide the lifecycle pattern; the moved listener/handler SPI provides shared transport behavior.
- Dependencies and consumers: moved MCP slice, compiler Strategy, runtime-core, DDC, properties/resources; Docker/process harness.
- Why now: Completes the independently runnable MCP artifact after ownership move.
- Contract/signature changes: new executable main class and internal named Beans; MCP public route behavior stays through the moved handler.
- Input/output and state mapping: bootstrap identity plus unified release snapshot → MCP compile/validate/atomic swap/LKG/ACK → public handler direct Provider invocation.
- Error and edge behavior: startup validation fails fast; listener drain and in-flight requests are bounded; compile/store/provider failure preserves LKG and publishes non-ready health/ACK without API fallback.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-PATTERN-001`; named Beans, qualified final fields, `@Slf4j`, `@RequiredArgsConstructor`, thin Adapter.
- Literal rule enforcement: Rule 1 exact class suffixes; Rule 2 validates config/context; Rule 3 immutable properties/compiled DTO; Rule 4 explicit Beans and qualifiers; Rule 9 Strategy plus handler Adapter; Rule 11 MCP-only bootstrap.
- Implementation pseudocode:

```java
@SpringBootApplication public class McpGatewayEngineApplication { public static void main(String[] args) { SpringApplication.run(...); } }
@Component("mcpGatewayEngineRuntime") @RequiredArgsConstructor @Slf4j final class McpGatewayEngineRuntime {
    void start() { restoreLkg(); subscribeUnifiedRelease(); compileValidateSwapAckMcpRole(); mcpGatewayHttpServer.start(); }
}
```

- Verification contribution: makes MCP context/runtime/LKG/direct-invocation tests GREEN.
- After this file: MCP has a complete executable call path independent from API Engine.

#### File 5 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/bootstrap/config/GatewayEngineConfiguration.java`

- Purpose: Remove MCP Beans/config/store dependencies and composite handler; finalize the API-only DTO/compiler names and retain API/RPC runtime, listener, RPC slot and DDC metadata.
- Symbols: `GatewayEngineConfiguration`, `GatewayEngineApplication`, `ApiRpcGatewayCompiledRulesDTO`, named `ApiRpcGatewayRuleCompilerStrategy`, API handler wiring.
- Repository evidence: current configuration contains a contiguous MCP Bean block and runtime starts API/RPC/MCP together.
- Dependencies and consumers: API handlers/listeners/RPC services, runtime-core configuration and API compiler.
- Why now: New MCP ownership is valid only when the old executable is clean in the same commit.
- Contract/signature changes: internal context removes MCP Beans/JDBC; `CompiledGatewayRules`/ `EngineGatewayRuleCompiler` become API-specific DTO/Strategy and drop MCP fields/branches; Engine artifact/main class and API/RPC ports/contracts remain.
- Input/output and state mapping: API properties/unified snapshot → API compile/atomic/LKG/ACK → HTTP/WS/RPC listeners; no MCP routing branch.
- Error and edge behavior: API compile/listener/provider failures preserve API LKG/ACK only; MCP failures cannot affect API process health.
- Standards impact: `MC-ARCH-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-CONFIG-001`, `MC-PATTERN-001`; explicit `apiRpcGatewayEngineRuntime` and qualified Strategy/server fields.
- Literal rule enforcement: Rule 1 API role naming; Rule 4 `@Slf4j`, named Bean/stereotype, `@RequiredArgsConstructor`, qualifiers; Rule 7 removes MCP key tree; Rule 9 API Strategy only; Rule 11 API ingress stays executable-owned.
- Implementation pseudocode:

```text
delete MCP Bean methods, datasource/store/security/task worker wiring, MCP handler branch, and composite data-plane handler
rename the transitional compiled record/compiler to ApiRpcGatewayCompiledRulesDTO and ApiRpcGatewayRuleCompilerStrategy, then remove MCP projection fields/branches
wire the API handler directly into the API-owned GatewayHttpServer and select the named API_RPC compiler Strategy
start/stop/health/ACK only HTTP, WebSocket, RPC, slot, API LKG, and DDC role metadata API_RPC
```

- Verification contribution: API context/package/dependency tests prove absence; API protocol tests prove preserved behavior.
- After this file: API Bean assembly and compiler projection are role-specific; lifecycle and obsolete Composite deletion follow next.

#### File 6 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/bootstrap/lifecycle/GatewayEngineRuntime.java`

- Purpose: Restrict the retained Engine lifecycle, readiness, drain, LKG and ACK flow to API HTTP/WebSocket, RPC server/slot, providers and the API compiler.
- Symbols: named `apiRpcGatewayEngineRuntime`, lifecycle `start`, `stop`, readiness/health and qualified final collaborators.
- Repository evidence: current SmartLifecycle starts/stops shared provider/rule components plus HTTP/RPC/slot; MCP readiness is currently composed through configuration/health.
- Dependencies and consumers: API configuration, shared activation/provider/runtime services, API server and RPC server/slot.
- Why now: Configuration cleanup is incomplete until process lifecycle no longer waits on or reports MCP state.
- Contract/signature changes: internal lifecycle dependencies become API-only; external API/RPC health behavior and Engine main class remain.
- Input/output and state mapping: API bootstrap/current release → API compile/LKG/atomic state/ACK → HTTP/RPC start/readiness; stop drains only API-owned ingress and shared local resources.
- Error and edge behavior: API listener/RPC/activation failure preserves API LKG and non-ready ACK; no MCP store/task/handler failure can participate in this process.
- Standards impact: `MC-ARCH-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-PATTERN-001`, `MC-SCOPE-001`; explicit named component, `@Slf4j`, `@RequiredArgsConstructor`, qualifiers.
- Literal rule enforcement: Rule 1 exact Runtime name; Rule 4 named Bean/logging/final qualified injection; Rule 9 uses the qualified API Strategy without mode branches; Rule 11 keeps API lifecycle in its executable.
- Implementation pseudocode:

```java
public void start() {
    restoreAndSubscribeWith(apiRpcGatewayRuleCompilerStrategy);
    startProviderDirectoryThenHttpAndRpcAndSlot();
    publishFixedRoleAndApiActivationMetadata(GatewayEngineRoleEnum.API_RPC);
}
```

- Verification contribution: runtime/context tests prove API readiness and shutdown no longer reference MCP state.
- After this file: the retained process lifecycle is fully API_RPC-only.

#### File 7 — `DELETE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/service/GatewayCompositeHttpDataPlaneHandler.java`

- Purpose: Remove the obsolete same-listener MCP-first dispatch after MCP obtains its own server.
- Symbols: `GatewayCompositeHttpDataPlaneHandler` and every construction/import/reference.
- Repository evidence: the class exists only to dispatch reserved MCP paths before the default API handler in the mixed process.
- Dependencies and consumers: API configuration currently constructs it; Step 6 Files 4–6 replace all consumers.
- Why now: Keeping it would preserve hidden MCP ownership and a possible fallback route in API Engine.
- Contract/signature changes: internal class deletion only; proxy keeps external MCP paths stable and directs them to MCP Engine.
- Input/output and state mapping: API listener now receives only `DefaultGatewayHttpDataPlaneHandler`; MCP requests never enter the API process.
- Error and edge behavior: API Engine must not fallback, redirect or proxy MCP reserved paths; package/context/search tests assert absence.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-SCOPE-001`, `MC-TEST-001`; removes obsolete indirection instead of retaining compatibility code.
- Literal rule enforcement: Rule 5 deletes rather than adds a pass-through utility; Rule 9 removes the no-longer-needed composite branch; Rule 11 enforces disjoint ingress ownership.
- Implementation pseudocode:

```text
delete GatewayCompositeHttpDataPlaneHandler after both configuration consumers have been replaced
search production and tests for every constructor, import, instanceof check, reserved-path branch, and Bean name
assert API context wires DefaultGatewayHttpDataPlaneHandler directly and MCP context wires McpGatewayHttpDataPlaneHandlerAdapter
```

- Verification contribution: negative context/package/search tests prove there is no same-process MCP fallback.
- After this file: the old mixed ingress seam no longer exists.

#### File 8 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/resources/application.yml`

- Purpose: Add MCP base/operations resources and Dockerfile while removing MCP/JDBC resources/dependencies from API Engine.
- Symbols: `application.yml`, `application-operations.yml`, MCP `Dockerfile`; modified API POM/YAML/Dockerfile.
- Repository evidence: API Engine already has base/operations resource conventions and a Boot Docker image; mixed YAML currently contains datasource/MCP/RBAC3 keys.
- Dependencies and consumers: properties binding, Maven repackage, Compose/process harness.
- Why now: Completes artifact packaging and configuration ownership after Java wiring.
- Contract/signature changes: MCP default data/management ports 18084/18085; old `GATEWAY_MCP_*` mappings preserved in MCP file; API loses MCP/database keys.
- Input/output and state mapping: environment/profile → role-specific properties/credentials/TLS/log/LKG; image → correct main jar.
- Error and edge behavior: operations overlay has key parity; missing secrets fail startup; no default cross-role credential reuse; no API datasource autoconfiguration.
- Standards impact: `MC-DEP-001`, `MC-CONFIG-001`, `MC-SCOPE-001`; existing managed starters move to MCP POM and Boot repackage is enabled there.
- Literal rule enforcement: Rule 5 moves existing dependencies without new helper libraries; Rule 7 enforces base/operations parity and old env compatibility; Rule 11 packages each role separately.
- Implementation pseudocode:

```yaml
egon.cola.component.gateway.mcp-engine:
  listener: { host: "0.0.0.0", port: 18084 }
  management-port: 18085
egon.cola.component.gateway.engine.mcp: ${GATEWAY_MCP_COMPATIBLE_BINDINGS}
management.server.port: 18085
```

- Verification contribution: configuration/context/package/dependency and jar inspection gates prove clean ownership.
- After this file: both role jars can be built independently; no service is started by this Step.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine -am '-Dtest=GatewayEngineConfigurationTest,GatewayEngineApplicationConfigurationTest,McpGatewayEngineContextTest,GatewayEnginePackageBoundaryTest,McpGatewayEnginePackageBoundaryTest,*Mcp*Test,*Lkg*Test' -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine -am package -DskipTests`
- Expected result: exit 0; both contexts and focused MCP/API tests pass; two repackaged jars exist; API dependency tree has no mcp-core/JDBC/PostgreSQL and neither executable depends on the other.
- Failure returns to: Files 1–2 for ownership expectations, File 3 for move/import behavior, Files 4–5 for lifecycle/wiring, File 6 for dependencies/resources/packaging.
- Completion criteria: the codebase contains one API_RPC executable and one MCP executable sharing runtime-core and one unified release identity, with disjoint Beans/state/config.
- Rollback: revert the entire Step commit; never deploy a mixed revision between Files 3–6.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/test/java/top/egon/cola/component/gateway/mcp/engine/bootstrap/McpGatewayEngineContextTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/test/java/top/egon/cola/component/gateway/engine/GatewayEngineConfigurationTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/mcp`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/java/top/egon/cola/component/gateway/mcp/engine/McpGatewayEngineApplication.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/bootstrap/config/GatewayEngineConfiguration.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/bootstrap/lifecycle/GatewayEngineRuntime.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine/src/main/java/top/egon/cola/component/gateway/engine/http/service/GatewayCompositeHttpDataPlaneHandler.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine/src/main/resources/application.yml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-runtime-core`, `egon-cola-platforms/egon-cola-platform-gateway/pom.xml`
- Commit: `refactor(gateway): split api rpc and mcp engines`

### Step 7 — Enforce role-complete consistency in the single Admin

- Requirements: `REQ-001`, `REQ-007`, `REQ-008`
- Dependencies: Step 1 and Step 6
- Baseline state: Admin projection checks release/version/checksum/READY ACK but neither requires both roles nor rejects unknown role metadata.
- Observable outcome: one Admin reports consistency only when every online node belongs to a known required role and all required roles are present on the same ready release.
- End state: named Strategy, qualified Clock/wiring, deterministic reasons, and unchanged projection/controller wire shape are covered.
- Test-first gate: Required — projection and controller tests initially fail for missing role detection and missing/unknown reason codes.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 4, Rule 6, Rule 9, Rule 10, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/application/projection/GatewayProjectionServiceTest.java`

- Purpose: Add missing-role, unknown-role, skew, not-ready, multi-replica, and fully consistent cases before service changes.
- Symbols: projection test methods and node fixtures with metadata `gateway.engine.role`.
- Repository evidence: existing tests construct node projections with release/version/checksum/ACK and inject a Clock.
- Dependencies and consumers: projection service and new role Strategy; controller/UI consume its result.
- Why now: It is the RED business acceptance for Admin consistency.
- Contract/signature changes: Existing projection fields remain; a node with missing role metadata uses `ROLE_MISSING`, an unrecognized value uses `ROLE_UNKNOWN`, and an entirely absent required role changes only the existing aggregate counts/`consistent` result.
- Input/output and state mapping: online node metadata/ACKs → per-node role state plus aggregate consistency/missing role reason.
- Error and edge behavior: zero nodes, duplicate replicas, one missing role, blank/unknown role, release/version/checksum skew, and non-ready ACK all remain inconsistent.
- Standards impact: `MC-VALID-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`, `MC-TEST-001`; deterministic fixtures and `Clock`.
- Literal rule enforcement: Rule 2 validates metadata boundary; Rule 6 preserves response fields; Rule 9 exercises Strategy branches; Rule 10 fixes time through injected Clock; Rule 11 stays in Admin service tests.
- Implementation pseudocode:

```java
GatewayRuntimeConsistencyVO projection = service.runtimeConsistency(groupWith(apiRpcReady(), mcpReady()));
assertThat(projection.consistent()).isTrue();
assertThat(service.runtimeConsistency(groupWith(apiRpcReady())).consistent()).isFalse();
assertThat(service.runtimeConsistency(groupWith(nodeWithRole("UNKNOWN"))).nodes().getFirst().reason()).isEqualTo("ROLE_UNKNOWN");
```

- Verification contribution: RED/GREEN proves role completeness is part of, not separate from, release consistency.
- After this file: tests fail under current release-only projection.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/runtime/service/GatewayEngineRoleConsistencyStrategy.java`

- Purpose: Isolate known-role parsing, required-role completeness, and unknown-role detection as the selected complex business Strategy.
- Symbols: named service `gatewayEngineRoleConsistencyStrategy`, methods `roleOf`, `missingRoles`, `hasUnknownRole`.
- Repository evidence: node metadata already reaches `GatewayProjectionService`; the canonical enum now lives in contract.
- Dependencies and consumers: `GatewayEngineRoleEnum`, DDC node projections, `GatewayProjectionService`.
- Why now: Role set variation is explicit and testable without adding a new Admin layer or state store.
- Contract/signature changes: internal Strategy class only; no controller signature.
- Input/output and state mapping: node metadata → optional enum/required-minus-present set/unknown flag; no persistence mutation.
- Error and edge behavior: null metadata/maps, blank role, unknown role, duplicates, offline filtering policy delegated consistently to projection input.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-LOG-001`, `MC-BEAN-001`, `MC-PATTERN-001`; explicit service name, `@Slf4j`, `@RequiredArgsConstructor` only if collaborators exist.
- Literal rule enforcement: Rule 1 exact Strategy suffix; Rule 2 strict metadata parsing; Rule 4 explicit named Bean/logging; Rule 9 Strategy handles required role variation; Rule 11 Admin application service placement.
- Implementation pseudocode:

```java
@Service("gatewayEngineRoleConsistencyStrategy") @Slf4j
public class GatewayEngineRoleConsistencyStrategy {
    Optional<GatewayEngineRoleEnum> roleOf(DdcManagementConfigClientInstance node) { return GatewayEngineRoleEnum.fromWire(node.metadata().get("gateway.engine.role")); }
    Set<GatewayEngineRoleEnum> missingRoles(List<DdcManagementConfigClientInstance> nodes) { return requiredRolesMinusKnownOnlineRoles(nodes); }
}
```

- Verification contribution: focused Strategy tests cover parsing/set logic independently from projection orchestration.
- After this file: role decision participant exists for service integration.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/runtime/service/GatewayProjectionService.java`

- Purpose: Integrate role completeness with existing release consistency and make Bean/Clock dependencies explicit.
- Symbols: named `gatewayProjectionService`, `@Slf4j`, `@RequiredArgsConstructor`, qualified final fields including role Strategy and `gatewayProjectionClock`.
- Repository evidence: service currently has handwritten constructors, `ObjectProvider<DdcManagementClient>`, repository/release service, and a Clock.
- Dependencies and consumers: Admin configuration, DDC client, release service/repository, controller, Strategy.
- Why now: RED tests have fixed aggregate behavior and reason precedence.
- Contract/signature changes: existing projection DTO/controller shape unchanged; consistency predicate adds known/present role conditions.
- Input/output and state mapping: existing release projection plus online node role set → same response with stricter `consistent` and deterministic reason.
- Error and edge behavior: missing/unknown node metadata marks that node not ready; an absent required role forces only aggregate `consistent=false`; existing offline/DDC unavailable/skew/not-ready handling remains ordered and tested.
- Standards impact: `MC-LOG-001`, `MC-BEAN-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-PATTERN-001`; named/qualified final constructor injection and Clock.
- Literal rule enforcement: Rule 4 named service, `@Slf4j`, `@RequiredArgsConstructor`, explicit qualifiers; Rule 6 no response rename; Rule 9 delegates role rule to Strategy; Rule 10 uses injected Clock; Rule 11 remains in Admin service layer.
- Implementation pseudocode:

```java
Set<GatewayEngineRoleEnum> missing = roleStrategy.missingRoles(onlineNodes);
List<GatewayEngineNodeConsistencyVO> nodes = evaluateExistingNodeRulesAndRoleMetadata(onlineNodes, clock.instant());
boolean rolesComplete = missing.isEmpty() && !roleStrategy.hasUnknownRole(onlineNodes);
return existingWireShape(nodes, rolesComplete && everyOnlineNodeReady(nodes), clock.instant());
```

- Verification contribution: projection tests observe integrated predicate and preserved release rules.
- After this file: Admin service behavior is role-complete but configuration/controller contract still needs proof.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/bootstrap/GatewayAdminConfiguration.java`

- Purpose: Provide named projection Clock/wiring and add a golden controller contract test without production controller changes.
- Symbols: Bean `gatewayProjectionClock`; updated `GatewayAdminConfigurationTest`; new `GatewayProjectionControllerContractTest`.
- Repository evidence: existing `gatewayOpenApiClock` is conditional; controller already returns the projection DTO and need not change.
- Dependencies and consumers: projection service and Admin Spring context/controller.
- Why now: Completes Rule 4/10 wiring and proves external Admin API compatibility.
- Contract/signature changes: internal Clock Bean only; controller JSON fields/status remain identical, with metadata map retained.
- Input/output and state mapping: system UTC Clock → deterministic service timestamp; projection → unchanged JSON golden structure.
- Error and edge behavior: context has exactly one projection Clock qualifier; controller test covers unavailable/inconsistent response without new status mapping.
- Standards impact: `MC-BEAN-001`, `MC-JSON-001`, `MC-TIME-001`, `MC-TEST-001`; explicit Bean name and golden wire test.
- Literal rule enforcement: Rule 4 explicit Bean/qualifier; Rule 6 golden JSON compatibility; Rule 10 `Clock.systemUTC()` and java.time only; Rule 11 Admin configuration/controller-test placement.
- Implementation pseudocode:

```java
@Bean("gatewayProjectionClock") Clock gatewayProjectionClock() { return Clock.systemUTC(); }
mockMvc.perform(get(existingProjectionPath)).andExpect(status().isOk())
        .andExpect(jsonPath("$.value[0].metadata['gateway.engine.role']").value("API_RPC"));
```

- Verification contribution: context and MockMvc tests prove DI stability and unchanged wire shape.
- After this file: one Admin consistently projects both roles through the existing endpoint.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin -am -Dtest=GatewayProjectionServiceTest,GatewayEngineRoleConsistencyStrategyTest,GatewayProjectionControllerContractTest,GatewayAdminConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; all consistency, context, and controller golden tests pass.
- Failure returns to: File 1 for rule precedence/fixtures, File 2 for role parsing, File 3 for integration, File 4 for DI/wire compatibility.
- Completion criteria: Admin requires both known roles and preserves all existing release/wire behavior.
- Rollback: revert the Step commit; Engine artifacts remain independent but Admin reverts to release-only visibility.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/application/projection/GatewayProjectionServiceTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/runtime/service/GatewayEngineRoleConsistencyStrategy.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/runtime/service/GatewayProjectionService.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/main/java/top/egon/cola/component/gateway/admin/bootstrap/GatewayAdminConfiguration.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/application/projection/GatewayEngineRoleConsistencyStrategyTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/interfaces/management/GatewayProjectionControllerContractTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/src/test/java/top/egon/cola/component/gateway/admin/GatewayAdminConfigurationTest.java`
- Commit: `feat(gateway-admin): enforce dual role consistency`

### Step 8 — Render API_RPC and MCP role state in Admin Web

- Requirements: `REQ-007`, `REQ-008`
- Dependencies: Step 7
- Baseline state: the shared Admin Web client preserves a generic node projection but does not type or render Engine role completeness separately.
- Observable outcome: group detail and MCP runtime status show independent API_RPC/MCP availability, release/ACK state, and missing/unknown/skew reasons through the existing endpoint.
- End state: no new endpoint/query key; loading, empty, denied, error, partial, skew, and healthy states have component tests.
- Test-first gate: Required — new component tests fail because role metadata is untyped and pages do not select/render both roles.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-JSON-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 3, Rule 6, Rule 11
- Ordered files:

#### File 1 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupDetailPage.test.tsx`

- Purpose: Define two-role rendering and all operator-visible projection states before page changes.
- Symbols: tests for API_RPC/MCP cards, replicas, release/checksum/ACK, missing/unknown/skew, loading/error/denied/empty.
- Repository evidence: existing React Testing Library/Vitest page tests and gateway query mocks establish fixtures/assertion style.
- Dependencies and consumers: `GatewayGroupDetailPage`, gateway API types/query; operators.
- Why now: It is the RED UI acceptance for the Admin projection already fixed in Step 7.
- Contract/signature changes: Test fixtures carry metadata `gateway.engine.role`; no API path/query key change.
- Input/output and state mapping: projection nodes/reason/consistency → grouped role cards/badges/messages.
- Error and edge behavior: absent metadata renders unknown, missing role remains visible, replicas aggregate without hiding skew, denied/error states do not show stale healthy status.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-JSON-001`, `MC-TEST-001`; typed role union and exhaustive view states.
- Literal rule enforcement: Rule 1 semantic role/type names; Rule 2 validates unknown metadata at UI boundary; Rule 3 immutable fixtures; Rule 6 consumes existing JSON fields; Rule 11 follows current React page/test structure.
- Implementation pseudocode:

```tsx
renderPageWithProjection(nodesFor("API_RPC", "MCP"), { consistent: true });
expect(screen.getByRole("heading", { name: "API / RPC Engine" })).toBeVisible();
expect(screen.getByRole("heading", { name: "MCP Engine" })).toBeVisible();
expect(renderMissingRole("MCP")).toShowWarning("Missing MCP Engine role");
```

- Verification contribution: RED/GREEN proves operator-visible role separation without browser automation.
- After this file: tests fail under the current single-status page.

#### File 2 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRuntimeStatus.test.tsx`

- Purpose: Define MCP-specific status selection without creating a second backend request or deriving health from API role.
- Symbols: MCP healthy/missing/skew/not-ready/unknown/loading/error tests.
- Repository evidence: current `McpRuntimeStatus` consumes the gateway projection query and shared status components.
- Dependencies and consumers: typed metadata and existing projection query.
- Why now: Prevents accidental reuse of API node state as MCP status.
- Contract/signature changes: component behavior only; props/query remain existing.
- Input/output and state mapping: same projection node list → filter exact `MCP` role → MCP status view.
- Error and edge behavior: multiple replicas aggregate, unknown role never counts as MCP, missing MCP reports unavailable even if API is healthy.
- Standards impact: `MC-VALID-001`, `MC-JSON-001`, `MC-TEST-001`; strict role discriminator.
- Literal rule enforcement: Rule 2 handles missing/unknown boundary; Rule 6 maps existing metadata; Rule 11 stays in current page component.
- Implementation pseudocode:

```tsx
mockGatewayQueries({ nodes: fixtureWithRoleReplicas({ API_RPC: 2, MCP: 0 }), consistent: false });
render(<McpRuntimeStatus gatewayGroupId="group-1" />);
expect(screen.getByText("MCP Engine unavailable")).toBeVisible();
expect(screen.queryByText("Healthy")).not.toBeInTheDocument();
```

- Verification contribution: focused tests prove exact MCP filtering and negative states.
- After this file: component behavior is fixed before implementation.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`

- Purpose: Type the existing node metadata role discriminator and preserve it through gateway API mapping.
- Symbols: `GatewayEngineRole`, required normalized `metadata: Record<string,string>`, role type guard/helper; existing query keys unchanged.
- Repository evidence: backend projection already returns metadata and gateway API mapping uses object spread, so no new fetch/mapping layer is necessary.
- Dependencies and consumers: group detail, MCP status, existing gateway API test.
- Why now: Pages need a safe discriminator after tests define behavior.
- Contract/signature changes: frontend-only type refinement; network response remains unchanged.
- Input/output and state mapping: nullable/raw metadata → normalized map → exact role union or unknown state.
- Error and edge behavior: missing/non-string/unknown values remain unknown and never default to a healthy role.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-MODEL-001`, `MC-JSON-001`; reuse existing mapping and avoid duplicate DTO.
- Literal rule enforcement: Rule 1 semantic type name; Rule 2 boundary normalization; Rule 3 readonly TypeScript model; Rule 6 exact JSON property; Rule 11 current API type layer.
- Implementation pseudocode:

```ts
export type GatewayEngineRole = 'API_RPC' | 'MCP';
export const gatewayEngineRoleOf = (metadata: Record<string, string>): GatewayEngineRole | undefined =>
  metadata['gateway.engine.role'] === 'API_RPC' || metadata['gateway.engine.role'] === 'MCP'
    ? metadata['gateway.engine.role'] as GatewayEngineRole : undefined;
// gatewayApi test asserts metadata survives the existing spread-based mapping
```

- Verification contribution: typecheck and gateway API test prove mapping compatibility.
- After this file: components can branch without unsafe casts or defaults.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupDetailPage.tsx`

- Purpose: Implement role grouping/rendering in group detail and MCP status using existing components/query state.
- Symbols: `GatewayGroupDetailPage`, `McpRuntimeStatus`, local pure role grouping/view helpers if consistent with nearby style.
- Repository evidence: both pages already consume projection state and render established cards/badges/empty/error components.
- Dependencies and consumers: typed helper, existing API hook/query key/status components.
- Why now: RED tests and types fix the minimum UI behavior.
- Contract/signature changes: visible role labels/status only; routes and backend calls unchanged.
- Input/output and state mapping: nodes grouped by canonical role → replica count/release/version/checksum/ACK and aggregate reason; unknown nodes remain explicitly visible.
- Error and edge behavior: preserve loading/error/denied/empty; do not collapse missing/skew/not-ready into generic healthy/unhealthy; no optional field guessing.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-VALID-001`, `MC-SCOPE-001`; reuse current UI primitives and avoid new state library.
- Literal rule enforcement: Rule 2 exhaustive state handling; Rule 6 existing contract fields only; Rule 11 current React page structure with no new architecture.
- Implementation pseudocode:

```tsx
const grouped = groupNodesByKnownRole(nodes.data ?? []);
return <>{renderRoleCard('API_RPC', grouped.API_RPC, consistency.data)}{renderRoleCard('MCP', grouped.MCP, consistency.data)}
  {renderUnknownRoleWarning(grouped.unknown)}</>;
// McpRuntimeStatus keeps its gatewayGroupId queries, selects grouped.MCP only, and preserves loading/error branches
```

- Verification contribution: component tests, typecheck, and build prove the complete UI path.
- After this file: Admin Web displays independent two-role state using one existing projection endpoint.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA/egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web`
- Verification command: `npm test -- src/features/gateway-groups/GatewayGroupDetailPage.test.tsx src/features/mcp/McpRuntimeStatus.test.tsx src/api/gatewayApi.test.ts && npm run typecheck && npm run lint && npm run build`
- Expected result: exit 0; focused tests, typecheck, and production build pass without staging unrelated shared-library build state.
- Failure returns to: Files 1–2 for acceptance fixtures, File 3 for contract typing/mapping, File 4 for rendering/state behavior.
- Completion criteria: both role states and all required error/partial conditions are visible; no new endpoint/query key exists.
- Rollback: revert the Step commit; backend role consistency remains available through the unchanged API.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupDetailPage.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRuntimeStatus.test.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/types.ts`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/gateway-groups/GatewayGroupDetailPage.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/features/mcp/McpRuntimeStatus.tsx`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin-web/src/api/gatewayApi.test.ts`
- Commit: `feat(gateway-admin-web): show dual engine roles`

### Step 9 — Upgrade the gateway test suite for dual-role releases

- Requirements: `REQ-006`, `REQ-008`, `REQ-012`
- Dependencies: Step 6 and Step 7
- Baseline state: the suite POM, process harness, live environment, MCP HA tests, and topology tests assume one Engine executable kind or one mixed base URL.
- Observable outcome: static/process/live fixtures resolve both jars and assert unified release identity with role-local lifecycle, LKG, ACK, lease, protocol ownership, and compatibility.
- End state: non-live focused tests run in the normal Maven gate; live tests are role-aware but remain disabled/user-controlled by profile/environment.
- Test-first gate: Required — harness/environment/topology contract tests fail because MCP artifact/base URL/role specs are absent.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-NAME-001`, `MC-VALID-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 1, Rule 2, Rule 5, Rule 6, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayProcessHarnessTest.java`

- Purpose: Define role-aware executable resolution, process specifications, ports, environment, health, and teardown without starting processes in the focused unit test.
- Symbols: `GatewayProcessHarnessTest`, `GatewayProcessSpec` role/main-class/artifact fields, artifact resolver cases for both engines.
- Repository evidence: current harness resolves the Engine jar/main class and provides probe/process lifecycle abstractions.
- Dependencies and consumers: two packaged artifacts, live topology tests, process probes.
- Why now: It is the RED test-infrastructure contract before live fixtures can be rewritten.
- Contract/signature changes: internal test harness adds explicit role/artifact/main-class/base-URL/management-URL fields.
- Input/output and state mapping: role process spec → exact jar/env/ports/probes → isolated process handle and logs.
- Error and edge behavior: missing/ambiguous jar, port collision, early exit, health timeout, and teardown are attributed to the correct role.
- Standards impact: `MC-NAME-001`, `MC-VALID-001`, `MC-CONFIG-001`, `MC-TEST-001`; role values come from contract enum.
- Literal rule enforcement: Rule 1 semantic process fields; Rule 2 validates missing/ambiguous artifacts; Rule 7 keeps environment keys role-specific; Rule 11 test infrastructure remains in suite.
- Implementation pseudocode:

```java
GatewayProcessSpec api = specFor(GatewayEngineRoleEnum.API_RPC, "gateway-engine", GatewayEngineApplication.class, 18081, 19090);
GatewayProcessSpec mcp = specFor(GatewayEngineRoleEnum.MCP, "gateway-mcp-engine", McpGatewayEngineApplication.class, 18084, 18085);
assertThat(harness.resolveJar(api)).isNotEqualTo(harness.resolveJar(mcp));
```

- Verification contribution: focused tests prove deterministic artifact/port/role ownership without live startup.
- After this file: tests fail until harness/spec support both executable kinds.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayProcessHarness.java`

- Purpose: Implement role-aware process specs/resolution and add the MCP test dependency to the suite POM.
- Symbols: `GatewayProcessHarness`, `GatewayProcessSpec`, `GatewayProcessProbe`, test-suite POM dependency on MCP Engine.
- Repository evidence: existing resolver is already main-class-aware and is the narrow extension point.
- Dependencies and consumers: both executable jars; all live tests.
- Why now: File 1 fixes the minimum infrastructure behavior.
- Contract/signature changes: test-only constructors/builders gain explicit role/artifact; no production API.
- Input/output and state mapping: spec → jar/command/env/log/probe; process lifecycle remains current.
- Error and edge behavior: no fallback from MCP artifact to API artifact; cleanup remains bounded and reports role-specific logs.
- Standards impact: `MC-REUSE-001`, `MC-DEP-001`, `MC-VALID-001`, `MC-SCOPE-001`; extends existing harness, adds only sibling test dependency.
- Literal rule enforcement: Rule 2 validates spec completeness; Rule 5 reuses Maven test dependency and JDK process APIs; Rule 11 no new test framework/layer.
- Implementation pseudocode:

```java
Path resolveJar(GatewayProcessSpec spec) {
    var matches = scanTargetForArtifactAndMainClass(spec.artifactId(), spec.mainClass());
    requireExactlyOne(matches, spec.role());
    return matches.getFirst();
}
```

- Verification contribution: makes process harness tests GREEN and unblocks role-aware live fixtures.
- After this file: suite can describe both processes without conflation.

#### File 3 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveEnvironment.java`

- Purpose: Split API and MCP live URLs/management/credentials/LKG inputs and update environment/contract/lease/topology fixtures.
- Symbols: `GatewayLiveEnvironment`, its unit test, `GatewayLiveTopologyContractTest`, `GatewayLiveTopologyEngineLeaseTest`, `GatewayLiveTopologyIT`.
- Repository evidence: current environment centralizes ports/credentials and topology tests construct two mixed Engine replicas.
- Dependencies and consumers: process harness, Admin/DDC/provider fixtures, two role executables.
- Why now: Live tests must encode the final ownership and identity rules before deployment files are changed.
- Contract/signature changes: test environment keys distinguish API and MCP process sets; old MCP route path remains.
- Input/output and state mapping: environment → API replicas + MCP replicas + one Admin/release/provider set; one artifact identity → both role-local ACKs.
- Error and edge behavior: missing role URL/credential, role skew, duplicate node ID, lease loss, one-role restart, one-role LKG restore, partial readiness and recovery are explicit.
- Standards impact: `MC-VALID-001`, `MC-JSON-001`, `MC-CONFIG-001`, `MC-TEST-001`; old wire paths and one release payload retained.
- Literal rule enforcement: Rule 2 validates environment completeness; Rule 6 keeps rule/Admin/MCP payloads; Rule 7 role-specific environment parity; Rule 11 live suite structure preserved.
- Implementation pseudocode:

```java
LiveTopology topology = topologyBuilder().apiRpcReplicas(2).mcpReplicas(2).singleAdmin().build();
publishOneRelease(topology.admin(), releaseArtifact);
awaitAllRoleAcks(topology, releaseArtifact.identity());
assertRoleSkewIsInconsistentWhenOneMcpReplicaUsesPreviousChecksum(topology);
```

- Verification contribution: static contract tests run now; live topology behavior is prepared for the later user-controlled gate.
- After this file: live fixture ownership matches the two-executable architecture.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpCompleteReleaseIT.java`

- Purpose: Point MCP release/HA/conformance/security tests to MCP Engine while retaining unified rule-wire and API/RPC compatibility tests.
- Symbols: `McpCompleteReleaseIT`, `McpHaRecoveryIT`, `GatewayRuleWireCompatibilityTest`, related MCP clients/base URL helpers.
- Repository evidence: current tests target MCP routes on the mixed Engine and already assert complete release/LKG/HA behavior.
- Dependencies and consumers: role-aware live environment and same release payload.
- Why now: Protocol proof must follow runtime ownership without weakening assertions.
- Contract/signature changes: test target URL changes only; paths, methods, JSON, errors, security headers, and rule bytes stay unchanged.
- Input/output and state mapping: same release artifact → API RPC/HTTP behavior plus MCP route behavior on separate base URLs; both report same identity.
- Error and edge behavior: MCP restart/LKG/remote token/session/subscription behavior remains independent from API replica restart; compatibility test compares unchanged bytes.
- Standards impact: `MC-JSON-001`, `MC-CONFIG-001`, `MC-TEST-001`; no golden contract update unless bytes are identical.
- Literal rule enforcement: Rule 6 preserves wire contracts byte-for-byte; Rule 7 selects role URLs through environment; Rule 11 tests remain in their protocol features.
- Implementation pseudocode:

```java
URI mcpBase = liveEnvironment.mcpDataPlaneBaseUri();
assertMcpInitializeToolsAndCallContracts(mcpBase, existingGoldenPayloads());
assertThat(fetchApiAndMcpAckIdentity()).containsOnly(expectedReleaseIdentity);
restartOnlyMcpReplicaAndAssertLkgRecoveryWithoutApiRestart();
```

- Verification contribution: non-live wire tests and later live profile prove protocol compatibility and independent recovery.
- After this file: suite covers both role ownership and single release semantics.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -Dtest=GatewayProcessHarnessTest,GatewayLiveEnvironmentTest,GatewayLiveTopologyContractTest,GatewayLiveTopologyEngineLeaseTest,GatewayRuleWireCompatibilityTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; all non-live harness/environment/topology/wire tests pass; live IT classes compile but do not start processes under the normal gate.
- Failure returns to: Files 1–2 for artifact/process modeling, File 3 for topology/identity, File 4 for protocol target/compatibility.
- Completion criteria: test suite represents two executable roles, one Admin/release, role-local ACK/LKG, and unchanged protocols.
- Rollback: revert the Step commit; production modules remain split but old suite is temporarily incompatible.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayProcessHarnessTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process/GatewayProcessHarness.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live/GatewayLiveEnvironment.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp/McpCompleteReleaseIT.java`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/pom.xml`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/process`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/live`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/mcp`
- Commit: `test(gateway): cover dual engine topology`

### Step 10 — Encode independent role deployment and stable data-plane routing

- Requirements: `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`
- Dependencies: Step 9
- Baseline state: Compose/overlays start identical mixed Engine replicas; MCP state/config is attached to them; existing HAProxy config fronts control plane only.
- Observable outcome: Compose defines independent API and MCP replicas, credentials, ports, health, LKG/state volumes, overlay parity, and stable Host/Path routing through a separate data-plane proxy.
- End state: configuration renders statically across base/HA/mTLS/HA-mTLS/demo; no containers are started in this Step.
- Test-first gate: Required — `GatewayComposeConfigurationTest` fails because MCP services/role metadata/proxy/routes/volumes and API MCP exclusions are absent.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-VALID-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`
- Literal Rules: Rule 2, Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/deployment/GatewayComposeConfigurationTest.java`

- Purpose: Define static deployment invariants for two API replicas, two MCP replicas, one Admin, independent identity/secrets/ports/volumes/probes, and stable routes.
- Symbols: Compose model assertions for base and every overlay; forbidden API MCP/database settings.
- Repository evidence: the existing test parses Compose/YAML and already asserts mixed Engine service configuration.
- Dependencies and consumers: deployment Compose and HAProxy files.
- Why now: It is the RED deployment contract before YAML changes.
- Contract/signature changes: test-only final topology; default replica examples are two per role while runtime contract remains `1..N`.
- Input/output and state mapping: rendered Compose/config → service/image/env/port/volume/health/router assertions.
- Error and edge behavior: duplicate node/port/credential, missing role, API database/MCP keys, shared LKG volume, overlay key drift, and control/data proxy collision fail the test.
- Standards impact: `MC-VALID-001`, `MC-CONFIG-001`, `MC-TEST-001`; static proof only.
- Literal rule enforcement: Rule 2 validates configuration boundary; Rule 7 enforces overlay parity; Rule 11 preserves deployment structure.
- Implementation pseudocode:

```java
ComposeModel model = parseAllGatewayComposeFiles();
assertIndependentRoleServices(model, "gateway-engine", "gateway-engine-2", "gateway-mcp-engine", "gateway-mcp-engine-2");
assertApiServicesContainNoMcpDatabaseSettings(model);
assertStableRoutes(model.dataPlaneProxy(), API_PATHS, MCP_PATHS);
```

- Verification contribution: RED/GREEN statically proves ownership and overlay parity without Docker startup.
- After this file: test fails against the current mixed topology.

#### File 2 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.yml`

- Purpose: Define base API_RPC/MCP services, distinct identities/credentials/ports/probes/LKG/state mounts, and dedicated data-plane proxy.
- Symbols: API services, MCP services, `gateway-data-plane-proxy`, networks/volumes/secrets/environment.
- Repository evidence: current base Compose already defines Admin, dependencies, mixed Engine, health checks, and test providers.
- Dependencies and consumers: both Dockerfiles, application properties, HAProxy config, scripts/readiness tests.
- Why now: File 1 fixes the exact rendered topology.
- Contract/signature changes: deployment service names/role metadata; public routes remain stable through proxy.
- Input/output and state mapping: Compose env/secret/volume → role-specific process and store; same DDC business/env/appCode with distinct instance/node/role.
- Error and edge behavior: API can be ready without MCP store health; MCP can restart/restore independently; no shared writable LKG directory or credentials.
- Standards impact: `MC-ARCH-001`, `MC-REUSE-001`, `MC-CONFIG-001`, `MC-SCOPE-001`; reuse images/networks/health patterns.
- Literal rule enforcement: Rule 5 no new application dependency; Rule 7 exact base key tree; Rule 11 one Admin/two executable roles.
- Implementation pseudocode:

```yaml
services:
  gateway-engine: { healthcheck: { test: API_MANAGEMENT_HEALTH } }
  gateway-mcp-engine: { ports: ["18084", "18085"], healthcheck: { test: MCP_MANAGEMENT_HEALTH } }
  gateway-data-plane-proxy: { volumes: ["./haproxy.data-plane.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro"] }
```

- Verification contribution: parsed base Compose proves separate ownership and wiring.
- After this file: base topology is correct; overlays and routes still need parity.

#### File 3 — `CREATE egon-cola-platforms/egon-cola-platform-gateway/deployment/haproxy.data-plane.cfg`

- Purpose: Route stable external API/RPC-compatible HTTP paths to API backends and MCP paths to MCP backends without changing the control-plane proxy.
- Symbols: frontend, API backend, MCP backend, health checks and ACLs for `/mcp/`, `/legacy/mcp/`, `/.well-known/oauth-protected-resource/mcp/`.
- Repository evidence: existing `haproxy.cfg` establishes image/syntax/health convention for Admin HA and remains untouched for data-plane rules.
- Dependencies and consumers: proxy service and role health/data ports.
- Why now: The two roles must remain externally compatible after physical split.
- Contract/signature changes: deployment routing only; Host/Path/API/MCP external contracts unchanged.
- Input/output and state mapping: incoming path/host → exact role backend pool → same response.
- Error and edge behavior: MCP well-known/legacy paths cannot fall through to API; API default cannot route to MCP; backend health removes only failed role replica.
- Standards impact: `MC-REUSE-001`, `MC-CONFIG-001`, `MC-SCOPE-001`; dedicated config isolates current control plane.
- Literal rule enforcement: Rule 7 route/config parity is test-backed; Rule 11 keeps proxy in deployment rather than application code.
- Implementation pseudocode:

```haproxy
acl is_mcp path_beg /mcp/ /legacy/mcp/ /.well-known/oauth-protected-resource/mcp/
use_backend gateway_mcp_engines if is_mcp
default_backend gateway_api_rpc_engines
# each backend uses role-specific health endpoints and contains only matching role replicas
```

- Verification contribution: static parser/assertions prove stable route ownership and control-plane isolation.
- After this file: base data-plane selection is explicit and loopback-free.

#### File 4 — `MODIFY egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.ha.yml`

- Purpose: Apply the same two-role structure to HA, mTLS, HA-mTLS, demo overlays and deployment scripts/docs.
- Symbols: `compose.ha.yml`, `compose.mtls.yml`, `compose.ha-mtls.yml`, `compose.demo.yml`, `scripts/demo.sh`, `wait-ready.sh`, `run-mcp-conformance.sh`, `run-mcp-security.sh`, `README.md`, `README.zh-CN.md`.
- Repository evidence: overlays currently target mixed Engine services and scripts wait/call their MCP ports.
- Dependencies and consumers: base Compose, proxy, test suite, operators.
- Why now: Base-only correctness would violate profile parity and make operational variants unsafe.
- Contract/signature changes: role-specific service targets, certificates, credentials, readiness and MCP base URL; protocol commands unchanged.
- Input/output and state mapping: overlay/profile → same logical topology with environment-specific TLS/HA/demo additions.
- Error and edge behavior: every overlay retains required role keys; mTLS certificates/identities are not shared between roles; readiness waits for both required roles/Admin consistency.
- Standards impact: `MC-VALID-001`, `MC-CONFIG-001`, `MC-TEST-001`; all profiles are audited together.
- Literal rule enforcement: Rule 2 validates script inputs; Rule 7 exact overlay parity; Rule 11 existing deployment profile structure.
- Implementation pseudocode:

```text
extend each overlay with matching API_RPC and MCP service overrides, distinct certificates/credentials, and role-specific probes
make demo/readiness/conformance/security scripts select the stable proxy or explicit MCP service URL as their existing contract requires
update both deployment READMEs with one Admin, role replicas, ports, health, LKG, scale, rollout, rollback, and no-start validation commands
```

- Verification contribution: Compose test and shell syntax/config rendering prove all variants without starting containers.
- After this file: deployment artifacts consistently encode the target topology.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite -am -Dtest=GatewayComposeConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false test && docker compose -f egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.yml config >/dev/null`
- Expected result: Maven test passes; if Docker CLI is installed, Compose config exits 0 without starting containers; otherwise the Maven parser remains the recorded static proof and CLI absence is reported.
- Failure returns to: File 1 for invariant mismatch, File 2 for base ownership, File 3 for routing, File 4 for overlay/script/doc parity.
- Completion criteria: all deployment profiles describe one Admin and independent API_RPC/MCP role services with stable routing and no API MCP database ownership.
- Rollback: revert the Step commit; no container/deployment state is mutated.
- Commit paths: `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-test/egon-cola-platform-gateway-test-suite/src/test/java/top/egon/cola/component/gateway/test/deployment/GatewayComposeConfigurationTest.java`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.yml`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/haproxy.data-plane.cfg`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.ha.yml`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.mtls.yml`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.ha-mtls.yml`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/compose.demo.yml`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/scripts`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/README.md`, `egon-cola-platforms/egon-cola-platform-gateway/deployment/README.zh-CN.md`
- Commit: `ops(gateway): define dual engine deployment`

### Step 11 — Align local lifecycle, CI, and operator runbooks

- Requirements: `REQ-009`, `REQ-011`, `REQ-012`
- Dependencies: Step 10
- Baseline state: unified identity scripts and RBAC3 workflow explicitly build/start/verify one Engine jar; runbooks describe mixed-process MCP ownership.
- Observable outcome: local lifecycle and CI statically cover both artifacts/roles while preserving current portal-origin and release-reconciliation behavior.
- End state: shell contract tests, workflow selector, developer integration, and runbooks agree on build/start/status/verify/stop, ports, health, LKG, skew, rollout and rollback; runtime commands remain user-triggered.
- Test-first gate: Required — shell/static contract tests fail because MCP artifact/process/health/stop/status entries are absent.
- Manual Checks: `MC-ARCH-001`, `MC-REUSE-001`, `MC-DEP-001`, `MC-VALID-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`, `MC-BLOCKER-001`
- Literal Rules: Rule 2, Rule 5, Rule 7, Rule 11
- Ordered files:

#### File 1 — `MODIFY scripts/unified-platform/test-direct-run-contract.sh`

- Purpose: Define static local-run lifecycle requirements for both jars/processes, distinct ports/logs/PIDs/LKG/config, and preserved portal/release logic.
- Symbols: shell assertions over prepare/start/status/verify/stop/unified wrapper scripts.
- Repository evidence: current contract test already verifies direct-run commands and protects local stack conventions.
- Dependencies and consumers: local scripts and runbooks; no service startup.
- Why now: It is the RED operator-automation contract before shell changes.
- Contract/signature changes: shell test expectations only.
- Input/output and state mapping: script text/config → required artifact/role lifecycle assertions.
- Error and edge behavior: rejects shared PID/log/LKG/credentials, missing MCP health, lost portal-origin/release reconciliation, and stopping only one role.
- Standards impact: `MC-VALID-001`, `MC-CONFIG-001`, `MC-TEST-001`; static validation remains non-destructive.
- Literal rule enforcement: Rule 2 validates shell inputs/state; Rule 7 checks role config parity; Rule 11 preserves current unified-platform script architecture.
- Implementation pseudocode:

```bash
assert_contains start-local-stack.sh 'egon-cola-platform-gateway-mcp-engine'
assert_distinct_runtime_paths API_RPC MCP PID_FILE LOG_FILE LKG_DIRECTORY
assert_contains start-local-stack.sh 'reconcile_gateway_release'
assert_contains verify-local-stack.sh 'MCP_MANAGEMENT_PORT'
```

- Verification contribution: RED/GREEN proves lifecycle coverage without launching a process.
- After this file: test fails against one-Engine scripts.

#### File 2 — `MODIFY scripts/unified-identity-local.sh`

- Purpose: Update wrapper, prepare, start, status, verify, and stop scripts to build/manage both roles with distinct state and fail-fast readiness.
- Symbols: `unified-identity-local.sh`, `prepare-local-stack.sh`, `start-local-stack.sh`, `status-local-stack.sh`, `verify-local-stack.sh`, `stop-local-stack.sh`.
- Repository evidence: current scripts centralize jar resolution, PID/log directories, release reconciliation, portal start/origin, and ordered teardown.
- Dependencies and consumers: both artifacts, local DDC/Admin/providers, user-invoked lifecycle.
- Why now: File 1 protects both new behavior and recent baseline fixes.
- Contract/signature changes: internal script variables/status output add MCP role; existing commands remain.
- Input/output and state mapping: user action/config → build/launch/readiness/status/verify/stop for Admin + API + MCP with separate PIDs/logs/LKG.
- Error and edge behavior: partial startup reports exact role and cleans only started child processes; stale PID/port and one-role health failure are distinct; stop remains idempotent.
- Standards impact: `MC-REUSE-001`, `MC-VALID-001`, `MC-UTIL-001`, `MC-CONFIG-001`, `MC-SCOPE-001`; reuse current shell helpers and do not alter unrelated migration/login scripts.
- Literal rule enforcement: Rule 2 fail-fast validation; Rule 5 existing shell/JDK/curl utilities only; Rule 7 role env parity; Rule 11 current local-stack structure.
- Implementation pseudocode:

```bash
resolve_and_validate_api_rpc_jar; resolve_and_validate_mcp_jar
start_api_rpc_with_own_pid_log_lkg_identity; await_api_rpc_management_health
start_mcp_with_own_pid_log_lkg_identity; await_mcp_management_health
verify_admin_reports_both_roles_same_release; preserve_portal_origin_and_release_reconciliation
```

- Verification contribution: shell contract and syntax tests prove lifecycle shape; no process is started automatically.
- After this file: local scripts are role-aware and preserve current baseline behavior.

#### File 3 — `MODIFY .github/workflows/rbac3.yml`

- Purpose: Include MCP Engine in the existing RBAC3/gateway selector and document the unchanged gateway workflow coverage.
- Symbols: Maven `-pl` selector/path filters for MCP security integration.
- Repository evidence: RBAC3 workflow explicitly names gateway-engine; gateway workflow builds through the test-suite reactor and needs no speculative edit.
- Dependencies and consumers: CI Maven reactor and MCP RBAC3 tests.
- Why now: Production security ownership moved to MCP and must remain in CI.
- Contract/signature changes: CI selection only.
- Input/output and state mapping: changed paths/job → both role security/context tests.
- Error and edge behavior: workflow must not omit API RBAC3 coverage or expand unrelated jobs; YAML remains valid.
- Standards impact: `MC-DEP-001`, `MC-CONFIG-001`, `MC-SCOPE-001`; no new action/dependency.
- Literal rule enforcement: Rule 5 reuses existing workflow/actions; Rule 7 selector parity; Rule 11 existing CI structure.
- Implementation pseudocode:

```yaml
run: ./mvnw -pl existing-rbac3-modules,egon-cola-platform-gateway-engine,egon-cola-platform-gateway-mcp-engine -am test
# retain existing triggers, permissions, Java setup, caches, and unrelated job boundaries
# verify both role context/security tests are selected while all pre-existing RBAC3 modules stay in the same job
# keep workflow path filters aligned with both executable module directories and do not add a second workflow
```

- Verification contribution: YAML/static search and Maven selectors prove both security contexts are covered.
- After this file: CI follows runtime ownership without workflow redesign.

#### File 4 — `MODIFY docs/operations/unified-identity-mcp-local-runbook.md`

- Purpose: Update MCP/local unified-identity runbooks and gateway developer integration with exact two-role commands, ports, health, logs, LKG, skew, rollout, rollback, and validation boundaries.
- Symbols: MCP local runbook, unified identity runbook, `developer-integration.zh-CN.md`.
- Repository evidence: these documents currently name the mixed Engine and are the operator/developer entry points.
- Dependencies and consumers: scripts, deployment files, user runtime testing.
- Why now: Documentation must reflect only final, statically verified names and commands.
- Contract/signature changes: documentation only; explicitly distinguishes build/static proof from user-controlled live proof.
- Input/output and state mapping: operator action → role-specific process/health/log/LKG/rollback observation.
- Error and edge behavior: documents missing role, unknown role, version/checksum/ACK skew, one-role rollback, credential/TLS isolation, and no-new-release mixed cutover rule.
- Standards impact: `MC-CONFIG-001`, `MC-SCOPE-001`, `MC-TEST-001`; every command/path is audited against current files.
- Literal rule enforcement: Rule 7 records exact base/operations/env compatibility; Rule 11 documents one Admin plus two executable role structure.
- Implementation pseudocode:

```text
document build and static validation first, then separately label user-triggered start/live verification commands
list API_RPC and MCP data/management ports, PIDs, logs, LKG directories, DDC role metadata, health and same-release ACK checks
document scale, one-role drain/rollback, skew diagnosis, credential/TLS isolation, and prohibition on a new release during mixed cutover
```

- Verification contribution: link/path/command audit closes operator-facing acceptance and live-boundary clarity.
- After this file: code, CI, scripts, and documentation use the same topology vocabulary.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `bash -n scripts/unified-identity-local.sh scripts/unified-platform/prepare-local-stack.sh scripts/unified-platform/start-local-stack.sh scripts/unified-platform/status-local-stack.sh scripts/unified-platform/verify-local-stack.sh scripts/unified-platform/stop-local-stack.sh scripts/unified-platform/test-direct-run-contract.sh && bash scripts/unified-platform/test-direct-run-contract.sh && rg -n 'gateway-mcp-engine|API_RPC|MCP|18084|18085' .github/workflows/rbac3.yml docs/operations/unified-identity-mcp-local-runbook.md docs/runbooks/unified-identity-local.md egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/docs/developer-integration.zh-CN.md`
- Expected result: exit 0; shell syntax/contract tests pass and every audited CI/doc path names both roles and MCP default ports; no services start.
- Failure returns to: File 1 for missing contract coverage, File 2 for lifecycle regression, File 3 for CI selection, File 4 for stale/unsafe operator instructions.
- Completion criteria: all repository-owned lifecycle/CI/docs consistently manage two roles; full static/reactor/frontend audit in §8 passes and live evidence remains pending explicit authorization.
- Rollback: revert the Step commit; deployment files remain usable directly while local wrapper/docs revert.
- Commit paths: `scripts/unified-platform/test-direct-run-contract.sh`, `scripts/unified-identity-local.sh`, `.github/workflows/rbac3.yml`, `docs/operations/unified-identity-mcp-local-runbook.md`, `scripts/unified-platform/prepare-local-stack.sh`, `scripts/unified-platform/start-local-stack.sh`, `scripts/unified-platform/status-local-stack.sh`, `scripts/unified-platform/verify-local-stack.sh`, `scripts/unified-platform/stop-local-stack.sh`, `docs/runbooks/unified-identity-local.md`, `egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-admin/docs/developer-integration.zh-CN.md`
- Commit: `docs(gateway): align dual engine operations`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Skill structure before execution | Repository root | `python3 .agents/skills/egon-coding-writing-plan/scripts/validate_plan.py docs/egon/plan/2026-09-02-21-03-gateway-dual-engine-separation-implementation.md --strict` | This Plan | Exit 0 | Plan authoring | Planning only |
| RED/GREEN Step 1 | Repository root | Contract command in Step 1 | Role enum | Expected missing type, then exit 0 | Step 1 Files 1–2 | `REQ-001`, `REQ-007`; module |
| RED/GREEN Steps 2–4 | Repository root | Focused runtime-core/API commands in each Step | Shared boundaries, transports, operation, activation/compiler | Expected missing module/import/contracts, then exit 0 | Owning Step | `REQ-002`, `REQ-004`, `REQ-005`, `REQ-006`; module |
| RED/GREEN Steps 5–6 | Repository root | MCP compiler/context/ownership/package commands in each Step | MCP module and both executable contexts | Expected missing MCP types/mixed ownership, then exit 0 | Owning Step | `REQ-002`–`REQ-006`, `REQ-010`, `REQ-011`; cross-module |
| RED/GREEN Step 7 | Repository root | Admin focused command in Step 7 | Role-complete consistency and wire contract | Expected missing role behavior, then exit 0 | Step 7 | `REQ-001`, `REQ-007`, `REQ-008`; module |
| RED/GREEN Step 8 | Gateway Admin Web module | Frontend command in Step 8 | Types, API mapping, role UI states | Expected missing UI behavior, then tests/typecheck/build exit 0 | Step 8 | `REQ-007`, `REQ-008`; frontend static/component |
| RED/GREEN Step 9 | Repository root | Test-suite focused command in Step 9 | Process/live fixtures and wire compatibility | Expected one-artifact assumptions, then exit 0 | Step 9 | `REQ-006`, `REQ-008`, `REQ-012`; non-live suite |
| RED/GREEN Steps 10–11 | Repository root | Compose and shell commands in each Step | Deployment profiles, scripts, CI, docs | Expected missing role config, then exit 0 | Owning Step | `REQ-009`–`REQ-012`; static |
| Java reactor regression | Repository root | `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway -am clean verify` | All Gateway modules and non-live tests | Exit 0; no test failure/dependency cycle/duplicate class | Steps 1–11 by owner | All requirements; reactor/static |
| Dependency/package audit | Repository root | `./mvnw -pl egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine,egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine -am dependency:tree` plus `rg` ownership searches | Executable DAG and forbidden ownership | API has no mcp-core/JDBC/PostgreSQL/MCP production packages; no executable cross-dependency | Steps 2, 3, 6 | `REQ-002`–`REQ-005`, `REQ-010`; static |
| Configuration parity audit | Repository root | Compare property classes with base/operations YAML, Compose overlays, scripts and docs using focused tests plus `rg` | All role profiles | New MCP keys are parity-complete; old MCP prefix/env preserved; API has no MCP/database keys | Steps 5, 6, 10, 11 | `REQ-009`–`REQ-011`; static |
| Frontend regression | Gateway Admin Web module | `npm test && npm run typecheck && npm run lint && npm run build` | Entire Gateway Admin Web | Exit 0 | Step 8 | `REQ-007`, `REQ-008`; frontend |
| Compose render | Gateway deployment directory | `docker compose -f compose.yml config` and each applicable overlay combination | Static Compose rendering | Exit 0 when Docker CLI is available; starts no containers | Step 10 | `REQ-009`–`REQ-012`; static |
| Working-tree/commit audit | Repository root | `git status --short`, `git diff --check`, path-limited staged diff before every commit | Step-owned paths and user changes | No unrelated path staged; no whitespace error | Every Step | All; source control |
| User-controlled live topology | Repository root/runtime environment | Existing live profile plus commands documented in Step 11/runbooks, only after explicit authorization | One Admin, two API replicas, two MCP replicas, providers/stores/proxy | Both roles register/ACK same release; API/RPC/MCP/conformance/security/HA/LKG/skew/rollback assertions pass | Steps 6–11 depending failure | `REQ-001`–`REQ-012`; live, not proven by this Plan/static execution |

Final source audits must search for stale mixed ownership rather than rely only on test names:

```bash
rg -n 'engine\.mcp|McpEngine|McpRuntime|GATEWAY_MCP_|spring\.datasource|postgresql|jdbc' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-engine
rg -n 'RpcGatewayServer|RpcGatewaySlotRuntime|GatewayCompositeHttpDataPlaneHandler|DefaultGatewayHttpDataPlaneHandler' \
  egon-cola-platforms/egon-cola-platform-gateway/egon-cola-platform-gateway-mcp-engine
rg -n 'egon-cola-platform-gateway-(engine|mcp-engine)' \
  egon-cola-platforms/egon-cola-platform-gateway/deployment scripts .github/workflows docs
```

Expected first and second searches return no forbidden production ownership; permitted compatibility strings must be
confined to MCP Engine or negative tests. The third search must show both artifacts anywhere lifecycle ownership is
expressed.

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Database and data migration

N/A. Primary Spec §17 explicitly adds no schema/table/column/index/data migration. Existing MCP PostgreSQL/Redis keys
and Admin release persistence remain unchanged; ownership/configuration moves to the MCP process. No Flyway file may be
edited or created for this work.

### 9.2 Build and artifact order

1. Build/publish the contract and runtime-core before the executable artifacts.
2. Build both API_RPC and MCP jars from the same repository revision and release them as one compatible deployment set.
3. Verify dependency/package/config/static protocol gates before producing images.
4. Build the existing API Engine image and new MCP Engine image with traceable matching version/revision labels.
5. Do not publish a new Admin gateway release while role traffic is in mixed cutover.

### 9.3 Compatibility and cutover order

1. Deploy the new MCP Engine dark, with distinct identity/credentials/TLS/ports/LKG and no external data-plane traffic.
2. Confirm it registers fixed metadata `gateway.engine.role=MCP`, downloads the current active artifact,
   compiles/validates it, restores or writes MCP LKG, and ACKs the same release/version/checksum as API_RPC.
3. Confirm Admin sees at least one healthy `API_RPC` and one healthy `MCP` role and reports consistent only after all
   online replicas are ready on the same identity.
4. Switch only MCP Host/Path rules in the dedicated data-plane proxy to MCP backends. API/HTTP/WS/RPC routing remains on
   the existing API Engine artifact.
5. Run MCP initialize/tools/call, legacy/well-known paths, identity/RBAC3/approval, session/task/subscription/remote,
   conformance/security, metrics/health, and direct-Provider checks.
6. Remove MCP state/config/credentials from API Engine deployment only after the new MCP role is healthy and routing is
   verified. The production code already lacks that ownership from Step 6; this rollout item covers external deployment
   values/secrets.
7. Scale each role independently and verify every new replica reaches the same release identity and READY ACK.

### 9.4 Rollback

- Before MCP traffic switch: stop/drain only MCP Engine; API/RPC remains unaffected. Fix forward or redeploy the previous MCP artifact while retaining current active release and MCP stores/LKG.
- After MCP traffic switch but before old mixed deployment is removed: route MCP paths back to the last known compatible mixed/API deployment only if that artifact revision still contains MCP and credentials/state configuration are intact. Do not publish a new release during this window.
- After old MCP capability is removed from API Engine: rollback MCP by deploying the previous standalone MCP Engine artifact and its role-local LKG/config; never route MCP to the new API_RPC-only jar.
- API rollback is independent: drain/rollback API replicas without stopping MCP, provided both versions accept the unchanged unified artifact/wire contracts.
- If Admin reports missing/unknown role, version/checksum skew, or non-ready ACK, halt cutover/new releases, retain the last active artifact, and repair/rollback only the affected role.

### 9.5 Forward-fix and irreversible boundaries

No database or wire-contract step is irreversible. The only operational boundary is removal of old mixed-process MCP
credentials/state configuration; the rollback target must therefore be the standalone MCP artifact after that point.
Release artifact/active key and existing state stores are never duplicated, rewritten, or cleared.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Primary §4, §5, §12 | Steps 1, 7 | role enum; Admin Strategy/projection/config | enum, projection, controller tests | Exactly two known roles and one Admin authority. |
| `REQ-002` | Primary §4, §7 | Steps 2, 6 | runtime-core; API POM/config/runtime/resources | API context/package/dependency/search gates | API Engine owns API/HTTP/WS/RPC only. |
| `REQ-003` | Primary §4, §7 | Steps 5, 6 | MCP module/compiler/application/config/runtime/server/resources | MCP compiler/context/boundary/jar tests | Independently runnable MCP artifact. |
| `REQ-004` | Primary §4, §6 | Steps 2, 3, 6 | parent/POMs/runtime-core/boundary tests | reactor and dependency tree | Both executables depend on runtime-core and not each other. |
| `REQ-005` | Primary §4, §8 | Steps 3, 6 | shared operation/outbound adapters; MCP handler Adapter | operation/context/self-call search tests | MCP reaches Providers directly. |
| `REQ-006` | Primary §4, §9 | Steps 4, 5, 6, 9 | DTO/Strategies/activation/LKG/harness/release tests | compiler, activation, LKG, release identity tests | One artifact/active key with role-local state/ACK. |
| `REQ-007` | Primary §4, §9, §12 | Steps 1, 7, 8 | role enum, Admin Strategy/projection, role UI | backend and component tests | Role-complete, skew-aware Admin/UI state. |
| `REQ-008` | Primary §4, §11 | Steps 7, 8, 9 | controller/UI types/pages/wire tests | golden JSON, rule wire, component tests | Existing external contracts remain compatible. |
| `REQ-009` | Primary §4, §14–§15 | Steps 1, 10, 11 | role enum; Compose/proxy/scripts/runbooks | enum, Compose/shell/static and user live gates | Code-fixed role identity plus independent operations and rollback. |
| `REQ-010` | Primary §4, §10 | Steps 6, 10 | moved MCP stores/security/config; API cleanup; Compose | context/dependency/config/Compose tests | MCP owns Redis/PostgreSQL/artifact/remote state. |
| `REQ-011` | Primary §4, §13 | Steps 5, 6, 10, 11 | properties/YAML/overlays/scripts/docs | binding/key-parity/static tests | Legacy MCP keys preserved; bootstrap profiles complete. |
| `REQ-012` | Primary §4, §16 | Steps 9, 10, 11 | test suite/deployment/scripts/CI/docs | full gate matrix and explicit live boundary | Required static/module/frontend/deployment proof is executable. |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | Compile closure may reveal one additional shared type import during mechanical moves. | Steps 2–4 runtime-core/API packages | Current package scan and imports define the closure; `PLAN-CLAR-003` limits ownership | Implementer | Closed planning action: move only a required value/port type, document it in Step evidence, never move ingress behavior. |
| `RISK-002` | Step 6 is larger than a normal commit. | Both executables | MCP class move and API deletion cannot be split without duplicate or missing Beans/classes | Implementer | Closed exception: one atomic path-limited commit with paired context/boundary tests and no deployment. |
| `RISK-003` | Local-stack/identity behavior and Archetype state changed after the Spec baseline. | Step 11 scripts and all commits | `main@ce10db63` includes release-reconciliation/portal-origin/IdP CORS fixes; Archetype paths are concurrently dirty | Implementer | Closed action: rebase Gateway work on current content, protect newer behavior in RED tests, and never stage `egon-cola-archetypes/**`. |
| `RISK-004` | `docker compose config` may be unavailable in the execution host. | Step 10 | Docker startup is prohibited and CLI availability is environmental | Implementer | Closed validation boundary: Maven Compose parser is mandatory; CLI render runs only if available and starts nothing. |
| `RISK-005` | Live HA/security/conformance evidence needs infrastructure and explicit authorization. | Steps 6–11 final live gate | User instructions prohibit automatic service/Docker/database startup | User/Implementer | Closed planning action: prepare tests/runbooks; report live validation as pending rather than claim it. |
| `DECISION-001` | Use existing Engine for API_RPC, new MCP Engine, and a shared runtime-core. | All implementation Steps | User-confirmed primary Spec | User | Confirmed. |
| `DECISION-002` | Use Strategy only for compiler/Admin role variation and Adapter for MCP handler; direct code elsewhere. | Steps 4–7 | Spec and simplicity audit | User/Implementer | Confirmed by approved Spec; no unresolved pattern decision. |

There is no unresolved blocker or user decision preventing implementation planning.

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

The Plan implements the confirmed “one Admin Server, two Gateway Engine roles” outcome and covers all twelve effective
primary requirements. It retains one unified release/control plane, separates the two data-plane executables, preserves
direct MCP Provider invocation and wire contracts, and includes independent deployment/operations plus role-complete
Admin/UI visibility.

### 12.2 Spec consistency

The Plan does not add endpoints, schemas, state stores, active keys, public fields, protocol aliases, self-calls,
two-phase commit, or new frameworks. `PLAN-CLAR-001` through `PLAN-CLAR-008` are repository-derived implementation
details: current-baseline reconciliation, minimal shared DTO access, compile closure, property-independent shared
configuration, bootstrap-key ownership, qualified Clock, isolated data-plane proxy, and a compilable module introduction
order. None changes business semantics.

The simplicity/necessity audit found no fetch-then-forward interface, caller-supplied value that should derive from
trusted context, speculative cache/layer, duplicate mapper/model, or material overdesign. Strategy is limited to two
genuine role variations; Adapter is limited to the MCP handler boundary.

### 12.3 Repository executability

All named modules and current source owners were inspected and reconciled against
`main@ce10db63a24eecf6b5a3280cd4e9afa7c1849c81`. Each Step names its RED reason, file/directory operation, symbols,
mapping, failures, standards, exact validation working directory/command, rollback, path-limited commit scope, and
semantic commit. Mechanical directory operations list their exact behavioral closure; compile/search gates catch any
missed import without authorizing broader ownership moves.

### 12.4 Test and release completeness

The order covers focused RED/GREEN, module regression, reactor verify, dependency/package/config audits, frontend
tests/typecheck/build, static Compose/shell/CI/docs checks, wire compatibility, role release/LKG/ACK/skew, and a
separately labeled user-controlled live gate. There is no database migration. Rollout/rollback keeps one active
artifact, forbids a new release during mixed cutover, and supports independent role rollback.

### 12.5 Blocking Manual Check

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable` | `PASS` | Current Gateway platform tree plus §4.7 and Steps 1–11 | One existing platform profile is preserved; runtime-core is non-executable and exactly two executables remain | None |
| `MC-REUSE-001` | `Applicable` | `PASS` | Capability ledger and Steps 2–7, 10 | Existing server, operation, provider, release, state, JSON/time and HAProxy conventions are reused | None |
| `MC-DEP-001` | `Applicable` | `PASS` | POM/dependency plan in Steps 2, 5, 6, 9 and dependency gates | Existing managed dependencies move to owners; no executable cycle or new framework | None |
| `MC-NAME-001` | `Applicable` | `PASS` | Role/type inventory in Steps 1, 4–8 | Exact semantic suffixes and role names are specified | None |
| `MC-VALID-001` | `Applicable` | `PASS` | Properties/DTO/metadata/environment/config negative tests in Steps 4–11 | All new trust/config boundaries have explicit positive and negative validation | None |
| `MC-MODEL-001` | `Applicable` | `PASS` | Immutable enum/records and Lombok plan in Steps 1, 4–7 | Value objects are immutable; business/config classes use complete constructor baseline | None |
| `MC-CONVERT-001` | `Not applicable` | `N/A` | §4.5 and §4.7 show no new layer model mapping; existing snapshot/projection models are reused | No MapStruct/BaseConverter bypass because no converter is introduced | None |
| `MC-LOG-001` | `Applicable` | `PASS` | Runtime/config/Strategy files in Steps 6–7 | Business lifecycle/services use repository-consistent `@Slf4j`; no ad hoc logger | None |
| `MC-BEAN-001` | `Applicable` | `PASS` | `lombok.config`, named Strategy/runtime/server/service/Clock wiring in Steps 2–7 | Bean names, final constructor fields, qualifiers and context tests are explicit | None |
| `MC-UTIL-001` | `Applicable` | `PASS` | Capability ledger, POM and script plans | Existing JDK/Spring/Reactor/Jackson/gRPC/shell utilities are reused; no helper library added | None |
| `MC-JSON-001` | `Applicable` | `PASS` | Generic DTO snapshot reuse, controller golden and rule-wire tests | Existing property names/defaults/bytes remain; no duplicate external DTO | None |
| `MC-TIME-001` | `Applicable` | `PASS` | Step 7 named Clock and projection tests | `Clock`/`java.time` preserve deterministic UTC semantics and precision | None |
| `MC-CONFIG-001` | `Applicable` | `PASS` | Steps 5–6 and 9–11 plus parity gates | Base/operations/overlays/scripts/docs have exact role key ownership; legacy MCP keys are retained | None |
| `MC-PATTERN-001` | `Applicable` | `PASS` | §4.5, compiler Strategies, Admin Strategy and MCP Adapter in Steps 4–7 | Complex role variations use Strategy; transport boundary uses Adapter; simple flows stay direct | None |
| `MC-SCOPE-001` | `Applicable` | `PASS` | §5 inventory, §6.1 protected paths, path-limited Step commits | Only Gateway/Admin Web/test/deployment/lifecycle/docs paths required by the Spec are owned | None |
| `MC-TEST-001` | `Applicable` | `PASS` | Step RED/GREEN gates and §8 ordered validation matrix | Static/module/frontend/deployment/live proof boundaries are complete and not conflated | None |
| `MC-BLOCKER-001` | `Applicable` | `PASS` | §11 and every Manual Check row | No unresolved blocker, failed check, or missing user decision remains | None |

### 12.6 Final verdict

PASS — Ready for user review
