# <Implementation Plan title>

| Field | Value |
| --- | --- |
| Document | `YYYY-MM-DD-HH-MM-abstract.md` |
| Template Version | `4` |
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

### 4.5 Spec Simplicity and Implementation-necessity Audit

Read `references/file-by-file-planning.md`. Verify the effective Spec before compiling its elements into files. Do not redesign here; return material defects to the Spec/user.

| Spec element | Spec necessity verdict/section | Current repository evidence | Direct/reuse alternative | Interaction/implementation cost | Plan decision |
| --- | --- | --- | --- | --- | --- |
| `<API/class/table/cache/job/page/dependency>` | `<Spec link §...>` | `<path/symbol/consumer>` | `<reuse/derive/merge>` | `<calls/state/files/failures/operations>` | Implement / Already exists / Return to Spec |

Explicitly check for fetch-then-forward APIs, caller-supplied values derivable from trusted context, speculative patterns/caches/layers, duplicate models/mappers, and direct alternatives that satisfy the same requirements. A material problem forces `REVISE` or `BLOCKED`; Plan detail is not justification.

### 4.6 Change-unit Dependency Matrix

| Change unit | Requirements | Proof/RED point | Compile/runtime prerequisites | Produces | Consumers/unblocks | Owning Step |
| --- | --- | --- | --- | --- | --- | --- |
| `<behavior/schema/contract/UI unit>` | `REQ-001` | `<test/gate>` | `<types/schema/previous Step>` | `<symbols/contracts/state>` | `<next files/Steps>` | Step 1 |

Use this matrix to derive Step and file order. Do not group independent outcomes merely because they share a module.

### 4.7 Java, Spring, and Egon-COLA Implementation Standards

For Java work, read `references/user-mandated-java-rules.md` and `references/java-spring-egon-coding-standards.md`, then complete both tables. For a non-Java Plan, retain the subsection and write an evidence-backed `N/A`; the Manual Check table still applies to scope, tests, and blockers.

| Concern | Current repository evidence | Effective Spec decision | Planned implementation consequence | Owning Steps/checks |
| --- | --- | --- | --- | --- |
| Architecture profile | `<actual tree, selected traditional or exact Archetype evidence>` | `<Spec section>` | `<preserved modules/packages/dependency direction>` | `Step N; MC-ARCH-001` |
| Reuse/capability | `<Spring/Starter/Egon/module candidate path or dependency>` | `<reuse/gap decision>` | `<exact reused type/configuration or approved addition>` | `Step N; MC-REUSE-001 / MC-DEP-001` |
| Naming/model/validation/conversion | `<nearby types, ValidationUtils, BaseConverter, MapStruct evidence>` | `<Spec sections>` | `<exact suffixes, groups, record/Lombok, converter contract>` | `<Steps and MC IDs>` |
| Bean/logging/util/JSON/time/config | `<stereotypes, lombok.config, profiles, dependencies>` | `<Spec sections>` | `<exact annotations, libraries, types, profile files>` | `<Steps and MC IDs>` |
| Business variation/pattern | `<current branch/rule/state evidence>` | `<mandatory pattern for Complex / direct only for Simple>` | `<participants, selection/wiring, or Simple evidence>` | `Step N; MC-PATTERN-001` |

#### Capability reuse ledger

| Need | Candidates inspected | Exact evidence | Fit/gap | Decision | Added dependency/custom code | Owning Step/check |
| --- | --- | --- | --- | --- | --- | --- |
| `<capability>` | `<Spring / Starter / Egon / module candidates>` | `<paths, resolved dependency, symbols>` | `<why sufficient or exact missing behavior>` | `<reuse / approved addition / blocked>` | `None / exact coordinate and impact` | `Step N; MC-REUSE-001 / MC-DEP-001` |

Any unresolved architecture selection, duplicate capability, dependency gap, or Spec conflict must appear in §11 and forces a non-PASS verdict.

### 4.8 User-mandated Java Rule Implementation Matrix

Read `references/user-mandated-java-rules.md`. Preserve each original rule independently and in exact numbering order. Do not merge rows into broad “code quality” work. A non-Java Plan retains every row with evidence-backed `N/A`.

