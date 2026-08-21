# Egon COLA Common MyBatis-Plus Starter Implementation Plan

| Field | Value |
| --- | --- |
| Document | `2026-08-20-15-01-common-mybatis-plus-implementation.md` |
| Template Version | `2` |
| Status | `Superseded` |
| Created | `2026-08-20 15:01 CST` |
| Updated | `2026-08-21 14:23 CST` |
| Owner | `Mario / Egon-COLA maintainers` |
| Repository | `Egon-COLA` |
| Scope | `egon-cola-components/egon-cola-component-common-mybatis-plus-spring-boot-starter plus parent dependency management, Common aggregation, Components BOM, bilingual documentation, and module-local tests` |
| Source Requirement | `2026-08-19 Common MyBatis-Plus request plus 2026-08-20 decisions: use the MyBatis-Plus Boot 3 Starter, use unrestricted nullable Long BusinessID, read it from MDC now without null validation, and preserve a future SecurityContext provider seam` |
| Baseline Revision | `main@4ae5419c4504250436b98886c03271889a8f2879; 2026-08-20 15:01 CST dirty-worktree snapshot; preserve unrelated Gateway MCP Spec/Plan documents` |
| Implements Spec | [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) |
| Spec Status | `Accepted` |
| Spec Revision | `Obsolete 2026-08-20 snapshot at main@4ae5419c4504250436b98886c03271889a8f2879; current Spec updated 2026-08-21 14:23 CST` |
| Effective Specs | [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md); [Common enterprise restructure](../../superpowers/specs/2026-07-07-egon-cola-component-common-enterprise-restructure-design.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | [Current ActiveRecord/TenantID implementation Plan](2026-08-21-14-23-common-mybatis-plus-implementation.md) |
| Related Plans | `None` |

## 1. Summary

This Plan implements the Common MyBatis-Plus Spec as one opt-in Spring Boot Starter, ordered from Maven contract publication through BusinessID infrastructure, injected Mapper SQL, 57-method Service parity, enhanced runtime semantics, H2/transaction proof, and release documentation. Seven semantic Steps produce one path-limited commit each. Completion is proved by RED/GREEN focused tests, reflection parity, real H2 SQL, transaction rollback, dependency-tree and packaged-Jar inspection, Common and Components reactor tests, and final Spec conformance; no service process or external database is started.

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: [Egon COLA Common MyBatis-Plus Starter full enhancement design](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md)
- Status: `Review`
- Revision: `Updated 2026-08-20 15:01 CST; main@4ae5419c4504250436b98886c03271889a8f2879`
- Approval evidence: The user explicitly invoked `egon-coding-writing-plan` after closing `DEC-001` through `DEC-003`; this authorizes a Review Plan, not implementation or Ready status.

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | [Common MyBatis-Plus Starter Spec](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) | `Review`, updated 2026-08-20 15:01 CST | Entire document, especially `REQ-001` through `REQ-023`, §7 through §18, and `INTERNAL-001` through `INTERNAL-065` | Defines every new Artifact, type, method, nullable BusinessID behavior, dependency boundary, test, and release condition |
| Normative dependency | [Common enterprise restructure](../../superpowers/specs/2026-07-07-egon-cola-component-common-enterprise-restructure-design.md) | Repository predecessor, current at baseline | §3.3 through §4.3 only | Requires `common` to remain a `pom` aggregator, concrete capabilities to be opt-in Jars, and consumable Artifacts to be exported by the Components BOM |

### 2.3 Superseded or excluded content

The FamilyAiButler Java files are reference evidence only. Their `String businessId`, single-row `selectByBusinessId`, `extention` package spelling, dual outer interceptors, and incomplete Service coverage are excluded by the primary Spec. MyBatis-Plus 3.5.17 repository APIs are excluded by `DEC-001`; 3.5.16 is the exact implementation baseline.

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | [Primary Spec](../spec/2026-08-19-16-11-common-mybatis-plus-starter.md) §4 | Add one opt-in Common MyBatis-Plus Starter and export it through aggregation/BOM. | Module resolves by Artifact ID in Common and Components reactors. | Three Maven POMs plus new module POM |
| `REQ-002` | Primary Spec §4 | Prefix every new public type with `EgonCola`. | Public-class source scan has no unprefixed API. | All production Java types |
| `REQ-003` | Primary Spec §4 | Load through Boot auto-configuration imports and support a global enabled switch. | ImportCandidates finds the configuration; disabled context has no EgonCola beans. | AutoConfiguration, properties, imports resource |
| `REQ-004` | Primary Spec §4, §9 | Provide `EgonColaMapper` while retaining every `BaseMapper` statement. | MyBatis Configuration contains default statements plus exactly three custom IDs. | Mapper and SQL Injector |
| `REQ-005` | Primary Spec §4, §9 | Provide a reusable EgonCola Service interface and implementation. | Consumer fixture compiles and executes through the concrete Service. | Service API/implementation |
| `REQ-006` | Primary Spec §4, §9 | Redeclare and explicitly override all 57 MyBatis-Plus 3.5.16 IService-visible methods. | Reflection parity reports no missing or extra upstream signature. | Service interface/implementation and parity test |
| `REQ-007` | Primary Spec §4, §7 | Resolve nullable BusinessID from an overrideable Provider; default reads MDC. | MDC Long, absent MDC, malformed text, custom Provider backoff, and thread isolation are asserted. | Provider SPI and MDC adapter |
| `REQ-008` | Primary Spec §4, §10 | Use boxed `Long businessId` / `business_id` with no range or null validation. | Null, zero, negative, and positive values bind without Java rejection. | Entity contract, properties, handler, SQL fixture |
| `REQ-009` | Primary Spec §4, §9 | Treat BusinessID as a multi-row scope and add list, count, and BusinessID-plus-id APIs. | Multi-row H2 results and exact/optional results match the current scope. | Four Service and three Mapper additions |
| `REQ-010` | Primary Spec §4, §7 | Normalize entity/explicit parameters from the current Provider and tenant-scope all supported SQL. | Existing/mismatched/null values are overwritten; SQL contains a LongValue or NullValue condition; column mutation is rejected. | Service, guard, TenantLine handler |
| `REQ-011` | Primary Spec §4, §7, §9 | Bound batch sizes, normalize an entire batch from the Service-entry value, and make batch writes atomic. | Empty/null/oversize behavior is stable; middle failure rolls back; mixed initial IDs are overwritten. | Service helpers and transaction tests |
| `REQ-012` | Primary Spec §4, §7 | Order guard, BlockAttack, TenantLine, optimistic lock, and pagination deterministically. | Context and SQL tests prove order; unsafe outer chains fail startup. | InnerInterceptor beans and validator |
| `REQ-013` | Primary Spec §4, §9, §10 | Enforce configured pagination limits and overflow behavior. | Size 0/501 fail before SQL; valid page is scoped and counted. | Properties, Service page guards, pagination bean |
| `REQ-014` | Primary Spec §4, §9 | Extend `DefaultSqlInjector` without losing defaults and preserve logic delete. | Default/custom MappedStatements exist and custom queries exclude deleted rows. | Injector and three AbstractMethod types |
| `REQ-015` | Primary Spec §4, §10 | Fill normalized BusinessID and configured audit-time fields with supported Java time types. | Fixed Clock tests prove create/update fill and nullable BusinessID overwrite. | MetaObjectHandler and Clock wiring |
| `REQ-016` | Primary Spec §4, §7 | Back off for safe custom beans and fail fast when a replacement breaks the required contract. | ContextRunner accepts safe replacements and rejects missing/order-invalid plugin or Injector contracts. | Conditional beans and contract validator |
| `REQ-017` | Primary Spec §4, §6 | Directly depend on MyBatis-Plus Boot3 Starter and JSqlParser, not native/raw MyBatis declarations or unrelated platform/data dependencies. | POM scan and dependency tree show the approved direct edge and excluded direct/unrelated edges. | Parent dependency management and module POM |
| `REQ-018` | Primary Spec §4, §9 | Preserve upstream return shapes, wrapper/chain behavior, logic delete, optimistic locking, and translated errors while enhancing safety. | Service/unit and H2 integration matrix passes. | All 57 method bodies and plugins |
| `REQ-019` | Primary Spec §4, §6, §9 | Freeze MyBatis-Plus to 3.5.16. | Effective POM and parity fixture resolve exactly 3.5.16. | Parent POM and dependency contract test |
| `REQ-020` | Primary Spec §4, §16 | Publish synchronized English and Chinese usage, configuration, limits, migration, and rollback documentation. | Both README pairs contain matching symbols, examples, and warnings. | Four Markdown files |
| `REQ-021` | Primary Spec §4, §14 | Keep all focused tests inside the Starter and package no test fixture. | Tests run from the module; packaged Jar excludes schema/test classes. | Module test tree and Jar gate |
| `REQ-022` | Primary Spec §4, §14 | Validate statically/module-wise without starting services or claiming live sharding proof. | All listed non-runtime commands exit zero; validation report states boundary. | Execution gates only |
| `REQ-023` | Primary Spec §4, §16 | Preserve unrelated dirty-worktree content and use isolated commits. | Every commit is path-limited; final status retains unrelated Gateway documents. | All Steps and final Git audit |

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Step 1 first makes the child Artifact resolvable and proves Boot import/disable behavior. Step 2 establishes the boxed nullable BusinessID SPI, the MDC Adapter strategy, the ordered plugin chain, fill handler, and fail-fast replacement validation. Step 3 publishes Mapper SQL only after tenant infrastructure exists. Step 4 freezes the complete Java API with reflection parity before behavior is optimized. Step 5 fills every explicit override using shared family helpers, transaction boundaries, argument guards, and BusinessID normalization. Step 6 runs the real H2/MyBatis path, fixes only evidence-backed integration gaps, and proves transaction/concurrency behavior. Step 7 exports the Artifact through the BOM, synchronizes documentation, and runs release-shaped reactors and package inspection.

No database migration, generated client, frontend, network contract, cache, job, controller, or service process exists in this scope. Compilation remains possible after every commit: Step 1 supplies the module and base auto-configuration; later commits only add types referenced by their own tests or already-published predecessors.

### 4.2 Test-first strategy

| Behavior | RED test and expected failure | Minimum GREEN implementation | Refactor/wiring allowed after GREEN |
| --- | --- | --- | --- |
| Artifact/import/disable | AutoConfiguration test cannot find the new import/type/disabled boundary. | Module POM, properties, empty-safe auto-configuration, imports resource. | POM ordering and configuration metadata only |
| Nullable MDC BusinessID and plugin contract | Context tests fail because Provider, Long/null parsing, ordered InnerInterceptors, handler, and replacement validator are absent. | Provider/Adapter/Scoped/TenantLine/guard/handler/validator beans. | Extract private validators inside the named classes; no Holder/cache |
| Mapper/Injector | MappedStatement assertions fail because custom methods and Injector do not exist. | Extend `DefaultSqlInjector`; append three AbstractMethods with bound parameters and logic-delete fragments. | Shared local SQL-fragment helper only when duplication is real |
| 57-method parity | Reflection test fails on missing declarations/overrides. | Exact 3.5.16 signatures in interface/class with compile-safe bodies. | Group private helpers by read/write/batch/page/chain family |
| Enhanced Service semantics | Fake-Mapper tests fail on unnormalized IDs, unbounded batches/pages, missing transactions, or inherited defaults. | Explicit bodies for all 57 upstream and four custom methods. | Remove duplicate guards without hiding any override |
| SQL/transaction/concurrency | H2 tests fail on missing predicates, logic-delete, null binding, plugin order, rollback, or context bleed. | Integration-grounded corrections to existing named implementation types. | SQL/test-fixture cleanup only |
| Distribution/docs | Source/package gates fail on missing BOM entry, docs, or forbidden dependency declaration. | BOM export and bilingual docs synchronized to actual code. | Formatting and link corrections only |

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | Components parent POM, Common aggregator, new module bootstrap | Establishes the only compilable Maven entry |
| Step 2 | Step 1 | None | AutoConfiguration, properties, BusinessID/plugin packages | These symbols form one context contract |
| Step 3 | Step 2 | None | Mapper/Injector packages and Injector wiring | Injector relies on configured BusinessID/logic-delete semantics |
| Step 4 | Step 3 | None | Service interface, implementation declaration, parity test | Public API must freeze before behavior work |
| Step 5 | Step 4 | None | Service implementation and focused Service test | All explicit bodies share the frozen signature set |
| Step 6 | Step 5 | None | Integration tests/support/schema and evidence-driven production corrections | Real SQL consumes every prior layer |
| Step 7 | Step 6 | None | BOM and four READMEs | Documentation/export must describe proven code |

The implementation is intentionally sequential. Parallel writes would overlap the same AutoConfiguration or Service implementation and would weaken the required RED/GREEN and per-Step commit history.

### 4.4 Commit boundaries

Each Step produces exactly one semantic, path-limited commit after its focused GREEN gate. A Step may touch a previously created type only where the Step names the new symbols/body families it owns. No Step stages the Review Spec/Plan, unrelated Gateway documents, or paths outside the inventory. Failed RED tests are observed but never committed; the corresponding GREEN state is the commit boundary.

### 4.5 Spec Simplicity and Implementation-necessity Audit

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| One concrete Starter Artifact | Necessary, Spec §6-§8 | Common is a `pom` aggregator and existing ID capability is a concrete Starter child. | Putting dependencies in Common POM would pollute every consumer. | One POM, one import resource, one BOM entry. | Implement |
| MyBatis-Plus Boot3 Starter 3.5.16 | User-selected, `DEC-001` | Components parent is Boot 3.5.16/Java 21; no current MyBatis-Plus edge exists. | Native MyBatis Starter or raw MyBatis-Plus violates the decision. | One managed version and two direct MP dependencies. | Implement |
| Provider Strategy plus MDC Adapter | Necessary, `DEC-003`, Spec §7 | Current source must be MDC and future source SecurityContext; no reusable Egon persistence context exists. | Direct static MDC calls in 57 methods duplicate source coupling; a new ThreadLocal Holder duplicates MDC. | Two small interfaces/classes and one conditional Bean; stateless reads at execution points. | Implement |
| `EgonColaBusinessScoped` instead of BaseEntity | Necessary, Spec §10 | Common restructure intentionally removed `BaseEntity`. | Reflection-only property access loses compile-time bounds; new base PO recreates rejected inheritance. | One boxed-Long behavioral interface. | Implement |
| Ordered InnerInterceptor Beans plus validator | Necessary, Spec §7 | Official 3.5.16 auto-config composes ordered `InnerInterceptor` beans unless an outer bean replaces it. | Multiple outer interceptors cannot prove order; silently accepting custom outer chains loses protection. | Five ordered beans and startup introspection. | Implement |
| Three custom Mapper methods | Necessary, Spec §9 | Reference single-row BusinessID method conflicts with multi-row tenant semantics. | Fetching all current rows then filtering/counting in Service is fetch-then-forward and wastes DB work. | Three injected statements, all using bound values and logic delete. | Implement |
| 57 explicit Service overrides | User-required, Spec §9 | 3.5.16 exposes 57 methods; 3.5.17 removes the API. | Inheriting defaults would not meet full rewrite or version-freeze requirements. | High maintenance cost contained by parity test and private family helpers. | Implement |
| Four custom Service methods | Necessary, Spec §9 | Mapper operations need a Provider-owned caller-safe surface. | Caller-supplied BusinessID overloads duplicate trusted context and expand cross-scope risk. | Four thin delegations over three Mapper statements. | Implement |
| SQL Injector subclass | Necessary, Spec §8-§9 | MyBatis-Plus extension point preserves default methods only through `DefaultSqlInjector`. | XML per consumer duplicates SQL and cannot publish Mapper defaults. | One Injector plus three AbstractMethods. | Implement |
| Contract validator | Necessary, Spec §7 | `@ConditionalOnMissingBean` allows outer interceptor/Injector replacement. | Refusing all customization contradicts override requirements; accepting all is unsafe. | Startup-only low-frequency validation. | Implement |
| MetaObjectHandler and Clock | Necessary, Spec §10, §15 | Reference code fills time and BusinessID; repository uses injectable Spring beans. | Fixed `new Date()` is nondeterministic and incomplete across Java time types. | One handler, one optional Clock Bean, fixed-clock tests. | Implement |
| Cache/factory/facade/domain layer | Unnecessary, Spec §3 and §6 | No remote state, object graph construction, or business domain behavior exists. | Direct Spring/MyBatis SPI composition is simpler. | Extra layers would add files without a variation point. | Do not implement |

The audit finds no caller-supplied value that should replace the Provider, no fetch-then-forward API, no duplicate DTO/PO/Mapper model, and no speculative cache. Strategy is used only for BusinessID source variation; Adapter is used for MDC and TenantLine integration; MyBatis-Plus inheritance is its required Template Method/SPI shape. Factory, Decorator, Facade, State, Observer, and Chain-of-Responsibility additions are rejected because direct beans and explicit overrides already express the required behavior.

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| Maven/bootstrap contract | `REQ-001`,`REQ-003`,`REQ-017`,`REQ-019`,`REQ-021`,`REQ-023` | AutoConfiguration import and direct-POM assertions | Current Components/Common parents | Resolvable Starter and base configuration | Every later Step | Step 1 |
| Nullable BusinessID and plugin chain | `REQ-002`,`REQ-007`,`REQ-008`,`REQ-010`,`REQ-012`,`REQ-015`,`REQ-016` | ContextRunner MDC/plugin/fill/replacement tests | Step 1 | Provider, Scoped contract, ordered interceptors, handler, validator | Mapper and Service runtime | Step 2 |
| Mapper/Injector contract | `REQ-004`,`REQ-009`,`REQ-014`,`REQ-017` | MappedStatement and SQL-template assertions | Step 2 | Mapper API and three injected methods | Custom Service methods/integration | Step 3 |
| Service parity contract | `REQ-005`,`REQ-006`,`REQ-018`,`REQ-019` | Reflection signature equality | Step 3 | 57 declarations/overrides plus four custom signatures | Behavioral Service tests | Step 4 |
| Enhanced Service behavior | `REQ-005`,`REQ-007`-`REQ-013`,`REQ-018` | Fake-Mapper input/result/error/transaction tests | Step 4 | Complete explicit method bodies and family helpers | H2 integration | Step 5 |
| Real SQL and transaction proof | `REQ-007`-`REQ-018`,`REQ-021`,`REQ-022` | H2 SQL, rollback, concurrency tests | Step 5 | Verified full persistence chain | Release/export | Step 6 |
| Distribution and documentation | `REQ-001`,`REQ-017`,`REQ-019`-`REQ-023` | BOM, package, dependency, docs, reactor gates | Step 6 | Consumable documented Artifact | User review/implementation acceptance | Step 7 |

## 5. Change File Tree

```text
egon-cola-components/
├── pom.xml                                                        MODIFY
├── egon-cola-components-bom/pom.xml                               MODIFY
└── egon-cola-component-common/
    ├── pom.xml                                                    MODIFY
    ├── README.md                                                  MODIFY
    ├── README.zh-CN.md                                            MODIFY
    └── egon-cola-component-common-mybatis-plus-spring-boot-starter/ CREATE
        ├── pom.xml
        ├── README.md
        ├── README.zh-CN.md
        └── src/
            ├── main/java/top/egon/cola/component/common/mybatis/
            │   ├── autoconfigure/{EgonColaMybatisPlusAutoConfiguration,EgonColaMybatisPlusProperties,EgonColaMybatisPlusContractValidator}.java
            │   ├── business/{EgonColaBusinessIdProvider,EgonColaMdcBusinessIdProvider,EgonColaBusinessScoped,EgonColaBusinessIdTenantLineHandler}.java
            │   ├── exception/EgonColaMybatisPlusConfigurationException.java
            │   ├── extension/{EgonColaMapper,EgonColaIService,EgonColaServiceImpl}.java
            │   ├── extension/injector/EgonColaSqlInjector.java
            │   ├── extension/injector/method/{EgonColaSelectListByBusinessId,EgonColaSelectCountByBusinessId,EgonColaSelectByBusinessIdAndId}.java
            │   ├── handler/EgonColaMetaObjectHandler.java
            │   └── interceptor/EgonColaBusinessIdGuardInnerInterceptor.java
            ├── main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
            ├── test/java/top/egon/cola/component/common/mybatis/
            │   ├── autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java
            │   ├── contract/EgonColaIServiceParityTest.java
            │   ├── extension/EgonColaServiceImplTest.java
            │   ├── integration/{EgonColaBusinessIdSqlIntegrationTest,EgonColaBatchTransactionIntegrationTest}.java
            │   └── support/{TestBusinessRecord,TestBusinessMapper,TestBusinessService,TestBusinessIdProvider}.java
            └── test/resources/schema.sql
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MODIFY | `egon-cola-components/pom.xml` | Boot 3.5.16/Java 21 dependency management; no MyBatis-Plus property | `mybatis-plus.version=3.5.16` and managed Boot3 Starter/JSqlParser | Single upstream version authority | Step 1 | `REQ-017`,`REQ-019` | effective-POM/dependency tree |
| MODIFY | `egon-cola-components/egon-cola-component-common/pom.xml` | Seven child modules | Adds the new Starter module | Common aggregation | Step 1 | `REQ-001`,`REQ-023` | Common reactor |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml` | Path absent | Jar with approved production/test dependencies | Artifact build boundary | Step 1 | `REQ-001`,`REQ-017`,`REQ-019`,`REQ-021` | POM scan/module test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java` | Type absent | Complete `egon.cola.component.mybatis-plus` property model | Typed defaults/bounds | Step 1 | `REQ-003`,`REQ-008`,`REQ-011`-`REQ-016` | properties/context tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java` | Type absent; ID Starter shows repository pattern | Conditional default beans and ordered wiring | Boot entry point | Steps 1-3 | `REQ-003`,`REQ-012`,`REQ-014`-`REQ-016` | ContextRunner |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | Resource absent | One auto-configuration class name | Boot metadata registration | Step 1 | `REQ-003`,`REQ-021` | ImportCandidates/Jar |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java` | Test absent; ID Starter has ContextRunner pattern | Import, property, Provider, plugin, handler, Injector, replacement, dependency boundary cases | Auto-config contract proof | Steps 1-3 | `REQ-003`,`REQ-007`,`REQ-008`,`REQ-012`,`REQ-015`-`REQ-019` | focused test selectors |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdProvider.java` | SPI absent | `Long currentBusinessId()` | Future-proof source Strategy | Step 2 | `REQ-002`,`REQ-007`,`REQ-016` | Provider tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcBusinessIdProvider.java` | Adapter absent | Configured MDC read, null pass-through, `Long.valueOf` | Current context Adapter | Step 2 | `REQ-007`,`REQ-008` | MDC matrix/concurrency |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessScoped.java` | Contract absent; Common BaseEntity intentionally removed | Boxed Long getter/setter | Consumer PO generic bound | Step 2 | `REQ-002`,`REQ-008`,`REQ-010`,`REQ-015` | compile fixture |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdTenantLineHandler.java` | Handler absent | Provider-backed tenant column/expression/ignored-table policy | MyBatis tenant Adapter | Step 2 | `REQ-007`,`REQ-008`,`REQ-010`,`REQ-016` | Context/H2 SQL |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java` | Exception absent | Non-secret startup contract error | Stable validation failure | Step 2 | `REQ-002`,`REQ-016` | failed-context assertions |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java` | Validator absent | Ordered outer-chain and Injector replacement validation | Prevent silent contract loss | Step 2 | `REQ-012`,`REQ-014`,`REQ-016` | replacement tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java` | Handler absent | Nullable BusinessID plus Clock-based audit fill | Persistence metadata fill | Step 2 | `REQ-008`,`REQ-015`,`REQ-016`,`REQ-018` | fixed-Clock tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaBusinessIdGuardInnerInterceptor.java` | Guard absent | Parameter/entity normalization and tenant-column mutation rejection | Direct Mapper/write guard | Step 2 | `REQ-008`,`REQ-010`,`REQ-012`,`REQ-018` | guard/H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java` | Mapper absent | BaseMapper plus three boxed-Long methods | Consumer Mapper API | Step 3 | `REQ-002`,`REQ-004`,`REQ-009`,`REQ-014` | Mapper contract/H2 |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/EgonColaSqlInjector.java` | Injector absent | Default method list plus three custom methods | Statement registration | Step 3 | `REQ-004`,`REQ-014`,`REQ-016` | Configuration assertions |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectListByBusinessId.java` | Method absent | Bound list query with logic delete | Multi-row query statement | Step 3 | `REQ-009`,`REQ-014` | mapped SQL test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectCountByBusinessId.java` | Method absent | Bound count query with logic delete | Scope count statement | Step 3 | `REQ-009`,`REQ-014` | mapped SQL test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectByBusinessIdAndId.java` | Method absent | Bound BusinessID plus TableId query with logic delete | Exact query statement | Step 3 | `REQ-009`,`REQ-014`,`REQ-018` | mapped SQL test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java` | Test absent | Exact 57-signature interface/class parity and version assertion | API drift gate | Step 4 | `REQ-006`,`REQ-019` | reflection test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java` | API absent | 57 upstream redeclarations plus four custom methods | Public Service contract | Step 4 | `REQ-002`,`REQ-005`,`REQ-006`,`REQ-009`,`REQ-018` | parity/compile fixture |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java` | Implementation absent | 57 explicit overrides, four custom methods, family helpers | Reusable enhanced implementation | Steps 4-6 | `REQ-005`-`REQ-013`,`REQ-018` | parity/unit/H2 tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java` | Test absent | Fake-Mapper matrix for every method family | Fast behavior/argument proof | Step 5 | `REQ-005`-`REQ-013`,`REQ-018` | focused unit test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBusinessIdSqlIntegrationTest.java` | Test absent | H2 plugin/custom SQL/null/logic-delete/page/lock/concurrency matrix | Real statement proof | Step 6 | `REQ-007`-`REQ-010`,`REQ-012`-`REQ-018`,`REQ-021`,`REQ-022` | Spring/MyBatis test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java` | Test absent | H2 batch overwrite/chunk/rollback matrix | Transaction proof | Step 6 | `REQ-011`,`REQ-018`,`REQ-021`,`REQ-022` | Spring transaction test |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessRecord.java` | Fixture absent | TableName/TableId/Version/TableLogic/FieldFill boxed-Long PO | Shared integration entity | Step 6 | `REQ-008`,`REQ-015`,`REQ-018` | H2 mapping |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java` | Fixture absent | `EgonColaMapper<TestBusinessRecord>` | Mapper scan fixture | Step 6 | `REQ-004`,`REQ-009`,`REQ-014` | H2 context |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java` | Fixture absent | Concrete `EgonColaServiceImpl` subclass | Service/transaction fixture | Step 6 | `REQ-005`,`REQ-018` | H2 context |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessIdProvider.java` | Fixture absent | Thread-confined mutable Provider for tests | Long/null/concurrency control | Step 6 | `REQ-007`,`REQ-008`,`REQ-010` | H2/concurrency tests |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql` | Fixture absent | Nullable H2 business/global tables and constraints | Test-only schema | Step 6 | `REQ-008`,`REQ-011`,`REQ-014`,`REQ-018`,`REQ-021` | H2 startup/Jar exclusion |
| MODIFY | `egon-cola-components/egon-cola-components-bom/pom.xml` | Existing Common Artifacts exported individually | Adds managed Starter dependency | Consumer versionless adoption | Step 7 | `REQ-001`,`REQ-019`,`REQ-020` | BOM/effective POM |
| MODIFY | `egon-cola-components/egon-cola-component-common/README.md` | Existing module list/design/validation | Adds English module link and usage boundary | Aggregator documentation | Step 7 | `REQ-020`,`REQ-022` | docs scan |
| MODIFY | `egon-cola-components/egon-cola-component-common/README.zh-CN.md` | Chinese mirror exists | Adds synchronized Chinese module link and usage boundary | Aggregator documentation | Step 7 | `REQ-020`,`REQ-022` | docs scan |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md` | File absent | Complete English dependency/config/API/limits/migration/rollback guide | Consumer documentation | Step 7 | `REQ-020`,`REQ-022` | docs/source scan |
| CREATE | `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md` | File absent | Synchronized Chinese guide | Consumer documentation | Step 7 | `REQ-020`,`REQ-022` | docs/source scan |

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Apply the user-provided AGENTS rules: smallest safe change, existing JavaDoc style, one tested commit per Step, no service startup, no existing Flyway edits, and no unrelated refactor.
- Recheck `git rev-parse HEAD`, `git branch --show-current`, and path-scoped `git status --short` before Step 1 because the baseline changed while design documents were being written.
- Preserve `docs/egon/plan/2026-08-19-14-28-gateway-engine-mcp-package-refactor.md` and `docs/egon/spec/2026-08-19-13-51-gateway-engine-mcp-package-refactor.md`; never stage them in this implementation.
- The Review Spec and this Review Plan are approval artifacts, not Step commit paths. Execution begins only after user approval changes the effective readiness state.
- No generated source is edited manually. Spring configuration metadata is generated by the configuration processor during the build.

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| JDK/Maven | `./mvnw -version` from repository root | Wrapper runs on Java 21-compatible JDK | Toolchain only |
| Parent baseline | `./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests validate` | Existing Components model resolves before edits | Static Maven model |
| Upstream API | MyBatis-Plus 3.5.16 `IService`/`IRepository` published source recorded in primary Spec `EVD-010` | Exactly 57 visible non-static methods | Published-source/compile contract, not live DB |
| Auto-config pattern | Existing Common ID Starter `IdGeneratorAutoConfiguration` and its ContextRunner test | `@AutoConfiguration`, conditional default Bean, imports metadata | Repository source pattern |
| SQL integration | New module test dependencies `spring-boot-starter-test` and H2 | In-memory DB only; no external credentials | Module integration |
| Worktree safety | `git status --short -- docs/egon egon-cola-components` | Only approved target paths added/modified; unrelated docs unchanged | Git/static |

### 6.3 Immutable constraints and approved decisions

- Use `com.baomidou:mybatis-plus-spring-boot3-starter:3.5.16`; do not directly declare native MyBatis Boot Starter, `mybatis`, `mybatis-spring`, or raw `mybatis-plus`.
- Use the same `3.5.16` for `mybatis-plus-jsqlparser`; do not use 3.5.17 APIs.
- BusinessID is boxed `Long`; zero, negative, positive, and null are legal. Do not add positive/range/null/sentinel validation.
- Default source is the configured MDC key `businessId`; absent MDC returns null, malformed nonempty text uses the normal `NumberFormatException` path, and a future custom Provider overrides the default Bean.
- Do not introduce a static or instance ThreadLocal BusinessID snapshot. Service normalization, Mapper guard, and TenantLine rewrite call the stateless Provider at their own execution points; the caller must keep its context stable throughout one synchronous operation/transaction.
- BusinessID is a tenant/shard scope, not a unique row identifier. Do not expose a public arbitrary-BusinessID Service overload or cross-scope bypass.
- Do not create or modify production schema/migrations, ShardingSphere topology, DataSource, SecurityContext integration, controller, frontend, or runtime service configuration.
- Every one of the 57 upstream signatures is visibly redeclared and explicitly overridden; shared helpers may reduce body duplication but cannot hide an inherited method.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | “Do not introduce native MyBatis” is enforced as no direct POM declaration; transitive native libraries brought by the official MyBatis-Plus Boot3 Starter remain necessary. | Primary Spec `EVD-015` explicitly distinguishes direct and transitive dependencies. | It implements the selected official Starter rather than silently rebuilding it. | A stricter no-transitive interpretation is technically incompatible with MyBatis-Plus and must return to the user. |
| `PLAN-CLAR-002` | Chain-wrapper methods resolve BusinessID at terminal Mapper execution, not at wrapper construction. | MyBatis-Plus chain wrappers are lazy; primary Spec §9 chain contracts were amended to preserve official behavior. | The SQL uses the current execution context and no hidden snapshot layer is added. | Resolving at construction would require a custom wrapper hierarchy and change upstream semantics. |
| `PLAN-CLAR-003` | Provider null is represented in TenantLine by JSqlParser `NullValue`; Java does not reject it and the consumer database decides insert nullability. | Primary Spec `REQ-007`,`REQ-008`,`REQ-010`, §10.7. | This is the exact “do not validate null” decision while retaining a tenant predicate node. | Dialect-specific null behavior remains an adoption test boundary. |

## 7. Ordered File-by-file Implementation Steps

> Every Step is independently verifiable and commit-sized. RED is observed before the corresponding production symbols are completed; only GREEN states are committed.

### Step 1 — Publish the compilable Starter and Boot import boundary

- Requirements: `REQ-001`, `REQ-003`, `REQ-017`, `REQ-019`, `REQ-021`, `REQ-023`
- Dependencies: `None`
- Baseline state: Common is a seven-module `pom` aggregator; Components parent manages Boot 3.5.16 but no MyBatis-Plus version; the target Artifact and import metadata are absent.
- Observable outcome: Maven resolves the new Jar, its direct dependency declarations use the approved MyBatis-Plus Boot3 artifacts, Boot discovers one disabled-capable auto-configuration without component scanning, and the structural focused test is GREEN.
- End state: The Artifact, base properties, base auto-configuration, and imports resource exist; BusinessID/plugin/Mapper/Service beans intentionally remain for Steps 2 through 5.
- Test-first gate: Required — after the Maven bootstrap files make test compilation possible, `EgonColaMybatisPlusAutoConfigurationTest` fails because the import, properties, and disabled boundary are missing; production files then make that selector GREEN.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/pom.xml`

- Purpose: Establish one 3.5.16 MyBatis-Plus version authority and managed coordinates.
- Symbols: `mybatis-plus.version`, dependency-management entries for `mybatis-plus-spring-boot3-starter` and `mybatis-plus-jsqlparser`.
- Repository evidence: The parent already centralizes Boot, database, and platform versions under `<properties>` and `<dependencyManagement>`.
- Dependencies and consumers: Consumed by the new child POM and every future BOM consumer; no runtime class is added here.
- Why now: The child Artifact cannot declare versionless approved dependencies until the parent manages them.
- Contract/signature changes: Adds Maven coordinates only; leaves Java/API and all existing dependency versions unchanged.
- Input/output and state mapping: Parent property `3.5.16` maps to both managed MP artifacts so an effective POM resolves one synchronized version.
- Error and edge behavior: Do not import an MP BOM or declare native MyBatis coordinates; an effective-POM version mismatch fails the Step gate.
- Implementation pseudocode:

```xml
<mybatis-plus.version>3.5.16</mybatis-plus.version>
<dependencyManagement><dependencies>
  <dependency><!-- com.baomidou:mybatis-plus-spring-boot3-starter:${mybatis-plus.version} --></dependency>
  <dependency><!-- com.baomidou:mybatis-plus-jsqlparser:${mybatis-plus.version} --></dependency>
</dependencies></dependencyManagement>
```

- Verification contribution: Effective-POM and dependency-tree gates prove one exact upstream version and no native direct declaration.
- After this file: Child modules can consume both official MP artifacts without local version literals.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/pom.xml`

- Purpose: Add the concrete Starter to the Common reactor.
- Symbols: New Maven child-module entry for `egon-cola-component-common-mybatis-plus-spring-boot-starter`.
- Repository evidence: The existing file is a pure `packaging=pom` aggregator with one child entry per concrete capability.
- Dependencies and consumers: Depends on File 3 existing; consumed by Common reactor commands and Components aggregation.
- Why now: Focused `-pl` and Common reactor builds need a discoverable child module.
- Contract/signature changes: Aggregation only; it does not add a dependency inherited by other Common children.
- Input/output and state mapping: Maven module path maps to the new Artifact while preserving the existing seven module order and behavior.
- Error and edge behavior: A typo or duplicate entry fails Maven model validation; do not turn the Common aggregator into a Jar or add runtime dependencies.
- Implementation pseudocode:

```text
modules:
  preserve every existing child in its current order
  append egon-cola-component-common-mybatis-plus-spring-boot-starter as one concrete child
```

- Verification contribution: The Common reactor resolves and reaches the new module.
- After this file: The module participates in Common builds without polluting sibling dependency graphs.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`

- Purpose: Define the opt-in Jar and its strict production/test dependency boundary.
- Symbols: Artifact ID, Jar packaging, configuration processor, approved MP dependencies, `slf4j-api`, Boot test, and H2 test scope.
- Repository evidence: The Common ID Starter POM uses the Common aggregator parent, Jar packaging, optional configuration processor, and module-local Boot tests.
- Dependencies and consumers: Parent is `egon-cola-component-common`; production code consumes MP Boot3/JSqlParser/SLF4J; test code consumes Boot test and H2.
- Why now: It is the compilation prerequisite for the RED auto-configuration test.
- Contract/signature changes: Publishes a new additive Artifact; no existing Artifact dependency changes.
- Input/output and state mapping: Managed version coordinates resolve into one Jar whose test dependencies cannot leak to consumers.
- Error and edge behavior: Direct declarations of `mybatis-spring-boot-starter`, `mybatis`, `mybatis-spring`, raw `mybatis-plus`, drivers, Flyway, ShardingSphere, IdP, or RBAC fail the boundary assertion.
- Implementation pseudocode:

```xml
<artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId>
<dependencies>
  <!-- production: MP Boot3 starter, MP jsqlparser, slf4j-api, optional configuration processor -->
  <!-- test only: spring-boot-starter-test and com.h2database:h2 -->
</dependencies>
```

- Verification contribution: `help:effective-pom`, source scan, and dependency tree establish the authorized edge.
- After this file: Maven can compile and test the otherwise empty new child module.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`

- Purpose: Define the first RED contract for import metadata, disabled behavior, defaults, and direct dependency declarations.
- Symbols: `bootMetadataRegistersAutoConfigurationWithoutComponentScanning`, `disabledConfigurationCreatesNoEgonColaBeans`, `bindsBootstrapDefaults`, `declaresOnlyApprovedDirectMybatisDependencies`.
- Repository evidence: `IdGeneratorAutoConfigurationTest` uses `ApplicationContextRunner`, `ImportCandidates`, AssertJ context assertions, and a no-`@ComponentScan` assertion.
- Dependencies and consumers: References Files 5 and 6; reads File 3 for the direct-dependency boundary; later Steps extend this same class with focused method groups.
- Why now: It fixes the public bootstrap contract before production auto-configuration exists.
- Contract/signature changes: Requires prefix `egon.cola.component.mybatis-plus`, default enabled true, disabled zero EgonCola beans, and exact imports metadata.
- Input/output and state mapping: Property values and classpath metadata map to context Bean presence/absence; POM XML maps to a set of allowed direct coordinates.
- Error and edge behavior: The RED failure must be missing target types/imports, not malformed test fixtures; disabled mode must not require DataSource or Provider.
- Implementation pseudocode:

```java
@Test void bootMetadataRegistersAutoConfigurationWithoutComponentScanning() {
    assertThat(ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()))
            .contains(EgonColaMybatisPlusAutoConfiguration.class.getName());
    assertThat(EgonColaMybatisPlusAutoConfiguration.class).isNotAnnotatedWith(ComponentScan.class);
}
@Test void disabledConfigurationCreatesNoEgonColaBeans() { contextRunner.withPropertyValues(PREFIX + ".enabled=false").run(context -> assertThat(context).doesNotHaveBean(EgonColaMybatisPlusProperties.class)); }
```

- Verification contribution: RED/GREEN selector for `REQ-003`, plus direct-POM evidence for `REQ-017`.
- After this file: Bootstrap behavior is executable as a test and initially fails for the intended missing implementation.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java`

- Purpose: Publish the complete typed configuration model early so later beans have one stable input.
- Symbols: `PREFIX`; nested `BusinessId`, `Pagination`, `Batch`, `BlockAttack`, `OptimisticLocker`, and `MetaFill` property groups with Spec defaults.
- Repository evidence: `IdGeneratorProperties` establishes JavaBean configuration binding and repository JavaDoc style.
- Dependencies and consumers: Bound by File 6; consumed by Provider, tenant handler, Service, plugins, handler, and validator in later Steps.
- Why now: The base auto-configuration and bootstrap test require a real properties type; later Steps must not invent scattered constants.
- Contract/signature changes: Adds additive configuration under `egon.cola.component.mybatis-plus`; `businessId.mdcKey` defaults to `businessId` and boxed BusinessID itself is not a property default.
- Input/output and state mapping: YAML/property scalar and collections map to the exact defaults/bounds in Spec §10.3.
- Error and edge behavior: JavaBean setters retain bound values; contract validation of blank names and numeric ranges occurs in Step 2, not via BusinessID null/range checks.
- Implementation pseudocode:

```java
@ConfigurationProperties(EgonColaMybatisPlusProperties.PREFIX)
public class EgonColaMybatisPlusProperties {
    public static final String PREFIX = "egon.cola.component.mybatis-plus";
    private boolean enabled = true; private final BusinessId businessId = new BusinessId();
    private final Pagination pagination = new Pagination(); private final Batch batch = new Batch();
    // nested beans expose the exact Spec defaults, including mdcKey=businessId and nullable-independent limits
}
```

- Verification contribution: Bootstrap defaults and later validation tests share this single type.
- After this file: All configuration names/defaults compile, while only the enabled boundary is wired.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`

- Purpose: Create the minimal Boot entry point and enabled condition before capability beans are added.
- Symbols: `@AutoConfiguration`, `@EnableConfigurationProperties`, `@ConditionalOnClass`, and `@ConditionalOnProperty` on `EgonColaMybatisPlusAutoConfiguration`.
- Repository evidence: Existing Common ID Starter uses the same annotations and a conditional default-on property.
- Dependencies and consumers: Loads File 5 and is named by File 7; later Steps add narrowly named `@Bean` methods.
- Why now: This is the minimum implementation that can satisfy the initial RED bootstrap test.
- Contract/signature changes: Adds one Boot auto-configuration activated only when MyBatis-Plus classes exist and `enabled` is not false.
- Input/output and state mapping: Classpath plus bound enabled flag decide whether the EgonCola configuration participates; no DataSource is created.
- Error and edge behavior: Disabled mode returns no EgonCola beans; missing MyBatis-Plus classes prevents activation rather than failing unrelated applications.
- Implementation pseudocode:

```java
@AutoConfiguration(afterName = "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@ConditionalOnClass(name = {"com.baomidou.mybatisplus.core.mapper.BaseMapper", "org.apache.ibatis.session.SqlSessionFactory"})
@ConditionalOnProperty(prefix = EgonColaMybatisPlusProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class EgonColaMybatisPlusAutoConfiguration { /* capability beans are added by Steps 2 and 3 */ }
```

- Verification contribution: Satisfies import/disabled/default property tests without pre-creating future beans.
- After this file: The bootstrap path is GREEN and intentionally exposes only configuration properties.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- Purpose: Register the auto-configuration through the Spring Boot 3 metadata mechanism.
- Symbols: Fully qualified `EgonColaMybatisPlusAutoConfiguration` class name on one line.
- Repository evidence: Every Common Boot Starter, including the ID Starter, uses this exact resource path.
- Dependencies and consumers: Boot `ImportCandidates` and packaged applications consume it; it depends on File 6.
- Why now: Class annotations alone do not make the auto-configuration discoverable.
- Contract/signature changes: Adds metadata only; no component scanning or legacy `spring.factories` entry.
- Input/output and state mapping: The one class-name line maps the Jar to one auto-configuration candidate.
- Error and edge behavior: Duplicate, misspelled, or extra class lines fail ImportCandidates and packaged-Jar checks.
- Implementation pseudocode:

```text
top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusAutoConfiguration
# keep exactly one candidate and do not add spring.factories or component scanning
# package inspection must find this resource in the production Jar
```

- Verification contribution: Makes the initial metadata test and final Jar gate observable.
- After this file: Step 1 has a complete, discoverable, disabled-capable compilation boundary.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaMybatisPlusAutoConfigurationTest test`
- Expected result: Exit 0; the structural auto-configuration tests pass, effective dependencies resolve at 3.5.16, and no other Common test regresses.
- Failure returns to: File 3 for Maven/dependency resolution, File 4 for fixture/assertion defects, File 5 for binding defaults, File 6 for activation, or File 7 for discovery.
- Completion criteria: Artifact resolution, import discovery, disabled zero-bean behavior, approved direct dependency set, and path-scoped `git diff --check` are all proven.
- Rollback: Revert only the three Step-owned POM edits/new module bootstrap paths; no schema or external state exists.
- Commit paths: `egon-cola-components/pom.xml`, `egon-cola-components/egon-cola-component-common/pom.xml`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/pom.xml`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusProperties.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Commit: `feat(common-mybatis-plus): bootstrap starter auto-configuration`

### Step 2 — Enforce nullable MDC BusinessID and the ordered plugin contract

- Requirements: `REQ-002`, `REQ-007`, `REQ-008`, `REQ-010`, `REQ-012`, `REQ-013`, `REQ-015`, `REQ-016`, `REQ-018`
- Dependencies: `Step 1`
- Baseline state: The module and properties bind, but no Provider, BusinessScoped contract, tenant expression, interceptor, fill handler, replacement validation, or capability Bean exists.
- Observable outcome: Default MDC and custom Providers, arbitrary Long/null behavior, property bounds, ordered InnerInterceptors, Clock-based fill, direct-Mapper normalization, and unsafe replacement failure are all GREEN in ContextRunner/unit tests.
- End state: BusinessID/plugin infrastructure is complete and public; Mapper/Injector and Service API remain intentionally absent.
- Test-first gate: Required — extend the existing auto-configuration test first; it fails on missing Provider/plugin/handler/validator classes and Bean order, then the following production files make it GREEN.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`

- Purpose: Add RED cases for nullable MDC behavior, custom Provider backoff, property validation, plugin ordering/replacement, MetaObjectHandler, and fixed Clock.
- Symbols: `createsDefaultMdcProvider`, `returnsNullForMissingMdc`, `acceptsAnyLong`, `propagatesMalformedMdcText`, `customProviderMakesDefaultBackOff`, `ordersInnerInterceptors`, `rejectsUnsafeOuterInterceptor`, `customHandlerMakesDefaultBackOff`, `fillsSupportedTimeTypes`.
- Repository evidence: The same class already owns bootstrap ContextRunner tests; ID Starter tests demonstrate custom-Bean backoff and failed-context root-cause assertions.
- Dependencies and consumers: Exercises Files 2 through 10 with lightweight context/user beans; later H2 tests prove actual SQL.
- Why now: It defines the Step 2 execution contract before the beans exist.
- Contract/signature changes: Requires boxed `Long`, MDC key configurability, no null/range rejection, order 100/200/300/400/500, and safe custom-bean semantics.
- Input/output and state mapping: MDC `"11"`, `"0"`, `"-7"`, absent, and malformed text map to Provider results/errors; Bean lists map to exact interceptor order.
- Error and edge behavior: Null succeeds; malformed nonempty text throws `NumberFormatException`; invalid technical properties and unsafe replacement chains fail startup with no raw BusinessID in messages.
- Implementation pseudocode:

```java
@Test void defaultProviderPreservesNullableLongContract() {
    contextRunner.run(context -> { var provider = context.getBean(EgonColaBusinessIdProvider.class); MDC.remove("businessId"); assertThat(provider.currentBusinessId()).isNull(); MDC.put("businessId", "-7"); assertThat(provider.currentBusinessId()).isEqualTo(-7L); });
}
@Test void ordersInnerInterceptorsAndRejectsUnsafeOuterReplacement() { /* assert guard, block, tenant, optimistic, page order; inject invalid outer list and assert configuration exception */ }
```

- Verification contribution: Focused RED/GREEN proof for `TEST-003` through `TEST-008`, `TEST-039` through `TEST-045` portions owned by auto-configuration.
- After this file: The missing behavior is explicit and fails for production-type/Bean absence, not environmental setup.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdProvider.java`

- Purpose: Define the only source Strategy seam used now by MDC and later by SecurityContext.
- Symbols: Public functional interface `EgonColaBusinessIdProvider` with `Long currentBusinessId()`.
- Repository evidence: Common modules use narrow behavioral interfaces rather than static holders; the Spec explicitly rejects a BaseEntity and ThreadLocal holder.
- Dependencies and consumers: Implemented by File 3 and test Providers; consumed by tenant handler, guard, meta handler, Service, and auto-configuration.
- Why now: Every BusinessID behavior needs a source contract before adapters and plugins compile.
- Contract/signature changes: Adds a nullable boxed-Long public API; it deliberately contains no validation/default/bypass method.
- Input/output and state mapping: Current caller context maps directly to `Long` or null at each invocation.
- Error and edge behavior: Implementor exceptions propagate unchanged; the interface does not translate, cache, or require non-null.
- Implementation pseudocode:

```java
@FunctionalInterface
public interface EgonColaBusinessIdProvider {
    Long currentBusinessId();
    // no set, clear, default tenant, arbitrary-id argument, validation, or ThreadLocal state
}
```

- Verification contribution: Custom Provider backoff and future source compatibility compile against this exact signature.
- After this file: Source variation is isolated with the smallest possible public contract.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcBusinessIdProvider.java`

- Purpose: Implement the current stateless MDC Adapter.
- Symbols: Constructor taking properties/business settings and `currentBusinessId()` implementation.
- Repository evidence: Common trace already depends on `slf4j-api`; the primary Spec fixes MDC as the temporary source and forbids an additional holder.
- Dependencies and consumers: Implements File 2; reads File 1 properties; default Bean is supplied by File 10.
- Why now: It is the user-selected current source and the GREEN implementation for Long/null parsing tests.
- Contract/signature changes: Reads configured key, returns null for absence, and calls `Long.valueOf` for any nonempty value.
- Input/output and state mapping: MDC missing maps to null; decimal Long text maps exactly; no trimming/default/range normalization is added unless the JDK conversion itself performs it.
- Error and edge behavior: Malformed nonempty text throws `NumberFormatException`; no null check, logging of raw content, cache, or MDC cleanup occurs.
- Implementation pseudocode:

```java
@Override public Long currentBusinessId() {
    String value = MDC.get(properties.getBusinessId().getMdcKey());
    if (value == null) return null;
    return Long.valueOf(value);
}
```

- Verification contribution: Proves the exact present-day source while `@ConditionalOnMissingBean` proves future replacement.
- After this file: MDC can supply every legal boxed Long/null without hidden state.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessScoped.java`

- Purpose: Give consumer persistence objects a compile-time boxed BusinessID contract without inheritance.
- Symbols: `Long getBusinessId()` and `void setBusinessId(Long businessId)`.
- Repository evidence: The Common restructure removed `BaseEntity`; interface composition preserves existing consumer parent classes.
- Dependencies and consumers: Generic bound for Mapper/Service in later Steps; used by guard/meta handler and test PO.
- Why now: Guard and handler need a type-safe target before Mapper/Service exist.
- Contract/signature changes: Adds one persistence behavior interface; it does not prescribe TableId, DTO shape, or DB nullability.
- Input/output and state mapping: Provider Long/null maps to the PO property and then consumer `business_id BIGINT` mapping.
- Error and edge behavior: Setter accepts all boxed values including null; implementation classes that use primitive accessors fail compile rather than being reflectively coerced.
- Implementation pseudocode:

```java
public interface EgonColaBusinessScoped {
    Long getBusinessId();
    void setBusinessId(Long businessId);
    // consumer PO owns every other persistence field and annotation
}
```

- Verification contribution: Compile fixtures and tests prove exact generic bounds and null support.
- After this file: Entity normalization can be direct and reflection-free for BusinessID.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdTenantLineHandler.java`

- Purpose: Adapt the Provider and configuration to MyBatis-Plus TenantLine SQL expressions.
- Symbols: `getTenantId`, `getTenantIdColumn`, `ignoreTable`, and constructor dependencies.
- Repository evidence: Primary Spec `EVD-011` selects ordered `InnerInterceptor` beans and TenantLine; ignored tables are explicit technical configuration.
- Dependencies and consumers: Uses Provider, properties, JSqlParser `LongValue`/`NullValue`; consumed by TenantLineInnerInterceptor Bean.
- Why now: Tenant SQL cannot be assembled or tested without this adapter.
- Contract/signature changes: Emits configured column and current Provider expression; table ignore comparison is case-insensitive exact matching.
- Input/output and state mapping: Provider null maps to `NullValue`; non-null maps to `LongValue`; configured ignored-table names bypass only those exact tables.
- Error and edge behavior: Does not skip tenant processing because the value is null; malformed Provider errors propagate before JDBC; blank column/table configuration is rejected by validator.
- Implementation pseudocode:

```java
@Override public Expression getTenantId() {
    Long businessId = provider.currentBusinessId();
    return businessId == null ? new NullValue() : new LongValue(businessId);
}
@Override public String getTenantIdColumn() { return properties.getBusinessId().getColumn(); }
@Override public boolean ignoreTable(String tableName) { return ignoredTables.stream().anyMatch(name -> name.equalsIgnoreCase(tableName)); }
```

- Verification contribution: Unit/context tests prove value/column/ignore policy; H2 tests later prove generated SQL.
- After this file: TenantLine can preserve a predicate for every Provider result, including null.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java`

- Purpose: Represent stable startup contract failures without leaking runtime SQL values.
- Symbols: Public runtime exception with message and message/cause constructors following nearby exception style.
- Repository evidence: Common defines named exceptions for durable platform failure categories; failed-context tests assert root-cause messages.
- Dependencies and consumers: Thrown by File 7 and technical property checks; never used for null BusinessID.
- Why now: Validator errors need a clear public failure type before validation is implemented.
- Contract/signature changes: Adds an additive configuration exception only; persistence exceptions remain Spring/MyBatis translations.
- Input/output and state mapping: Invalid bean topology/property name maps to a low-cardinality message naming the missing type/order/field.
- Error and edge behavior: Must not include raw BusinessID, entity, SQL parameter, credentials, or connection data.
- Implementation pseudocode:

```java
public class EgonColaMybatisPlusConfigurationException extends RuntimeException {
    public EgonColaMybatisPlusConfigurationException(String message) { super(message); }
    public EgonColaMybatisPlusConfigurationException(String message, Throwable cause) { super(message, cause); }
    // this type is never thrown merely because currentBusinessId() returned null
}
```

- Verification contribution: Gives failed-context assertions a stable root cause distinct from normal nullable BusinessID flow.
- After this file: Startup validation can fail explicitly and safely.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`

- Purpose: Fail fast when configuration bounds, custom outer chains, or replacement components silently remove required contracts.
- Symbols: Constructor dependencies, `afterSingletonsInstantiated`, `validateProperties`, `validateInterceptorChain`, and later Injector validation hook.
- Repository evidence: Official MP backs off its outer interceptor when a consumer defines one; primary Spec requires safe override rather than silent protection loss.
- Dependencies and consumers: Uses properties, `ObjectProvider<MybatisPlusInterceptor>`, known InnerInterceptor classes, and File 6; Step 3 completes Injector checking.
- Why now: Conditional plugin Beans alone cannot validate a consumer-supplied outer chain.
- Contract/signature changes: Adds startup-only validation; it does not intercept business requests or validate BusinessID values.
- Input/output and state mapping: Bound technical properties and actual interceptor list map to accepted context or a named configuration exception.
- Error and edge behavior: Reject blank identifiers, invalid page/batch ranges, missing guard/TenantLine, enabled plugin absence, duplicates, or wrong relative order; accept disabled optional plugins and safe exact chains.
- Implementation pseudocode:

```java
@Override public void afterSingletonsInstantiated() {
    validateTechnicalPropertyNamesAndBounds(properties);
    MybatisPlusInterceptor outer = outerProvider.getIfAvailable();
    if (outer != null) requireOrderedTypes(outer.getInterceptors(), requiredTypesForEnabledFeatures());
    // Step 3 adds the DefaultSqlInjector-preservation check using ObjectProvider<ISqlInjector>
}
```

- Verification contribution: ContextRunner proves safe customizations work and unsafe chains fail before any JDBC call.
- After this file: Bean replacement cannot silently discard the plugin contract.

#### File 8 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java`

- Purpose: Normalize BusinessID and fill configured audit-time fields from one injectable Clock.
- Symbols: `insertFill`, `updateFill`, time-type conversion helper, and BusinessID setter helper.
- Repository evidence: Reference implementation proves a MetaObjectHandler extension point; repository style prefers injectable time sources for deterministic behavior.
- Dependencies and consumers: Uses Provider, properties, `Clock`, MyBatis MetaObject, and MP strict fill APIs; registered conditionally by File 10.
- Why now: Insert/update metadata behavior is part of the plugin-level contract and needs fixed-Clock unit coverage before Service work.
- Contract/signature changes: Overwrites BusinessID with current nullable Provider value and fills only configured, present, appropriately annotated time fields.
- Input/output and state mapping: Clock instant/zone maps to Date, Instant, LocalDateTime, or OffsetDateTime; Provider Long/null maps to configured BusinessID property.
- Error and edge behavior: Unsupported time types are left unchanged or rejected per the exact Spec test contract; existing create values are preserved, update time refreshes, and null BusinessID is never rejected.
- Implementation pseudocode:

```java
@Override public void insertFill(MetaObject metaObject) {
    setFieldValByName(properties.getBusinessId().getProperty(), provider.currentBusinessId(), metaObject);
    fillCreateAndUpdateFields(metaObject, clock.instant(), properties.getMetaFill().getZone());
}
@Override public void updateFill(MetaObject metaObject) { setFieldValByName(properties.getBusinessId().getProperty(), provider.currentBusinessId(), metaObject); fillUpdateField(metaObject, clock.instant()); }
```

- Verification contribution: Fixed-Clock and existing/null value tests prove deterministic metadata semantics.
- After this file: Consumer PO inserts/updates can receive normalized BusinessID and audit fields without a base entity.

#### File 9 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaBusinessIdGuardInnerInterceptor.java`

- Purpose: Normalize direct Mapper parameters/entities and reject deliberate tenant-column mutation before JDBC.
- Symbols: `beforeQuery`, `beforeUpdate`, parameter-map/entity normalization helpers, and update-column inspection.
- Repository evidence: TenantLine alone scopes SQL but cannot make caller-supplied custom parameters authoritative or prevent Wrapper SET of the tenant column.
- Dependencies and consumers: Uses Provider, properties, `EgonColaBusinessScoped`, MyBatis parameter structures/BoundSql; ordered first by File 10.
- Why now: Direct Mapper paths must be protected before Mapper custom methods are published.
- Contract/signature changes: Replaces explicit `businessId` and entity property with the current Provider result at execution time; no mismatch/null/range rejection is added.
- Input/output and state mapping: Parameter map key `businessId`, nested `et` entity, or direct scoped entity maps to current Long/null; Wrapper SET targeting configured column maps to a pre-JDBC exception.
- Error and edge behavior: Unsupported parameter shapes are not silently mutated; tenant-column update and unsafe SQL parse paths fail closed, while missing Provider value continues as null.
- Implementation pseudocode:

```java
private void normalize(Object parameter) {
    Long current = provider.currentBusinessId();
    if (parameter instanceof EgonColaBusinessScoped scoped) scoped.setBusinessId(current);
    if (parameter instanceof Map<?, ?> map) overwriteBusinessIdAndScopedEntities(map, current);
    rejectUpdateAssignmentTo(properties.getBusinessId().getColumn(), parameter);
}
```

- Verification contribution: Unit/context assertions prove overwrite and mutation rejection; Step 6 proves actual SQL behavior.
- After this file: Direct Mapper execution has an early normalization/immutability guard.

#### File 10 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`

- Purpose: Wire the default Provider, tenant adapter, five ordered InnerInterceptors, Clock, MetaObjectHandler, and contract validator with safe backoff.
- Symbols: Bean methods for `EgonColaBusinessIdProvider`, `Clock`, tenant handler, guard, BlockAttack, TenantLine, optimistic locker, pagination, MetaObjectHandler, and validator.
- Repository evidence: Existing Starter beans use `@ConditionalOnMissingBean`; official 3.5.16 MP auto-config collects ordered `InnerInterceptor` Beans into one outer interceptor.
- Dependencies and consumers: Creates Files 3, 5, 7, 8, and 9; uses properties; consumed by official MP auto-configuration and applications.
- Why now: All Step 2 component types and RED expectations are available.
- Contract/signature changes: Adds default beans at order 100/200/300/400/500; feature flags control optional beans but not global guard/tenant behavior.
- Input/output and state mapping: Properties choose optional plugin presence/limits; custom Provider/Clock/MetaObjectHandler beans cause default backoff; official outer composition consumes ordered inner beans.
- Error and edge behavior: A consumer outer interceptor causes official backoff and is then validated; disabled global configuration creates none; null Provider result is ordinary data, not startup failure.
- Implementation pseudocode:

```java
@Bean @ConditionalOnMissingBean(EgonColaBusinessIdProvider.class) EgonColaBusinessIdProvider businessIdProvider(...) { return new EgonColaMdcBusinessIdProvider(properties); }
@Bean @Order(100) EgonColaBusinessIdGuardInnerInterceptor businessIdGuard(...) { return new EgonColaBusinessIdGuardInnerInterceptor(provider, properties); }
@Bean @Order(200) @ConditionalOnProperty(/* block enabled */) BlockAttackInnerInterceptor blockAttack() { return new BlockAttackInnerInterceptor(); }
// order 300 TenantLine, 400 OptimisticLocker, 500 Pagination; conditional Clock/MetaObjectHandler; validator last
```

- Verification contribution: Turns all Step 2 context tests GREEN and establishes the ordered runtime chain.
- After this file: Nullable BusinessID, plugin ordering, fill, and override contracts are fully wired without Mapper/Service APIs.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaMybatisPlusAutoConfigurationTest test`
- Expected result: Exit 0; default/custom Provider, Long/null/malformed MDC, technical property bounds, exact plugin order, handler/Clock backoff, and unsafe outer-chain tests pass.
- Failure returns to: File 1 for assertion/fixture errors, Files 2-5 for BusinessID contracts, File 7 for topology checks, Files 8-9 for runtime helpers, or File 10 for conditions/order.
- Completion criteria: All Step 2 test cases are GREEN, no Bean stores BusinessID state, null is not rejected, and `git diff --check` passes.
- Rollback: Revert the Step 2 Java/test changes only; Step 1 remains a harmless empty-capability Starter bootstrap.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaMdcBusinessIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessScoped.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/business/EgonColaBusinessIdTenantLineHandler.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/exception/EgonColaMybatisPlusConfigurationException.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/handler/EgonColaMetaObjectHandler.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/interceptor/EgonColaBusinessIdGuardInnerInterceptor.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`
- Commit: `feat(common-mybatis-plus): add business id plugin infrastructure`

### Step 3 — Inject the complete EgonCola Mapper statement set

- Requirements: `REQ-002`, `REQ-004`, `REQ-009`, `REQ-010`, `REQ-014`, `REQ-016`, `REQ-017`, `REQ-018`
- Dependencies: `Step 2`
- Baseline state: BusinessID and tenant plugins exist, but consumers only have upstream `BaseMapper`; no EgonCola Mapper, custom MappedStatement, default Injector, or replacement validation exists.
- Observable outcome: MyBatis registers every default `BaseMapper` method plus exactly three EgonCola methods whose SQL uses bound BusinessID/TableId parameters and logic-delete fragments.
- End state: Mapper/Injector contracts and safe customization are complete; Service types remain absent until Step 4.
- Test-first gate: Required — add Injector/statement assertions to the auto-configuration test before creating Mapper/Injector types; RED is missing types/statements, GREEN is exact default-plus-three registration.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`

- Purpose: Add RED contract tests for default Injector registration, all default methods, exactly three custom IDs, safe subclass acceptance, and unrelated Injector rejection.
- Symbols: `registersDefaultAndEgonColaMappedStatements`, `customEgonColaInjectorSubclassIsAccepted`, `unrelatedInjectorFailsFast`, and SQL-template bound-parameter assertions.
- Repository evidence: This test already owns Bean replacement and startup-contract assertions; MyBatis Configuration can be constructed without an external database for statement registration.
- Dependencies and consumers: References Files 2 through 6 and the modified validator/auto-configuration Files 7-8.
- Why now: It fixes Injector behavior before implementation and prevents a custom Injector from silently removing default/custom methods.
- Contract/signature changes: Requires method IDs `selectListByBusinessId`, `selectCountByBusinessId`, `selectByBusinessIdAndId`, all upstream defaults, `#{}` bindings, and logic-delete conditions.
- Input/output and state mapping: Test entity TableInfo plus Injector maps to MappedStatement IDs/SQL; custom Bean class maps to accepted or failed context.
- Error and edge behavior: Duplicate IDs, `${}` substitution, lost default methods, or unrelated Injector replacement fail the test/startup before JDBC.
- Implementation pseudocode:

```java
@Test void registersDefaultAndEgonColaMappedStatements() {
    List<AbstractMethod> methods = injector.getMethodList(configuration, TestMapper.class, tableInfo);
    assertThat(methods).extracting(AbstractMethod::getMethod).contains(/* every default */, "selectListByBusinessId", "selectCountByBusinessId", "selectByBusinessIdAndId");
    assertThat(renderedSql).contains("#{businessId}", "#{id}").doesNotContain("${businessId}");
}
```

- Verification contribution: Supplies RED/GREEN proof for `TEST-037`, `TEST-038`, and `TEST-045` Injector branch.
- After this file: Injector API, SQL safety, and replacement expectations are executable and initially fail for missing production types.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java`

- Purpose: Publish the consumer Mapper interface with boxed BusinessID and multi-row-safe operations.
- Symbols: `EgonColaMapper<T extends EgonColaBusinessScoped>` plus three `@Param` methods.
- Repository evidence: MyBatis-Plus extends behavior from `BaseMapper`; the reference Mapper confirms the injection shape but its String/single-row contract is rejected.
- Dependencies and consumers: Extends `BaseMapper<T>`; implemented by MyBatis proxy; consumed by Service and consumer Mapper subinterfaces.
- Why now: AbstractMethods need exact method names/signatures before statement IDs are appended.
- Contract/signature changes: Adds list, boxed `Long` count result, and BusinessID-plus-Serializable-id query; does not expose an unscoped write/bypass.
- Input/output and state mapping: Guard overwrites explicit BusinessID with Provider value; Injector binds it and optional TableId; result maps through normal MyBatis entity mapping.
- Error and edge behavior: Null BusinessID remains bound; exact query returns null for no row and standard MyBatis multiple-row behavior otherwise; Spring/MyBatis errors propagate.
- Implementation pseudocode:

```java
public interface EgonColaMapper<T extends EgonColaBusinessScoped> extends BaseMapper<T> {
    List<T> selectListByBusinessId(@Param("businessId") Long businessId);
    Long selectCountByBusinessId(@Param("businessId") Long businessId);
    T selectByBusinessIdAndId(@Param("businessId") Long businessId, @Param("id") Serializable id);
}
```

- Verification contribution: Reflection/compile tests and mapped statement names derive from these exact methods.
- After this file: Consumers have a type-safe Mapper API but statements still require the Injector.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectListByBusinessId.java`

- Purpose: Inject the current-scope multi-row query while preserving table metadata and logic delete.
- Symbols: AbstractMethod subclass and `injectMappedStatement`.
- Repository evidence: MyBatis-Plus AbstractMethod/TableInfo APIs generate safe default statements; reference code demonstrates `DefaultSqlInjector` extension but assumes uniqueness.
- Dependencies and consumers: Bound to `EgonColaMapper.selectListByBusinessId`; appended by File 6.
- Why now: It is the first custom statement and establishes the safe SQL assembly pattern for the next two.
- Contract/signature changes: Generates `SELECT <columns> FROM <table> WHERE <business_column> = #{businessId} <logic-delete>` returning a list.
- Input/output and state mapping: TableInfo supplies table/column/resultMap; bound Long/null supplies the parameter; database rows map to `List<T>`.
- Error and edge behavior: Never interpolates BusinessID; unsupported metadata fails statement injection rather than dropping tenant/logic-delete conditions.
- Implementation pseudocode:

```java
String sql = "SELECT " + sqlSelectColumns(tableInfo) + " FROM " + tableInfo.getTableName()
        + " WHERE " + configuredBusinessColumn + " = #{businessId} " + tableInfo.getLogicDeleteSql(true, true);
SqlSource source = languageDriver.createSqlSource(configuration, sql, modelClass);
return addSelectMappedStatementForTable(mapperClass, methodName, source, tableInfo);
```

- Verification contribution: SQL-template assertion proves multi-row result, bound value, and logic delete.
- After this file: The list statement can be appended without copying default statements.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectCountByBusinessId.java`

- Purpose: Inject a database-side count for the current BusinessID scope.
- Symbols: AbstractMethod subclass and count MappedStatement creation.
- Repository evidence: Fetching current rows then counting is explicitly rejected by the simplicity audit; MyBatis-Plus has count statement helpers/result types.
- Dependencies and consumers: Bound to `EgonColaMapper.selectCountByBusinessId`; appended by File 6.
- Why now: Service count must avoid fetch-then-forward and share the same logic-delete condition as list.
- Contract/signature changes: Generates `SELECT COUNT(*)` over the BusinessID and logic-delete predicates with `Long` result mapping.
- Input/output and state mapping: Bound current Long/null maps to one database aggregate; JDBC numeric output maps to boxed Long and Service returns primitive long after null-safe normalization.
- Error and edge behavior: Zero matching rows returns zero; SQL/JDBC mapping failures propagate; no in-memory row materialization occurs.
- Implementation pseudocode:

```java
String sql = "SELECT COUNT(*) FROM " + tableInfo.getTableName()
        + " WHERE " + configuredBusinessColumn + " = #{businessId} " + tableInfo.getLogicDeleteSql(true, true);
SqlSource source = languageDriver.createSqlSource(configuration, sql, modelClass);
return addSelectMappedStatementForOther(mapperClass, methodName, source, Long.class);
```

- Verification contribution: Statement inspection and Step 6 H2 count prove aggregate correctness.
- After this file: Count behavior is database-native and tenant/logic-delete aligned.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectByBusinessIdAndId.java`

- Purpose: Inject an exact query using current BusinessID and consumer TableId.
- Symbols: AbstractMethod subclass using TableInfo key column/property and result mapping.
- Repository evidence: BusinessID is non-unique; TableId metadata is the only repository-consistent exact-row discriminator.
- Dependencies and consumers: Bound to `EgonColaMapper.selectByBusinessIdAndId`; appended by File 6.
- Why now: Custom exact/Optional Service methods need an unambiguous statement before API publication.
- Contract/signature changes: Generates a bound composite predicate and logic-delete fragment; retains consumer primary-key type through Serializable input.
- Input/output and state mapping: TableInfo key column and `#{id}` combine with current `#{businessId}`; zero rows maps to null.
- Error and edge behavior: Missing TableId metadata fails injection with configuration context; null id is rejected by Service before Mapper; no BusinessID uniqueness assumption.
- Implementation pseudocode:

```java
requireTableId(tableInfo);
String sql = "SELECT " + sqlSelectColumns(tableInfo) + " FROM " + tableInfo.getTableName()
        + " WHERE " + configuredBusinessColumn + " = #{businessId} AND " + tableInfo.getKeyColumn() + " = #{id} "
        + tableInfo.getLogicDeleteSql(true, true);
return addSelectMappedStatementForTable(mapperClass, methodName, languageDriver.createSqlSource(configuration, sql, modelClass), tableInfo);
```

- Verification contribution: Statement/H2 exact query proves BusinessID-plus-TableId semantics and logic delete.
- After this file: The three custom Mapper behaviors are individually implementable and inspectable.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/EgonColaSqlInjector.java`

- Purpose: Preserve the official method list and append exactly the three EgonCola AbstractMethods.
- Symbols: `EgonColaSqlInjector extends DefaultSqlInjector` and `getMethodList` override.
- Repository evidence: Primary Spec and reference source both require extending `DefaultSqlInjector`; rebuilding defaults would drift across versions.
- Dependencies and consumers: Instantiates Files 3-5 with configured column; registered by File 8; validated by File 7.
- Why now: All Mapper signatures and AbstractMethods exist, so composition can be exact.
- Contract/signature changes: Adds three statements after `super.getMethodList`; no default ID is replaced.
- Input/output and state mapping: MyBatis Configuration/Mapper/TableInfo input maps to a fresh list containing upstream defaults followed by three stable custom methods.
- Error and edge behavior: Never mutates an immutable `super` list in place; duplicate custom IDs are prevented by one append path; metadata errors remain startup failures.
- Implementation pseudocode:

```java
@Override public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
    List<AbstractMethod> methods = new ArrayList<>(super.getMethodList(configuration, mapperClass, tableInfo));
    methods.add(new EgonColaSelectListByBusinessId(column)); methods.add(new EgonColaSelectCountByBusinessId(column));
    methods.add(new EgonColaSelectByBusinessIdAndId(column)); return methods;
}
```

- Verification contribution: Exact method-set and no-default-loss assertions turn GREEN.
- After this file: A default Injector can publish every official and EgonCola Mapper statement.

#### File 7 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`

