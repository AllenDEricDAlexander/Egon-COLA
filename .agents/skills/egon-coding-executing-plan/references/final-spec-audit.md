# Final Spec Conformance Audit

Perform this audit after all Plan Steps have verified commits. Treat it as an independent review of the repository, not a confirmation of the Plan author's assumptions.

## 1. Freeze the audit baseline

- Record current branch and HEAD.
- Record the exact Plan path, status, revision, and `Implements Spec` reference.
- Resolve the complete final `Effective Specs` set, including amendments, supersessions, and dependencies.
- Record every Step commit and any approved corrective commit.
- Confirm unrelated work remains outside the delivery commits.

## 2. Build a requirement inventory

Extract individually auditable rows for:

- user requirements and acceptance criteria;
- architecture, module/package, and dependency boundaries;
- interfaces, request/response fields, error semantics, and compatibility;
- entities, invariants, state transitions, persistence, schema, migrations, and rollback;
- frontend routes, pages, states, permissions, accessibility, and API integration;
- unit, integration, contract, migration, UI, regression, and runtime verification;
- security, performance, observability, concurrency, idempotency, deployment, and other non-functional constraints;
- explicit non-goals and out-of-scope behavior.

Do not merge unrelated requirements into one row merely to reduce the table size.

## 3. Map evidence

Use this matrix:

| Requirement ID | Effective Spec source | Expected behavior | Commit evidence | Code/symbol evidence | Test/validation evidence | Status | Gap/impact/next action |
| --- | --- | --- | --- | --- | --- | --- | --- |

Evidence rules:

- A commit hash alone does not prove behavior; include paths and symbols.
- Source inspection alone does not prove a required test; include the actual command and result.
- Unit/module evidence does not prove live integration, deployment, credentials, data, browser, or external-provider behavior.
- Generated output is not authoritative when the repository defines a source configuration or generator input.
- If a validation was skipped, timed out, lost, or only partially observed, do not mark it passed.

## 4. Assign exactly one status

- `Satisfied`: the implementation and all authorized non-runtime evidence required by the Spec are present and consistent.
- `Partial`: only part of the behavior or evidence is present.
- `Not satisfied`: the behavior is absent, contradicts the Spec, violates a boundary, or regresses a non-goal.
- `Runtime unverified`: source/module evidence is present, but the Spec explicitly requires user-controlled live-system proof that was not performed.

Do not downgrade a concrete implementation defect to `Runtime unverified`. That status is only for otherwise plausible implementation whose remaining proof is inherently runtime-bound.

## 5. Run final gates

- Run every safe, authorized final command listed by the Plan.
- Add only the smallest repository-standard regression checks needed for affected consumers.
- Inspect complete output, exit status, skips, warnings, and generated changes.
- Do not start services, databases, browsers, deployment, or external systems without explicit authorization.
- Recheck `git status --short` after validation and identify any generated or modified files.

## 6. Audit delivery discipline

- Every Plan Step has a verified non-empty commit.
- Commit order matches Step order.
- Each commit scope matches its Step paths.
- Corrective commits are explicit and attributed; history was not silently rewritten.
- No required Plan file or validation gate was skipped.
- No unrelated work was committed.
- No immutable migration was edited.

## 7. Re-run the blocking Manual Check

Read `references/java-spring-egon-coding-standards.md` and inspect the final tree plus all delivery commits. Do not copy the Plan's predicted statuses or merely aggregate Step assertions. Execute every row independently with current evidence.

| Check ID | Applicability | Status | Evidence | Finding | Required action/exception |
| --- | --- | --- | --- | --- | --- |
| `MC-ARCH-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<final tree/archetype/dependency evidence>` | `<architecture result>` | `<None or action/owner>` |
| `MC-REUSE-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<reuse ledger plus final paths>` | `<reuse result>` | `<None or action/owner>` |
| `MC-DEP-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<final manifests/gap approval>` | `<dependency result>` | `<None or action/owner>` |
| `MC-NAME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<changed Java types/search>` | `<naming result>` | `<None or action/owner>` |
| `MC-VALID-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<boundaries/groups/tests>` | `<validation result>` | `<None or action/owner>` |
| `MC-MODEL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<record/class/Lombok inspection>` | `<model result>` | `<None or action/owner>` |
| `MC-CONVERT-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<converter generation/search/tests>` | `<conversion result>` | `<None or action/owner>` |
| `MC-LOG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<business-class/log inspection>` | `<logging result>` | `<None or action/owner>` |
| `MC-BEAN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<Bean names/injection/Qualifier evidence>` | `<Bean result>` | `<None or action/owner>` |
| `MC-UTIL-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<imports/dependencies/helper inventory>` | `<utility result>` | `<None or action/owner>` |
| `MC-JSON-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<JSON imports/contracts/tests>` | `<JSON result>` | `<None or action/owner>` |
| `MC-TIME-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<time types/boundaries/tests>` | `<time result>` | `<None or action/owner>` |
| `MC-CONFIG-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<all profile key comparisons>` | `<configuration result>` | `<None or action/owner>` |
| `MC-PATTERN-001` | `Applicable / Not applicable` | `PASS / N/A / FAIL / BLOCKED` | `<variation/branch/participant inspection>` | `<pattern result>` | `<None or action/owner>` |
| `MC-SCOPE-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<commit scopes/final diff/non-goals>` | `<scope result>` | `<None or action/owner>` |
| `MC-TEST-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<commands/output/static checks>` | `<proof result>` | `<None or action/owner>` |
| `MC-BLOCKER-001` | `Applicable` | `PASS / FAIL / BLOCKED` | `<all rows/requirements/open risks>` | `<closure result>` | `<None or exact unresolved action/owner>` |

Gate rules:

- every stable ID appears exactly once;
- every applicable row is `PASS` with concrete final evidence;
- `N/A` requires positive scope evidence and reason;
- `MC-BLOCKER-001` is `PASS` only when every other row is `PASS` or valid `N/A`;
- any failed/blocked/unknown/missing/unsupported row prohibits final PASS and must be reported, not silently fixed during the audit.

## 8. Determine the verdict

- `PASS — Implementation conforms to the effective Specs`: every requirement is `Satisfied`, no required runtime evidence is missing, and every applicable Manual Check is `PASS` with all other rows evidence-backed `N/A`.
- `PARTIAL — Spec requirements are unmet or unverified`: one or more requirement rows are `Partial`, `Not satisfied`, or `Runtime unverified`, or one or more Manual Checks are failed, blocked, unknown, missing, or unsupported.
- `BLOCKED — Final verification could not be completed`: the effective baseline, repository evidence, or required safe validation cannot be determined.

List every non-passing row with its evidence, impact, and recommended corrective Plan/Step. Do not silently implement a newly discovered gap during this audit.
