---
name: egon-coding-writing-plan
description: Use when a coding task has a specific reviewed or accepted specification and needs a repository-grounded, file-by-file implementation plan before code changes begin.
---

# EGON Coding Plan Writing

## Purpose

Translate one specific coding Spec and the current repository state into an ordered, file-by-file implementation path. Write the Plan under `docs/egon/plan` after design review and before implementation.

The Plan defines **which file is handled first, what is written there, which file follows, and how each step is proven complete**. It implements the Spec; it must not redesign it or start coding.

## Scope and output contract

- Use this skill only for coding work backed by an identifiable Spec.
- Produce or revise Plan documents, Plan relationship metadata, and the target Spec's `Related Plans` metadata only. Do not modify production/test code, execute migrations, or start the project.
- Write the Plan in the language requested by the user. If unspecified, use the target Spec's natural language. Keep repository paths, symbols, code identifiers, schemas, and commands exact.
- Save every new Plan as `docs/egon/plan/YYYY-MM-DD-HH-MM-ABSTRACT.md`.
  - Use the user's/repository's local creation minute.
  - Replace `ABSTRACT` with a concise lowercase ASCII kebab-case summary, normally 3–8 words.
  - Example: `docs/egon/plan/2026-08-15-16-10-account-lockout-implementation.md`.
  - Never overwrite a same-minute/same-abstract document; choose a more specific abstract.
- Start from `assets/plan-template.md`. Keep all numbered chapters. Use evidence-backed `N/A` when a chapter does not apply.

## Non-negotiable rules

1. A Plan must name exactly one primary target in `Implements Spec` using a repository-relative Markdown link. It must also list the complete `Effective Specs` set: the primary Spec plus applicable accepted amendments, normative dependencies, and replacements.
2. Read the primary and effective Specs completely, including metadata relationships, requirements, acceptance criteria, target file tree, interfaces, models, database, frontend, tests, compatibility, rollout, risks, and approval state.
3. Read all applicable `AGENTS.md` files and inspect the current repository at the target baseline. Verify paths, symbols, consumers, build/test commands, migrations, generated sources, and relevant worktree changes rather than copying stale Spec claims.
4. Do not generate a finalized Plan when no target Spec exists, the target is ambiguous, the effective Specs conflict, or an open major decision changes implementation. Use `egon-coding-writing-spec` or ask the user to resolve the target/decision.
5. Do not introduce behavior, public contracts, schema fields, pages, dependencies, architectural layers, or refactors absent from the effective Spec. A major Spec defect or material repository drift must return to the user and the Spec before planning continues.
6. Infer only small, local, reversible implementation details that preserve the effective design and follow one clear repository convention. Record consequential inferences as `Plan Clarification` entries with evidence and impact if wrong.
7. Every implementation Step must be small enough to verify independently and normally map to one semantic commit. Respect repository instructions requiring one commit per task.
8. Every Step must name covered source requirement IDs, dependencies, observable outcome, a strict ordered file list, file operation, symbols, repository-language pseudocode, intermediate result, exact validation, completion criteria, rollback point, and proposed commit message.
9. Plan behavior changes test-first: first write or modify a focused failing unit/contract test, state its expected RED reason, then plan the minimum production change, then refactor/wire/verify. If test-first is technically impossible, treat the exception as a major decision unless the user or repository explicitly authorizes it.
10. Pseudocode must be implementation-bearing but not production code. Use actual class/function/component/table names, signatures, field mappings, branches, calls, state changes, error paths, transactions, and assertions in the repository's language and framework style.
11. Determine file order from real dependencies. Do not mechanically apply a layer list when migration, generated code, contract publication, cross-module compilation, or frontend/backend compatibility requires a different order.
12. Include all applicable migrations, configuration, permissions, observability, documentation, compatibility, rollout, rollback, and release verification files in the ordered steps.
13. Never modify an existing immutable Flyway migration. If the Spec requires one database change, plan exactly one new next-version migration unless the user explicitly approved a different migration decomposition.
14. Review the finished Plan against both the original user requirements recorded by the Spec and the effective Spec design. Fix omissions and inconsistencies before delivery.
15. Do not mark a Plan `Ready` without an explicitly accepted primary Spec and explicit user/decision-owner approval of the Plan. A complete Plan awaiting review is `Review`; a Spec or decision blocker requires `Draft` or `Blocked`.

## Target Spec and effective-design resolution

Read `references/spec-resolution.md` before writing.

If the user provides a Spec path, use it. Otherwise search `docs/egon/spec` and relevant legacy design directories; select a target only when exactly one current document unambiguously governs the request. Ask the user when multiple candidates remain.

Use relative links rather than requiring a special numeric ID. A legacy Spec is eligible only when it provides a sufficiently complete and still-current coding design. If it lacks traceable requirements, contains unresolved decisions, or describes already completed work, stop and explain whether it needs an amending Spec, a residual-work Spec, or verification rather than another Plan.

Resolve the effective design in this order:

1. Start with the primary target Spec.
2. Replace content superseded for the current scope.
3. Apply accepted amendments in chronological order.
4. Include normative dependencies.
5. Preserve the source requirement identifiers (`REQ-*` or an established predecessor scheme). If a source has no identifiers, create Plan-local trace aliases only for existing statements; never invent new requirements.
6. Treat ungoverned conflicts as major blockers.

Record the exact Spec status, update/revision, baseline commit, and links in the Plan header. An explicit user request may authorize a Draft/Review Plan against a non-accepted Spec, but it remains non-Ready and must expose that risk.

## Spec defect, drift, and inference boundary