- Purpose: Complete replacement validation for SQL Injector beans.
- Symbols: `ObjectProvider<ISqlInjector>` dependency and `validateSqlInjector` branch.
- Repository evidence: `@ConditionalOnMissingBean` permits consumer replacement, but the Spec requires preservation of EgonCola statements.
- Dependencies and consumers: Recognizes File 6 or a subclass as safe; called during the existing startup validation lifecycle.
- Why now: Injector validation can only be implemented after its safe base type exists.
- Contract/signature changes: A non-EgonCola Injector in an enabled Starter context becomes a clear startup configuration failure.
- Input/output and state mapping: Actual Injector runtime type maps to accepted default/subclass or a non-secret configuration exception.
- Error and edge behavior: Multiple/ambiguous Injectors remain normal Spring ambiguity failures; disabled Starter does not validate unrelated MyBatis configuration.
- Implementation pseudocode:

```java
private void validateSqlInjector() {
    ISqlInjector injector = injectorProvider.getIfAvailable();
    if (injector != null && !(injector instanceof EgonColaSqlInjector))
        throw new EgonColaMybatisPlusConfigurationException("enabled EgonCola MyBatis-Plus requires an EgonColaSqlInjector or subclass");
}
```

- Verification contribution: Safe subclass and unsafe unrelated replacement ContextRunner cases become GREEN.
- After this file: Injector override behavior cannot silently remove custom/default contracts.

