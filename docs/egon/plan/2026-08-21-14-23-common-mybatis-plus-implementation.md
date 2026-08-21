# Egon COLA Common MyBatis-Plus Starter Implementation Plan

| Field | Value |
| --- | --- |
| Document | `2026-08-21-14-23-common-mybatis-plus-implementation.md` |
| Template Version | `2` |
| Status | `Ready` |
| Created | `2026-08-21 14:23 CST` |
| Updated | `2026-08-21 14:50 CST` |
| Owner | `Mario / Egon-COLA maintainers` |
| Repository | `Egon-COLA` |
| Scope | `common-core Jakarta Validation facade and boundary tests; one opt-in Common MyBatis-Plus Boot 3 Starter with EgonModel ActiveRecord, EgonCola Mapper/IService/ServiceImpl, TenantID isolation, audit fill, repository validation, module tests, BOM export, and bilingual documentation` |
| Source Requirement | `2026-08-19 through 2026-08-21 Common MyBatis-Plus decisions, finalized by removing four duplicate current-tenant Service methods, three tenant Mapper statements, and every custom SQL Injector, and enhancing the official list/count/getById/getOptById and remaining MyBatis-Plus contracts instead` |
| Baseline Revision | `main@0b7b9b3a2a4bc71ae4bb3ce127270d00033e8b60; 2026-08-21 14:23 CST dirty-worktree snapshot; target Common source unchanged since the accepted Spec baseline; preserve all unrelated staged and untracked work` |
| Implements Spec | [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Updated 2026-08-21 14:23 CST; baseline main@0b7b9b3a2a4bc71ae4bb3ce127270d00033e8b60` |
| Effective Specs | [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md); [Common enterprise restructure](../../superpowers/specs/2026-07-07-egon-cola-component-common-enterprise-restructure-design.md) |
| Depends On Plans | `None` |
| Supersedes | [Obsolete pre-ActiveRecord implementation Plan](2026-08-20-15-01-common-mybatis-plus-implementation.md) |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

This Plan implements the accepted Common MyBatis-Plus Spec in eight sequential, commit-sized Steps. It first adds a narrowly bounded Jakarta Validation facade to `common-core`, then publishes the new Maven module, establishes `EgonModel` ActiveRecord and authoritative context filling, freezes the zero-addition Mapper plus 57-method IService surface, wires one shared tenant/validation interception chain, proves the DTO-to-PO-to-Model and AR/Mapper/IService flows with H2, and finally exports and documents the Artifact. Completion requires reflection parity, focused unit and ContextRunner tests, real MyBatis/H2 SQL and transaction evidence, dependency/source/package gates, three reactor commands, and a final path-scoped Git audit; no service process, external database, production migration, or live sharding topology is started.

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md)
- Status: `Accepted`
- Revision: `Updated 2026-08-21 14:23 CST; main@0b7b9b3a2a4bc71ae4bb3ce127270d00033e8b60`
- Approval evidence: the user explicitly confirmed this Plan in the current conversation, requested `egon-coding-executing-plan`, and authorized a new worktree, implementation, and merge to `main`.

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [Common MyBatis-Plus Starter Spec](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) | `Accepted`, updated 2026-08-21 14:23 CST | Entire document, including `REQ-001` through `REQ-030`, `DEC-001` through `DEC-015`, `INTERNAL-001` through `INTERNAL-067`, and `TEST-001` through `TEST-076` | Defines the exact public types, upstream parity, seven Model fields, TenantID/audit semantics, validation/conversion boundaries, no-custom-Injector decision, files, tests, and release gates |
| Normative dependency | [Common enterprise restructure](../../superpowers/specs/2026-07-07-egon-cola-component-common-enterprise-restructure-design.md) | Repository predecessor at baseline | §3.3-§4.3, §5.1, §6, §11-§12, as explicitly amended by the primary Spec | Preserves the Common `pom` aggregator, opt-in concrete Jars, downward dependency direction, BOM export, and source-boundary discipline; the primary Spec narrowly permits Jakarta Validation API in core and the persistence base only in the Starter |

### 2.3 Superseded or excluded content

The [2026-08-20 implementation Plan](2026-08-20-15-01-common-mybatis-plus-implementation.md) is superseded in full because it predates `EgonModel`, layered validation, the final TenantID-only field model, and the deletion of duplicate tenant APIs. FamilyAiButler sources remain reference evidence only. Their `businessId`, user-name fields, `deleted` column, `extention` spelling, custom injected statements, and partial Service implementation are excluded. MyBatis-Plus 3.5.17 APIs, old `Model<M>` compatibility shims, consumer database migrations, production DTO/PO/Model classes, SecurityContext adapters, and ShardingSphere topology are also outside this Plan.

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | Primary Spec §4 | Add one opt-in Common MyBatis-Plus Starter and no extra core/test child module. | Common and Components reactors resolve the exact Artifact. | Parent, aggregator, Starter POM, BOM |
| `REQ-002` | Primary Spec §4 | Prefix new public MyBatis types with `EgonCola`, except the explicitly named `EgonModel` and `ValidationUtils`. | Source/API scan finds no copied `Family*`, `IEgonServiceImpl`, or custom Injector type. | Every production Java type and docs |
| `REQ-003` | Primary Spec §4, §7 | Load through Boot 3 imports and honor the global enabled property. | ImportCandidates loads the configuration without component scan; disabled context has no EgonCola beans. | AutoConfiguration, properties, imports |
| `REQ-004` | Primary Spec §4, §9 | Provide `EgonColaMapper<T extends EgonModel<T>>` with all official `BaseMapper` abilities and no added method. | Reflection reports zero declarations; MyBatis Configuration retains official statements. | Mapper API and SQL tests |
| `REQ-005` | Primary Spec §4, §9 | Provide the generic EgonCola IService interface and implementation base. | Consumer fixture compiles and executes through the concrete technical Service. | IService, ServiceImpl, support fixture |
| `REQ-006` | Primary Spec §4, §9 | Redeclare and explicitly override all 57 MyBatis-Plus 3.5.16 IService-visible methods. | Interface and implementation declared-signature parity are exact: no missing or extra method. | IService/ServiceImpl/parity tests |
| `REQ-007` | Primary Spec §4, §7 | Obtain a non-null current `Long tenantId` from an overrideable Provider; default reads MDC key `tenantId`. | Any Long is accepted; missing or malformed context fails before JDBC; custom Provider backs off the MDC Bean. | Tenant Provider, MDC adapter, guard, fill |
| `REQ-008` | Primary Spec §4, §10 | Keep `tenantId/tenant_id` as the only tenant key; persistent state is non-null with no range restriction. | Zero, negative, and positive IDs work; null loaded/persisted Model fails; H2 uses BIGINT NOT NULL. | Model annotations, validation, schema |
| `REQ-009` | Primary Spec §4, §9 | Treat TenantID as a multi-row scope and enhance official query methods only. | `list/count` return the current tenant's multiple rows; `getById/getOptById` combine TableId, logic delete, and TenantLine. | Service behavior and integration tests |
| `REQ-010` | Primary Spec §4, §7, §9 | AR, IService, Mapper, Wrapper, chains, and direct Mapper calls share one isolation chain. | Writes receive authoritative tenant fill; explicit predicates must equal context; every supported SQL receives TenantLine; loaded Model tenant matches. | Guard, TenantLine, handler, validation plugin |
| `REQ-011` | Primary Spec §4, §7, §9 | Batch writes capture one tenant snapshot, prevalidate the collection, enforce bounds, and roll back atomically. | Initial errors issue zero JDBC; context drift or middle failure rolls back all rows; legal batches commit. | Service batch families and transaction tests |
| `REQ-012` | Primary Spec §4, §7 | Add and deterministically order wide-write protection, tenant isolation, optimistic locking, and pagination. | Context order and real SQL prove BlockAttack -> guard -> TenantLine -> optimistic lock -> pagination behavior. | InnerInterceptor beans and contract validator |
| `REQ-013` | Primary Spec §4, §9 | Enforce positive configured pagination with default maximum 500 and `overflow=false`. | Invalid sizes fail before SQL; a valid page remains scoped and returns official shape. | Properties, Service page guards, plugin |
| `REQ-014` | Primary Spec §4, §9 | Reuse the default MyBatis-Plus Injector; add no tenant aliases or custom statements. | Negative source/reflection scan excludes the removed 4 Service methods, 3 Mapper methods, and Injector classes; official inventory remains. | Mapper/IService/API and auto-config negative tests |
| `REQ-015` | Primary Spec §4, §10 | Insert-fill six technical fields and update-fill three, excluding `id` and every user-name field. | Fixed-Clock tests prove exact authoritative fields: tenant/user IDs, Instants, and `isDeleted=false`. | MetaObjectHandler and context Providers |
| `REQ-016` | Primary Spec §4, §7 | Permit safe Provider/outer-plugin/handler replacement without silently dropping isolation, validation, or fill. | Safe custom beans start; broken chain/handler/validation contracts fail startup; no Injector seam exists. | Conditional beans and startup validator |
| `REQ-017` | Primary Spec §4, §6 | Use official MP Boot3 Starter/JSqlParser plus core/Boot Validation; core receives only validation API. | POM/dependency/source gates exclude direct raw/native MyBatis and other runtime frameworks in core. | Managed dependencies, module POM, boundary allowlist |
| `REQ-018` | Primary Spec §4, §9 | Preserve official return shapes, Wrapper/Map/Obj/Kotlin chain, logic delete, optimistic lock, and transaction semantics. | Unit/integration method-family matrix passes with upstream-compatible results and errors. | 57 Service bodies and runtime plugins |
| `REQ-019` | Primary Spec §4, §6 | Freeze both MyBatis-Plus Artifacts and the parity baseline to 3.5.16. | Effective POM/dependency tree and ABI tests all resolve 3.5.16. | Parent/module POM and parity tests |
| `REQ-020` | Primary Spec §4, §16 | Publish synchronized English/Chinese adoption, limits, migration, and rollback docs. | Both README pairs contain matching symbols, configuration, examples, and warnings. | Four Markdown files |
| `REQ-021` | Primary Spec §4, §14 | Keep all focused fixtures/tests inside the Starter and out of the production Jar. | Module tests run; packaged Jar excludes H2 schema and support classes. | Starter test tree/package gate |
| `REQ-022` | Primary Spec §4, §14 | Validate without starting a service or connecting to a real database. | Only build, ContextRunner, embedded H2, static, and Git commands execute. | Execution gates |
| `REQ-023` | Primary Spec §4, §16 | Preserve unrelated dirty-worktree state and commit one Step at a time. | Path-scoped status/diff/staging prove unrelated files unchanged. | Every Step and final audit |
| `REQ-024` | Primary Spec §4, §9 | Provide `EgonModel<M extends EgonModel<M>> extends AbstractModel<M>` with 14-method AR parity and six enhanced write/delete roots. | Reflection and dynamic-dispatch tests prove 14 abilities, final templates, single hooks, and official `insertOrUpdate`. | Model/groups/AR tests |
| `REQ-025` | Primary Spec §4, §10 | Define exactly seven common fields with Jakarta constraints and `isDeleted/is_deleted` logic delete. | Metadata/annotation tests and H2 mapping prove exact fields and non-null persisted state. | EgonModel, fill, validation, schema |
| `REQ-026` | Primary Spec §4, §9 | Add instance-based common-core `ValidationUtils` for object/property/value/group manual validation. | Unit tests prove immutable stable violations, boolean checks, standard exceptions, and invalid-argument handling. | core API/POM/boundary tests |
| `REQ-027` | Primary Spec §4, §7, §9 | Add repository-specific manual validation and global post-fill parameter/result validation. | AR/Mapper/IService invalid write and dirty result paths share the same violations before/after JDBC. | ModelValidationUtils and MyBatis interceptor |
| `REQ-028` | Primary Spec §4, §10 | Keep validation ownership Controller DTO -> business Service PO -> Repository Model. | Test fixtures prove MVC `@Valid`, method validation, and repository validation without making technical IService a business Service. | Integration support/tests/docs |
| `REQ-029` | Primary Spec §4, §10 | Reuse `BaseConverter` at DTO<->PO and PO<->Model boundaries. | Forward/reverse/list mappings are explicit; PO->Model ignores all seven technical fields. | Test converters/object-flow proof/docs |
| `REQ-030` | Primary Spec §4, §10 | Validate subclass business-field constraints globally while leaving complex business rules in Service. | Invalid business Model fails through all persistence entries; a stateful rule exists only in the business-Service test fixture. | Model fixture, validation tests, docs |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Step 1 changes the lowest dependency boundary first: common-core gains the pure Jakarta facade and a narrowly parameterized source allowlist. Step 2 then publishes a compilable but behavior-free Starter Maven module. Step 3 freezes the persistence model and ActiveRecord template before any Service generic bounds consume it. Step 4 supplies stateless TenantID/UserID Strategies and authoritative MetaObjectHandler filling. Step 5 freezes the zero-addition Mapper and exact 57-method Service ABI, repository validation facade, and family-based enhanced bodies. Step 6 connects those pieces through one ordered MyBatis/Spring Boot runtime chain and proves official statements with H2. Step 7 adds higher-level AR, validation, transaction, and DTO/PO/Model conversion proofs without adding production business types. Step 8 exports the proven Artifact and synchronizes documentation.

Every intermediate commit compiles: Step 2 has no import resource pointing at a missing class; Step 3 depends only on dependencies already declared in Step 2; Steps 4 and 5 use direct constructors in focused tests; Step 6 creates the Boot import and full context only after every bean type exists. No generated client, network contract, frontend, consumer schema migration, or runtime service is part of the chain.

### 4.2 Test-first strategy

| Behavior | RED test and expected failure | Minimum GREEN implementation | Refactor/wiring allowed after GREEN |
| --- | --- | --- | --- |
| Core validation and boundary | Tests fail because `ValidationUtils` and validation-only import allowlist are absent. | API dependency, instance facade, ordered immutable violations, narrow boundary overload. | Shared private sorting helper inside `ValidationUtils`; existing no-argument boundary method delegates with empty allowlist. |
| Maven module | No Java RED is meaningful before the Artifact exists; Maven model cannot resolve the child. | Managed 3.5.16 dependencies, child module, Starter POM with production/test scopes. | POM ordering only. |
| EgonModel/AR | Parity and lifecycle tests fail on missing type, fields, annotations, final roots, and hooks. | Groups plus exact seven-field `EgonModel` extending `AbstractModel`, six final template roots, `pkVal`. | One private argument helper where duplication is proven; no Spring locator. |
| Context and fill | Model tests fail because providers, MDC parsing, exact insert/update fill, Clock semantics, and hooks do not exist. | Provider Strategies, MDC Adapters, configuration keys, final MetaObjectHandler templates. | Protected post-fill hooks only. |
| Mapper/IService/validation API | Parity and Service tests fail on missing zero-method Mapper, 57 declarations/overrides, prevalidation, batch/page bounds, and official return behavior. | Exact APIs plus small private read/write/batch/page/chain family helpers and repository validation facade. | Helper extraction cannot hide a declaration or alter upstream signature. |
| Shared runtime chain | ContextRunner/H2 tests fail on absent import, plugin order, TenantLine, explicit-tenant guard, parameter/result validation, and startup contract checks. | Ordered beans, fail-closed interceptors, imports resource, default Injector reuse, test schema. | Local SQL AST/recursive traversal helpers only. |
| Complete flows | AR/model-validation/batch/object-flow tests fail until real dispatch, rollback, MVC/PO validation, and converters operate together. | Test fixtures plus evidence-backed corrections returned to the owning production file. | Test fixture cleanup only; no production business abstraction. |
| Distribution/docs | BOM/source/package/doc gates fail before export and bilingual docs exist. | BOM entry and four synchronized README updates. | Link/format correction only. |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | common-core POM/validation package and common-test boundary utility | Establishes the lowest reusable contract. |
| Step 2 | Step 1 | None | Components/Common parent POMs and new module POM | Publishes the only Maven entry point for later Java. |
| Step 3 | Step 2 | None | model package and AR parity fixture/tests | Every later generic API depends on this model contract. |
| Step 4 | Step 3 | None | properties, business Providers, handler, and model unit test | One coherent context/fill lifecycle must land together. |
| Step 5 | Steps 3-4 | None | extension package, repository validation facade, parity/Service support | The 57 bodies share one frozen model/context contract. |
| Step 6 | Steps 1-5 | None | auto-config, interceptors, exception, imports, H2 schema/tenant SQL test | Full bean/SQL chain consumes every preceding symbol. |
| Step 7 | Step 6 | None | remaining integration tests and DTO/PO/converter fixtures | Proves cross-layer behavior without overlapping production writes. |
| Step 8 | Step 7 | None | BOM and four README files | Export/docs must match validated code. |

Execution is intentionally sequential. Parallel writes would overlap generic contracts or the Spring/MyBatis chain and would make test-first ownership and one-commit-per-Step history ambiguous.

### 4.4 Commit boundaries

Each Step ends in exactly one path-limited semantic commit after its focused GREEN gate. A Step stages only the exact files in its `Commit paths`; RED states, this Review Plan, the accepted Spec, and unrelated dirty files are never staged. Later integration failures return to the earlier owning file and are fixed before the Step 7 test commit; if such a correction changes a public contract rather than an implementation detail, execution stops and returns to the Spec instead of folding the redesign into a test commit.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| One opt-in Starter | Necessary, Spec §6-§8 | Common is a `pom` aggregator; concrete ID/desensitize capabilities are child Jars. | Adding MP to the aggregate POM would pollute every consumer. | One child POM, one import resource, one BOM edge. | Implement |
| `ValidationUtils` in core | Necessary, `REQ-026` | No reusable manual Validator facade exists; core boundary currently rejects all Jakarta imports. | Direct Validator calls duplicate ordering/error conventions at every boundary. | One API-only dependency, one class, narrow allowlist/tests. | Implement as a small Facade |
| `EgonModel` AR inheritance | User-required, `REQ-024`,`REQ-025` | MP 3.5.16 Jar contains `AbstractModel`, not old `Model`; repository has no persistence base in core. | A composition wrapper cannot participate in MP ActiveRecord; downgrading or fake alias breaks version/API decisions. | One seven-field base with six final roots and hooks. | Implement using framework Template Method |
| Provider source variation | Necessary, `DEC-003` | Current source is MDC and future source is SecurityContext; no existing persistence-context port exists. | Static MDC calls in Model/Service/plugins would couple every entry and frustrate replacement. | Two Strategies and two MDC Adapters; stateless calls. | Implement |
| Authoritative MetaObject fill | Necessary, `REQ-015` | MP exposes one supported `MetaObjectHandler`; caller technical fields are untrusted. | Filling separately in AR/Service/Mapper duplicates logic and misses direct Mapper calls. | One final handler with two protected extension hooks. | Implement using Template Method |
| Zero-method EgonColaMapper | User-required integration point, `REQ-004`,`REQ-014` | `BaseMapper` already supplies statements and default Injector; TenantLine handles scoping. | Custom tenant methods add no independent result and duplicate official list/count/get paths. | One empty generic interface. | Implement; no Injector or methods |
| 57 explicit Service overrides | User-required, `REQ-006` | MP 3.5.16 exposes exactly 57 visible IService/IRepository methods. | Inherited defaults cannot satisfy full override/version-freeze requirement. | High body count bounded by parity tests and private family helpers. | Implement accepted trade-off |
| Shared tenant SQL guard/TenantLine | Necessary, `REQ-010` | AR/Mapper/IService all converge at MyBatis; no existing Common tenant interceptor. | Per-entry filters miss direct Mapper/AR paths and allow wrapper spoofing. | Two InnerInterceptors plus startup order validation. | Implement as Adapter/SPI composition |
| One parameter/result validation plugin | Necessary, `DEC-010` | MyBatis ParameterHandler runs after ID/fill; ResultSetHandler is the common read return point. | Three AOP paths for AR/Mapper/Service drift and still miss raw Mapper calls. | One interceptor with bounded recursive extraction and cycle guard. | Implement |
| Repository validation facade | Necessary, `REQ-027` | Common facade lacks Model operation groups/current tenant check. | Copying operation-group composition into Service and interceptor duplicates rules. | One thin composed facade; no global state. | Implement |
| DTO/PO/Model converters | Proof-only in this module, `REQ-028`,`REQ-029` | `BaseConverter<S,T>` already exists and owns list defaults. | A new universal/reflection converter would duplicate an established contract. | Test-only explicit boundary converters; consumers create real ones. | Reuse, do not add production converter |
| Custom tenant APIs/SQL Injector | Rejected, `DEC-015` | Official statements plus TenantLine already produce scoped list/count/exact reads. | Four Service aliases, three Mapper methods, and a custom Injector are fetch/contract duplication. | Extra public API, SQL maintenance, injector replacement risk. | Do not implement |
| Cache/factory/domain-service/static holder | Unnecessary, Spec §3, §6 | No expensive immutable derivation, complex construction, or domain state transition exists. | Direct constructor-injected beans and MP SPI are clearer. | Additional lifecycle/state/files with no requirement. | Do not implement |

The selected patterns solve real variation points only: Strategy isolates TenantID/UserID sources, Adapter maps MDC and MP TenantLine APIs, Template Method protects AR/fill invariants while permitting local technical hooks, and two small Facades normalize validation use. Factory, Builder, Decorator, State, Observer, Command, and a custom Chain-of-Responsibility hierarchy are rejected because Spring and MyBatis already supply the required composition mechanisms.

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Core validation boundary | `REQ-017`,`REQ-026` | `ValidationUtilsTest`, `CoreBoundaryTest`, `SourceBoundaryAssertTest` | Existing common-core/common-test | Stable manual validation facade and narrow Jakarta allowlist | Model validation and object-flow tests | Step 1 |
| Maven publication edge | `REQ-001`,`REQ-017`,`REQ-019`,`REQ-021`,`REQ-023` | Maven model/package commands | Existing Components/Common parents | Resolvable Starter with exact dependency scopes | All Starter Java | Step 2 |
| AR model contract | `REQ-002`,`REQ-018`,`REQ-024`,`REQ-025`,`REQ-030` | AR parity and structural tests | Step 2 MP/Jakarta dependencies | EgonModel, groups, business Model fixture | Handler, Mapper, Service, plugins | Step 3 |
| Context and fill lifecycle | `REQ-007`,`REQ-008`,`REQ-015`,`REQ-016`,`REQ-025` | fixed-Clock/MDC/model tests | Step 3 EgonModel | Providers, properties, exact authoritative fill | Service validation and runtime chain | Step 4 |
| Mapper/Service ABI and behavior | `REQ-004`-`REQ-006`,`REQ-009`-`REQ-011`,`REQ-013`,`REQ-014`,`REQ-018`,`REQ-027`,`REQ-030` | reflection parity and focused Service tests | Steps 3-4 | zero-method Mapper, 57-method IService/Impl, repository validation | Full MyBatis context/H2 | Step 5 |
| Shared Boot/MyBatis chain | `REQ-003`,`REQ-007`-`REQ-010`,`REQ-012`-`REQ-019`,`REQ-021`,`REQ-022`,`REQ-025`,`REQ-027`,`REQ-030` | ContextRunner and tenant SQL integration | Steps 1-5 | ordered beans, default Injector SQL, guarded/fill/validated runtime | Higher integration flows | Step 6 |
| Cross-entry and layered proof | `REQ-010`,`REQ-011`,`REQ-018`,`REQ-021`,`REQ-022`,`REQ-024`-`REQ-030` | AR/model/batch/object-flow integration tests | Step 6 full context | Evidence for all entry points, transactions, DTO/PO/Model boundaries | Release gates/docs | Step 7 |
| Distribution and adoption | `REQ-001`-`REQ-003`,`REQ-014`,`REQ-017`,`REQ-019`-`REQ-030` | BOM/docs/package/reactor/source gates | Step 7 GREEN | Consumable Artifact and synchronized guidance | Consumer adoption | Step 8 |

## 5. Change File Tree

```text
egon-cola-components/
├── pom.xml                                                        # MODIFY [Step 2]
├── egon-cola-components-bom/pom.xml                               # MODIFY [Step 8]
└── egon-cola-component-common/
    ├── pom.xml                                                    # MODIFY [Step 2]
    ├── README.md                                                  # MODIFY [Step 8]
    ├── README.zh-CN.md                                            # MODIFY [Step 8]
    ├── egon-cola-component-common-core/
    │   ├── pom.xml                                                # MODIFY [Step 1]
    │   └── src/
    │       ├── main/java/top/egon/cola/component/common/core/validation/ValidationUtils.java # CREATE [Step 1]
    │       └── test/java/top/egon/cola/component/common/
    │           ├── core/CoreBoundaryTest.java                     # MODIFY [Step 1]
    │           └── validation/ValidationUtilsTest.java            # CREATE [Step 1]
    ├── egon-cola-component-common-test/src/
    │   ├── main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java # MODIFY [Step 1]
    │   └── test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java # MODIFY [Step 1]
    └── egon-cola-component-common-mybatis-plus-spring-boot-starter/
        ├── pom.xml                                                # CREATE [Step 2]
        ├── README.md                                              # CREATE [Step 8]
        ├── README.zh-CN.md                                        # CREATE [Step 8]
        └── src/
            ├── main/
            │   ├── java/top/egon/cola/component/common/mybatis/
            │   │   ├── autoconfigure/{EgonColaMybatisPlusAutoConfiguration.java,EgonColaMybatisPlusProperties.java,EgonColaMybatisPlusContractValidator.java}
            │   │   ├── business/{EgonColaTenantIdProvider.java,EgonColaMdcTenantIdProvider.java,EgonColaUserIdProvider.java,EgonColaMdcUserIdProvider.java,EgonColaTenantIdTenantLineHandler.java}
            │   │   ├── exception/EgonColaMybatisPlusConfigurationException.java
            │   │   ├── extension/{EgonColaMapper.java,EgonColaIService.java,EgonColaServiceImpl.java}
            │   │   ├── handler/EgonColaMetaObjectHandler.java
            │   │   ├── interceptor/{EgonColaTenantIdGuardInnerInterceptor.java,EgonColaModelValidationInterceptor.java}
            │   │   └── model/{EgonModel.java,EgonColaModelValidationGroups.java,EgonColaModelValidationUtils.java}
            │   └── resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
            └── test/
                ├── java/top/egon/cola/component/common/mybatis/
                │   ├── autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java
                │   ├── contract/{EgonColaIServiceParityTest.java,EgonModelActiveRecordParityTest.java}
                │   ├── extension/EgonColaServiceImplTest.java
                │   ├── integration/{EgonColaTenantIdSqlIntegrationTest.java,EgonColaActiveRecordIntegrationTest.java,EgonColaModelValidationIntegrationTest.java,EgonColaBatchTransactionIntegrationTest.java}
                │   ├── model/EgonModelTest.java
                │   └── support/{TestBusinessDTO.java,TestBusinessPO.java,TestBusinessModel.java,TestBusinessMapper.java,TestBusinessService.java,TestTenantIdProvider.java,TestUserIdProvider.java,TestBusinessConverters.java}
                └── resources/schema.sql
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml` | Core has no Validation dependency. | API-only `jakarta.validation-api`. | Core dependency boundary. | Step 1 | `REQ-017`,`REQ-026` | Core tests/dependency tree |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/validation/ValidationUtils.java` | Class absent. | Instance facade and stable violations. | Manual Jakarta validation. | Step 1 | `REQ-026` | `ValidationUtilsTest` |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java` | Rejects every Jakarta import. | Permits only validation imports. | Core architectural boundary. | Step 1 | `REQ-017`,`REQ-026` | `CoreBoundaryTest` |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/validation/ValidationUtilsTest.java` | Test absent. | Object/property/value/group contract suite. | Core facade proof. | Step 1 | `REQ-026` | Self |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java` | Only blanket forbidden-import assertion. | Backward-compatible allow-prefix overload. | Reusable narrow source gate. | Step 1 | `REQ-017`,`REQ-026` | Boundary tests |
| MODIFY | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java` | Covers blanket behavior only. | Covers exact allowed prefix and continued rejection. | Allowlist regression proof. | Step 1 | `REQ-017` | Self |
| MODIFY | `egon-cola-components/pom.xml` | No MP version/management. | 3.5.16 starter/jsqlparser management. | Version freeze. | Step 2 | `REQ-017`,`REQ-019` | Effective POM |
| MODIFY | `egon-cola-components/egon-cola-component-common/pom.xml` | Seven children; no MP child. | Adds one Starter module. | Common aggregation. | Step 2 | `REQ-001` | Common reactor |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml` | Module absent. | Production/test scopes and package tooling. | Starter Artifact. | Step 2 | `REQ-001`,`REQ-017`,`REQ-019`,`REQ-021` | Maven model/package |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonModelActiveRecordParityTest.java` | Test absent. | 14-method/final-root parity. | Freeze AR ABI. | Step 3 | `REQ-024` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessModel.java` | Fixture absent. | Concrete table Model with business constraint/version. | Shared repository fixture. | Step 3 | `REQ-025`,`REQ-030` | Model/integration tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationGroups.java` | Groups absent. | Insert/Update/Delete/Query/Persisted markers and operation enum. | Validation lifecycle vocabulary. | Step 3 | `REQ-025`,`REQ-027` | Model tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java` | AR base absent. | Seven fields, annotations, hooks, six final roots. | Persistent AR base. | Step 3 | `REQ-018`,`REQ-024`,`REQ-025` | AR/model tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java` | Test absent. | Field, MDC/fill, hook, and authoritative metadata tests. | Unit lifecycle proof. | Step 4 | `REQ-007`,`REQ-008`,`REQ-015`,`REQ-025` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestTenantIdProvider.java` | Fixture absent. | Thread-confined mutable Provider. | Tenant test control. | Step 4 | `REQ-007`,`REQ-010` | Integration tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestUserIdProvider.java` | Fixture absent. | Mutable UserID Provider. | Audit test control. | Step 4 | `REQ-015` | Model/integration tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java` | Properties absent. | Prefix plus validated nested defaults. | Technical configuration contract. | Step 4 | `REQ-003`,`REQ-011`-`REQ-016` | Context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdProvider.java` | SPI absent. | `Long currentTenantId()`. | Trusted tenant source Strategy. | Step 4 | `REQ-007`,`REQ-010`,`REQ-016` | Model/context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcTenantIdProvider.java` | Adapter absent. | Configured MDC parse/fail-closed. | Default tenant adapter. | Step 4 | `REQ-007`,`REQ-008` | Model/context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaUserIdProvider.java` | SPI absent. | `String currentUserId()`. | Trusted audit source Strategy. | Step 4 | `REQ-015`,`REQ-016` | Model tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcUserIdProvider.java` | Adapter absent. | Configured nonblank MDC ID. | Default audit adapter. | Step 4 | `REQ-015` | Model/context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java` | Handler absent. | Final insert/update fill templates. | Authoritative common fields. | Step 4 | `REQ-008`,`REQ-015`,`REQ-016`,`REQ-025` | Model/H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java` | Test absent. | Exact 57/zero-extra API parity and no-Injector scan. | Freeze official surface. | Step 5 | `REQ-004`,`REQ-006`,`REQ-014`,`REQ-019` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java` | Test absent. | Method-family unit contract. | Enhanced Service proof. | Step 5 | `REQ-005`,`REQ-009`-`REQ-011`,`REQ-013`,`REQ-018` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java` | API absent. | Empty generic BaseMapper extension. | Unified Mapper type. | Step 5 | `REQ-004`,`REQ-014` | Parity/H2 |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java` | API absent. | Exactly 57 declarations. | Frozen technical Service API. | Step 5 | `REQ-005`,`REQ-006` | Parity test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationUtils.java` | Facade absent. | Operation/Persisted/tenant-aware Model validation. | Repository manual validation. | Step 5 | `REQ-027`,`REQ-030` | Service/integration tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java` | Base absent. | 57 explicit enhanced implementations. | Technical persistence Service. | Step 5 | `REQ-005`,`REQ-006`,`REQ-009`-`REQ-011`,`REQ-013`,`REQ-018` | Parity/Service/H2 |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java` | Fixture absent. | Zero-method production path plus test-only custom statements. | MyBatis fixture. | Step 5 | `REQ-004`,`REQ-014` | H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java` | Fixture absent. | Concrete constructor-forwarding Service. | Transaction/service fixture. | Step 5 | `REQ-005`,`REQ-018` | H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java` | Test absent. | Import/disable/backoff/order/fail-fast matrix. | Boot context proof. | Step 6 | `REQ-003`,`REQ-012`,`REQ-016` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaTenantIdSqlIntegrationTest.java` | Test absent. | Official scoped SQL/logic-delete/guard/default-Injector matrix. | Real SQL proof. | Step 6 | `REQ-008`-`REQ-010`,`REQ-012`-`REQ-018` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java` | Exception absent. | Stable low-cardinality startup error codes. | Configuration failure model. | Step 6 | `REQ-016` | Context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdTenantLineHandler.java` | Handler absent. | LongValue/current tenant and ignored tables. | MP TenantLine adapter. | Step 6 | `REQ-007`,`REQ-010`,`REQ-016` | SQL tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaTenantIdGuardInnerInterceptor.java` | Guard absent. | AST/parameter equality and protected-column checks. | Fail-closed SQL guard. | Step 6 | `REQ-007`,`REQ-010`,`REQ-012` | SQL tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaModelValidationInterceptor.java` | Plugin absent. | Post-fill parameter and result Model validation. | Global repository validation. | Step 6 | `REQ-027`,`REQ-030` | Validation integration |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java` | Auto-config absent. | Ordered conditional bean graph. | Starter wiring. | Step 6 | `REQ-003`,`REQ-012`,`REQ-015`-`REQ-017`,`REQ-027` | Context/H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java` | Validator absent. | Startup chain/handler/plugin verification. | Safe replacement enforcement. | Step 6 | `REQ-012`,`REQ-014`,`REQ-016` | Context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Resource absent. | One auto-configuration class name. | Boot discovery. | Step 6 | `REQ-003` | ImportCandidates test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql` | Schema absent. | H2 business/global fixture with common columns. | SQL-only test schema. | Step 6 | `REQ-008`,`REQ-021`,`REQ-022`,`REQ-025` | H2/package gates |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaActiveRecordIntegrationTest.java` | Test absent. | Real 14-AR-entry scoping/lifecycle matrix. | ActiveRecord proof. | Step 7 | `REQ-010`,`REQ-018`,`REQ-024`-`REQ-027` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaModelValidationIntegrationTest.java` | Test absent. | Parameter/result/MVC/PO/Model validation matrix. | Layered validation proof. | Step 7 | `REQ-027`,`REQ-028`,`REQ-030` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java` | Test absent. | Atomic batch/context drift/bounds/concurrency matrix. | Transaction proof. | Step 7 | `REQ-011`,`REQ-018`,`REQ-021` | Self |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessDTO.java` | Fixture absent. | DTO constraints without technical fields. | Controller-boundary fixture. | Step 7 | `REQ-028`,`REQ-029` | Validation integration |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessPO.java` | Fixture absent. | PO constraints/business-rule input. | Service-boundary fixture. | Step 7 | `REQ-028`,`REQ-029`,`REQ-030` | Validation integration |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessConverters.java` | Fixture absent. | Two explicit BaseConverter implementations. | Object-flow mapping proof. | Step 7 | `REQ-029` | Validation integration |
| MODIFY | `egon-cola-components/egon-cola-components-bom/pom.xml` | Existing concrete Common artifacts exported. | Adds managed Starter dependency. | Consumer adoption. | Step 8 | `REQ-001`,`REQ-019` | Effective BOM |
| MODIFY | `egon-cola-components/egon-cola-component-common/README.md` | Existing English module table. | Module entry and concise boundary. | Aggregate documentation. | Step 8 | `REQ-020` | Docs gate |
| MODIFY | `egon-cola-components/egon-cola-component-common/README.zh-CN.md` | Chinese mirror exists. | Synchronized module entry/boundary. | Aggregate documentation. | Step 8 | `REQ-020` | Docs gate |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md` | File absent. | Complete English adoption guide. | Module documentation. | Step 8 | `REQ-020`,`REQ-024`-`REQ-030` | Docs/source gate |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md` | File absent. | Complete Chinese mirror. | Module documentation. | Step 8 | `REQ-020`,`REQ-024`-`REQ-030` | Docs/source gate |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Apply the user-provided AGENTS rules: smallest safe change, repository JavaDoc/annotation style, design-pattern restraint, exact validation evidence, one tested commit per Step, and no automatic service startup.
- Immediately before Step 1, re-run `git branch --show-current`, `git rev-parse HEAD`, and `git status --short`; reconcile only drift inside the target paths and stop if a public contract changed after the accepted Spec.
- Preserve the staged deletion of `GatewayContractVersions.java`, untracked file `0`, unrelated Gateway/RPC/IdP Spec/Plan documents, and all other user changes. Never stage by workspace-wide glob.
- The accepted Spec, this Review Plan, and the superseded Plan are approval/relationship artifacts, not implementation commit paths.
- There is no production database change. `schema.sql` is a test resource; any adopting application needs a separate reviewed schema Spec and exactly one new migration under its own `classpath:db`.
- Do not start the application after implementation. The user owns runtime testing.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Toolchain | `./mvnw -version` | Wrapper uses Maven >= 3.9.14 and Java 21-compatible JDK. | Toolchain only |
| Clean target baseline | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -DskipTests validate` | Existing Common Maven model resolves before target edits. | Static Maven model |
| Upstream ABI | Local/published MP 3.5.16 Jars recorded by Spec `EVD-010`,`EVD-016`; parity tests compile against dependencies. | 57 IService-visible methods and 14 AbstractModel AR methods. | Published ABI, not live DB |
| Auto-config pattern | Existing `IdGeneratorAutoConfiguration` and `AutoConfiguration.imports` in Common ID Starter. | Configuration is discovered without component scan and defaults back off. | Repository/static/context |
| Validation boundary | Existing `CoreBoundaryTest` and `SourceBoundaryAssert`. | Only `jakarta.validation.*` becomes allowed in core. | Source imports/dependency tree |
| SQL runtime | Module test dependencies `spring-boot-starter-test`, test-only `spring-boot-starter-web`, and H2. | In-memory MyBatis context only; no external credentials. | Embedded module integration |
| Worktree safety | `git status --short -- egon-cola-components docs/egon` plus Step-specific `git diff --cached --name-only`. | Only Step-owned target paths are staged. | Git/static |

### 6.3 Immutable constraints and approved decisions

- Direct production dependencies are `mybatis-plus-spring-boot3-starter:3.5.16`, same-version `mybatis-plus-jsqlparser`, common-core, Boot Validation, and only already-standard supporting APIs. Do not directly declare native MyBatis Starter, `mybatis`, `mybatis-spring`, raw `mybatis-plus`, a database driver, Security, ShardingSphere, or platform modules.
- The only tenant field is non-null persistent `Long tenantId` / `tenant_id`; every Long value is legal. Missing/malformed context fails before JDBC. There is no `businessId`, audit name, `deleted` property, static tenant holder, caller-supplied cross-tenant API, or bypass annotation exposed by this Starter.
- `EgonModel` contains exactly `id`, `tenantId`, `createUserId`, `createTime`, `updateUserId`, `updateTime`, `isDeleted`; Java time is `Instant`, logical-delete column is `is_deleted`, and ordinary update cannot mutate `tenant_id` or `is_deleted`.
- `EgonColaMapper` declares zero methods. The official default Injector remains untouched; no `ISqlInjector` bean or custom `AbstractMethod` is created.
- `EgonColaIService` and `EgonColaServiceImpl` visibly declare exactly all 57 MP 3.5.16 signatures. Private family helpers may centralize checks but may not erase declarations, add current-tenant aliases, change return shapes, or upgrade to 3.5.17.
- DTO, PO, and Model remain separate consumer-owned roles. Only test fixtures model their flow here. Complex database/authorization/state rules remain in consumer business Services, not in `EgonModel`, constraints, or the technical Service base.
- All AR/Mapper/IService writes and reads converge on the same MyBatis fill/guard/TenantLine/validation chain. Batch methods additionally capture one method-entry TenantID snapshot and remain transactional.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | Order `EgonColaMybatisPlusAutoConfiguration` before both `MybatisPlusInnerInterceptorAutoConfiguration` and `MybatisPlusAutoConfiguration`. | MP 3.5.16 inner auto-config collects `List<InnerInterceptor>` while main auto-config consumes Handler/plugins. | It ensures the Spec-defined beans exist before official conditional collection; public configuration and plugin order are unchanged. | If Boot ordering differs, ContextRunner/H2 RED returns to the auto-config file before commit. |
| `PLAN-CLAR-002` | Implement startup contract checking as `SmartInitializingSingleton` over final beans rather than an earlier post-processor. | The required chain can only be inspected after conditional custom beans and the official outer interceptor are instantiated. | The same startup failure contract is enforced at the first safe lifecycle point and no request can execute first. | If initialization timing is too late/early, the ContextRunner failure test returns to the validator design. |
| `PLAN-CLAR-003` | Add `SourceBoundaryAssert.assertNoForbiddenImports(Path, Set<String> allowedImportPrefixes)` and keep the existing overload delegating with an empty set. | Repository search finds only the current signature/tests; core needs one exact validation-only exception. | Existing consumers retain blanket behavior; only explicit callers opt into the narrow allowance. | A discovered external source consumer still compiles because the old method remains. |
| `PLAN-CLAR-004` | Add `spring-boot-starter-web` in test scope only for the MVC DTO-validation slice. | Production scope has no HTTP contract; Spec `TEST-072` requires automatic Controller validation proof. | It proves an adoption boundary without adding web runtime to the published Starter. | If MockMvc can be avoided with existing test facilities, retaining or removing this test-only dependency is an implementation detail verified by dependency tree. |

## 7. Ordered File-by-file Implementation Steps

> Every Step is independently verifiable and commit-sized. Test files precede the production behavior they define, except Maven bootstrap files that must exist before a test source can compile.

### Step 1 — Establish the common-core Jakarta Validation facade and narrow source boundary

- Requirements: `REQ-017`, `REQ-026`
- Dependencies: `None`
- Baseline state: common-core has no Validation dependency or facade; `CoreBoundaryTest` and `SourceBoundaryAssert` reject every `jakarta.*` import, and their current tests pass.
- Observable outcome: callers can inject a Jakarta `Validator` into `ValidationUtils` for object/property/value/group validation while core permits only `jakarta.validation.*` imports and retains every other framework prohibition.
- End state: the facade, API-only dependency, stable violation ordering, backward-compatible boundary overload, and focused tests are committed; no Starter source exists yet.
- Test-first gate: Required — Files 1-3 fail because `ValidationUtils` and the allow-prefix overload are missing and the existing core boundary rejects the new validation package.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/validation/ValidationUtilsTest.java`

- Purpose: Freeze all manual validation facade behaviors before adding the core implementation.
- Symbols: `ValidationUtilsTest`, constrained fixture/annotation, tests for `validate`, `violations`, `isValid`, `validateProperty`, and `validateValue` with groups.
- Repository evidence: common-core uses JUnit 5 contract tests and AssertJ-style assertions; Jakarta validation is already managed by the Boot parent but not declared in core.
- Dependencies and consumers: compiles against the future `ValidationUtils`, a test ValidatorFactory, and the core test runtime; later Model tests reuse the same error semantics.
- Why now: This is the RED contract for `INTERNAL-065` and prevents a Spring/static implementation from entering core.
- Contract/signature changes: asserts constructor null rejection, null/invalid target arguments, `ConstraintViolationException` for `validate*`, immutable deterministic sets for `violations*`, and group-sensitive results.
- Input/output and state mapping: valid/invalid beans and property values map to an empty/stably ordered violation set, boolean validity, or the standard exception containing the same violations; the facade mutates nothing.
- Error and edge behavior: null Validator/model/property/type, blank property, invalid group arrays, multiple violations, and attempts to mutate returned sets are covered.
- Implementation pseudocode:

```java
Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
ValidationUtils utils = new ValidationUtils(validator);
assertThat(utils.violations(invalidFixture, Insert.class)).extracting(v -> v.getPropertyPath().toString()).containsExactly("code", "name");
assertThatThrownBy(() -> utils.validate(invalidFixture, Insert.class)).isInstanceOf(ConstraintViolationException.class);
```

- Verification contribution: focused RED/GREEN selector proves the entire facade contract without Spring.
- After this file: The desired API is explicit and fails only because the production class is absent.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java`

- Purpose: Prove the new allowlist is exact and the existing blanket overload remains unchanged.
- Symbols: new tests `allowsConfiguredValidationPrefixOnly`, `rejectsOtherJakartaImportsWithAllowlist`, and existing blanket assertions.
- Repository evidence: this test already creates temporary Java sources and calls `SourceBoundaryAssert.assertNoForbiddenImports(Path)`.
- Dependencies and consumers: consumes the new overload in File 6; `CoreBoundaryTest` becomes the first production caller.
- Why now: The boundary exception must be test-first so it cannot become a general Jakarta bypass.
- Contract/signature changes: adds assertions for a set of allowed import prefixes while preserving the existing method's failure behavior and diagnostics.
- Input/output and state mapping: temporary files with validation, persistence, servlet, Spring, and mixed imports map to pass only for the configured validation prefix; source files remain untouched.
- Error and edge behavior: empty allowlist, exact prefix boundary, static imports, multiple files, and allowed-plus-forbidden combinations remain fail-closed.
- Implementation pseudocode:

```java
Path sourceRoot = writeSources("import jakarta.validation.Validator;", "import jakarta.persistence.Entity;");
assertThatCode(() -> SourceBoundaryAssert.assertNoForbiddenImports(validationOnlyRoot, Set.of("jakarta.validation."))).doesNotThrowAnyException();
assertThatThrownBy(() -> SourceBoundaryAssert.assertNoForbiddenImports(sourceRoot, Set.of("jakarta.validation."))).hasMessageContaining("jakarta.persistence.Entity");
assertThatThrownBy(() -> SourceBoundaryAssert.assertNoForbiddenImports(validationOnlyRoot)).hasMessageContaining("jakarta.validation.Validator");
```

- Verification contribution: proves the allowlist cannot weaken existing callers or permit unrelated Jakarta APIs.
- After this file: Boundary requirements are executable and RED on the missing overload.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java`

- Purpose: Change the core architecture gate from blanket Jakarta rejection to validation-only permission.
- Symbols: existing boundary test invocation plus assertions against Spring, persistence, servlet, and non-validation Jakarta imports.
- Repository evidence: the current test resolves core source root and delegates to `SourceBoundaryAssert` for forbidden imports.
- Dependencies and consumers: consumes File 6 overload and guards File 5 plus all future core source.
- Why now: The API dependency is allowed only if its repository boundary changes in the same commit.
- Contract/signature changes: passes `Set.of("jakarta.validation.")` to the reusable assertion; all other forbidden prefixes remain in force.
- Input/output and state mapping: the core source tree is scanned deterministically; a validation import passes and any unrelated framework import identifies its exact file/import.
- Error and edge behavior: missing source root remains an assertion failure, and the allowlist is not applied to generated/test source or Starter modules.
- Implementation pseudocode:

```java
Path coreMainJava = repositoryRoot.resolve("egon-cola-component-common-core/src/main/java");
Set<String> allowedImports = Set.of("jakarta.validation.");
SourceBoundaryAssert.assertNoForbiddenImports(coreMainJava, allowedImports);
assertCorePomHasNoSpringJpaServletOrImplementationValidationProvider();
```

- Verification contribution: owns the architectural GREEN gate for the narrowly amended predecessor rule.
- After this file: The intended core exception is recorded but cannot compile until the overload and dependency are present.

#### File 4 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml`

- Purpose: Provide only the Jakarta Validation API required by the facade.
- Symbols: one direct `jakarta.validation:jakarta.validation-api` dependency using parent management.
- Repository evidence: core currently contains API/JDK utilities and no Spring runtime; the Components parent manages Boot/Jakarta versions.
- Dependencies and consumers: compiles File 5 and its tests; Starter later supplies Boot's Validator implementation at runtime.
- Why now: Production imports cannot compile before the API edge exists.
- Contract/signature changes: adds no transitive provider or Spring dependency and changes no existing Artifact coordinates.
- Input/output and state mapping: Maven resolves annotations/interfaces only; runtime provider selection remains the consumer/ApplicationContext responsibility.
- Error and edge behavior: dependency-tree checks fail on Hibernate Validator, Spring, JPA, Servlet, or implementation-scoped provider leakage into core.
- Implementation pseudocode:

```xml
<dependency>
  <groupId>jakarta.validation</groupId><artifactId>jakarta.validation-api</artifactId>
</dependency>
<!-- keep core free of validation providers and Spring runtime artifacts -->
```

- Verification contribution: makes the facade compile while keeping the declared dependency boundary reviewable.
- After this file: Jakarta validation types resolve in core; the facade and source utility are still missing.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/validation/ValidationUtils.java`

- Purpose: Implement the instance-based manual validation Facade defined by File 1.
- Symbols: constructor, `validate`, `violations`, `isValid`, `validateProperty`, `propertyViolations`, `validateValue`, and `valueViolations` overload families.
- Repository evidence: core public utilities use constructor validation and standard Java/Jakarta exceptions; no static ApplicationContext or singleton Validator exists.
- Dependencies and consumers: wraps `jakarta.validation.Validator`; consumed by repository Model validation and consumer Controller/Service manual validation.
- Why now: Test contracts and the API dependency are fixed.
- Contract/signature changes: exposes typed group varargs and immutable `Set<ConstraintViolation<T>>`; throwing variants use standard `ConstraintViolationException`.
- Input/output and state mapping: delegates to the injected Validator, sorts violations by property path, constraint annotation type, and message into a `LinkedHashSet`, and returns an unmodifiable copy.
- Error and edge behavior: rejects null target/type/property/groups and blank property with `IllegalArgumentException`; empty violations do not throw; no input or Validator state is cached.
- Implementation pseudocode:

```java
Set<ConstraintViolation<T>> ordered = validator.validate(requireTarget(target), requireGroups(groups)).stream()
    .sorted(comparing(path).thenComparing(annotationType).thenComparing(ConstraintViolation::getMessage))
    .collect(toCollection(LinkedHashSet::new));
Set<ConstraintViolation<T>> result = Collections.unmodifiableSet(ordered);
if (throwOnViolation && !result.isEmpty()) throw new ConstraintViolationException(result);
```

- Verification contribution: turns all File 1 behavior GREEN and provides the common dependency for Step 5.
- After this file: Manual object/property/value/group validation is available without Spring or static state.

#### File 6 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java`

- Purpose: Add an explicit allowed-import-prefix seam while preserving the original strict API.
- Symbols: overloaded `assertNoForbiddenImports(Path, Set<String>)` and delegation from `assertNoForbiddenImports(Path)`.
- Repository evidence: the existing utility performs source scanning with a fixed forbidden prefix set and repository-standard AssertionError diagnostics.
- Dependencies and consumers: called by File 3; existing tests/callers continue using the original signature.
- Why now: This is the minimum reusable change that supports the accepted core amendment.
- Contract/signature changes: allowed prefixes suppress only a forbidden import that starts with an exact configured prefix; no default allowance is introduced.
- Input/output and state mapping: normalized immutable allow prefixes plus parsed import lines produce the same deterministic violation list and error message structure.
- Error and edge behavior: rejects null path/set/prefix and blank prefixes; canonicalizes separators; an allowed prefix never hides another forbidden import on the same file.
- Implementation pseudocode:

```java
public static void assertNoForbiddenImports(Path root) { assertNoForbiddenImports(root, Set.of()); }
Set<String> allowed = allowedImportPrefixes.stream().map(SourceBoundaryAssert::requirePrefix).collect(toUnmodifiableSet());
boolean forbidden = FORBIDDEN_PREFIXES.stream().anyMatch(importName::startsWith);
if (forbidden && allowed.stream().noneMatch(importName::startsWith)) violations.add(source + ":" + importName);
```

- Verification contribution: makes Files 2-3 GREEN without changing any existing caller semantics.
- After this file: Step 1 has a complete manual validation API and narrowly enforced dependency boundary.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-core,egon-cola-component-common-test -am -Dtest=ValidationUtilsTest,CoreBoundaryTest,SourceBoundaryAssertTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-core -am dependency:tree`
- Expected result: exit 0; named tests pass; dependency tree shows `jakarta.validation-api` but no core-direct/runtime Spring, JPA, Servlet, or validation provider edge.
- Failure returns to: File 5 for facade semantics, File 6 for allowlist behavior, File 4 for dependency leakage, or the Spec if another Jakarta family is actually required.
- Completion criteria: all Step requirements pass, `git diff --check` is clean for Step paths, and no unrelated path is staged.
- Rollback: revert the one Step commit; the original blanket boundary and dependency-free core are restored together.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/validation/ValidationUtilsTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/test/java/top/egon/cola/component/common/test/SourceBoundaryAssertTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/test/java/top/egon/cola/component/common/core/CoreBoundaryTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/pom.xml`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-core/src/main/java/top/egon/cola/component/common/core/validation/ValidationUtils.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-test/src/main/java/top/egon/cola/component/common/test/SourceBoundaryAssert.java`
- Commit: `feat(common-core): add jakarta validation facade`

### Step 2 — Publish the compilable MyBatis-Plus Starter Maven boundary

- Requirements: `REQ-001`, `REQ-017`, `REQ-019`, `REQ-021`, `REQ-023`
- Dependencies: `Step 1`
- Baseline state: Components and Common parents build seven existing Common modules; no MyBatis-Plus version, Artifact, child module, Java source, import resource, or test fixture exists.
- Observable outcome: Maven resolves one new empty Starter Jar with exact official MP 3.5.16 production dependencies and embedded-H2/web-test dependencies scoped to tests.
- End state: parent version management, Common aggregation, and the child POM are committed; the module compiles/packages but intentionally has no auto-configuration import or public Java API until later Steps.
- Test-first gate: Not applicable — a Java RED test cannot exist before its module and dependencies; the pre-change Maven selector fails because the child Artifact is unknown, which is the observable bootstrap gap.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/pom.xml`

- Purpose: Freeze MyBatis-Plus dependency versions once at the Components parent.
- Symbols: `mybatis-plus.version=3.5.16` and dependencyManagement entries for Boot3 Starter and JSqlParser.
- Repository evidence: this POM owns Java 21, Boot 3.5.16, shared dependency versions, compiler, Surefire, and Enforcer policy.
- Dependencies and consumers: consumed by the new Starter POM and effective BOM builds; no existing child gains a direct dependency.
- Why now: The child POM must not hardcode divergent official versions.
- Contract/signature changes: manages two `com.baomidou` coordinates at the same property version; parent coordinates and existing management remain unchanged.
- Input/output and state mapping: a versionless child dependency resolves to 3.5.16; transitive native MyBatis remains solely an official Starter implementation detail.
- Error and edge behavior: Enforcer/effective-POM checks fail on 3.5.17 drift, raw `mybatis-plus`, native Starter management, or mismatched JSqlParser version.
- Implementation pseudocode:

```xml
<mybatis-plus.version>3.5.16</mybatis-plus.version>
<dependencyManagement><dependencies>
  <dependency>mybatis-plus-spring-boot3-starter:${mybatis-plus.version}</dependency><dependency>mybatis-plus-jsqlparser:${mybatis-plus.version}</dependency>
</dependencies></dependencyManagement>
```

- Verification contribution: effective POM provides the single version source checked again in Steps 5 and 8.
- After this file: The parent can manage the future child, but Common does not aggregate it yet.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/pom.xml`

- Purpose: Add exactly one new concrete capability to the Common reactor.
- Symbols: module entry `egon-cola-component-common-mybatis-plus-spring-boot-starter`.
- Repository evidence: Common is a packaging `pom` with seven sibling modules and no dependencies of its own.
- Dependencies and consumers: makes File 3 reachable through Common and Components `-am` builds; existing children remain siblings.
- Why now: The new child must be buildable from the repository-native aggregate.
- Contract/signature changes: adds one module only; does not add dependencies, source, or a separate test/core child.
- Input/output and state mapping: Maven reactor discovery maps the relative directory to the child Artifact and keeps prior module ordering stable.
- Error and edge behavior: duplicate module names, missing directory/POM, parent-relativePath errors, and cycles fail Maven validate immediately.
- Implementation pseudocode:

```text
Common modules retain every existing child in the current order.
Append exactly: egon-cola-component-common-mybatis-plus-spring-boot-starter
Maven resolves that relative directory as one sibling Jar module, without adding an aggregate dependency.
```

- Verification contribution: Common reactor can now address the new Artifact by module path/Artifact ID.
- After this file: Maven expects File 3 to exist; no Java behavior is published.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`

- Purpose: Define production/test dependency scopes and repository-standard build behavior for the Starter Jar.
- Symbols: Artifact coordinates, parent, production dependencies, configuration processor, and test-only Boot/H2/web dependencies.
- Repository evidence: ID and desensitize Starters use the Common parent, Boot autoconfigure/test, configuration processor, JUnit/ContextRunner, and Jar packaging conventions.
- Dependencies and consumers: production uses MP Boot3 Starter, MP JSqlParser, common-core, Boot Validation, and SLF4J as needed; tests use common-test, Boot test/web, and H2.
- Why now: All later source and tests compile inside this one module.
- Contract/signature changes: creates one opt-in Jar; it does not directly declare raw/native MyBatis, database, Security, ShardingSphere, or unrelated Common capabilities.
- Input/output and state mapping: Maven compile classpath exposes MP/Validation/Core; test classpath adds embedded fixtures; the packaged Jar excludes all test classes/resources by normal lifecycle.
- Error and edge behavior: dependency-tree/source gates reject direct forbidden coordinates, version overrides, production H2/web, or accidental test-fixture attachment.
- Implementation pseudocode:

```xml
<artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
<dependencies><!-- MP Boot3 Starter + JSqlParser + common-core + Boot Validation in production scope -->
  <!-- common-test + Boot test/web + H2 are test scope only; configuration processor is optional -->
</dependencies>
```

- Verification contribution: owns the Artifact resolution, dependency-scope, and empty-Jar package gates.
- After this file: Step 2 ends with a compilable module ready for model-first test-driven source.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -DskipTests package && ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter dependency:tree`
- Expected result: exit 0; Maven builds one empty Starter Jar; dependency tree resolves both MP coordinates at 3.5.16, shows test-only H2/web only under test scope, and contains no direct raw/native MyBatis declaration.
- Failure returns to: File 3 for scope/parent problems, File 1 for management/version drift, or File 2 for reactor discovery.
- Completion criteria: the Artifact packages from the Components parent, no auto-configuration class/resource exists prematurely, and path-scoped status contains only the three POMs.
- Rollback: revert the Step commit; Maven no longer knows the child and no source/data state exists to migrate.
- Commit paths: `egon-cola-components/pom.xml`, `egon-cola-components/egon-cola-component-common/pom.xml`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`
- Commit: `feat(common-mybatis-plus): publish starter module boundary`

### Step 3 — Freeze EgonModel fields, validation groups, and ActiveRecord lifecycle

- Requirements: `REQ-002`, `REQ-018`, `REQ-024`, `REQ-025`, `REQ-030`
- Dependencies: `Step 2`
- Baseline state: the Starter module compiles but has no Java types; MP 3.5.16 exposes `AbstractModel` with 14 public AR methods and no `Model` class.
- Observable outcome: concrete business Models can extend a seven-field `EgonModel`, inherit all 14 AR abilities, and use six final write/delete Template Methods with single before/after technical hooks.
- End state: validation-group vocabulary, AR base, concrete constrained fixture, and parity tests are committed; tenant/user Providers and actual fill remain intentionally absent until Step 4.
- Test-first gate: Required — Files 1-2 fail because the generic model base, exact annotations/fields, hooks, and operation groups do not exist.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonModelActiveRecordParityTest.java`

- Purpose: Freeze the MP 3.5.16 AR ABI and the exact enhanced root-method shape.
- Symbols: `EgonModelActiveRecordParityTest`, reflection helpers, final-method and dynamic-dispatch assertions.
- Repository evidence: the accepted Spec records `javap` evidence for 14 `AbstractModel` methods; repository contract tests already use reflection for API parity.
- Dependencies and consumers: reflects future `EgonModel` and `TestBusinessModel`; guards all AR consumers and upgrades.
- Why now: Parent ABI and lifecycle override decisions must be executable before writing inheritance code.
- Contract/signature changes: asserts superclass `AbstractModel`, 14 effective methods, six declared final roots, protected hook signatures, inherited `insertOrUpdate`, and no static Spring lookup field/method.
- Input/output and state mapping: reflection maps parent/effective signatures to exact sets; a spy subclass invokes insert/update/delete paths and records each hook once around the upstream result.
- Error and edge behavior: missing/extra override, non-final root, hook double invocation, wrong visibility/generic return, old `Model` reference, or copied `insertOrUpdate` algorithm fails.
- Implementation pseudocode:

```java
Set<MethodKey> upstream = publicInstanceMethods(AbstractModel.class);
Set<MethodKey> effective = publicInstanceMethods(EgonModel.class);
assertThat(effective).containsAll(upstream).hasSize(upstream.size());
assertFinalRootsAndSingleDynamicHookDispatch(EgonModel.class, INSERT, UPDATE_BY_ID, DELETE_BY_ID, DELETE, SELECTIVE_DELETE, SELECTIVE_UPDATE);
```

- Verification contribution: prevents silent AR loss or an implementation against the obsolete `Model<M>` documentation class.
- After this file: AR expectations are RED on the absent base and fixture.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessModel.java`

- Purpose: Provide one concrete Model used by structural, Service, and H2 tests.
- Symbols: `TestBusinessModel extends EgonModel<TestBusinessModel>`, `title`, `payload`, and optional `version` field/annotations.
- Repository evidence: MP tests require a concrete `@TableName` entity and Mapper generic; existing project POJOs use explicit Jakarta constraints and MyBatis annotations where applicable.
- Dependencies and consumers: consumed by every Starter test after this Step; maps only the H2 `test_business_record` fixture.
- Why now: Generic self-type, inherited fields, subclass constraints, logic delete, and optimistic lock need a real compile target.
- Contract/signature changes: test-only table mapping adds `@NotBlank title`, nullable payload, and `@Version` to exercise subclass/default validation and optimistic locking.
- Input/output and state mapping: constructors/setters populate business fields only; seven technical fields remain inherited and are filled later by runtime infrastructure.
- Error and edge behavior: blank title is invalid, payload may be null, version begins null/new and increments through MP; no tenant/audit field is shadowed.
- Implementation pseudocode:

```java
@TableName("test_business_record")
final class TestBusinessModel extends EgonModel<TestBusinessModel> {
    @NotBlank private String title; private String payload; @Version private Long version;
    TestBusinessModel businessValues(String title, String payload) { this.title = title; this.payload = payload; return this; }
}
```

- Verification contribution: gives parity/model tests a concrete self-typed entity and later integration tests one stable mapping.
- After this file: Tests compile only after the groups and base Model are created.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationGroups.java`

- Purpose: Define operation-aware validation vocabulary shared by Model, repository facade, Service, and plugin.
- Symbols: marker interfaces `Insert`, `Update`, `Delete`, `Query`, `Persisted`; enum `Operation { INSERT, UPDATE, DELETE, QUERY, LOADED }` with group mapping.
- Repository evidence: no current Common Model groups exist; Jakarta marker interfaces and enum mapping are the smallest explicit contract.
- Dependencies and consumers: referenced by `EgonModel` annotations, Step 5 validation facade/Service, and Step 6 plugin.
- Why now: The seven fields and root methods need stable group names before annotations are authored.
- Contract/signature changes: adds public nested marker types and a non-null operation-to-primary-group mapping; Jakarta `Default` remains the standard group.
- Input/output and state mapping: each operation enum resolves deterministically to one marker; `LOADED` uses Persisted plus Default through the facade rather than inventing a loaded annotation group.
- Error and edge behavior: null operations are rejected by consumers; groups carry no state and do not inherit unrelated groups.
- Implementation pseudocode:

```java
public final class EgonColaModelValidationGroups {
    public interface Insert {} public interface Update {} public interface Delete {} public interface Query {} public interface Persisted {}
    public enum Operation { INSERT(Insert.class), UPDATE(Update.class), DELETE(Delete.class), QUERY(Query.class), LOADED(Query.class); private final Class<?> group; }
}
```

- Verification contribution: supplies exact annotation/group types inspected by AR/model tests and later validation integration.
- After this file: The lifecycle vocabulary compiles independently; it has no Validator or Spring coupling.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java`

- Purpose: Implement the unified seven-field ActiveRecord persistence base and final lifecycle roots.
- Symbols: generic `EgonModel<M extends EgonModel<M>> extends AbstractModel<M>`, seven fields, accessors, `pkVal`, six final AR roots, six protected hooks.
- Repository evidence: Spec `EVD-016` confirms the actual 3.5.16 superclass/methods; nearby project models use Lombok only where already configured, so implementation follows existing explicit annotation/style convention.
- Dependencies and consumers: bounded by MP/Jakarta; extended by consumer Models and File 2; consumed by Mapper/IService and validation plugins.
- Why now: It is the foundational compile-time boundary for every persistence entry.
- Contract/signature changes: fields are `Long id/tenantId`, `String createUserId/updateUserId`, `Instant createTime/updateTime`, `Boolean isDeleted`; annotations map TableId/fill/never-update/logic-delete and Persisted non-null rules exactly.
- Input/output and state mapping: `pkVal()` returns id; final roots perform lightweight argument checks, call one before hook, delegate to `super`, call one after hook only on normal return, and preserve the upstream boolean result.
- Error and edge behavior: null required id/wrapper fails before SqlSession; hook/super exceptions propagate and skip after hook; no context lookup, business I/O, extra field, user name, or ordinary `tenant_id/is_deleted` SET is introduced.
- Implementation pseudocode:

```java
@TableId(type = IdType.ASSIGN_ID) @NotNull(groups = Persisted.class) private Long id;
@TableField(fill = INSERT_UPDATE, updateStrategy = NEVER) @NotNull(groups = Persisted.class) private Long tenantId;
@TableLogic(value = "0", delval = "1") @TableField("is_deleted") @NotNull(groups = Persisted.class) private Boolean isDeleted;
public final boolean updateById() { beforeUpdate(); boolean result = super.updateById(); afterUpdate(result); return result; }
```

- Verification contribution: makes AR parity GREEN and gives later fill/validation/SQL tests their exact persistent contract.
- After this file: Step 3 publishes the AR/model API; technical fields may still be null before the Step 4 handler runs.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonModelActiveRecordParityTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; reflection proves all 14 upstream AR methods, exact final roots/hooks, generic superclass, field metadata, and no copied old `Model` dependency.
- Failure returns to: File 4 for superclass/method/annotation mismatch, File 3 for group shape, or the Spec if the resolved 3.5.16 ABI differs from accepted evidence.
- Completion criteria: parity is exact, production API contains only approved public names/fields, and no context/fill behavior is falsely claimed.
- Rollback: revert the Step commit; no consumer schema or runtime state exists.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonModelActiveRecordParityTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessModel.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationGroups.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonModel.java`
- Commit: `feat(common-mybatis-plus): add egon active record model`

### Step 4 — Add TenantID/UserID context Strategies and authoritative common-field fill

- Requirements: `REQ-003`, `REQ-007`, `REQ-008`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-025`
- Dependencies: `Step 3`
- Baseline state: EgonModel fields/hooks exist but no source can supply tenant/user IDs and no MetaObjectHandler or configuration defaults fill them.
- Observable outcome: direct unit calls prove configured MDC parsing, overrideable Provider contracts, exact fixed-Clock insert/update fill, protected extension hooks, and all validated technical defaults.
- End state: properties, four Provider types, MetaObjectHandler, two mutable test Providers, and comprehensive Model unit tests are committed; Spring bean wiring waits for Step 6.
- Test-first gate: Required — File 1 fails on absent properties/Providers/handler and on technical fields remaining caller-controlled or null.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`

- Purpose: Specify seven-field metadata plus MDC Provider and exact MetaObjectHandler lifecycle behavior.
- Symbols: `EgonModelTest`, metadata assertions, MDC isolation tests, fixed-Clock insert/update fill tests, custom handler hook spy.
- Repository evidence: Common Starter tests use JUnit cleanup for MDC and direct construction for deterministic utility behavior; MP exposes `MetaObject` test helpers.
- Dependencies and consumers: drives Files 4-9 and reuses `TestBusinessModel`; later ContextRunner/H2 tests complement rather than duplicate it.
- Why now: Context parsing and fill authority must be RED before writing adapters/handler.
- Contract/signature changes: asserts default/configured MDC keys, arbitrary Long acceptance, stable error codes, exact six insert/three update fields, no id/name fill, `Instant`, `isDeleted=false`, and final handler methods.
- Input/output and state mapping: forged Model technical values plus tenant/user Providers and fixed Clock map to authoritative insert/update values; non-EgonModel parameters retain normal handler no-op behavior.
- Error and edge behavior: missing/blank/malformed MDC, null user ID, negative/zero tenant IDs, thread isolation, hook exception propagation, update immutability of create fields/id, and no extra fields are covered.
- Implementation pseudocode:

```java
MDC.put("tenantId", "-7"); MDC.put("userId", "operator-1");
EgonColaMetaObjectHandler handler = new EgonColaMetaObjectHandler(tenantProvider, userProvider, fixedClock, properties);
handler.insertFill(SystemMetaObject.forObject(forgedModel));
assertThat(forgedModel).extracting("tenantId", "createUserId", "updateUserId", "isDeleted").containsExactly(-7L, "operator-1", "operator-1", false);
```

- Verification contribution: owns `TEST-003`,`TEST-012`-`014`,`TEST-039`-`043`-class unit evidence from the Spec matrix.
- After this file: The complete context/fill contract is executable and RED for missing production symbols.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestTenantIdProvider.java`

- Purpose: Provide deterministic current-tenant control for unit, H2, batch-drift, and concurrency tests.
- Symbols: `TestTenantIdProvider implements EgonColaTenantIdProvider`, thread-confined setter/clear/current methods.
- Repository evidence: test context must be isolated between threads; production forbids a custom static holder, but a test-only controlled Provider is the supported SPI seam.
- Dependencies and consumers: consumed by Model, auto-config, SQL, AR, validation, and batch integration tests.
- Why now: Direct handler tests need an overrideable Provider before Spring wiring exists.
- Contract/signature changes: test-only mutable implementation returns the current thread's boxed Long or reproduces configured missing-context failure.
- Input/output and state mapping: per-thread test assignment maps to `currentTenantId`; teardown removes the value so executor reuse cannot leak state.
- Error and edge behavior: unset value, explicit zero/negative values, concurrent threads, and cleanup after exceptions are asserted by consumers.
- Implementation pseudocode:

```java
final class TestTenantIdProvider implements EgonColaTenantIdProvider {
    private final ThreadLocal<Long> current = new ThreadLocal<>();
    void set(Long tenantId) { current.set(tenantId); } void clear() { current.remove(); }
    public Long currentTenantId() { return Objects.requireNonNull(current.get(), "TENANT_CONTEXT_MISSING"); }
}
```

- Verification contribution: enables deterministic tenant snapshots without changing production context architecture.
- After this file: Tests can inject tenant values directly; production still remains stateless/MDC-backed.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestUserIdProvider.java`

- Purpose: Provide deterministic audit user IDs for fill and H2 assertions.
- Symbols: `TestUserIdProvider implements EgonColaUserIdProvider`, mutable value setter and current-user method.
- Repository evidence: audit context is consumer-overridable and the test suite must avoid process MDC coupling where concurrency is not under test.
- Dependencies and consumers: passed to MetaObjectHandler in Model/auto-config/integration tests.
- Why now: Exact insert/update audit assertions require a controlled ID independent of wall-clock/user session.
- Contract/signature changes: test-only implementation returns a nonblank String ID and rejects missing/blank values with the same low-cardinality code.
- Input/output and state mapping: configured test ID maps to both create/update IDs on insert and update ID only on update.
- Error and edge behavior: null, empty, whitespace, changed ID between operations, and no user-name value are covered by consuming tests.
- Implementation pseudocode:

```java
final class TestUserIdProvider implements EgonColaUserIdProvider {
    private String userId;
    void set(String value) { userId = value; }
    public String currentUserId() { if (userId == null || userId.isBlank()) throw new IllegalStateException("USER_CONTEXT_MISSING"); return userId; }
}
```

- Verification contribution: isolates audit-field behavior from MDC adapter tests.
- After this file: Fixed handler tests have both trusted context sources.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java`

- Purpose: Define one validated technical configuration contract for the Starter.
- Symbols: `@ConfigurationProperties("egon.cola.component.mybatis-plus")`, nested tenant, audit, pagination, batch, block-attack, optimistic-locker, and meta-fill properties.
- Repository evidence: existing Common Starters use typed Boot properties, constructor/default values, and validation metadata rather than arbitrary environment reads.
- Dependencies and consumers: MDC adapters, handler, Service bounds, auto-config, interceptors, docs, and ContextRunner tests consume exact fields.
- Why now: Providers/handler must read stable keys/defaults before their constructors are fixed.
- Contract/signature changes: defaults are enabled=true, tenant key `tenantId`, user key `userId`, page max 500/overflow false, batch 1000/1000/10000, protection plugins enabled, ignored tables empty.
- Input/output and state mapping: Boot Binder maps configuration to typed immutable/validated nested state used directly by runtime beans.
- Error and edge behavior: blank keys, nonpositive/out-of-range page/batch sizes, contradictory default/max sizes, null ignored-table set, and invalid nested config fail context binding.
- Implementation pseudocode:

```java
@ConfigurationProperties("egon.cola.component.mybatis-plus")
@Validated public final class EgonColaMybatisPlusProperties {
    private boolean enabled = true; private final TenantId tenantId = new TenantId("tenantId", Set.of());
    private final Audit audit = new Audit("userId"); private final Pagination pagination = new Pagination(true, 500, false); private final Batch batch = new Batch(1000, 1000, 10000);
}
```

- Verification contribution: Model unit tests verify defaults; Step 6 ContextRunner verifies binding and invalid configurations.
- After this file: Context/fill classes can use one source of configuration truth.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdProvider.java`

- Purpose: Isolate the trusted current TenantID source from MDC and future SecurityContext.
- Symbols: public functional interface `EgonColaTenantIdProvider`, method `Long currentTenantId()`.
- Repository evidence: the accepted Spec selects a Provider Strategy and rejects static context access from Model/Service/plugin code.
- Dependencies and consumers: implemented by File 6/test Provider; consumed by handler, guard, TenantLine, Model validation, Service batch snapshots.
- Why now: All isolation/fill code needs the same constructor-injected seam.
- Contract/signature changes: publishes one method only; implementations must return non-null but accept any Long value.
- Input/output and state mapping: current execution security/request context maps to a boxed Long without caching or caller override.
- Error and edge behavior: implementations fail closed on missing/invalid context before SQL and must not include raw tenant values in exception messages/logs.
- Implementation pseudocode:

```java
@FunctionalInterface
public interface EgonColaTenantIdProvider {
    // Return the current execution's trusted, non-null tenant identifier; every signed Long value is valid.
    // Implementations fail closed when no unambiguous context exists and must not cache a cross-request snapshot.
    Long currentTenantId();
}
```

- Verification contribution: custom Provider backoff and every current-tenant assertion compile against a minimal stable port.
- After this file: Context source is abstracted; the default adapter follows.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcTenantIdProvider.java`

- Purpose: Implement the current MDC TenantID source with strict parsing and no range rule.
- Symbols: final/default adapter class, constructor with properties, `currentTenantId()`.
- Repository evidence: SLF4J MDC is already available transitively in Boot applications; user selected MDC now and SecurityContext later.
- Dependencies and consumers: reads File 4 tenant key and implements File 5; registered conditionally in Step 6.
- Why now: Unit tests need the concrete current adapter before bean wiring.
- Contract/signature changes: reads configured key, trims text, parses `Long`; zero/negative/positive succeed; absent/blank and malformed values use distinct stable errors.
- Input/output and state mapping: one method call reads current thread MDC at that instant and returns the parsed value; no ThreadLocal snapshot is added.
- Error and edge behavior: missing/blank -> `TENANT_CONTEXT_MISSING`; overflow/non-numeric -> `TENANT_CONTEXT_MALFORMED` with NumberFormatException cause; message omits raw input.
- Implementation pseudocode:

```java
String value = MDC.get(properties.getTenantId().getMdcKey());
if (value == null || value.isBlank()) throw new IllegalStateException("TENANT_CONTEXT_MISSING");
try { return Long.valueOf(value.trim()); }
catch (NumberFormatException cause) { throw new IllegalStateException("TENANT_CONTEXT_MALFORMED", cause); }
```

- Verification contribution: turns MDC parsing/value-domain/thread-isolation cases GREEN.
- After this file: The default tenant Strategy is complete but not auto-registered.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaUserIdProvider.java`

- Purpose: Isolate the trusted audit user-ID source from its current MDC representation.
- Symbols: public functional interface `EgonColaUserIdProvider`, method `String currentUserId()`.
- Repository evidence: user explicitly removed name fields while retaining create/update user IDs and a future security-context seam.
- Dependencies and consumers: implemented by File 8/test Provider; consumed only by the MetaObjectHandler and future consumer adapters.
- Why now: Handler constructor and exact field authority need a user-ID-only port.
- Contract/signature changes: publishes one nonblank String-returning method and no name/value object.
- Input/output and state mapping: current execution identity maps to its stable ID for audit fill, with no Model/DTO-supplied override.
- Error and edge behavior: missing/blank current identity fails before parameter binding and raw identity is never logged by the Starter.
- Implementation pseudocode:

```java
@FunctionalInterface
public interface EgonColaUserIdProvider {
    // Return the current execution's trusted, nonblank audit identity ID and never a display/user name.
    // Implementations fail before parameter binding when identity context is absent or ambiguous.
    String currentUserId();
}
```

- Verification contribution: compile-time scan proves the public audit context contains ID only.
- After this file: Audit source is abstracted and ready for the default MDC adapter.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcUserIdProvider.java`

- Purpose: Implement current user-ID lookup from the configured MDC key.
- Symbols: default adapter constructor and `currentUserId()`.
- Repository evidence: audit IDs elsewhere are Strings and the accepted design requires MDC now without a user-name companion.
- Dependencies and consumers: reads File 4 audit key, implements File 7, and backs off in Step 6 when a SecurityContext/custom bean exists.
- Why now: Handler unit tests need production-identical ID validation.
- Contract/signature changes: returns trimmed nonblank ID; no numeric parsing, name key, caching, or fallback principal is introduced.
- Input/output and state mapping: each call reads current thread MDC and maps one configured value to create/update audit IDs.
- Error and edge behavior: missing/blank -> `USER_CONTEXT_MISSING`; concurrent MDC values remain thread-isolated; exception/log text contains no ID.
- Implementation pseudocode:

```java
String userId = MDC.get(properties.getAudit().getUserIdMdcKey());
if (userId == null || userId.isBlank()) throw new IllegalStateException("USER_CONTEXT_MISSING");
return userId.trim();
```

- Verification contribution: makes the default user context and no-name assertions GREEN.
- After this file: Both production context adapters are directly testable.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java`

- Purpose: Authoritatively fill all approved common technical fields through the MP lifecycle.
- Symbols: constructor-injected Providers/Clock/properties, final `insertFill`/`updateFill`, protected `afterInsertFill`/`afterUpdateFill` hooks.
- Repository evidence: MP `MybatisParameterHandler` performs ID/fill before setting JDBC parameters; existing reference handler demonstrates the SPI but not the final field contract.
- Dependencies and consumers: receives Files 4-8; registered/validated in Step 6; acts on every EgonModel from AR/Mapper/IService.
- Why now: Context and field metadata are frozen and unit tests define authority before runtime wiring.
- Contract/signature changes: insert overwrites tenantId, createUserId/createTime, updateUserId/updateTime, isDeleted=false; update overwrites tenantId/updateUserId/updateTime only; never fills id/name/business fields.
- Input/output and state mapping: capture one `Instant now`, one tenant ID, and one user ID per operation; strict object fill writes exact values, then one protected extension hook runs for EgonModel and non-EgonModel remains safely untouched except hook contract.
- Error and edge behavior: Provider/Clock failures propagate before JDBC; update keeps create fields/id/isDeleted unchanged; tenant update strategy prevents SET; final roots prevent subclass bypass while hooks may add business technical fields.
- Implementation pseudocode:

```java
public final void insertFill(MetaObject metaObject) {
    if (!(metaObject.getOriginalObject() instanceof EgonModel<?>)) { afterInsertFill(metaObject); return; }
    Instant now = clock.instant(); overwrite(metaObject, "tenantId", tenant.currentTenantId(), "createUserId", user.currentUserId(), "createTime", now, "updateUserId", userId, "updateTime", now, "isDeleted", false);
    afterInsertFill(metaObject);
}
```

- Verification contribution: exact authoritative insert/update fields, final Template Method, fixed time, and hook behavior become GREEN.
- After this file: Step 4 completes context/fill contracts; Spring registration and post-fill validation are still deliberately absent.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonModelTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; all metadata/MDC/value-domain/fixed-Clock/fill/hook tests pass and the public source scan contains no audit-name or alternate tenant field.
- Failure returns to: File 9 for fill order/authority, Files 5-8 for context errors, File 4 for property defaults, or Step 3 File 4 for annotation strategy.
- Completion criteria: exact six insert and three update fields are proven, any non-null Long tenant works, missing tenant/user fail closed, and no bean has yet been implicitly registered.
- Rollback: revert the Step commit; EgonModel remains structurally usable but unfilled, so Steps 5-8 must not remain applied independently.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/model/EgonModelTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestTenantIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestUserIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcTenantIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaUserIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcUserIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java`
- Commit: `feat(common-mybatis-plus): add tenant audit context and fill`

### Step 5 — Freeze and enhance the zero-addition Mapper and all 57 IService methods

- Requirements: `REQ-004`, `REQ-005`, `REQ-006`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-013`, `REQ-014`, `REQ-018`, `REQ-019`, `REQ-027`, `REQ-030`
- Dependencies: `Steps 3-4`
- Baseline state: EgonModel and context/fill contracts compile, but consumers have no unified Mapper/Service API, repository validation facade, or enhanced method bodies.
- Observable outcome: reflection proves a zero-method Mapper and exact 57/57 IService declarations/overrides; focused tests prove context checks, Model business prevalidation, wrapper/page/batch guards, transactional families, and upstream-compatible result shapes.
- End state: Mapper, IService, ServiceImpl, repository validation facade, concrete Mapper/Service fixtures, parity and method-family tests are committed; real SQL interception and Boot wiring intentionally wait for Step 6.
- Test-first gate: Required — Files 1-2 fail on missing types/signatures and then on inherited/default bodies that do not perform the accepted preconditions or method-family semantics.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java`

- Purpose: Freeze exact MP 3.5.16 Mapper/IService public and implementation ABI, including negative API decisions.
- Symbols: `EgonColaIServiceParityTest`, `MethodKey`, upstream visible-method collector, declared-interface/implementation/source assertions.
- Repository evidence: Spec records 4 direct IService plus 53 IRepository methods; local 3.5.16 artifacts are the compile baseline and no existing Common API fills this role.
- Dependencies and consumers: reflects Files 3-6 and scans production source; blocks accidental upgrades and duplicate tenant APIs/Injector classes.
- Why now: Full method inventory and no-extra decisions must be RED before authoring dozens of signatures.
- Contract/signature changes: asserts Mapper declares zero methods, IService declares exactly 57 upstream keys, ServiceImpl declares each exact override, generic bounds use EgonModel/EgonColaMapper, and no forbidden custom names exist.
- Input/output and state mapping: upstream reflection sets are normalized by name/erased parameters/return/generic ownership and compared bidirectionally to declared target sets.
- Error and edge behavior: inherited-but-not-declared methods, bridge/synthetic/static pollution, extra aliases, wrong overload/generic return, 3.5.17 drift, custom Injector/AbstractMethod, or copied reference names fail with a precise diff.
- Implementation pseudocode:

```java
Set<MethodKey> upstream = visiblePublicInstanceMethods(IService.class, IRepository.class);
assertThat(declaredMethodKeys(EgonColaIService.class)).containsExactlyInAnyOrderElementsOf(upstream).hasSize(57);
assertThat(declaredOverrideKeys(EgonColaServiceImpl.class)).containsExactlyInAnyOrderElementsOf(upstream).hasSize(57);
assertThat(EgonColaMapper.class.getDeclaredMethods()).isEmpty(); assertNoRemovedTenantApiOrInjectorInProductionSources();
```

- Verification contribution: owns the exact version/public-surface gate and prevents the deleted 4+3+Injector design from returning.
- After this file: API parity is executable and RED for all missing production types.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java`

- Purpose: Define enhanced semantics by official method family without mocking away the Mapper boundary.
- Symbols: `EgonColaServiceImplTest`, recording Mapper proxy, transaction annotation assertions, read/write/batch/page/chain/metadata test families.
- Repository evidence: MyBatis-Plus ServiceImpl delegates to `BaseMapper` and exposes chain wrappers; repository tests use dynamic proxies when database behavior is reserved for integration.
- Dependencies and consumers: drives Files 5-8 with `TestBusinessModel`, Providers, properties, and `ValidationUtils`; Step 6 supplies real SQL proof.
- Why now: Parity alone could permit 57 trivial `super` calls that ignore the requested enhancements.
- Contract/signature changes: asserts every official method is callable with original return type while method-entry context, arguments, prevalidation, batch/page bounds, wrapper safety, and transactions are applied by family.
- Input/output and state mapping: fixtures/provider/property inputs map to exact BaseMapper calls and official boolean/list/map/obj/page/chain results; no tenant alias call or extra statement is observed.
- Error and edge behavior: null/empty/oversize batches, invalid Model title, missing tenant, context drift, nonpositive/oversize page, empty update/delete wrappers, null IDs/maps/functions, optimistic-lock false, and Mapper exceptions are covered.
- Implementation pseudocode:

```java
TestBusinessService service = serviceWith(recordingMapper, tenantProvider(9L), modelValidation, configuredBounds());
assertThat(service.list()).isSameAs(recordingMapper.listResult()); assertThat(recordingMapper.lastStatement()).isEqualTo("selectList");
assertThatThrownBy(() -> service.saveBatch(invalidModels, 100)).isInstanceOf(ConstraintViolationException.class); assertThat(recordingMapper.jdbcCallCount()).isZero();
assertTransactionalRollbackForException(EgonColaServiceImpl.class, "saveBatch", Collection.class, int.class);
```

- Verification contribution: proves Java-level enhancements for all signature families before SQL plugins are involved.
- After this file: Required behavior is RED on missing API/implementation and later on insufficient family helpers.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java`

- Purpose: Publish the uniform Mapper generic boundary while retaining only official statements.
- Symbols: `EgonColaMapper<T extends EgonModel<T>> extends BaseMapper<T>`.
- Repository evidence: MP default Injector already registers BaseMapper CRUD; final user decision removes every custom tenant statement and Injector.
- Dependencies and consumers: extended by File 7 and consumer Mappers; used by ServiceImpl generic bound and AR mapped-statement lookup.
- Why now: IService implementation and fixtures need the concrete Mapper bound.
- Contract/signature changes: creates an empty public interface with no annotations/default/static/custom methods.
- Input/output and state mapping: official BaseMapper parameters/results pass unchanged into the shared MyBatis interceptor chain added in Step 6.
- Error and edge behavior: arbitrary Mapper methods remain consumer responsibility and must pass the same guard/parser; no built-in cross-tenant or ignored-table escape is added.
- Implementation pseudocode:

```java
public interface EgonColaMapper<T extends EgonModel<T>> extends BaseMapper<T> {
    // intentionally zero declarations: default Injector and official statements are the complete framework surface
}
```

- Verification contribution: parity test proves no duplicate API; H2 later proves official statement registration and scoping.
- After this file: Consumers have the requested Mapper name and model bound without SQL duplication.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java`

- Purpose: Freeze the exact MP 3.5.16 technical Service contract for explicit enhancement.
- Symbols: `EgonColaIService<T extends EgonModel<T>> extends IService<T>` and exactly 57 `@Override` method declarations from `INTERNAL-001` through `INTERNAL-057`.
- Repository evidence: accepted Spec contains each exact signature and local MP 3.5.16 ABI; 3.5.17 removes this contract.
- Dependencies and consumers: implemented by File 6 and extended by consumer technical Service interfaces; parity test is the authoritative inventory gate.
- Why now: Implementation bodies must compile against one visible, reviewable upstream-version surface.
- Contract/signature changes: redeclares all save/remove/update/get/list/page/count/chain/metadata methods with unchanged generic signatures, overloads, default status where required, return types, and checked-exception shape; adds nothing.
- Input/output and state mapping: caller-visible inputs and outputs remain official; enhanced context/validation/guard behavior occurs behind the same call signatures.
- Error and edge behavior: upstream null/empty/result semantics stay documented per method; no `listByCurrentTenantId` or other tenant-named alias is declared.
- Implementation pseudocode:

```java
public interface EgonColaIService<T extends EgonModel<T>> extends IService<T> {
    @Override boolean save(T entity); @Override boolean saveBatch(Collection<T> entityList, int batchSize);
    // explicitly transcribe every remaining MP 3.5.16 IService/IRepository signature INTERNAL-003..057
    @Override Class<T> getEntityClass(); @Override QueryChainWrapper<T> query(); @Override LambdaUpdateChainWrapper<T> lambdaUpdate();
}
```

- Verification contribution: reflection parity reports exact missing/extra keys and version drift.
- After this file: The public interface is complete; all methods still require explicit implementation in File 6.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationUtils.java`

- Purpose: Compose generic Jakarta validation with repository operation groups and current-tenant invariants.
- Symbols: constructor, `validateBusiness(M, Operation)`, `validate(M, Operation)`, group resolver, tenant equality check.
- Repository evidence: common-core facade is generic and cannot depend on MP/tenant context; the accepted design requires one repository-specific manual/global implementation.
- Dependencies and consumers: composes `ValidationUtils` and TenantIdProvider; consumed by ServiceImpl, Step 6 Model plugin, and consumer repositories.
- Why now: Service batch prevalidation and later post-fill/result validation must share group semantics.
- Contract/signature changes: `validateBusiness` applies Default+operation before fill; `validate` applies Default+operation+Persisted, while LOADED applies Default+Persisted and verifies current tenant.
- Input/output and state mapping: non-null Model/operation map to the same Model on success without mutation; violations use stable standard exceptions and context mismatch uses a low-cardinality state error.
- Error and edge behavior: null Model/operation -> IllegalArgumentException; field/subclass invalid -> ConstraintViolationException; null/mismatched persistent tenant -> validation or `TENANT_CONTEXT_MISMATCH`; no complex business I/O occurs.
- Implementation pseudocode:

```java
public <M extends EgonModel<M>> M validateBusiness(M model, Operation operation) {
    validationUtils.validate(requireModel(model), Default.class, operation.group()); return model;
}
public <M extends EgonModel<M>> M validate(M model, Operation operation) { validationUtils.validate(model, persistedGroups(operation)); if (!model.getTenantId().equals(tenant.currentTenantId())) throw mismatch(); return model; }
```

- Verification contribution: focused Service tests prove zero-JDBC business prevalidation; Step 6/7 prove the same facade after fill and load.
- After this file: One manual repository API centralizes validation without coupling EgonModel to Spring.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`

- Purpose: Explicitly enhance all 57 official Service methods while preserving the MP public contract.
- Symbols: generic `EgonColaServiceImpl<M extends EgonColaMapper<T>,T extends EgonModel<T>>`, protected constructor, 57 overrides, private read/write/batch/page/wrapper/chain helpers.
- Repository evidence: MP `ServiceImpl`/`CrudRepository` exposes protected `baseMapper` and official `SqlHelper`/chain constructors; user requires visible reimplementation rather than inherited defaults.
- Dependencies and consumers: extends MP `ServiceImpl`, implements File 4, receives ModelValidationUtils/TenantIdProvider/properties, and is subclassed by File 8/consumers.
- Why now: Interface parity and focused family behavior are fixed.
- Contract/signature changes: every `INTERNAL-001..057` signature appears as an explicit method; no new public method or custom Mapper statement is added; batch methods use `@Transactional(rollbackFor=Exception.class)`.
- Input/output and state mapping: reads require current tenant then use official BaseMapper/chain paths; Model writes prevalidate business fields, wrappers/pages/maps/functions receive exact argument guards, and batches capture one tenant snapshot before any Mapper call.
- Error and edge behavior: missing context, null arguments, unsafe empty update/delete predicates, invalid/oversize batch/page, business violations, fill mismatch/context drift, optimistic-lock false, and Mapper exceptions preserve stated rollback/return rules; chains resolve tenant at terminal Mapper execution.
- Implementation pseudocode:

```java
@Override public List<T> list() { requireCurrentTenant(); return baseMapper.selectList(Wrappers.emptyWrapper()); }
@Override public T getById(Serializable id) { requireCurrentTenant(); return baseMapper.selectById(requireId(id)); }
@Transactional(rollbackFor = Exception.class) @Override public boolean saveBatch(Collection<T> models, int batchSize) { Long snapshot = requireCurrentTenant(); prevalidateAll(models, INSERT, batchSize); return executeOfficialBatch(models, batchSize, snapshot); }
@Override public <E extends IPage<T>> E page(E page, Wrapper<T> wrapper) { requireCurrentTenant(); requirePageBounds(page); return baseMapper.selectPage(page, wrapper); }
```

- Verification contribution: Java-level family tests and reflection prove all official methods are enhanced explicitly; H2/transaction tests prove runtime consequences later.
- After this file: The full technical Service suite compiles with no hidden tenant aliases or Injector dependency.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java`

- Purpose: Provide one concrete EgonColaMapper for Service, default-statement, and adversarial SQL integration tests.
- Symbols: `TestBusinessMapper extends EgonColaMapper<TestBusinessModel>` and narrowly named test-only annotated statements for explicit tenant predicates/protected-column/unsupported-SQL cases.
- Repository evidence: MyBatis test Mappers in Spring contexts use interface annotations for focused custom statements; production EgonColaMapper must remain empty.
- Dependencies and consumers: injected into File 8 and all H2 tests; official CRUD statements come from default Injector.
- Why now: Concrete Service generic resolution and later SQL guard tests require one Mapper registration target.
- Contract/signature changes: test-only methods are not production API and exist only where official methods cannot create an adversarial SQL shape; all normal CRUD calls use inherited BaseMapper methods.
- Input/output and state mapping: inherited calls map TestBusinessModel to `test_business_record`; custom test parameters expose equal/mismatched/null tenant predicates and forbidden SET clauses to the guard.
- Error and edge behavior: no consumer-friendly cross-tenant method is introduced; unsupported SQL is expected to fail closed in Step 6.
- Implementation pseudocode:

```java
@Mapper interface TestBusinessMapper extends EgonColaMapper<TestBusinessModel> {
    @Select("select * from test_business_record where tenant_id = #{tenantId}") List<TestBusinessModel> explicitTenant(Long tenantId);
    @Update("update test_business_record set tenant_id = #{tenantId} where id = #{id}") int forbiddenTenantMutation(Long id, Long tenantId);
}
```

- Verification contribution: parity still sees zero production Mapper additions while H2 can exercise explicit/unsupported guard paths.
- After this file: MyBatis can register a real concrete Mapper in Step 6.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java`

- Purpose: Provide a concrete technical Service subclass with constructor-forwarded framework dependencies.
- Symbols: `TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper,TestBusinessModel>` and explicit constructor.
- Repository evidence: accepted design forbids static locators and distinguishes this repository technical Service from `biz.service` business logic.
- Dependencies and consumers: receives concrete Mapper through inherited MP injection plus ModelValidationUtils, TenantIdProvider, and properties; used by unit/H2/transaction tests.
- Why now: Generic Service bodies and Spring proxy transactions need a concrete bean/test instance.
- Contract/signature changes: test-only constructor forwards dependencies exactly; adds no CRUD override or business rule.
- Input/output and state mapping: injected collaborators become protected/base state in the technical Service; every call follows File 6's official method path.
- Error and edge behavior: missing constructor dependency fails test-context creation; no field injection, default constructor, Model conversion, or business validation is hidden here.
- Implementation pseudocode:

```java
final class TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper, TestBusinessModel> {
    TestBusinessService(EgonColaModelValidationUtils validation, EgonColaTenantIdProvider tenant, EgonColaMybatisPlusProperties properties) {
        super(validation, tenant, properties);
    }
}
```

- Verification contribution: proves the intended consumer extension shape and allows Spring transaction proxy testing.
- After this file: Step 5 has a compilable concrete Mapper/Service path ready for real MyBatis wiring.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaIServiceParityTest,EgonColaServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; Mapper has zero declared methods; interface/implementation sets are exactly 57; method-family guards/returns/transactions pass; deleted API/Injector source names are absent.
- Failure returns to: File 6 for behavior/override families, File 4 for interface signatures, File 5 for validation groups, Files 7-8 for fixture-only compilation, or the Spec if the resolved upstream ABI changed.
- Completion criteria: exact ABI parity and focused behavior are GREEN, no custom SQL Injector exists, and every batch/page/wrapper failure occurs before the recording Mapper is invoked.
- Rollback: revert the Step commit; Model/context Steps remain, but no unified Mapper/Service API remains for consumers.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/model/EgonColaModelValidationUtils.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java`
- Commit: `feat(common-mybatis-plus): enhance official mapper and service contracts`

### Step 6 — Wire and prove the shared Boot/MyBatis isolation, fill, and validation chain

- Requirements: `REQ-003`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-017`, `REQ-018`, `REQ-019`, `REQ-021`, `REQ-022`, `REQ-025`, `REQ-027`, `REQ-030`
- Dependencies: `Steps 1-5`
- Baseline state: all Java contracts are directly constructible, but Boot cannot discover them and real MyBatis SQL has no ordered tenant guard/TenantLine/model-validation chain.
- Observable outcome: ContextRunner proves conditional beans and startup validation; embedded H2 proves official default Mapper statements, tenant scoping, logic delete, wide-write protection, explicit-predicate equality, optimistic locking, pagination, authoritative fill, and result validation.
- End state: full auto-configuration, runtime handlers/interceptors, exception model, imports resource, H2 schema, and focused context/tenant-SQL tests are committed; higher AR/batch/layered matrices remain for Step 7.
- Test-first gate: Required — Files 1-2 initially fail because Boot imports, beans/order/fail-fast checks, guard/TenantLine, validation plugin, default statement runtime, and schema do not exist.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`

- Purpose: Freeze auto-configuration discovery, conditions, defaults, ordering, replacement, and fail-fast contracts.
- Symbols: `EgonColaMybatisPlusAutoConfigurationTest`, ApplicationContextRunner fixtures, ImportCandidates assertion, ordered-chain helper, custom safe/broken bean configurations.
- Repository evidence: Common ID Starter uses `ApplicationContextRunner` and `AutoConfiguration.imports`; MP official auto-config assembles ordered `InnerInterceptor` beans when no outer bean exists.
- Dependencies and consumers: drives Files 3-9 and uses existing properties/Providers/handler/validation facade; no web server or external DataSource is started.
- Why now: Runtime bean composition is a public adoption contract and must be RED before wiring.
- Contract/signature changes: asserts enabled/disabled behavior, defaults, custom Provider/Clock/handler/outer interceptor backoff, exact mandatory/optional order, `ValidationUtils`, plugin, no `ISqlInjector` bean, and stable startup errors.
- Input/output and state mapping: property maps plus user configurations produce an ApplicationContext bean graph; the final outer interceptor list and handler/plugin types map to accepted ordered capabilities.
- Error and edge behavior: missing Validator, invalid properties, missing guard/TenantLine, swapped order, enabled optional plugin absent, non-EgonCola handler, missing model plugin, and safe disabled options are each distinguished.
- Implementation pseudocode:

```java
runner.withConfiguration(AutoConfigurations.of(EgonColaMybatisPlusAutoConfiguration.class, officialMpConfigurations()))
    .run(context -> assertThat(context).hasSingleBean(EgonColaTenantIdProvider.class).hasSingleBean(EgonColaModelValidationInterceptor.class));
assertThat(innerTypes(context)).containsExactly(Guard.class, BlockAttackInnerInterceptor.class, TenantLineInnerInterceptor.class, OptimisticLockerInnerInterceptor.class, PaginationInnerInterceptor.class);
assertBrokenReplacementFailsWith(contextRunnerFor(swappedOuterChain), "MYBATIS_PLUS_INTERCEPTOR_ORDER_INVALID");
```

- Verification contribution: covers `TEST-001`-`008`,`TEST-042`-`047`-class startup/default-Injector contracts.
- After this file: Complete wiring expectations are RED on missing runtime types/resource.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaTenantIdSqlIntegrationTest.java`

- Purpose: Prove tenant and safety behavior against real MP mapped statements and JDBC rows.
- Symbols: `EgonColaTenantIdSqlIntegrationTest`, Spring MyBatis/H2 configuration, SQL-capture assertions, current-tenant row fixtures, official method matrix.
- Repository evidence: Spec mandates embedded H2 as module proof; production has no concrete business table and default Injector behavior must be tested rather than replaced.
- Dependencies and consumers: uses TestBusinessModel/Mapper/Service, Providers, Files 4-10, and `schema.sql`.
- Why now: AST and plugin order cannot be accepted from unit mocks alone.
- Contract/signature changes: asserts official `insert/selectById/selectList/selectCount/updateById/deleteById/selectPage` registration and scoped behavior through Mapper and IService without any custom tenant statement.
- Input/output and state mapping: two tenants' rows plus current Provider map to isolated list/count/id/optional results, authoritative stored audit values, logic-delete visibility, optimistic version transitions, and scoped pages.
- Error and edge behavior: missing/mismatched/null explicit tenant predicate, forbidden tenant/is_deleted SET, empty wrapper writes, unsupported custom SQL, ignored global table, malformed context, and other-tenant ID all fail/return exactly as specified.
- Implementation pseudocode:

```java
tenant.set(11L); service.save(model("a")); tenant.set(22L); service.save(model("b"));
tenant.set(11L); assertThat(service.list()).extracting(TestBusinessModel::getTitle).containsExactly("a"); assertThat(service.count()).isEqualTo(1L);
assertThat(service.getById(otherTenantId)).isNull(); assertThat(service.getOptById(otherTenantId)).isEmpty();
assertThatThrownBy(() -> mapper.forbiddenTenantMutation(id, 22L)).hasMessageContaining("TENANT_COLUMN_MUTATION_FORBIDDEN");
```

- Verification contribution: owns official SQL scoping/default Injector/logic-delete/page/guard evidence for `REQ-004`,`REQ-009`,`REQ-010`,`REQ-012`-`REQ-018`.
- After this file: The real runtime contract is RED until the chain and schema are present.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java`

- Purpose: Represent deterministic startup contract failures without leaking tenant/user/config values.
- Symbols: runtime exception class, stable code constructor/accessor, optional cause.
- Repository evidence: Common exceptions use stable codes and preserve causes; configuration failures must be distinguishable from persistence/validation errors.
- Dependencies and consumers: thrown by auto-config contract validator and invalid custom runtime-chain checks; asserted by ContextRunner.
- Why now: Fail-fast wiring needs a named low-cardinality failure type before validators are implemented.
- Contract/signature changes: adds one public EgonCola-prefixed runtime exception carrying a fixed code and safe message; no HTTP mapping is defined.
- Input/output and state mapping: known contract violation code/cause map to startup failure analysis while raw TenantID, user ID, SQL parameters, and credentials are excluded.
- Error and edge behavior: null/blank code is rejected; cause is retained; message remains deterministic for test/operations triage.
- Implementation pseudocode:

```java
public final class EgonColaMybatisPlusConfigurationException extends IllegalStateException {
    private final String code;
    public EgonColaMybatisPlusConfigurationException(String code, Throwable cause) { super(requireSafeCode(code), cause); this.code = code; }
    public String getCode() { return code; }
}
```

- Verification contribution: ContextRunner can assert exact startup categories rather than brittle framework wrapper text.
- After this file: Runtime validators have a stable error model.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdTenantLineHandler.java`

- Purpose: Adapt the trusted Provider and ignored-table policy to MP TenantLine SQL expressions.
- Symbols: `EgonColaTenantIdTenantLineHandler implements TenantLineHandler`, `getTenantId`, `getTenantIdColumn`, `ignoreTable`.
- Repository evidence: MP 3.5.16 TenantLine requires a JSqlParser Expression and supports per-table ignore checks; the selected tenant column is fixed.
- Dependencies and consumers: receives TenantIdProvider/properties; wrapped by `TenantLineInnerInterceptor` in File 7.
- Why now: Real SQL scoping needs one shared adapter rather than Service-invented predicates.
- Contract/signature changes: returns `LongValue(currentTenantId)` and column `tenant_id`; ignored tables are exact case-insensitive configured names.
- Input/output and state mapping: each SQL rewrite obtains current context at execution time, creates a numeric AST literal, and decides table inclusion from immutable normalized configuration.
- Error and edge behavior: Provider missing/malformed errors propagate before JDBC; null is impossible by contract; schema-qualified/aliased names are normalized conservatively; no wildcard ignore exists.
- Implementation pseudocode:

```java
public Expression getTenantId() { return new LongValue(tenantIdProvider.currentTenantId()); }
public String getTenantIdColumn() { return "tenant_id"; }
public boolean ignoreTable(String tableName) { return ignoredTables.contains(normalizeExactTableName(tableName)); }
```

- Verification contribution: H2/SQL capture proves the same current Long enters official SELECT/INSERT/UPDATE/DELETE statements.
- After this file: TenantLine can scope SQL once auto-config creates its ordered interceptor.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaTenantIdGuardInnerInterceptor.java`

- Purpose: Fail closed on unsupported SQL, context absence, explicit tenant spoofing, and protected-column mutation before TenantLine rewrite.
- Symbols: `EgonColaTenantIdGuardInnerInterceptor implements InnerInterceptor`, query/update hooks, AST/BoundSql/parameter traversal helpers.
- Repository evidence: TenantLine adds predicates but does not verify caller-provided tenant conditions or reject ordinary SET of `tenant_id/is_deleted`; JSqlParser is an approved direct dependency.
- Dependencies and consumers: receives Provider/properties, parses BoundSql before BlockAttack/TenantLine, and is ordered `100` by auto-config.
- Why now: Shared SQL protection must cover direct Mapper and AR paths that Service argument checks cannot see.
- Contract/signature changes: enforces current context for nonignored tables, explicit tenant predicate non-null/equality, no protected SET, and parser-supported statement shapes; adds no bypass API.
- Input/output and state mapping: BoundSql SQL plus ParameterMapping order/object recursively resolve JDBC placeholders, compare tenant literals/values to one current ID, and either pass unchanged or throw before execution.
- Error and edge behavior: null/mismatch, alias/case variants, maps/beans, multiple placeholders, UPDATE SET protected columns, unsupported AST/dialect, parse failure, and ignored tables use deterministic fail-closed outcomes without raw-value logging.
- Implementation pseudocode:

```java
Statement ast = CCJSqlParserUtil.parse(boundSql.getSql()); Long current = tenant.currentTenantId();
if (isIgnoredOnly(ast, properties)) return; rejectUnsupportedStatementShape(ast);
for (TenantPredicate predicate : findTenantPredicates(ast, "tenant_id")) requireNonNullAndEqual(resolveValue(predicate, boundSql), current);
if (isUpdate(ast) && assignedColumns(ast).stream().anyMatch(PROTECTED_COLUMNS::contains)) throw guardError("TENANT_COLUMN_MUTATION_FORBIDDEN");
```

- Verification contribution: direct custom Mapper tests prove equality/protected-column/unsupported SQL failures before row changes.
- After this file: Caller SQL cannot weaken tenant authority, but TenantLine still needs registration.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaModelValidationInterceptor.java`

- Purpose: Apply one repository validation implementation after MP fill and after result mapping for every persistence entry.
- Symbols: MyBatis `@Intercepts` for `ParameterHandler.setParameters(PreparedStatement)` and `ResultSetHandler.handleResultSets(Statement)`, recursive Model extractor, command-to-operation mapper.
- Repository evidence: MP `MybatisParameterHandler` performs ID and MetaObject fill in its constructor before `setParameters`; MyBatis ResultSetHandler is common to AR/Mapper/IService reads.
- Dependencies and consumers: receives ModelValidationUtils; auto-config registers it as a standard MyBatis `Interceptor`; processes TestBusinessModel and all consumer EgonModel subclasses.
- Why now: Unit prevalidation does not prove filled persistent fields or dirty database results.
- Contract/signature changes: validates parameters with operation/Persisted groups before binding and validates all returned Models as LOADED; no public annotation/bypass is added.
- Input/output and state mapping: unwrap MappedStatement/parameter/result via MyBatis MetaObject, derive operation from `SqlCommandType`, recursively visit EgonModel/Iterable/array/Map/IPage/wrapper entity once by identity, and return original plugin result unchanged.
- Error and edge behavior: cycles, duplicate references, scalar/map metadata, nulls, batch collections, custom wrappers, result pages, constraint violations, tenant mismatch, and reflection access failure are bounded/fail-closed for EgonModel while unrelated objects pass.
- Implementation pseudocode:

```java
if (isParameterBinding(invocation)) { Operation op = operation(mappedStatement(invocation).getSqlCommandType()); visitModels(parameterObject(invocation), identitySet(), model -> validation.validate(model, op)); }
Object result = invocation.proceed();
if (isResultHandling(invocation)) visitModels(result, identitySet(), model -> validation.validate(model, Operation.LOADED));
return result;
```

- Verification contribution: H2 fill-before-bind and dirty-result tests prove AR/Mapper/IService share one Model validation chain.
- After this file: The global validation plugin is complete and awaits Boot registration/contract checking.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`

- Purpose: Register the complete conditional bean graph early enough for official MP auto-configuration to assemble it.
- Symbols: `@AutoConfiguration(before={MybatisPlusInnerInterceptorAutoConfiguration.class,MybatisPlusAutoConfiguration.class})`, properties enablement, Beans for Validator facade, Clock, Providers, handler, model facade/plugin, five ordered InnerInterceptors, contract validator.
- Repository evidence: Common Starters use Boot imports/conditional defaults; MP 3.5.16 collects ordered InnerInterceptor beans into one outer interceptor.
- Dependencies and consumers: wires all production classes from Steps 1, 4-6; runs in adopting MyBatis applications and tests.
- Why now: Every dependent type is stable and RED context/SQL tests define the final graph.
- Contract/signature changes: enabled-by-default prefix creates mandatory guard `100` and TenantLine `300`, optional BlockAttack `200`/optimistic `400`/pagination `500`, one model plugin, one EgonCola handler, and conditional MDC Providers/Clock/ValidationUtils.
- Input/output and state mapping: Boot properties and existing custom beans map to defaults/backoff; official MP auto-config receives ordered InnerInterceptor list and MyBatis receives the validation Interceptor/MetaObjectHandler.
- Error and edge behavior: `enabled=false` creates none; absent Jakarta Validator fails clearly; optional disabled plugins are omitted but Service guards stay; custom safe beans are preserved; no `ISqlInjector` Bean is created or inspected.
- Implementation pseudocode:

```java
@Bean @Order(100) InnerInterceptor tenantGuard(...) { return new EgonColaTenantIdGuardInnerInterceptor(...); }
@Bean @Order(200) @ConditionalOnProperty(name="block-attack.enabled", matchIfMissing=true) InnerInterceptor blockAttack() { return new BlockAttackInnerInterceptor(); }
@Bean @Order(300) InnerInterceptor tenantLine(...) { return new TenantLineInnerInterceptor(new EgonColaTenantIdTenantLineHandler(...)); }
@Bean @Order(500) @ConditionalOnProperty(name="pagination.enabled", matchIfMissing=true) InnerInterceptor pagination(...) { return configuredPagination(properties); }
```

- Verification contribution: turns bean discovery/default/backoff and complete H2 runtime paths GREEN.
- After this file: Full bean definitions exist but still require startup contract validation and imports metadata.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`

- Purpose: Reject custom runtime replacements that silently omit or reorder mandatory isolation/fill/validation capabilities.
- Symbols: `SmartInitializingSingleton`, constructor with bean providers/properties, `afterSingletonsInstantiated`, ordered-chain/handler/plugin assertions.
- Repository evidence: conditional backoff can replace MP outer interceptor or MetaObjectHandler; annotations alone cannot prove the final chain.
- Dependencies and consumers: inspects final `MybatisPlusInterceptor`, MyBatis Interceptor list, MetaObjectHandler, and configured optional flags; throws File 3 exception.
- Why now: Customization safety is part of the auto-config RED contract.
- Contract/signature changes: verifies one outer list with mandatory guard before TenantLine and configured optional relative order, model validation plugin presence, and handler subtype; deliberately ignores default Injector because no custom seam is owned.
- Input/output and state mapping: finalized beans map to a normalized low-cardinality class/order list for validation/logging; valid graphs complete startup and invalid graphs produce one stable code.
- Error and edge behavior: missing/duplicate/reordered classes, disabled optional plugins, subclassed safe Handler, proxied standard Interceptor, absent outer chain, and sensitive-value logging are handled explicitly.
- Implementation pseudocode:

```java
List<Class<?>> chain = normalizeInnerTypes(requiredOuterInterceptor().getInterceptors());
requireOrdered(chain, EgonColaTenantIdGuardInnerInterceptor.class, TenantLineInnerInterceptor.class);
requireOptionalOrder(chain, properties, BlockAttackInnerInterceptor.class, OptimisticLockerInnerInterceptor.class, PaginationInnerInterceptor.class);
requirePlugin(EgonColaModelValidationInterceptor.class); requireHandlerAssignableTo(EgonColaMetaObjectHandler.class);
```

- Verification contribution: broken custom replacements fail ContextRunner before any Mapper call; safe overrides start.
- After this file: Runtime protection remains enforceable even when official conditional beans back off.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: Publish the Boot 3 discovery entry for the Starter.
- Symbols: one line naming `top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration`.
- Repository evidence: existing Common Starters use this resource rather than `spring.factories` or component scanning.
- Dependencies and consumers: read by Boot ImportCandidates whenever the Jar is on a consumer classpath; points only to File 7.
- Why now: The class and complete bean graph exist and can safely be discovered.
- Contract/signature changes: adds automatic registration without application annotations; global enabled property remains the opt-out.
- Input/output and state mapping: classpath presence maps to one candidate class, which then applies Boot conditions and ordered beans.
- Error and edge behavior: duplicate/blank/old factory entries are disallowed; package/Jar tests assert exact spelling and a single line.
- Implementation pseudocode:

```text
top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration
# no spring.factories duplicate and no additional configuration candidate
# disabled behavior is controlled by the configuration class condition
```

- Verification contribution: ImportCandidates and packaged-Jar gates prove production discovery.
- After this file: An adopting Boot application can load the Starter without component scan.

#### File 10 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql`

- Purpose: Provide deterministic embedded tables for real SQL, logic delete, optimistic lock, and ignored-table tests only.
- Symbols: `test_business_record` and `test_global_record` DDL with explicit common/business columns and indexes as needed for tests.
- Repository evidence: the Starter owns no production schema; H2 test resources are the accepted isolated verification boundary.
- Dependencies and consumers: initialized by Files 2 and Step 7 tests; maps TestBusinessModel and a test-only ignored global statement.
- Why now: Real mapped statements need physical columns matching the accepted Model contract.
- Contract/signature changes: test business table has BIGINT id/tenant_id, VARCHAR user IDs/title/payload, timestamp audit columns, boolean `is_deleted`, optional version, and all seven common fields NOT NULL.
- Input/output and state mapping: MP-generated SQL maps exact snake_case columns; test setup truncates/inserts deterministic multi-tenant rows between cases.
- Error and edge behavior: no production Flyway file is created; incompatible null/common column writes fail H2; test global table is explicitly ignored only by configured name.
- Implementation pseudocode:

```sql
CREATE TABLE test_business_record (id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, create_user_id VARCHAR(128) NOT NULL, create_time TIMESTAMP NOT NULL,
 update_user_id VARCHAR(128) NOT NULL, update_time TIMESTAMP NOT NULL, is_deleted BOOLEAN NOT NULL, title VARCHAR(255) NOT NULL, payload VARCHAR(1024), version BIGINT);
CREATE INDEX idx_test_business_tenant_deleted ON test_business_record(tenant_id, is_deleted);
CREATE TABLE test_global_record (id BIGINT PRIMARY KEY, payload VARCHAR(255));
```

- Verification contribution: enables module-local SQL evidence and is later checked absent from the production Jar.
- After this file: Step 6 has a complete embedded runtime without any consumer/production migration.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaMybatisPlusAutoConfigurationTest,EgonColaTenantIdSqlIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test`
- Expected result: exit 0; ContextRunner matrix passes; H2 official statements are registered by the default Injector and enforce tenant/filter/fill/validation/order/logic-delete/locking/pagination contracts.
- Failure returns to: File 7/8/9 for bean discovery/order, File 5 for SQL guard, File 4 for TenantLine, File 6 for Model traversal/timing, File 10 for test DDL, or Step 5 owner for Service behavior.
- Completion criteria: all scoped official CRUD paths pass without a custom Injector/method, broken replacements fail at startup, and no HTTP service/external database is started.
- Rollback: revert the Step commit; auto-discovery and runtime interception disappear together while prior direct Java APIs remain unadvertised and incomplete for adoption.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaTenantIdSqlIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaTenantIdTenantLineHandler.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaTenantIdGuardInnerInterceptor.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaModelValidationInterceptor.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql`
- Commit: `feat(common-mybatis-plus): wire tenant and model protection chain`

### Step 7 — Prove ActiveRecord, layered validation/conversion, and atomic batches end to end

- Requirements: `REQ-010`, `REQ-011`, `REQ-018`, `REQ-021`, `REQ-022`, `REQ-024`, `REQ-025`, `REQ-026`, `REQ-027`, `REQ-028`, `REQ-029`, `REQ-030`
- Dependencies: `Step 6`
- Baseline state: focused context and official tenant SQL tests pass, but complete AR dispatch, parameter/result validation, Spring MVC/Method Validation object flow, batch rollback/context drift, and concurrency are not yet proven together.
- Observable outcome: embedded integration tests exercise all 14 AR abilities, invalid/stale Models, DTO/PO/Model ownership, two BaseConverter boundaries, atomic batch families, tenant drift, and thread isolation.
- End state: test-only integration and support files are committed; any production defect found is fixed in its earlier owning file before this Step commit and reruns that earlier Step gate.
- Test-first gate: Not applicable — this Step introduces no new production contract; it is an integration-proof Step over Steps 1-6. New tests are written first, and any failure must return to the named production owner rather than being masked by test fixtures.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaActiveRecordIntegrationTest.java`

- Purpose: Prove every inherited/enhanced AR entry uses the same mapped statements, tenant scope, fill, logic delete, validation, and hooks.
- Symbols: `EgonColaActiveRecordIntegrationTest`, AR method matrix, hook-spy Model subclass/configuration, multi-tenant fixtures.
- Repository evidence: MP ActiveRecord opens SqlSessions by Model class and requires a registered BaseMapper; TestBusinessMapper and H2 context exist after Step 6.
- Dependencies and consumers: uses full auto-config, TestBusinessModel/Mapper, Providers, and schema; validates `INTERNAL-059..064` plus eight inherited AR methods.
- Why now: Service/Mapper SQL success does not prove AbstractModel dynamic dispatch or result validation.
- Contract/signature changes: no production API change; asserts insert/updateById/deleteById/delete(wrapper)/selective update/delete final roots and inherited insertOrUpdate/selectAll/selectById/selectList/selectOne/selectPage/selectCount.
- Input/output and state mapping: AR Model business fields plus current context map to filled scoped rows; query methods return only current-tenant nondeleted validated Models; hooks record one normal completion each.
- Error and edge behavior: missing tenant/user, blank title, other-tenant ID, empty write wrapper, optimistic stale version, logic-deleted row, hook exception, and `insertOrUpdate` double-hook risk are covered.
- Implementation pseudocode:

```java
tenant.set(31L); user.set("ar-user"); HookedBusinessModel model = new HookedBusinessModel("valid");
assertThat(model.insert()).isTrue(); assertThat(model.events()).containsExactly("beforeInsert", "afterInsert:true");
tenant.set(32L); assertThat(model.selectById(model.getId())).isNull(); tenant.set(31L); assertThat(model.selectAll()).hasSize(1);
assertInsertOrUpdateDispatchesToExactlyOneFinalRootAndOneHookPair(model);
```

- Verification contribution: proves actual AR parity behavior rather than reflection alone.
- After this file: all AR entry points have real SQL and lifecycle evidence.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaModelValidationIntegrationTest.java`

- Purpose: Prove manual/global repository validation and Controller DTO/business Service PO boundaries in one object flow.
- Symbols: `EgonColaModelValidationIntegrationTest`, nested test Controller/business Service, MethodValidation proxy/config, dirty-row loader, DTO/PO/Model assertions.
- Repository evidence: repository uses Spring MVC `@Valid`, `@Validated` Service patterns, existing `BaseConverter`, and test-only Boot web scope selected in the Spec.
- Dependencies and consumers: uses Files 4-6, `ValidationUtils`, ModelValidationUtils/plugin, TestBusinessModel/Mapper/Service, MockMvc or equivalent MVC slice.
- Why now: Validation ownership must be observable across distinct objects, not inferred from annotations.
- Contract/signature changes: no production endpoint is added; test-only controller accepts DTO, converter maps PO/Model, business Service enforces PO/complex rule, repository plugin enforces Model/write/load rules.
- Input/output and state mapping: valid JSON/DTO -> validated DTO -> PO -> validated business rule -> Model business fields -> authoritative fill/persistence -> validated Model -> reverse PO/DTO; technical fields never originate from DTO/PO.
- Error and edge behavior: invalid DTO issues zero Service/SQL calls; invalid PO issues zero repository calls; invalid Model fails before bind; corrupt loaded row fails before return; complex duplicate/title-state rule appears only in the nested business Service.
- Implementation pseudocode:

```java
mockMvc.perform(post("/test-business").content(invalidDtoJson)).andExpect(status().isBadRequest()); assertThat(repository.calls()).isZero();
TestBusinessPO po = dtoToPo.toTarget(validDto); validatedBusinessService.create(po);
TestBusinessModel model = poToModel.toTarget(po); assertTechnicalFieldsNull(model); technicalService.save(model);
assertThatThrownBy(() -> mapper.selectById(corruptRowId)).isInstanceOf(ConstraintViolationException.class);
```

- Verification contribution: owns `REQ-026`-`REQ-030` cross-layer acceptance without shipping business Controller/Service types.
- After this file: automatic/manual validation ownership and no-technical-field conversion are proven end to end.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java`

- Purpose: Prove bounded batch families are atomic, tenant-stable, prevalidated, and thread-isolated under a real transaction manager.
- Symbols: `EgonColaBatchTransactionIntegrationTest`, parameterized save/update/remove batches, provider-drift trigger, concurrent executor test, row-count assertions.
- Repository evidence: MP batch methods rely on Spring transactions/SqlSession batching; unit proxy tests cannot prove rollback or thread context isolation.
- Dependencies and consumers: uses proxied TestBusinessService, test Providers, H2 schema, and Step 6 runtime chain.
- Why now: This is the final high-risk persistence proof before distribution.
- Contract/signature changes: no production method is added; tests all official batch overloads and configured bounds through their existing signatures.
- Input/output and state mapping: a collection, chunk size, method-entry tenant snapshot, and current transaction map to all rows committed or zero rows; statement fills must equal snapshot and end-context must still match.
- Error and edge behavior: null/empty/oversize collection, invalid element, invalid chunk, middle constraint/SQL failure, context change after first statement, rollback-only state, and two concurrent tenants are asserted with cleanup.
- Implementation pseudocode:

```java
tenant.set(41L); assertThat(service.saveBatch(validModels(5), 2)).isTrue(); assertThat(rowsFor(41L)).hasSize(5);
assertThatThrownBy(() -> service.saveBatch(modelsWithMiddleFailure(), 2)).isInstanceOf(RuntimeException.class); assertThat(rowsFor(41L)).isEmpty();
assertThatThrownBy(() -> saveWhileProviderChangesAfterFirstStatement(41L, 42L)).hasMessageContaining("TENANT_CONTEXT_MISMATCH"); assertNoRowsForEitherTenant();
assertConcurrentTenantExecutionsRemainDisjoint(51L, 52L);
```

- Verification contribution: proves transaction annotations and snapshot checks produce actual atomic database state.
- After this file: batch and concurrency risks have embedded runtime evidence.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessDTO.java`

- Purpose: Represent the consumer Controller transport object without persistence fields.
- Symbols: `TestBusinessDTO`, `title`, `payload`, Jakarta transport constraints/accessors.
- Repository evidence: accepted architecture uses `@Valid` DTO at Controller and existing business modules keep API objects separate from persistence entities.
- Dependencies and consumers: consumed only by TestBusinessConverters and the nested MVC test controller.
- Why now: Layered validation proof requires a distinct transport object rather than reusing EgonModel.
- Contract/signature changes: test-only DTO exposes business fields and `@NotBlank title`; it has no id, tenant, audit, delete, MP, or table annotation.
- Input/output and state mapping: request JSON maps to transport fields; validation runs before DTO->PO conversion; reverse converter creates response DTO from PO.
- Error and edge behavior: blank/null title fails Controller validation, nullable payload passes, and unknown technical input cannot bind to absent properties.
- Implementation pseudocode:

```java
final class TestBusinessDTO {
    @NotBlank private String title;
    private String payload;
    // transport accessors only; no id, tenantId, audit timestamps, user IDs, or isDeleted
}
```

- Verification contribution: makes DTO ownership and technical-field exclusion reflectable/assertable.
- After this file: MVC tests can distinguish transport failures from PO/Model failures.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessPO.java`

- Purpose: Represent the consumer business Service process object and method-validation input.
- Symbols: `TestBusinessPO`, business fields/constraints and optional state used by the complex test rule.
- Repository evidence: the accepted naming explicitly reserves PO for Service-layer business/process input, not ORM persistence.
- Dependencies and consumers: target of DTO converter, source of Model converter, and argument to the nested `@Validated` business Service.
- Why now: DTO and Model cannot prove the middle validation/complex-rule ownership boundary.
- Contract/signature changes: test-only PO includes business fields and simple Jakarta constraints but no MP/table/technical common fields.
- Input/output and state mapping: Controller converter maps DTO fields to PO; Method Validation checks simple constraints; business Service checks the stateful rule before repository conversion.
- Error and edge behavior: simple invalid fields throw ConstraintViolationException from the Service proxy; duplicate/forbidden state rule throws the test business exception; neither reaches Mapper.
- Implementation pseudocode:

```java
final class TestBusinessPO {
    @NotBlank private String title; private String payload; private String requestedState;
    // business Service may query a stub repository for duplicate title; Jakarta constraints remain field-local
}
```

- Verification contribution: demonstrates PO validation is neither Controller DTO validation nor repository Model validation.
- After this file: Full three-object flow can compile.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessConverters.java`

- Purpose: Prove two explicit `BaseConverter` boundaries and the exclusion of Model technical fields.
- Symbols: DTO/PO and PO/Model converter implementations plus field/list/reverse mapping helpers/assertions.
- Repository evidence: common-core `BaseConverter<S,T>` already defines single and list forward/reverse contracts and is the mandated reuse point.
- Dependencies and consumers: maps Files 4-5 and TestBusinessModel; consumed by File 2 only and packaged nowhere.
- Why now: The accepted design requires actual converter behavior evidence without introducing a production universal converter.
- Contract/signature changes: test-only implementations map title/payload explicitly; PO->Model creates new Model with business fields only, and Model->PO ignores all seven technical fields.
- Input/output and state mapping: DTO<->PO and PO<->Model preserve declared business values/nulls/order for single/list conversion while tenant/audit/id/delete remain handler/repository-owned.
- Error and edge behavior: null single/list behavior follows existing BaseConverter contract; empty lists stay empty; forged technical DTO keys cannot enter Model; reverse conversion does not expose technical values.
- Implementation pseudocode:

```java
BaseConverter<TestBusinessDTO, TestBusinessPO> dtoPo = explicit(title, payload);
BaseConverter<TestBusinessPO, TestBusinessModel> poModel = new BaseConverter<>() {
    public TestBusinessModel toTarget(TestBusinessPO po) { return new TestBusinessModel().businessValues(po.getTitle(), po.getPayload()); }
    public TestBusinessPO toSource(TestBusinessModel model) { return new TestBusinessPO(model.getTitle(), model.getPayload()); }
};
```

- Verification contribution: proves reuse, direction ownership, list behavior, and exact technical-field exclusion.
- After this file: Step 7's layered object-flow tests are fully supported without production fixture leakage.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaActiveRecordIntegrationTest,EgonColaModelValidationIntegrationTest,EgonColaBatchTransactionIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test && ./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am test`
- Expected result: exit 0; all three named integration classes and the full Starter suite pass with H2 only; transaction/concurrency rows are disjoint/atomic and validation/conversion failures stop at the owning boundary.
- Failure returns to: Step 3 File 4 for AR lifecycle, Step 4 File 9 for fill, Step 5 Files 5-6 for prevalidation/batch semantics, Step 6 Files 5-8 for runtime chain, or current fixture file for test-only mapping errors.
- Completion criteria: every `REQ-024`-`REQ-030` flow has executable evidence, earlier focused gates remain GREEN after any correction, and the production Jar still contains no test fixture/schema.
- Rollback: revert this test-only commit; production code remains unchanged, but the Plan cannot proceed to distribution because required integration evidence is lost.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaActiveRecordIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaModelValidationIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessDTO.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessPO.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessConverters.java`
- Commit: `test(common-mybatis-plus): prove active record and layered persistence flows`

### Step 8 — Export, document, and release-validate the Starter

- Requirements: `REQ-001`, `REQ-002`, `REQ-003`, `REQ-014`, `REQ-017`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`, `REQ-024`, `REQ-025`, `REQ-026`, `REQ-027`, `REQ-028`, `REQ-029`, `REQ-030`
- Dependencies: `Step 7`
- Baseline state: module code and all embedded tests pass, but consumers cannot import its version from the Components BOM and no adoption/migration/rollback guidance exists.
- Observable outcome: the BOM exports the Artifact, both aggregate/module README pairs are synchronized, release-shaped reactors/dependency/package/source/Git gates pass, and validation boundaries are stated precisely.
- End state: the complete implementation is documented and distributable; no application is started, no production table is migrated, and runtime/sharding adoption remains consumer-owned.
- Test-first gate: Not applicable — this Step adds no runtime behavior; pre-change BOM/docs/source/package gates identify missing export and guidance, then final release gates validate the already-tested implementation.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: Export the validated concrete Starter for versionless consumer adoption.
- Symbols: one managed dependency entry for `egon-cola-component-common-mybatis-plus-spring-boot-starter` at `${project.version}`.
- Repository evidence: the BOM already lists concrete Common capabilities individually rather than exporting the aggregate POM.
- Dependencies and consumers: consumer applications import this BOM and then declare the Starter without a version; no transitive dependency is added merely by BOM import.
- Why now: Only a fully tested Artifact should become part of the platform distribution contract.
- Contract/signature changes: adds one dependencyManagement entry and preserves existing order/group/version convention.
- Input/output and state mapping: Components BOM import plus versionless Starter declaration maps to the current reactor version and parent-managed MP 3.5.16.
- Error and edge behavior: duplicate entry, wrong Artifact ID, literal version, dependency instead of dependencyManagement, or missing reactor module fails effective-POM/BOM tests.
- Implementation pseudocode:

```xml
<dependencyManagement><dependencies>
  <!-- retain existing managed component artifacts -->
  <dependency><groupId>${project.groupId}</groupId><artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId><version>${project.version}</version></dependency>
</dependencies></dependencyManagement>
```

- Verification contribution: proves consumers can resolve the exact opt-in Artifact through the standard Components BOM.
- After this file: Distribution metadata is complete; documentation now describes usage.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/README.md`

- Purpose: Add the Starter to the English Common capability index and state its opt-in boundary.
- Symbols: module table/link, short capability summary, validation command reference.
- Repository evidence: the existing README documents each Common child and mirrors the Chinese file.
- Dependencies and consumers: links File 4 and module coordinates; read by platform maintainers/consumers.
- Why now: Aggregate docs must expose the newly exported Artifact only after its contract is proven.
- Contract/signature changes: adds no code; describes AR/Mapper/IService, tenant isolation, validation, and explicit consumer schema responsibility.
- Input/output and state mapping: a reader moves from Common index to exact module dependency/guide and understands that adding the aggregate alone does not enable persistence.
- Error and edge behavior: does not claim live ShardingSphere/production proof, automatic schema migration, cross-tenant APIs, custom Injector, or SecurityContext adapter.
- Implementation pseudocode:

```markdown
| MyBatis-Plus Starter | `egon-cola-component-common-mybatis-plus-spring-boot-starter` | EgonModel AR, zero-addition Mapper, 57-method Service, TenantID and repository validation |
Use the concrete module explicitly; Common remains an aggregator.
See the module guide for required columns, MDC defaults, provider overrides, limits, validation, and rollback.
```

- Verification contribution: aggregate documentation/source scan proves the module is discoverable and accurately bounded.
- After this file: English aggregate documentation references the full module guide.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/README.zh-CN.md`

- Purpose: Mirror the Common module index and boundary in Chinese.
- Symbols: synchronized module row/link, capability and validation boundary text.
- Repository evidence: Common maintains paired English/Chinese READMEs with matching module inventory.
- Dependencies and consumers: links File 5 and same Artifact; consumed by Chinese reviewers/adopters.
- Why now: The repository requires both language views to change together.
- Contract/signature changes: documentation-only mirror with identical coordinates/symbols/defaults and no extra implied behavior.
- Input/output and state mapping: Chinese readers follow the same aggregate-to-module adoption path and see the same consumer-owned schema/runtime boundary.
- Error and edge behavior: the mirror must not reintroduce removed tenant aliases, businessId, user name, `deleted`, custom Injector, or live validation claims.
- Implementation pseudocode:

```markdown
| MyBatis-Plus Starter | `egon-cola-component-common-mybatis-plus-spring-boot-starter` | EgonModel AR、零新增方法 Mapper、57 方法 Service、TenantID 与仓储校验 |
必须显式依赖具体模块，Common 继续只是聚合 POM。
公共列迁移、Provider 覆盖、限制、验证与回滚见模块中文说明。
```

- Verification contribution: bilingual symbol/default parity scan compares this file with File 2.
- After this file: Common aggregate documentation is synchronized.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md`

- Purpose: Publish the complete English consumer and operator guide for the Starter.
- Symbols: dependency/configuration snippets, EgonModel/Mapper/Service examples, Provider override, layered validation/conversion, schema checklist, limits, migration, verification, rollback.
- Repository evidence: existing Common Starters ship module-local English READMEs and repository validation commands.
- Dependencies and consumers: describes all final production symbols/properties and points to common-core `ValidationUtils`/`BaseConverter` without adding code.
- Why now: Documentation must be generated from the actual GREEN implementation and effective POM, not the pre-implementation design alone.
- Contract/signature changes: documents exact seven fields, 57 official enhancements, 14 AR abilities, zero custom Mapper methods/Injector, six/three fill sets, MDC keys, property defaults, and safe custom Provider/handler rules.
- Input/output and state mapping: adoption sequence maps BOM/dependency -> consumer DDL/migration -> MDC or custom Providers -> concrete Model/Mapper/technical Service -> DTO/PO converters/business Service -> validation commands.
- Error and edge behavior: covers missing context, malformed tenant, dirty historic rows, unsupported SQL, batch/page bounds, wide writes, no cross-tenant bypass, migration/rollback order, and static/module versus live topology proof.
- Implementation pseudocode:

```markdown
## Install and configure
## Define EgonModel, EgonColaMapper, and technical Service; validate DTO -> PO -> Model through BaseConverter boundaries
## Tenant/audit fill, official method enhancements, required seven-column schema, limits, failure codes, migration and rollback
## Verification boundary: embedded H2/module evidence only; consumers verify dialect, indexes, SecurityContext mapping, and sharding topology
```

- Verification contribution: docs gate checks every public type/property/default/forbidden alias and exact validation commands.
- After this file: English adopters have a complete, non-overclaiming guide.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md`

- Purpose: Publish the complete synchronized Chinese consumer/operator guide.
- Symbols: same sections, coordinates, Java/property examples, schema/validation/migration/rollback content as File 4.
- Repository evidence: paired module documentation is the established Common convention and the user reviews design details in Chinese.
- Dependencies and consumers: mirrors the final implementation contract for Chinese platform/consumer teams.
- Why now: Both guides must be reviewed and committed atomically with BOM export.
- Contract/signature changes: documentation-only; preserves exact class/method/property/field names and explicit no-custom-API decision.
- Input/output and state mapping: dependency and context setup lead to the same Model/Mapper/Service and DTO/PO/Model flow as English documentation.
- Error and edge behavior: explicitly warns that missing TenantID fails closed, historic invalid rows fail result validation, ordinary update cannot mutate tenant/delete fields, and live DB/sharding proof remains consumer-owned.
- Implementation pseudocode:

```markdown
## 引入与配置
## 定义 EgonModel、EgonColaMapper 与技术 Service；通过 BaseConverter 完成 DTO -> PO -> Model 分层校验
## TenantID/审计填充、官方方法增强、七列建表前置、限制、错误、迁移与回滚
## 验证边界：这里只证明静态、模块与 H2；方言、索引、SecurityContext 映射和真实分片由采用方验证
```

- Verification contribution: bilingual parity and source scans prove no drift or obsolete term survives.
- After this file: Distribution metadata and both documentation languages match the validated Artifact.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test && ./mvnw -B -ntp -f egon-cola-components/pom.xml test && ./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter dependency:tree && jar tf egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/target/egon-cola-component-common-mybatis-plus-spring-boot-starter-*.jar && git diff --check`
- Expected result: all commands exit 0; BOM/effective POM resolves the Artifact/3.5.16; three relevant reactors pass; Jar contains production classes/import resource but no test support/schema; docs/source scans are synchronized and clean.
- Failure returns to: File 1 for export/effective POM, Files 2-5 for documentation drift, Step 2 File 3 for scopes/package, or the owning earlier Step for any regression.
- Completion criteria: all 30 requirements map to GREEN evidence, all eight semantic commits exist in order, unrelated worktree state is preserved, and final audit explicitly distinguishes module proof from consumer production proof.
- Rollback: consumers can remove the Starter dependency and auto-configuration; platform rollback reverts File 1/doc commit first, then prior implementation commits in reverse. Consumer schema rollback is separately owned and must preserve/backfill data safely.
- Commit paths: `egon-cola-components/egon-cola-components-bom/pom.xml`, `egon-cola-components/egon-cola-component-common/README.md`, `egon-cola-components/egon-cola-component-common/README.zh-CN.md`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md`
- Commit: `docs(common-mybatis-plus): export and document starter`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Baseline before Step 1 | `/Users/mario/SelfProject/Egon-COLA` | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -DskipTests validate` | Existing Common Maven model | Exit 0 before target implementation; otherwise separate baseline failure. | Repository baseline/user if target drift | Pre-existing static model |
| RED Step 1 | repository root | Step 1 focused tests before Files 4-6 | Core validation/boundary | Fails on absent class/overload or forbidden validation import, not unrelated fixture failure. | Step 1 Files 1-3/Spec | `REQ-017`,`REQ-026`; unit/static |
| GREEN Step 1 | repository root | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml -pl egon-cola-component-common-core,egon-cola-component-common-test -am -Dtest=ValidationUtilsTest,CoreBoundaryTest,SourceBoundaryAssertTest -Dsurefire.failIfNoSpecifiedTests=false test` | Core/common-test | Exit 0; stable violations and narrow allowlist pass. | Step 1 Files 4-6 | `REQ-017`,`REQ-026`; unit/static |
| GREEN Step 2 | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -DskipTests package` | Maven bootstrap | Exit 0; empty child Jar resolves with exact scopes. | Step 2 POM owner | `REQ-001`,`REQ-017`,`REQ-019`,`REQ-021`; static/package |
| RED/GREEN Step 3 | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -pl egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonModelActiveRecordParityTest -Dsurefire.failIfNoSpecifiedTests=false test` | AR ABI/model | RED absent base; GREEN exact 14/final roots. | Step 3 Model/groups | `REQ-024`,`REQ-025`; unit/reflection |
| RED/GREEN Step 4 | repository root | same module selector with `-Dtest=EgonModelTest` | Context/fill | RED absent adapters/handler; GREEN exact MDC/fill/hooks. | Step 4 owning file | `REQ-007`,`REQ-008`,`REQ-015`,`REQ-025`; unit |
| RED/GREEN Step 5 | repository root | same selector with `-Dtest=EgonColaIServiceParityTest,EgonColaServiceImplTest` | Mapper/Service ABI/Java semantics | RED missing/insufficient declarations; GREEN zero Mapper additions and exact 57 bodies/family guards. | Step 5 API/body file | `REQ-004`-`REQ-006`,`REQ-009`-`REQ-014`,`REQ-018`,`REQ-019`,`REQ-027`; unit/reflection |
| RED/GREEN Step 6 | repository root | same selector with `-Dtest=EgonColaMybatisPlusAutoConfigurationTest,EgonColaTenantIdSqlIntegrationTest` | Boot/MyBatis/H2 | RED absent imports/chain; GREEN context and official SQL matrix pass. | Step 6 owner | `REQ-003`,`REQ-007`-`REQ-019`,`REQ-025`,`REQ-027`,`REQ-030`; embedded integration |
| GREEN Step 7 focused | repository root | same selector with `-Dtest=EgonColaActiveRecordIntegrationTest,EgonColaModelValidationIntegrationTest,EgonColaBatchTransactionIntegrationTest` | AR/layers/batches | Exit 0; scoped AR, validation flow, atomic/concurrent batches pass. | Earlier owner or current test fixture | `REQ-010`,`REQ-011`,`REQ-018`,`REQ-024`-`REQ-030`; embedded integration |
| Starter regression | repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am test` | Starter plus dependencies | Exit 0; all module tests pass. | Owning Step | All runtime module requirements; no live system |
| Common reactor | repository root | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test` | Every Common child | Exit 0; Jakarta boundary amendment causes no sibling regression. | Step 1/2/owning Step | `REQ-001`,`REQ-017`,`REQ-022`; module reactor |
| Components reactor | repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml test` | All Components | Exit 0 or evidence-backed unrelated baseline failure clearly separated; no hidden target failure. | Owning target Step or external baseline | `REQ-001`,`REQ-019`,`REQ-022`; repository module boundary |
| Dependency gate | repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter dependency:tree` plus effective-POM inspection | Direct/transitive coordinates | Direct approved dependencies only, MP pair 3.5.16; native MyBatis appears only transitively through official Starter. | Steps 1-2/8 | `REQ-017`,`REQ-019`; static dependency graph |
| Source/API negative gate | repository root | `rg` exact removed API/field/Injector names under Starter production source plus reflection tests | Production API | No removed tenant aliases/custom Injector, audit name, alternate tenant field, or wrong logical-delete property; only documentation migration warnings may name exclusions. | Owning source/doc file | `REQ-002`,`REQ-014`,`REQ-025`; static |
| Package gate | repository root | `jar tf .../target/egon-cola-component-common-mybatis-plus-spring-boot-starter-*.jar` | Published Jar | Contains production classes and imports resource; excludes support tests and `schema.sql`. | Step 2 POM/Step 6 resource scope | `REQ-003`,`REQ-021`; package |
| Documentation parity | repository root | exact symbol/property/default heading scan and manual side-by-side English/Chinese review | Four READMEs | Same coordinates, seven fields, defaults, validation/conversion flow, limitations, commands, migration/rollback. | Step 8 Files 2-5 | `REQ-020`,`REQ-024`-`REQ-030`; static/manual docs |
| Format/Git audit | repository root | `git diff --check`; `git status --short`; per-Step `git diff --cached --name-only`; `git log --oneline -8` | All target commits/worktree | No whitespace errors; exactly eight path-limited semantic commits; unrelated staged/untracked state preserved. | Owning Step or user if external drift | `REQ-023`; static Git |

These gates prove source, ABI, Spring context, embedded H2/MyBatis behavior, Maven reactors, packaging, and documentation. They do not prove a consumer's PostgreSQL/MySQL dialect, production DDL/index plan, SecurityContext mapping, ShardingSphere routing, live transaction manager, traffic profile, or historical-row cleanliness.

## 9. Migration, Compatibility, Rollout, and Rollback

### 9.1 Platform implementation sequence

1. Merge the eight Step commits in order only after this Review Plan is approved.
2. Publish the Components BOM and Starter together at the same repository version; no consumer is auto-enabled until it declares the concrete dependency.
3. Do not edit any existing Flyway migration in Egon-COLA or a consumer. This module has no production DDL.
4. Before a consumer adopts `EgonModel`, create a separate schema RFC that maps/backfills all seven columns, selects its next Flyway version, adds TenantID/logic-delete access indexes, validates legacy rows, and defines rollback/data retention.
5. Configure MDC `tenantId/userId` only as the current adapter, or provide reviewed SecurityContext Providers before enabling real traffic. The Starter never guesses String-to-Long tenant mappings.
6. Run consumer-dialect SQL integration, EXPLAIN/index checks, transaction rollback, dirty-row scan, sharding route, and observability tests before production rollout.

### 9.2 Compatibility matrix

| Concern | Compatible state | Incompatible/change trigger | Required action |
| --- | --- | --- | --- |
| MyBatis-Plus | Exactly Boot3 Starter/JSqlParser 3.5.16 and 57-method IService/14-method AbstractModel ABI. | 3.5.17+ removes/changes the frozen Service contract; older `Model` differs. | New Spec and parity migration; never silent version bump. |
| Spring/JDK | Repository Boot 3.5.16, Java 21, Jakarta Validation. | Boot 2/`javax.validation`, unsupported JDK. | Remain on platform baseline or design a compatibility release. |
| Consumer Model | Extends `EgonModel<Self>` and maps seven common fields exactly. | Shadowed fields, alternate delete/tenant columns, partial update entity with missing persisted fields. | Adapt Model/DDL and use load+merge or wrapper patch semantics. |
| Consumer table | BIGINT tenant/id, non-null audit/time/delete columns, logic-delete defaults/history cleaned. | Missing/null/incorrect column/type or dirty historic row. | One new consumer migration plus preflight data scan/backfill. |
| Context | Non-null `Long tenantId`, nonblank String user ID; any Long tenant value allowed. | Missing/malformed TenantID, unavailable user ID, ambiguous String tenant mapping. | Fail closed; deploy a reviewed Provider adapter. |
| SQL | JSqlParser-supported statements or configured exact global tables. | Unsupported dialect/custom SQL, protected SET, bypass annotation. | Add consumer integration evidence or redesign; no runtime permissive fallback. |
| Public API | Zero-method Mapper, exact 57 Service methods, existing 14 AR abilities. | Reintroducing aliases/Injector or changing official result shapes. | Return to accepted Spec/user review. |

### 9.3 Rollout checkpoints

| Checkpoint | Evidence required | Stop condition |
| --- | --- | --- |
| Library merge | Eight GREEN commits, final conformance audit, clean target diff. | Any required test/relationship/traceability gap. |
| Artifact publication | BOM/effective-POM/package gates and synchronized READMEs. | Wrong coordinate/version/scope or fixture in Jar. |
| Consumer development | Separate schema migration, provider mapping, Model/Mapper/technical Service/converter tests. | Missing columns/index/backfill or unreviewed tenant mapping. |
| Preproduction | Target dialect SQL, transaction, dirty-result, sharding route, EXPLAIN, load/capacity tests. | Parser incompatibility, cross-tenant result, validation breakage, rollback failure, unacceptable plan. |
| Production | Consumer-owned canary/observability/rollback procedure. | Any isolation, data-integrity, or compatibility signal. |

### 9.4 Rollback

- Before consumer adoption, remove/revert the BOM/docs commit, then reverse implementation commits 7 through 1; no database state exists in this repository.
- A consumer disables auto-configuration with `egon.cola.component.mybatis-plus.enabled=false` only as a controlled rollback while also removing dependence on EgonCola runtime protection. Disabling it while continuing persistence calls is unsafe and not an isolation bypass.
- Consumer schema rollback is not a library action. Preserve data, stop writes, revert application dependency/code first, then execute the consumer's separately reviewed forward-safe migration/rollback plan. Never edit an applied migration checksum.
- If only a custom Provider/handler/outer chain fails, restore the validated default or last known safe implementation; startup fail-fast prevents partial protection from serving traffic.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Owning Step(s) | Files/symbols | Primary proof | Completion evidence |
| --- | --- | --- | --- | --- |
| `REQ-001` | Steps 2, 8 | parent/Common/Starter POM, BOM | Maven package/reactors/effective BOM | One resolvable exported child Jar |
| `REQ-002` | Steps 3-8 | all production types/docs | API/source negative scan | Only EgonCola exceptions plus named EgonModel/ValidationUtils |
| `REQ-003` | Steps 4, 6, 8 | properties, auto-config, imports | ContextRunner/import/package tests | Enabled/disabled discovery works |
| `REQ-004` | Steps 5-6 | EgonColaMapper | reflection/default-statement H2 | Zero declarations; official statements execute |
| `REQ-005` | Steps 5-7 | IService/Impl/TestBusinessService | compile/focused/H2 | Consumer extension executes |
| `REQ-006` | Step 5 | IService/Impl | reflection parity | Exactly 57 declared/overridden |
| `REQ-007` | Steps 4, 6 | tenant Provider/MDC/guard/TenantLine | Model/ContextRunner/H2 | Any Long accepted; missing/invalid fail before JDBC |
| `REQ-008` | Steps 3, 4, 6 | EgonModel, handler, schema | metadata/fill/H2/result validation | Only non-null persistent Long tenantId |
| `REQ-009` | Steps 5-6 | official Service/Mapper calls | multi-tenant H2 | list/count multi-row; id/optional exact and scoped |
| `REQ-010` | Steps 5-7 | Service, guard, TenantLine, plugin | H2 AR/Mapper/IService matrix | One isolation chain for every entry |
| `REQ-011` | Steps 4, 5, 7 | batch properties/Service/tests | unit pre-JDBC and H2 rollback | bounded snapshot batches atomic |
| `REQ-012` | Steps 4, 6 | properties/ordered interceptors/validator | ContextRunner + H2 unsafe writes | Guard 100, Block 200, Tenant 300, Lock 400, Page 500 |
| `REQ-013` | Steps 4-6 | pagination properties/Service/plugin | unit + H2 page tests | positive size/max 500/overflow false |
| `REQ-014` | Steps 5, 6, 8 | empty Mapper/default Injector/negative scans | parity, Configuration mapped IDs, source scan | No aliases, custom statements, or Injector |
| `REQ-015` | Steps 4, 6 | Providers/MetaObjectHandler | fixed Clock + H2 stored row | Exact six insert/three update fields; no id/name fill |
| `REQ-016` | Steps 4, 6 | SPIs/conditional beans/contract validator | ContextRunner safe/broken replacement matrix | Safe customization or startup failure |
| `REQ-017` | Steps 1, 2, 6, 8 | core/Starter POMs/source boundary | dependency tree/effective POM/reactors | Only approved direct dependencies/boundary exception |
| `REQ-018` | Steps 3, 5-7 | Model/Service/plugins/tests | method-family/AR/H2/transaction tests | Official shapes/wrappers/chains/delete/lock preserved |
| `REQ-019` | Steps 2, 5, 8 | managed deps/parity/BOM | dependency tree/reflection/effective POM | All baselines resolve 3.5.16 |
| `REQ-020` | Step 8 | four READMEs | bilingual parity/manual review | Complete synchronized guides |
| `REQ-021` | Steps 2, 6-8 | Starter test tree/POM/package | full module tests + Jar listing | Tests local; fixtures/schema excluded from Jar |
| `REQ-022` | Steps 6-8 | embedded gates/validation report | command log | No service/external DB started; proof boundary stated |
| `REQ-023` | Steps 2, 8 and every commit | Git scopes/status | cached-path/log/diff audit | Unrelated state preserved; eight isolated commits |
| `REQ-024` | Steps 3, 7, 8 | EgonModel/AR tests/docs | reflection + real AR matrix | 14 abilities and six final roots proven |
| `REQ-025` | Steps 3, 4, 6-8 | Model/handler/schema/docs | metadata/fill/H2/negative scan | Exact seven fields and isDeleted/is_deleted |
| `REQ-026` | Steps 1, 7-8 | ValidationUtils/object flow/docs | core unit + layered integration | Manual facade/group/error contract proven |
| `REQ-027` | Steps 3, 5-8 | groups/Model facade/plugin | Service + parameter/result/AR tests | Manual/global repository validation unified |
| `REQ-028` | Steps 7-8 | DTO/PO/Model fixtures/docs | MVC/Method Validation/repository integration | Validation ownership fixed by layer |
| `REQ-029` | Steps 7-8 | TestBusinessConverters/docs | forward/reverse/list object-flow assertions | Two BaseConverter boundaries; technical fields ignored |
| `REQ-030` | Steps 3, 5-8 | constrained Model/facades/business test Service | invalid Model through all entries + service-only complex rule | Simple constraints global; complex business logic stays Service |

## 11. Risks, Blockers, and User Decisions

### 11.1 Risks

| ID | Risk | Probability | Impact | Mitigation/evidence | Status |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | MP 3.5.17+ API drift breaks 57-method contract. | High | Compile/public API failure. | Pin 3.5.16 in parent/module/BOM and use bidirectional parity. | Mitigated |
| `RISK-002` | JSqlParser rejects a consumer dialect/custom statement. | Medium | Statement fails before JDBC. | Fail closed, test official H2 statements, require consumer dialect integration; no permissive fallback. | Consumer-owned adoption prerequisite |
| `RISK-003` | Consumer custom outer interceptor/handler omits mandatory protection. | Medium | Isolation/fill/validation loss. | Startup contract validator and safe/broken replacement tests. | Mitigated |
| `RISK-004` | 57 explicit bodies become repetitive and fragile. | High | Upgrade/maintenance cost. | Private family helpers plus exact declared-method parity and method-family tests. | Accepted trade-off |
| `RISK-005` | Historical rows violate new non-null/business constraints. | High | Reads fail before returning Models. | Consumer preflight scan/backfill and separate migration; no runtime bypass. | Consumer-owned adoption prerequisite |
| `RISK-006` | Consumer identity tenant is String and cannot be safely mapped to Long. | High | Context fail closed or wrong isolation if guessed. | Starter does not guess; consumer supplies reviewed SecurityContext Provider mapping. | Consumer-owned adoption prerequisite |
| `RISK-007` | Batch context changes mid-transaction. | Medium | Mixed-tenant write risk. | Method-entry snapshot, per-statement post-fill/end checks, rollback tests. | Mitigated |
| `RISK-008` | Result traversal misses a nested Model or loops on cycles. | Medium | Dirty Model escapes or stack/resource failure. | Explicit supported containers, identity set, unit/H2 page/map/list tests, fail closed on EgonModel extraction errors. | Mitigated |
| `RISK-009` | Core Jakarta exception broadens into framework leakage. | Medium | Common-core dependency inversion. | Exact `jakarta.validation.` allow prefix, old overload preserved, dependency/source tests. | Mitigated |
| `RISK-010` | Dirty worktree causes accidental staging/overwrite. | Medium | User work loss or mixed commits. | Exact commit paths, status/cached-path audit after each Step, no broad reset/checkout. | Mitigated by process |
| `RISK-011` | Consumer schema/index/sharding plan is incompatible. | High | Runtime SQL/latency/routing failure. | Outside library proof; separate schema RFC, migration, target DB EXPLAIN and live topology verification required. | Consumer-owned adoption prerequisite |

### 11.2 Closed user decisions

| Decision | Effective answer | Plan consequence |
| --- | --- | --- |
| Dependency | Use official `mybatis-plus-spring-boot3-starter`, not native Starter/raw direct dependencies; pin 3.5.16 with same JSqlParser version. | Steps 2/8 dependency and parity gates. |
| Tenant key | Keep only arbitrary-value `Long tenantId`; persistent/current context is non-null; default source MDC and future source a custom Provider. | EgonModel, Provider, Handler, TenantLine, validation, schema all use only tenantId. |
| Audit identity | Keep user ID only, no name; fill create/update IDs and times. | Exact handler fields and negative API/doc scans. |
| Logical delete | Java `isDeleted`, physical `is_deleted`, MP `@TableLogic`. | Exact Model metadata, handler default, H2 DDL, docs. |
| AR base | User-facing class is `EgonModel`; MP 3.5.16 implementation extends real `AbstractModel`. | Step 3 parity/template implementation; no downgrade/shim. |
| Validation/conversion | Controller validates DTO, business Service validates PO/complex rules, repository validates Model; use common-core ValidationUtils and BaseConverter twice. | Steps 1, 5, 7 and docs; no business logic in Model. |
| Query/API surface | Remove the four duplicate current-tenant Service methods, three Mapper methods, and all custom Injector code; enhance official list/count/getById/getOptById and all other official contracts. | Empty EgonColaMapper, default Injector, exact 57 methods, negative tests. |

There is no open design decision blocking implementation. Consumer-specific schema, dialect, SecurityContext mapping, indexes, and live sharding are adoption prerequisites, not hidden choices for this library Plan.

## 12. Review and Acceptance

### 12.1 Review checklist

- The Plan uses the Accepted Spec as the only primary design and marks the pre-ActiveRecord Plan superseded.
- All `REQ-001` through `REQ-030` appear in Step ownership and the traceability matrix.
- The target inventory contains exactly 51 files: 6 Step 1, 3 Step 2, 4 Step 3, 9 Step 4, 8 Step 5, 10 Step 6, 6 Step 7, and 5 Step 8.
- No file is assigned to two Step commits; integration defects return to the earlier owner before Step 7 is committed.
- Public persistence surface is `EgonModel + EgonColaMapper + EgonColaIService + EgonColaServiceImpl`; Mapper adds zero methods and IService/Impl each declare exactly 57.
- Tenant/audit/delete contracts contain only `tenantId`, user IDs, Instants, and `isDeleted/is_deleted`; negative gates exclude removed fields/APIs/Injector.
- Strategy, Adapter, Template Method, and Facade are used only at demonstrated variation points; speculative patterns/layers/caches are absent.
- Validation distinguishes static/reflection/context/embedded-H2/module evidence from consumer production proof.
- Implementation does not begin until the user approves this Plan.

### 12.2 Acceptance workflow

1. The user has confirmed this Plan against the linked Accepted Spec, including Step 5's exact official-method enhancement and Step 6's shared runtime chain.
2. Plan status is now `Ready`; execution proceeds only in the declared worktree and Step order.
3. Execute one Step at a time, observe the defined RED where applicable, reach focused GREEN, audit exact paths, and create one commit.
4. After Step 8, perform a fresh conformance audit against the effective Specs and report any consumer-owned proof gaps without starting the application.

### 12.3 Final verdict

`PASS — Ready for user review`
