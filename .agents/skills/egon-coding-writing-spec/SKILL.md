---
name: egon-coding-writing-spec
description: Use when a coding task needs repository-grounded requirements and use-case analysis, system architecture, high-level design, detailed design, complex cross-module analysis, fully expanded interface contracts, Mermaid ER/database design, an RFC-style specification, or an approved design baseline before implementation planning. For Java package design, the current supported profile is the traditional three-layer structure with biz.controller, biz.service, nested biz.service.impl, biz.dao, biz.config, biz.utils, and biz.domain.
---

# EGON Coding Spec Writing

## Purpose

Turn a coding request and the current repository state into a reviewable system-architecture, high-level-design, or detailed-design specification. Write the artifact under `docs/egon/spec` before implementation planning or code changes.

The specification defines **what must be built and why the design is coherent**. It is not an implementation sequence and must not start coding.

## Scope and output contract

- Use this skill only for coding work in an existing or newly initialized repository.
- Ground every material technical statement in the current repository, an explicit user decision, or a cited predecessor specification.
- Produce or revise specification documents and relationship metadata only. Do not produce a Plan, modify production code, apply migrations, or start the project.
- Write the specification in the language requested by the user. If unspecified, follow the language used by the user and nearby repository documentation. Preserve source identifiers, code symbols, paths, schemas, and protocol names exactly.
- Save every new specification as `docs/egon/spec/YYYY-MM-DD-HH-MM-ABSTRACT.md`.
  - Use the user's/repository's local time at creation.
  - Replace `ABSTRACT` with a concise lowercase ASCII kebab-case summary, normally 3–8 words.
  - Example: `docs/egon/spec/2026-08-15-14-30-account-lockout-design.md`.
  - Never overwrite a document with the same minute and abstract; choose a more specific abstract.
- Start from Template Version 2 in `assets/spec-template.md`. Keep every numbered chapter. Write `N/A` with repository evidence and a reason when a chapter does not apply.

## Non-negotiable rules

1. Locate the repository root and read all applicable `AGENTS.md` files before designing.
2. Inspect the relevant build manifests, modules, source packages, migrations, tests, frontend code, configuration, documentation, and existing `docs/egon/spec` documents. Search legacy design locations when they may contain authoritative decisions.
3. Identify the actual programming languages, versions, frameworks, module boundaries, package conventions, architecture style, error model, persistence strategy, migration mechanism, frontend stack, and test tools. Follow existing patterns unless the specification explicitly justifies a change.
4. Convert the original request into stable atomic requirements (`REQ-001`, `REQ-002`, ...), each with observable acceptance criteria and source wording.
5. Do not silently decide a major ambiguity or repair a major requirement/design defect. Present repository evidence, impact, viable options, and a recommendation; ask the user to decide before finalizing affected design sections.
6. Resolve small, local, reversible gaps with the smallest repository-consistent inference. Record consequential inferences as `ASM-*` with evidence and the impact if wrong; do not interrupt the user for ordinary naming, placement, or formatting choices.
7. Use the same metadata header in every Spec. Use RFC-style `Amends`, `Supersedes`, `Depends On`, and `Related Specs` links to make the effective design traceable.
8. Never silently rewrite an approved predecessor's normative design. Create a later Spec that names the earlier document and exact sections it changes. Metadata-only backlinks may be added when repository policy permits.
9. A later Spec may fill gaps or correct earlier design. State whether the later document amends only named sections or supersedes the earlier document for a defined scope; unchanged predecessor sections remain effective.
10. Do not mark a Spec `Accepted` without explicit user/decision-owner approval. An internally complete draft awaiting approval is `Review`; unresolved major decisions require `Draft` and a blocked conclusion.
11. Design every applicable layer at detailed-design depth: exact paths/packages, symbols, contracts, fields, state rules, schema, page states, test cases, compatibility, and failure semantics. Do not invent full production implementations.
12. Review the finished Spec against the original user request and the current repository before delivery. Fix internal defects yourself; surface only unresolved major decisions.
13. Classify Java objects by their actual boundary and lifecycle roles. Follow `references/pojo-modeling.md`; never treat POJO/PO/DO/DTO/VO/BO/Entity/Query/Command/Request/Response/Form/Param/PageQuery/PageResult as mandatory parallel classes.
14. Prevent class explosion. Require a concrete semantic reason for every distinct object and mapper. PO/ORM Entity inheritance is allowed only with repository and lifecycle justification; concrete business services default to composition and delegation rather than inheritance.
15. For Java package design, read `references/three-layer-architecture.md` and use only the traditional three-layer profile currently standardized by this skill: `biz.controller`, `biz.service`, `biz.service.impl`, `biz.dao`, `biz.config`, `biz.utils`, and `biz.domain`. Do not design DDD or COLA packages until this skill is explicitly extended. If the existing repository uses another architecture, preserve it and ask before proposing a structural migration.
16. Classify every Spec as `Simple` or `Complex` using `references/complex-scenario-analysis.md`. For a Complex Spec, complete the evidence map, scenario matrix, ownership/consistency analysis, quality constraints, and evidence-to-decision conclusion chain before selecting the architecture. Do not burden a Simple Spec with ceremonial analysis.
17. Split Chapter 7 into System Architecture Design, High-Level Design, and Detailed Design. A Complex Spec must contain an architecture Mermaid flowchart, a separate critical business/control flowchart, and a Mermaid swimlane/sequence view covering the main participants and important failure behavior.
18. Read `references/interface-contract-design.md` whenever interfaces exist. Assign one interface ID per atomic Method + URL or protocol operation; never group a CRUD family. Keep an inventory, then expand every ID with its exact URL or protocol symbol, complete request rules, actual response/error payloads, frontend-oriented logic, compatibility, and verification. HTTP JSON examples use documentation-only `jsonc`; every field requires a line-end meaning comment.
19. Read `references/database-design.md` whenever persisted data is used or changed. Inventory and expand every affected table and index, including complete column semantics, real queries/access paths, index-order rationale, migration/history handling, transactions, locks, compatibility, verification, and rollback/forward-fix boundaries.
20. Read `references/requirements-use-case-analysis.md` for every Spec. Requirements analysis must identify real actors and stable `UC-*` use cases with triggers, preconditions, main outcomes, alternatives/failures, postconditions, and traceability. Use a complete table or a Mermaid `flowchart`; prefer a Mermaid system-boundary view for complex or multi-actor behavior. Do not confuse use cases with Controller methods or architecture call chains.
21. Whenever relational tables are read, created, or changed, add a Mermaid `erDiagram` covering every inventory table, directly relevant neighboring tables, actual cardinalities, relationship labels, and material PK/FK/UK fields. Map renderer-safe entity names to exact physical tables. The ER diagram complements rather than replaces per-table, per-column, per-index, migration, and transaction design.