#### File 8 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`

- Purpose: Register the default Injector only when the consumer has not supplied one.
- Symbols: `egonColaSqlInjector(EgonColaMybatisPlusProperties)` Bean method.
- Repository evidence: Existing auto-config Beans use conditional backoff; MyBatis-Plus discovers a single `ISqlInjector` Bean.
- Dependencies and consumers: Creates File 6 with configured business column; validated by File 7 and consumed by Mapper scanning.
- Why now: The complete and tested Injector type exists.
- Contract/signature changes: Adds one conditional `ISqlInjector` Bean; custom safe subclasses retain precedence.
- Input/output and state mapping: Configured business column maps into all three injected SQL method instances.
- Error and edge behavior: Invalid column is caught by existing technical property validation; unrelated custom Injector causes fail-fast rather than default Bean competition.
- Implementation pseudocode:

```java
@Bean
@ConditionalOnMissingBean(ISqlInjector.class)
public EgonColaSqlInjector egonColaSqlInjector(EgonColaMybatisPlusProperties properties) {
    return new EgonColaSqlInjector(properties.getBusinessId().getColumn());
}
```

- Verification contribution: ContextRunner now sees one default Injector and mapped statement tests can build the complete Configuration.
- After this file: Step 3 publishes a safe default-plus-custom Mapper method set.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaMybatisPlusAutoConfigurationTest test`
- Expected result: Exit 0; all default statements and exactly three custom IDs are present, SQL uses bound parameters/logic delete, and safe/unsafe Injector replacement cases pass.
- Failure returns to: File 1 for test fixture/rendering, File 2 for signatures, Files 3-5 for SQL/result mapping, File 6 for list composition, or Files 7-8 for validation/wiring.
- Completion criteria: Mapper API compiles, Injector preserves upstream defaults, no `${}` BusinessID substitution exists, and focused/context tests plus `git diff --check` pass.
- Rollback: Revert only the Step 3 Mapper/Injector and two wiring/test modifications; Step 2 remains complete.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfigurationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaMapper.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectListByBusinessId.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectCountByBusinessId.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/method/EgonColaSelectByBusinessIdAndId.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/injector/EgonColaSqlInjector.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusContractValidator.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/autoconfigure/EgonColaMybatisPlusAutoConfiguration.java`
- Commit: `feat(common-mybatis-plus): inject scoped mapper statements`

