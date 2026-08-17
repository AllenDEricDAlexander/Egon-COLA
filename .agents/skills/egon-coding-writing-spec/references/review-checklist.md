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

## Design completeness and consistency

- [ ] Architecture, target file tree, interfaces, fields, models, schema, pages, tests, rollout, and failure behavior describe the same system.
- [ ] Chapter 7 has separate System Architecture Design, High-Level Design, and Detailed Design sections with no contradictory responsibility or flow descriptions.
- [ ] A Complex Spec contains separate Mermaid architecture and critical-flow `flowchart` views plus a swimlane/`sequenceDiagram`; diagrams use real components/contracts/data and include important failure behavior.
- [ ] Mermaid diagrams match dependency rules, interface IDs, state changes, tables, and prose; they do not introduce shortcut calls or unowned data.
- [ ] Detailed Design names transaction/visibility boundaries, concurrency/version/idempotency identities, each material failure point, retry exhaustion, unknown outcomes, recovery owner, reconciliation, and verification.
- [ ] Observability names emitters and lifecycle points, stable low-cardinality fields, correlation propagation, sensitive-data treatment, thresholds, alerts/runbooks, owners, and static-versus-runtime validation limits.
- [ ] The target tree names exact Create/Modify/Delete paths, symbols, responsibilities, ownership, consumers, and requirement mapping.
- [ ] Java package design uses the approved traditional three-layer profile only, unless the existing non-three-layer structure is preserved or a structural migration is an open user decision.
- [ ] `impl` is nested at `biz.service.impl`, not placed beside `biz.service`.
- [ ] Controllers depend on Service interfaces and never call DAO or `service.impl` directly.
- [ ] `service.impl` owns business orchestration and normal transaction boundaries; DAO owns persistence access and no business policy.
- [ ] Config contains technical wiring rather than workflows; Utils is stateless, cohesive, and not a business-rule dumping ground.
- [ ] The current profile does not introduce DDD/COLA packages, aggregates, domain services, repository ports, or DDD value objects.
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
- [ ] Security, tenancy, compatibility, migration, observability, rollback, and operational concerns are covered or evidence-backed `N/A`.

## Interface-contract review

- [ ] Chapter 9 has a complete interface inventory and exactly one detailed subsection for every `API-*`, `RPC-*`, `EVENT-*`, `JOB-*`, or `INTERNAL-*` ID.
- [ ] Each ID represents one atomic Method + URL or protocol operation; CRUD families and independently callable collection/detail/status operations are not grouped.
- [ ] Every HTTP contract has a repository-verified Method and full application URL, including class/method mappings and applicable context/gateway/version prefixes.
- [ ] Path, Query, Header, Cookie, Multipart, and Body inputs are separated; every parameter has type, required/null/default behavior, exact validation, meaning, example, and source.
- [ ] Every HTTP Request Body and success/error Response Body uses the actual complete `jsonc` shape; every field key, including wrapper/nested/array/paging metadata, has a line-end meaning comment.
- [ ] Response design expands the real wrapper and nested fields rather than using a class name, `...`, or undocumented inherited fields.
- [ ] Error mappings name trigger condition, transport status, stable business code, retryability, response shape, and frontend handling.
- [ ] Interface logic states preconditions, validation/permission order, main processing, transactions/data/external calls, side effects, failures, and frontend loading/refresh/retry/error/polling behavior.
- [ ] Every interface detail contains the six required subsections in order; HTTP consumer logic explicitly covers all seven ordered behavior categories, using evidence-backed `N/A` only where a category truly does not apply.
- [ ] List endpoints define pagination/cursor base and limits, filter/null semantics, deterministic sort/tie-breaker, empty-page behavior, count/query cost, concurrent-change behavior, index, frontend states, and list-specific tests.
- [ ] RPC/event/job/internal contracts use their exact symbols/topics and protocol semantics rather than fabricated HTTP URLs.
- [ ] Interface fields, nullability, enums, time/precision, errors, POJOs, database columns, frontend usage, and tests agree.

## Database-design review

- [ ] Chapter 11 inventories every created, altered, read, or written table and expands every inventory item in a detailed subsection.
- [ ] When relational tables exist, Chapter 11 includes a Mermaid `erDiagram` covering every inventory table and the direct unchanged neighbors needed to explain ownership/cardinality.
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
- [ ] Every non-applicable mandatory chapter says `N/A` with evidence and reason.
- [ ] Final verdict is exactly `PASS`, `BLOCKED`, or `REVISE` and matches reality.
- [ ] No Plan, production code, migration execution, service start, or runtime claim was produced as a side effect.
