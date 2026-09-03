# EGON Coding Spec review checklist

## Original-request fidelity

- [ ] Every explicit user requirement maps to a `REQ-*` or an explicit non-goal.
- [ ] Requirements are atomic and acceptance criteria are observable.
- [ ] User constraints and exclusions remain literal; none were weakened or reconstructed from a different source.
- [ ] Major ambiguities/defects were not silently decided.
- [ ] Small assumptions are minimal, reversible, and supported by repository evidence.

## Complexity and conclusion quality

- [ ] `Complexity` is `Simple` or `Complex`, and `Complexity Drivers` names real decision/interaction risks rather than file count.
- [ ] A Simple Spec remains lightweight without losing exact contracts, files, data, tests, and acceptance evidence.
- [ ] A Complex Spec contains an evidence/current-chain map, scenario matrix, boundary/data-ownership map, applicable quality constraints, and critical failure/recovery paths.
- [ ] Unless a repository-backed `Depth exception:` explains fewer real elements, a Complex Spec has at least two evidence/current-chain rows, three materially distinct scenarios, three quality/constraint rows, and two conclusion chains.
- [ ] Evidence statements are classified as static repository evidence, user decision, inference, or time/environment-bounded runtime evidence; unknowns are not presented as facts.
- [ ] Complex conclusions follow `Evidence -> Constraint/Requirement -> Decision -> Consequence/Trade-off -> Verification`; generic best practice is not used as sole evidence.
- [ ] Every critical actor, consumer, source of truth, write owner, transaction boundary, and compatibility obligation is resolved or recorded as an open major decision.
- [ ] Happy path, partial failure, retry, timeout, duplicate, concurrency, rollback, recovery, permission denial, and empty states are covered as applicable.

## Change surface and proportional depth

- [ ] The Header contains an exact `Change Surface` and `Affected Chapters`; §3.3 contains the evidence-backed change-surface matrix.
- [ ] Complexity and change surface were decided independently; `Simple` did not weaken affected detail, and `Complex` did not widen implementation scope.
- [ ] Every materially relevant area is classified as exactly one of `Affected`, `Context-only`, `Unchanged`, or `Not applicable`.
- [ ] Every `Affected` row names exact paths/symbols, the changed behavior or contract, required Spec treatment, and the chapters containing its detailed design.
- [ ] The Header's `Affected Chapters` exactly matches the distinct chapter numbers referenced by `Affected` rows.
- [ ] `Context-only` areas contain only boundary evidence, the relied-upon invariant, the reason no modification is required, and focused verification.
- [ ] `Unchanged` areas contain only exact current evidence, the preserved invariant, the stopping reason, and focused verification; they introduce no target files, fields, contracts, pages, schema, diagrams, or abstractions.
- [ ] `Not applicable` is used only when the area/concern does not exist or is irrelevant; an existing unchanged layer is not mislabeled `N/A`.
- [ ] Caller/callee tracing stops when an unchanged semantic boundary is proven; inspected context is not silently converted into implementation scope.
- [ ] If repository evidence requires a material expansion beyond the user's stated boundary, the Spec stops for approval rather than silently widening or forcing an impossible narrow design.
- [ ] A DAO-only change does not reproduce unchanged Controller JSON, Service design, unrelated POJOs, full table definitions, ER relationships, or frontend pages.
- [ ] Focused tests prove the changed behavior and the smallest preserved caller/contract/schema boundary; the test plan is not expanded ceremonially to every layer.

## Requirements and use-case review

- [ ] Chapter 4 identifies repository/user-evidenced `ACTOR-*` roles and stable `UC-*` actor goals rather than implementation classes or generic CRUD verbs.
- [ ] Use cases are presented through a complete table or Mermaid `flowchart`; a visual form has a named system/module boundary and real actor-to-goal relationships.
- [ ] Every use case states trigger, preconditions, main success outcome, material alternatives/failures, success/failure postconditions, requirements, contracts/pages, and tests, either in the artifact or adjacent detail.
- [ ] Every material behavioral `REQ-*` maps to at least one use case; non-functional requirements name the use cases they constrain.
- [ ] Use-case branches agree with the scenario matrix, permissions, interface outcomes, state transitions, database effects, frontend states, and test cases.
- [ ] Pure refactors describe the developer/operational use case and preserved behavior rather than using an unexplained `N/A`.

## Repository fidelity

- [ ] Applicable `AGENTS.md` instructions and repository status were checked.
- [ ] Languages, versions, frameworks, module boundaries, build/test commands, and migration rules come from current files.
- [ ] Existing call chains, consumers, reusable code, schemas, pages, and historical Specs are cited with exact paths/symbols/sections.
- [ ] Proposed packages, files, names, dependencies, comments, and annotations follow current project style.
- [ ] Static/source evidence is not presented as live-runtime proof.
- [ ] No unrelated refactor, new dependency, or architecture layer was smuggled into scope.