| Literal rule | Spec source | Repository evidence | Exact files and order | Pseudocode obligations | Validation gate | Steps | Status/blocker |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | `<Spec §6.2/§8/§10>` | `<all affected Java types>` | `<ordered type/consumer files>` | `<mandatory suffixes and reuse>` | `<inventory/compile/search>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 2 | `<Spec §6.2/§7/§9/§10>` | `<all affected layer handoffs>` | `<tests, groups, boundary files>` | `<@Valid/@Validated/group/ValidationUtils/libphonenumber>` | `<positive/negative/group tests>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 3 | `<Spec §6.2/§10>` | `<objects/converters/BaseConverter>` | `<tests, model, converter, consumers>` | `<record/@Value/complete Lombok baseline/MapStruct>` | `<constructor/mapping compile/tests>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 4 | `<Spec §6.2/§7/§13>` | `<business Beans/lombok.config>` | `<tests, business files, config>` | `<@Slf4j/name/@RequiredArgsConstructor/@Qualifier>` | `<compile/wiring/log review>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 5 | `<Spec §6.2/§15>` | `<imports/dependencies/helpers>` | `<affected files/manifests>` | `<closed allowlist only>` | `<dependency/import search>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 6 | `<Spec §6.2/§9/§10>` | `<external JSON contracts>` | `<contract/tests>` | `<Jackson annotation/default decisions>` | `<serialization/compatibility tests>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 7 | `<Spec §6.2/§15>` | `<all profile files>` | `<base + every environment + properties/tests>` | `<identical key structure>` | `<key-parity/config tests>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 9 | `<Spec §6.2/§13>` | `<flow complexity/variation>` | `<pattern participants/wiring/tests>` | `<mandatory pattern for Complex>` | `<branch/pattern behavior tests>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 10 | `<Spec §6.2/§10/§15>` | `<time fields/imports>` | `<model/converter/persistence/JSON/tests>` | `<java.time/zone/precision/Clock>` | `<time tests + forbidden import search>` | `<Step N>` | PASS / N/A / BLOCKED |
| Rule 11 | `<Spec §6.1/§6.2/§8>` | `<current tree/archetype/verifier>` | `<every target file in selected profile>` | `<no hybrid/third structure>` | `<architecture verifier/static gate>` | `Every Step` | PASS / BLOCKED |

## 5. Change File Tree

```text
<complete target tree with CREATE / MODIFY / DELETE / RENAME / GENERATED markers>
```

| Operation | Path | Current evidence/symbol | Final symbols/state | Responsibility | Step | Requirements | Validation owner |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CREATE / MODIFY / DELETE / RENAME / GENERATED | `<exact repository-relative path>` | `<path/symbol or absence/search evidence>` | `<exact final symbols/state>` | `<single responsibility>` | Step 1 | `REQ-001` | `<test/gate>` |

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
- Baseline state: `<current passing behavior/files and required committed predecessors>`
- Observable outcome: `<behavior established by this Step>`
- End state: `<exact contracts/files/tests now available and intentionally remaining work>`
- Test-first gate: `Required — <expected RED reason> / Not applicable — <repository or technical evidence>`
- Manual Checks: `MC-ARCH-001, MC-...` — list every standard affected by this Step; use `MC-SCOPE-001` and `MC-TEST-001` for every coding Step
- Literal Rules: `Rule 11, Rule ...` — list every original rule implemented or verified by this Step; Rule 11 applies to every Step containing repository files
- Ordered files:

#### File 1 — `CREATE path/to/FocusedBehaviorTest.java`

- Purpose: Define the missing behavior before production implementation.
- Symbols: `<test class and test method names>`
- Repository evidence: `<existing test path, fixture, naming, assertion, and framework pattern>`
- Dependencies and consumers: `<production symbol under test, fixtures, test runtime/module>`
- Why now: This is the RED contract for the Step.
- Contract/signature changes: `<test-visible production API and exact assertions>`
- Input/output and state mapping: `<fixture fields/context -> call -> result/state/side effects>`
- Error and edge behavior: `<invalid/missing/duplicate/concurrent/failure assertions and negative effects>`
- Standards impact: `<applicable MC-* IDs; exact naming, validation/group, modeling, converter, Bean/logging, JSON/time/config/pattern consequences or evidence-backed N/A>`
- Literal rule enforcement: `<exact Rule N values and the concrete mandatory behavior/evidence in this file>`
- Implementation pseudocode:

```java
@Test
void <behavior_name>() {
    // arrange repository-consistent fixtures and real collaborators
    // call the public production symbol named in the Spec
    // assert result, state transition, persisted/published effects, and error semantics
}
```

- Verification contribution: `<exact RED/GREEN test selector and what this file proves>`
- After this file: The focused test compiles when possible and fails for the expected missing-behavior reason, not for a fixture or environment error.

#### File 2 — `MODIFY path/to/ProductionType.java`

- Purpose: Implement the minimum behavior required by File 1.
- Symbols: `<class, method, field, annotation>`
- Repository evidence: `<existing implementation path, neighboring method, transaction/error/style convention>`
- Dependencies and consumers: `<callers, collaborators, DAO/client, configuration, downstream consumers>`
- Why now: The RED test fixes the desired public behavior.
- Contract/signature changes: `<exact method/field/error/transaction contract>`
- Input/output and state mapping: `<trusted context/request/model/column -> result and state effects>`
- Error and edge behavior: `<validation/permission/missing/duplicate/concurrency/dependency/rollback branches>`
- Standards impact: `<applicable MC-* IDs and exact repository-consistent annotations/contracts; include reuse/dependency decision>`
- Literal rule enforcement: `<exact Rule N values; do not replace required annotations, BaseConverter, Validation, or patterns with preferences>`
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

- Verification contribution: `<which focused/integration assertion observes this implementation>`
- After this file: The focused test reaches GREEN with the smallest Spec-compliant implementation; no unrelated behavior changes.

#### File 3 — `MODIFY path/to/WiringOrMappingFile.java`

- Purpose: Connect the implementation to its existing entry point or consumer.
- Symbols: `<configuration, mapper, controller, route, component>`
- Repository evidence: `<existing registration/export/route/mapping pattern and path>`
- Dependencies and consumers: `<implementation, DI/container/router/client/page/generated consumer>`
- Why now: The behavior exists and can be wired without speculative abstractions.
- Contract/signature changes: `<exact registration/mapping/prop/API change>`
- Input/output and state mapping: `<source -> target mapping, defaults, nulls, frontend/server state>`
- Error and edge behavior: `<missing registration, denied/error/loading/compatibility behavior>`
- Standards impact: `<applicable MC-* IDs; stable Bean name/Qualifier/configuration/profile/contract consequences>`
- Literal rule enforcement: `<exact Rule N values and the concrete wiring/mapping/configuration proof>`
- Implementation pseudocode:

```text
register or inject <named implementation>
map <exact source fields> to <exact target fields>
preserve <compatibility/error/permission branch>
expose the behavior only through <Spec-defined entry point>
```

- Verification contribution: `<call-path/contract/component test and assertion>`
- After this file: The Step's complete call path is connected and ready for focused verification.

- Validation working directory: `<exact repository/module path>`
- Verification command: `<exact repository command targeting this Step>`
- Expected result: `<test count, compilation result, generated diff, or observable contract>`
- Failure returns to: `<File N / earlier Step / Spec decision, with trigger>`
- Completion criteria: `<objective evidence for all Step requirements>`
- Rollback: `<path-limited revert/forward-fix point, or N/A with reason>`
- Commit paths: `<exact Step-owned repository-relative paths>`
- Commit: `<type(scope): semantic summary>`

### Step 2 — <next imperative goal>

Repeat the same structure. Do not replace exact file order with “update service, controller, and frontend.” Adapt pseudocode fences to Java, TypeScript, SQL, XML, YAML, shell, or the repository's actual language.

## 8. Test, Validation, and Quality Gates