## Mandatory reference loading and drafting passes

Do not judge completeness by the line count of `SKILL.md` or the generated Spec. This file is the workflow entry point; the detailed operating rules live in references and the output template. Judge the result by evidence density, contract completeness, cross-section consistency, and whether another engineer can implement it without inventing design decisions.

Read every applicable reference **completely before drafting the corresponding chapter**. Do not rely on the short summary in this file:

| Situation | References that must be read completely |
| --- | --- |
| Every Spec | `references/ambiguity-policy.md`, `references/rfc-governance.md`, `references/complex-scenario-analysis.md`, `references/requirements-use-case-analysis.md`, `references/review-checklist.md`, and `assets/spec-template.md` |
| Java design | `references/three-layer-architecture.md` and `references/pojo-modeling.md` |
| Any HTTP/RPC/event/job/internal contract | `references/interface-contract-design.md` |
| Any persisted read/write or schema dependency | `references/database-design.md` |

For a Complex Spec, use four explicit passes. Preserve the resulting analysis in the Spec instead of collapsing it into a summary:

1. **Discovery pass** — collect exact repository evidence, current call chains, consumers, data stores, configuration, tests, and predecessor decisions; mark evidence, inference, and unverified runtime behavior.
2. **Scenario pass** — enumerate happy, alternative, validation, permission, empty, duplicate, concurrent, timeout, partial-failure, rollback, retry, and recovery paths that materially apply.
3. **Design pass** — decide ownership, contracts, data flow, transaction/consistency boundaries, failure semantics, compatibility, files, models, schema, frontend states, and tests; derive each material decision from evidence.
4. **Consistency pass** — compare requirements, diagrams, interfaces, POJOs, tables/indexes, pages, tests, rollout, and traceability field-by-field and state-by-state; repair contradictions before validation.

Minimum depth is structural, not numerical padding:

- a Complex Spec must contain at least two evidence/current-chain rows, three materially distinct scenario rows, three applicable quality/constraint rows, and two evidence-to-decision conclusion chains unless the affected subsection records `Depth exception:` followed by repository evidence proving that fewer real items exist;
- every Spec must contain evidenced actors and stable `UC-*` goals in either a complete table or Mermaid use-case view, with conditions, outcomes, postconditions, and forward traceability;
- every inventory contract must contain all required per-interface subsections, actual protocol identity, complete parameter rules, success and error outcomes, ordered consumer logic, and verification;
- every inventory table must contain all required per-table subsections, a complete affected-column table, per-index justification tied to real access paths, migration/history handling, and consistency/recovery rules;
- every relational table inventory must be represented in a Mermaid `erDiagram` with physical-name mapping and relationship/key semantics consistent with the detailed design;
- `N/A`, “same as existing,” “handled by framework,” class names, or links may replace detailed content only when the Spec cites the exact authoritative implementation/contract and explains why no new decision is needed.

