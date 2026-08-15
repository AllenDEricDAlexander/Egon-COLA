---
name: egon-coding-writing-spec
description: Use when a coding task needs a repository-grounded system architecture, high-level design, detailed design, RFC-style specification, or an approved design baseline before implementation planning. For Java package design, the current supported profile is the traditional three-layer structure with biz.controller, biz.service, nested biz.service.impl, biz.dao, biz.config, biz.utils, and biz.domain.
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
- Start from `assets/spec-template.md`. Keep every numbered chapter. Write `N/A` with repository evidence and a reason when a chapter does not apply.

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
| Status | `Draft`, `Review`, `Accepted`, `Implemented`, `Superseded`, or `Rejected` |
| Type | `Feature`, `Refactor`, `Bugfix`, `Architecture`, or another clearly defined coding type |
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

1. **Reconstruct the request**
   - Quote or accurately paraphrase the original goal, constraints, exclusions, and success criteria.
   - Create the `REQ-*` inventory and identify missing decisions.
2. **Inspect the repository**
   - Record real files, symbols, consumers, call chains, schemas, pages, tests, and build commands.
   - Separate repository/static evidence from assumptions and from unverified runtime claims.
3. **Resolve design history**
   - Search current and legacy Spec locations.
   - Determine whether this Spec is new, amending, superseding, dependent, or merely related.
4. **Handle ambiguity**
   - Ask for major decisions before finalizing affected sections.
   - Infer small gaps and record consequential assumptions.
5. **Design the solution**
   - Evaluate at least a direct repository-consistent design and any materially different viable alternative.
   - Explicitly consider appropriate patterns such as Strategy, Template Method, Factory, Adapter, Facade, State, Observer, Command, or Specification.
   - Select a pattern only when it resolves a real variation point, coupling problem, lifecycle, orchestration concern, or testability problem. Otherwise record why direct design is clearer and avoids over-engineering.
   - For Java work, read `references/three-layer-architecture.md` and `references/pojo-modeling.md`. Confirm the traditional three-layer applicability gate, keep `impl` under `service`, classify each proposed object by semantic role, apply the class-necessity test, and evaluate persistence inheritance separately from service composition.
6. **Write the Spec**
   - Copy `assets/spec-template.md` and fill all chapters with repository-specific content.
   - Use exact signatures, field tables, state transitions, file trees, and pseudocode where they clarify design; do not write production-ready method bodies.
7. **Review and repair**
   - Apply `references/review-checklist.md`.
   - Repair omissions, contradictions, stale paths, vague placeholders, broken traceability, and unjustified scope expansion.
8. **Validate and deliver**
   - Run `scripts/validate_spec.py <spec-path> --strict`.
   - Report the path, status, predecessor relationships, assumptions, and unresolved user decisions.
   - Stop for user review. Do not write a Plan until the user explicitly requests planning against this Spec and any required approval gate is satisfied.

## Required chapters and depth

The template is normative. At minimum, the Spec must contain:

1. **Summary** — problem, chosen direction, affected scope, and intended result.
2. **Background and current state** — actual behavior, call chain, existing consumers, repository evidence, and gap.
3. **Goals and non-goals** — explicit scope control.
4. **Requirements and acceptance criteria** — atomic `REQ-*` items and observable outcomes.
5. **Constraints, assumptions, and decisions** — confirmed constraints, `ASM-*`, resolved decisions, and open blockers.
6. **Project technology context** — current language/framework/build/module/persistence/frontend/testing facts.
7. **Architecture design** — boundaries, responsibilities, dependencies, data/control flow, transactions, concurrency, consistency, failure handling, and observability. For the supported Java three-layer profile, define Controller, Service interface, `service.impl`, DAO, Config, Utils, and POJO responsibilities plus allowed dependencies.
8. **Package structure and code file tree** — current relevant tree, the selected three-layer target tree, exact create/modify/delete paths, symbols, responsibilities, and requirement mapping. Keep `biz.service.impl` nested under `biz.service`. This is the target design, not implementation order.
9. **Interface definitions** — HTTP/RPC/event/internal contracts, signatures, field semantics, validation, errors, auth, idempotency, versioning, and compatibility.
10. **POJO and data model design** — repository-defined POJO roles, object ownership/boundaries, class-necessity decisions, persistence objects or ORM entities, DTOs/commands/queries/View Objects/BOs, field types, nullability, validation, state transitions, mappings, inheritance, and safe reuse. Do not require DDD aggregates, domain services, repository ports, or value objects.
11. **Database design** — tables/columns/types/defaults/constraints/indexes/query patterns, migration shape, historical-data handling, transaction/locking/audit, rollback, and compatibility. Never modify an existing migration when repository policy requires a new one.
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
| Choosing major semantics because one option seems obvious | Present evidence/options and ask the user |
| Interrupting for names or reversible local details | Infer the smallest repository-consistent choice |
| Silently editing an approved predecessor | Create an amending or superseding Spec with exact section links |
| Listing packages without a file tree or responsibilities | Add exact target paths, operations, symbols, ownership, and `REQ-*` mapping |
| Interfaces, entities, schema, UI, and tests disagree | Repair through field/state/requirement traceability |
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

When changing this skill, run `references/acceptance-scenarios.md` as review cases and keep `SKILL.zh-CN.md`, `references/three-layer-architecture.zh-CN.md`, and `references/pojo-modeling.zh-CN.md` synchronized with the English operational contract.