Return to the Spec/user when a finding changes business behavior, scope, acceptance, public API/RPC/event contracts, model/schema, ownership, dependency direction, permissions/security/tenancy, transactions/consistency/idempotency, technology selection, compatibility, migration, rollout, destructive behavior, or operational cost.

Record a `Plan Clarification` only when the detail is local, reversible, not externally observable beyond an already-decided contract, and directly supported by current code—for example an internal helper name, test fixture placement, or a class rename with identical semantics.

Do not disguise a redesign as a clarification.

## Required planning workflow

1. **Lock the target Spec**
   - Record the primary path, status, revision, relations, approval evidence, and original source request.
2. **Build the effective requirement set**
   - Extract every effective requirement, acceptance criterion, interface, field, state rule, table/page/test requirement, non-functional constraint, migration, and rollout condition.
3. **Inspect the current repository baseline**
   - Verify actual files/symbols and identify already-complete, missing, moved, generated, or conflicting work.
   - Preserve unrelated dirty-worktree changes and plan path-limited commits.
4. **Resolve blockers and clarifications**
   - Ask about major defects/ambiguities; infer and record only small implementation gaps.
5. **Derive the dependency path**
   - Establish compilation, contract, data, migration, runtime, and consumer order.
   - Explicitly identify steps that may run in parallel and steps that must remain sequential, without creating overlapping write scopes.
6. **Write the target file tree**
   - List every Create/Modify/Delete path once with symbols, responsibility, requirement mapping, and owning Step.
7. **Write ordered implementation Steps**
   - For each behavior, place the focused failing test before its production implementation.
   - For each file, write language/framework-specific pseudocode and the state after that file is completed.
   - End each Step with targeted verification, objective completion evidence, rollback, and one proposed commit.
8. **Write quality and release gates**
   - Use exact repository commands for focused tests, module tests, static checks, builds, integration/E2E/manual checks, migration validation, and final regression.
9. **Review and repair**
   - Apply `references/review-checklist.md` and reconcile every mismatch with the effective Spec.
10. **Validate and deliver**
    - Run `scripts/validate_plan.py <plan-path> --strict`.
    - Report the Plan path/status, target/effective Specs, Step count, clarification entries, blockers, and validation boundary.
    - Stop for user review. Do not begin implementation as a side effect.

## File-order and pseudocode contract

Each Step must use this sequence contract:

1. State requirements, dependencies, and one observable outcome.
2. List files in the exact order an implementer should handle them.
3. For each file, provide:
   - `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` operation;
   - exact repository-relative path and affected symbols;
   - why this file occurs at this point in the sequence;
   - signatures/contracts/fields that must be added or changed;
   - language-appropriate pseudocode for control flow, mapping, persistence, errors, and tests;
   - the expected intermediate repository state after this file.
4. State the focused verification command and exact success result.
5. State completion evidence, rollback point, and one semantic commit message.

Good Java pseudocode names annotations, method signatures, collaborators, transaction boundaries, domain calls, mapper/repository operations, exceptions, and assertions. Good TypeScript/React pseudocode names props/types, hooks/state, API calls, render branches, events, and component tests. Good SQL pseudocode names the new migration, DDL/DML, constraints, indexes, backfill, guards, and rollback/forward-fix limits.

Avoid placeholders such as “implement service,” “handle errors,” “update frontend,” or “run tests.” The implementer must not need to invent architecture or file order.

## Required chapters

1. **Summary** — target Spec, scope, implementation direction, and final evidence.
2. **Target Spec and effective design** — exact relative links, status/revisions, relationships, approval, and source requirements.
3. **Effective requirements and acceptance** — all source IDs/statements, exact Spec sections, acceptance, and implementation impact.
4. **Implementation strategy and dependency order** — why the sequence works, test strategy, migration/compatibility constraints, parallelism, and commit boundaries.
5. **Change file tree** — complete Create/Modify/Delete/Rename/Generated tree mapped to Steps and requirements.
6. **Prerequisites, constraints, and Plan Clarifications** — commands, environments, immutable files/contracts, decisions, dirty-worktree precautions, and small evidence-backed inferences.
7. **Ordered file-by-file implementation Steps** — exact path order and pseudocode contract above.
8. **Test, validation, and quality gates** — RED/GREEN points, focused/module/full checks, expected results, and failure return points.
9. **Migration, compatibility, rollout, and rollback**.
10. **Requirement-to-Step traceability matrix**.
11. **Risks, blockers, and user decisions**.
12. **Review and acceptance** — original requirement fidelity, Spec consistency, repository executability, coverage, release safety, and final verdict.

## Completion verdicts

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — Spec or user decision required`
- `REVISE — Plan and Spec are inconsistent`

`PASS` means internally complete, not user-approved or ready to implement. Never claim code, database, service, browser, or runtime verification from a Plan-only task.

## Common failures

| Failure | Required correction |
| --- | --- |
| Writing a Plan from a one-line request | Write/approve a Spec first |
| Requiring a numeric Spec ID instead of an exact path | Link the actual governing Spec and its effective relations |
| Copying a stale target tree | Re-inspect current paths, symbols, consumers, and worktree state |
| Treating a new API/table/page as a Plan clarification | Stop and amend the Spec with user approval |
| Listing phases without exact files | Expand each Step into strict file order and symbols |
| Generic pseudocode such as “implement validation” | Name signatures, fields, branches, collaborators, errors, and assertions |
| Putting all tests after production code | Plan focused RED tests before each behavior implementation |
| Guessing validation commands | Read repository scripts/build files and state objective pass criteria |
| Omitting migration/config/docs/permission/observability files | Add every applicable file to the tree and ordered Steps |
| Plan file tree differs from Spec without escalation | Return to the Spec unless it is a proven semantic-preserving rename |
| Writing code or starting runtime after the Plan | Stop and deliver for user review |