## Ambiguity and decision boundary

Read `references/ambiguity-policy.md` before asking questions.

Ask the user when a wrong choice would materially change business behavior, scope, public API/RPC/event contracts, ownership boundaries, persistent data, migrations, security/permissions/tenancy, financial correctness, consistency/concurrency, technology selection, deployment topology, external dependencies, compatibility, or irreversible operations.

When several major questions are discovered during one inspection pass, bundle the connected questions with options and consequences so the user can make one coherent decision. Do not repeatedly interrupt for newly discovered details that the repository can answer.

Infer only when the gap is local, reversible, non-observable outside an already-decided contract, and strongly supported by repository convention.

## RFC-style metadata and relationships

Use this exact header field set in every Spec:

| Field | Required meaning |
| --- | --- |
| Document | Current filename as a relative repository link or code value |
| Template Version | `2` for the current template |
| Status | `Draft`, `Review`, `Accepted`, `Implemented`, `Superseded`, or `Rejected` |
| Type | `Feature`, `Refactor`, `Bugfix`, `Architecture`, or another clearly defined coding type |
| Complexity | `Simple` or `Complex` |
| Complexity Drivers | Material interaction/decision-risk drivers, or `None` for a Simple Spec |
| Created | `YYYY-MM-DD HH:mm ZONE` |
| Updated | `YYYY-MM-DD HH:mm ZONE` |
| Owner | User, team, or decision owner |
| Repository | Repository name |
| Scope | Affected modules or bounded context |
| Source Requirement | User request, issue, ticket, or linked brief |
| Baseline Revision | Git commit/branch or explicit uncommitted-worktree snapshot |
| Amends | Earlier Spec links plus exact sections partially changed, or `None` |
| Supersedes | Earlier Spec links plus replaced scope, or `None` |
| Depends On | Normative predecessor/dependency links plus exact sections, or `None` |
| Related Specs | Non-normative contextual links, or `None` |
| Related Plans | Plans implementing this Spec, or `None` |

Relationship targets may be any repository design document, including legacy paths outside `docs/egon/spec`, when that document is still authoritative. Use relative Markdown links and exact section anchors/numbers. Resolve the effective design in this order:

1. Start from the referenced base design.
2. Exclude content superseded for the current scope.
3. Apply accepted amendments in chronological order.
4. Include normative dependencies.
5. Stop and ask the user if accepted documents conflict without a governing relationship.

Read `references/rfc-governance.md` for lifecycle and backlink rules.

## Required design workflow

1. **Reconstruct and classify the request**
   - Quote or accurately paraphrase the original goal, constraints, exclusions, and success criteria.
   - Create the `REQ-*` inventory and identify missing decisions.
   - Apply `references/requirements-use-case-analysis.md`; identify evidenced actors and map behavioral requirements into `UC-*` goals, flows, outcomes, and postconditions.
   - Apply `references/complex-scenario-analysis.md`; record `Simple`/`Complex` and the concrete drivers.
2. **Inspect the repository**
   - Record real files, symbols, consumers, call chains, schemas, pages, tests, and build commands.
   - Separate repository/static evidence from assumptions and from unverified runtime claims.
3. **Resolve design history**
   - Search current and legacy Spec locations.
   - Determine whether this Spec is new, amending, superseding, dependent, or merely related.
4. **Handle ambiguity**
   - Ask for major decisions before finalizing affected sections.
   - Infer small gaps and record consequential assumptions.
5. **Analyze complex scenarios**
   - For a Complex Spec, complete the evidence/current-chain map, scenario matrix, boundary/data ownership map, quality-attribute matrix, critical failures, and conclusion chains before choosing the architecture.
   - For a Simple Spec, state why the lightweight path is sufficient and continue without invented complexity.
