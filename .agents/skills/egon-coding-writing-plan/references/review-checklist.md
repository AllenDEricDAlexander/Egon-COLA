# EGON Coding Plan review checklist

## Target identity and effective design

- [ ] `Implements Spec` has exactly one valid repository-relative link and identifies the actual governing document.
- [ ] Spec status, revision, baseline, approval evidence, amendments, supersessions, and dependencies match current documents.
- [ ] `Effective Specs` is complete and ungoverned conflicts are blocked.
- [ ] Every source requirement/acceptance criterion is preserved; Plan-local aliases point only to existing statements.
- [ ] The target describes unmet coding work rather than already completed implementation or verification-only work.

## Original requirement and Spec fidelity

- [ ] Every effective requirement appears in at least one Step and the traceability matrix.
- [ ] User constraints and exclusions recorded by the Spec remain literal.
- [ ] No new business behavior, public contract, field, schema, page, dependency, architecture layer, or unrelated refactor was introduced.
- [ ] Architecture, package/file tree, interfaces, DTO/domain/PO mappings, schema, frontend states, permissions, error semantics, transactions, idempotency, observability, compatibility, migration, and rollout match the effective Specs.
- [ ] Major defects/drift were escalated; `Plan Clarification` contains only small, reversible, evidence-backed details.

## Repository executability

- [ ] Applicable `AGENTS.md`, current commit/branch, dirty worktree, and concurrent changes were inspected.
- [ ] Every Create/Modify/Delete/Rename/Generated path appears once in the inventory and follows repository layout.
- [ ] Existing paths and symbols were verified; new paths/names follow nearby style and module boundaries.
- [ ] Consumers, registration/wiring, generated sources, build order, migrations, and cross-module dependencies are represented.
- [ ] Exact commands come from repository build/scripts rather than guesses.
- [ ] Unrelated work is protected by non-overlapping, path-limited scopes and commits.

## Step and pseudocode quality

- [ ] Every Step has source requirements, dependencies, one observable outcome, ordered files, verification, expected result, completion criteria, rollback, and one commit.
- [ ] Every file has operation, exact path, symbols, purpose, sequence reason, contract/signature changes, language-appropriate pseudocode, and after-file state.
- [ ] Pseudocode names real methods/types/fields/calls/branches/errors/transactions/assertions rather than generic actions.
- [ ] File order respects compilation, RED/GREEN, schema/data, contract publication, consumer, frontend/backend, configuration, and rollout dependencies.
- [ ] Steps are independently verifiable and small enough for semantic commits.
- [ ] Parallel steps have non-overlapping write scopes; sequential constraints are explicit.

## Test-first and validation quality

- [ ] Every behavior change places a focused failing unit/contract test before production implementation.
- [ ] Expected RED reason proves missing behavior rather than broken fixtures or environment.
- [ ] Minimum GREEN implementation and subsequent refactor/wiring are distinguishable.
- [ ] Unit, integration, persistence/mapper, contract, component, frontend, E2E, and runtime responsibilities match the Spec.
- [ ] Focused, module, cross-module, static/format, migration, full, and manual/runtime gates are sequenced with objective pass criteria and failure return points.
- [ ] Future validation is not misreported as already executed proof.

## Migration and release safety

- [ ] Historical immutable migrations are not modified.
- [ ] A single database change creates exactly one new next-version migration unless explicitly approved otherwise.
- [ ] Schema/data backfill, compatibility window, deployment order, feature flags, pre/post checks, rollback limits, and forward-fix are explicit or evidence-backed `N/A`.
- [ ] Permission, configuration, audit, logging, metrics, tracing, documentation, and operational files are included as applicable.

## Metadata and final gate

- [ ] Filename matches `YYYY-MM-DD-HH-MM-abstract.md`; header document/timestamps/status match it.
- [ ] All Spec/Plan relationship links are relative, valid, and revision-aware.
- [ ] No unresolved `TBD`, `TODO`, `FIXME`, vague placeholder, uncovered requirement, or internal contradiction remains in a `Review`/`Ready` Plan.
- [ ] A non-accepted/blocked Spec cannot produce a `Ready` Plan.
- [ ] Final verdict is exactly `PASS`, `BLOCKED`, or `REVISE` and matches reality.
- [ ] No source/test code, migration execution, service start, browser action, database change, or runtime claim occurred as a side effect.
