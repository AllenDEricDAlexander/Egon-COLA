---
name: egon-coding-executing-plan
description: Use when an approved coding Plan must be implemented in a repository one Step at a time, with validation and a separate commit after every completed Step, followed by a final conformance audit against the effective Specs.
---

# EGON Coding Plan Execution

## Purpose

Execute one approved coding Plan sequentially. Complete, verify, and commit exactly one Plan Step before starting the next. After all Steps, audit the delivered repository against the effective Specs and report every unmet, partial, or runtime-unverified requirement.

The Plan controls implementation order. The effective Specs control correctness. A completed Plan is not sufficient when the implementation still violates a Spec.

## Entry conditions and execution authorization

Before modifying code:

1. Identify one exact Plan path and read it completely.
2. Resolve and read the primary `Implements Spec` plus every `Effective Specs` document and governing amendment, supersession, or dependency.
3. Read all applicable `AGENTS.md` files and repository instructions.
4. Confirm the Plan revision and repository baseline. Reinspect every current path, symbol, migration sequence, consumer, and validation command used by the next Step.
5. Confirm execution authorization:
   - normally the Plan is `Ready` and the primary Spec is `Accepted` or `Implemented`; or
   - the user explicitly authorizes execution of the exact Plan and Spec revisions in the current conversation.
6. Run the Plan structural validator when the Plan skill provides one, for example `egon-coding-writing-plan/scripts/validate_plan.py <plan-path> --strict`.
7. Inspect `git status`, the current branch/HEAD, staged changes, and untracked files. Preserve all unrelated work.

Stop before implementation when the Plan target is ambiguous, authorization is absent, an effective Spec conflicts, a major decision is open, the Plan is structurally invalid, or repository drift changes architecture, behavior, contracts, data, security, migration, compatibility, or Step ownership.

Do not start services, browsers, databases, local stacks, or long-running runtime tests unless the user explicitly requests them. Source, compile, static, and focused/module test execution remains allowed when specified by the Plan.

## Step state machine

Treat each Plan Step as a strict state transition:

```text
Pending -> In Progress -> Verified -> Committed
              |              |
              +-> Blocked <--+
```

- `Pending`: no Step-owned edits have begun.
- `In Progress`: only the current Step's files may be changed.
- `Verified`: every required Step validation passed with fresh evidence and the diff review passed.
- `Committed`: one path-limited semantic commit exists and its hash/scope were verified.
- `Blocked`: the Step cannot safely reach `Verified` or `Committed` within the approved Plan.

**Never begin Step N+1 until Step N is `Committed`.** A test pass, code completion, or staged diff is not a completed Step without its commit.

## Non-negotiable execution rules

1. Execute Steps in Plan order. Do not batch several Steps into one working-tree change or one commit.
2. Keep at most one Step `In Progress`.
3. Follow the Step's ordered file sequence, operations, symbols, pseudocode, requirements, and validation gates. Do not redesign while implementing.
4. Use test-first execution for every behavior covered by a planned RED/GREEN cycle: write the focused test, observe the expected failure, implement the minimum behavior, then refactor/wire while tests remain green.
5. Modify only the current Step's declared files and generated outputs. A required undeclared file, public contract, migration, dependency, or behavior is Plan drift; stop and ask for a Plan/Spec correction.
6. Preserve unrelated dirty or staged work. Never use broad staging such as `git add -A` or `git add .`. Stage and commit only explicit Step paths.
7. Do not overwrite, revert, reformat, stage, or commit another person's unrelated changes. If unrelated edits overlap a Step-owned file, stop and report the overlap.
8. Run the exact focused validation required by the Step, plus the smallest relevant compile/static/regression gate needed to prove the Step is internally complete.
9. Read the full command output and exit status. A lost process handle, partial log, timeout, skipped test, or warning treated as failure by repository policy is not a pass.
10. Review the Step diff against its requirements and the effective Spec before committing. Remove accidental files, debug output, secrets, generated noise, and unrelated refactors.
11. Commit every verified Step immediately using a path-limited semantic commit. Prefer the Plan's proposed message when it matches the final diff and repository convention.
12. Never create an empty commit to simulate Step completion. If a Step is already implemented or produces no semantic diff, classify it as repository/Plan drift and stop for direction.
13. After committing, verify the commit hash, file list, diff summary, validation evidence, and remaining worktree state. Record the commit against the Step before advancing.
14. Do not amend, squash, reset, or rewrite committed history automatically. If a later Step exposes a defect in an earlier commit, stop advancing, make the smallest dedicated corrective commit attributed to the originating Step, rerun affected gates, and report the deviation.
15. Never modify an existing immutable Flyway migration. Execute only the new migration file named by the approved Plan and Spec.
16. Do not silently skip, reorder, merge, split, or expand Steps. Obtain user approval for a material execution-sequence change.

