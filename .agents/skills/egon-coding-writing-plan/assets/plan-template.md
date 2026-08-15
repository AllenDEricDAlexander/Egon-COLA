# <Implementation Plan title>

| Field | Value |
| --- | --- |
| Document | `YYYY-MM-DD-HH-MM-abstract.md` |
| Status | `Draft` |
| Created | `YYYY-MM-DD HH:mm ZONE` |
| Updated | `YYYY-MM-DD HH:mm ZONE` |
| Owner | `<decision owner>` |
| Repository | `<repository>` |
| Scope | `<modules or bounded context>` |
| Source Requirement | `<user request / issue / ticket / brief>` |
| Baseline Revision | `<commit and branch, or explicit dirty-worktree snapshot>` |
| Implements Spec | [<primary Spec title>](../spec/YYYY-MM-DD-HH-MM-primary-spec.md) |
| Spec Status | `Review / Accepted` |
| Spec Revision | `<Spec Updated value and/or commit>` |
| Effective Specs | [<primary Spec>](../spec/YYYY-MM-DD-HH-MM-primary-spec.md) |
| Depends On Plans | `None` |
| Supersedes | `None` |
| Superseded By | `None` |
| Related Plans | `None` |

## 1. Summary

State which Spec this Plan implements, the coding scope, the overall dependency direction, the number of implementation Steps, and the evidence that will prove completion. Do not restate the entire design.

## 2. Target Spec and Effective Design

### 2.1 Primary target

- Path: `<relative link>`
- Status: `<actual status>`
- Revision: `<Updated timestamp and baseline commit>`
- Approval evidence: `<explicit decision or Draft-plan authorization>`

### 2.2 Effective Spec set

| Role | Spec/link | Status/revision | Effective sections | Why included |
| --- | --- | --- | --- | --- |
| Primary | `<link>` | `<...>` | `<all or exact sections>` | `<...>` |
| Amendment / Dependency / Replacement | `<link>` | `<...>` | `<exact sections>` | `<...>` |

### 2.3 Superseded or excluded content

Name content that is not effective for this Plan and the governing relationship. If none, write `None`.

## 3. Effective Requirements and Acceptance

| Requirement | Source Spec section | Effective statement | Observable acceptance | Implementation impact |
| --- | --- | --- | --- | --- |
| `REQ-001` | `<Spec link> §4` | `<verbatim or faithful requirement>` | `<observable result>` | `<modules/contracts/data/UI/tests>` |

Preserve source identifiers. When an eligible legacy Spec has no IDs, assign a Plan-local alias such as `PLAN-REQ-001` to an exact existing statement and say that it is a trace alias, not a new requirement.

## 4. Implementation Strategy and Dependency Order

### 4.1 Ordered strategy

Explain why the implementation sequence is compilable, testable, migration-safe, and compatible. Identify contract publication, generated-code, database, backend, frontend, configuration, and consumer dependencies as applicable.

### 4.2 Test-first strategy

Map each behavior to its RED test, minimum GREEN implementation, and permitted refactor/wiring work. State the expected reason each new/changed test fails before implementation.

### 4.3 Sequential and parallel boundaries

| Step | Depends on | May run in parallel with | Must not overlap with | Reason |
| --- | --- | --- | --- | --- |
| Step 1 | None | None | `<write scope>` | `<dependency>` |

### 4.4 Commit boundaries

Each Step normally produces one semantic, path-limited commit. Explain exceptions required by repository policy or inseparable cross-module compilation.

## 5. Change File Tree

```text
<complete target tree with CREATE / MODIFY / DELETE / RENAME / GENERATED markers>
```

| Operation | Path | Symbols | Responsibility | Step | Requirements |
| --- | --- | --- | --- | --- | --- |
| CREATE / MODIFY / DELETE / RENAME / GENERATED | `<exact repository-relative path>` | `<symbols>` | `<single responsibility>` | Step 1 | `REQ-001` |

Every affected file appears exactly once in this inventory. The tree must match the effective Spec or be covered by an evidence-backed `Plan Clarification` that preserves semantics.

## 6. Prerequisites, Constraints, and Plan Clarifications

### 6.1 Repository and worktree baseline

- Applicable repository instructions
- Branch/commit and dirty-worktree state
- Unrelated paths that must remain untouched
- Generated-file and path-limited commit rules

### 6.2 Build, test, and environment prerequisites

| Concern | Exact command/source | Required state | Validation boundary |
| --- | --- | --- | --- |
| Build tool | `<repository evidence/command>` | `<...>` | `<static/module/runtime>` |

### 6.3 Immutable constraints and approved decisions

List immutable migrations, public contracts, compatibility windows, security decisions, and other constraints from the Spec/repository.

### 6.4 Plan Clarifications

| ID | Small implementation inference | Repository evidence | Why semantics are unchanged | Impact if wrong |
| --- | --- | --- | --- | --- |
| `PLAN-CLAR-001` | `<...>` | `<path/symbol>` | `<...>` | `<...>` |

Write `None` when no clarification is needed. Never place new business/design decisions here.

## 7. Ordered File-by-file Implementation Steps

> Every Step is independently verifiable and normally commit-sized. Every marker below is required.

### Step 1 — <imperative, observable goal>

- Requirements: `REQ-001`
- Dependencies: `None / Step N`
- Observable outcome: `<behavior established by this Step>`
- Ordered files:

#### File 1 — `CREATE path/to/FocusedBehaviorTest.java`