## REST, CQRS, GraphQL, springdoc, and OpenAPI review

- [ ] Every affected external operation has one atomic `API-*` inventory row and detail; a GraphQL row identifies `Query.field`, `Mutation.field`, or `Subscription.field`, not only `/graphql`.
- [ ] Every API is classified as REST/GraphQL and Query/Command/Subscription, with the smallest sufficient CQRS level and repository/use-case evidence.
- [ ] Queries have no durable business mutation; Commands express a business task/change and define validation, transaction, idempotency, concurrency, side effects, and retry; subscriptions define stream lifecycle.
- [ ] CQRS does not introduce a bus, read store, event sourcing, handler layer, or package structure without a present requirement and full consistency/operations design.
- [ ] REST URIs are resource-oriented; method safety/idempotency, statuses, response bodies, headers, async behavior, conditional requests, pagination/filter/sort, and compatibility agree with RFC 9110 and repository policy.
- [ ] Request fields trace to their true owners; tenant/actor/server-derived values are not caller controlled, and no preflight API exists only to feed a Command.
- [ ] Runtime Bean Validation/groups/normalization, Jackson, Java types, complete commented wire examples, and generated schemas describe the same fields and constraints.
- [ ] The current response/error infrastructure is reused; RFC 9457/Spring `ProblemDetail` is considered for new REST contracts without an established envelope, and no parallel Swagger-only wrapper exists.
- [ ] Every REST operation has explicit stable `operationId`, tags, summary/description, parameters/request body, all material success/error responses, headers, schemas, security, and deprecation behavior as applicable.
- [ ] Only `io.swagger.v3.oas.annotations.*` is designed for new code; Springfox/Swagger 2 annotations and configuration are absent or explicitly bounded as legacy compatibility.
- [ ] The selected springdoc starter/version source matches the actual Spring Boot generation and MVC/WebFlux stack; dependency/configuration, document/UI exposure, Security paths, profiles, gateway/proxy, and aggregation behavior are evidenced.
- [ ] Annotation ownership follows an existing Controller or real API/Facade boundary; no annotation-only interface, fake documentation endpoint, wrapper subclass, or customizer is added without a proven generator gap and drift test.
- [ ] Every affected GraphQL field has complete SDL, named consumer operation, variables and selection, nullability, enums/scalars, resolver mapping, coercion/errors/partial data, authorization, pagination/batching/N+1/cost, and compatibility.
- [ ] REST and GraphQL adapters share one business behavior and cannot diverge in validation, permission, transaction, state, event, or idempotency semantics.
- [ ] Exact feasible OpenAPI generation/parse/assert/diff and GraphQL schema/`GraphQlTester` checks are defined without presenting planned runtime checks as executed evidence.
- [ ] `API-GATE-001` through `API-GATE-009` each appear once with `PASS` or evidence-backed protocol-absence `N/A`; failures or blockers are reflected in Chapter 20 and prohibit final PASS.

## Java, Spring, Egon-COLA, and Manual Check gate