## Per-Step execution workflow

Read `references/step-gate-checklist.md` at the start and end of every Step.

### 1. Lock the Step

- Record Step number/title, requirements, dependencies, declared paths, expected RED/GREEN behavior, validation commands, rollback point, and proposed commit.
- Record `git rev-parse HEAD` as the Step baseline.
- Verify all dependencies are represented by earlier committed hashes.
- Confirm the Step's paths do not overlap unrelated work.

### 2. Revalidate the current repository

- Reopen the actual files and symbols before editing; do not rely only on Plan pseudocode.
- Confirm the Plan's implementation direction still matches current APIs, consumers, language/framework style, and migration sequence.
- Treat semantic drift as a blocker. Resolve only mechanical, local details already permitted by a `Plan Clarification` or an unambiguous repository convention.

### 3. Execute the Step in file order

- Apply each declared `CREATE`, `MODIFY`, `DELETE`, `RENAME`, or `GENERATED` action in order.
- For behavior changes, run the focused test at the RED point and confirm the failure is caused by missing behavior rather than syntax, fixture, dependency, or environment errors.
- Implement only the minimum Spec-compliant behavior needed for GREEN.
- Preserve current project style, comments/annotations, module boundaries, public compatibility, and unrelated behavior.

### 4. Verify the Step

- Run the Step's exact focused command and confirm the objective expected result.
- Run applicable compile, lint/format, mapper/XML/schema, module, or cross-module checks required by the Step and repository.
- Run `git diff --check` on the Step paths.
- Re-read the Step requirements and relevant Spec sections. Confirm every stated behavior, error path, field, state, permission, migration, UI state, and test obligation represented by this Step is implemented.

Any failed gate keeps the Step `In Progress` or `Blocked`; it cannot be committed as complete.

### 5. Review and commit the Step

- Inspect `git diff -- <Step paths>` and `git status --short`.
- Confirm only Step-owned paths will be committed.
- Stage explicit paths, then inspect `git diff --cached --check`, `--stat`, and `--name-only`.
- Commit only those paths. If other work is already staged, use a path-limited commit that leaves it untouched.
- Capture the resulting hash and inspect `git show --stat --oneline <hash>` plus the committed file list.
- Confirm unrelated staged/unstaged/untracked work remains preserved.

Only now mark the Step `Committed` and begin the next one.

## Failure and blocker handling

Stop the current Step and report evidence when:

- the Plan or Spec is ambiguous, contradictory, unapproved, or materially stale;
- a required file/symbol/consumer is missing or already changed with different semantics;
- the Step needs an undeclared contract, table/column, migration, dependency, page, permission, or architecture change;
- validation fails for a reason that cannot be repaired inside the Step's approved scope;
- credentials, permissions, external services, or runtime state are required but unavailable;
- another change overlaps a Step-owned file;
- a safe path-limited commit cannot be produced.