- Purpose: Define the missing behavior before production implementation.
- Symbols: `<test class and test method names>`
- Why now: This is the RED contract for the Step.
- Contract/signature changes: `<test-visible production API and exact assertions>`
- Implementation pseudocode:

```java
@Test
void <behavior_name>() {
    // arrange repository-consistent fixtures and real collaborators
    // call the public production symbol named in the Spec
    // assert result, state transition, persisted/published effects, and error semantics
}
```

- After this file: The focused test compiles when possible and fails for the expected missing-behavior reason, not for a fixture or environment error.

#### File 2 — `MODIFY path/to/ProductionType.java`

- Purpose: Implement the minimum behavior required by File 1.
- Symbols: `<class, method, field, annotation>`
- Why now: The RED test fixes the desired public behavior.
- Contract/signature changes: `<exact method/field/error/transaction contract>`
- Implementation pseudocode:

```java
<ReturnType> <method>(<TypedInput> input) {
    validate <Spec-defined preconditions>
    load <state> through <existing repository/port>
    invoke <domain invariant or state transition>
    persist/publish through <named existing abstraction>
    map <domain result/error> to <repository-standard contract>
}
```

- After this file: The focused test reaches GREEN with the smallest Spec-compliant implementation; no unrelated behavior changes.

#### File 3 — `MODIFY path/to/WiringOrMappingFile.java`

- Purpose: Connect the implementation to its existing entry point or consumer.
- Symbols: `<configuration, mapper, controller, route, component>`
- Why now: The behavior exists and can be wired without speculative abstractions.
- Contract/signature changes: `<exact registration/mapping/prop/API change>`
- Implementation pseudocode:

```text
register or inject <named implementation>
map <exact source fields> to <exact target fields>
preserve <compatibility/error/permission branch>
expose the behavior only through <Spec-defined entry point>
```

- After this file: The Step's complete call path is connected and ready for focused verification.

- Verification command: `<exact repository command targeting this Step>`
- Expected result: `<test count, compilation result, generated diff, or observable contract>`
- Completion criteria: `<objective evidence for all Step requirements>`
- Rollback: `<path-limited revert/forward-fix point, or N/A with reason>`
- Commit: `<type(scope): semantic summary>`

### Step 2 — <next imperative goal>

Repeat the same structure. Do not replace exact file order with “update service, controller, and frontend.” Adapt pseudocode fences to Java, TypeScript, SQL, XML, YAML, shell, or the repository's actual language.

## 8. Test, Validation, and Quality Gates

| Gate/order | Command or method | Scope | Expected result | Failure returns to | Requirements |
| --- | --- | --- | --- | --- | --- |
| RED for Step 1 | `<focused command>` | `<test>` | Fails for stated missing behavior | File 1 | `REQ-001` |
| GREEN for Step 1 | `<focused command>` | `<test/module>` | Pass with no unexpected warnings | File 2/3 | `REQ-001` |
| Static/format | `<command>` | `<paths/module>` | No errors | Owning Step | All |
| Module regression | `<command>` | `<module>` | All relevant tests pass | Owning Step | All |
| Full/integration/manual | `<command or explicit steps>` | `<system boundary>` | `<observable result>` | `<Step>` | `<IDs>` |

State when to run focused, module, cross-module, full, migration, frontend, and manual/runtime gates. Do not claim runtime proof when the Plan only defines future validation.

## 9. Migration, Compatibility, Rollout, and Rollback

Define the exact order for applicable migration files, generated contracts, data backfill, dual-read/write, API/event compatibility, configuration, feature flags, deployment, post-deploy checks, rollback, and forward-fix. For Flyway, name only the new next-version file and preserve all historical migrations.

Write `N/A` with a target-Spec section and repository reason when no such work applies.

## 10. Requirement-to-Step Traceability Matrix

| Requirement | Effective Spec section | Steps | Files | Tests/gates | Completion evidence |
| --- | --- | --- | --- | --- | --- |
| `REQ-001` | `<Spec link> §...` | Step 1 | `<paths>` | `<test IDs/commands>` | `<artifact/output>` |

Every effective requirement must appear in at least one Step's `Requirements` line, not only in this matrix. Every Step/file must trace to a requirement or documented necessary infrastructure rationale.

## 11. Risks, Blockers, and User Decisions

| ID | Risk or decision | Impacted Steps/files | Evidence | Owner | Status/action |
| --- | --- | --- | --- | --- | --- |
| `BLOCK-001` | `<...>` | `<...>` | `<...>` | User | Open / Closed and action |

An unresolved major blocker forces Plan status `Draft` or `Blocked`; it cannot be `Review` or `Ready`.

## 12. Review and Acceptance

### 12.1 Original requirement fidelity

Confirm that the Plan covers every effective requirement and preserves the user's stated constraints and exclusions.

### 12.2 Spec consistency

Confirm the Plan does not redesign architecture, contracts, fields, state, schema, UI, tests, compatibility, or rollout. List every evidence-backed clarification.

### 12.3 Repository executability

Confirm every path/symbol/command against the current baseline, exact dependency order, isolated write scope, intermediate compilability, and commit boundary.

### 12.4 Test and release completeness

Confirm RED/GREEN order, requirement coverage, migration safety, compatibility, observability, rollout, rollback, and validation boundaries.

### 12.5 Final verdict

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — Spec or user decision required`
- `REVISE — Plan and Spec are inconsistent`