6. **Design the solution**
   - Evaluate at least a direct repository-consistent design and any materially different viable alternative.
   - Explicitly consider appropriate patterns such as Strategy, Template Method, Factory, Adapter, Facade, State, Observer, Command, or Specification.
   - Select a pattern only when it resolves a real variation point, coupling problem, lifecycle, orchestration concern, or testability problem. Otherwise record why direct design is clearer and avoids over-engineering.
   - For Java work, read `references/three-layer-architecture.md` and `references/pojo-modeling.md`. Confirm the traditional three-layer applicability gate, keep `impl` under `service`, classify each proposed object by semantic role, apply the class-necessity test, and evaluate persistence inheritance separately from service composition.
   - Read `references/interface-contract-design.md` and `references/database-design.md` when their chapters apply.
   - For relational persistence, derive a Mermaid `erDiagram` from evidenced table ownership, keys, and cardinalities before finalizing per-table details.
7. **Write the Spec**
   - Copy `assets/spec-template.md` and fill all chapters with repository-specific content.
   - Use exact signatures, field tables, state transitions, file trees, and pseudocode where they clarify design; do not write production-ready method bodies.
   - Include the selected use-case artifact and, when relational persistence applies, the Mermaid ER diagram.
   - Expand every interface and every affected table/index; do not stop at inventory tables.
8. **Review and repair**
   - Apply `references/review-checklist.md`.
   - Repair omissions, contradictions, stale paths, vague placeholders, broken traceability, and unjustified scope expansion.
9. **Validate and deliver**
   - Run `scripts/validate_spec.py <spec-path> --strict`.
   - Report the path, status, predecessor relationships, assumptions, and unresolved user decisions.
   - Stop for user review. Do not write a Plan until the user explicitly requests planning against this Spec and any required approval gate is satisfied.

## Required chapters and depth

The template is normative. At minimum, the Spec must contain:

1. **Summary** — problem, chosen direction, affected scope, and intended result.
2. **Background and current state** — actual behavior, call chain, existing consumers, repository evidence, and gap.
3. **Goals and non-goals** — explicit scope control.
4. **Requirements, acceptance criteria, and use-case analysis** — atomic `REQ-*` items and observable outcomes plus evidenced actors and `UC-*` goals. Use a complete use-case table or Mermaid `flowchart`; define triggers, preconditions, main/alternative/failure outcomes, postconditions, interfaces/pages, and tests.
5. **Constraints, assumptions, and decisions** — confirmed constraints, `ASM-*`, resolved decisions, and open blockers.
6. **Project technology context** — current language/framework/build/module/persistence/frontend/testing facts.
7. **Architecture design** — three explicit parts: System Architecture Design, High-Level Design, and Detailed Design. Define boundaries, responsibilities, dependencies, data/control flow, transactions, concurrency, consistency, failure handling, and observability. For a Complex Spec include the required architecture flowchart, critical-flow flowchart, and swimlane/sequence view. For the Java three-layer profile, define Controller, Service interface, `service.impl`, DAO, Config, Utils, and POJO responsibilities plus allowed dependencies.
8. **Package structure and code file tree** — current relevant tree, the selected three-layer target tree, exact create/modify/delete paths, symbols, responsibilities, and requirement mapping. Keep `biz.service.impl` nested under `biz.service`. This is the target design, not implementation order.
9. **Interface definitions** — first provide the complete HTTP/RPC/event/internal inventory, then expand every interface ID. For HTTP include the verified method and URL, all path/query/header/body rules, full success/error `jsonc` payloads with a line-end comment on every field, frontend-oriented interface logic, auth/tenant/idempotency, versioning, compatibility, and tests. Apply equivalent protocol-specific depth to non-HTTP contracts.
10. **POJO and data model design** — repository-defined POJO roles, object ownership/boundaries, class-necessity decisions, persistence objects or ORM entities, DTOs/commands/queries/View Objects/BOs, field types, nullability, validation, state transitions, mappings, inheritance, and safe reuse. Relational model relationships must agree with the ER diagram. Do not require DDD aggregates, domain services, repository ports, or value objects.
11. **Database design** — first inventory every affected table, draw a Mermaid `erDiagram` covering all inventory tables and relevant direct neighbors, then expand each table and each retained/added/changed/removed index. Define cardinalities, PK/FK/UK fields, all relevant columns, native types, null/default/constraint semantics, relationships, real query/access patterns, index column order/selectivity/cost, migration and historical-data handling, transactions/locks/audit, verification, rollback, and compatibility. Never modify an existing migration when repository policy requires a new one.
12. **Frontend page design** — routes, navigation, permissions, layout/component tree, user flows, form rules, API/state mapping, loading/empty/error/disabled/denied states, accessibility, responsiveness, and key copy.
13. **Design patterns and architecture principles** — chosen/rejected patterns, variation point, simplicity test, and alignment with the three-layer architecture; include Controller-to-Service dependency, Service-to-DAO orchestration, cohesion, coupling, information hiding, SOLID, YAGNI, persistence-inheritance safety, and composition-over-inheritance for `service.impl` classes.
14. **Test design** — unit tests for behavior and invariants plus applicable integration, contract, mapper/repository, component, and end-to-end tests; define test data, boundaries, failure cases, expected assertions, tools, and requirement mapping.
15. **Non-functional and cross-cutting design** — security, tenancy, privacy, performance, capacity, caching, audit, logging, metrics, tracing, operations, and maintainability.
16. **Compatibility, migration, rollout, and rollback**.
17. **Alternatives and decisions** — evidence-backed trade-offs and rejected options.
18. **Risks and open questions**.
19. **Traceability matrix** — every `REQ-*` maps to design, contracts/models/pages as applicable, tests, and acceptance evidence; every proposed element maps back to a requirement or necessary infrastructure rationale.
20. **Review and acceptance** — original-request fidelity, repository fidelity, cross-section consistency, relationship correctness, and final verdict.

