# Per-Step Gate Checklist

Use this checklist for every Plan Step. Record objective evidence; do not replace failed or missing evidence with a narrative assertion.

## Step lock

- [ ] Exact Plan path and revision recorded.
- [ ] Step number, title, requirement IDs, dependencies, and proposed commit recorded.
- [ ] Effective Spec sections for this Step reread.
- [ ] `git rev-parse HEAD` recorded as the Step baseline.
- [ ] Current branch, `git status --short`, staged paths, and untracked paths inspected.
- [ ] Every earlier dependency is represented by an already verified commit.
- [ ] Declared Step paths do not overlap unrelated or concurrent edits.
- [ ] Planned files, symbols, consumers, migration sequence, and commands still exist.
- [ ] No material repository, Plan, or Spec drift is present.

## RED gate when the Step changes behavior

- [ ] The focused test is created or modified before production behavior.
- [ ] The exact focused command is run.
- [ ] The test fails for the expected missing behavior.
- [ ] Failure is not caused by compilation, fixture, dependency, environment, or unrelated errors.
- [ ] RED evidence is recorded. If RED is legitimately impossible, the approved reason is recorded.

## Implementation gate

- [ ] Only one Step is `In Progress`.
- [ ] Files are handled in the Plan's declared order.
- [ ] Every operation is limited to declared `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` paths.
- [ ] Implementation follows real repository APIs and conventions while preserving the Plan's semantics.
- [ ] No undeclared public contract, dependency, migration, schema, permission, page, or architecture change was introduced.
- [ ] No unrelated refactor, formatting sweep, debug output, secret, or generated noise was added.

## User-mandated Literal Rule gate

Read `references/user-mandated-java-rules.md`. Copy all rows into the Step record at Step lock and refresh them against the final diff before commit. Do not merge rows or substitute a general standards statement.

| Literal rule | Applicability | Status | Diff/path/symbol evidence | Test/static evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- | --- |
| Rule 1 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<type inventory and declarations>` | `<compile/name search>` | `<semantic suffix result>` | `None / action and owner` |
| Rule 2 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<every affected handoff/groups/normalization>` | `<positive/negative/group tests>` | `<layer validation result>` | `None / action and owner` |
| Rule 3 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<record/@Value/complete Lombok/Converter diff>` | `<compile/mapping tests/generated mapper>` | `<model/conversion result>` | `None / action and owner` |
| Rule 4 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<business classes/Bean names/Qualifier/lombok.config>` | `<compile/wiring/log review>` | `<logging/injection result>` | `None / action and owner` |
| Rule 5 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<imports/manifests/helpers>` | `<dependency/import search>` | `<allowlist result>` | `None / action and owner` |
| Rule 6 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<external contracts/Jackson annotations>` | `<serialization/compatibility tests>` | `<Jackson result>` | `None / action and owner` |
| Rule 7 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<base and every environment profile>` | `<normalized key-parity/config tests>` | `<profile structure result>` | `None / action and owner` |
| Rule 9 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<flow classification/pattern participants/wiring>` | `<pattern behavior/branch search>` | `<Complex pattern or Simple direct result>` | `None / action and owner` |
| Rule 10 | Applicable / Not applicable | PASS / N/A / FAIL / BLOCKED | `<time fields/imports/mappings>` | `<time tests/forbidden import search>` | `<java.time result>` | `None / action and owner` |
| Rule 11 | Applicable | PASS / FAIL / BLOCKED | `<Step paths/current and target tree/verifier>` | `<architecture/static/build gate>` | `<selected structure preserved>` | `None / action and owner` |

Literal Rule verdict rules:

- [ ] Original order is exactly Rule 1, 2, 3, 4, 5, 6, 7, 9, 10, 11; no Rule 8 is invented.
- [ ] Rule 11 is always `Applicable` and `PASS` before a completion commit.
- [ ] Every applicable rule is `PASS` with fresh final-diff evidence.
- [ ] Every `N/A` has positive scope evidence and does not hide a touched violation.
- [ ] Tests/compile/static searches supplement manual inspection and cannot waive a rule.
- [ ] Any missing row/evidence, `FAIL`, `BLOCKED`, `UNKNOWN`, reduced mandatory implementation, or silent exception prohibits `Verified` and commit-as-complete.

## Blocking Manual Check gate

