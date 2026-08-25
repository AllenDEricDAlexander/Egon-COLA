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

## Spec simplicity and implementation-necessity audit

- [ ] Every new/expanded API, class, layer, table, cache, job, page, dependency, and frontend state container maps to a Spec necessity verdict and current requirement.
- [ ] Current repository evidence and the direct/reuse alternative were rechecked before assigning files.
- [ ] No Step implements a fetch-then-forward API or caller-supplied context that the target can derive/validate; material findings return to the Spec rather than being “clarified.”
- [ ] No speculative pattern, mapper, model, cache, abstraction, compatibility layer, or future extension appears without an approved present need.
- [ ] The Plan exposes added interactions, states, files, failures, migrations, and operations instead of using implementation detail to hide complexity.

## Repository executability

- [ ] Applicable `AGENTS.md`, current commit/branch, dirty worktree, and concurrent changes were inspected.
- [ ] Every Create/Modify/Delete/Rename/Generated path appears once in the inventory and follows repository layout.
- [ ] Existing paths and symbols were verified; new paths/names follow nearby style and module boundaries.
- [ ] Consumers, registration/wiring, generated sources, build order, migrations, and cross-module dependencies are represented.
- [ ] Exact commands come from repository build/scripts rather than guesses.
- [ ] Every validation gate names its working directory, exact command/method and selectors/environment, expected exit/result, failure return point, and static/module/runtime boundary.
- [ ] Unrelated work is protected by non-overlapping, path-limited scopes and commits.

## Java, Spring, Egon-COLA, and Manual Check gate

- [ ] The exact source block from `references/user-mandated-java-rules.md` remains in both entrypoints; Chapter 4 contains Rule 1, 2, 3, 4, 5, 6, 7, 9, 10, and 11 as separate rows in that order.
- [ ] Every Step has `Literal Rules:` and includes Rule 11; every affected file has `Literal rule enforcement:` with exact Rule numbers and concrete implementation evidence.
- [ ] Rule 2 plans every affected layer handoff separately, not just Controller validation, including groups, `ValidationUtils`, telephone normalization, errors, and tests.
- [ ] Rule 3 plans Record/`@Value`/the complete complex-class Lombok baseline and mandatory MapStruct/MapStructPlus plus Egon `BaseConverter`; conflicts are returned to the Spec/user.
- [ ] Rule 4 plans `@Slf4j`, explicit Bean names, `@RequiredArgsConstructor`, per-dependency `@Qualifier`, and `lombok.config` propagation in exact files.
- [ ] Rule 9 assigns a concrete pattern and participant file order to every Complex flow; only evidenced Simple flows remain direct.
- [ ] For Java work, `references/java-spring-egon-coding-standards.md` was read completely and the actual tree was classified as exactly one traditional-layer or exact selected Archetype profile; no hybrid/new layer was invented.
- [ ] The capability reuse ledger names Spring/Spring Boot Starter, Egon-COLA Component/common infrastructure, and module-local candidates with exact evidence, fit/gap, decision, and owning Step.
- [ ] Every added dependency/custom replacement has an effective-Spec decision, proven gap, managed version/owner, impact, and validation; otherwise the Plan reuses existing capability.
- [ ] New/changed Java type names have semantic PO/BO/DTO/VO/Query/Command/Event/Request/Response or behavior roles and do not introduce ambiguous `Data`, `Info`, `Param`, or `Bean` carriers.
- [ ] Each affected cross-layer input plans Jakarta/Spring Validation, `@Valid`/`@Validated`, groups for reused objects, approved normalization, error mapping, and tests; telephone rules use an approved mature standard rather than a duplicate Validator.
- [ ] Each object uses the mandated Record/`@Value`/complete complex-class Lombok classification; every new affected Converter uses MapStruct/MapStructPlus and mandatory Egon `BaseConverter` without manual/BeanUtils/JSON copying.
- [ ] Each affected Spring Bean plans a stable explicit name, `@RequiredArgsConstructor`, final qualified dependencies, verified `lombok.config` propagation, and `@Slf4j` for concrete business classes.
- [ ] Utility, Jackson-only JSON, `java.time`, `@ConfigurationProperties`, and all-environment key-parity consequences appear in exact files/pseudocode/tests.
- [ ] Pattern decisions assign an actual pattern to every Complex flow; only Simple flow evidence permits direct logic, and neither hard-coded complexity nor ceremonial abstraction remains.
- [ ] Every Step lists applicable `MC-*` IDs, every file has an evidence-bearing `Standards impact`, and its validation proves the checks before commit.
- [ ] Chapter 12 contains all stable Manual Check IDs exactly once; every applicable row is `PASS`, every `N/A` has evidence/reason, and `MC-BLOCKER-001` matches all unresolved rows.

## Step and pseudocode quality

- [ ] Every Step has source requirements, dependencies, baseline/end state, one observable outcome, a `Required` or evidence-backed `Not applicable` test-first gate, ordered files, validation working directory, verification, expected result, completion criteria, rollback, exact commit paths, and one commit.
- [ ] Every file has operation, exact path, symbols, purpose, repository evidence, dependencies/consumers, sequence reason, contract/signature changes, input/output/state mapping, error/edge behavior, language-appropriate pseudocode, verification contribution, and after-file state.
- [ ] Pseudocode names real methods/types/fields/calls/branches/errors/transactions/assertions rather than generic actions.
- [ ] Pseudocode is concrete enough to define control flow and mapping while leaving only syntax—not architecture or behavior choices—to implementation.
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
- [ ] A PASS verdict has no missing evidence, `FAIL`, `BLOCKED`, `UNKNOWN`, unresolved exception, or open Manual Check.
- [ ] No source/test code, migration execution, service start, browser action, database change, or runtime claim occurred as a side effect.