### Step 4 — Freeze all 57 IService signatures in EgonCola APIs

- Requirements: `REQ-002`, `REQ-005`, `REQ-006`, `REQ-009`, `REQ-018`, `REQ-019`
- Dependencies: `Step 3`
- Baseline state: Mapper and injected SQL are available, but no EgonCola Service contract/implementation exists and upstream 3.5.16 method drift is unchecked.
- Observable outcome: Reflection proves `EgonColaIService` redeclares and `EgonColaServiceImpl` explicitly overrides exactly the 57 visible upstream methods, while four custom Service methods compile against EgonCola Mapper.
- End state: Public Service signatures are frozen with compile-safe explicit bodies; Step 5 replaces minimal delegations with the full enhanced family behavior.
- Test-first gate: Required — create the reflection parity test first; it fails because both Service types/signatures are absent, then exact declarations/overrides make it GREEN.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java`

- Purpose: Turn the user’s “rewrite every IService method” requirement and 3.5.16 freeze into an executable API gate.
- Symbols: `redeclaresEveryVisibleIServiceMethod`, `implementationOverridesEveryVisibleIServiceMethod`, `addsOnlyFourDocumentedServiceMethods`, `usesMybatisPlus3516Baseline`.
- Repository evidence: Primary Spec `EVD-010` inventories 4 direct IService plus 53 IRepository methods; repository tests use reflection for stable contract checks.
- Dependencies and consumers: Reflects upstream `IService`, Files 2-3, and resolved package implementation version; it does not start Spring/MyBatis.
- Why now: Public signatures must be exact before enhanced bodies are implemented.
- Contract/signature changes: Defines erased/generic signature comparison including return type, parameters, varargs, annotations relevant to override, and method count.
- Input/output and state mapping: Upstream non-static visible method descriptors map one-to-one to declared methods in interface and class; four named custom descriptors form the only additions.
- Error and edge behavior: A missing declaration, inherited-only implementation, 3.5.17 drift, return-type mismatch, or extra arbitrary-BusinessID method fails with a method-level diff.
- Implementation pseudocode:

```java
@Test void redeclaresAndOverridesEveryVisibleIServiceMethod() {
    Set<MethodKey> upstream = visibleNonStaticMethods(IService.class);
    assertThat(declaredKeys(EgonColaIService.class)).containsAll(upstream);
    assertThat(declaredKeys(EgonColaServiceImpl.class)).containsAll(upstream);
    assertThat(customKeys(EgonColaIService.class, upstream)).containsExactlyInAnyOrder(theFourSpecMethods());
}
```

- Verification contribution: Direct proof for `TEST-009` through `TEST-011`, independent of later behavior tests.
- After this file: The exact API gap is machine-readable and initially RED.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java`