| Gate/order | Working directory | Command or method | Scope | Expected result | Failure returns to | Requirements/runtime boundary |
| --- | --- | --- | --- | --- | --- | --- |
| RED for Step 1 | `<cwd>` | `<focused command>` | `<test>` | Fails for stated missing behavior | File 1 | `REQ-001`; module |
| GREEN for Step 1 | `<cwd>` | `<focused command>` | `<test/module>` | Exit 0; named tests pass without unexpected warnings | File 2/3 | `REQ-001`; module |
| Static/format | `<cwd>` | `<command>` | `<paths/module>` | Exit 0; no errors | Owning Step | All; static |
| Module regression | `<cwd>` | `<command>` | `<module>` | All relevant tests pass | Owning Step | All; module |
| Full/integration/manual | `<cwd/system>` | `<command or explicit steps>` | `<system boundary>` | `<observable result>` | `<Step>` | `<IDs>`; user-controlled runtime when applicable |

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

Confirm the Spec simplicity/necessity audit found no fetch-then-forward interface or other material overdesign. If it did, use `REVISE`/`BLOCKED` and identify the exact Spec sections rather than planning the flawed element.

### 12.3 Repository executability

Confirm every path/symbol/command against the current baseline, exact dependency order, isolated write scope, per-file evidence/mapping/error behavior, intermediate RED/GREEN/compilability state, validation working directory, and exact commit boundary.

### 12.4 Test and release completeness

Confirm RED/GREEN order, requirement coverage, migration safety, compatibility, observability, rollout, rollback, and validation boundaries.

### 12.5 Blocking Manual Check

Complete every row individually after reconciling the Plan with the effective Specs and current repository. `Applicable` permits only `PASS`, `FAIL`, or `BLOCKED`; `Not applicable` permits only `N/A`. Evidence and finding are mandatory for every row. A failed/blocked row requires an exact action and owner.

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<actual tree and selected profile>` | `<one profile preserved>` | `<None or action/owner>` |
| `MC-REUSE-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<reuse ledger and paths>` | `<reuse conclusion>` | `<None or action/owner>` |
| `MC-DEP-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<dependency evidence or proof none added>` | `<gap/impact conclusion>` | `<None or action/owner>` |
| `MC-NAME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<planned type inventory>` | `<semantic suffix conclusion>` | `<None or action/owner>` |
| `MC-VALID-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<boundary/group/normalization plan>` | `<coverage conclusion>` | `<None or action/owner>` |
| `MC-MODEL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<record/@Value/complete Lombok baseline>` | `<construction or blocking conflict>` | `<None or action/owner>` |
| `MC-CONVERT-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<MapStruct + mandatory BaseConverter plan>` | `<mapping/no-bypass conclusion>` | `<None or action/owner>` |
| `MC-LOG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<business-class files>` | `<logging conclusion>` | `<None or action/owner>` |
| `MC-BEAN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<Bean names/injection/Qualifier/lombok.config>` | `<DI conclusion>` | `<None or action/owner>` |
| `MC-UTIL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<utility dependency/file evidence>` | `<utility conclusion>` | `<None or action/owner>` |
| `MC-JSON-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<contract/serializer evidence>` | `<Jackson conclusion>` | `<None or action/owner>` |
| `MC-TIME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<time fields/converters/contracts>` | `<java.time conclusion>` | `<None or action/owner>` |
| `MC-CONFIG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<all profile files/properties class>` | `<key-parity conclusion>` | `<None or action/owner>` |
| `MC-PATTERN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<flow classification/pattern files>` | `<mandatory Complex pattern / Simple direct conclusion>` | `<None or action/owner>` |
| `MC-SCOPE-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<change tree and untouched paths>` | `<scope conclusion>` | `<None or action/owner>` |
| `MC-TEST-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<Step validation matrix>` | `<standards proof conclusion>` | `<None or action/owner>` |
| `MC-BLOCKER-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<§11 plus all rows above>` | `<closure conclusion>` | `<None or exact unresolved owner/action>` |

### 12.6 Final verdict

Use exactly one:

- `PASS — Ready for user review`
- `BLOCKED — Spec or user decision required`
- `REVISE — Plan and Spec are inconsistent`