## Completion verdicts

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — User decision required`
- `REVISE — Internal inconsistency found`

`PASS` means the document is internally complete, not that the user has accepted it. `BLOCKED` must name the decisions required. Never claim implementation or runtime verification from a Spec-only task.

## Common failures

| Failure | Required correction |
| --- | --- |
| Restating the request without repository evidence | Inspect real code, contracts, data, UI, tests, and consumers first |
| Producing a weak conclusion for a complex scenario from one happy-path call chain | Build the scenario, ownership, failure, consistency, and quality-attribute analysis; derive each conclusion through evidence -> constraint -> decision -> consequence -> verification |
| Listing requirements without actor goals and use cases | Add evidenced `ACTOR-*` and `UC-*` analysis using a complete table or Mermaid system-boundary view, including triggers, conditions, outcomes, failures, postconditions, and traceability |
| Choosing major semantics because one option seems obvious | Present evidence/options and ask the user |
| Interrupting for names or reversible local details | Infer the smallest repository-consistent choice |
| Silently editing an approved predecessor | Create an amending or superseding Spec with exact section links |
| Listing packages without a file tree or responsibilities | Add exact target paths, operations, symbols, ownership, and `REQ-*` mapping |
| Interfaces, entities, schema, UI, and tests disagree | Repair through field/state/requirement traceability |
| Providing an interface inventory without expanding each contract | Add one detailed subsection per ID with exact route/symbol, request rules, response/error payloads, logic, consumers, compatibility, and tests |
| Showing a response class name or abbreviated JSON | Show the actual full `jsonc` wire shape and add a line-end meaning comment to every field |
| Listing tables or indexes without per-item design | Expand every table and index with columns, semantics, query/access evidence, index rationale, migration, locking, verification, and rollback |
| Designing relational data without an ER diagram, or drawing an ER diagram that omits inventory tables | Add a Mermaid `erDiagram` with physical-name mapping, actual cardinalities/labels, and material PK/FK/UK fields; reconcile it with every table detail |
| Using a single decorative diagram for a complex architecture | Provide separate architecture, critical-flow, and swimlane Mermaid views that match contracts, data, failures, and dependency rules |
| Creating PO/DO/Entity/BO/DTO/VO/Request/Response for every layer by default | Apply the class-necessity test; reuse semantically identical safe types and keep only justified boundaries |
| Using ambiguous `DO`, `VO`, or `Entity` terminology without repository meaning | State the exact role; in the current profile, `VO` means View Object and `Entity` must have an explicit persistence/ORM meaning |
| Reusing a persistence object as a public contract to reduce classes | Keep persistence concerns behind the boundary and create only the necessary transport/view type |
| Designing business services through a base-class hierarchy for code reuse | Compose explicit collaborators; allow inheritance only for a justified existing framework extension contract |
| Placing `impl` beside `service` | Move implementations under `biz.service.impl` |
| Introducing aggregates, domain services, repository ports, or COLA layers into the current profile | Remove the deferred DDD/COLA structure and use the approved traditional three-layer packages |
| Letting a Controller access DAO or `service.impl` directly | Depend on the Service interface and keep persistence behind the implementation |
| Naming a design pattern without a variation point | Reject it or explain the concrete problem it solves |
| Treating integration tests as unit-test design | Define isolated unit behavior and separate higher-level coverage |
| Omitting a non-applicable chapter | Keep it and write evidence-backed `N/A` |
| Writing implementation order or code | Stop at design; use `egon-coding-writing-plan` after review |

## Skill maintenance

When changing this skill, run `references/acceptance-scenarios.md` as review cases and keep `SKILL.zh-CN.md` plus all `*.zh-CN.md` review mirrors synchronized with the English operational contract.