- [ ] The exact source block from `references/user-mandated-java-rules.md` remains present in both Skill entrypoints and was treated as mandatory rather than summarized guidance.
- [ ] §6.2 contains separate Rule 1, 2, 3, 4, 5, 6, 7, 9, 10, and 11 rows in that order; no literal rule is merged, renumbered, weakened, or omitted.
- [ ] Rule 2 inventories every affected layer-to-layer handoff, not only Controller input, and names Validation Group/`ValidationUtils`/libphonenumber behavior plus tests.
- [ ] Rule 3 uses Record for simple objects, `@Value` for immutable non-records, the complete user-mandated Lombok baseline for complex classes, and mandatory MapStruct/MapStructPlus plus Egon `BaseConverter`; conflicts are blocked rather than silently relaxed.
- [ ] Rule 4 inventories every business class and proves `@Slf4j`, explicit Bean name, Lombok `@RequiredArgsConstructor`, per-field `@Qualifier`, and `lombok.config` propagation.
- [ ] Rule 9 classifies affected business logic and mandates a concrete pattern for Complex logic; direct logic is accepted only for Simple logic.
- [ ] For Java work, `references/java-spring-egon-coding-standards.md` was applied and every `MC-*` ID appears exactly once in §20.5.
- [ ] `MC-ARCH-001` proves exactly one allowed architecture profile from the actual tree, selected Archetype/base package, module dependencies, and verifier; no hybrid or invented layer exists.
- [ ] `MC-REUSE-001` contains a repository-backed reuse ledger covering JDK/Spring, Spring Boot Starters, Egon-COLA Components/common infrastructure, and module-local candidates.
- [ ] `MC-DEP-001` proves every added dependency/custom replacement fills a documented gap with version/maintenance/security/operational impact; no duplicate Spring/Egon capability is designed.
- [ ] `MC-NAME-001` inventories affected Java types and enforces explicit PO/BO/DTO/VO/Query/Command/Event/Request/Response or behavior suffixes; no new ambiguous `Data`, `Info`, `Param`, or `Bean` carrier exists.
- [ ] `MC-VALID-001` maps every affected input boundary to `spring-boot-starter-validation`, Jakarta annotations, `@Valid`/`@Validated`, groups for reused inputs, `ValidationUtils`, normalization, error mapping, and tests; custom validators have a proven gap.
- [ ] `MC-MODEL-001` requires Record for simple objects, `@Value` for immutable non-records, and the complete mandated Lombok annotation baseline for complex classes; a generated-constructor/framework conflict is blocked instead of silently relaxed.
- [ ] `MC-CONVERT-001` assigns MapStruct/MapStructPlus conversion and mandatory Egon `BaseConverter<S,T>` integration for every new affected Converter; business code does not bypass it with manual set/get, `BeanUtils.copyProperties`, reflection, JSON mapping, or a local converter abstraction.
- [ ] `MC-LOG-001` and `MC-BEAN-001` cover every affected business/Spring type with `@Slf4j`, stable Bean name, final-field constructor injection, `@RequiredArgsConstructor`, dependency `@Qualifier`, and verified Lombok annotation propagation.
- [ ] `MC-UTIL-001`, `MC-JSON-001`, and `MC-TIME-001` enforce the approved utility set, Jackson-only external serialization, and `java.time` boundary semantics.
- [ ] `MC-CONFIG-001` compares every environment profile's key structure and designs typed `@ConfigurationProperties` where applicable.
- [ ] `MC-PATTERN-001` assigns a concrete design pattern to every affected Complex business flow; only Simple logic may remain direct, and neither hard-coded branch explosion nor ceremonial pattern exists.
- [ ] `MC-SCOPE-001` limits remediation to touched code and reports any compliance conflict requiring a broader approved change.
- [ ] `MC-TEST-001` names focused tests/static gates for the standards, and `MC-BLOCKER-001` accurately summarizes every unresolved Manual Check.
- [ ] Every applicable row is `PASS` with concrete evidence; each `N/A` has concrete evidence and reason; any `FAIL`/`BLOCKED` prevents the final PASS verdict.

## Design completeness and consistency

- [ ] Architecture, target file tree, interfaces, fields, models, schema, pages, tests, rollout, and failure behavior describe the same affected system slice and preserve the declared unchanged boundaries.
- [ ] Chapter 7 has separate System Architecture Design, High-Level Design, and Detailed Design sections with no contradictory responsibility or flow descriptions.
- [ ] A Complex Spec contains separate Mermaid architecture and critical-flow `flowchart` views plus a swimlane/`sequenceDiagram`; diagrams use real components/contracts/data and include important failure behavior.
- [ ] Mermaid diagrams match dependency rules, interface IDs, state changes, tables, and prose; they do not introduce shortcut calls or unowned data.
- [ ] Detailed Design names transaction/visibility boundaries, concurrency/version/idempotency identities, each material failure point, retry exhaustion, unknown outcomes, recovery owner, reconciliation, and verification.
- [ ] Observability names emitters and lifecycle points, stable low-cardinality fields, correlation propagation, sensitive-data treatment, thresholds, alerts/runbooks, owners, and static-versus-runtime validation limits.
- [ ] The target tree names exact Create/Modify/Delete paths, symbols, responsibilities, ownership, consumers, and requirement mapping, and omits unrelated layer skeletons.
- [ ] Java package design uses the approved traditional three-layer profile only, unless the existing non-three-layer structure is preserved or a structural migration is an open user decision.
- [ ] `impl` is nested at `biz.service.impl`, not placed beside `biz.service`.
- [ ] Controllers depend on Service interfaces and never call DAO or `service.impl` directly.
- [ ] `service.impl` owns business orchestration and normal transaction boundaries; DAO owns persistence access and no business policy.
- [ ] Config contains technical wiring rather than workflows; Utils is stateless, cohesive, and not a business-rule dumping ground.
- [ ] A traditional profile does not introduce COLA/DDD layers; an Egon-COLA profile follows the exact selected Archetype and does not introduce `biz.*` or custom modules.
- [ ] Interface fields trace through only the applicable, justified transport/domain/persistence/frontend roles; an inapplicable layer does not force a wrapper class.
- [ ] Every proposed Java object has one repository-defined role, owner, boundary, consumers, and a concrete reason to exist or evidence that reuse is safe.
- [ ] POJO is treated as an umbrella term; DAO/Repository/Mapper/Gateway types are treated as access components, not data carriers.
- [ ] Ambiguous `DO`, `VO`, and `Entity` terminology is resolved explicitly; `VO` means View Object and `Entity` has an exact persistence/ORM meaning in this profile.
- [ ] The design does not mechanically create PO/DO/Entity/BO/DTO/VO/Request/Response variants for every layer.
- [ ] Reused types have the same semantics, lifecycle, validation, exposure, and dependency direction; persistence objects do not leak into public contracts.
- [ ] Every mapper/conversion crosses a real semantic boundary and has a named owner; no no-op mapping chain exists.
- [ ] PO/ORM Entity state rules, database constraints, transaction boundaries, locks, idempotency, and error semantics agree.
- [ ] Frontend routes, permissions, components, user flows, states, validation, and copy agree with contracts.
- [ ] Frontend design expands route/navigation/guards, component tree and ownership, ordered flows/forms, complete UI-state transitions, field/API/cache mapping, accessibility, responsiveness, double-submit/destructive-action safety, and test/manual-verification boundaries.
- [ ] Unit tests target isolated production behavior; higher-level tests have separate responsibilities.
- [ ] Affected security, tenancy, compatibility, migration, observability, rollback, and operational concerns are designed; unchanged concerns name preserved invariants, and only inapplicable concerns use evidence-backed `N/A`.

