# <Specification title>

| Field | Value |
| --- | --- |
| Document | `YYYY-MM-DD-HH-MM-abstract.md` |
| Status | `Draft` |
| Type | `Feature / Refactor / Bugfix / Architecture` |
| Created | `YYYY-MM-DD HH:mm ZONE` |
| Updated | `YYYY-MM-DD HH:mm ZONE` |
| Owner | `<decision owner>` |
| Repository | `<repository>` |
| Scope | `<modules or bounded context>` |
| Source Requirement | `<user request / issue / ticket / brief>` |
| Baseline Revision | `<commit and branch, or explicit dirty-worktree snapshot>` |
| Amends | `None` |
| Supersedes | `None` |
| Depends On | `None` |
| Related Specs | `None` |
| Related Plans | `None` |

## 1. Summary

In one to three paragraphs, state the problem, selected direction, affected scope, and intended result. This section must independently answer why the change is needed, what will change, and what success looks like.

## 2. Background and Current State

### 2.1 Business and user context

### 2.2 Repository evidence

Name exact modules, paths, packages, symbols, call chains, consumers, contracts, tables, pages, configuration, tests, and predecessor Specs.

| Evidence | Current responsibility or behavior | Design significance |
| --- | --- | --- |
| `<path:line or symbol>` | `<observed fact>` | `<why it constrains the design>` |

### 2.3 Problem statement and gap

Describe current behavior, desired behavior, the gap, and its impact. Separate static repository proof from runtime claims that were not verified.

## 3. Goals and Non-goals

### 3.1 Goals

### 3.2 Non-goals

Define explicit exclusions so scope cannot silently expand during planning or implementation.

## 4. Requirements and Acceptance Criteria

| ID | Atomic requirement | Priority | Observable acceptance criteria | Source |
| --- | --- | --- | --- | --- |
| `REQ-001` | `<one verifiable behavior or constraint>` | Must | `<observable result>` | `<original user wording or decision>` |

Avoid requirements that say only “support,” “optimize,” or “improve.” Each item must be independently testable.

## 5. Constraints, Assumptions, and Decisions

### 5.1 Confirmed constraints

### 5.2 Small-gap assumptions

| ID | Inference | Repository evidence | Why locally reversible | Impact if wrong |
| --- | --- | --- | --- | --- |
| `ASM-001` | `<minimal inference>` | `<path/convention>` | `<reason>` | `<impact>` |

### 5.3 Resolved decisions

| ID | Decision | Decision owner | Evidence and rationale | Requirements |
| --- | --- | --- | --- | --- |
| `DEC-001` | `<confirmed choice>` | `<owner>` | `<evidence>` | `REQ-001` |

### 5.4 Open major decisions

| ID | Question and options | Recommendation, not decision | Impact | Owner | Status |
| --- | --- | --- | --- | --- | --- |
| `DEC-002` | `<blocking choice>` | `<recommended option and why>` | `<scope/contract/data impact>` | User | Open |

## 6. Project Technology Context

Document the repository's actual programming languages and versions, frameworks, build tools, module structure, architecture style, persistence and migration tools, frontend stack, test frameworks, deployment model, and applicable repository instructions. Cite evidence for each material fact.

| Concern | Current choice | Repository evidence | Constraint on design |
| --- | --- | --- | --- |
| Language/runtime | `<...>` | `<manifest/path>` | `<...>` |

## 7. Architecture Design

### 7.1 Architecture overview

Describe the selected design and why it fits the current architecture.

### 7.2 Module boundaries and responsibilities

| Module/component | Responsibility | Inputs/outputs | Dependencies | Requirements |
| --- | --- | --- | --- | --- |
| `<name>` | `<single responsibility>` | `<contracts>` | `<allowed dependencies>` | `REQ-001` |

### 7.3 Call chain, control flow, and data flow

Use a Mermaid diagram when it materially clarifies three or more components. The diagram must agree with the interface, model, schema, frontend, and file-tree sections.

### 7.4 Transaction, consistency, concurrency, and idempotency

### 7.5 Failure semantics and recovery

### 7.6 Observability and operational boundaries

## 8. Package Structure and Code File Tree

### 8.1 Current relevant tree