Read `references/java-spring-egon-coding-standards.md`. Copy all stable IDs below into the Step record and evaluate them one by one after implementation and again immediately before commit. `Applicable` permits `PASS`, `FAIL`, or `BLOCKED`; `Not applicable` permits only `N/A`. Every row requires concrete evidence and a finding. A failed/blocked row requires an exact action and owner.

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<tree/module/dependency evidence>` | `<profile preserved or impact absent>` | `<None or action/owner>` |
| `MC-REUSE-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<search paths/resolved capability>` | `<reuse conclusion>` | `<None or action/owner>` |
| `MC-DEP-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<manifest/diff/gap approval>` | `<dependency conclusion>` | `<None or action/owner>` |
| `MC-NAME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<touched type list/search>` | `<semantic-name conclusion>` | `<None or action/owner>` |
| `MC-VALID-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<constraints/groups/normalizer/tests>` | `<boundary coverage>` | `<None or action/owner>` |
| `MC-MODEL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<record/@Value/complete complex Lombok diff>` | `<construction/conflict conclusion>` | `<None or action/owner>` |
| `MC-CONVERT-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<MapStruct + mandatory BaseConverter diff/search>` | `<mapping/no-bypass conclusion>` | `<None or action/owner>` |
| `MC-LOG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<business-class/log diff>` | `<logging conclusion>` | `<None or action/owner>` |
| `MC-BEAN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<Bean names/Qualifier/lombok.config>` | `<injection conclusion>` | `<None or action/owner>` |
| `MC-UTIL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<imports/dependencies/helper search>` | `<utility conclusion>` | `<None or action/owner>` |
| `MC-JSON-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<JSON imports/contract annotations/tests>` | `<Jackson conclusion>` | `<None or action/owner>` |
| `MC-TIME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<time types/converters/tests>` | `<java.time conclusion>` | `<None or action/owner>` |
| `MC-CONFIG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<all profile key comparisons/properties>` | `<configuration parity>` | `<None or action/owner>` |
| `MC-PATTERN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<flow classification/pattern participant evidence>` | `<mandatory Complex pattern / Simple direct conclusion>` | `<None or action/owner>` |
| `MC-SCOPE-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<Step diff/status and untouched paths>` | `<scope conclusion>` | `<None or action/owner>` |
| `MC-TEST-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<actual commands/output/static review>` | `<standards/behavior proof>` | `<None or action/owner>` |
| `MC-BLOCKER-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<all rows and Step blockers>` | `<closure conclusion>` | `<None or exact unresolved action/owner>` |

Manual Check verdict rules:

- [ ] Every stable ID appears exactly once; no row is merged into another.
- [ ] Every applicable concern is `PASS` with current Step evidence.
- [ ] Every `N/A` has positive scope evidence and an explicit reason; `N/A` is not used to hide a violation.
- [ ] Static checks/tests supplement, but do not replace, line-by-line manual review.
- [ ] `MC-BLOCKER-001` is `PASS` only when all other rows are `PASS` or valid `N/A`.
- [ ] Any `FAIL`, `BLOCKED`, `UNKNOWN`, missing ID/evidence, or unresolved exception keeps the Step `In Progress`/`Blocked` and prohibits a completion commit.

## Verification gate

- [ ] Exact focused validation passed with complete output and exit status.
- [ ] Required compile, typecheck, lint/format, XML/schema, module, integration, or regression gates passed.
- [ ] Expected GREEN behavior and relevant error paths are covered.
- [ ] Current Step requirements and effective Spec sections were reread against the implementation.
- [ ] All Literal Rule rows are `PASS` or evidence-backed `N/A`, with Rule 11 `PASS`; evidence was refreshed after the final diff.
- [ ] All Step Manual Check rows are `PASS` or evidence-backed `N/A`; evidence was refreshed after the final diff.
- [ ] `git diff --check -- <Step paths>` passed.
- [ ] Failures, skips, warnings, timeouts, and unavailable runtime evidence are classified honestly.

## Pre-commit gate

- [ ] `git diff -- <Step paths>` was reviewed line by line.
- [ ] `git status --short` was reviewed again.
- [ ] Only explicit Step paths were staged; broad staging was not used.
- [ ] `git diff --cached --check` passed for the Step paths.
- [ ] Cached `--stat` and `--name-only` match the declared Step scope.
- [ ] The commit message is semantic and matches the Plan proposal or repository convention.
- [ ] The commit is non-empty.
- [ ] The final Literal Rule verdict is PASS; no mandatory rule was reduced or waived because tests passed.
- [ ] The final pre-commit Manual Check verdict is PASS; no blocker was waived in prose.

## Post-commit gate

- [ ] Commit succeeded and its hash was recorded.
- [ ] `git show --stat --oneline <hash>` was inspected.
- [ ] The committed file list contains only Step-owned paths.
- [ ] Validation commands and observed results are recorded against the hash.
- [ ] Deviations are recorded as `None`, approved clarification, or corrective-commit explanation.
- [ ] Unrelated staged, unstaged, and untracked work remains preserved.
- [ ] The Step is marked `Committed` before any later Step begins.

If any required box cannot be checked, keep the Step `In Progress` or mark it `Blocked`; do not advance.