Do not mark a blocker as complete, skip to a later Step, or create a misleading commit. Report the affected Step, evidence, Spec/Plan impact, safe options, and recommended next action.

## Commit contract

Every implementation Step must produce at least one non-empty semantic commit before the next Step begins.

For each Step record:

| Evidence | Required value |
| --- | --- |
| Step | Number and exact Plan title |
| Requirements | Source requirement IDs |
| Baseline | Commit before Step edits |
| Commit | Resulting full or short hash |
| Paths | Exact committed file list |
| Validation | Commands and observed results |
| Deviations | `None`, approved clarification, or corrective-commit explanation |

Use path-limited staging/commits. Never include unrelated work merely because it was already staged. Do not push, open a PR, merge, or release unless the user separately authorizes it.

## Final Spec conformance audit

After every Plan Step is committed, read `references/final-spec-audit.md` and perform a fresh audit. Do not rely on the Plan's traceability matrix alone.

1. Re-resolve the final effective Spec set and exact revisions.
2. Extract every effective requirement, acceptance criterion, non-goal, interface, model/schema rule, UI behavior, test obligation, non-functional constraint, migration, compatibility, rollout, and rollback requirement.
3. Map each requirement to concrete implementation evidence: commit(s), paths/symbols, and validation/test output.
4. Run the Plan's final source/static/module/full regression commands that are safe and authorized. Do not automatically start runtime systems.
5. Assign one status to every requirement:
   - `Satisfied`: implementation and required non-runtime evidence prove it.
   - `Partial`: only part of the requirement is implemented or proven.
   - `Not satisfied`: implementation conflicts with or omits the requirement.
   - `Runtime unverified`: source/module evidence exists, but the Spec requires user-controlled live-system proof that was not run.
6. Check non-goals and scope boundaries for accidental behavior, dependency, migration, or refactor expansion.
7. Check every Plan Step has a verified commit and no planned file/validation gate was silently omitted.
8. Report every `Partial`, `Not satisfied`, and `Runtime unverified` item with evidence, impact, and recommended next action.

Do not silently add unplanned fixes during the final audit. If the audit finds a gap, report it and wait for the user to approve a corrective Plan/Step.

## Final report contract

The completion report must contain:

- Plan path/revision and effective Spec paths/revisions;
- a Step table with status, commit hash, committed paths, and validation evidence;
- final validation commands and actual results;
- a Spec conformance matrix with every requirement status;
- explicit unmet, partial, and runtime-unverified requirements;
- approved deviations and corrective commits;
- remaining worktree state and confirmation that unrelated work was preserved;
- whether runtime, database, browser, deployment, push, PR, or release actions were not performed;
- one final verdict:
  - `PASS — Implementation conforms to the effective Specs`
  - `PARTIAL — Spec requirements are unmet or unverified`
  - `BLOCKED — Final verification could not be completed`

Never claim full completion when any effective requirement is `Partial`, `Not satisfied`, or required runtime evidence is missing.

## Common failures

| Failure | Required correction |
| --- | --- |
| Editing several Steps before committing | Revert only the unapproved later-Step edits safely; finish and commit the current Step first |
| Starting Step N+1 after tests but before commit | Stop; verify and commit Step N |
| Committing unrelated staged files | Use path-limited commit and verify the committed file list |
| Creating an empty Step commit | Stop and report Plan/baseline drift |
| Treating Plan completion as Spec compliance | Run the independent final Spec audit |
| Fixing an unplanned Spec gap during final audit | Report it and request a corrective Plan/Step |
| Claiming runtime acceptance from unit/module tests | Mark the requirement `Runtime unverified` |
| Rewriting earlier Step commits after later work | Preserve history; use an attributed corrective commit and report it |
| Starting the project automatically | Leave runtime testing to the user unless explicitly requested |

## Skill maintenance

When changing this skill, review `references/acceptance-scenarios.md` and confirm the English operational file, Chinese review mirror, checklists, and metadata still express the same execution contract.