```text
<only repository paths relevant to this design>
```

### 8.2 Target tree

```text
<exact CREATE / MODIFY / DELETE files and packages; do not put implementation order here>
```

### 8.3 Package and file responsibilities

| Operation | Path/package | Symbols | Responsibility | Dependencies | Requirements |
| --- | --- | --- | --- | --- | --- |
| Create / Modify / Delete | `<exact path>` | `<class/function/component>` | `<single responsibility>` | `<existing/new dependencies>` | `REQ-001` |

Explain moves or deletions, generated-file handling, registration/wiring ownership, and consumer impact. The target tree must be complete enough for a Plan to derive ordered file steps without inventing architecture.

## 9. Interface Definitions

Cover applicable HTTP, RPC, event/message, CLI, scheduled-job, and internal service contracts.

| ID | Kind/layer | Method, route, topic, or symbol | Input | Output | Error/status | Auth/tenant | Idempotency/version | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `API-001` | HTTP | `POST /...` | `<Request>` | `<Response>` | `<error model>` | `<permission>` | `<rules>` | `REQ-001` |

For every applicable contract, define:

- exact signature/path/topic and ownership;
- request/response/event fields with type, required/null/default semantics, validation, examples, and sensitive-data treatment;
- error mapping, retryability, timeout, ordering, deduplication, authentication, authorization, tenancy, versioning, and compatibility;
- producer, consumers, call sequence, and boundary adapters.

Use repository-language signature or payload pseudocode where useful, but do not write a complete production implementation.

## 10. Entity and Domain Model Design

### 10.1 Aggregates, entities, value objects, and invariants

| Model | Kind | Ownership/lifecycle | Invariants | Persistence | Requirements |
| --- | --- | --- | --- | --- | --- |
| `<name>` | Aggregate / Entity / Value Object | `<owner>` | `<rules>` | `<table/none>` | `REQ-001` |

### 10.2 Field design

| Model.field | Type | Required/null/default | Validation and semantics | Source/mapping | Requirements |
| --- | --- | --- | --- | --- | --- |
| `<Type.field>` | `<language type>` | `<rules>` | `<meaning>` | `<DTO/PO/column>` | `REQ-001` |

### 10.3 DTO, Command, Query, VO, PO, and mapper relationships

### 10.4 State transitions and lifecycle

Define allowed transitions, guards, side effects, invalid transitions, and concurrency/version rules.

## 11. Database Design

If no database change is involved, write `N/A` with repository evidence and explain why. Otherwise define:

- tables, columns, database-native types, lengths/precision, defaults, null semantics, primary/foreign/unique/check constraints;
- indexes tied to real query patterns and expected cardinality/order;
- repository/mapper ownership and entity-column mappings;
- exactly one new migration when repository policy requires it; never modify an existing immutable migration;
- schema and data migration pseudocode, existing-data handling, backfill, compatibility window, rollout order, and rollback/forward-fix strategy;
- transaction, lock, optimistic version, idempotency, retention, audit, and privacy behavior.

| Table | Change | Columns | Constraints/indexes | Access pattern | Migration path | Requirements |
| --- | --- | --- | --- | --- | --- | --- |
| `<table>` | Create / Alter / None | `<field definitions>` | `<constraints>` | `<queries>` | `<new migration path>` | `REQ-001` |

## 12. Frontend Page Design

If the repository has no affected frontend, write `N/A` with repository evidence. Otherwise define:

- route, navigation/menu entry, page ownership, permissions, tenant scope, and deep-link behavior;
- page layout and component tree, responsive behavior, accessibility, focus/keyboard rules, and key copy;
- user flows, form fields, client/server validation, confirmations, destructive-action protection, and success feedback;
- API mapping, state ownership, caching, invalidation, optimistic/pessimistic updates, and refresh behavior;
- initial, loading, skeleton, empty, populated, partial, error, retry, disabled, read-only, and permission-denied states.

| Page/component | Route/entry | User action | API/contract | State/error behavior | Permission | Requirements |
| --- | --- | --- | --- | --- | --- | --- |
| `<name>` | `/...` | `<action>` | `API-001` | `<states>` | `<permission>` | `REQ-001` |