- Purpose: Publish the complete explicit Service contract over boxed-BusinessScoped entities.
- Symbols: `EgonColaIService<T extends EgonColaBusinessScoped> extends IService<T>`, 57 `@Override` declarations from `INTERNAL-001` through `INTERNAL-057`, and four custom methods `INTERNAL-058` through `INTERNAL-061`.
- Repository evidence: The primary Spec §9 lists every exact signature and return shape; 3.5.17 is excluded because it removes this API.
- Dependencies and consumers: Extends MP `IService<T>`; implemented by File 3; consumed by concrete business Service interfaces/classes.
- Why now: Interface publication follows Mapper completeness and precedes all enhanced behavior.
- Contract/signature changes: Repeats all upstream signatures exactly and adds only current-scope list/count/exact/optional operations with no arbitrary BusinessID argument.
- Input/output and state mapping: Upstream inputs/results remain shape-compatible; BusinessID is derived from Provider in implementation; id stays caller-owned Serializable.
- Error and edge behavior: Interface adds no default bypass or null validation; detailed behavior/errors are implemented in Step 5 and remain Spring/MyBatis compatible.
- Implementation pseudocode:

```java
public interface EgonColaIService<T extends EgonColaBusinessScoped> extends IService<T> {
    // declare every exact signature enumerated by INTERNAL-001..INTERNAL-057: save/remove/update/get/list/page/count/map/obj/batch/chain/getBaseMapper/getEntityClass
    @Override boolean save(T entity); @Override boolean saveBatch(Collection<T> entities, int batchSize);
    // continue with the full reflection-derived 3.5.16 set, without inherited-only omissions
    List<T> listByCurrentBusinessId(); long countByCurrentBusinessId();
    T getByCurrentBusinessIdAndId(Serializable id); Optional<T> getOptByCurrentBusinessIdAndId(Serializable id);
}
```