## Minimum-design and element-necessity review

- [ ] The direct repository-consistent reuse/no-new-element option was evaluated before a more complex architecture.
- [ ] Every new or materially expanded API, class, layer, table, cache, job, dependency, and frontend store/provider has one current requirement, an existing/direct alternative, its concrete inadequacy, added cost/failure modes, and an `Add/Keep/Merge/Remove` verdict.
- [ ] No interface exists only to return values that the caller copies unchanged into another request or that the target backend can derive from identity, tenant, current resource, persisted relationship, configuration, or a stable business key.
- [ ] Every selector/discovery interface has a real independent display/search/choice/audit/negotiation use case, server-owned variability, cache/version behavior, and command-time revalidation.
- [ ] Critical-path before/after network calls, client states, server contracts/state, failure points, and TOCTOU behavior are explicit.
- [ ] Speculative future reuse, pattern names, template completeness, or generic “decoupling” is not used as present necessity evidence.
- [ ] When two options satisfy the same approved requirements, the option with fewer contracts, states, dependencies, calls, migration obligations, and operational burden is selected.

## Interface-contract review

- [ ] When §9 is `Affected`, it has a complete changed-interface inventory and exactly one detailed subsection for every `API-*`, `RPC-*`, `EVENT-*`, `JOB-*`, or `INTERNAL-*` ID; otherwise it contains only the required concise boundary record.
- [ ] The remaining detailed interface checks in this section are applied only when §9 is `Affected`; an unchanged boundary is not expanded merely to satisfy the checklist.
- [ ] Each ID represents one atomic Method + URL or protocol operation; CRUD families and independently callable collection/detail/status operations are not grouped.
- [ ] Every HTTP contract has a repository-verified Method and full application URL, including class/method mappings and applicable context/gateway/version prefixes.
- [ ] Path, Query, Header, Cookie, Multipart, and Body inputs are separated; every parameter has type, required/null/default behavior, exact validation, meaning, example, and source.
- [ ] Every HTTP Request Body and success/error Response Body uses the actual complete `jsonc` shape; every field key, including wrapper/nested/array/paging metadata, has a line-end meaning comment.
- [ ] Response design expands the real wrapper and nested fields rather than using a class name, `...`, or undocumented inherited fields.
- [ ] Error mappings name trigger condition, transport status, stable business code, retryability, response shape, and frontend handling.
- [ ] Interface logic states preconditions, validation/permission order, main processing, transactions/data/external calls, side effects, failures, and frontend loading/refresh/retry/error/polling behavior.
- [ ] Every interface detail contains the seven required subsections in order, beginning with necessity/interaction cost; HTTP consumer logic explicitly covers all seven ordered behavior categories, using evidence-backed `N/A` only where a category truly does not apply.
- [ ] List endpoints define pagination/cursor base and limits, filter/null semantics, deterministic sort/tie-breaker, empty-page behavior, count/query cost, concurrent-change behavior, index, frontend states, and list-specific tests.
- [ ] RPC/event/job/internal contracts use their exact symbols/topics and protocol semantics rather than fabricated HTTP URLs.
- [ ] Interface fields, nullability, enums, time/precision, errors, POJOs, database columns, frontend usage, and tests agree.