## 13. Design Patterns and Architecture Principles

### 13.1 Selected patterns

| Pattern/principle | Concrete variation point or problem | Placement | Why direct code is insufficient | Repository alignment |
| --- | --- | --- | --- | --- |
| `<pattern>` | `<real problem>` | `<paths/types>` | `<reason>` | `<existing precedent>` |

### 13.2 Rejected patterns and simpler alternative

Record why Strategy, Template Method, Factory, Adapter, Facade, State, Observer, Command, Specification, Domain Service, or another candidate is unnecessary when direct design is clearer.

### 13.3 Architecture principles

Explain applicable choices around cohesion, coupling, dependency direction/inversion, information hiding, SOLID, DDD/hexagonal/layered/CQRS/event-driven ideas, YAGNI, testability, and maintainability. Do not claim a principle without showing how paths and dependencies enforce it.

## 14. Test Design

### 14.1 Unit tests

Define isolated behavior and invariant tests for concrete production symbols. State fixtures, action, assertions, boundaries, invalid input, state transitions, exceptions, concurrency decisions, and mocks/fakes only where unavoidable.

### 14.2 Integration, contract, persistence, component, and end-to-end tests

Separate these responsibilities from unit tests and use the repository's actual tools.

### 14.3 Test cases and data

| ID | Level | Target | Scenario/input | Expected assertion | Test double/data | Tool/path | Requirements |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `TEST-001` | Unit | `<symbol>` | `<case>` | `<observable assertion>` | `<fixture>` | `<framework/path>` | `REQ-001` |

Cover happy paths, boundaries, invalid input, permissions, tenancy, retries/timeouts, concurrency, partial failure, rollback, compatibility, and regression cases as applicable.

## 15. Non-functional and Cross-cutting Design

Address applicable security, authorization, tenancy, privacy, secrets, performance, capacity, latency, caching, rate limiting, availability, audit, logging, metrics, tracing, alerting, internationalization, accessibility, operability, and maintainability. Give an evidence-backed `N/A` for materially relevant categories that do not apply.

## 16. Compatibility, Migration, Rollout, and Rollback

Define source/binary/API/data compatibility, old clients and data, migration/backfill sequence, feature flags, deployment order, staged rollout, pre/post-deploy checks, rollback limits, and forward-fix strategy.

## 17. Alternatives and Decisions

| Option | Advantages | Disadvantages/risks | Repository fit | Decision and rationale |
| --- | --- | --- | --- | --- |
| A | `<...>` | `<...>` | `<...>` | Selected / Rejected |

Record why the chosen design is preferable. Do not add alternatives merely to fill the table.

## 18. Risks and Open Questions

| ID | Risk/question | Probability | Impact | Mitigation or decision owner | Status |
| --- | --- | --- | --- | --- | --- |
| `RISK-001` | `<...>` | Low / Medium / High | `<...>` | `<...>` | Open / Closed |

## 19. Traceability Matrix

| Requirement | Architecture/packages | Interface | Model/database | Frontend | Tests | Acceptance evidence |
| --- | --- | --- | --- | --- | --- | --- |
| `REQ-001` | `§7 / §8` | `API-001` | `<model/table or N/A>` | `<page or N/A>` | `TEST-001` | `§4 criterion` |

Every `REQ-*` must map to design, tests, and acceptance. Every proposed contract, model, file, page, migration, and test must map back to a requirement or documented necessary infrastructure rationale.

## 20. Review and Acceptance

### 20.1 Original-request fidelity

Confirm every explicit request is represented by a `REQ-*` or explicit non-goal and that no requested outcome was weakened.

### 20.2 Repository and technical fidelity

Confirm paths, symbols, consumers, commands, language/framework choices, migration policy, and test tools against the current baseline.

### 20.3 Cross-section consistency

Confirm architecture, file tree, interfaces, fields, entity state, schema, page flows, failure semantics, security, compatibility, tests, and traceability describe one design.

### 20.4 Relationship and effective-design review

Confirm all predecessor links and exact sections, amendment/supersession scope, status, and unchanged effective content.

### 20.5 Final verdict

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — User decision required`
- `REVISE — Internal inconsistency found`