- Verification contribution: Interface half of parity and consumer compile fixture become GREEN.
- After this file: The public Service API is complete, explicit, and version-frozen.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`

- Purpose: Provide a compile-safe implementation class with every upstream method explicitly declared before behavior hardening.
- Symbols: `EgonColaServiceImpl<M extends EgonColaMapper<T>, T extends EgonColaBusinessScoped> extends ServiceImpl<M,T> implements EgonColaIService<T>`, protected constructor `(M,EgonColaBusinessIdProvider,EgonColaMybatisPlusProperties)`, 57 overrides, four custom methods.
- Repository evidence: User requested the ServiceImpl extension shape; MyBatis-Plus `ServiceImpl` is the 3.5.16 framework base and Mapper type is complete from Step 3.
- Dependencies and consumers: Uses Mapper, Provider, and properties; extended by business/test Services; Step 5 modifies method bodies/private helpers only.
- Why now: Reflection requires actual declared class methods and later behavior needs one stable implementation surface.
- Contract/signature changes: Explicitly overrides every upstream signature and implements the four additions; no method is satisfied only through inheritance.
- Input/output and state mapping: The protected constructor assigns the inherited `baseMapper` and immutable Provider/properties collaborators for consumer constructor injection; minimal bodies delegate to exact upstream/Mapper behavior while preserving return shapes.
- Error and edge behavior: Compile-safe delegations propagate upstream errors; no new unsupported exception translation is introduced; Step 5 owns enhanced argument/transaction behavior.
- Implementation pseudocode:

```java
public class EgonColaServiceImpl<M extends EgonColaMapper<T>, T extends EgonColaBusinessScoped>
        extends ServiceImpl<M, T> implements EgonColaIService<T> {
    protected final EgonColaBusinessIdProvider businessIdProvider; protected final EgonColaMybatisPlusProperties properties;
    protected EgonColaServiceImpl(M mapper, EgonColaBusinessIdProvider provider, EgonColaMybatisPlusProperties properties) { this.baseMapper = Objects.requireNonNull(mapper); this.businessIdProvider = Objects.requireNonNull(provider); this.properties = Objects.requireNonNull(properties); }
    @Override public boolean save(T entity) { return super.save(entity); }
    // explicitly declare/delegate every INTERNAL-002..INTERNAL-057 signature; do not rely on inherited-only methods
    @Override public List<T> listByCurrentBusinessId() { return getBaseMapper().selectListByBusinessId(businessIdProvider.currentBusinessId()); }
    // implement count/exact/optional with the three EgonCola Mapper methods
}
```

- Verification contribution: Implementation parity becomes GREEN and fixes the exact body locations Step 5 must enhance.
- After this file: API parity is complete; runtime enhancements are the only intentional remaining Service work.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaIServiceParityTest test`
- Expected result: Exit 0; exactly 57 upstream signatures are redeclared and explicitly overridden, exactly four custom methods exist, and resolved MP implementation version is 3.5.16.
- Failure returns to: File 1 for descriptor normalization, File 2 for interface signatures, or File 3 for missing/incorrect explicit overrides.
- Completion criteria: Reflection parity, test compilation, Javadocs for public classes/method families, and `git diff --check` pass with no arbitrary-BusinessID Service API.
- Rollback: Revert the three Step 4 files; Mapper/plugin infrastructure remains usable independently.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/contract/EgonColaIServiceParityTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaIService.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`
- Commit: `feat(common-mybatis-plus): freeze enhanced service api parity`

### Step 5 — Implement every enhanced Service method family

- Requirements: `REQ-005`, `REQ-006`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-018`
- Dependencies: `Step 4`
- Baseline state: All 57 overrides and four custom methods compile, but most upstream bodies are minimal parity delegations and do not yet enforce the full normalization, size, transaction, wide-write, page, and result contracts.
- Observable outcome: Every explicit override belongs to a tested method family and applies the Spec enhancement while retaining the exact upstream signature/return shape.
- End state: Unit-level Service behavior is complete; only real MyBatis/JDBC integration proof and evidence-driven corrections remain for Step 6.
- Test-first gate: Required — create fake-Mapper/spy-based family tests before replacing delegations; RED observes unnormalized entities, unchecked inputs, wrong default sizes, missing transaction metadata, unsafe wide writes, or incorrect custom delegation.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java`

- Purpose: Prove fast deterministic semantics for all method families without hiding signature coverage behind integration tests.
- Symbols: Parameterized/nested test groups `SaveFamily`, `RemoveFamily`, `UpdateFamily`, `ReadFamily`, `ProjectionFamily`, `PageFamily`, `BatchFamily`, `ChainFamily`, `MetadataFamily`, and `CustomBusinessIdFamily`.
- Repository evidence: Module-local JUnit/AssertJ tests are the repository norm; parity test already proves the exact 57 method set, so behavior tests can be family-based with named representative and edge paths.
- Dependencies and consumers: Uses a recording `EgonColaMapper`, configurable Provider, properties, and a concrete test subclass exposing mapper/entity metadata.
- Why now: It defines enhancement outcomes before modifying the 57 bodies.
- Contract/signature changes: Requires Provider overwrite for entity/custom parameters, non-null id where specified, configured batch/page limits, one Service-entry value for batch entities, wide-write rejection, standard empty/result shapes, transaction annotations, and lazy chain behavior.
- Input/output and state mapping: Arbitrary/null entity BusinessIDs, collections, wrappers, pages, projections, ids, and Mapper results map to normalized calls and exact upstream-compatible outputs.
- Error and edge behavior: Null collection/invalid size/id/page/wide write fail before Mapper; empty collection returns the specified stable false/empty shape; Mapper failures propagate; null current BusinessID is accepted.
- Implementation pseudocode:

```java
@ParameterizedTest @MethodSource("writeFamilyCases") void normalizesAndDelegatesEveryWriteFamily(ServiceCall call) {
    provider.set(null); entity.setBusinessId(99L); call.invoke(service, entity);
    assertThat(entity.getBusinessId()).isNull(); assertThat(recordingMapper.calls()).containsExactly(call.expectedMapperCall());
}
@Test void batchUsesOneServiceEntryValueAndBoundsBeforeMapper() { /* Provider changes on a later read; assert all entities received first value, valid chunks delegate, oversize makes zero calls */ }
@Test void parityMethodsAreAssignedToOneBehaviorFamily() { assertThat(allParityMethodKeys()).isEqualTo(unionOfAllFamilyMethodKeys()); }
```

- Verification contribution: Covers the complete method-family map and edge contracts of `INTERNAL-001` through `INTERNAL-061` while parity retains per-signature coverage.
- After this file: The minimal Step 4 delegations fail in the exact enhancement branches that Step 5 must implement.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`

- Purpose: Replace all minimal delegations with complete, explicit, repository-debuggable enhanced method bodies.
- Symbols: All 57 overrides/four custom methods plus private/protected helpers `currentBusinessId`, `normalizeEntity`, `normalizeBatch`, `requireId`, `requireWriteWrapper`, `validateBatch`, `validatePage`, and family-specific delegation helpers.
- Repository evidence: Primary Spec §9 provides a detailed contract for every method; AGENTS rules permit helpers only for real duplication and require each override to remain visible.
- Dependencies and consumers: Uses Provider/properties/Mapper and upstream ServiceImpl/chain wrappers; concrete consumer/test Services extend it.
- Why now: API parity and behavior tests are both fixed, so implementation can be complete without signature redesign.
- Contract/signature changes: No signature changes from Step 4; bodies add normalization/guards/transactions and preserve official result types.
- Input/output and state mapping: The implementation matrix is: save/update entity families overwrite current Long/null; batch validates once then overwrites all with one Service-entry value; remove/update wrapper families reject null/empty business predicates and tenant-column SET; reads delegate with TenantLine scoping; page methods validate size; projection/map/obj preserve converter/result shape; chain factories stay lazy and terminal SQL resolves context; metadata accessors preserve upstream values; custom methods derive BusinessID then call the three Mapper methods.
- Error and edge behavior: `IllegalArgumentException` handles invalid caller arguments/limits before Mapper; configuration errors remain Step 2 exception; persistence errors remain translated; all write/batch methods use Spring rollback semantics; null BusinessID is ordinary and never a validation branch.
- Implementation pseudocode:

```java
@Transactional(rollbackFor = Exception.class)
@Override public boolean save(T entity) { requireNonNull(entity, "entity"); normalizeEntity(entity, currentBusinessId()); return super.save(entity); }
@Transactional(rollbackFor = Exception.class)
@Override public boolean saveBatch(Collection<T> entities, int batchSize) { validateBatch(entities, batchSize); Long id = currentBusinessId(); entities.forEach(entity -> normalizeEntity(entity, id)); return super.saveBatch(entities, batchSize); }
@Override public <E extends IPage<T>> E page(E page, Wrapper<T> wrapper) { validatePage(page); return super.page(page, wrapper); }
@Override public List<T> listByCurrentBusinessId() { return getBaseMapper().selectListByBusinessId(currentBusinessId()); }
@Override public long countByCurrentBusinessId() { Long value = getBaseMapper().selectCountByBusinessId(currentBusinessId()); return value == null ? 0L : value; }
// explicitly implement every remaining Spec method through the named family helper while preserving its exact return type and annotations
```

- Verification contribution: Turns all Service family tests GREEN while the Step 4 parity test proves no explicit override was lost during refactoring.
- After this file: Every upstream and custom Service method has both per-signature and per-family executable proof.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaIServiceParityTest,EgonColaServiceImplTest test`
- Expected result: Exit 0; reflection reports all 57 overrides, the family-union assertion covers all of them, normalization/limits/transactions/results pass, and null BusinessID remains accepted.
- Failure returns to: File 1 if a fake/behavior expectation diverges from the Spec; File 2 method family/helper if production behavior is missing; return to the Spec only for a genuine signature/semantic contradiction.
- Completion criteria: Every parity method belongs to exactly one behavior family, all unit selectors are GREEN, transactional annotations are observable, no method accepts arbitrary Service BusinessID, and `git diff --check` passes.
- Rollback: Revert the Step 5 test and Service body commit; Step 4 retains API parity but must not be released as complete behavior.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImplTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/main/java/top/egon/cola/component/common/mybatis/extension/EgonColaServiceImpl.java`
- Commit: `feat(common-mybatis-plus): enhance all service method families`

### Step 6 — Prove the complete chain with H2 SQL, transactions, and concurrency

- Requirements: `REQ-004`, `REQ-005`, `REQ-007`, `REQ-008`, `REQ-009`, `REQ-010`, `REQ-011`, `REQ-012`, `REQ-013`, `REQ-014`, `REQ-015`, `REQ-016`, `REQ-018`, `REQ-021`, `REQ-022`
- Dependencies: `Step 5`
- Baseline state: Auto-configuration, Mapper, Injector, and Service behavior pass focused/unit tests, but no real SqlSession/JDBC path proves SQL rewrite, logic delete, optimistic lock, H2 null behavior, rollback, or parallel context isolation.
- Observable outcome: A non-web Spring test context executes actual H2 statements and proves every Spec integration case without starting a service or connecting external infrastructure.
- End state: The module implementation is behavior-complete and release-testable; only BOM/docs/distribution gates remain.
- Test-first gate: Required — create both integration test classes before their fixtures/schema; initial RED is missing test Mapper/Service/table configuration. Add the minimum support/schema, then require the already-implemented production chain to pass; a production behavior failure returns to its owning earlier Step instead of being patched opaquely here.
- Ordered files:

#### File 1 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBusinessIdSqlIntegrationTest.java`

- Purpose: Exercise the real MyBatis-Plus plugin/Injector/Service chain for reads, writes, null, logic delete, page, optimistic lock, wide-write protection, ignored tables, and concurrency.
- Symbols: Non-web Spring test configuration and cases corresponding to `TEST-012` through `TEST-024`, `TEST-032` through `TEST-036`, `TEST-041`, and `TEST-042`.
- Repository evidence: Primary Spec §14 enumerates exact fixtures/results; module-local Boot integration tests are allowed and do not constitute starting the project.
- Dependencies and consumers: Uses Files 3-7, H2, actual auto-configuration, MP auto-config, Mapper scanning, JdbcTemplate/SqlSession for observation.
- Why now: Unit tests cannot prove generated SQL or plugin order at the JDBC boundary.
- Contract/signature changes: No production API change; defines observable SQL/data outcomes for every single-row/query/plugin branch.
- Input/output and state mapping: Fixture rows for BusinessIDs 11/22/null and deleted/versioned states map through Provider context to scoped result lists/counts/pages/update counts and persisted columns.
- Error and edge behavior: Missing MDC inserts/queries with null without Java rejection; malformed MDC, unsupported SQL, tenant-column update, empty wide write, stale version, and invalid page assert zero unintended rows and expected exceptions/results.
- Implementation pseudocode:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = TestPersistenceConfiguration.class)
class EgonColaBusinessIdSqlIntegrationTest {
    @Test void scopesOfficialAndCustomQueriesIncludingNullableContextAndLogicDelete() { seedBusinessRows(); provider.set(11L); assertThat(service.list()).extracting(TestBusinessRecord::getBusinessId).containsOnly(11L); provider.set(null); assertThat(service.list()).isEmpty(); }
    @Test void blocksWideWritesAndBusinessIdMutationBeforeJdbc() { assertThatThrownBy(() -> service.remove(Wrappers.emptyWrapper())).isInstanceOf(Exception.class); assertUnchangedFixture(); }
    @Test void isolatesParallelProviderValues() { runWithLatches(11L, 22L, null); assertEachThreadObservedOnlyItsExpectedScope(); }
}
```

- Verification contribution: Real SQL proof for tenant conditions, custom statements, logic delete, plugin order, page/lock/fill/null/concurrency behavior.
- After this file: The integration acceptance matrix exists and is RED until support configuration/schema are added.

#### File 2 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java`

- Purpose: Prove batch normalization, chunk limits, atomic commit, and rollback on a real transaction manager/JDBC connection.
- Symbols: Cases corresponding to `TEST-025` through `TEST-031`.
- Repository evidence: Primary Spec requires all-or-none semantics and an injected middle failure; focused Service tests cannot observe committed rows.
- Dependencies and consumers: Uses the same H2 context/support/schema as File 1 and the actual transactional `TestBusinessService`.
- Why now: Batch transaction correctness is a distinct persistence outcome and deserves an isolated test class.
- Contract/signature changes: No production API change; fixes commit/rollback and normalized entity expectations.
- Input/output and state mapping: Mixed/null entity IDs plus stable Provider map to one normalized value across rows; a unique/constraint failure in a later chunk maps to zero committed batch rows.
- Error and edge behavior: Empty/null/invalid size fail before JDBC as specified; mid-batch insert/update/remove failure rolls back prior chunks; Provider null is accepted unless the test constraint intentionally rejects another field.
- Implementation pseudocode:

```java
@Test void overwritesMixedIdsAndCommitsAllChunks() { provider.set(-7L); List<TestBusinessRecord> batch = mixedNullableRecords(); assertThat(service.saveBatch(batch, 2)).isTrue(); assertThat(batch).allMatch(row -> Objects.equals(row.getBusinessId(), -7L)); assertPersistedCount(batch.size()); }
@Test void rollsBackEveryChunkAfterMiddleConstraintFailure() { provider.set(11L); assertThatThrownBy(() -> service.saveBatch(batchWithMiddleFailure(), 2)).isInstanceOf(DataAccessException.class); assertThat(jdbcTemplate.queryForObject("select count(*) from test_business_record where name like 'rollback-%'", Long.class)).isZero(); }
@AfterEach void clearProviderAndRows() { provider.clear(); jdbcTemplate.update("delete from test_business_record"); }
```

- Verification contribution: Real transaction evidence for batch atomicity and overwrite semantics.
- After this file: Batch acceptance is executable and RED only because the shared fixture/schema are not yet defined.

#### File 3 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessRecord.java`

- Purpose: Model one consumer-owned annotated PO covering every persistence feature without adding a production base entity.
- Symbols: `TestBusinessRecord implements EgonColaBusinessScoped` with `@TableName`, `@TableId`, boxed `businessId`, `@Version`, `@TableLogic`, and audit `@TableField(fill=...)` fields.
- Repository evidence: The Spec target tree and §10 require a consumer PO fixture rather than a Starter-owned entity.
- Dependencies and consumers: Used by Mapper, Service, both integration tests, MetaObjectHandler, and H2 schema.
- Why now: Real statement generation requires TableInfo metadata matching the tested table.
- Contract/signature changes: Test-only mapping; does not publish a production model.
- Input/output and state mapping: H2 columns map to id, nullable BusinessID, name, version, deleted, createTime, updateTime with exact Java types used in assertions.
- Error and edge behavior: Boxed BusinessID can be null; equality/helper methods must not hide changed version/time values from assertions.
- Implementation pseudocode:

```java
@TableName("test_business_record")
class TestBusinessRecord implements EgonColaBusinessScoped {
    @TableId private Long id; private Long businessId; private String name;
    @Version private Integer version; @TableLogic private Boolean deleted;
    @TableField(fill = FieldFill.INSERT) private Instant createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Instant updateTime;
}
```

- Verification contribution: Drives TableInfo, Injector, logic-delete, optimistic-lock, fill, and nullable mapping proof.
- After this file: MyBatis can derive complete consumer table metadata for the integration context.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java`

- Purpose: Provide a scanned consumer Mapper proxy over the EgonCola API.
- Symbols: `TestBusinessMapper extends EgonColaMapper<TestBusinessRecord>` and one intentionally unsupported SQL fixture method if needed by `TEST-024`.
- Repository evidence: Consumer adoption is by extending the common Mapper, not by modifying the Starter’s production scan configuration.
- Dependencies and consumers: Scanned by integration test configuration; injected into TestBusinessService and direct-Mapper tests.
- Why now: Actual MappedStatement injection and direct Mapper guards require a concrete Mapper interface.
- Contract/signature changes: Test-only extension; any custom unsupported statement is isolated and named as a negative fixture.
- Input/output and state mapping: Method parameters flow through the guard/tenant interceptors into H2 and map to TestBusinessRecord/results.
- Error and edge behavior: The negative statement must fail closed without modifying rows; no XML resource or production API is introduced.
- Implementation pseudocode:

```java
@Mapper
interface TestBusinessMapper extends EgonColaMapper<TestBusinessRecord> {
    @Select("select * from test_business_record where name = #{name}")
    List<TestBusinessRecord> selectUnsupportedShape(@Param("name") String name);
    // inherited default and three injected EgonCola methods remain the primary fixture surface
}
```

- Verification contribution: Proves compile adoption, statement injection, direct Mapper normalization, and unsupported SQL behavior.
- After this file: The H2 context can create a real Mapper proxy for all tested operations.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java`

- Purpose: Provide the smallest concrete Spring Service over the generic implementation.
- Symbols: `TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper,TestBusinessRecord>` with repository-consistent constructor/Bean wiring.
- Repository evidence: MyBatis-Plus consumer Services subclass `ServiceImpl`; the user explicitly requested the EgonCola equivalent.
- Dependencies and consumers: Injects Mapper/Provider/properties as required by the final superclass; used by both integration tests.
- Why now: Transaction proxies and the complete official/custom Service surface require a concrete bean.
- Contract/signature changes: Test-only consumer adoption example; adds no overridden business behavior.
- Input/output and state mapping: Test calls pass unchanged into the production superclass and actual Mapper proxy.
- Error and edge behavior: It must not catch/translate persistence failures, because rollback tests need the original transactional exception path.
- Implementation pseudocode:

```java
@Service
class TestBusinessService extends EgonColaServiceImpl<TestBusinessMapper, TestBusinessRecord> {
    TestBusinessService(TestBusinessMapper mapper, EgonColaBusinessIdProvider provider, EgonColaMybatisPlusProperties properties) {
        super(mapper, provider, properties);
    }
    // no behavior override: every tested result must come from the public production implementation
}
```

- Verification contribution: Proves the intended consumer inheritance/wiring and activates Spring transaction advice.
- After this file: Integration tests can call the complete production Service through a concrete proxy.

#### File 6 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessIdProvider.java`

- Purpose: Control Long/null current values and parallel isolation without changing production MDC state globally.
- Symbols: Test-only Provider with per-thread value setup/clear helpers and optional invocation observation.
- Repository evidence: The Provider SPI explicitly permits alternate implementations; test isolation may use thread-confined state even though the Starter must not.
- Dependencies and consumers: Registered as the custom Provider Bean so default MDC Provider backs off; used by both integration tests.
- Why now: Deterministic H2 and parallel tests need controllable context values.
- Contract/signature changes: Test-only implementation of the existing SPI; no production API change.
- Input/output and state mapping: Each test thread’s configured Long/null maps to `currentBusinessId`; clear removes test state in `finally`/after-each.
- Error and edge behavior: No default value is synthesized; leak-detection assertions fail if a thread observes another value or teardown leaves state.
- Implementation pseudocode:

```java
final class TestBusinessIdProvider implements EgonColaBusinessIdProvider {
    private final ThreadLocal<Long> current = new ThreadLocal<>();
    void set(Long value) { if (value == null) current.remove(); else current.set(value); }
    @Override public Long currentBusinessId() { return current.get(); }
    void clear() { current.remove(); }
}
```

- Verification contribution: Makes Long/null/concurrency integration outcomes deterministic and proves default Provider backoff.
- After this file: Tests can isolate three parallel scopes without a production Holder.

#### File 7 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql`

- Purpose: Define the test-only nullable business table, global ignored table, and a constraint usable for rollback injection.
- Symbols: `test_business_record`, `test_global_record`, primary/version/logic-delete/audit columns, and deterministic constraint/index fixtures.
- Repository evidence: Primary Spec §10.7 explicitly defines an H2 nullable fixture and forbids production migrations/test schema packaging.
- Dependencies and consumers: Loaded only by the test context; columns match TestBusinessRecord and SQL assertions.
- Why now: It is the last prerequisite for integration tests to leave fixture-missing RED and exercise production behavior.
- Contract/signature changes: Test database only; no Flyway file or consumer schema template.
- Input/output and state mapping: DDL maps annotated PO fields to H2 types; `business_id BIGINT NULL` accepts absent Provider; a named check/unique rule triggers rollback data without conflating BusinessID nullability.
- Error and edge behavior: Existing Flyway history is untouched; packaged Jar gate must exclude this file; schema initialization failure returns to this fixture, not production code.
- Implementation pseudocode:

```sql
CREATE TABLE test_business_record (
  id BIGINT PRIMARY KEY, business_id BIGINT NULL, name VARCHAR(128) NOT NULL,
  version INT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  create_time TIMESTAMP NULL, update_time TIMESTAMP NULL
);
CREATE TABLE test_global_record (id BIGINT PRIMARY KEY, name VARCHAR(128) NOT NULL);
```

- Verification contribution: Enables real SQL/null/rollback/ignored-table proof and later package-exclusion inspection.
- After this file: Both integration classes must execute against H2 and become GREEN without production schema changes.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaBusinessIdSqlIntegrationTest,EgonColaBatchTransactionIntegrationTest test`
- Expected result: Exit 0; actual H2 statements prove scoped/null/custom/logic-delete/page/lock/block/fill/ignored/concurrent behavior and full batch rollback, with no web server or external connection.
- Failure returns to: Files 3-7 for fixture/configuration errors; otherwise return to the owning production file and Step 2, 3, or 5, rerun its focused gate, then rerun this Step. A dialect/topology requirement outside H2 returns to the Spec/user rather than widening scope.
- Completion criteria: All Spec integration cases pass, test teardown clears thread state/data, no production process starts, and `git diff --check` passes.
- Rollback: Revert only the seven Step 6 test/support/schema files; production Steps remain unit-proven but release remains blocked without integration evidence.
- Commit paths: `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBusinessIdSqlIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/integration/EgonColaBatchTransactionIntegrationTest.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessRecord.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessMapper.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessService.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/java/top/egon/cola/component/common/mybatis/support/TestBusinessIdProvider.java`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/src/test/resources/schema.sql`
- Commit: `test(common-mybatis-plus): prove sql and transaction contracts`

### Step 7 — Export, document, and release-validate the Starter

- Requirements: `REQ-001`, `REQ-002`, `REQ-017`, `REQ-019`, `REQ-020`, `REQ-021`, `REQ-022`, `REQ-023`
- Dependencies: `Step 6`
- Baseline state: The Artifact is complete and tested inside the Common reactor but is not exported by the Components BOM and has no consumer-facing English/Chinese module documentation.
- Observable outcome: Consumers can resolve the versionless Starter from the BOM; both README pairs describe actual code/configuration and nullable MDC behavior; package/dependency/Common/Components gates pass.
- End state: All Spec implementation files, tests, distribution metadata, and documentation are complete; the final audit can compare the implementation against the effective Spec.
- Test-first gate: Not applicable — this Step changes distribution metadata and documentation after behavior is proven; BOM/effective-POM, docs-symbol scan, packaged-Jar, dependency-tree, and reactor commands are the pre-commit executable gates.
- Ordered files:

#### File 1 — `MODIFY egon-cola-components/egon-cola-components-bom/pom.xml`

- Purpose: Export the new concrete Artifact at `${project.version}` for versionless consumer use.
- Symbols: Managed dependency `top.egon:egon-cola-component-common-mybatis-plus-spring-boot-starter`.
- Repository evidence: The BOM lists each consumable Common Artifact separately and never exports the Common aggregator as a Jar.
- Dependencies and consumers: Depends on Steps 1-6 producing a buildable Artifact; consumed by external/business application POMs.
- Why now: Export must follow proven implementation so the BOM never points at an incomplete module.
- Contract/signature changes: Adds one additive dependency-management entry; no existing managed coordinate/version changes.
- Input/output and state mapping: BOM `${project.version}` maps the Artifact to the release train version while its MP internals stay managed by the Components parent during build.
- Error and edge behavior: A wrong Artifact ID/version or dependency placement fails effective-POM/Components reactor resolution.
- Implementation pseudocode:

```xml
<dependencyManagement><dependencies>
  <!-- preserve existing managed artifacts -->
  <dependency><groupId>top.egon</groupId><artifactId>egon-cola-component-common-mybatis-plus-spring-boot-starter</artifactId><version>${project.version}</version></dependency>
</dependencies></dependencyManagement>
```

- Verification contribution: BOM/effective-POM and Components reactor prove consumer resolution.
- After this file: The new Starter is a first-class versionless Components BOM capability.

#### File 2 — `MODIFY egon-cola-components/egon-cola-component-common/README.md`

- Purpose: Add the English aggregator-level module inventory, opt-in rule, link, and validation command.
- Symbols: New module row/section linking the module README and explaining it is not inherited from the aggregator.
- Repository evidence: Current README documents each Common capability, Design Principles, migration notes, and one Common reactor command.
- Dependencies and consumers: Links File 4 and exact Artifact name; consumed by repository maintainers and Common users.
- Why now: Aggregator documentation should point only to a proven/exported module.
- Contract/signature changes: Documentation only; states direct MP Boot3 dependency, current MDC source, nullable Long, and future Provider override without promising live sharding validation.
- Input/output and state mapping: Consumer need maps to opt-in dependency/link/validation instructions rather than an all-in-one Common dependency.
- Error and edge behavior: Must warn that null is not Java-validated, context must stay stable during an operation, consumer schema/topology remains owned externally, and 3.5.17 is unsupported.
- Implementation pseudocode:

```markdown
| MyBatis-Plus Starter | `egon-cola-component-common-mybatis-plus-spring-boot-starter` | Auto-configuration, EgonCola Mapper/Service, nullable BusinessID scoping |

Use the concrete Artifact only in MyBatis-Plus applications; see the module README for MDC, Provider override, schema, plugin, batch, and validation contracts.
Keep the existing Common reactor command and add the focused Artifact command without claiming a running service test.
```

- Verification contribution: English docs/source scan checks exact symbols, properties, links, and boundaries.
- After this file: English Common users can discover the capability without assuming all-in-one behavior.

#### File 3 — `MODIFY egon-cola-components/egon-cola-component-common/README.zh-CN.md`

- Purpose: Mirror the aggregator-level module inventory, opt-in rule, link, and validation boundary in Chinese.
- Symbols: Chinese module row/section with the same Artifact, API names, and commands as File 2.
- Repository evidence: Common maintains a separate synchronized Chinese README.
- Dependencies and consumers: Links File 5 and shares exact code symbols with English docs.
- Why now: The project requires bilingual review content and synchronized technical contracts.
- Contract/signature changes: Documentation only; no translation may change Long/null/MDC/version/dependency semantics.
- Input/output and state mapping: The same consumer adoption path is expressed in Chinese with unchanged Maven and Java identifiers.
- Error and edge behavior: The same warnings about no null validation, stable context, consumer schema/sharding ownership, and no live-runtime proof must appear.
- Implementation pseudocode:

```markdown
| MyBatis-Plus Starter | `egon-cola-component-common-mybatis-plus-spring-boot-starter` | 自动装配、EgonCola Mapper/Service、nullable BusinessID 作用域 |

仅在需要 MyBatis-Plus 的应用中按需依赖具体 Artifact；MDC、Provider 覆盖、schema、插件、批次和验证合同链接到模块中文 README。
保留与英文版完全相同的命令、类名、属性名、版本和验证边界。
```

- Verification contribution: Chinese/English symbol parity scan catches missing or semantically divergent entries.
- After this file: Aggregator documentation is bilingual and synchronized.

#### File 4 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md`

- Purpose: Provide the complete English consumer guide grounded in the implemented API.
- Symbols: Artifact dependency, configuration table, PO/Mapper/Service examples, 57-method parity statement, four custom Service methods, three Mapper methods, plugin order, null/MDC/SecurityContext seam, limits, schema/sharding boundary, migration, rollback, and validation commands.
- Repository evidence: Existing Common Starters own module-specific README usage and operational limits; primary Spec §16 defines adoption/rollback.
- Dependencies and consumers: References actual production symbols/properties and the BOM; consumed by application developers.
- Why now: All code/test names and behavior are stable, so documentation can be verified rather than speculative.
- Contract/signature changes: Documentation only; examples use boxed `Long`, default MDC key, official Boot3 Starter transitively through the Egon Artifact, and no direct native MyBatis declaration.
- Input/output and state mapping: Maven dependency plus consumer PO/Mapper/Service declarations map to the auto-configured runtime path and expected SQL/data ownership.
- Error and edge behavior: Documents malformed MDC, null SQL/DB consequences, stable-context responsibility, wide-write rejection, batch/page bounds, plugin replacement failure, version freeze, unsupported complex SQL, and rollback.
- Implementation pseudocode:

```markdown
## Install and configure
Show the BOM-managed Egon Starter dependency and `egon.cola.component.mybatis-plus.*` properties, including `business-id.mdc-key: businessId`.
## Use
Show `Long businessId`, `EgonColaBusinessScoped`, `EgonColaMapper`, `EgonColaIService`, and `EgonColaServiceImpl` with current-scope methods.
## Limits and verification
State no null/range validation, future Provider override, consumer schema/sharding ownership, 3.5.16 freeze, plugin order, test commands, rollout and rollback.
```

- Verification contribution: Docs scan and human review compare every named symbol/property/command with source.
- After this file: English consumers have an implementation-faithful adoption and operations guide.

#### File 5 — `CREATE egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md`

- Purpose: Provide the complete synchronized Chinese module guide.
- Symbols: Same Maven/Java/property/API/plugin/test/migration/rollback identifiers and section coverage as File 4.
- Repository evidence: Existing Common modules publish paired English/Chinese READMEs; user review language is Chinese.
- Dependencies and consumers: Mirrors File 4 and links back to Common Chinese README where appropriate.
- Why now: Bilingual documentation is a release requirement, not a later translation task.
- Contract/signature changes: Documentation only; prose translation cannot alter technical identifiers or behavior.
- Input/output and state mapping: The same consumer dependency/configuration/code path is explained in Chinese with code blocks copied semantically, not independently redesigned.
- Error and edge behavior: Every warning and validation boundary from English appears here, including nullable BusinessID, malformed MDC, stable context, schema/sharding ownership, and 3.5.16 limitation.
- Implementation pseudocode:

```markdown
## 引入与配置
使用 BOM 管理的 Egon Starter，列出完整 `egon.cola.component.mybatis-plus.*` 配置及默认 MDC key。
## 使用方式
用相同代码符号展示 boxed `Long` PO、Mapper、IService、ServiceImpl 和当前 BusinessID 自有方法。
## 边界与验证
同步说明 null/值域不校验、未来 Provider 覆盖、schema/分片归消费者、版本/插件/批次限制、命令、发布与回滚。
```

- Verification contribution: Pairwise symbol/heading/code-block scan plus user review proves synchronization.
- After this file: Module documentation is release-complete in both languages.