## Database-design review

- [ ] When §11 is `Affected`, it inventories and expands every affected schema/data/constraint/index/transaction element; a query-only DAO change with unchanged schema uses a concise access-path and preserved-schema record instead of a full table inventory.
- [ ] The remaining per-table checks in this section are applied only to affected database inventory items; context-only or unchanged tables are not promoted into the inventory.
- [ ] When the relational model or relationships are affected, Chapter 11 includes a Mermaid `erDiagram` covering every affected inventory table and direct unchanged neighbors needed to explain ownership/cardinality; unchanged query-only work does not redraw the ER model.
- [ ] Every ER entity maps to an exact physical table; relationships use correct cardinality and labels and identify material PK/FK/UK fields.
- [ ] ER relationships agree with database- versus application-enforcement, optionality, tenant scope, lifecycle, update/delete behavior, orphan handling, PO/Entity mappings, and per-table constraints.
- [ ] Every table detail contains all seven required subsections in order and states its exact schema/table, owner, authoritative writer, readers, lifecycle, tenant/retention, volume evidence, and source-vs-live verification boundary.
- [ ] Every relevant column has database-native type, length/precision, null/default/generated semantics, constraints, meaning, mapping, and example.
- [ ] Keys, relationships, tenant/soft-delete behavior, states, money/rounding, time zone, IDs, audit/version fields, and retention are explicit as applicable.
- [ ] Every retained/added/changed/removed index names its exact ordered definition, real query/access path, selectivity/cardinality evidence, sort/coverage purpose, uniqueness semantics, overlap, and write/build/storage cost.
- [ ] No speculative index lacks a query; no critical query lacks a credible index/access-path decision and verification plan.
- [ ] Access patterns state caller, predicates/joins/order/page behavior, expected rows, transaction owner, locks/isolation, failure, and idempotency.
- [ ] Migration design names the exact new version/path, historical-data/backfill sequence, compatibility window, locking/build risks, verification SQL, rollback limit, and forward-fix.
- [ ] Existing immutable migrations remain unchanged, and source inspection is not presented as live-schema or execution-plan proof.

## Design-pattern and architecture review

- [ ] Each selected pattern names the concrete variation point/problem, placement, and repository precedent.
- [ ] Direct implementation was considered and rejected only for a concrete reason.
- [ ] Rejected patterns and YAGNI trade-offs prevent needless interfaces, factories, handlers, or inheritance.
- [ ] PO/ORM Entity inheritance, when selected, has an `is-a` or common-lifecycle justification and covers ORM, identity/equality, serialization, migration, compatibility, and test implications.
- [ ] Concrete classes in `biz.service.impl` use composition/delegation by default; any inheritance is an explicit, repository-backed framework or Template Method exception rather than a code-reuse hierarchy.
- [ ] Dependency direction, cohesion, coupling, information hiding, and testability match the stated architecture principles.

## RFC governance

- [ ] Filename matches `YYYY-MM-DD-HH-MM-abstract.md`; header document/timestamps/status match it.
- [ ] `Amends`, `Supersedes`, `Depends On`, `Related Specs`, and `Related Plans` use valid relative links or `None`.
- [ ] Amendment/supersession scope names exact predecessor sections and unchanged content remains effective.
- [ ] Approved predecessor normative text was not silently rewritten.
- [ ] Legacy authoritative design paths are linked directly rather than hidden because they lack a new naming convention.
- [ ] `Accepted` is backed by explicit user/owner approval.

## Traceability and final gate

- [ ] Every `REQ-*` maps to design, tests, and acceptance evidence.
- [ ] Every proposed interface, model, file, migration, page, and test maps to a requirement or necessary infrastructure rationale.
- [ ] Happy path, boundaries, invalid input, permissions, tenancy, concurrency, failure, migration, compatibility, and regression cases are considered as applicable.
- [ ] No unresolved `TBD`, `TODO`, `FIXME`, vague placeholder, or internal contradiction remains in a `Review`/`Accepted` document.
- [ ] Every `Not applicable` mandatory chapter says `N/A` with evidence and reason; every existing but unchanged area uses `Unchanged` instead.
- [ ] Final verdict is exactly `PASS`, `BLOCKED`, or `REVISE` and matches reality.
- [ ] No Plan, production code, migration execution, service start, or runtime claim was produced as a side effect.