- Validation working directory: `/Users/mario/SelfProject/Egon-COLA`
- Verification command: `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am test && ./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am package -DskipTests && ./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter dependency:tree && ./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test && ./mvnw -B -ntp -f egon-cola-components/pom.xml test`
- Expected result: Every command exits 0; BOM resolves the Artifact, full tests pass, dependency tree respects the boundary, packaged Jar contains imports/public classes but no test schema/classes, both reactor tests pass, and docs symbols match.
- Failure returns to: File 1 for BOM resolution, Files 2-5 for docs parity, the owning earlier Step for code/test/package failures, or the Spec/user for an external runtime/topology expectation.
- Completion criteria: All release gates and `git diff --check` pass; `git status --short` contains only approved implementation paths plus preserved unrelated docs; no service process was started.
- Rollback: Revert the Step 7 BOM/docs commit to stop distribution/discovery while retaining the tested internal module; no database or external state rollback exists.
- Commit paths: `egon-cola-components/egon-cola-components-bom/pom.xml`, `egon-cola-components/egon-cola-component-common/README.md`, `egon-cola-components/egon-cola-component-common/README.zh-CN.md`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.md`, `egon-cola-components/egon-cola-component-common/egon-cola-component-common-mybatis-plus-spring-boot-starter/README.zh-CN.md`
- Commit: `docs(common-mybatis-plus): export and document starter`

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| Baseline model | Repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml -DskipTests validate` | Existing Components Maven model | Exit 0 before Step 1 edits | Baseline/repository owner | `REQ-023`; static |
| RED Step 1 | Repository root | Focused `EgonColaMybatisPlusAutoConfigurationTest` after module/test bootstrap and before production types | Import/disable/dependency contract | Fails for missing auto-configuration/properties/imports | Step 1 File 4 confirms expected reason | `REQ-001`,`REQ-003`,`REQ-017`,`REQ-019` |
| GREEN Step 1 | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaMybatisPlusAutoConfigurationTest test` | Starter bootstrap | Exit 0; import/disabled/default/direct-POM tests pass | Step 1 Files 1-7 | Module/static |
| RED Step 2 | Repository root | Same focused selector after adding MDC/plugin cases and before capability types | Provider/plugin/fill/validator | Fails for missing Beans/types/order | Step 2 File 1 confirms expected reason | `REQ-007`,`REQ-008`,`REQ-012`,`REQ-015`,`REQ-016` |
| GREEN Step 2 | Repository root | Same focused selector | BusinessID and plugin context | Exit 0 for nullable Long, malformed MDC, custom backoff, order, fill, replacement cases | Step 2 owning file | Module context; no JDBC |
| RED Step 3 | Repository root | Same focused selector after Injector tests and before Mapper/Injector | MappedStatement contract | Fails for missing methods/statements | Step 3 File 1 | `REQ-004`,`REQ-009`,`REQ-014` |
| GREEN Step 3 | Repository root | Same focused selector | Mapper/Injector | Exit 0; defaults plus exactly three safe statements | Step 3 Files 2-8 | MyBatis configuration only |
| RED Step 4 | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaIServiceParityTest test` before Service types | API parity | Fails on absent types/signatures | Step 4 File 1 | `REQ-005`,`REQ-006`,`REQ-019` |
| GREEN Step 4 | Repository root | Same parity selector | 57 upstream plus four custom signatures | Exit 0; no inherited-only omission or 3.5.17 drift | Step 4 Files 2-3 | Compile/reflection |
| RED Step 5 | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaIServiceParityTest,EgonColaServiceImplTest test` before body changes | Enhanced Service families | Fails on normalization/limit/transaction/delegation cases | Step 5 File 1 | `REQ-005`-`REQ-013`,`REQ-018` |
| GREEN Step 5 | Repository root | Same parity/unit selector | All explicit Service methods | Exit 0; parity and complete family-union behavior pass | Step 5 File 2 | Module unit |
| RED Step 6 | Repository root | Focused integration selector before support/schema | H2 full chain | Fails for missing test Mapper/Service/table fixture | Step 6 Files 1-2 confirm expected reason | `REQ-007`-`REQ-018`; in-memory integration |
| GREEN Step 6 | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am -Dtest=EgonColaBusinessIdSqlIntegrationTest,EgonColaBatchTransactionIntegrationTest test` | SQL/plugin/transaction/concurrency | Exit 0; all H2 outcomes and rollback pass | Step 6 fixture or owning earlier production Step | In-memory DB; no service/external DB |
| Full module | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am test` | All Starter tests | Exit 0; all 52 Spec cases represented and pass | Owning Step/test | `REQ-021`,`REQ-022` |
| Package | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter -am package -DskipTests` then `jar tf` | Production Jar | Imports/public classes present; `schema.sql` and test classes absent | Steps 1/6 | `REQ-003`,`REQ-021` |
| Dependency boundary | Repository root | `./mvnw -B -ntp -pl :egon-cola-component-common-mybatis-plus-spring-boot-starter dependency:tree` plus direct-POM test | Direct/transitive graph | Approved direct MP Boot3/JSqlParser edges; no unrelated platform/data dependencies | Step 1 | `REQ-017`,`REQ-019` |
| Common reactor | Repository root | `./mvnw -B -ntp -f egon-cola-components/egon-cola-component-common/pom.xml test` | All Common modules | Exit 0; no sibling regression | Owning Step | `REQ-001`,`REQ-021` |
| Components reactor | Repository root | `./mvnw -B -ntp -f egon-cola-components/pom.xml test` | Component parent/BOM integration | Exit 0; Artifact and BOM resolve | Step 1/7 or existing failing module owner | `REQ-001`,`REQ-019` |
| Docs parity | Repository root | `rg`/source script comparing Artifact, classes, properties, commands, headings in four READMEs | English/Chinese docs | Exact technical symbols and warnings appear in both languages | Step 7 Files 2-5 | `REQ-020`; static |
| Static/Git | Repository root | `git diff --check` and path-scoped `git status --short`/`git diff --name-only` after each Step | Formatting/scope | Exit 0; only Step paths staged; unrelated docs preserved | Current Step | `REQ-023`; static |
| Final conformance | Repository root | Compare `REQ-001` through `REQ-023`, `INTERNAL-001` through `INTERNAL-065`, tests, file tree, and seven commits against the effective Spec | Release readiness | No missing/extra path/API/behavior; validation boundary recorded | Spec/user if mismatch is material | All; no live ShardingSphere claim |

Focused tests run before each Step commit; the complete module runs after Steps 4-7; Common and Components reactors run only after the module is internally GREEN. No HTTP server, application main class, PostgreSQL, production ShardingSphere, SecurityContext, or consumer schema is started or accessed. A successful H2 gate proves module behavior only, not production routing/topology.

## 9. Migration, Compatibility, Rollout, and Rollback

Database migration is `N/A`: the primary Spec §10.7/§11 states that the Starter owns no production table and `schema.sql` is a test fixture. No Flyway file is created or modified.

Implementation and release order is fixed:

1. Publish the additive Artifact contract and approved MP 3.5.16 dependency management.
2. Complete BusinessID/plugin/Mapper/Service behavior and in-memory proof before BOM exposure.
3. Export the same-version Artifact through the Components BOM and publish synchronized documentation.
4. Consumers opt in by adding the concrete Artifact, implementing `EgonColaBusinessScoped`, extending the Mapper/Service types, and keeping a Long/null value in the configured MDC key.
5. Consumers independently validate their `business_id BIGINT` column nullability, indexes, historical data, logic-delete/version mappings, SQL dialect, and ShardingSphere route before deployment.
6. A future SecurityContext integration replaces only the `EgonColaBusinessIdProvider` Bean; the default MDC Bean backs off without changing Mapper/Service signatures.

Compatibility is additive for current Egon-COLA modules. Adopting consumers are pinned to MyBatis-Plus 3.5.16 because 3.5.17 removes the IService/ServiceImpl contract. FamilyAiButler String APIs receive no compatibility shim. Global tables retain plain `BaseMapper` or explicit ignored-table configuration.

Rollback is code/dependency-only: remove or downgrade the Egon Starter dependency/BOM entry and restore the consumer’s previous Mapper/Service bases. No Flyway repair or shared data rollback is performed. Disabling `egon.cola.component.mybatis-plus.enabled` removes all EgonCola beans; applications needing unscoped MyBatis-Plus should use the official Starter directly rather than retaining a partially disabled Egon contract. Production parse/provider failures must never fall back to SQL without the BusinessID condition.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | Primary §4, §8, §16 | Steps 1, 7 | Components/Common/module/BOM POMs | bootstrap, Common/Components reactors | Versionless consumable Artifact resolves |
| `REQ-002` | Primary §4, §8 | Steps 2-4, 7 | All public Java types/docs | source/API scan | Every new public class starts `EgonCola` |
| `REQ-003` | Primary §4, §7 | Step 1 | properties, auto-config, imports | ImportCandidates/disabled/Jar | Boot discovery and global switch work |
| `REQ-004` | Primary §4, §9 | Steps 3, 6 | Mapper/Injector/test Mapper | statement/H2/compile gates | Defaults plus three custom statements work |
| `REQ-005` | Primary §4, §9 | Steps 4-6 | IService/Impl/tests | parity/unit/H2 | Consumer Service compiles and executes |
| `REQ-006` | Primary §4, §9 | Steps 4-5 | IService/Impl/parity test | exact reflection/family union | All 57 signatures declared and enhanced |
| `REQ-007` | Primary §4, §7 | Steps 2, 5, 6 | Provider/MDC/Service/integration tests | MDC/null/custom/concurrency | Current nullable BusinessID is execution-owned |
| `REQ-008` | Primary §4, §10 | Steps 2, 5, 6 | Scoped/properties/handler/test PO/schema | Long/null/value matrix | Boxed Long has no Java range/null rejection |
| `REQ-009` | Primary §4, §9 | Steps 3-6 | Mapper/Injector/IService/Impl | custom list/count/exact/optional H2 | Multi-row scope semantics hold |
| `REQ-010` | Primary §4, §7 | Steps 2, 3, 5, 6 | guard/tenant/Service/tests | overwrite/mutation/SQL/concurrency | All supported paths use current Provider |
| `REQ-011` | Primary §4, §7, §9 | Steps 5-6 | Service/batch tests/schema | bounds/chunks/rollback | Batch normalization and atomicity hold |
| `REQ-012` | Primary §4, §7 | Steps 2, 5, 6 | interceptors/validator/Service | order/block/lock tests | Protection chain is deterministic |
| `REQ-013` | Primary §4, §9 | Steps 2, 5, 6 | properties/Service/H2 test | page boundary/SQL | Max page and overflow rules hold |
| `REQ-014` | Primary §4, §9 | Steps 3, 6 | Injector/methods/H2 test | statement/logic-delete | Defaults preserved; custom SQL works |
| `REQ-015` | Primary §4, §10 | Steps 2, 6 | MetaObjectHandler/Clock/test PO | fixed-time/H2 fill | BusinessID/audit fields fill correctly |
| `REQ-016` | Primary §4, §7 | Steps 2, 3, 6 | auto-config/validator/custom fixtures | ContextRunner/ignored table | Safe overrides work; unsafe ones fail early |
| `REQ-017` | Primary §4, §6 | Steps 1, 3, 7 | parent/module POM/docs | direct-POM/dependency tree | Only approved direct MyBatis-Plus edge exists |
| `REQ-018` | Primary §4, §9 | Steps 2-6 | all runtime types/tests | parity/unit/H2/rollback | Upstream shapes and enhanced behavior coexist |
| `REQ-019` | Primary §4, §6, §9 | Steps 1, 4, 7 | parent POM/parity/BOM | version/effective-POM/reactor | 3.5.16 remains frozen |
| `REQ-020` | Primary §4, §16 | Step 7 | BOM and four READMEs | docs parity/source scan | Bilingual adoption/limits are synchronized |
| `REQ-021` | Primary §4, §14 | Steps 1, 6, 7 | module tests/schema/POM | full module/Jar inspection | Tests remain internal and un-packaged |
| `REQ-022` | Primary §4, §14 | Steps 6-7 | tests/docs/gates | H2/module/reactors only | Validation boundary is explicit; no service started |
| `REQ-023` | Primary §4, §16, §18 | Steps 1, 7 and every commit | all Step commit paths | status/diff/check/commit audit | Unrelated work is preserved and commits isolated |

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | IService disappears in MyBatis-Plus 3.5.17. | Steps 1, 4, 7; parent POM/parity/docs | Primary Spec `EVD-009`,`EVD-010`, `DEC-001` | Maintainers | Mitigated — pin 3.5.16, assert parity/version, document upgrade break |
| `RISK-002` | Consumer mutates MDC/SecurityContext during one operation, so Service normalization and TenantLine rewrite see different values. | Steps 2, 5, 6; Provider/Service/integration tests | Stateless-provider design and no Holder constraint | Consumer owner | Accepted boundary — document stability requirement and test concurrent isolation, not mid-call mutation support |
| `RISK-003` | JSqlParser cannot rewrite a consumer-specific SQL dialect/statement. | Steps 2, 3, 6; tenant/guard/integration tests | Primary Spec `RISK-004` | Consumer Mapper owner | Mitigated — fail closed and require consumer integration tests/explicit global-table policy |
| `RISK-004` | Provider null yields database/dialect-specific query/insert behavior. | Steps 2, 5, 6; TenantLine/schema/tests | User decision and `PLAN-CLAR-003` | Consumer schema owner | Accepted — no Java validation; H2 fixture proves module path; consumer owns nullability/routing |
| `RISK-005` | Custom outer interceptor or Injector removes the public protection/statement contract. | Steps 2-3; validator/auto-config | Official backoff behavior in primary Spec `EVD-011` | Starter maintainer | Mitigated — startup validator and safe/unsafe replacement tests |
| `RISK-006` | 57 explicit overrides drift or become repetitive. | Steps 4-5; IService/Impl/parity/unit test | User requirement and 3.5.16 inventory | Starter maintainer | Mitigated — exact reflection gate plus private family helpers; each override remains visible |
| `RISK-007` | Components reactor has unrelated concurrent failures or baseline moves again. | Every Step; POMs/commit boundaries | HEAD changed during design and unrelated docs exist | Repository owner | Mitigated — pre-Step baseline recheck, focused tests first, path-limited commits, report unrelated failures separately |
| `RISK-008` | Plan is reviewed but not approved for execution. | Entire implementation | Primary Spec/Plan both have Review status | User | Closed for planning, execution gated — no production/test source edit begins until explicit approval |

There is no unresolved implementation decision in this Plan. If the user changes the dependency artifact, BusinessID null/value semantics, source ownership, public cross-scope API, or MyBatis-Plus version, update the Spec first and regenerate the affected Plan sections instead of improvising during execution.

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

The Plan covers the requested Common module, MyBatis-Plus public auto-configuration, EgonCola-named Mapper/IService/ServiceImpl, every one of the 57 official IService-visible methods plus four custom Service and three Mapper methods, official Boot3 Starter dependency, unrestricted nullable boxed Long BusinessID, current MDC source, no null validation, and future SecurityContext Provider replacement. It does not treat the attached FamilyAiButler implementation as authoritative instructions.

### 12.2 Spec consistency

All target paths, 65 internal contracts, plugin order, nullable field model, SQL Injector methods, test fixture, compatibility boundary, and rollout/rollback behavior come from the primary Spec. The only implementation clarification distinguishes approved direct dependencies from unavoidable transitive MyBatis libraries, preserves lazy chain execution, and defines the JSqlParser null node already selected by the Spec. The simplicity audit found no fetch-then-forward interface, caller-supplied BusinessID that should supersede trusted context, duplicate model, cache, extra layer, or speculative design pattern.

### 12.3 Repository executability

Every affected file has an exact operation, final responsibility, owning Step, RED/GREEN point, input/output mapping, error behavior, command, rollback point, and commit path. Sequence follows the current Maven/Common Starter conventions and preserves the unrelated dirty-worktree documents. The baseline must be refreshed before execution because current HEAD changed during planning; that refresh cannot expand scope.

### 12.4 Test and release completeness

The Plan combines exact reflection parity, family-level unit behavior, real H2 SQL/plugin/logic-delete/page/lock/fill/null/concurrency tests, transaction rollback, dependency/package/BOM checks, bilingual docs parity, Common/Components reactors, and final Spec conformance. It explicitly stops at module/in-memory proof and does not claim a live PostgreSQL, ShardingSphere, SecurityContext, or application-runtime pass.

### 12.5 Final verdict

`PASS — Ready for user review`

Execution remains unauthorized until the user approves this Plan and the effective Spec/Plan readiness state is updated. After approval, use `egon-coding-executing-plan` to execute exactly one Step and one tested commit at a time.
